import processing.core.PApplet;

/**
 * Manuscript Adapter to use G2P5 Wrapper
 * 
 * @author edumo
 *
 */

public class G2P5ManucaptureAdapter implements G2P5Listener {

	protected String fullTargetPath = "";

	protected String targetFileName;
	protected String folderPath;

	G2P5 g2p5;

	String exposure = "unknown";

	public ManuCapture_v1_1 context;

	boolean propertyMirrorUpChanged = false;
	boolean focus = false;
	boolean mirrorUp = false;

	long lastEventMillis = 0;

	String lastMask = null;

	public G2P5ManucaptureAdapter(ManuCapture_v1_1 context, G2P5 g2p5) {
		this.context = context;
		this.g2p5 = g2p5;
	}
	
	public void setTargetFile(String folderPath, String targetFileName) {
		this.targetFileName = targetFileName;
		this.folderPath = folderPath;
	}

	public void setFullTargetPath(String ic) {
		fullTargetPath = folderPath + "/" + targetFileName + "_"+ ic +"_"+ g2p5.id + ".cr2";
	}

	public void newEvent(G2P5Event event) {
		lastEventMillis = context.millis();
		if (event.eventID == G2P5Event.NEW_PHOTO) {
			context.newPhotoEvent(event);
			mirrorUp = false;
		} else if (event.eventID == G2P5Event.EVENT_EXPOSURE) {
			exposure = event.content;
		} else if(event.eventID == G2P5Event.EVENT_PTP) {
			if( g2p5 instanceof Canon700D_G2P5){
				if(event.content.contains("d102")) {
					 //System.out.println(g2p5.id + " Property related with Mirror up changed ");
					 propertyMirrorUpChanged = true;
				}
			} else if(g2p5 instanceof CanonEOS5DSR_G2P5){
				if(event.content.contains("d1bf")) {
					 //System.out.println(g2p5.id + " Property related with Mirror up changed ");
					 propertyMirrorUpChanged = true;
				}
			}
		}
		else if (event.eventID == G2P5Event.EVENT_MASK) {
			if( g2p5 instanceof Canon700D_G2P5){
				if (propertyMirrorUpChanged && event.content.trim().endsWith("3")) {				
					if(context.getCaptureState()==context.CAMERAS_FOCUSSING) {
						mirrorUp = true;
						PApplet.println("MIRROR UP " + g2p5.id);
					}
				} 	
			} else if(g2p5 instanceof CanonEOS5DSR_G2P5){
				if (propertyMirrorUpChanged && event.content.trim().endsWith("1")) {	
					if(context.getCaptureState()==context.CAMERAS_FOCUSSING) {
						mirrorUp = true;
						PApplet.println("MIRROR UP " + g2p5.id);
					}
				} 	
			}
			propertyMirrorUpChanged = false;
			lastMask = event.content.trim();
		}
	}

	public String getFullTargetPath() {
		return fullTargetPath;
	}

}
