import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

	protected String id;

	String exposure = "unknown";

	public ManuCapture_v1_1 manuCapture;

	boolean focus = false;
	boolean mirrorUp = false;

	/*
	 * public synchronized boolean captureTethered(boolean on) { if(active){
	 * if(!on){ killAllProcessByName(id+".cr2"); } else { t = new
	 * TetheredCaptureThread(); t.start(); } } tethering = on; return false; }
	 */

	public void setTargetFile(String folderPath, String targetFileName) {
		this.targetFileName = targetFileName;
		this.folderPath = folderPath;
	}

	public void setFullTargetPath(String ic) {
		fullTargetPath = folderPath + "/" + targetFileName + "_" + g2p5.id + "_" + ic + ".cr2";
	}

	public void newEvent(G2P5Event event) {


		if (event.eventID == G2P5Event.NEW_PHOTO) {
			//
			int ic = G2P5Manager.addImageCount();
			manuCapture.newPhotoEvent(event, "" + ic);
			mirrorUp = false;
		} else if (event.eventID == G2P5Event.EVENT_EXPOSURE) {
			exposure = event.content;
		} else if (event.eventID == G2P5Event.EVENT_FOCUS) {
			System.out.println(event.content);
			focus = true;
		} else if (event.eventID == G2P5Event.EVENT_NO_FOCUS) {
			System.out.println(event.content);
			focus = false;
		} else if (event.eventID == G2P5Event.EVENT_BUTTON) {
			System.out.println(event.content);
			int bid = Integer.parseInt(event.content.trim());
			if (bid % 1024 == 8) {
			}
			if (bid % 1024 == 1) {
			}
		}else if (event.eventID == G2P5Event.EVENT_MASK) {
			System.out.println(event.content);
			// It seems that mask 983 means "mirror up" after focus 
			if (event.content.trim().endsWith("3")){// || event.content.trim().equals("bc3")) {
				mirrorUp = true;
			} // Detects 
			if (event.content.trim().endsWith("00")) {
				mirrorUp = false;				
			}
		}

	}

	public String getFullTargetPath() {

		// fullTargetPath = folderPath + "/" + targetFileName;

		return fullTargetPath;
		// return super.getFullTargetPath();
	}

}
