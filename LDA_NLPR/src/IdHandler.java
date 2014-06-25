import java.util.PriorityQueue;


public class IdHandler {
	private static Integer curMaxID = 1;
	private static PriorityQueue<Integer> freedIDs = new PriorityQueue<>();

	public static Integer getID() {
		if(freedIDs.isEmpty()) {
			return curMaxID++;
		}
		else {
			return freedIDs.remove();
		}
	}
	
	public static void freeID(Integer ID) {
		freedIDs.add(ID);
	}
}
