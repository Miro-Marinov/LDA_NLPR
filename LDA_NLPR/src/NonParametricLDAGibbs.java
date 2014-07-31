import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class NonParametricLDAGibbs {
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

	public static final Integer K = 1; // max (initial) number of topics
    private  Integer N; // total number of words across all documents
    private  Integer B; // total number of unique words across all documents
    List<Topic> topics;
	List<Document> documents;
	private HashSet<String> globalWordSet;
	private  double alpha = 0.01; // alpha is now a concentration parameter
	private  double beta = 1.; // beta - parameter(s) for the words distributions in topics Dir prior
	String targetWord;
	Random randGen;
	

	public NonParametricLDAGibbs(File file) {
		System.out.println("\n\nPerforming LDA on the specified documents..");

		this.N = 0;
		topics = new ArrayList<>();

		// populate the topics
		for(int i = 0 ; i < K ; i++) {
			topics.add(new Topic());
		}
		//textProcessor = new TextProcessor();
		globalWordSet = new HashSet<>();

		documents = new ArrayList<>();

		
		randGen = new Random();
		Integer documentID = 0;
		
		System.out.println("\n\nInitial random topic assignment..");
		
		try {	
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brdocuments = new BufferedReader(new FileReader(file));
			if(dbg)System.out.println("\n\nRarget word is " + targetWord);
			String oneLine;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			// parse documents
			while((oneLine = brdocuments.readLine()) != null) {	// for each document
				
				String documentString = oneLine.split(" / ")[0];
				documentString = documentString.split(" \\| ")[0];
				String[] wordStrings = tokenizer.tokenize(documentString);
				Document newdocument = new Document(documentString, documentID++);
				// parse word strings in a document
				for(String wordString : wordStrings) {
					if(!wordString.equals(targetWord)) {
						Integer randomtopicIndex = randGen.nextInt(K); // gives me a random topic of a word
						Topic randomtopic = topics.get(randomtopicIndex);
						Word word = new Word(wordString, newdocument, randomtopic);
						newdocument.words.add(word);
						//random topic assignment
						putWord (word, randomtopic);
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
		this.B = globalWordSet.size();
	}

	private void removeWord (Word word) {
		Topic topic = word.topic;
		Document document = word.document;
		if(topic != null) {
		// how many times the topic has been used for this document
		Integer topicCount = document.topicCountMap.get(topic); 

		topic.remove(word);
		// remove topic from the topics collection
		if(topic.size() == 0) {
			idHandler.freeID(topic.id);
			topics.remove(topic);
		}
		word.topic = null;

		if(topicCount != null && topicCount != 0)
			document.topicCountMap.put(topic, topicCount - 1);
			if(topicCount == 1) {
				document.topicCountMap.remove(topic);
			}
		}
	}
	// put a word in a topic
	private void putWord (Word word, Topic topic) {
		Document document = word.document;
		word.topic = topic; // associate word with the topic
		topic.add(word); //add word to the topic
		Integer topicCount = document.topicCountMap.get(topic);

		if(topicCount == null)
			document.topicCountMap.put(topic, 1);
		else document.topicCountMap.put(topic, topicCount + 1);
	}

	// performs LDA
	public void performLDA(Integer iter, Integer shuffleLag) {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if ((shuffleLag > 0) && (i > 0) && (i % shuffleLag == 0))
				doShuffle();
			for(Document document : documents) {
				if(dbg)System.out.println("In document: " + document.toString());
				for(Word word : document.words) {
					// don't have to topic the target word
					if(!word.wordString.equals(targetWord)) {
						if(dbg)System.out.println("topicing word " + word.toString());
						// remove word from the topic
						removeWord(word);
						// choose the topic for the word via fully collapsed Gibbs
						gibbsEstimate(word);	
						if(dbg)printtopics();
						if(dbg)printdocuments();
						}
					}
				}
			System.out.println("iter = " + i + " #topics = " + topics.size());
			}
		 printtopics();	
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

	private void gibbsEstimate(Word word) {
			Document document = word.document;
			Double pSum = .0;
			List<Double> p = new ArrayList<>();
			if(dbg)System.out.println("Calculating probabilities: ");
			for(Topic topic : topics) {
				Integer timestopicUsedFordocument = document.topicCountMap.get(topic);
				if(timestopicUsedFordocument == null) timestopicUsedFordocument = 0;
				
				Integer timesWordPresentIntopic = topic.wordCounts.get(word.wordString); 
				if(timesWordPresentIntopic == null) timesWordPresentIntopic = 0;
				// p (zi | X, Z-i, a, b) = p(zi | a, Z-i).p (xi | theta_zi) = term1.term2
				// using conjugate term2 = p(xi | {xj | zj = k}, beta}

				double term1 = ((double)(timestopicUsedFordocument)) / (double) (document.words.size() - 1 + alpha); 
			    /* not topic.size() - 1 since the word data point is not yet included in the topic */
				double term2 = ((double)(timesWordPresentIntopic + beta)) / (double)(topic.size() + B); // B = N => beta = 1 
				if(dbg) {
					System.out.println("\nterm1: " + term1 + "\n");
					System.out.println("term2: " + term2 + "\n");
				}
				// probability for existing topics
				double prob = term1 * term2;
				pSum += prob;
				p.add(pSum);
			}
			//probability for a new topic
			double term1 = ((double)(alpha)) / (double)(document.words.size() - 1 + alpha);
			double term2 = ((double)(beta)) / (double)B; //uniform probability for each word
			
			if(dbg) {
				System.out.println("\nterm1 new: " + term1 + "\n");
				System.out.println("term2 new: " + term2 + "\n");
			}
			
			double prob = term1 * term2;
			pSum += prob;
			p.add(pSum);
			Double r = randGen.nextDouble() * pSum;
			int j;
			for (j = 0; j <= topics.size(); j++)
				if (r < p.get(j)) 
					break;	// decided which table the word i is assigned to
			if( j == topics.size()) {
				Topic newTopic = new Topic();
				topics.add(newTopic);
				putWord(word, newTopic);
			}
			else {
				putWord(word, topics.get(j));
			}
	}


	/* not needed to sample from the DP directly - we are working with the posterior conditional
	private Double stickBreaking() {
		 Double u = randGen.nextDouble();
		 Double remainder = 0.;
		 for(Double theta : thetas.values()) {
			 remainder += theta;
		 }
		 Double newTheta = (1. - remainder) * u;
		 return newTheta;
	}
	*/
	
	private List<Double> normalize(List<Double> probs, Double normConst) { // not really needed since probabilities are proportional to the unnormalised
		double sumToOne = 0;											    // terms but normalised probabilities are easier to understand
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
	// sample based on probability vector
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
		if(dbg)System.out.println("Topic chosen (messed up) " + 1 + "\n");
		return 1;
	}
	// perform LDA on the collection of documents
	public static void main(String[] args) {
		NonParametricLDAGibbs infLDA = new NonParametricLDAGibbs(new File("testHDP/cheat.txt"));
		infLDA.performLDA(200, 10);
		for(Document document : infLDA.documents) {
			document.printProbsBesttopic();
		}
		
	}	
}