
/**
 * G2P5 Events
 * 
 * @author dudito
 *
 */

public class G2P5Event {

	//zero and negative are for processed events
	
	public static int EVENT_EXPOSURE = -1;
	public static int NEW_PHOTO = 0;
	
	public static int EVENT_MASK = 1;
	public static int EVENT_CODE = 2;
	public static int EVENT_BUTTON = 3;
	public static int EVENT_PTP = 4;
	public static int EVENT_CAMERA = 5;
	

	public int eventID = -1;

	public String eventCode;

	public String content;

	public G2P5 g2p5;

	public G2P5Event(int eventID) {
		super();
		this.eventID = eventID;
	}

	public G2P5Event(int eventID, String eventCode, String content) {
		super();
		this.eventID = eventID;
		this.eventCode = eventCode;
		this.content = content;
	}

}
