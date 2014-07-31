import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

public class HDPGibbs {
    
	//private static boolean dbg = false;
	String targetWord;
	Random randGen;
	private double beta  = 0.5; //0.5
	private double gamma = 1.0; //topics 1.0
	private double alpha = 1.5; //table 1.5
	private List<Double> p = new ArrayList<>(); 
	private List<Double> f = new ArrayList<>(); 
	
	List<Document> documents; //protected DOCState[] docStates;
	List<Cluster> topics;
	private HashSet<String> globalWordSet;
	
	IdHandler topicIDHandler = new IdHandler();
	IdHandler tableIDHandler = new IdHandler();
	
	protected int totalNumberOfWords;   
	protected int sizeOfVocabulary; 
	protected int numberOfInitialTopics = 1; 
	protected int totalNumberOfTables;
	private HashMap<Cluster, Integer> numberOfTablesByTopic = new HashMap<>(); 
	
	public HDPGibbs() {
		System.out.println("\n\nPerforming LDA on the specified documents..");
		totalNumberOfWords = 0;
		totalNumberOfTables = 0;
		topics = new ArrayList<>(); //global topics
		//textProcessor = new TextProcessor();
		globalWordSet = new HashSet<>();
		documents = new ArrayList<>();
		randGen = new Random();
		System.out.println("Initial random topic assignment..\n\n");
		// initialize the topics
		for(int k = 0 ; k < numberOfInitialTopics ; k++) {
			topics.add(new Cluster(topicIDHandler.getID()));
		}
	}
	
	private void readData(File file, boolean test) {
		int k, j;
		Integer documentID = 0;
		documents.clear();
		if(test) System.out.println("Reading testing data");
		else System.out.println("Reading training data");
		try {	
			targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brdocuments = new BufferedReader(new FileReader(file));
			String oneLine;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			String version = "null";
			// parse input and generate the documents
			while((oneLine = brdocuments.readLine()) != null) {
				String documentString;
				Document newdocument;
				
				if(test) {
				String[] split = oneLine.split(" \\|\\| ");
				oneLine = split[1];
				version = split[0];
				newdocument = new Document(documentID++, version);
				}
				else {
					newdocument = new Document(documentID++);	
				}
				
				documentString = oneLine.split(" / ")[0];
				documentString = documentString.split(" \\| ")[1]; // BAG OF DEPENDENCIES
				String[] wordStrings = tokenizer.tokenize(documentString);
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
		//printTopics();
		//printDocuments();
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
		
		@Override
	    public String toString() {
	    	return "clr(" + id + ")";
	    }
		
		
		public Integer size() {
			return words.size();
		}
	}
	
		class Document {
			final int id;
			List<Cluster> tables;
			HashMap<Cluster, Cluster> tableToTopic;
			HashMap<Cluster, Integer> topicCountMap;
			List<Word> words;
		    final String version;
			
			public Document(int id, String version) {
				this.id = id;
				this.version = version;
			    words = new ArrayList<>();	
			    tableToTopic = new HashMap<>();
			    topicCountMap = new HashMap<>();
			    tables = new ArrayList<>();
			    
			   
			}
			
			public Document(int id) {
				this.id = id;
				version = "null";
			    words = new ArrayList<>();	
			    tableToTopic = new HashMap<>();
			    topicCountMap = new HashMap<>();
			    tables = new ArrayList<>();
			    
			   
			}
			
			@Override
		    public String toString() {
		    	return "doc(" + id + ")";
		    }
			
			public void printProbsBesttopic() {
				
				Integer normalizingConstant = 0;
				
				for(Integer value: topicCountMap.values()) {
					normalizingConstant += value;
				}
				Cluster besttopic = null;
				Double bestProb = Double.NEGATIVE_INFINITY;
				System.out.println("Document is: " + id );
				for(Cluster topic: topicCountMap.keySet()) {
					Double topicProb = (double)topicCountMap.get(topic)/(double)normalizingConstant;
					System.out.println("Topic: " + topic + " times used is:" + topicCountMap.get(topic));
					System.out.println("Topic: " + topic + " prob is:" + topicProb);
					if( topicProb > bestProb) {
						bestProb = topicProb;
						besttopic = topic;
					}
				}
				System.out.print("For document" + id + " best topic is: " + besttopic.id + " with prob: " + bestProb);
				System.out.println();
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
				return wordString + "(d:" + document + " tbl:" + tableID + " tpc:" + topicID + ")";
			}
		 }
		
		public void run(int maxIter, int shuffleLag) {
			for (int curIter = 0; curIter < maxIter; curIter++) {
				
				if ((shuffleLag > 0) && (curIter > 0) && (curIter % shuffleLag == 0))
					doShuffle();
				nextGibbsIter();
				System.out.println("iter = " + curIter + " #topics = " + topics.size() + ", #tables = "
						+ totalNumberOfTables );
			}
			//printTopics();
			//printDocuments();
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
						Cluster table = new Cluster(tableIDHandler.getID());
						
						topicIndex = sampleTopic(); // sampling its Topic
						
						Cluster topic;
						if(topicIndex == topics.size()) { // new Topic
							topic = new Cluster(topicIDHandler.getID());
							topics.add(topic); 
						}
						else { // existing Topic
							topic = topics.get(topicIndex);
						}
						putWord(w, table, topic);
						
						Integer topicCount = d.topicCountMap.get(topic);
						if(topicCount == null)
							d.topicCountMap.put(topic, 1);	
						else d.topicCountMap.put(topic, topicCount + 1);
						
						
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
						Cluster topic = d.tableToTopic.get(table);
						putWord(w, table, topic); 
						
						Integer topicCount = d.topicCountMap.get(topic);
						if(topicCount == null)
							d.topicCountMap.put(topic, 1);	
						else 
							d.topicCountMap.put(topic, topicCount + 1);
					}
				}
			}
		}
		
		
		int sampleTable(Word word) {	
			int k, j;
			double pSum = 0.0, vb = sizeOfVocabulary * beta, fNew, r;
			Document doc = word.document;
			fNew = gamma / sizeOfVocabulary;
			f.clear();
			p.clear();
			for (k = 0; k < topics.size(); k++) {
			    Cluster topic = topics.get(k);
				Integer timesWordWasUsedInTopic = topic.wordCounts.get(word.wordString);
				if(timesWordWasUsedInTopic == null) timesWordWasUsedInTopic = 0;
				
			    Double term = (timesWordWasUsedInTopic + beta) / 
			    			  (topic.words.size() + vb);
				f.add(term);
				fNew += numberOfTablesByTopic.get(topic) * term;
			}
			Integer numberOfTables = doc.tables.size();
			for (j = 0; j < numberOfTables; j++) {
				Cluster table = doc.tables.get(j);
				assert (table.words.size() > 0);
				if (table.words.size() > 0)  {
					Cluster topic = doc.tableToTopic.get(table);
					pSum += table.words.size() * f.get(topics.indexOf(topic));
				}
				p.add(pSum);
			}
			pSum += alpha * fNew / (totalNumberOfTables + gamma); // Probability for new Table
			p.add(pSum);
			r = randGen.nextDouble() * pSum;
			
			for (j = 0; j <= numberOfTables; j++)
				if (r < p.get(j)) 
					break;	// decided which table the word i is assigned to
			return j;
		}
		
		
		private int sampleTopic() {
			double r, pSum = 0.0;
			int k;
			p.clear();
			for (k = 0 ; k < topics.size() ; k++) {
				Cluster topic = topics.get(k);
				pSum += numberOfTablesByTopic.get(topic) * f.get(k);
				p.add(pSum);
			}
			// new Topic
			pSum += gamma / sizeOfVocabulary;
			p.add(pSum) ;
			r = randGen.nextDouble() * pSum;
			for (k = 0; k <= topics.size(); k++)
				if (r < p.get(k))
					break;
			return k;
		}
		
		
		protected void putWord(Word word, Cluster table, Cluster topic) {
			assert(word.table == null);
			word.table = table;
			table.add(word);
			topic.add(word);
		}
		
		/**
		 * Removes a word from the bookkeeping
		 */
		private void removeWord (Word word) {
			Document document = word.document;
			Cluster table = word.table;
			Cluster topic = document.tableToTopic.get(table);
			assert(table != null);
			assert(topic != null);
			table.remove(word); //docState.wordCountByTable[table]--; 
			topic.remove(word); //wordCountByTopic[k]--; 	
			
			Integer topicCount = document.topicCountMap.get(topic);
			if(topicCount != null && topicCount != 0) {
				document.topicCountMap.put(topic, topicCount - 1);
			if(topicCount == 1)
					document.topicCountMap.remove(topic);
			}
			
			// remove table from the table collection if needed
			if(table.size() <= 0) {
				tableIDHandler.freeID(table.id);
				document.tables.remove(table);
				totalNumberOfTables--;
				Integer numberOfTablesByThisTopic = numberOfTablesByTopic.get(topic); 
				assert(numberOfTablesByThisTopic != null);
				numberOfTablesByTopic.put(topic, numberOfTablesByThisTopic - 1);
				document.tableToTopic.remove(table);
				word.table = null;
			}
			// remove topic from the topic collection if needed
			if(topic.size() <= 0) {
				topicIDHandler.freeID(topic.id);
				topics.remove(topic);
				
				Integer numberOfTablesByThisTopic = numberOfTablesByTopic.get(topic); 
				assert(numberOfTablesByThisTopic == 0);
				numberOfTablesByTopic.remove(topic);
				assert(document.topicCountMap.get(topic) == 0);
				document.topicCountMap.remove(topic);
			}
		}
		
		
	/**
	 * Permute the ordering of documents and words in the bookkeeping (initiated from time to time)
	 */
	protected void doShuffle() {
		Collections.shuffle(documents);
		for (Document d : documents){
			Collections.shuffle(d.words);
		}
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
			System.out.println("\nDocument " + document + " (" + document.words.size() + ")");
			System.out.println(document.words.toString());
			System.out.println("DocTables(" + document.tables.size() + ")");
			for(Cluster table : document.tables)
				System.out.println(table.words);
		}
		System.out.println("Words in all the documents: " + count);
		System.out.println("Number of topics: " + topics.size());
		System.out.println("Number of tables: " + totalNumberOfTables);
	}
	
	private void saveResult() {
		 File outFileResult = new File("output/" + targetWord + ".txt");		
			if (outFileResult.exists()) {
				outFileResult.delete();
			}
			
		 try {
			 outFileResult.createNewFile();
			 BufferedWriter bwResult = new BufferedWriter(new FileWriter(outFileResult));
		
		//absorb.v absorb.v.2 absorb.cluster.1/0.8 absorb.cluster.2/0.2
	    StringBuilder toWrite = new StringBuilder();
		for(Document doc : documents) {
			Integer normalizingConstant = 0;
			toWrite.setLength(0);
			toWrite.append(doc.version.split(".\\d")[0] + " ");
			toWrite.append(doc.version);
			for(Integer value: doc.topicCountMap.values()) {
				normalizingConstant += value;
			}
			Cluster besttopic = null;
			Double bestProb = Double.NEGATIVE_INFINITY;
			System.out.println("Document is: " + doc.id );
			for(Cluster topic: doc.topicCountMap.keySet()) {
				Double topicProb = (double)doc.topicCountMap.get(topic)/(double)normalizingConstant;
				System.out.println("Topic: " + topic + " times used is:" + doc.topicCountMap.get(topic));
				System.out.println("Topic: " + topic + " prob is:" + topicProb);
				if( topicProb > bestProb) {
					bestProb = topicProb;
					besttopic = topic;
				}
				toWrite.append(" " + targetWord + ".cluster" + "." + topic.id + "/" + topicProb);
			}
			bwResult.write(toWrite.toString());
			bwResult.newLine();
			bwResult.flush();
			System.out.print("For document" + doc.id + " best topic is: " + besttopic.id + " with prob: " + bestProb);
			System.out.println();
		}
		
		bwResult.close();
	 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	// perform HDP on the collection of documents
	public static void main(String[] args) {
		
		XMLparserTesting parser = new XMLparserTesting();
		parser.files.clear();
		parser.getFileNamesInFolder(new File("Train"));
		
		for(File file : parser.files) {
			String targetWord = file.getName().substring(file.getName().lastIndexOf("/") + 1, file.getName().lastIndexOf("."));
			System.out.println(targetWord);
			
			HDPGibbs hdp = new HDPGibbs();
			hdp.readData(file, false);
			hdp.run(3000, 10);
			
			
			hdp.readData(new File("Test/" + targetWord + ".txt"), true);
			hdp.run(500, 10);
			hdp.saveResult();
			
			HashSet<Cluster> topicsUsed = new HashSet<>();
			for(Document document : hdp.documents) {
				//document.printProbsBesttopic();
				topicsUsed.addAll(document.topicCountMap.keySet());
			}
			
			System.out.println("Topics used globally: " + topicsUsed.size());
			System.out.println(topicsUsed);
		}
		
		
		
	}	
}