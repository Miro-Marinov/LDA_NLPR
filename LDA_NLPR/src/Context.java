import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Context {
	List<Word> words; // bag of words for the context
	HashMap<Cluster, Integer> clusterCountMap; // Cluster -> count for that context
	Integer id;
	private static final boolean dbg = false;
	
	public Context(String contextString, Integer id) {
		this.id = id;
		words = new ArrayList<>();
		clusterCountMap = new HashMap<>();
	}
	public void printProbsBestCluster() {
		
		Integer normalizingConstant = 0;
		
		for(Integer value: clusterCountMap.values()) {
			normalizingConstant += value;
		}
		Cluster bestCluster = null;
		Double bestProb = Double.NEGATIVE_INFINITY;
		if(dbg)System.out.println("Context is: " + id );
		for(Cluster cluster: clusterCountMap.keySet()) {
			Double clusterProb = (double)clusterCountMap.get(cluster)/(double)normalizingConstant;
			
			if(dbg)System.out.println("Cluster: " + cluster.id + " times used is:" + clusterCountMap.get(cluster));
			if( clusterProb > bestProb) {
				bestProb = clusterProb;
				bestCluster = cluster;
			}
		}
		System.out.print("For context" + id + " best cluster is: " + bestCluster.id + " with prob: " + bestProb);
		System.out.println();
	}
	
	@Override
	public String toString() {
		return words.toString();
	}
	
}
