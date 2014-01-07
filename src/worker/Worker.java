package worker;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import util.FileLock;

/**
 * A worker is a process that runs on a single NUMA node.
 * All workers supported to run in scheduler must inherent 
 * from the Worker class.
 *  
 * @author czhang
 */
public abstract class Worker {
	
	String lockfile;
	
	int mypos;
	
	int NEPOCH;
	
	abstract boolean should_terminate();
	
	abstract void do_realwork();
	
	abstract void dump_result();
	
	abstract void load_static_data();
	
	abstract void load_dynamic_data();

	abstract void init();
	
	void load_locks(String [] args) throws IOException{
		this.lockfile = args[args.length - 1];
		this.mypos = Integer.parseInt(args[args.length - 2]);
		this.NEPOCH = Integer.parseInt(args[args.length - 3]);
	}
	
	void execute(String [] args) throws IOException, InterruptedException{
		
		this.load_locks(args);
		
		this.load_static_data();
		
		while(!this.should_terminate()){
			
			// load dynamic data
			this.load_dynamic_data();
			
			// do real work
			this.do_realwork();

			// output
			this.dump_result();
			
			FileChannel fc = new RandomAccessFile(this.lockfile, "rw").getChannel();
			MappedByteBuffer mem =fc.map(FileChannel.MapMode.READ_WRITE, this.mypos, 8);

			mem.putLong(1);
			
			while(true){
				mem =fc.map(FileChannel.MapMode.READ_WRITE, this.mypos, 8);
				if(mem.getLong() == 0){
					break;
				}
				Thread.sleep(10);
			}
		}

	}

	
	/*
	static void main(String[] args){
		Worker worker = new Worker();
		worker.execute();
	}
	*/
	
}
