

package cs455.cdn.node;



import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;



import cs455.cdn.communications.*;
import cs455.cdn.mst.Edge;
import cs455.cdn.mst.KruskalEdges;
import cs455.cdn.threads.ReadOrWriteTask;
import cs455.cdn.threads.ThreadPool;
import cs455.cdn.util.ConsoleListener;

@SuppressWarnings("unused")
public class MessagingNode extends Node {


	private String node_id;
	private String registry_host;
	private int registry_port;
	private int thread_pool_size;
	private int tracker;

	private int tcp_port;
	private int udp_port;
	private String my_address;

	private SocketChannel regsc;
	private TCPConnection tcpc;

	private ServerSocketChannel tcpserver;
	private DatagramChannel udpserver;
	private ServerSocketChannel scalableServer;


	private String cdnConnectionType = "tcp";
	private KruskalEdges vv; 
	private KruskalEdges cdnPathList;
	private ConnectionFactory cf;

	private String tempfilename = "";
	private ThreadPool tPool;


	@SuppressWarnings("static-access")
	MessagingNode(String node_id, String registry_host, int registry_port, int thread_pool_size){
		tracker = 0;
		this.type = 'n';

		this.node_id = node_id;
		this.registry_host = registry_host;
		this.registry_port = registry_port;
		this.thread_pool_size = thread_pool_size;

		nodelist = new ArrayList<String>();
		cf = new ConnectionFactory("tcp"); // Defaults


		try{

			// Create and bind a tcp channel to listen for connections on.
			tcpserver = ServerSocketChannel.open();
			tcpserver.socket().bind(new InetSocketAddress(0));
			tcp_port = tcpserver.socket().getLocalPort();

			// Also create and bind a DatagramChannel to listen on.
			udpserver = DatagramChannel.open();
			udpserver.socket().bind(new InetSocketAddress(0));
			udp_port = udpserver.socket().getLocalPort();

			// Address of the machine
			my_address = tcpserver.socket().getInetAddress().getLocalHost().getHostAddress();

			// Specify non-blocking mode for both channels, since our
			// Selector object will be doing the blocking for us.
			tcpserver.configureBlocking(false);
			udpserver.configureBlocking(false);



			File file = new File(node_id+".file");
			if(file.exists()){
				file.delete();
			}


		} catch (IOException e){
			e.printStackTrace();
		}
	}

	
	/**
	 * sets up initial connection with the register to start off the node process
	 */
	public void regConnect(){
		try{
			regsc = SocketChannel.open();
			regsc.configureBlocking(false);

			regsc.connect(new InetSocketAddress(registry_host, registry_port));	
			tcpc = new TCPConnection(regsc);

			tcpc.write(REGISTRY_REQUEST+";"+node_id+";"+my_address+";"+tcp_port+";"+udp_port);	
			Thread.sleep(11); // So that it has time to read otherwise there is no message.


			byte[] b = tcpc.read();

			String message = byteArrayToString(b);
			//System.out.println("message "+message);
			Scanner mesScan = new Scanner(message);
			mesScan.useDelimiter(";");

			mesScan.nextInt();
			mesScan.nextInt();
			String res = mesScan.next();


			System.out.println("Registration: "+res);

			mesScan.close();
			regsc.close();

			Thread t2 = new Thread(listen);
			t2.start();


		} catch (IOException  e){
			System.out.println("No Register Was Found, Going into Part 2 Mode!");
			scalable();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * listen
	 * 
	 * this is the messaging node listener that works for registry communciations and node to node communications
	 * depending on protocal headers
	 */
	Runnable listen = new Runnable(){
		public void run(){

			System.out.println("Waiting for Connection...");
			while(true){
				try {

					SocketChannel sc = tcpserver.accept();

					if (sc == null) {
						//System.out.println("is this thing on?");
					} else { // received an incoming connection
						//System.out.println("Received a connection");
						TCPConnection t = new TCPConnection(sc);

						byte[] b = t.read();
						String message = byteArrayToString(b);


						Scanner mesScan = new Scanner(message);
						mesScan.useDelimiter(";");

						int num = mesScan.nextInt();

						////////////////////////////////////////////////////////////////////////


						if(num == PEER_NODE_LIST_MESSAGE) {
							nodelist = new ArrayList<String>();

							cdnConnectionType = mesScan.next();
							cf = new ConnectionFactory(cdnConnectionType);

							//int numConnections = mesScan.nextInt();

							Scanner listScan = new Scanner(message);
							listScan.useDelimiter("\n");

							listScan.next();

							while(listScan.hasNext()){
								nodelist.add(listScan.next());
							}

							listScan.close();
							System.out.println("Received Peer Node List");

							justifyConnections();
						}


						////////////////////////////////////////////////////////////////////////


						else if(num == NODE_TO_NODE_JUSTIFY){
							String node = message.replaceFirst(NODE_TO_NODE_JUSTIFY+";", "");

							//System.out.println("MESSAGE : "+message);
							//System.out.println("NODE ST : "+node);

							nodelist.add(node);
							System.out.println(node+" has been added.");
						}

						////////////////////////////////////////////////////////////////////////

						else if(num == WEIGHT_UPDATE){

							System.out.println("Computing Minimum Spanning Tree");
							computeMinimumSpanningTree(message.replaceFirst(WEIGHT_UPDATE+";\n", ""));		
						}

						////////////////////////////////////////////////////////////////////////

						else if(num == DATA_PACKET){
							int flag = mesScan.nextInt();


							if(flag == 1){
								tempfilename = mesScan.next();
								if(!tempfilename.contains("/")){
									tempfilename = "/tmp/mersman/"+tempfilename;
								}
//								else{
//									tempfilename = "/tmp/mersman/"+tempfilename.substring(tempfilename.lastIndexOf("/"), tempfilename.length()-1);
//								}

								File f = new File(tempfilename);
								f.delete();

								cdnPathList = new KruskalEdges();
								while (mesScan.hasNextLine()){
									String line = mesScan.nextLine();
									if(!line.equals(";")){
										Edge e = new Edge(getNodeA(line),getNodeB(line),getWeight(line));
										cdnPathList.insertEdge(e);
									}
								}
								System.out.println("Receiving Data: "+tempfilename);
							}
							else if(flag == 2){
								File f = new File(tempfilename);
								if(!tempfilename.contains("/")){
									tempfilename = "/tmp/mersman/"+tempfilename;
								}
//								else{
//									tempfilename = "/tmp/mersman/"+tempfilename.substring(tempfilename.lastIndexOf("/"), tempfilename.length()-1);
//								}

								File dir=new File("/tmp/mersman");
								dir.mkdirs();

								FileOutputStream fos = new FileOutputStream(f,true);
								byte[] arr = Arrays.copyOfRange(b, (num+";2;").length(), b.length);

								fos.write(arr);
								System.out.print(".");
								fos.close();
							}
							else { // flag == 3
								System.out.println("Done");
								System.out.println("filename = "+tempfilename);
								sendData(tempfilename);
							}

						}

						////////////////////////////////////////////////////////////////////////

						mesScan.close();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// helps create the path of the cdn
	public void initializeSending(){
		cdnPathList=vv;
	}

	/**
	 * sendData
	 * @param filename
	 * 
	 * the filename is passed for getting the data from memory to send through the wire two other nodes
	 * something to notice is that this wont work for a path with a file, just a file name that is in 
	 * the same directory as this file structure is
	 */
	public void sendData(String filename){

		
		ArrayList<byte[]> sections = new ArrayList<byte[]>();
		FileInputStream fileInputStream=null;

		File file = new File(filename);

		byte[] bFile = new byte[(int) file.length()];

		try {
			//convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();

			//cdnPathList = new KruskalEdges();


			String firstMessage = DATA_PACKET+";1;"+filename+";";


			for(Edge e : cdnPathList.getEdges()){

				String a = e.getVertexA();
				String b = e.getVertexB();

				if(getNodeName(a).equals(node_id) || getNodeName(b).equals(node_id)){

				}
				else{
					firstMessage += "\n"+a+"\t"+b+"\t"+e.getWeight();
					//cdnPathList.insertEdge(e);
					//System.out.println(e);
				}
			}
			sections.add(firstMessage.getBytes());

			if(bFile.length>4096){
				byte[] section = new byte[4096];

				int i;
				for( i = 0;i+4096<bFile.length;i+=(4096)){

					section = Arrays.copyOfRange(bFile, i, i+4096);

					sections.add(section);
				}

				byte[] newer = Arrays.copyOfRange(bFile, i, bFile.length);
				sections.add(newer);

			}
			else{
				byte[] section = bFile;
				sections.add(section);
			}

			for(Edge e : cdnPathList.getEdges()){

				String a = e.getVertexA();
				String b = e.getVertexB();

				for(int i = 0;i<=sections.size();i++){
					tracker++;
					if(getNodeName(a).equals(node_id)){


						String add = getAddressFromNode(b);
						int port = getPortFromNode(b);

						Connection c = cf.getConnector(cf.getConnection(add, port));

						if(i==sections.size()){
							byte[] total = (DATA_PACKET+";3;").getBytes();
							c.write(total);
							System.out.println("Done!");
						}
						else if(i==0){
							System.out.println("Sending data to "+b);
							c.write(sections.get(i));
						}
						else{
							byte[] total = arrConcat(stringToByteArray(DATA_PACKET+";2;"),sections.get(i));
							c.write(total);
							System.out.print(".");
						}
					}

					if(getNodeName(b).equals(node_id)){
						String add = getAddressFromNode(a);
						int port = getPortFromNode(a);

						Connection c = cf.getConnector(cf.getConnection(add, port));

						if(i==sections.size()){
							byte[] total = (DATA_PACKET+";3;").getBytes();
							c.write(total);
							System.out.println("Done!");
						}
						else if(i==0){
							System.out.println("Sending data to "+a);
							c.write(sections.get(i));
						}
						else{
							byte[] total = arrConcat(stringToByteArray(DATA_PACKET+";2;"),sections.get(i));
							c.write(total);
							System.out.print(".");
						}
					}
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println("File: "+filename+" does not exist.");
		}

	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * computeMinimumSpanningTree
	 * @param message
	 * 
	 * this generates the best way to go using Kruskal's algorithm. Code was heavily dependant another source.
	 */
	public void computeMinimumSpanningTree(String message){
		Scanner listScan = new Scanner(message);
		listScan.useDelimiter("\n");

		ArrayList<String> edges = new ArrayList<String>();

		while(listScan.hasNext()){
			String line = listScan.next();
			edges.add(line);
		}

		/////////////////////////////////////////////////////////////////////////////////////
		vv = new KruskalEdges();
		while(!edges.isEmpty()){

			String line = removeMinEdge(edges);
			Edge edge = new Edge(getNodeA(line),getNodeB(line),getWeight(line));
			vv.insertEdge(edge);
		}

		for (Edge edge : vv.getEdges()) {
			System.out.println(edge);
		}

		listScan.close();
	}

	//// Helpers for MST ////

	public void printMST(){ // TODO
		if(vv == null){
			System.out.println("There is no MST");
		}
		else{
			String mst = node_id;

			TreeSet<Edge> edges = vv.getEdges();
			int timeout = edges.size();

			recursiveMST(edges, mst, timeout);
		}
	}

	/**
	 * 
	 * recursiveMST
	 * @param edges
	 * @param mst
	 * @param timeout
	 * @return
	 * 
	 * this is to help print out the form that is required.
	 */
	public TreeSet<Edge> recursiveMST(TreeSet<Edge> edges, String mst, int timeout){
		if(edges.size()==0){
			System.out.println(mst);
			return null;
		}
		else if(timeout==0){
			System.out.println(mst);
			return null;
		}
		else{
			
			TreeSet<Edge> totaledges = edges;

			for(Edge e : edges){
				
				String nodeA = getNodeName(e.getVertexA());
				String nodeB = getNodeName(e.getVertexB());
				int weight = e.getWeight();

				//System.out.println("nodeA "+nodeA);
				//System.out.println(getLastNode(mst)[0]);
				if(nodeA.equals(getLastNode(mst)[0]) && !nodeB.equals(getLastNode(mst)[1])){
					
					//System.out.println(nodeA+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeB+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeB;
					totaledges = removeEdge(totaledges,e);
				}
				
			}
			
			for(Edge e : edges){
				String nodeA = getNodeName(e.getVertexA());
				String nodeB = getNodeName(e.getVertexB());
				int weight = e.getWeight();

				//System.out.println("nodeA "+nodeA);
				//System.out.println(getLastNode(mst)[0]);
				
				if(nodeB.equals(getLastNode(mst)[0]) && !nodeA.equals(getLastNode(mst)[1])){
					//System.out.println(nodeB+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeA+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeA;
					totaledges = removeEdge(totaledges,e);
				}
			}
			
			TreeSet<Edge> temp = totaledges;
			
			for(Edge e : temp){
				String nodeA = getNodeName(e.getVertexA());
				String nodeB = getNodeName(e.getVertexB());
				int weight = e.getWeight();

				//System.out.println("nodeA "+nodeA);
				//System.out.println(getLastNode(mst)[0]);
				if(nodeA.equals(getLastNode(mst)[0]) && !nodeB.equals(getLastNode(mst)[1])){
					//System.out.println(nodeA+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeB+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeB;
					totaledges = removeEdge(totaledges,e);
				}
				
				if(nodeB.equals(getLastNode(mst)[0]) && !nodeA.equals(getLastNode(mst)[1])){
					//System.out.println(nodeB+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeA+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeA;
					totaledges = removeEdge(totaledges,e);
				}
			}
			
			for(Edge e : temp){
				String nodeA = getNodeName(e.getVertexA());
				String nodeB = getNodeName(e.getVertexB());
				int weight = e.getWeight();

				//System.out.println("nodeA "+nodeA);
				//System.out.println(getLastNode(mst)[0]);
				
				if(nodeB.equals(getLastNode(mst)[0]) && !nodeA.equals(getLastNode(mst)[1])){
					//System.out.println(nodeB+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeA+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeA;
					totaledges = removeEdge(totaledges,e);
				}
				
				if(nodeA.equals(getLastNode(mst)[0]) && !nodeB.equals(getLastNode(mst)[1])){
					//System.out.println(nodeA+" equals "+getLastNode(mst)[0]);
					//System.out.println(nodeB+" !equals "+getLastNode(mst)[1]);

					mst+="--"+weight+"--"+nodeB;
					totaledges = removeEdge(totaledges,e);
				}
			}

			if(!mst.contains("\n"+node_id))
			mst = mst+"\n"+node_id;

			return recursiveMST(totaledges,mst,timeout-1);
		}
	}
	
	
	public TreeSet<Edge> removeEdge(TreeSet<Edge> edges, Edge e){
		TreeSet<Edge> temp = new TreeSet<Edge>();
		
		System.out.println("");
		for(Edge t : edges){
			if(!e.toString().equals(t.toString())){
				System.out.println(t.toString());
				temp.add(t);
			}
			else{
				System.out.println(t+" was removed");
			}
		}
		
		return temp;
		
	}



	public String[] getLastNode(String tree){
		String name = "";
		String weight="";
		String prev = "";

		Scanner scan = new Scanner(tree);
		scan.useDelimiter("--");

		while(scan.hasNext()){
			prev = weight;
			weight = name;
			name = scan.next();
		}

		scan.close();
		
		if(name.contains("\n")){
			Scanner newline = new Scanner(name);
			newline.useDelimiter("\n");
			
			newline.next();
			name = newline.next();
			prev = "";
			newline.close();
		}

		String[] arr = new String[2];
		arr[0] = name;
		arr[1] = prev;
		return arr;
	}



	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public void shutdown() {
		try{
			regsc = SocketChannel.open();
			regsc.configureBlocking(false);

			regsc.connect(new InetSocketAddress(registry_host, registry_port));	
			tcpc = new TCPConnection(regsc);

			tcpc.write(DEREGISTER_REQUEST+";"+node_id+";"+my_address+";"+tcp_port+";"+udp_port);
			Thread.sleep(10); // So that it has time to read otherwise there is no message.


			byte[] b = tcpc.read();

			String message = byteArrayToString(b);
			Scanner mesScan = new Scanner(message);
			mesScan.useDelimiter(";");

			mesScan.nextInt();
			String res = mesScan.next();

			System.out.println("Deregistration: "+res);

			mesScan.close();
			exitNicely();

		} catch (IOException | InterruptedException e){
			e.printStackTrace();
		}
	}

	/**
	 * justifyConnections
	 * 
	 * this method allows the nodes that are already registered to ping those that don't know that they have
	 * edges with. This allows for us to create a full undirected graph of the data making it easier to 
	 * implement the CDN and MST
	 */
	public synchronized void justifyConnections(){
		ConnectionFactory cf = new ConnectionFactory(cdnConnectionType);

		for(int i=0;i<nodelist.size();i++){
			String node = nodelist.get(i);
			Scanner scan = new Scanner(node);
			scan.useDelimiter(";");

			scan.next();
			String nodeAddress = scan.next();

			int port = 0;
			if(cdnConnectionType.equals("tcp"))
				port = scan.nextInt();
			else if(cdnConnectionType.equals("udp")){
				scan.next(); port=scan.nextInt();
			}

			Channel ch = cf.getConnection(nodeAddress, port);
			Connection c = cf.getConnector(ch);

			try {
				if(cdnConnectionType.equals("tcp")){
					TCPConnection tcpCon = (TCPConnection)c;
					tcpCon.write(NODE_TO_NODE_JUSTIFY+";"+node_id+";"+my_address+";"+tcp_port+";"+udp_port);
				}
				else if(cdnConnectionType.equals("udp")){
					UDPConnection udpCon = (UDPConnection)c;
					udpCon.write("nothing");
				}


				ch.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			scan.close();
		}

	}

	public void setPort(int port){
		try {
			scalableServer = ServerSocketChannel.open();
			scalableServer.socket().bind(new InetSocketAddress(port));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * sccalable()
	 * 
	 * this method allows us to go into part 2 mode of the assignment to see if our MessengerNodes are capable of handling 
	 * hundreds of connections.
	 */
	@SuppressWarnings("static-access")
	public void scalable(){
		try {

			tPool = new ThreadPool(thread_pool_size);

			setPort(0);
			System.out.println("Node port set to "+scalableServer.socket().getLocalPort()+" on machine "+scalableServer.socket().getInetAddress().getLocalHost().getHostAddress());


			Selector select = Selector.open();

			//set up ServerSocketChannel

			scalableServer.configureBlocking(false);
			scalableServer.register(select , SelectionKey.OP_ACCEPT);

			Set<SelectionKey> keys = select.selectedKeys( );

			System.out.println("Waiting for Connection...");
			//look for accept notifications
			while(true){
				select.select(); //find something that's ready

				for(Iterator<SelectionKey> i = keys.iterator( ); i.hasNext( ); ) {

					// Get a key from the set, and remove it from the set
					SelectionKey key = (SelectionKey)i.next( );
					i.remove( );
					//housekeeping...
					if (key.isAcceptable()) {
						//accept the connection
						SocketChannel client = scalableServer.accept();
						client.configureBlocking (false);
						//client.register(select,SelectionKey.OP_READ);
						TCPConnection tcp = new TCPConnection(client);

						ReadOrWriteTask row = new ReadOrWriteTask(tcp);

						tPool.runTask(row);
					} /*account for other cases*/ 
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Start helpers

	private void printArrayList(ArrayList<String> e){
		System.out.println("ArrayList");
		for(int i =0;i<e.size();i++){
			System.out.println("\t"+e.get(i));
		}
	}
	private String removeMinEdge(ArrayList<String> edges){
		String minEdge = " \t \t11";

		for(int i = 0;i<edges.size();i++){
			int a = getWeight(minEdge);
			int b = getWeight(edges.get(i));


			if(b<a){
				minEdge=edges.get(i);
			}
		}

		edges.remove(minEdge);
		return minEdge;
	}
	private String getNodeName(String node){
		Scanner scan = new Scanner(node);
		scan.useDelimiter(";");
		String a = scan.next();
		scan.close();
		return a;
	}
	private String getNodeA(String edge){
		Scanner scan = new Scanner(edge);
		scan.useDelimiter("\t");
		String a = scan.next();
		scan.close();
		return a;
	}
	private String getNodeB(String edge){
		Scanner scan = new Scanner(edge);
		scan.useDelimiter("\t");
		scan.next();
		String b = scan.next();
		scan.close();
		return b;
	}
	private int getWeight(String edge){
		Scanner scan = new Scanner(edge);
		scan.useDelimiter("\t");
		scan.next();
		scan.next();

		int c = scan.nextInt();

		scan.close();
		return c;
	}
	public String getAddressFromNode(String node){
		Scanner scan = new Scanner(node);
		scan.useDelimiter(";");
		scan.next();
		String address = scan.next();
		scan.close();
		return address;
	}
	public int getPortFromNode(String node){
		Scanner scan = new Scanner(node);
		scan.useDelimiter(";");
		scan.next();
		scan.next();

		int port = 0;

		if(cdnConnectionType.equals("tcp")){
			port = scan.nextInt();
		}
		else if(cdnConnectionType.equals("udp")){
			scan.nextInt();
			port = scan.nextInt();
		}
		scan.close();
		return port;
	}
	public void exitNicely() throws IOException {
		regsc.close();
		tcpserver.close();
		udpserver.close();
		System.exit(0);
	}


	// End helpers

	public static void main(String args[])
	{
		if(args.length==4){
			MessagingNode node = new MessagingNode(args[0],args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]));
			ConsoleListener terminal = new ConsoleListener(node);

			Thread t1 = new Thread(terminal);
			t1.start();

			node.regConnect();

			//Thread t3 = new Thread(node.listenudp);
			//t3.start();
		}
		else{
			System.out.println("Usage: java MessagingNode assigned_id registry-host registry-port threadpool_size");
		}
	}




}

