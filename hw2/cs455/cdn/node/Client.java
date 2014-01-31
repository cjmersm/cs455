package cs455.cdn.node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;

import cs455.cdn.communications.TCPConnection;

public class Client {

	private LinkedList<String> hashes;

	private int rate;
	private SocketChannel sc;
	TCPConnection tcp;
	int counter = 0;
	


	public Client(String address, int port, int rate){
		hashes = new LinkedList<String>();
		this.rate = rate;
		
		try {
			sc = SocketChannel.open();
			sc.connect(new InetSocketAddress(address,port));
			sc.configureBlocking(true);

			tcp = new TCPConnection(sc);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * listens on connections for the returned hash from the messenger node
	 */
	public Runnable listen = new Runnable(){
		public synchronized void run(){
			try {
				while(true){
					byte[] b = tcp.read();
					String s = new String(b);
					//System.out.println("s: "+s);
					boolean success = hashes.remove(s);
				
					if(success){
						System.out.println("Success "+counter++);
					}
					else{
						System.out.println("Nope "+counter++);
					}
				}

			} catch ( IOException  e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Client is done listening.");
				System.exit(0);

			}
		}
	};

	/**
	 * always sending data to the messenger nodes
	 */
	public Runnable send = new Runnable(){
		public synchronized void run(){
			try {
				Random r = new Random();
				String s = "";
				byte[] b = new byte[4096];
				
				while(true){
										
					r.nextBytes(b);
					
					s = SHA1FromBytes(b);				
					hashes.add(s);
					
					//System.out.println("Writing"+s);

					//System.out.println(b.length);
					tcp.write(b);

					Thread.sleep(1000/rate);
				}

			} catch (InterruptedException | IOException | NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Client is done writing.");
				System.exit(0);
			}
		}
	};

	public String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		return hashInt.toString(16);
	}

	public static void main(String args[]){
		if(args.length!=3){
			System.out.println("Usage: java Client <node-host> <node-port> <message-rate>");
		}
		else{
			Client c = new Client(args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]));

			Thread t1 = new Thread(c.send);
			t1.start();

			Thread t2 = new Thread(c.listen);
			t2.start();		
		}
	}

}
