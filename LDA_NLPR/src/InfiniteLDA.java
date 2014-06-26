import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeSet;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class InfiniteLDA {
    
	private static boolean dbg = false;

	private final Integer iter = 3000;
	public static final Integer K = 25; // max (initial) number of clusters
    private  Integer N; // total number of words across all contexts
    private  Integer B; // total number of unique words across all contexts
    List<Cluster> clusters;
	List<Context> contexts;
	private HashSet<String> globalWordSet;
	private  double alpha = 0.01; // alpha is now a concentration parameter
	private  double beta = 1.; // beta - parameter(s) for the words distributions in topics Dir prior
	String targetWord;
	Random randGen;
	

	public InfiniteLDA(File file) {
		System.out.println("\n\nPerforming LDA on the specified documents..");

		this.N = 0;
		clusters = new ArrayList<>();

		// populate the clusters
		for(int i = 0 ; i < K ; i++) {
			clusters.add(new Cluster());
		}
		//textProcessor = new TextProcessor();
		globalWordSet = new HashSet<>();

		contexts = new ArrayList<>();

		
		randGen = new Random();
		Integer contextID = 0;
		
		System.out.println("\n\nInitial random cluster assignment..");
		
		try {	
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			BufferedReader brContexts = new BufferedReader(new FileReader(file));
			if(dbg)System.out.println("\n\nRarget word is " + targetWord);
			String contextString;
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			Tokenizer tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			// parse contexts
			while((contextString = brContexts.readLine()) != null) {	// for each context
				String[] wordStrings = tokenizer.tokenize(contextString);
				Context newContext = new Context(contextString, contextID++);
				// parse word strings in a context
				for(String wordString : wordStrings) {
					if(!wordString.equals(targetWord)) {
						Integer randomClusterIndex = randGen.nextInt(K); // gives me a random cluster of a word
						Cluster randomCluster = clusters.get(randomClusterIndex);
						Word word = new Word(wordString, newContext, randomCluster);
						newContext.words.add(word);
						//random cluster assignment
						putWord (word, randomCluster);
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
		this.B = globalWordSet.size();
	}

	private void removeWord (Word word) {
		Cluster cluster = word.cluster;
		Context context = word.context;
		if(cluster != null) {
		// how many times the cluster has been used for this context
		Integer clusterCount = context.clusterCountMap.get(cluster); 

		cluster.remove(word);
		// remove cluster from the clusters collection
		if(cluster.size() == 0) {
			IdHandler.freeID(cluster.id);
			clusters.remove(cluster);
		}
		word.cluster = null;

		if(clusterCount != null && clusterCount != 0)
			context.clusterCountMap.put(cluster, clusterCount - 1);
		}
	}
	// put a word in a cluster
	private void putWord (Word word, Cluster cluster) {
		Context context = word.context;
		word.cluster = cluster; // associate word with the cluster
		cluster.add(word); //add word to the cluster
		Integer clusterCount = context.clusterCountMap.get(cluster);

		if(clusterCount == null)
			context.clusterCountMap.put(cluster, 1);
		else context.clusterCountMap.put(cluster, clusterCount + 1);
	}

	// performs LDA
	public void performLDA() {
		System.out.println("Gibbs sampling: ");
		for(int i = 0 ; i < iter; i++) {
			if(i % 1000 == 0) System.out.println("ITER: " + i);
			for(Context context : contexts) {
				if(dbg)System.out.println("In context: " + context.toString());
				for(Word word : context.words) {
					// don't have to cluster the target word
					if(!word.wordString.equals(targetWord)) {
						if(dbg)System.out.println("Clustering word " + word.toString());
						// remove word from the cluster
						removeWord(word);
						// choose the cluster for the word via fully collapsed Gibbs
						gibbsEstimate(word);	
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

	private void gibbsEstimate(Word word) {
			Context context = word.context;
			List<Double> probs = new ArrayList<>(); 
			Double normConst = 0.;
			if(dbg)System.out.println("Calculating probabilities: ");
			for(Cluster cluster : clusters) {
				Integer timesClusterUsedForContext = context.clusterCountMap.get(cluster);
				if(timesClusterUsedForContext == null) timesClusterUsedForContext = 0;
				
				Integer timesWordPresentInCluster = cluster.wordCounts.get(word.wordString); 
				if(timesWordPresentInCluster == null) timesWordPresentInCluster = 0;
				// p (zi | X, Z-i, a, b) = p(zi | a, Z-i).p (xi | theta_zi) = term1.term2
				// using conjugate term2 = p(xi | {xj | zj = k}, beta}

				double term1 = ((double)(timesClusterUsedForContext)) / (double) (context.words.size() - 1 + alpha); 
			    double term2 = ((double)(timesWordPresentInCluster + beta)) / (double)(cluster.size() - 1 + B); // B = N => beta = 1 
				if(dbg) {
					System.out.println("\nterm1: " + term1 + "\n");
					System.out.println("term2: " + term2 + "\n");
				}
				// probability for existing clusters
				double prob = term1 * term2;
				probs.add(prob);
				
				normConst += prob;
			}
			//probability for a new cluster
			double term1 = ((double)(alpha)) / (double)(context.words.size() - 1 + alpha);
			double term2 = ((double)(beta)) / (double)B; //uniform probability for each word
			
			if(dbg) {
				System.out.println("\nterm1 new: " + term1 + "\n");
				System.out.println("term2 new: " + term2 + "\n");
			}
			
			double prob = term1 * term2;
			probs.add(term1 * term2);
			normConst += prob;
			// normalize probabilities
			probs = normalize(probs, normConst);
			// sample a new cluster
			Integer newCluserIndex = sample(probs);
			// if assigned to existing cluster: just put the word in that cluster
			if(newCluserIndex != probs.size()-1)
				putWord (word, clusters.get(newCluserIndex));
			// otherwise:
			else {
				// create a new cluster
				Cluster newCluster = new Cluster();
				// add it to the clusters list
				clusters.add(newCluster);
				// add the word in the new cluster
				putWord (word, newCluster);
			}
			if(dbg)System.out.println();
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
		    	if(dbg)System.out.println("Cluster chosen " + i + "\n");
		    	return i;
		    }
		}
		if(dbg)System.out.println("Cluster chosen (messed up) " + 1 + "\n");
		return 1;
	}
	// perform LDA on the collection of contexts
	public static void main(String[] args) {
		InfiniteLDA infLDA = new InfiniteLDA(new File("SemiEval2010 sentenceContexts/absorb.txt"));
		infLDA.performLDA();
		for(Context context : infLDA.contexts) {
			context.printProbsBestCluster();
		}
		
	}	
}