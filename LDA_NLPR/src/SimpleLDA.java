import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;


public class SimpleLDA {
    private final Integer iter = 4000;
	
	private  Integer K; //number of clusters
    private  Integer A;
    private  Integer B;
    private  Integer N;
    
	private List<Cluster> clusters;
	private HashMap<AdornedWord, Integer> clusterMap; // word -> clusterId
	private HashMap<AdornedWord, Integer> sentenceMap; // word -> sentenceID
	private TextProcessor textProcessor;
	private List<List<AdornedWord>> sentences;
	
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
		clusterMap = new HashMap<>();
		sentenceMap = new HashMap<>();
		globalWordSet = new HashSet<>();
		
		sentences = textProcessor.processDocuments(documentFiles, "processedSentencesLDA.txt", "stopwords.txt");
		
		Random randGen = new Random();
		Integer sentenceID = 0;
		for(List<AdornedWord> sentence : sentences){
			for(AdornedWord word : sentence) {
				Integer randomCluster = randGen.nextInt(K); // gives me a random cluster of a word
				sentenceMap.put(word, sentenceID);
				putWord (word, randomCluster);
				N++;
				globalWordSet.add(word.getLemmata()); // set of all the words (as strings)
				
			}
		}
		
		System.out.println("\n\nTotal number of words initially clustered: " + N + "\n\n");
		
		this.A = K;
		this.B = N;
		this.alpha =  A/(float)K;
		this.beta = B/(float)N;
	}
	
	private void removeWord (AdornedWord word) {
		clusters.get(clusterMap.get(word)).remove(word);
		clusterMap.remove(word);
	}
	
	private void putWord (AdornedWord word, Integer clusterId) {
		clusterMap.put(word, clusterId); // associate word with the cluster
		clusters.get(clusterId).add(word); //add word to the cluster
	}
	
	
	public void performLDA() {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if(i % 300 == 0) System.out.println("ITER: " + i);
			for(List<AdornedWord> sentence : sentences) {
				
				for(AdornedWord word : sentence) {
					removeWord(word);
					gibbsEstimate(word, sentence);	
					}
				}
			}
		System.out.println("\n\nClusters outlook:\n\n");
		for(Cluster cluster : clusters) {
			System.out.println(cluster.words.toString());
		}
	}
	
	private void gibbsEstimate(AdornedWord word, List<AdornedWord> sentence) {
		
			List<Double> probs = new ArrayList<>(K); 
			Double normConst = 0.;
			for(int i = 0 ; i < clusters.size() ; i++ ) {
				Cluster cluster = clusters.get(i);
				double term1 = ((double)(cluster.size() + alpha)) / (double)(N - 1 + A ); // A = K => alpha = 1
				Integer thisWordInThisClusterCount = cluster.wordCounts.get(word.getLemmata()); 
				if(thisWordInThisClusterCount == null) thisWordInThisClusterCount = 0;
				double term2 = ((double)(thisWordInThisClusterCount + beta)) / (double)(N - 1 + B); // B = N => beta = 1
				
				Double prob = term1 * term2;
				
				probs.add(prob) ;
				normConst += prob;
					
		}
			
			probs = normalize(probs, normConst);
			Integer newCluserId = sample(probs);
			putWord (word, newCluserId);		
	}
	
	
	private List<Double> normalize(List<Double> probs, Double normConst) {
			for(Double prob : probs) {
				prob = prob/normConst;
			}
			return probs;
		}
	
	private Integer sample(List<Double> probs) {
		double p = Math.random();
		double cumulativeProbability = 0.0;
		for (int i = 0 ; i < probs.size() ; i++) {
		    
			cumulativeProbability += probs.get(i);
		    if (p <= cumulativeProbability) {
		        return i;
		    }
		}
		return 1;
	}
	
	public static void main(String[] args) {
		String[] documentFiles = new String[] {"Church Murder.txt", "Starcraft Drama.txt", "Missing Plane.txt", "Five whole fingers.txt"};
		SimpleLDA simpleLDA = new SimpleLDA(15, documentFiles);
		simpleLDA.performLDA();
	}	
}