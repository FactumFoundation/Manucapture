import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import g4p_controls.G4P;
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;

public class ManuCaptureContext {

	// size view on screen
	float ratioAspect = 1f;
	
	int wImageViewerSize = (int)(1000*ratioAspect);
	int hImageViewerSize = (int)(667*ratioAspect);

	// width size of the preview
	public int viewerWidthResolution = 2000;

	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_TO_EVENT = 3000;

	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_FOCUSSING = 7000;

	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_CAPTURE_MACHINE_STATE = 15000;

	PrintWriter logOutput;

	ManuCapture_v1_1 parent;

	RawFile thumbnail;

	G2P5 gphotoA;
	G2P5 gphotoB;

	G2P5ManucaptureAdapter gphotoAAdapter;
	G2P5ManucaptureAdapter gphotoBAdapter;

	G2P5ManucaptureView viewA;
	G2P5ManucaptureView viewB;

	OscP5 oscP5;

	GUI gui;

	NetAddress viewerLocation;

	GUIController guiController;

	Project project;

	boolean renderLeft = true;
	boolean renderRight = true;
	
	PImage imgPreviewLeft;
	PImage imgPreviewRight;

	String appPath = "/home/factum/git/book_scanner/bookScanner/Manucapture";

	int rotationA = 270;
	int rotationB = 90;

	List<HotArea> pointsLeft = new ArrayList<>();
	List<HotArea> pointsRight = new ArrayList<>();

	float scaleA = 1;
	float scaleB = 1;

	// width resolution for viewer

	boolean cameraActiveA = false;
	boolean cameraActiveB = false;

	boolean cameraAProcessingNewPhoto = false;
	boolean cameraBProcessingNewPhoto = false;

	String serialCameraA;
	String serialCameraB;

	XML serialXMLA;
	XML serialXMLB;

	String newImagePathA = "";
	String newImagePathB = "";

	String lastImagePathA = "";
	String lastImagePathB = "";

	public NetAddress arduinoDriverLocation;

	PImage lastLeftPreview = null;
	PImage lastRightPreview = null;

	public static int CAMERAS_INACTIVE = -1;
	public static int CAMERAS_IDLE = 0;
	public static int CAMERAS_FOCUSSING = 1;
	public static int CAMERAS_MIRROR_UP = 2;
	public static int CAMERAS_PROCESSING = 3;

	int captureState = CAMERAS_INACTIVE;
	int counterEvent = 0;

	long lastCaptureMillis = 0;

	public boolean ignoreNextPhotoA = false;
	public boolean ignoreNextPhotoB = false;

	long lastCameraAAction = 0;
	long lastCameraBAction = 0;

	MessageContainer messageContainer;

	public void initCropHotAreas() {
		
		pointsLeft = new ArrayList<>();
		pointsRight = new ArrayList<>();

		int size = 50;

		PVector translatePos1 = new PVector(parent.marginLeftViewerLeft, parent.marginTopViewer);
		PVector translatePos2 = new PVector(parent.marginLeftViewerRight, parent.marginTopViewer);


		pointsLeft.add(new HotArea(new PVector(size, size), translatePos1, 0, size, "LTL"));
		pointsLeft.add(new HotArea(new PVector(hImageViewerSize - size, 0 + size), translatePos1, 1, size, "LTR"));
		pointsLeft.add(new HotArea(new PVector(hImageViewerSize - size, wImageViewerSize - size), translatePos1, 2,
				size, "LBL"));
		pointsLeft.add(new HotArea(new PVector(0 + size, wImageViewerSize - size), translatePos1, 3, size, "LBR"));

		pointsRight.add(new HotArea(new PVector(size, size), translatePos2, 0, size, "RTL"));
		pointsRight.add(new HotArea(new PVector(hImageViewerSize - size, size), translatePos2, 1, size, "RTR"));
		pointsRight.add(new HotArea(new PVector(hImageViewerSize - size, wImageViewerSize - size), translatePos2, 2,
				size, "RBL"));
		pointsRight.add(new HotArea(new PVector(size, wImageViewerSize - size), translatePos2, 3, size, "RBR"));
	}

	public G2P5ManucaptureAdapter createG2P5(String serial, String name) {
		G2P5 g2p5 = G2P5.create(parent.homeDirectory(), serial, name);
		G2P5ManucaptureAdapter adapter = new G2P5ManucaptureAdapter();
		adapter.g2p5 = g2p5;
		adapter.manuCapture = parent;
		// TODO check if is null if not project created
		adapter.setTargetFile(parent.project.projectDirectory + "raw", parent.project.projectName);
		return adapter;
	}

	public List<HotArea> copyMesh(List<HotArea> mesh) {
		List<HotArea> temp = new ArrayList<>();
		for (int i = 0; i < mesh.size(); i++) {
			HotArea ha = mesh.get(i);
			temp.add(new HotArea(ha.pos.copy(), ha.translatePos.copy(), ha.id, ha.threshold, ha.name));
		}

		return temp;
	}

	public void deleteAllFiles(String targetFilePath, String suf) {
		// parent.println("delete all " + suf + "files " + targetFilePath);
		File storageDir = new File(targetFilePath);

		if (!storageDir.exists()) {
			storageDir.mkdirs();
		}

		for (File tempFile : storageDir.listFiles()) {
			if (tempFile.getName().endsWith(suf))
				tempFile.delete();
		}
		// parent.println("end delete all " + suf + "files " + targetFilePath);
	}

	public void clearPaths() {
		lastImagePathA = newImagePathA;
		lastImagePathB = newImagePathB;
		newImagePathA = "";
		newImagePathB = "";
	}

	public void deleteFile(String targetFilePath) {
		String commandGenerate = "rm " + targetFilePath;
		parent.println(commandGenerate);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandGenerate };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// println("InputStreamReader : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void copyFiles(String form, String to) {

	}

	public boolean moveFile(String fullPath, String toFullPath) {
		String commandToRun;
		commandToRun = "cp " + fullPath + " " + toFullPath;
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
				try {
					iStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if (bReader != null)
				try {
					bReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

	}

	public boolean writeExifData(String fullFileName, String documentId, String xResolution, String yResolution) {

		Process pr = null;
		InputStream in = null;
		String exifToolError = "";

		try {
			String command = "exiftool -Xresolution=" + xResolution + " -Yresolution=" + yResolution + " -DocumentName="
					+ documentId + " -overwrite_original -P  " + fullFileName;
			command = new String(command.getBytes(), "UTF-8");
			pr = Runtime.getRuntime().exec(command);
			in = pr.getErrorStream();
			int data = in.read();
			while (data != -1) {
				// do something with data...
				exifToolError += (char) data;
				data = in.read();
			}
			if (in != null)
				in.close();
			if (!exifToolError.equals("")) {
				_println("exiftool error" + exifToolError);
			}
		} catch (IOException e) {
		} finally {
			if (pr != null)
				pr.destroy();
		}

		return false;
	}

	public void _println(String message) {
		int s = parent.second(); // Values from 0 - 59
		int min = parent.minute(); // Values from 0 - 59
		int h = parent.hour(); // Values from 0 - 23
		int d = parent.day(); // Values from 1 - 31
		int m = parent.month(); // Values from 1 - 12
		int y = parent.year(); // 2003, 2004, 2005, etc.
		String date = d + "/" + m + "/" + y + "-" + h + ":" + min + ":" + s;
		if (logOutput == null) {
			logOutput = parent.createWriter("log_" + date + ".txt");
		}
		logOutput.println("[" + date + "] " + message);
		logOutput.flush(); // Writes the remaining data to the file
	}

	/*
	 * MetaData_Utils : Extra functions for Metadata generation: md5, timestamp
	 * 
	 */

	public String generateMD5(String id, String side) {

		Process pr = null;
		InputStream in = null;
		String md5 = "";
		try {
			pr = Runtime.getRuntime().exec("md5sum " + project.projectDirectory + id + "_" + side + ".cr2");
			in = pr.getInputStream();
			int data = in.read();
			while (data != -1) {
				// do something with data...
				md5 += (char) data;
				data = in.read();
			}
			in.close();
		} catch (IOException e) {
			_println("Problem generating md5");
		} finally {
			if (pr != null)
				pr.destroy();
		}
		String[] parts = md5.split(" ");
		if (parts.length > 1) {
			md5 = parts[0];
		}

		try {
			PrintWriter writer = new PrintWriter(project.projectDirectory + id + "_" + side + ".md5", "UTF-8");
			writer.println(md5);
			writer.close();
		} catch (Exception e) {
			_println("Problem saving md5");
		}

		return md5;
	}

	public String generateTimeStamp(String id, String side) {
		String timeStamp = "";
		File file = new File(project.projectDirectory + id + "_" + side + ".cr2");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		timeStamp = sdf.format(file.lastModified());
		return timeStamp;
	}

	public void init() {

		gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);

		gphotoA = gphotoAAdapter.g2p5;
		gphotoB = gphotoBAdapter.g2p5;

		gphotoA.listener = gphotoAAdapter;
		gphotoB.listener = gphotoBAdapter;

		captureState = CAMERAS_IDLE;

		// if(!cameraActiveA){
		// guiController.camera_A_active_button_click(null, null);
		// }
		//
		// if(!cameraActiveB){
		// guiController.camera_B_active_click(null, null);
		// }

		// releaseCameras();
	}

	public void capture() {
		// Init capture secuence
		if (captureState == CAMERAS_IDLE) {
			if (gphotoA.isConnected() && gphotoB.isConnected()) {
				captureState = CAMERAS_FOCUSSING;
				pressCameras();
				lastCaptureMillis = parent.millis();
			} else {
				G4P.showMessage(parent, messageContainer.getText("sw.notconnected"), "", G4P.WARNING);
			}

		} else {
			if (captureState == CAMERAS_INACTIVE) {
				G4P.showMessage(parent, messageContainer.getText("sw.notready"), "", G4P.WARNING);
			}
		}
	}

	public void processCamerasEvent(G2P5Event event) {

	}

	public void camerasStateMachineLoop() {

		boolean failedA = false;
		boolean failedB = false;

		if (lastCameraAAction > 0 && gphotoAAdapter.lastEventMillis > lastCameraAAction + MAX_TIME_TO_EVENT) {
			// // we have a failing state, pulse lost
			// resetCamerasFailingB();
			// ignoreNextPhotoA = true;
			 lastCameraAAction = -1;
			failedA = true;
			parent.println("FAILED A");
		}
		//
		if (lastCameraBAction > 0 && gphotoBAdapter.lastEventMillis > lastCameraBAction + MAX_TIME_TO_EVENT) {
			// // we have a failing state, pulse lost
			// resetCamerasFailingA();
			// ignoreNextPhotoB = true;
			 lastCameraBAction = -1;
			failedB = true;
			parent.println("FAILED B");
		}

		// if we are out from Idle more than a time we break
		if (captureState != CAMERAS_IDLE && parent.millis() - lastCaptureMillis > MAX_TIME_CAPTURE_MACHINE_STATE) {
			restoreCamerasStateAfterFailure();
		}

		if (captureState == CAMERAS_INACTIVE && gphotoA.active && gphotoB.active) {
			captureState = CAMERAS_IDLE;
		} else if (captureState == CAMERAS_FOCUSSING) {
			// @TODO failedA and failedB
			if (failedA) {
				// resetCamerasFailingB();
				// ignoreNextPhotoA = true;
				 lastCameraAAction = -1;
				 restoreCamerasStateAfterFailure();
				G4P.showMessage(parent, messageContainer.getText("sw.noeventA"), "", G4P.WARNING);
			}

			if (failedB) {
				// resetCamerasFailingA();
				// ignoreNextPhotoB = true;
				 lastCameraBAction = -1;
				 restoreCamerasStateAfterFailure();
				G4P.showMessage(parent, messageContainer.getText("sw.noeventB"), "", G4P.WARNING);
			}
			if (gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
				releaseAndShutterCameras();
				captureState = CAMERAS_MIRROR_UP;
				// if (parent.state == parent.CHART) {
				// }
			} else {
				if (parent.millis() - lastCaptureMillis > MAX_TIME_FOCUSSING) {
					parent.println("Lsa dos cámaras no están dispuestas a poner el mirror en up");
					restoreCamerasStateAfterFailure();
				}
			}
		} else if (captureState == CAMERAS_MIRROR_UP) {
			if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
				captureState = CAMERAS_PROCESSING;
			}

		}

		else if (captureState == CAMERAS_PROCESSING) {
			if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
				// captureState = CAMERAS_PROCESSING;
			}
		}

	}

	private void restoreCamerasStateAfterFailure() {
		if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
			releaseCameras();		
			G4P.showMessage(parent, messageContainer.getText("sw.fails"), "", G4P.WARNING);
		} else if (!gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
//			resetCamerasFailingA();
//			ignoreNextPhotoB = true;
			G4P.showMessage(parent, messageContainer.getText("sw.failsA"), "", G4P.WARNING);
		} else if (gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
//			resetCamerasFailingB();
//			ignoreNextPhotoA = true;
			G4P.showMessage(parent, messageContainer.getText("sw.failsB"), "", G4P.WARNING);
		}
		captureState = CAMERAS_IDLE;
	}

	public void pressCameras() {

		lastCameraAAction = parent.millis();
		lastCameraBAction = parent.millis();

		parent.println("Press Cameras by OSC");
		
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('P');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void releaseCameras() {

		lastCameraAAction = parent.millis();
		lastCameraBAction = parent.millis();

		parent.println("Release Cameras by OSC");
		
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('R');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void releaseAndShutterCameras() {

		lastCameraAAction = parent.millis();
		lastCameraBAction = parent.millis();

		parent.println("shutter cameras");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('W');
		oscP5.send(myMessage, arduinoDriverLocation);

		parent.println("Release And Shutters Cameras by OSC");
		
	}

	public void clickCamera() {

		lastCameraAAction = parent.millis();
		lastCameraBAction = parent.millis();

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('S');
		oscP5.send(myMessage, arduinoDriverLocation);
		
		parent.println("Shutter Cameras by OSC");
	}

	public void resetCamerasFailingA() {

		// COMMENT THIS BECAUSE CAN ENTER IN LOOP, IF FAIL THIS WE TRY TO RESET
		// IF NEEDED HERE ADD A COUNTER TO STOP
		// lastCameraAAction = parent.millis();

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Y');
		oscP5.send(myMessage, arduinoDriverLocation);
		
		parent.println("Reset Camera A failing by OSC");

	}

	public void resetCamerasFailingB() {

		// lastCameraBAction = parent.millis();
		//
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Z');
		oscP5.send(myMessage, arduinoDriverLocation);
		
		parent.println("Reset Camera B failing by OSC");

	}

	// G4P code for message dialogs
	public void handleMessageDialog(String title, String message, int type) {
		// Determine message type
		G4P.showMessage(parent, message, title, type);
	}

	public boolean isAllMirrorsReady() {
		return !gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp;
	}

	public void clearPreviews() {

	}

	public String msg(String key) {
		return messageContainer.getText(key);
	}

}
