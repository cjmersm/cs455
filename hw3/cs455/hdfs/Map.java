package cs455.hdfs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import cmu.arktweetnlp.Tagger;
import cmu.arktweetnlp.Tagger.TaggedToken;

public class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text>
{
	private IntWritable sent = new IntWritable();
	private Text word = new Text();

	private Tagger tagger;

	private HashMap <String,String> posVerb;
	private HashMap <String,String> negVerb;
	private HashMap <String,String> posAdj;
	private HashMap <String,String> negAdj;
	private HashMap <String,String> posEmo;
	private HashMap <String,String> negEmo;
	private HashMap <String,String> posExc;
	private HashMap <String,String> negExc;


	public void configure(JobConf job) {

		String DICTPATH = "hdfs://squash:55555/user/mersman/hw3/dicts/";
		
		tagger = new Tagger();
		try {

			tagger.loadModel("/cmu/arktweetnlp/model.20120919");

			posVerb = diction(DICTPATH+"positiveVerbs");
			negVerb = diction(DICTPATH+"negativeVerbs");
			posAdj  = diction(DICTPATH+"positiveStopwords");
			negAdj  = diction(DICTPATH+"negativeStopwords");
			posEmo  = diction(DICTPATH+"positiveEmotes");
			negEmo  = diction(DICTPATH+"negativeEmotes");
			posExc  = diction(DICTPATH+"positiveExclaim");
			negExc  = diction(DICTPATH+"negativeExclaim");

		} catch (IOException e){
			e.printStackTrace();
		}
	}

	public void map(LongWritable key, Text value, OutputCollector<Text, Text> output, Reporter reporter) throws IOException
	{
		String line = value.toString();
		ArrayList<String> nouns = new ArrayList<String>();
		String tweet = ParseTweetData(line);
		
		int sentiment = POSAssigner(tweet, nouns);
		sent.set(sentiment);
		
		for(String noun : nouns)
		{
			if(noun.equals("dog") || noun.equals("kitten") || noun.equals("puppy") || noun.equals("cat") ||
					noun.equals("rabbit") || noun.equals("bunny") || noun.equals("fish")){
				word.set(noun);
				Text t = new Text(sentiment+"\t"+1); // 1 for word count
				output.collect(word, t);
			}
		}
	}

	/** ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| **/

	public int POSAssigner(String tweet, ArrayList<String> nouns) throws IOException{

		if(tweet.equals("") || tweet.equals(" ")){
			return 0;
		}
		List<TaggedToken> taggedTokens = tagger.tokenizeAndTag(tweet);
		int sentiment = 0;

		
		for (TaggedToken token : taggedTokens) {
			String POS = token.tag;
			String word= token.token;


			if(POS.equals("N")){
				nouns.add(word.toLowerCase());
			}
			else if(POS.equals("!")){ // Exclaim
				sentiment += AssignExclaimSentiment(token.token);
			}
			else if(POS.equals("A")){ // Adj
				sentiment += AssignAdjSentiment(token.token);
			}
			else if(POS.equals("V")){ // Verb
				sentiment += AssignVerbSentiment(token.token);
			}
			else if(POS.equals("E")){ // Emo
				sentiment += AssignEmotionSentiment(token.token);
			}
		}

		//		for (String noun : nouns){
		//			System.out.printf("%s\t%s\n", sentiment , noun);
		//		}

		return sentiment;
	}

	public String ParseTweetData(String data){
		Scanner lineScan = new Scanner(data);
		lineScan.useDelimiter("\"");
		String tweet = "";

		while(lineScan.hasNext()){
			if(lineScan.next().equals("text") && lineScan.next().equals(":")){
				String next = lineScan.next();
				while(!next.equals(",")){
					tweet+=next+" ";
					next = lineScan.next();
				}
				tweet = tweet.replace("\\ ", "\"");
				tweet = tweet.replace("\\/", "\\");


				if(tweet.contains("RT @")){
					tweet = "";
				}

				Scanner tweetScan = new Scanner(tweet);


				tweetScan.close();
			}
		}
		lineScan.close();

		System.out.print(tweet);
		System.out.println();

		return tweet;
	}

	/** ----------------------------------------------------------------------------------- **/
	/** ----------------------------------------------------------------------------------- **/
	/** ----------------------------------------------------------------------------------- **/

	public int AssignVerbSentiment(String verb) throws IOException{ 

		if(posVerb.containsValue(verb)) 
			return  1;
		else if(negVerb.containsValue(verb)) 
			return -1;
		else 
			return 0;
	}

	public int AssignAdjSentiment(String adj) throws IOException{

		if(posAdj.containsValue(adj)) 
			return  1;
		else if(negAdj.containsValue(adj)) 
			return -1;
		else 
			return 0;
	}

	public int AssignExclaimSentiment(String x) throws IOException{

		if(posExc.containsValue(x)) 
			return  1;
		else if(negExc.containsValue(x)) 
			return -1;
		else 
			return 0;
	}

	public int AssignEmotionSentiment(String emo) throws IOException{

		if(posEmo.containsValue(emo)) 
			return  1;
		else if(negEmo.containsValue(emo)) 
			return -1;
		else 
			return 0;
	}


	public HashMap<String,String> diction(String path) throws IOException{
		HashMap<String,String> words = new HashMap<String, String>();

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(conf);
		Path dict = new Path(path);

		FSDataInputStream in = fs.open(dict);
		Scanner fileScan = new Scanner(in);

		while(fileScan.hasNext()){
			String word = fileScan.next();
			words.put(word, word);
		}

		fileScan.close();

		return words;
	}


	/** ----------------------------------------------------------------------------------- **/
	/** ----------------------------------------------------------------------------------- **/
	/** ----------------------------------------------------------------------------------- 
	 * @throws FileNotFoundException **/

	@SuppressWarnings("resource")
	public static void main(String args[]) throws FileNotFoundException{
		Map m = new Map();
		File f = new File("tweet50.txt");
		Scanner s = new Scanner(f);
		
		while(s.hasNext()){
			System.out.println(m.ParseTweetData(s.nextLine()));
		}
		
		s.close();

	}

}

