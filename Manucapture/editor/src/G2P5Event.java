
/**
 * G2P5 Events
 * 
 * @author dudito
 *
 */

public class G2P5Event {

	public static int NEW_PHOTO = 0;
	public static int EVENT_MASK = 1;
	public static int EVENT_CODE = 2;

	public int eventID = -1;

	public String eventCode;

	public String content;

	public String fullPath;

	public G2P5 g2p5;

	public G2P5Event(int eventID) {
		super();
		this.eventID = eventID;
	}

	public G2P5Event(int eventID, String fullPath) {
		super();
		this.eventID = eventID;
		this.fullPath = fullPath;
	}

	public G2P5Event(int eventID, String eventCode, String content) {
		super();
		this.eventID = eventID;
		this.eventCode = eventCode;
		this.content = content;
	}

}
