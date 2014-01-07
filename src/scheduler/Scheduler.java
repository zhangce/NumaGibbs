package scheduler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import util.FileLock;

public class Scheduler {

	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		//String[] NUMANodes = args[0].split(",");
		//int threads_per_node = Integer.parseInt(args[1]);
		//String command = args[2];
		
		String[] NUMANodes = args[0].split(",");
		int threads_per_node = Integer.parseInt(args[1]);
		String command = args[2];
		
		long task_size = Long.parseLong(args[3]);
		
		//"java -jar /Users/czhang/Desktop/NumaGibbs/jars/TestWorker.jar";
		
		//String command_args = "   300000";
		int NEPOCH = 10;
				
		int nProcess = NUMANodes.length;

		File f = File.createTempFile("NumaGibbs_MMFile_Barrier", ".txt");
		int bufferSize = 8*nProcess;
		FileChannel fc = new RandomAccessFile(f, "rw").getChannel();
		MappedByteBuffer mem =fc.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
		
		////////////IN BETWEEN ARE JUST FOR TESTING PARALLEL SUM////////////
		File fstatic = new File("/tmp/static.txt");
		long fstatic_bufferSize = 4*task_size;
		FileChannel fstatic_fc = new RandomAccessFile(fstatic, "rw").getChannel();
		MappedByteBuffer fstatic_mem =fstatic_fc.map(FileChannel.MapMode.READ_WRITE, 0, fstatic_bufferSize);
		for(int i=0;i<task_size;i++){
			fstatic_mem.putInt(i);
		}	
		fstatic_mem.force();
		
		File fresult = new File("/tmp/output.txt");
		long fresult_bufferSize = nProcess * 8;
		FileChannel fresult_fc = new RandomAccessFile(fresult, "rw").getChannel();
		MappedByteBuffer fresult_mem =fresult_fc.map(FileChannel.MapMode.READ_WRITE, 0, fresult_bufferSize);
		for(int i=0;i<nProcess;i++){
			fresult_mem.putLong(0);
		}	
		fresult_mem.force();

		////////////////////////////////////////////////////////////////////	
		
		for(int i=0;i<nProcess;i++){
			mem.putLong(0);
		}
		
		long startTime = System.currentTimeMillis();
		
		ArrayList<Process> processes = new ArrayList<Process>();
		for(int i=0;i<nProcess;i++){
			List<String> commands = new ArrayList<String>();
			for(String s : command.replaceAll("{NODE}", "" + i).split(" ")){
				commands.add(s);
			}
			commands.add("" + task_size);
			commands.add("" + threads_per_node);
			commands.add("" + NEPOCH);
			commands.add("");
			commands.add(f.getAbsolutePath());
			commands.set(commands.size()-2, "" + (i*8));
			ProcessBuilder pb = new ProcessBuilder().inheritIO().command(commands);
			processes.add(pb.start());
		}
		
		while(NEPOCH >= 1){
			
			NEPOCH = NEPOCH - 1;
			
			// wait until done
			while(true){
				long n = 0;
				mem =fc.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
				for(int i=0;i<nProcess;i++){
					n = n + mem.getLong();
				}
				if(n == nProcess){
					break;
				}
				Thread.sleep(10);
			}
			
			long endTime   = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			
			System.out.println("TOTAL TIME : " + totalTime);
			fresult_mem =fresult_fc.map(FileChannel.MapMode.READ_WRITE, 0, fresult_bufferSize);
			for(int i=0;i<nProcess;i++){
				System.out.println(fresult_mem.getLong(0));
			}	
				
			mem =fc.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for(int i=0;i<nProcess;i++){
				mem.putLong(0);
			}
			
			startTime = System.currentTimeMillis();

		}
		
		for(Process p : processes){
	        p.waitFor();
	        p.destroy();	
		}
		
		System.out.println("-----");
		
	}
	
}
