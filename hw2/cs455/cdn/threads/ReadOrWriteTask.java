package cs455.cdn.threads;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;


import cs455.cdn.communications.TCPConnection;

public class ReadOrWriteTask implements Runnable{

	SocketChannel sc;
	TCPConnection tcp;
	private int localname;
	private static int name =0;
	
	/**
	 * This class is just the task that is managed by the workers of the thread pool
	 * this class uses NIO to allow readability over a socket and then respond quickly to that 
	 * socket
	 * @param tcp
	 */
	public ReadOrWriteTask(TCPConnection tcp){
		this.sc = tcp.getSocket();
		this.tcp = tcp;

		//name = ran.nextInt(100);
		localname = name++;
	}

	public void run(){

		try {

			Selector select = Selector.open();
			sc.configureBlocking(false);
			sc.register(select, SelectionKey.OP_READ /*| SelectionKey.OP_WRITE */);

			Set<SelectionKey> keys = select.selectedKeys( );

			//while(true){
			select.select(); //find something that's ready
			for(Iterator<SelectionKey> i = keys.iterator( ); i.hasNext( ); ) {

				SelectionKey key = (SelectionKey)i.next( );
				i.remove( );

				String temp = "";
				
				if (key.isReadable()) {
					Thread.sleep(1);
					
					byte[]b = tcp.read();
														
					temp = SHA1FromBytes(b);
					System.out.println("Thread "+localname+": reading "+temp);

					tcp.write(temp);
				} 
				//if(key.isWritable()){
					//tcp.write(temp);
					//System.out.println("writeable");
				//}
			}
			
			select.close();
			
			
			//}
			//}
		} catch (IOException | NoSuchAlgorithmException | InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}

	public String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		return hashInt.toString(16);
	}
}
