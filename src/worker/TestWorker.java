package worker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class TestWorker extends Worker {
	
	MappedByteBuffer fstatic_mem;
	
	MappedByteBuffer fresult_mem;
	
	long rs;
	
	int[] data;
	
	int size;
	
	int nthread;
	
	@Override
	boolean should_terminate() {
		this.NEPOCH =  this.NEPOCH - 1;
		if(this.NEPOCH < 0){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	void init(){

	}

	public class ThreadWorker implements Runnable {

	    int[] data;
	    int id;
	    int total;
	    long rs;

	    public ThreadWorker(int[] _data, int _id, int _total) {
	        this.data = _data;
	        this.id = _id;
	        this.total = _total;
	    }

	    public void run() {
	    	this.rs = 0;
	    	for(int i=this.id;i<this.data.length;i+=this.total){
	    		this.rs = this.rs + this.data[i];
	    	}
	    }
	}
	
	@Override
	void do_realwork() {
		
		ArrayList<ThreadWorker> workers = new ArrayList<ThreadWorker>();
		ArrayList<Thread> threads = new ArrayList<Thread>();
		this.rs = 0;
		for(int i=0;i<this.nthread;i++){
			ThreadWorker tw = new ThreadWorker(this.data, i, this.nthread);
			workers.add(tw);
			Thread t = new Thread(tw);
			threads.add(t);
		}
		
		for(int i=0;i<this.nthread;i++){
			threads.get(i).start();
		}

		try {
			for(int i=0;i<this.nthread;i++){
				threads.get(i).join();
				this.rs = this.rs + workers.get(i).rs;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	void dump_result() {
		
		try {
			File fresult = new File("/tmp/output.txt");
			int fresult_bufferSize = 8;
			FileChannel fresult_fc = new RandomAccessFile(fresult, "rw").getChannel();
			this.fresult_mem =fresult_fc.map(FileChannel.MapMode.READ_WRITE, this.mypos, fresult_bufferSize);
			fresult_mem.putLong(this.rs);
			fresult_mem.force();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	void load_static_data() {
		try {
			this.data = new int[this.size];
			File fstatic = new File("/tmp/static.txt");
			int fstatic_bufferSize = 4*this.size;
			FileChannel fstatic_fc = new RandomAccessFile(fstatic, "rw").getChannel();
			this.fstatic_mem =fstatic_fc.map(FileChannel.MapMode.READ_WRITE, 0, fstatic_bufferSize);
			for(int i=0;i<this.size;i++){
				data[i] = fstatic_mem.getInt();
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	void load_dynamic_data() {

	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		TestWorker worker = new TestWorker();

		worker.nthread = Integer.parseInt(args[args.length-4]);
		worker.size = Integer.parseInt(args[args.length-5]);
		
		worker.execute(args);
	}

}
