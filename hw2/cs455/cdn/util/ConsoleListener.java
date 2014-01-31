/**
 *  ConsoleListener
 *  
 *  This class is used for the commands implemented while running
 *  the processes. It will be its own thread. Each node will have one
 *  and depending on the flag it has, it will have appropriate 
 *  commands available.
 */


package cs455.cdn.util;

import java.util.Scanner;

import cs455.cdn.node.MessagingNode;
import cs455.cdn.node.Node;
import cs455.cdn.node.Registry;

public class ConsoleListener implements Runnable {

	char flag;
	Node n;
	Thread t ;

	public ConsoleListener(Node n) {
		this.flag = n.type;  // r for registry and n for node
		this.n=n;
	}

	public void run(){
		
		String command="";
		Scanner in = new Scanner(System.in);
		
		while(true){
			
			
			command = in.next();
			command = command.toLowerCase();
			

			if(command.equals("setup-cdn") && flag=='r'){
				
				int numOfConnections = in.nextInt();
				String connectType	= in.next();
				
				
				Registry reg = (Registry)n;
				
				int size = n.nodelist.size();

				if(size<=1){
					System.out.println("You need more than 1 node already registered.");
				}
				else if(size<=numOfConnections){
					System.out.println("Your number of connections need to be less than the number of nodes in cdn.");
				}
				else if(!connectType.equals("tcp") && !connectType.equals("udp")){
					System.out.println("Unknown Protocal Specified: "+connectType);
				}
				else{
					
					Registry r = (Registry)n;
					
					
					reg.setupcdn(numOfConnections,connectType);
					Thread t3 = new Thread(r.updateWeight);
					t3.start();
				}
			}

		
			else if((command.equals("print-MST") || command.equals("print")) && flag=='n'){
				MessagingNode m = (MessagingNode)n;
				m.printMST();
			}

	
			else if((command.equals("send-data") || command.equals("send")) && flag=='n'){
				MessagingNode m = (MessagingNode)n;
				m.initializeSending();
				m.sendData(in.next()); // filename
			}
			
			else if(command.equals("set") && flag=='n'){
				MessagingNode m = (MessagingNode)n;
				//String addr = in.next();
				int port = in.nextInt();
				m.setPort(port);
			}

			else if(command.equals("list-nodes")||command.equals("list")){						
				
				if(n.nodelist == null){
					System.out.println("There are no Nodes.");
				}
				else if(n.nodelist.isEmpty()){
					System.out.println("There are no Nodes.");
				}
				else{
				System.out.println("Node List: ");
					for(String s : n.nodelist){
						System.out.println("\t"+s);
					}
				}
				
			}
			
			
			else if((command.equals("exit-cdn")||command.equals("exit"))&&flag=='n'){
				System.out.println("Exiting the CDN");
				in.close();
				n.shutdown();
			}
			
					
			else if(command.equals("shutdown")){
				if(flag=='r'){
					if(!n.nodelist.isEmpty()){
						System.out.println("Can't shutdown, these nodes are still in the system: ");
						for(int i = 0;i<n.nodelist.size();i++){
							System.out.println("\t"+n.nodelist.get(i));
						}
					}
					else{
						in.close();
						n.shutdown();
					}
				}
				
				if(flag=='n'){
					System.out.println("Shutting Down");
					in.close();
					n.shutdown();
				}
			}
			
			else if(command.equals("help")){
				if(flag=='r'){
					System.out.println("Commands:");
					System.out.println("\tsetup-cdn <numOfCon> <ri>.... sets up cdn");
					System.out.println("\tlist-nodes .................. list nodes registered");
					System.out.println("\tshutdown .................... terminate the register");
					
				}
				else if(flag=='n'){
					System.out.println("Commands:");
					System.out.println("\tlist-nodes ............ list nodes in list");
					System.out.println("\tprint-MST ............. prints the minimum spanning tree of cdn");
					System.out.println("\tsend-data <filename> .. sends all data of filename to nodes using MST");
					System.out.println("\texit-cdn .............. deregester and quit");
					System.out.println("\tshutdown .............. deregester and quit");
				}
				
			}


			else{
				System.out.println("Command not recognized.");
			}


		}
		
		
	}
	//};
}
