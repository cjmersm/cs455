package cs455.cdn.communications;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class UDPConnection extends Connection {

	DatagramChannel gc;
	DatagramSocket gs;
	ByteBuffer bb;

	public UDPConnection(DatagramChannel gc){
		this.gc = gc;
		bb = ByteBuffer.allocate(4100);
				
	}
	
	
	public UDPConnection(DatagramSocket gs){
		this.gs = gs;
	}

	
	public byte[] read() throws IOException{
		bb.clear();

		int size = gc.read(bb);
		bb.flip();
		
		byte[] temp = new byte[size];
		bb.get(temp);
		
		System.out.println("Read: "+byteArrayToString(temp));
		
		return temp;
	}

	
	public void write(byte[] b) throws IOException{
		bb.clear();
		bb.put(b);
		bb.flip();
		gc.write(bb);
		bb.clear();
	}
	
	
	public void write(String s) throws IOException{
		byte[] b = s.getBytes();
		System.out.println("Write: "+s);
		this.write(b);
	}
	
}
