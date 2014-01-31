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

public class ReadOrWrite implements Runnable{

	SocketChannel sc;
	TCPConnection tcp;


	public ReadOrWrite(SocketChannel sc){
		this.sc = sc;
		tcp = new TCPConnection(sc);

	}

	public void run(){

		//set up ServerSocketChannel
		try {

			Selector select = Selector.open();
			sc.configureBlocking(false);
			sc.register(select, SelectionKey.OP_READ);

			Set<SelectionKey> keys = select.selectedKeys( );

			//look for accept notifications
			while(true){
				select.select(); //find something that's ready
				for(Iterator<SelectionKey> i = keys.iterator( ); i.hasNext( ); ) {
					// Get a key from the set, and remove it from the set
					SelectionKey key = (SelectionKey)i.next( );
					i.remove( );
					
					String temp = "";
					//housekeeping...
					if (key.isReadable()) {
						byte[]b = tcp.read();

						temp = SHA1FromBytes(b);
						System.out.println("reading "+temp);
						
						tcp.write(temp);
						
						break;

					} 
					if(key.isWritable() && !temp.equals("")){
						tcp.write(temp);
			
						System.out.println("writing "+temp);
						//break;
					}
				}
			}
		} catch (IOException | NoSuchAlgorithmException e1) {
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
