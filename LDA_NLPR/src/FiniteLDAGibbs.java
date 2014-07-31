import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class FiniteLDAGibbs {
	IdHandler idHandler = new IdHandler();
	
	public class Topic {
		// words in the cluster
		HashSet<Word> words ;
		
		// counts for each wordString in the cluster
		HashMap<String, Integer> wordCounts ;
		Integer id;
		
		public Topic() {
			this.id = idHandler.getID();
			words = new HashSet<>();
			wordCounts = new HashMap<>();
		}

		// add word to the cluster
		public void add(Word word){
			String wordString = word.wordString;
			Integer wordCount = wordCounts.get(wordString);

			if(wordCount == null)
				wordCounts.put(wordString, 1);	
			else wordCounts.put(wordString, wordCount + 1);	

			words.add(word);
		}
		// remove word from the cluster
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
		List<Word> words; // bag of words for the context
		HashMap<Topic, Integer> topicCountMap; // Topic -> count for that context
		Integer id;
		private static final boolean dbg = false;
		
		public Document(String contextString, Integer id) {
			this.id = id;
			words = new ArrayList<>();
			topicCountMap = new HashMap<>();
		}
		
		
		public void printProbsBesttopic() {
			
			Integer normalizingConstant = 0;
			
			for(Integer value: topicCountMap.values()) {
				normalizingConstant += value;
			}
			Topic besttopic = null;
			Double bestProb = Double.NEGATIVE_INFINITY;
			if(dbg)System.out.println("Document is: " + id );
			for(Topic topic: topicCountMap.keySet()) {
				Double topicProb = (double)topicCountMap.get(topic)/(double)normalizingConstant;
				
				if(dbg)System.out.println("Topic: " + topic.id + " times used is:" + topicCountMap.get(topic));
				if( topicProb > bestProb) {
					bestProb = topicProb;
					besttopic = topic;
				}
			}
			System.out.print("For context" + id + " best topic is: " + besttopic.id + " with prob: " + bestProb);
			System.out.println();
		}
		
		@Override
		public String toString() {
			return words.toString();
		}
	}
	
	 class Word { //word object 
		final String wordString; //string representation (can be a stem, PoS etc..)
		final Document document; // from which document (document)
		Topic topic; // current topic of the word
		
		public Word(String wordString, Document document, Topic topic) {
			this.document = document;
			this.wordString = wordString;
			this.topic = topic;
		}
		
		@Override
		public String toString() {
			return wordString + "(cxt:" + document.id + " clr" + topic.id + ")";
		}
	}
	

	
    
	private static boolean dbg = false;

	private final Integer iter = 4000; // number of iterations for the Gibbs sampling

	private  Integer K; // number of topics
    private  Integer A; // alpha = A/K
    private  Integer N; // total number of unique words across all documents
    private  Integer B; // beta = B/N
    
    List<Topic> topics; // list of topics
	List<Document> documents; // list of documents

	private HashSet<String> globalWordSet; // set of distinct words across the whole document collection

	private final double alpha = 1.; // alpha = K/A
	private final double beta = 1.;  // beta
	String targetWord;

	public FiniteLDAGibbs(Integer K, File file) {
		
		System.out.println("\n\nPerforming LDA on the specified documents..");
		this.K = K;
		this.N = 0;
		topics = new ArrayList<>(K);
		
		// populate the topics
		for(int i = 0 ; i < K ; i++) {
			topics.add(new Topic());
		}

		globalWordSet = new HashSet<>();
		documents = new ArrayList<>();

		
		Random randGen = new Random();
		Integer documentID = 0;
		
			
		
		System.out.println("\n\nInitial random topic assignment..");
		try {	
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brdocuments = new BufferedReader(new FileReader(file));
			if(dbg)System.out.println("\n\nRarget word is " + targetWord);
			String documentString;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			// parse all the documents
			while((documentString = brdocuments.readLine()) != null) {	// for each document
				String[] wordStrings = tokenizer.tokenize(documentString);
				Document newdocument = new Document(documentString, documentID++);
				// parse all the strings for a document
				for(String wordString : wordStrings) {
					if(!wordString.equals(targetWord)) {
						Integer randomtopicId = randGen.nextInt(K); // gives me a random topic of a word
						Topic randomtopic = topics.get(randomtopicId);
						
						Word word = new Word(wordString, newdocument, randomtopic);
						newdocument.words.add(word);
						//random topic assignment
						putWord (word, randomtopic, newdocument);
		
						N++;
						globalWordSet.add(wordString); // set of all unique words present across all documents
					}
				}
				documents.add(newdocument);
			}
			brdocuments.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		
		printtopics();
		System.out.println("\n\nTotal number of words initially topiced: " + N);
		System.out.println("\n\nTotal unique words: " + globalWordSet.size() + "\n\n");
		System.out.println("\n\nNumber of topics: " + topics.size() + "\n\n");
		this.A = K;
		this.B = globalWordSet.size(); //number of unique words
	}
	// remove word from a topic
	private void removeWord (Word word, Document document) {
		Topic topic = word.topic;
		Integer topicCount = document.topicCountMap.get(topic); // how many times the topic has been used for this document

		topic.remove(word);
		word.topic = null;

		if(topicCount != null && topicCount != 0)
			document.topicCountMap.put(topic, topicCount - 1);
	}
	// put a word in a topic
	private void putWord (Word word, Topic topic, Document document) {
		word.topic = topic; // associate word with the topic
		topic.add(word); //add word to the topic
		Integer topicCount = document.topicCountMap.get(topic);

		if(topicCount == null)
			document.topicCountMap.put(topic, 1);
		else document.topicCountMap.put(topic, topicCount + 1);

	}

	// perform LDA for the document collection for an ambiguous word
	public void performLDA() {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if(i % 100 == 0) System.out.println("ITER: " + i);
			for(Document document : documents) {
				if(dbg)System.out.println("In document: " + document.toString());
				for(Word word : document.words) {
					// dont have to topic the target word
					if(!word.wordString.equals(targetWord)) {
						if(dbg)System.out.println("topicing word " + word.toString());
						// remove word from the topic it was previously in
						removeWord(word, document);
						// perform Gibbs for the word
						gibbsEstimate(word, document);	
						if(dbg)printtopics();
						if(dbg)printdocuments();
						}
					}
				}
			}
		 printtopics();	
		 printdocuments();
	}

	private void printtopics() {
		int count = 0;
		System.out.println("\n\ntopics outlook:\n\n");
		for(Topic topic : topics) {
			count += topic.words.size();
			System.out.println(topic.words.toString());
		}
		System.out.println("\nWords in all the topics: " + count + "\n");
		System.out.println("\n\nNumber of topics: " + topics.size() + "\n\n");
	}
	
	private void printdocuments() {
		int count = 0;
		System.out.println("\n\ndocument outlook:\n\n");
		for(Document document : documents) {
			count += document.words.size();
			System.out.println(document.words.toString());
		}
		System.out.println("\nWords in all the documents: " + count + "\n");
	}
	
	// Chooses a new topic for the target word, based on all other instantiations
	private void gibbsEstimate(Word word, Document document) {
			List<Double> probs = new ArrayList<>(K); 
			Double normConst = 0.;
			if(dbg)System.out.println("Calculating probabilities: ");
			for(int i = 0 ; i < topics.size() ; i++ ) {

				Topic topic = topics.get(i);
				// times the topic was used for the words in this document
				Integer timestopicUsedFordocument = document.topicCountMap.get(topic);
				if(timestopicUsedFordocument == null) timestopicUsedFordocument = 0;
				double term1 = ((double)(timestopicUsedFordocument + alpha)) / (double)(document.words.size() - 1 + A ); // A = K => alpha = A/K = 1
				// times this word has been topiced in this topic (across all documents)
				Integer timesWordPresentIntopic = topic.wordCounts.get(word.wordString); 
				if(timesWordPresentIntopic == null) timesWordPresentIntopic = 0;
				double term2 = ((double)(timesWordPresentIntopic + beta)) / (double)(topic.size() + B); // B = N => beta = B/N = 1

				Double prob = term1 * term2;
				if(dbg)System.out.print("topic: " + i + " term1: " + term1 + " term2: " + term2 + " prob: " + prob + "\n");
				probs.add(prob) ;
				normConst += prob;
		}
			// normalize probabilities
			probs = normalize(probs, normConst);
			// select a new topic
			Integer newCluserId = sample(probs);
			putWord (word, topics.get(newCluserId), document);
			if(dbg)System.out.println();
	}
	
	// not really needed since probabilities are proportional to the unnormalised
    // terms but normalised probabilities are easier to understand
	private List<Double> normalize(List<Double> probs, Double normConst) { 
		double sumToOne = 0;											   
		List<Double> normProbs = new ArrayList<>();
		if(dbg)System.out.println("Normalized probabilites: ");
			for(double prob : probs) {
				prob = prob/normConst;
				if(dbg)System.out.println(prob);
				sumToOne+=prob;
				normProbs.add(prob);
			}
			if(dbg)System.out.println(normProbs.toString());
			if(dbg)System.out.println("Sum of probs:" + sumToOne);
			return normProbs;
		}
	// sample given the probability vector
	private Integer sample(List<Double> probs) {
		double p = Math.random();
		double cumulativeProbability = 0.0;
		for (int i = 0 ; i < probs.size() ; i++) {
			cumulativeProbability += probs.get(i);
		    if (p <= cumulativeProbability) {
		    	if(dbg)System.out.println("Topic chosen " + i + "\n");
		    	return i;
		    }
		}
		if(dbg)System.out.println("Topic chosen (messed up) " + (probs.size() - 1) + "\n");
		return (probs.size() - 1);
	}
	// performs LDA on a collection of documents
	public static void main(String[] args) {
		FiniteLDAGibbs simpleLDA = new FiniteLDAGibbs(25, new File("SemiEval2010 dependencydocuments/class.txt"));
		simpleLDA.performLDA();
		for(Document document : simpleLDA.documents) {
			document.printProbsBesttopic();
		}
	}	
}