import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class FiniteLDA {
    
	private static boolean dbg = true;

	private final Integer iter = 10000; // number of iterations for the Gibbs sampling

	private  Integer K; // number of clusters
    private  Integer A; // alpha = A/K
    private  Integer N; // total number of unique words across all contexts
    private  Integer B; // beta = B/N
    
    List<Cluster> clusters; // list of clusters
	List<Context> contexts; // list of contexts

	private HashSet<String> globalWordSet; // set of distinct words across the whole context collection

	private final double alpha = 1.; // alpha = K/A
	private final double beta = 1.;  // beta
	String targetWord;

	public FiniteLDA(Integer K, File file) {
		System.out.println("\n\nPerforming LDA on the specified documents..");

		this.K = K;
		this.N = 0;
		clusters = new ArrayList<>(K);
		
		// populate the clusters
		for(int i = 0 ; i < K ; i++) {
			clusters.add(new Cluster());
		}

		globalWordSet = new HashSet<>();
		contexts = new ArrayList<>();

		
		Random randGen = new Random();
		Integer contextID = 0;
		
		System.out.println("\n\nInitial random cluster assignment..");
		
		try {	
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brContexts = new BufferedReader(new FileReader(file));
			if(dbg)System.out.println("\n\nRarget word is " + targetWord);
			String contextString;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			// parse all the contexts
			while((contextString = brContexts.readLine()) != null) {	// for each context
				String[] wordStrings = tokenizer.tokenize(contextString);
				Context newContext = new Context(contextString, contextID++);
				// parse all the strings for a context
				for(String wordString : wordStrings) {
					if(!wordString.equals(targetWord)) {
						Integer randomClusterId = randGen.nextInt(K); // gives me a random cluster of a word
						Cluster randomCluster = clusters.get(randomClusterId);
						
						Word word = new Word(wordString, newContext, randomCluster);
						newContext.words.add(word);
						//random cluster assignment
						putWord (word, randomCluster, newContext);
		
						N++;
						globalWordSet.add(wordString); // set of all unique words present across all contexts
					}
				}
				contexts.add(newContext);
			}
			brContexts.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		
		printClusters();
		System.out.println("\n\nTotal number of words initially clustered: " + N);
		System.out.println("\n\nTotal unique words: " + globalWordSet.size() + "\n\n");
		System.out.println("\n\nNumber of clusters: " + clusters.size() + "\n\n");
		this.A = K;
		this.B = globalWordSet.size(); //number of unique words
	}
	// remove word from a cluster
	private void removeWord (Word word, Context context) {
		Cluster cluster = word.cluster;
		Integer clusterCount = context.clusterCountMap.get(cluster); // how many times the cluster has been used for this context

		cluster.remove(word);
		word.cluster = null;

		if(clusterCount != null && clusterCount != 0)
			context.clusterCountMap.put(cluster, clusterCount - 1);
	}
	// put a word in a cluster
	private void putWord (Word word, Cluster cluster, Context context) {
		word.cluster = cluster; // associate word with the cluster
		cluster.add(word); //add word to the cluster
		Integer clusterCount = context.clusterCountMap.get(cluster);

		if(clusterCount == null)
			context.clusterCountMap.put(cluster, 1);
		else context.clusterCountMap.put(cluster, clusterCount + 1);

	}

	// perform LDA for the context collection for an ambiguous word
	public void performLDA() {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if(i % 1000 == 0) System.out.println("ITER: " + i);
			for(Context context : contexts) {
				if(dbg)System.out.println("In context: " + context.toString());
				for(Word word : context.words) {
					// dont have to cluster the target word
					if(!word.wordString.equals(targetWord)) {
						if(dbg)System.out.println("Clustering word " + word.toString());
						// remove word from the cluster it was previously in
						removeWord(word, context);
						// perform Gibbs for the word
						gibbsEstimate(word, context);	
						if(dbg)printClusters();
						if(dbg)printContexts();
						}
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
		System.out.println("\n\nNumber of clusters: " + clusters.size() + "\n\n");
	}
	
	private void printContexts() {
		int count = 0;
		System.out.println("\n\nContext outlook:\n\n");
		for(Context context : contexts) {
			count += context.words.size();
			System.out.println(context.words.toString());
		}
		System.out.println("\nWords in all the contexts: " + count + "\n");
	}
	
	// Chooses a new cluster for the target word, based on all other instantiations
	private void gibbsEstimate(Word word, Context context) {
			List<Double> probs = new ArrayList<>(K); 
			Double normConst = 0.;
			if(dbg)System.out.println("Calculating probabilities: ");
			for(int i = 0 ; i < clusters.size() ; i++ ) {

				Cluster cluster = clusters.get(i);
				// times the cluster was used for the words in this context
				Integer timesClusterUsedForContext = context.clusterCountMap.get(cluster);
				if(timesClusterUsedForContext == null) timesClusterUsedForContext = 0;
				double term1 = ((double)(timesClusterUsedForContext + alpha)) / (double)(context.words.size() - 1 + A ); // A = K => alpha = A/K = 1
				// times this word has been clustered in this cluster (across all contexts)
				Integer timesWordPresentInCluster = cluster.wordCounts.get(word.wordString); 
				if(timesWordPresentInCluster == null) timesWordPresentInCluster = 0;
				double term2 = ((double)(timesWordPresentInCluster + beta)) / (double)(cluster.size() + B); // B = N => beta = B/N = 1

				Double prob = term1 * term2;
				if(dbg)System.out.print("cluster: " + i + " term1: " + term1 + " term2: " + term2 + " prob: " + prob + "\n");
				probs.add(prob) ;
				normConst += prob;
		}
			// normalize probabilities
			probs = normalize(probs, normConst);
			// select a new cluster
			Integer newCluserId = sample(probs);
			putWord (word, clusters.get(newCluserId), context);
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
		    	if(dbg)System.out.println("Cluster chosen " + i + "\n");
		    	return i;
		    }
		}
		if(dbg)System.out.println("Cluster chosen (messed up) " + 1 + "\n");
		return 1;
	}
	// performs LDA on a collection of contexts
	public static void main(String[] args) {
		FiniteLDA simpleLDA = new FiniteLDA(25, new File("SemiEval2010 dependencyContexts/testLDA.txt"));
		simpleLDA.performLDA();
		for(Context context : simpleLDA.contexts) {
			context.printProbsBestCluster();
		}
	}	
}