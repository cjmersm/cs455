package cs455.cdn.communications;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


public class TCPConnection extends Connection {

	SocketChannel sc;
	ByteBuffer bb;

	public TCPConnection(SocketChannel sc){
		this.sc = sc;
		
		bb = ByteBuffer.allocate(4100);
		try {
			while (!this.sc.finishConnect()) {
				// pretend to do something useful here
				//System.out.println("Doing something useful...");.
				//System.out.print(".");
			}
			//System.out.println(".");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	public SocketChannel getSocket(){
		return sc;
	}
	
	public byte[] read() throws IOException{
//		try {
//			Thread.sleep(1);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		bb.clear();
		
		int size = sc.read(bb);
		
		bb.flip();	
		byte[] temp = new byte[size];
		bb.get(temp);
		
		bb.clear();

		//System.out.println("Reading: "+byteArrayToString(temp));
		
		return temp;
	}

	
	public void write(byte[] b) throws IOException{
		
		bb.clear();
		
		bb.put(b);
		bb.flip();
		
		sc.write(bb);
		bb.clear();
	}
	
	
	public void write(String s) throws IOException{
		byte[] b = s.getBytes();
		//System.out.println("Writing: "+s);
		this.write(b);
	}
}
