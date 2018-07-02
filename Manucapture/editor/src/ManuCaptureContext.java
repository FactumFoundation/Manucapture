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
import processing.data.XML;

public class ManuCaptureContext {

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

	Project project;

	boolean renderLeft = true;
	boolean renderRight = true;

	String appPath = "/home/factum/git/book_scanner/bookScanner/Manucapture";

	int rotationA = 270;
	int rotationB = 90;

	List<HotArea> pointsLeft = new ArrayList<>();
	List<HotArea> pointsRight = new ArrayList<>();

	int wImageViewerSize = 1000;
	int hImageViewerSize = 667;

	float scaleA = 1;
	float scaleB = 1;

	// width resolution for viewer
	public int viewerWidthResolution = 3000;

	boolean cameraActiveA = false;
	boolean cameraActiveB = false;

	String serialCameraA;
	String serialCameraB;

	XML serialXMLA;
	XML serialXMLB;

	String newImagePathA = "";
	String newImagePathB = "";

	String lastImagePathA = "";
	String lastImagePathB = "";

	public NetAddress arduinoDriverLocation;

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

		releaseCameras();
	}

	public static int CAMERAS_INACTIVE = -1;
	public static int CAMERAS_IDLE = 0;
	public static int CAMERAS_FOCUSSING = 1;
	public static int CAMERAS_MIRROR_UP = 2;

	int captureState = CAMERAS_INACTIVE;
	int counterEvent = 0;

	long lastCaptureMillis = 0;

	public void capture() {
		// Init capture secuence
		if (captureState == CAMERAS_IDLE) {
			captureState = CAMERAS_FOCUSSING;
			pressCameras();
			lastCaptureMillis = parent.millis();
		} else {
			if (captureState == CAMERAS_INACTIVE) {
				G4P.showMessage(parent, "Can't Trigger, cameras are not active", "", G4P.WARNING);
			}
		}
	}

	public void processCamerasEvent(G2P5Event event) {

	}

	public void camerasStateMachineLoop() {

		if (captureState == CAMERAS_INACTIVE && gphotoA.active && gphotoB.active) {
			captureState = CAMERAS_IDLE;
		}

		if (captureState == CAMERAS_FOCUSSING) {
			if (gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
				releaseAndShutterCameras();
				captureState = CAMERAS_MIRROR_UP;
//				if (parent.state == parent.CHART) {
//				}
			} else {
				if (parent.millis() - lastCaptureMillis > 5000) {
					parent.println("Lsa dos cámaras no están dispuestas a poner el mirror en up");
					if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
						releaseCameras();
					} else if (!gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
						resetCamerasFailingA();
					} else if (gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
						resetCamerasFailingB();
					}
					captureState = CAMERAS_IDLE;
				}
			}
		}
		if (captureState == CAMERAS_MIRROR_UP) {
			if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
				captureState = CAMERAS_IDLE;
			}
		}

	}

	public void pressCameras() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('P');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void releaseCameras() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('R');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void releaseAndShutterCameras() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('W');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void clickCamera() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('S');
		oscP5.send(myMessage, arduinoDriverLocation);
	}

	public void resetCamerasFailingA() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Y');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	public void resetCamerasFailingB() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Z');
		oscP5.send(myMessage, arduinoDriverLocation);

	}

	// G4P code for message dialogs
	public void handleMessageDialog(String title, String message, int type) {
		// Determine message type
		G4P.showMessage(parent, message, title, type);
	}

}
