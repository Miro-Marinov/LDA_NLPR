import java.util.HashMap;
import java.util.HashSet;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;

public class Cluster {
	HashSet<AdornedWord> words ;	
	HashMap<String, Integer> wordCounts ;
	
	public Cluster() {
		words = new HashSet<>();
		wordCounts = new HashMap<>();
	}
	
	public void add(AdornedWord word){

		Integer wordCount = wordCounts.get(word.getLemmata());
		
		if(wordCount == null)
			wordCounts.put(word.getLemmata(), 1);	
		else wordCounts.put(word.getLemmata(), wordCount + 1);	
		
		words.add(word);
	}
	
	public void remove(AdornedWord word){
		words.remove(word);
		Integer wordCount = wordCounts.get(word.getLemmata());
		if(wordCount != null)
			wordCounts.put(word.getLemmata(), wordCount - 1);	
	}
	
	public Integer size() {
		return words.size();
	}
}
