import java.util.HashMap;
import java.util.HashSet;


public class Cluster {
	// words in the cluster
	HashSet<Word> words ;
	
	// counts for each wordString in the cluster
	HashMap<String, Integer> wordCounts ;
	Integer id;
	
	public Cluster() {
		this.id = IdHandler.getID();
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