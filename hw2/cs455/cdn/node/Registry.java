
package cs455.cdn.node;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import cs455.cdn.communications.TCPConnection;
//import cs455.cdn.util.ConsoleListener;
import cs455.cdn.util.ConsoleListener;

public class Registry extends Node{

	private int refresh_interval;

	private ServerSocketChannel tcpserver;
	private ArrayList<ArrayList<String>> aas;



	Registry(int registry_port,int refresh_interval){
		this.type = 'r';
		this.refresh_interval = refresh_interval;
		aas = new ArrayList<ArrayList<String>>();

		try {
			tcpserver = ServerSocketChannel.open();
			tcpserver.socket().bind(new InetSocketAddress(registry_port));
			tcpserver.configureBlocking(false);

			selector = Selector.open();

		} catch (IOException e) {
			e.printStackTrace();
		}

		nodelist = new ArrayList<String>();
	}


	public void listen(){
		try{
			System.out.println("Waiting for Connections...");
			//while (true) {
				SocketChannel sc = null;
				Selector select = Selector.open();
				tcpserver.register(select, SelectionKey.OP_ACCEPT);

				Set<SelectionKey> keys = select.selectedKeys( );

				//look for accept notifications
				while(true){
					select.select(); //find something that's ready
					for(Iterator<SelectionKey> i = keys.iterator( ); i.hasNext( ); ) {
						// Get a key from the set, and remove it from the set
						SelectionKey key = (SelectionKey)i.next( );
						i.remove( );
						//housekeeping...
						if (key.isAcceptable()){
							sc = tcpserver.accept();
							sc.configureBlocking(false);
							sc.register(select, SelectionKey.OP_READ);
						}
						if (key.isReadable()) {

							TCPConnection t = new TCPConnection(sc);

							byte[] b = t.read();
							String message = byteArrayToString(b);


							Scanner mesScan = new Scanner(message);
							mesScan.useDelimiter(";");

							int num = mesScan.nextInt();

							if(num == REGISTRY_REQUEST) {

								String node_id = mesScan.next(); //System.out.println(node_id);
								String address = mesScan.next(); //System.out.println(address);
								int tcpPort = mesScan.nextInt(); //System.out.println(tcpPort);
								int udpPort = mesScan.nextInt(); //System.out.println(udpPort);

								nodelist.add(node_id+";"+address+";"+tcpPort+";"+udpPort);

								System.out.println("Register: Node:" +node_id+ " | Address:"  +address + " | TCP:"+tcpPort+ " | UDP:"+ udpPort + " | has Registered.");

								String response = "2;1;Success";
								t.write(response);

							}
							else if(num == DEREGISTER_REQUEST) {
								String node_id = mesScan.next(); //System.out.println(node_id);
								String address = mesScan.next(); //System.out.println(address);
								int tcpPort = mesScan.nextInt(); //System.out.println(tcpPort);
								int udpPort = mesScan.nextInt(); //System.out.println(udpPort);

								nodelist.remove(node_id+";"+address+";"+tcpPort+";"+udpPort);

								System.out.println("Deregister: Node:" +node_id+ " | Address:"  +address + " | TCP:"+tcpPort+ " | UDP:"+ udpPort + " | has Deregistered.");

								String response = DEREGISTER_RESPONSE+";SeeYa";
								t.write(response);
							}


							mesScan.close();
							sc.close();

						} 
					}
				}

				

			//}
		} catch (IOException  e){
			e.printStackTrace();
		}
	}

	/**
	 * setupcdn
	 * 
	 * this creates the graph and sends the peer node list to all nodes in the system so that
	 * they can know where they need to go.
	 * @param numOfConnections
	 * @param connectionType
	 */
	public void setupcdn(int numOfConnections, String connectionType){


		aas = new ArrayList<ArrayList<String>>();

		for(int i =0;i<nodelist.size();i++){ // initialize arraylist for peer node lists
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(nodelist.get(i));
			aas.add(temp);
		}

		for(int i = 0;i<nodelist.size();i++){ // create all the connections involved

			for(int j=1;j<=numOfConnections/2;j++){
				if(i+j>nodelist.size()-1){
					aas.get(i).add(nodelist.get(i+j-(nodelist.size())));
				}
				else{
					aas.get(i).add(nodelist.get(i+j));
				}
			}
		}

		for(int i=0;i<aas.size();i++){	// print peer node list
			for(int j=0;j<aas.get(i).size();j++){
				System.out.print(aas.get(i).get(j).charAt(0)+ "\t");
			}
			System.out.println();
		}


		///////////// Sending the peer lists ////////////////

		for(int i =0;i<aas.size();i++){

			String node = aas.get(i).get(0);
			Scanner nodeScan = new Scanner(node);
			nodeScan.useDelimiter(";");

			String name = nodeScan.next();
			String address = nodeScan.next();
			int tcpport = nodeScan.nextInt();

			System.out.println("Sending peer node list to "+name+" at "+address+":"+tcpport);
			//nodeScan.nextInt();

			String message = PEER_NODE_LIST_MESSAGE+";"+"tcp"+";"+(aas.get(i).size()-2);
			for(int j=1;j<aas.get(i).size();j++){
				message += "\n"+aas.get(i).get(j);
			}

			SocketChannel temp;
			try {
				temp = SocketChannel.open();

				temp.configureBlocking(false);

				temp.connect(new InetSocketAddress(address, tcpport));	
				TCPConnection con = new TCPConnection(temp);

				con.write(message);	

				temp.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			nodeScan.close();
		}
		System.out.println("Done!");

		///////////// /////////////////////// ////////////////

	}

	/**
	 * continously send weights updated for every edge within the graph so that
	 * the nodes can generate a minimum spanning tree.
	 */
	public Runnable updateWeight = new Runnable(){
		public void run(){
			try {
				Thread.sleep(40);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			while(true){
				try {
					String weights = WEIGHT_UPDATE+";\n";
					Random r = new Random();

					for(int i = 0;i<aas.size();i++){
						for(int j = 1;j<aas.get(i).size();j++){
							weights += nodelist.get(i)+ "\t" + aas.get(i).get(j)+ "\t" + (r.nextInt(10)+1) + "\n";
						}
					}

					
					//System.out.println(weights);
					printNewWeights(weights);
					

					for(int i =0;i<nodelist.size();i++){  // Send to all nodes, should make this a method.
						Scanner scan = new Scanner(nodelist.get(i));
						scan.useDelimiter(";");
						scan.next();
						String address = scan.next();
						int port = scan.nextInt();

						SocketChannel temp;
						temp = SocketChannel.open();
						temp.configureBlocking(false);

						temp.connect(new InetSocketAddress(address, port));	
						TCPConnection con = new TCPConnection(temp);

						con.write(weights);

						temp.close();
						scan.close();
					}


					System.out.println("Sent Weight Updates");

					Thread.sleep(refresh_interval);


				} catch (InterruptedException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};


	public void printNewWeights(String lines){
		Scanner scan = new Scanner(lines);

		scan.useDelimiter("\n");
		scan.next();
		System.out.println("Updated Weights:");
		while(scan.hasNext()){
			System.out.println("\t"+scan.next());
		}
		
		scan.close();
	}
	
	
	public void shutdown(){
		System.out.println("\n\nThank you for your time.");
		System.exit(0);
	}
	



	/////////////////////////////////// MAIN //////////////////////////////////////////

	public static void main(String args[]) throws InterruptedException
	{
		if(args.length==2){

			Registry reg = new Registry(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
			ConsoleListener terminal = new ConsoleListener(reg);

			Thread t1 = new Thread(terminal);
			t1.start();

			reg.listen();


		}
		else{
			System.out.println("Usage: java Registry registry_port refresh_interval");
		}
	}

}
