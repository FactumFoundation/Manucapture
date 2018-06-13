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

	/*
	 * public synchronized boolean captureTethered(boolean on) { if(active){
	 * if(!on){ killAllProcessByName(id+".cr2"); } else { t = new
	 * TetheredCaptureThread(); t.start(); } } tethering = on; return false; }
	 */

	public void setTargetFile(String folderPath, String targetFileName) {
		this.targetFileName = targetFileName;
		this.folderPath = folderPath;
	}

	public void setFullTargetPath(int ic) {

	}

	public void newEvent(G2P5Event event) {

		System.out.println();

		if (event.eventID == G2P5Event.NEW_PHOTO) {
			//
			int ic = G2P5Manager.addImageCount();
			fullTargetPath = folderPath + "/" + targetFileName + "_" + id + "_" + ic + ".cr2";
			// fullTargetPath = folderPath + "/" + targetFileName;
			try {
				moveFile(event.content, fullTargetPath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public String getFullTargetPath() {

		fullTargetPath = folderPath + "/" + targetFileName;
		return fullTargetPath;
		// return super.getFullTargetPath();
	}

	public boolean moveFile(String fullPath, String toFullPath) throws IOException {
		String commandToRun;
		commandToRun = "mv " + fullPath + " " + toFullPath;
		PApplet.println(commandToRun);
		InputStream iStream = null;
		BufferedReader bReader = null;
		try {
			String[] cmds = new String[] { "/bin/sh", "-c", commandToRun };
			Process p = new ProcessBuilder(cmds).start();
			iStream = p.getInputStream();
			bReader = new BufferedReader(new InputStreamReader(iStream), 1);
			String moveline;
			while ((moveline = bReader.readLine()) != null) {
				PApplet.println("MV message : " + moveline);
			}
			// Thread.sleep(500);
			return true;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		} finally {
			if (iStream != null)
				iStream.close();
			if (bReader != null)
				bReader.close();
		}

	}
}
