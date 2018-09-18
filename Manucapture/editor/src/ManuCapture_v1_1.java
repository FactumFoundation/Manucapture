
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
import g4p_controls.GButton;
import g4p_controls.GCScheme;
import g4p_controls.GEvent;
import g4p_controls.GImageButton;
import g4p_controls.GTextField;
import g4p_controls.GWinData;
import g4p_controls.GWindow;
import netP5.NetAddress;
import netP5.StringUtils;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;
import processing.event.MouseEvent;

public class ManuCapture_v1_1 extends PApplet {

	/*
	 * ManuCapture.pde A Visual tool for recording books using DSLR Cameras
	 * 
	 * This source file is part of the ManuCapture software For the latest info, see
	 * http://www.factumfoundation.org/pag/235/Digitisation-of-oriental-
	 * manuscripts-in-Daghestan
	 * 
	 * Copyright (c) 2016-2018 Jorge Cano, Enrique Esteban and Eduardo Moriana in Factum Foundation
	 * 
	 * This program is free software; you can redistribute it and/or modify it under
	 * the terms of the GNU General Public License as published by the Free Software
	 * Foundation; either version 2 of the License, or (at your option) any later
	 * version.
	 * 
	 * This program is distributed in the hope that it will be useful, but WITHOUT
	 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
	 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
	 * details.
	 * 
	 * You should have received a copy of the GNU General Public License along with
	 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
	 * Place, Suite 330, Boston, MA 02111-1307 USA
	 */
	

	
	GUI gui;
	GUIController guiController;
	ItemsGUI itemsGUI;
	ContentGUI contentGUI;
	MessageContainer messageContainer;

	String appPath = null;
	String baseDirectory = "";
	Project project = null;
	PrintWriter logOutput;
	
	OscP5 oscP5;
	NetAddress viewerLocation;

	int receivePort = 3334;
	int sendPort = 3333;
	int arduinoDriverPort = 13000;
	boolean arduinoConnected = true;
	
	public static int STATE_APP_NO_PROJECT = 0;
	public static int STATE_APP_PROJECT = 1;
	public static int STATE_APP_EDITING_PROJECT = 2;
	private int stateApp = STATE_APP_NO_PROJECT;
	boolean loading = false;

	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_TO_EVENT = 3000;
	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_CAPTURE_MACHINE_STATE = 5000;
	
	G2P5 gphotoA;
	G2P5 gphotoB;
	G2P5ManucaptureAdapter gphotoAAdapter;
	G2P5ManucaptureAdapter gphotoBAdapter;

	int rotationA = 270;
	int rotationB = 90;

	// width resolution for viewer
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

	public static int CAMERAS_INACTIVE = -1;
	public static int CAMERAS_IDLE = 0;
	public static int CAMERAS_FOCUSSING = 1;
	public static int CAMERAS_MIRROR_UP = 2;
	public static int CAMERAS_PROCESSING = 3;
	public static int CAMERAS_RECOVERING = 4;
	private int captureState = CAMERAS_IDLE;

	long lastCaptureMillis = 0;

	public boolean ignoreNextPhotoA = false;
	public boolean ignoreNextPhotoB = false;

	long lastCameraAAction = -1;
	long lastCameraBAction = -1;

	int shutterMode = 0;
	static int NORMAL_SHUTTER = 0;
	static int REPEAT_SHUTTER = 1;
	static int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;

	int liveViewActive = -1;

	boolean mock = false;

	// *********************
	boolean initSelectedItem = false;
	
	// Chart identification
	public static int STATE_CAPTURING = 0;
	public static int STATE_CHART = 1;
	int cameraState = STATE_CAPTURING;

	int chartStateMachine = 0;
	boolean cropMode = false;

	
	public void setup() {

		System.setOut(new TracingPrintStream(System.out));

		// OSC comms to Arduino Driver
		oscP5 = new OscP5(this, receivePort);
		viewerLocation = new NetAddress("127.0.0.1", sendPort);
		arduinoDriverLocation = new NetAddress("127.0.0.1", arduinoDriverPort);

	    // Project
		appPath = sketchPath() + "/..";
		project = new Project();
		project.context = this;
		String home = homeDirectory();
		File file = new File(home);
		if (!file.exists()) {
			file.mkdir();
		}
		
		// Cameras
		XML serialXML = loadXML("cameraSerials.xml");
		serialCameraA = serialXML.getChild("Camera_A").getContent();
		serialCameraB = serialXML.getChild("Camera_B").getContent();
		serialXMLA = serialXML.getChild("Camera_A");
		serialXMLB = serialXML.getChild("Camera_B");
		String rotA = serialXML.getChild("Camera_A").getString("rotation");
		String rotB = serialXML.getChild("Camera_B").getString("rotation");
		if (rotA != null)
			rotationA = Integer.parseInt(rotA);
		if (rotB != null)
			rotationB = Integer.parseInt(rotB);
		System.out.println("camera A rotation" + rotA);
		System.out.println("camera B rotation" + rotB);
		if (mock) {
			gphotoA = G2P5MockDisk.create(this, serialCameraA, "A");
			gphotoAAdapter.setTargetFile(homeDirectory(), "test");
			gphotoB = G2P5MockDisk.create(this, serialCameraB, "B");
			gphotoBAdapter.setTargetFile(homeDirectory(), "test");
		} else {
			G2P5Manager.init(0);
			gphotoAAdapter = createG2P5(serialCameraA, "A");
			gphotoBAdapter = createG2P5(serialCameraB, "B");
			gphotoA = gphotoAAdapter.g2p5;
			gphotoB = gphotoBAdapter.g2p5;
			gphotoA.listener = gphotoAAdapter;
			gphotoB.listener = gphotoBAdapter;
			gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		}

		// Init GUI
		// Main window position
		surface.setTitle("ManuCapture v1");
		if (surface != null) {
			surface.setLocation(0, 0);
		}
		// Custom GUIs
		contentGUI = new ContentGUI(this);
		itemsGUI = new ItemsGUI(this);
		// Buttons and text imputs
		messageContainer = new MessageContainer();
		messageContainer.init();
		guiController = new GUIController(this);
		gui = new GUI();
		gui.createGUI(this);		
		// Final graphics conf
		setStateApp(STATE_APP_NO_PROJECT);
		registerMethod("post", this);
		background(backgroundColor);
		frameRate(25);
	}
	
	public void post() {
		if (frameCount < 10) {
			background(backgroundColor);
			text("LOADING...", width / 2, height / 3);
		}
	}

	public void newPhotoEvent(G2P5Event event, String ic) {

		println("New photo Event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", event.content);
		
		if (project.projectName == null || project.projectName.equals("")) {
			handleMessageDialog("Error", "Can't capture photos without project name", G4P.ERROR);
			return;
		}

		if (event.g2p5 == gphotoA) {
			if (ignoreNextPhotoA) {
				println("Ignoring A");
				ignoreNextPhotoA = false;
			} else {
				gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				gphotoAAdapter.setFullTargetPath(ic);
				moveFile(event.content, gphotoAAdapter.getFullTargetPath());
				newImagePathA = gphotoAAdapter.getFullTargetPath();
			}
		} else if (event.g2p5 == gphotoB) {
			if (ignoreNextPhotoB) {
				println("Ignoring B");
				ignoreNextPhotoB = false;
			} else {
				gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				gphotoBAdapter.setFullTargetPath(ic);
				moveFile(event.content, gphotoBAdapter.getFullTargetPath());
				newImagePathB = gphotoBAdapter.getFullTargetPath();
			}
		}

		// Adding new Item!! Here
		
		if ((gphotoA.isConnected() && gphotoB.isConnected()
				&& (!newImagePathA.equals("") && !newImagePathB.equals("")))
				// this allows use only one camera if the other is inactive
				|| (gphotoA.isConnected() && !gphotoB.isConnected()
						&& !newImagePathA.equals(""))
				// this allows use only one camera if the other is inactive
				|| (!gphotoA.isConnected() && gphotoB.isConnected()
						&& !newImagePathB.equals(""))) {
			if (shutterMode == NORMAL_SHUTTER) {
				doNormalShutter(Item.TYPE_ITEM);
			} else if (shutterMode == REPEAT_SHUTTER) {
				if (project.items.size() > 0) {
					float newPageNum = project.selectedItem.pagNum;
					Item newItem = initNewItem(project.selectedItem.type, newPageNum);
					newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					clearPaths();
				}
			} else if (shutterMode == CALIB_SHUTTER) {
				println("Calib shutter");
				if (chartStateMachine == 1) {
					// we do background and first photo normally
					doNormalShutter(Item.TYPE_CHART);
					contentGUI.updateLastPreviews();
					chartStateMachine++;
				} else if (chartStateMachine == 2) {
					float newPageNum = project.selectedItem.pagNum;
					Item newItem = initNewItem(Item.TYPE_CHART, newPageNum);
					contentGUI.imgPreviewLeft = newItem.loadLeftPreview(project.projectDirectory,
							project.projectDirectory + newItem.mImageLeft.imagePath);
					contentGUI.lastLeftPreview = contentGUI.imgPreviewLeft;
					// newItem.mImageLeft.remove();
					newItem.mImageLeft.imagePath = "";
					newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					clearPaths();
					chartStateMachine++;
				} else {
					guiController.normal_shutter_click1(null, null);
				}
			}
		}
	}


	public void draw() {
		background(75);
		if (getStateApp() == STATE_APP_NO_PROJECT) {
			if (!arduinoConnected) {
				drawErrorWindow();
			} else {
				drawInitWindow();
			}
		} else if (getStateApp() == STATE_APP_PROJECT) {
			drawApp();
			fill(255);
			textAlign(CENTER);
			textSize(18);
		} else if(getStateApp() == STATE_APP_EDITING_PROJECT) {
			fill(155, 255);
			rect(0, 0, width, height);
			fill(255, 0, 0);
		}
		if (loading) {
			fill(0, 200);
			rect(0, 0, width, height);
			fill(255, 0, 0);
			text("LOADING...", width / 2, height / 3);
		}
	}

	private void drawErrorWindow() {
		background(100, 0, 0);
		pushStyle();
		fill(255);
		textSize(32);
		textAlign(CENTER);
		text(msg("sw.failsSerial"), width / 2, 250);
		popStyle();
	}
	
	private void drawInitWindow() {
		// MOSTRAR CARGAR O NUEVO
		// ellipse(width/2,500,1000,1000);
		textAlign(CENTER, CENTER);

		PVector m = new PVector(mouseX, mouseY);
		float dist = m.dist(new PVector(width / 2, 500));
		float dist1 = m.dist(new PVector(width / 2 + width / 4, 500));
		float dist2 = m.dist(new PVector(width / 2 - width / 4, 500));

		fill(255);
		textSize(32);
		text(msg("sw.name"), width / 2, 250);
		textSize(18);
		text(msg("factum.name") + " " + msg("sw.version"), width / 2, height - 30);
		textSize(20);

		if (dist2 < 100)
			fill(100);
		else
			fill(255);

		ellipse(width / 2 - width / 4, 500, 200, 200);
		fill(0);
		text(msg("sw.newproject"), width / 2 - width / 4, 500);

		if (dist < 100)
			fill(100);
		else
			fill(255);

		ellipse(width / 2, 500, 200, 200);
		fill(0);
		text(msg("sw.lastproject"), width / 2, 500);

		if (dist1 < 100)
			fill(100);
		else
			fill(255);
		ellipse(width / 2 + width / 4, 500, 200, 200);
		fill(0);
		text(msg("sw.openproject"), width / 2 + width / 4, 500);

		if (mousePressed && !loading) {
			if (dist < 100) {
				loading = true;
				gui.grpAll.setVisible(0, true);
				thread("loadLastSessionData");
			}
			if (dist1 < 100) {
				loading = true;
				thread("load_click");
			}
			if (dist2 < 100) {
				guiController.new_button_click(null, null);
			}
		}
	}

	private void drawApp() {

		camerasStateWatchDog();

		if (liveViewActive == 1) {
			gphotoA.setActive(false);
			gphotoB.setActive(false);
			G2P5.killAllGphotoProcess();
			String command = appPath + "/GPhotoLiveView/bin/GPhotoLiveViewer_debug";
			try {
				Process process = Runtime.getRuntime().exec(command);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				OscMessage myMessage = new OscMessage("/cameraPorts");
				println(gphotoA.getPort(), gphotoB.getPort());
				if (gphotoA.getPort() == null || gphotoB.getPort() == null) {
					G4P.showMessage(this, msg("sw.nocamera"), "", G4P.WARNING);
				} else {
					myMessage.add(gphotoA.getPort()); /* add an int to the osc message */
					myMessage.add(gphotoB.getPort()); /* add an int to the osc message */
					oscP5.send(myMessage, viewerLocation);
					process.waitFor();
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				G2P5.killAllGphotoProcess();

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (!gphotoA.isConnected()) {
					guiController.camera_A_active_button_click(null, null);
				}
				if (!gphotoB.isConnected()) {
					guiController.camera_B_active_button_click(null, null);
				}

				gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);

				liveViewActive = -1;

				gui.btnLiveView.setEnabled(true);
				gui.btnLiveView.setVisible(true);
			}
		}

		fill(backgroundColor);

		// CAMERA STATE SECTION
		// ///////////////////////////////////////////////////////

		/*
		if (gphotoA.isConnected()) {
			gui.camera_A_connected_button.setText("CONNECTED");
			gui.camera_A_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			gui.camera_A_connected_button.setText("DISCONNECTED");
			gui.camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		if (gphotoB.isConnected()) {
			gui.camera_B_connected_button.setText("CONNECTED");
			gui.camera_B_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			gui.camera_B_connected_button.setText("DISCONNECTED");
			gui.camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}
		*/

		itemsGUI.drawItemsViewPort();
		contentGUI.draw();
		stroke(255);
		textAlign(LEFT);
		pushStyle();
		//textSize(24);
		//fill(255);
		//text("Project " + project.projectName, 40, 35);
		fill(255);
		textSize(18);
		text("Project code " + project.projectCode, 40, 30);
		text("Camera status", width-155, 30);
		popStyle();
		textSize(16);
		fill(255, 0, 0);
		if (liveViewActive == 0) {
			fill(0, 200);
			rect(0, 0, width, height);
			textSize(32);
			fill(255, 0, 0);
			text(msg("sw.liveviewenable"), width / 2 - 100, height / 2);
			liveViewActive++;
		} 
		if (shutterMode == NORMAL_SHUTTER && gui.btnTriggerRepeat.isVisible()) {
			gui.btnTriggerRepeat.setVisible(false);
		}
		if (shutterMode == REPEAT_SHUTTER && gui.btnTriggerNormal.isVisible()) {
			gui.btnTriggerNormal.setVisible(false);
		}
		
		
		// datos de cámara
		if (!gphotoB.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(width-54, 69, 30, 30);

		// datos de cámara
		if (!gphotoA.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(width-125, 69, 30, 30);
		
		// trigger button color
		if (isAllMirrorsReady()) {
			fill(0, 255, 0);
		} else {
			fill(255, 0, 0);
		}
		ellipse(width - 88, 962, 50, 50);
	}

	public void mouseMoved() {
		itemsGUI.mouseMoved();
		contentGUI.mouseMoved();
	}

	public void startCropMode() {
		cropMode = !cropMode;
	}

	public void mousePressed() {
		contentGUI.mousePressed();
		itemsGUI.mousePressed();
	}

	public void mouseWheel(MouseEvent event) {
		contentGUI.mouseWheel(event.getCount());
	}

	public void keyPressed() {
		if (key == ' ') {
			guiController.trigger_button_click(null, null);
		}
	}

	public void mouseDragged() {
		itemsGUI.mouseDragged();
		contentGUI.mouseDragged();
	}

	public void mouseReleased() {
		itemsGUI.mouseReleased();
		// hotAreaSelected = null;
	}

	private void doNormalShutter(String type) {
		float newPageNum;
		if (project.selectedItemIndex < 0) {
			newPageNum = 1;
		} else {
			newPageNum = (int) project.items.get(project.selectedItemIndex).pagNum + 1;
		}

		Item newItem = initNewItem(type, newPageNum);
		newItem.saveMetadata();
		newItem.loadThumbnails();
		project.addItem(project.selectedItemIndex + 1, newItem);
		clearPaths();
	}

	private Item initNewItem(String type, float newPageNum) {
		if (project.projectDirectory.equals("")) {
			clearPaths();
		}
		String relNewImagePathA = "";
		if (!newImagePathA.equals("")) {
			relNewImagePathA = newImagePathA.substring(project.projectDirectory.length());
		}
		String relNewImagePathB = "";
		if (!newImagePathB.equals(""))
			relNewImagePathB = newImagePathB.substring(project.projectDirectory.length());
		// TODO here we decide what is in the left and the right
		Item newItem = new Item(this, relNewImagePathA, relNewImagePathB, newPageNum, "", type);
		return newItem;
	}

	public void loadLastSessionData() {
		guiController.normal_shutter_click1(null, null);
		String value;
		try {
			XML lastSessionData = loadXML("lastSession.xml");

			File projectFile = new File(lastSessionData.getChild("Project").getContent());
			if (projectFile.exists())
				loadProject(projectFile.getPath());
			else
				println("Error loading the project: Project file doesn't exist");

			project.selectedItemIndex = new Integer(lastSessionData.getChild("Current_Item").getContent());

			value = lastSessionData.getChild("Camera_A_Active").getContent();
			if (value.equals("1") && !gphotoA.isConnected())
				guiController.camera_A_active_button_click(null, null);

			value = lastSessionData.getChild("Camera_B_Active").getContent();
			if (value.equals("1") && !gphotoB.isConnected())
				guiController.camera_B_active_button_click(null, null);

			project.forceSelectedItem(project.selectedItemIndex, false);

			gui.grpAll.setVisible(1, true);

			loading = false;

			contentGUI.noZoom();

			if (project.items.isEmpty()) {
				contentGUI.initCropHotAreas();
			}

		} catch (Exception e) {
			_println("lastSession.xml not found");
			// txtLog.insertText("Error reconstructing last session: check the
			// integrity of your session folder ");
			e.printStackTrace();
			G4P.showMessage(this, msg("sw.errorloadingproject"), "", G4P.WARNING);
			loading = false;
		}
	}

	public void saveLastSessionData() {

		String value;

		XML lastSessionData = new XML("Session");

		XML lastdocumentFileName = new XML("Project");
		lastdocumentFileName.setContent(project.projectFilePath);
		lastSessionData.addChild(lastdocumentFileName);

		XML lastCurrentItem = new XML("Current_Item");
		lastCurrentItem.setContent(String.valueOf(project.selectedItemIndex));
		lastSessionData.addChild(lastCurrentItem);

		XML lastCameraActiveA = new XML("Camera_A_Active");

		if (gphotoA.isConnected())
			value = "1";
		else
			value = "0";
		lastCameraActiveA.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraActiveA);

		if (gphotoB.isConnected())
			value = "1";
		else
			value = "0";
		XML lastCameraActiveB = new XML("Camera_B_Active");
		lastCameraActiveB.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraActiveB);

		saveXML(lastSessionData, "data/lastSession.xml");
	}

	/*
	 * Project management
	 * 
	 */

	public synchronized void createProject(String projectFolderPath) {
		if (!project.projectFilePath.equals("")) {
			project.closeProject();
			// gui.page_comments_text.setText("");
			// gui.page_num_text.setText("0");

		}
		XML projectDataXML = loadXML("project_template.xml");
		project.loadProjectMetadata(projectDataXML);

		project.rotationA = rotationA;
		project.rotationB = rotationB;

		project.serialA = serialCameraA;
		project.serialB = serialCameraB;

		saveXML(projectDataXML, projectFolderPath + "/project.xml");

		File thumbnailsFolder = new File(projectFolderPath + "/thumbnails");
		if (!thumbnailsFolder.exists()) {
			if (thumbnailsFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + thumbnailsFolder.getPath());
				} catch (Exception e) {
					_println("Couldn't create thumbnail directory permisions");
				}
			} else {
				_println("Failed to create thumbnail directory!");
			}
		}
		File rawFolder = new File(projectFolderPath + "/raw");
		if (!rawFolder.exists()) {
			if (rawFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + rawFolder.getPath());
				} catch (Exception e) {
					_println("Couldn't create raw directory permisions");
				}
			} else {
				_println("Failed to create thumbnail directory!");
			}
		}
		project.projectDirectory = projectFolderPath;
		project.projectFilePath = projectFolderPath + "/project.xml";
		project.selectedItemIndex = -1;
		project.thumbnailsLoaded = true;

		gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		setCaptureState(CAMERAS_IDLE);

		saveLastSessionData();
		gui.project_info.setText("PROJECT INFO " + project.projectFilePath);
	}

	public void loadProject(String projectPath) {
		project.selectedItem = null;
		project.loadProjectMethod(projectPath);
		String errors = "";
		if (project.rotationA != rotationA) {
			errors += msg("sw.rotationAChanged") + project.rotationA + "->" + rotationA + "\n";
		}
		if (project.rotationB != rotationB) {
			errors += msg("sw.rotationBChanged") + project.rotationB + "->" + rotationB + "\n";
		}
		if (!project.serialA.equals(serialCameraA)) {
			errors += msg("sw.serialAChanged") + project.serialA + "->" + serialCameraA + "\n";
		}
		if (!project.serialB.equals(serialCameraB)) {
			errors += msg("sw.serialBChanged") + project.serialB + "->" + serialCameraB + "\n";
		}
		if (!errors.equals("")) {
			G4P.showMessage(this, errors, "", G4P.WARNING);
		}
		initSelectedItem = true;
		gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		setCaptureState(CAMERAS_IDLE);
		G2P5Manager.setImageCount(project.items.size());
		project.forceSelectedItem(project.items.size(), false);
		if (project.items.isEmpty()) {
			contentGUI.initCropHotAreas();
		}
		saveLastSessionData();
		project.removeUnusedImages();
		setStateApp(STATE_APP_PROJECT);
	}

	public String homeDirectory() {
		String pathApp = System.getProperty("user.home") + "/.manucapture";
		return pathApp;
	}

	public void settings() {
		size(1920, 1080, P2D);
	}

	public void oscEvent(OscMessage theOscMessage) {
		print("### received an osc message.");
		print(" addrpattern: " + theOscMessage.addrPattern());
		println(" typetag: " + theOscMessage.typetag());
		if (theOscMessage.addrPattern().equals("/error")) {
			arduinoConnected = false;
			println("Problem opening Serial Port");
		} else {
			capture();
		}
	}
	
	public G2P5ManucaptureAdapter createG2P5(String serial, String name) {
		G2P5 g2p5 = G2P5.create(homeDirectory(), serial, name);
		G2P5ManucaptureAdapter adapter = new G2P5ManucaptureAdapter(this, g2p5);
		adapter.setTargetFile(project.projectDirectory + "raw", project.projectName);
		return adapter;
	}

	public void deleteAllFiles(String targetFilePath, String suf) {
		// println("delete all " + suf + "files " + targetFilePath);
		File storageDir = new File(targetFilePath);

		if (!storageDir.exists()) {
			storageDir.mkdirs();
		}

		for (File tempFile : storageDir.listFiles()) {
			if (tempFile.getName().endsWith(suf))
				tempFile.delete();
		}
		// println("end delete all " + suf + "files " + targetFilePath);
	}

	public void clearPaths() {
		lastImagePathA = newImagePathA;
		lastImagePathB = newImagePathB;
		newImagePathA = "";
		newImagePathB = "";
	}

	public void deleteFile(String targetFilePath) {
		String commandGenerate = "rm " + targetFilePath;
		println(commandGenerate);
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
		int s = second(); // Values from 0 - 59
		int min = minute(); // Values from 0 - 59
		int h = hour(); // Values from 0 - 23
		int d = day(); // Values from 1 - 31
		int m = month(); // Values from 1 - 12
		int y = year(); // 2003, 2004, 2005, etc.
		String date = d + "/" + m + "/" + y + "-" + h + ":" + min + ":" + s;
		if (logOutput == null) {
			logOutput = createWriter("log_" + date + ".txt");
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

	public void capture() {
		// Init capture secuence
		if (getCaptureState() == CAMERAS_IDLE) {
			if (gphotoA.isConnected() && gphotoB.isConnected()) {
				setCaptureState(CAMERAS_FOCUSSING);
			} else {
				G4P.showMessage(this, messageContainer.getText("sw.notconnected"), "", G4P.WARNING);
			}
		} else {
			if (getCaptureState() == CAMERAS_INACTIVE) {
				G4P.showMessage(this, messageContainer.getText("sw.notready"), "", G4P.WARNING);
			}
		}
	}

	public void camerasStateWatchDog() {

		// Check response of individual cameras 
		/*if (lastCameraAAction > 0 && gphotoAAdapter.lastEventMillis > lastCameraAAction + MAX_TIME_TO_EVENT) {
			println("Camera A not responding to actions");
			G4P.showMessage(this, messageContainer.getText("sw.noeventA"), "", G4P.WARNING);
			lastCameraAAction = -1;
		}
		//
		if (lastCameraBAction > 0 && gphotoBAdapter.lastEventMillis > lastCameraBAction + MAX_TIME_TO_EVENT) {
			println("Camera B not responding to actions");
			G4P.showMessage(this, messageContainer.getText("sw.noeventB"), "", G4P.WARNING);
			lastCameraBAction = -1;
		}
		*/
		
		//Check general timing in Capture State to solve the triggering
		if (getCaptureState() == CAMERAS_INACTIVE && gphotoA.active && gphotoB.active) {
			setCaptureState(CAMERAS_IDLE);
		} 
		else if (getCaptureState() == CAMERAS_FOCUSSING) {
			if (gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
				setCaptureState(CAMERAS_MIRROR_UP);
			} else if(millis() > lastCaptureMillis + MAX_TIME_CAPTURE_MACHINE_STATE) {
				setCaptureState(CAMERAS_RECOVERING);
			}
		} else if (getCaptureState() == CAMERAS_MIRROR_UP) {
			if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
				setCaptureState(CAMERAS_IDLE);
			}
		} else if (getCaptureState() == CAMERAS_RECOVERING) {
			if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
				setCaptureState(CAMERAS_IDLE);
			} else if(millis() > lastCaptureMillis + MAX_TIME_CAPTURE_MACHINE_STATE) {
				if(gphotoAAdapter.mirrorUp) {
					clickCameraA();
					ignoreNextPhotoA = true;
				}
				if(gphotoBAdapter.mirrorUp) {
					clickCameraB();
					ignoreNextPhotoB = true;
				}
				setCaptureState(CAMERAS_IDLE);
			}
		} else if(getCaptureState() == CAMERAS_IDLE) {
			
		}
	}


	int getCaptureState() {
		return captureState;
	}

	void setCaptureState(int captureState) {

		if(captureState == CAMERAS_RECOVERING) {
			restoreCamerasStateAfterFailure();
		}
		if (captureState == CAMERAS_FOCUSSING){
			pressCameras();
		}
		else if(captureState == CAMERAS_MIRROR_UP) {
			releaseAndShutterCameras();
		} else if(captureState == CAMERAS_IDLE) {

		}
		lastCaptureMillis = millis();
		this.captureState = captureState;
	}

	
	private void restoreCamerasStateAfterFailure() {
		if (!gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
			releaseCameras();		
			G4P.showMessage(this, messageContainer.getText("sw.fails"), "", G4P.WARNING);
		} else if (!gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
			clickCameraB();
			ignoreNextPhotoB = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsA"), "", G4P.WARNING);
		} else if (gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
			clickCameraA();
			ignoreNextPhotoA = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsB"), "", G4P.WARNING);
		}
	}

	public void pressCameras() {
		lastCameraAAction = millis();
		lastCameraBAction = millis();
		println("Press Cameras by OSC");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('P');
		oscP5.send(myMessage, arduinoDriverLocation);
	}

	public void releaseCameras() {
		lastCameraAAction = millis();
		lastCameraBAction = millis();
		println("Release Cameras by OSC");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('R');
		oscP5.send(myMessage, arduinoDriverLocation);
	}

	public void releaseAndShutterCameras() {
		lastCameraAAction = millis();
		lastCameraBAction = millis();
		println("shutter cameras");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('W');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Release And Shutters Cameras by OSC");
	}

	public void clickCamera() {
		lastCameraAAction = millis();
		lastCameraBAction = millis();
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('S');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Shutter Cameras by OSC");
	}

	public void clickCameraB() {
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Y');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Reset Camera A failing by OSC");
	}

	public void clickCameraA() {
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Z');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Reset Camera B failing by OSC");
	}

	public void handleMessageDialog(String title, String message, int type) {
		// Determine message type
		G4P.showMessage(this, message, title, type);
	}

	public boolean isAllMirrorsReady() {
		return !gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp;
	}

	public String msg(String key) {
		return messageContainer.getText(key);
	}
	
	public int getStateApp() {
		return stateApp;
	}

	public void setStateApp(int stateApp) {
		this.stateApp = stateApp;
		contentGUI.noZoom();
		if(stateApp == STATE_APP_NO_PROJECT) {
			gui.grpProject.setVisible(1, false);
			gui.grpAll.setVisible(1, false);
		}
		else if(stateApp == STATE_APP_PROJECT) {
			gui.grpProject.setVisible(1, false);
			gui.grpAll.setVisible(1, true);
		}		
		if(stateApp == STATE_APP_EDITING_PROJECT) {
			gui.grpProject.setVisible(1, true);
			gui.grpAll.setVisible(1, false);
		}
	}


	static public void main(String[] passedArgs) {

		try {
			String[] appletArgs = new String[] { "ArduinoDriver", "" };// ,
																		// location
																		// };
			if (passedArgs != null) {
				PApplet.main(concat(appletArgs, passedArgs));
			} else {
				PApplet.main(appletArgs);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("End of programmm");
		}

		try {
			String[] appletArgs = new String[] { "ManuCapture_v1_1", "--present" };// ,
																					// location
																					// };
			if (passedArgs != null) {
				PApplet.main(concat(appletArgs, passedArgs));
			} else {
				PApplet.main(appletArgs);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("End of programmm");
		}
	}

}
