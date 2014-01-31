package cs455.hdfs;

import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

public class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text>
{
	public void reduce(Text key, Iterator<Text> values, OutputCollector<Text, Text> output, Reporter reporter) throws IOException
	{
		int accurance = 0;
		int sentiment= 0;
		
		
		while (values.hasNext())
		{
			String word = values.next().toString();
			Scanner scan = new Scanner(word);
			scan.useDelimiter("\t");
			
			sentiment += scan.nextInt();
			accurance += scan.nextInt();
			
			scan.close();
		}
		
		Text t = new Text(sentiment+"\t"+accurance);
		output.collect(key, t);
	}
}