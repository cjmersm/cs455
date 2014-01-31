package cs455.cdn.node;

import java.nio.channels.Selector;
import java.util.ArrayList;

public abstract class Node {
	public final int REGISTRY_REQUEST = 1;
	public final int REGISTRY_RESPONCE = 2;
	public final int PEER_NODE_LIST_MESSAGE = 3;
	public final int NODE_TO_NODE_JUSTIFY = 4;
	public final int WEIGHT_UPDATE = 5;
	public final int DATA_PACKET = 6;
	public final int DEREGISTER_REQUEST = 7;
	public final int DEREGISTER_RESPONSE = 8;
	
	
	protected Selector selector;
	public char type;
	public ArrayList<String> nodelist;
	
	public abstract void shutdown();
	
	public int byteArrayToInt(byte[] b) { // byte[] to int
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	public byte[] intToByteArray(int a) { // int to byte[]
		byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);
		ret[2] = (byte) ((a >> 8) & 0xFF);
		ret[1] = (byte) ((a >> 16) & 0xFF);
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}
	
	public String byteArrayToString(byte[] b){ // byte[] to string
		String str = new String(b);
		return str;
	}
	
	public byte[] stringToByteArray(String str){ // string to byte[]
		return str.getBytes();
	}

	public byte[] arrConcat(byte[] a, byte[] b) {  // putting bytes together
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}
	
	public byte[] arrConcat(byte a,byte[] b){	// putting a byte on front of byte arr
		byte[] c = new byte[1+b.length];
		c[0]=a;
		System.arraycopy(b, 0, c, 1, b.length);
		
		return c;
	}
	
	public byte[] splitByteArr(byte[] b, int from, int to){  // splitting byte arrs
		byte[] a = new byte[to-from];	
		int j = 0;
		
		for(int i=from;i<to;i++){
			a[j] = b[i];
			j++;
		}
		
		return a;
	}
	
	public void printlist(ArrayList<String> al){
		for(int i = 0; i< al.size();i++){
			System.out.println("\t"+al.get(i));
		}
	}

}

