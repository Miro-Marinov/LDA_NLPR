
public class Word { //word object 
	final String wordString; //string representation (can be a stem, PoS etc..)
	final Context context; // from which context (document)
	Cluster cluster; // current cluster of the word
	
	public Word(String wordString, Context context, Cluster cluster) {
		this.context = context;
		this.wordString = wordString;
		this.cluster = cluster;
	}
	
	@Override
	public String toString() {
		return wordString + "(cxt:" + context.id + " clr" + cluster.id + ")";
	}
	
}
