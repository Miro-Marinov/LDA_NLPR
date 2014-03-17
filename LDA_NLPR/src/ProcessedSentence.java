import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ProcessedSentence {
	List<ProcessedWord> words; // list of words for tha sentence
	HashMap<Integer, Integer> clusterCountMap; // ClusterID -> count for that sentence
	
	public ProcessedSentence() {
		words = new ArrayList<>();
		clusterCountMap = new HashMap<>();
	}
	@Override
	public String toString() {
		return words.toString();
	}
}
