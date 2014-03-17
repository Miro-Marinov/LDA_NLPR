import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;


public class SimpleLDA {
    
	private static boolean dbg = false;
	
	private final Integer iter = 10000;
	
	private  Integer K; //number of clusters
    private  Integer A;
    private  Integer B;
    private  Integer N;
    
	private List<Cluster> clusters;
	private TextProcessor textProcessor;
	List<ProcessedSentence> pSentences;
	
	private HashSet<String> globalWordSet;
	
	private final double alpha; 
	private final double beta; 
	
	
	public SimpleLDA(Integer K, String[] documentFiles) {
		System.out.println("\n\nPerforming LDA on the specified documents..");
		
		this.K = K;
		this.N = 0;
		clusters = new ArrayList<>(K);
		
		for(int i = 0 ; i < K ; i++)
			clusters.add(new Cluster());
		
		textProcessor = new TextProcessor();
		globalWordSet = new HashSet<>();
		
		List<List<AdornedWord>> adornedSentences = textProcessor.processDocuments(documentFiles, "processedSentencesLDA.txt", "stopwords.txt");
		pSentences = new ArrayList<>();
		
		System.out.println("\n\nInitial random cluster assignment..");
		Random randGen = new Random();
		Integer sentenceID = 0;
		
		for(List<AdornedWord> sentence : adornedSentences){
			ProcessedSentence pSentence = new ProcessedSentence();
			for(AdornedWord word : sentence) {
				Integer randomCluster = randGen.nextInt(K); // gives me a random cluster of a word
				String lemmata = word.getLemmata();
				ProcessedWord pWord = new ProcessedWord(lemmata, word.getPartsOfSpeech(), sentenceID, randomCluster);
				pSentence.words.add(pWord);
				
				putWord (pWord, randomCluster, pSentence);
				
				
				N++;
				globalWordSet.add(lemmata); // set of all the words (as strings)
				
			}
			sentenceID++;
			pSentences.add(pSentence);
			
		}
		printClusters();
		System.out.println("\n\nTotal number of words initially clustered: " + N);
		System.out.println("\n\nTotal set of words: " + globalWordSet.size() + "\n\n");
		this.A = K;
		this.B = N;
		this.alpha =  A/(float)K;
		this.beta = B/(float)N;
	}
	
	private void removeWord (ProcessedWord pWord, ProcessedSentence pSentence) {
		Integer clusterId = pWord.clusterID;
		Integer clusterCount = pSentence.clusterCountMap.get(clusterId);
		
		clusters.get(clusterId).remove(pWord);
		pWord.clusterID = null;
			
		if(clusterCount != null)
			pSentence.clusterCountMap.put(clusterId, clusterCount - 1);
	}
	
	private void putWord (ProcessedWord pWord, Integer clusterId, ProcessedSentence pSentence) {
		pWord.clusterID = clusterId; // associate word with the cluster
		clusters.get(clusterId).add(pWord); //add word to the cluster
		
		
		Integer clusterCount = pSentence.clusterCountMap.get(clusterId);
		
		if(clusterCount == null)
			pSentence.clusterCountMap.put(clusterId, 1);
		else pSentence.clusterCountMap.put(clusterId, clusterCount + 1);
		
	}
	
	
	public void performLDA() {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if(i % 300 == 0) System.out.println("ITER: " + i);
			for(ProcessedSentence pSentence : pSentences) {
				if(dbg)System.out.println("In sentence: " + pSentence.toString());
				for(ProcessedWord word : pSentence.words) {
					if(dbg)System.out.println("Clustering word " + word.toString());
					removeWord(word, pSentence);
					gibbsEstimate(word, pSentence);	
					if(dbg)printClusters();
					}
				}
			}
		 printClusters();	
	}
	
	private void printClusters() {
		int count = 0;
		System.out.println("\n\nClusters outlook:\n\n");
		for(Cluster cluster : clusters) {
			count += cluster.words.size();
			System.out.println(cluster.words.toString());
		}
		System.out.println("\nWords in all the clusters: " + count + "\n");
	}
	
	private void gibbsEstimate(ProcessedWord word, ProcessedSentence pSentence) {
		
			List<Double> probs = new ArrayList<>(K); 
			Double normConst = 0.;
			if(dbg)System.out.println("Calculating probabilities: ");
			for(int i = 0 ; i < clusters.size() ; i++ ) {
				
				Cluster cluster = clusters.get(i);
				
				Integer thisSentenceClusterCount = pSentence.clusterCountMap.get(i);
				if(thisSentenceClusterCount == null) thisSentenceClusterCount = 0;
				double term1 = ((double)(thisSentenceClusterCount + alpha)) / (double)(pSentence.words.size() - 1 + A ); // A = K => alpha = 1
				
				Integer thisWordInThisClusterCount = cluster.wordCounts.get(word.lemma); 
				if(thisWordInThisClusterCount == null) thisWordInThisClusterCount = 0;
				double term2 = ((double)(thisWordInThisClusterCount + beta)) / (double)(N - 1 + B); // B = N => beta = 1
				
				Double prob = term1 * term2;
				if(dbg)System.out.print("cluster: " + i + " term1: " + term1 + " term2: " + term2 + " prob: " + prob + "\n");
				probs.add(prob) ;
				normConst += prob;
					
		}
			
			probs = normalize(probs, normConst);
			Integer newCluserId = sample(probs);
			putWord (word, newCluserId, pSentence);
			if(dbg)System.out.println();
	}
	
	
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
	
	private Integer sample(List<Double> probs) {
		double p = Math.random();
		double cumulativeProbability = 0.0;
		for (int i = 0 ; i < probs.size() ; i++) {
		    
			cumulativeProbability += probs.get(i);
		    if (p <= cumulativeProbability) {
		    	if(dbg)System.out.println("Cluster chosen " + i + "\n");
		    	return i;
		    }
		}
		if(dbg)System.out.println("Cluster chosen (messed up) " + 1 + "\n");
		return 1;
	}
	
	public static void main(String[] args) {
		String[] documentFiles = new String[] {"Church Murder.txt", "Starcraft Drama.txt", "Missing Plane.txt", "Five whole fingers.txt"};
		SimpleLDA simpleLDA = new SimpleLDA(25, documentFiles);
		simpleLDA.performLDA();
	}	
}