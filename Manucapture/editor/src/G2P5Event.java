
/**
 * G2P5 Events
 *  
 * @author dudito
 *
 */

public class G2P5Event {

	public static int NEW_PHOTO = 0;

	public int eventID = -1;

	public String fullPath;

	public G2P5Event(int eventID) {
		super();
		this.eventID = eventID;
	}

	public G2P5Event(int eventID, String fullPath) {
		super();
		this.eventID = eventID;
		this.fullPath = fullPath;
	}

}
