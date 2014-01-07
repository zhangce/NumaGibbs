package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileLock {

	String filename = "";
	FileChannel in;
	java.nio.channels.FileLock lock;
	
	public FileLock(String _filename) throws IOException{
		this.filename = _filename;
	}
	
	public void finalize() throws IOException{
		this.in.close();
	}
	
	public void lock() throws IOException{
		this.in = new RandomAccessFile(this.filename, "rw").getChannel();
		this.lock = this.in.lock();		
	}
	
	public void release() throws IOException{
		this.lock.release();
		this.in.close();
	}
	
}
