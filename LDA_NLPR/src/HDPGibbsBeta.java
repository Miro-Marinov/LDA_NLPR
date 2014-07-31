import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;


import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class HDPGibbsBeta {
    
	private static boolean dbg = false;
	//private  double alpha = 0.01; // alpha is now a concentration parameter
	//private  double beta = 1.; // beta - parameter(s) for the words distributions in topics Dir prior
	
	String targetWord;
	Random randGen;
	
	private double beta  = 0.5; // default only
	private double gamma = 1.5;
	private double alpha = 1.0;
	private List<Double> p = new ArrayList<>(); //private double[] p;
	private List<Double> f = new ArrayList<>(); //private double[] f;
	
	List<Document> documents; //protected DOCState[] docStates;
	List<Cluster> topics;
	private HashSet<String> globalWordSet;
	
	IdHandler topicIDHandler = new IdHandler();
	IdHandler tableIDHandler = new IdHandler();
	
	protected int totalNumberOfWords;   // private  Integer N; // total number of words across all documents
	protected int sizeOfVocabulary; //private  Integer B; // total number of unique words across all documents
	protected int numberOfInitialTopics = 1; //public static final Integer K = 25; // max (initial) number of topics
	protected int totalNumberOfTables;
	private HashMap<Cluster, Integer> numberOfTablesByTopic = new HashMap<>(); 
	
	public HDPGibbsBeta(File file) {
		System.out.println("\n\nPerforming LDA on the specified documents..");

		totalNumberOfWords = 0;
		totalNumberOfTables = 0;
		topics = new ArrayList<>(); //global topics
		int k, j;
		
		//textProcessor = new TextProcessor();
		globalWordSet = new HashSet<>();
		documents = new ArrayList<>();

		randGen = new Random();
		Integer documentID = 0;
		
		System.out.println("Initial random topic assignment..");
		// initialize the topics
		for(k = 0 ; k < numberOfInitialTopics ; k++) {
			topics.add(new Cluster(topicIDHandler.getID()));
		}	
		
		try {	
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brdocuments = new BufferedReader(new FileReader(file));
			if(dbg)System.out.println("\n\nRarget word is " + targetWord);
			String documentString;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			
			// parse input and generate the documents
			while((documentString = brdocuments.readLine()) != null) {
				String[] wordStrings = tokenizer.tokenize(documentString);
				Document newdocument = new Document(documentID++);	
				newdocument.tables.add(new Cluster(tableIDHandler.getID()));
				totalNumberOfTables++;
				documents.add(newdocument);
				globalWordSet.addAll(Arrays.asList(wordStrings));
				// generate word objects for this document
				for(String wordString : wordStrings) {
					if(!wordString.equals(targetWord)) {
						Word word = new Word(wordString, newdocument, null);
						newdocument.words.add(word);
						totalNumberOfWords ++;
					}
				}
			}
			
			
			// all topics have now one document
			for (k = 0 ; k < topics.size() ; k++) {
				Document doc = documents.get(k);
				Cluster topic = topics.get(k);
				Cluster table = doc.tables.get(0);
				doc.tableToTopic.put(table, topic);
				
				Integer numberOfTablesByThisTopic  = numberOfTablesByTopic.get(topic); 
				if(numberOfTablesByThisTopic == null) 
					numberOfTablesByTopic.put(topic, 1);
				else 
					numberOfTablesByTopic.put(topic, ++numberOfTablesByThisTopic);
				
				for (Word word : doc.words) {
					putWord(word, table, topic);
				}
			}
			
			// the words in the remaining documents are now assigned too
			for (j = topics.size(); j < documents.size(); j++) {
				Document doc = documents.get(j);
				k = randGen.nextInt(topics.size());
				Cluster topic = topics.get(k);
				Cluster table = doc.tables.get(0);
				doc.tableToTopic.put(table, topic);
				
				Integer numberOfTablesByThisTopic  = numberOfTablesByTopic.get(topic); 
				if(numberOfTablesByThisTopic == null) 
					numberOfTablesByTopic.put(topic, 1);
				else 
					numberOfTablesByTopic.put(topic, ++numberOfTablesByThisTopic);
				
				for (Word word : documents.get(j).words) {
					putWord(word, table, topic);
				}
			}
		brdocuments.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		printTopics();
		printDocuments();
		System.out.println("\nTotal number of words initially topiced: " + totalNumberOfWords);
		System.out.println("Size of vocalubary: " + globalWordSet.size());
		System.out.println("Number of topics: " + topics.size());
		System.out.println("Number of tables: " + totalNumberOfTables + "\n");
		this.sizeOfVocabulary = globalWordSet.size();
	}
	
	class Cluster {
		// words in the topic
		HashSet<Word> words ;
		
		// counts for each wordString in the cluster
		HashMap<String, Integer> wordCounts ;
		Integer id;
		
		public Cluster(Integer id) {
			this.id = id;
			words = new HashSet<>();
			wordCounts = new HashMap<>();
		}

		// add word to the topic
		public void add(Word word){
			String wordString = word.wordString;
			Integer wordCount = wordCounts.get(wordString);

			if(wordCount == null)
				wordCounts.put(wordString, 1);	
			else wordCounts.put(wordString, wordCount + 1);	

			words.add(word);
		}
		
		// remove word from the topic
		public void remove(Word word){
			String wordString = word.wordString;
			words.remove(word);
			Integer wordCount = wordCounts.get(wordString);
			if(wordCount != null && wordCount != 0)
				wordCounts.put(wordString, wordCount - 1);	
		}

		public Integer size() {
			return words.size();
		}
	}
	
		class Document {
			int id;
			List<Cluster> tables;
			HashMap<Cluster, Cluster> tableToTopic;
			List<Word> words;
			
			public Document(int id) {
				this.id = id;
			    words = new ArrayList<>();	
			    tableToTopic = new HashMap<>();
			    tables = new ArrayList<>();
			}
		}
	
		 class Word { //word object 
			final String wordString; //string representation (can be a stem, PoS etc..)
			final Document document; // from which document (document)
			Cluster table; // Table assignment
			
			public Word(String wordString, Document document, Cluster table) {
				this.document = document;
				this.wordString = wordString;
				this.table = table;
			}
			
			@Override
			public String toString() {
				assert(table != null);
				assert(document.tableToTopic.get(table) != null);
				assert(document != null);
				int topicID, tableID;
				if(table != null) {
					topicID = document.tableToTopic.get(table).id;
					tableID = table.id;
				}
				else {
					topicID = -1;
					tableID = -1;
				}
				
				return wordString + "(d:" + document.id + " tbl:" + tableID + " tpc:" + topicID + ")";
			}
		 }
		
		public void run(int maxIter, int shuffleLag) {
			for (int curIter = 0; curIter < maxIter; curIter++) {
				System.out.println("Before: iter = " + curIter + " #topics = " + topics.size() + ", #tables = "
						+ totalNumberOfTables );
				if ((shuffleLag > 0) && (curIter > 0) && (curIter % shuffleLag == 0)) {
					if(dbg) System.out.println("Everyday Im shuffelin'"); 
					doShuffle();
					}
				nextGibbsIter();
				if(dbg) System.out.println("After: iter = " + curIter + " #topics = " + topics.size() + ", #tables = "
						+ totalNumberOfTables );
			}
		}
		
		/**
		 * Step one step ahead
		 * 
		 */
		protected void nextGibbsIter() {
			int tableIndex, topicIndex;
			for (Document d : documents) {
				for (Word w : d.words) {
					removeWord(w); // remove the word i from the current state
					
					tableIndex = sampleTable(w); // sampling Table
					if (tableIndex == d.tables.size()) { // new Table
						if(dbg)System.out.println("Creating new table!");
						Cluster table = new Cluster(tableIDHandler.getID());
						
						topicIndex = sampleTopic(); // sampling its Topic
						
						Cluster topic;
						if(topicIndex == topics.size()) { // new Topic
							if(dbg)System.out.println("Creating new topic!");
							topic = new Cluster(topicIDHandler.getID());
							topics.add(topic);
							putWord(w, table, topic);  
						}
						else { // existing Topic
							topic = topics.get(topicIndex);
							if(dbg)System.out.println("New table, existing topic which is: " + topic.id);
							putWord(w, table, topic);
						}
						
						d.tables.add(table);
						d.tableToTopic.put(table, topic);
						totalNumberOfTables++;
						Integer numberOfTablesByThisTopic = numberOfTablesByTopic.get(topic); 
						if(numberOfTablesByThisTopic == null) 
							numberOfTablesByTopic.put(topic, 1);
						else 
							numberOfTablesByTopic.put(topic, ++numberOfTablesByThisTopic);
						
					}
					else { // existing Table
						Cluster table = d.tables.get(tableIndex);
						if(dbg)System.out.println("Existing table:" + table.id + " with topic: " + d.tableToTopic.get(table).id);
						putWord(w, table, d.tableToTopic.get(table)); 
					}
				}
			}
		}
		
		
		int sampleTable(Word word) {	
			if(dbg)System.out.println("Sampling table for word: " + word);
			int k, j;
			double pSum = 0.0, vb = sizeOfVocabulary * beta, fNew, r;
			Document doc = word.document;
			fNew = gamma / sizeOfVocabulary;
			if(dbg)System.out.println("fNew is:" + fNew);
			f.clear();
			p.clear();
			for (k = 0; k < topics.size(); k++) {
			    Cluster topic = topics.get(k);
				Integer timesWordWasUsedInTopic = topic.wordCounts.get(word.wordString);
				if(timesWordWasUsedInTopic == null) timesWordWasUsedInTopic = 0;
				
				if(dbg)System.out.println("Times word was used in topic " + k + " is: " + timesWordWasUsedInTopic);
			    Double term = (timesWordWasUsedInTopic + beta) / 
			    			  (topic.words.size() + vb);
			    if(dbg)System.out.println("Term is:" + term);
				f.add(term);
				fNew += numberOfTablesByTopic.get(topic) * term;
				if(dbg)System.out.println("#tables for topic" + topics.get(k).id + ": " + numberOfTablesByTopic.get(topic));
				if(dbg)System.out.println("Adding to fNew for topic:" + topics.get(k).id + ": " + numberOfTablesByTopic.get(topic) * term);
				if(dbg)System.out.println("fNew is:" + fNew);
			}
			Integer numberOfTables = doc.tables.size();
			for (j = 0; j < numberOfTables; j++) {
				Cluster table = doc.tables.get(j);
				assert (table.words.size() > 0);
				if (table.words.size() > 0)  {
					Cluster topic = doc.tableToTopic.get(table);
					
					pSum += table.words.size() * f.get(topics.indexOf(topic));
					if(dbg)System.out.println("Adding to pSum for table:" + doc.tables.get(j).id + ": " + table.words.size() * f.get(topics.indexOf(topic)));
					if(dbg)System.out.println("pSum is:" + pSum);
				}
				else {
					if(dbg)System.out.println("Adding to pSum for table:" + 0);
					if(dbg)System.out.println("pSum is:" + pSum);
				}
				p.add(pSum);
			}
			pSum += alpha * fNew / (totalNumberOfTables + gamma); // Probability for t = tNew
			
			if(dbg)System.out.println("Adding to pSum for new table:" + alpha * fNew / (totalNumberOfTables + gamma));
			if(dbg)System.out.println("pSum is:" + pSum);
			p.add(pSum);
			r = randGen.nextDouble() * pSum;
			
			if(dbg)System.out.println("Draw random number:" + r);
			for (j = 0; j <= numberOfTables; j++)
				if (r < p.get(j)) 
					break;	// decided which table the word i is assigned to
			if(dbg)System.out.println("p: " + p);
			if(dbg)System.out.println("f: " + f);
			if(dbg)System.out.println("Draw random tableIndex:" + j);
			return j;
		}
		
		
		private int sampleTopic() {
			if(dbg)System.out.println("Sampling topic");
			double r, pSum = 0.0;
			int k;
			p.clear();
			for (k = 0 ; k < topics.size() ; k++) {
				Cluster topic = topics.get(k);
				pSum += numberOfTablesByTopic.get(topic) * f.get(k);
				if(dbg)System.out.println("Adding to pSum for topic:" + topics.get(k).id + ": " + numberOfTablesByTopic.get(topic) * f.get(k));
				if(dbg)System.out.println("pSum is:" + pSum);
				p.add(pSum);
			}
			// new Cluster
			pSum += gamma / sizeOfVocabulary;
			if(dbg)System.out.println("Adding to pSum for new topic:" + (gamma / sizeOfVocabulary));
			if(dbg)System.out.println("pSum is:" + pSum);
			p.add(pSum) ;
			r = randGen.nextDouble() * pSum;
			if(dbg)System.out.println("Draw random number:" + r);
			for (k = 0; k <= topics.size(); k++)
				if (r < p.get(k))
					break;
			if(dbg)System.out.println("p: " + p);
			if(dbg)System.out.println("f: " + f);
			if(dbg)System.out.println("Draw random topicIndex:" + k);
			return k;
		}
		
		
		protected void putWord(Word word, Cluster table, Cluster topic) {
			assert(word.table == null);
			if(dbg)System.out.println("Removing word: " + word  + " to table: " + table.id + " assigned to topic: " + topic.id);
			word.table = table;
			table.add(word);
			topic.add(word);
		}
		
		/**
		 * Removes a word from the bookkeeping
		 */
		private void removeWord (Word word) {
			if(dbg)System.out.println("Removing word: " + word);
			Document document = word.document;
			Cluster table = word.table;
			Cluster topic = document.tableToTopic.get(table);
			assert(table != null);
			assert(topic != null);
			if(dbg)System.out.println("Removing word from table: " + table.id);
			if(dbg)System.out.println("Removing word from topic: " + topic.id);
			
			if(dbg)System.out.println("Before removal: " + table.words);
			if(dbg)System.out.println("Before remova: " + topic.words);
			
			table.remove(word); //docState.wordCountByTable[table]--; 
			topic.remove(word); //wordCountByTopic[k]--; 	
			
			if(dbg)System.out.println("After removal: " + table.words);
			if(dbg)System.out.println("After remova: " + topic.words);
			// remove table from the table collection if needed
			if(table.size() <= 0) {
				if(dbg)System.out.println("Table is size is 0 => delete table");
				tableIDHandler.freeID(table.id);
				document.tables.remove(table);
				totalNumberOfTables--;
				Integer numberOfTablesByThisTopic = numberOfTablesByTopic.get(topic); 
				assert(numberOfTablesByThisTopic != null);
				numberOfTablesByTopic.put(topic, numberOfTablesByThisTopic - 1);
				document.tableToTopic.remove(table);
				word.table = null;
				if(dbg)printDocuments();
				
			}
			// remove table from the topic collection if needed
			if(topic.size() <= 0) {
				if(dbg)System.out.println("Cluster is size is 0 => delete topic");
				topicIDHandler.freeID(topic.id);
				topics.remove(topic);
				Integer numberOfTablesByThisTopic = numberOfTablesByTopic.get(topic); 
				assert(numberOfTablesByThisTopic == 0);
				numberOfTablesByTopic.remove(topic);
				if(dbg)printTopics();
			}
		}
		
		
	/**
	 * Permute the ordering of documents and words in the bookkeeping
	 */
	protected void doShuffle(){
		if(dbg)System.out.println("Before suffeling:");
		if(dbg)printDocuments();
		Collections.shuffle(documents);
		for (Document d : documents){
			Collections.shuffle(d.words);
		}
		if(dbg)System.out.println("After suffeling:");
		if(dbg)printDocuments();
	}	
	
	private void printTopics() {
		int count = 0;
		System.out.println("\n\nTopics outlook:");
		for(Cluster topic : topics) {
			count += topic.words.size();
			System.out.println(topic.words.toString());
		}
		System.out.println("Words in all the topics: " + count);
		System.out.println("Number of topics: " + topics.size() + "\n\n");
	}
	
	private void printDocuments() {
		int count = 0;
		System.out.println("Documents outlook:");
		for(Document document : documents) {
			count += document.words.size();
			System.out.println("\nDocument:");
			System.out.println(document.words.toString());
			System.out.println("DocTables(" + document.tables.size() + ")");
			for(Cluster table : document.tables)
				System.out.println(table.words);
		}
		System.out.println("Words in all the documents: " + count);
	}
	
	
	// perform HDP on the collection of documents
	public static void main(String[] args) {
		HDPGibbsBeta hdp = new HDPGibbsBeta(new File("testHDP/test.txt"));
		hdp.run(5, 2);
		
		/*
		for(Document document : hdp.documents) {
			document.printProbsBesttopic();
		}
		*/
	}	
}