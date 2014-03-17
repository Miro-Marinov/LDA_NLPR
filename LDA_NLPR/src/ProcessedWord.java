
public class ProcessedWord {
	String lemma;
	String pos;
	Integer sentenceID;
	Integer clusterID;
	
	public ProcessedWord(String lemma, String pos, Integer sentenceID, Integer clusterID) {
		this.clusterID = clusterID;
		this.lemma = lemma;
		this.pos = pos;
		this.sentenceID = sentenceID;
	}
	
	@Override
	public String toString() {
		return lemma + "(s" + sentenceID + " c" + clusterID + ")";
	}
	
}
