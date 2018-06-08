import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import netP5.NetAddress;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;

public class ManuCaptureContext {


	PrintWriter logOutput;

	
	ManuCapture_v1_1 parent;

	RawFile thumbnail;

	G2P5 gphotoA;
	G2P5 gphotoB;

	OscP5 oscP5;

	GUI gui;

	NetAddress viewerLocation;

	Project project;

	boolean renderLeft = true;
	boolean renderRight = true;

	String appPath = "/home/factum/git/book_scanner/bookScanner/Manucapture";

	int rotA = 270;
	int rotB = 90;

	List<HotArea> pointsLeft = new ArrayList<>();
	List<HotArea> pointsRight = new ArrayList<>();

	int wImageViewerSize = 1000;
	int hImageViewerSize = 667;

	// width resolution for viewer
	public int viewerWidthResolution = 3000;
	
	boolean cameraActiveA = false;
	boolean cameraActiveB = false;

	String serialCameraA;
	String serialCameraB;

	String newImagePathA = "";
	String newImagePathB = "";

	String lastImagePathA = "";
	String lastImagePathB = "";
	
	public G2P5 createG2P5(String serial, String name) {
		G2P5 g2p5 = G2P5.create(parent, serial, name);
		g2p5.setTargetFile(parent.homeDirectory(), "test");
		return g2p5;
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
		parent.println("delete all " + suf + "files " + targetFilePath);
		File storageDir = new File(targetFilePath);

		if (!storageDir.exists()) {
			storageDir.mkdirs();
		}

		for (File tempFile : storageDir.listFiles()) {
			if (tempFile.getName().endsWith(suf))
				tempFile.delete();
		}
		parent.println("end delete all " + suf + "files " + targetFilePath);
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

}
