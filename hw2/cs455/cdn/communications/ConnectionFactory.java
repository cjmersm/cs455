package cs455.cdn.communications;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

public class ConnectionFactory {
	
	private String connectionType;
	
	public ConnectionFactory(String connectionType){
		this.connectionType = connectionType;
	}
		
	public Channel getConnection(String address, int port) {
		try{
	      // Create and bind a tcp channel to listen for connections on.
	      if(connectionType.equals("tcp")){
	    	  SocketChannel sc = SocketChannel.open();
	    	  sc.configureBlocking(false);
	    	  sc.connect(new InetSocketAddress(address,port));
	    	  return sc;
	      }
	      
	      else if(connectionType.equals("udp")){
	    	  DatagramChannel dc = DatagramChannel.open((ProtocolFamily) new InetSocketAddress(address,port));
	    	  // TODO!!!
	    	  return dc;
	      }
	      
	      else
	    	  return null;
	
		} catch (IOException e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	public Connection getConnector(Channel c){
		if(connectionType.equals("tcp")){
			return new TCPConnection((SocketChannel)c);
		}
		else if(connectionType.equals("udp")){
			return new UDPConnection((DatagramChannel)c);			
		}
		else
			return null;
	}
}

