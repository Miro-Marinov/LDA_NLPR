import java.util.HashMap;
import java.util.HashSet;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;

public class Cluster {
	HashSet<ProcessedWord> words ;	
	HashMap<String, Integer> wordCounts ;
	
	public Cluster() {
		words = new HashSet<>();
		wordCounts = new HashMap<>();
	}
	
	public void add(ProcessedWord word){
		String lemma = word.lemma;
		Integer wordCount = wordCounts.get(lemma);
		
		if(wordCount == null)
			wordCounts.put(lemma, 1);	
		else wordCounts.put(lemma, wordCount + 1);	
		
		words.add(word);
	}
	
	public void remove(ProcessedWord word){
		String lemma = word.lemma;
		words.remove(word);
		Integer wordCount = wordCounts.get(lemma);
		if(wordCount != null)
			wordCounts.put(lemma, wordCount - 1);	
	}
	
	public Integer size() {
		return words.size();
	}
}
