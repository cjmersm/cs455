package cs455.cdn.util;

import java.util.Scanner;

public class MessageParser {
	
	public String[] parseRegistryRequest(String message){
		String[] s = new String[4];
		
		Scanner scan = new Scanner(message);
		scan.useDelimiter(";");
		
		for(int i = 0;scan.hasNext();i++){
			s[i] = scan.next();
		}
		
		return s;
	}
	

}
