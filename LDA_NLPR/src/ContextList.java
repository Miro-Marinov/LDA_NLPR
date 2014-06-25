import java.util.ArrayList;
import java.util.List;


public class ContextList {
	List<String> contexts;
	String targetWord;
	
	public ContextList(String targetWord, List<String> contexts) {
		this.contexts = contexts;
		this.targetWord = targetWord;
	}
	
	public ContextList(String targetWord) {
		this.contexts = new ArrayList<>();;
		this.targetWord = targetWord;
	}
	
	public ContextList() {
		this(null);
	}
	
	
}
