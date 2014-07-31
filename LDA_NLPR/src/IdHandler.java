import java.util.PriorityQueue;


public class IdHandler {
	private Integer curMaxID = 0;
	private PriorityQueue<Integer> freedIDs = new PriorityQueue<>();

	public Integer getID() {
		if(freedIDs.isEmpty()) {
			return curMaxID++;
		}
		else {
			return freedIDs.remove();
		}
	}
	
	public void freeID(Integer ID) {
		freedIDs.add(ID);
	}
}
