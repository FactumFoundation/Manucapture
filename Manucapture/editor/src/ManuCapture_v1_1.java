
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import g4p_controls.G4P;
import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PVector;
import processing.data.XML;
import processing.event.MouseEvent;
import boofcv.processing.*;
import boofcv.struct.image.*;
import boofcv.alg.feature.detect.line.*;
import boofcv.factory.feature.detect.line.*;
import georegression.struct.line.*;
import processing.video.*;
import processing.event.KeyEvent;


public class ManuCapture_v1_1 extends PApplet {

	/*
	 * ManuCapture.pde A Visual tool for recording books using DSLR Cameras
	 * 
	 * This source file is part of the ManuCapture software For the latest info, see
	 * http://www.factumfoundation.org/pag/235/Digitisation-of-oriental-
	 * manuscripts-in-Daghestan
	 * 
	 * Copyright (c) 2016-2018 Jorge Cano, Enrique Esteban and Eduardo Moriana in
	 * Factum Foundation
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

	public static final String PAGE_LEFT_NAME = "Left";
	public static final String PAGE_RIGHT_NAME = "Right";

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
	// static public int MAX_TIME_TO_EVENT = 3000;
	// max time waitting from send action to camera and receive event
	static public int MAX_TIME_CAPTURE_MACHINE_STATE = 5000;

	G2P5 gphotoPageRight;
	G2P5 gphotoPageLeft;
	G2P5ManucaptureAdapter gphotoPageRightAdapter;
	G2P5ManucaptureAdapter gphotoPageLeftAdapter;

	int rotationPageRight = 270;
	int rotationPageLeft = 90;

	// width resolution for viewer
	boolean cameraAProcessingNewPhoto = false;
	boolean cameraBProcessingNewPhoto = false;

	String serialCameraPageRight;
	String serialCameraPageLeft;

	XML serialXMLCameraPageRight;
	XML serialXMLCameraPageLeft;

	String newPageRightPath = "";
	String newPageLeftPath = "";

	public NetAddress arduinoDriverLocation;

	public static int CAMERAS_INACTIVE = -1;
	public static int CAMERAS_IDLE = 0;
	public static int CAMERAS_FOCUSSING = 1;
	public static int CAMERAS_MIRROR_UP = 2;
	public static int CAMERAS_PROCESSING = 3;
	public static int CAMERAS_RECOVERING = 4;
	private int captureState = CAMERAS_IDLE;

	long lastCaptureMillis = 0;

	public boolean ignoreNextPageRight = false;
	public boolean ignoreNextPageLeft = false;

	long lastCameraAAction = -1;
	long lastCameraBAction = -1;

	int shutterMode = 0;
	static int NORMAL_SHUTTER = 0;
	static int REPEAT_SHUTTER = 1;
	static int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;

	static int NO_LIVEVIEW = -1;
	static int STOP_LIVEVIEW = -2;
	static int ENABLING_LIVEVIEW =-3;
	static int START_LIVEVIEW = 2;
	static int ON_LIVEVIEW = 1;
	static int DISSABLING_LIVEVIEW = 3;
	int liveViewState = NO_LIVEVIEW;
		
	boolean mock = false;

	// *********************
	boolean initSelectedItem = false;

	// Chart identification
	public static int STATE_CAPTURING = 0;
	public static int STATE_CHART = 1;
	int cameraState = STATE_CAPTURING;

	int chartStateMachine = 0;
	boolean cropMode = false;

	List<LineParametric2D_F32> leftLinesFootSub;
	List<LineParametric2D_F32> rightLinesFootSub;

	int rawW = 5200;
	int rawH = 3468;

	String proyectsRepositoryFolder = null;
	boolean creatingProyect = false;
	//boolean insertCalibItemPrevious = false;
	public String source;
	public String cameraModel;

	// Liveview images
	Movie liveViewLeft;
	Movie liveViewRight;
	
	
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
		serialCameraPageRight = serialXML.getChild("Camera_Page_Right").getContent();
		serialCameraPageLeft = serialXML.getChild("Camera_Page_Left").getContent();
		serialXMLCameraPageRight = serialXML.getChild("Camera_Page_Right");
		serialXMLCameraPageLeft = serialXML.getChild("Camera_Page_Left");
		rawW = serialXML.getInt("raw_width");
		rawH = serialXML.getInt("raw_height");
		source = serialXML.getString("source");
		cameraModel = serialXML.getString("cameraModel");
		proyectsRepositoryFolder = serialXML.getString("repository");
		String rotPageRight = serialXML.getChild("Camera_Page_Right").getString("rotation");
		String rotPageLeft = serialXML.getChild("Camera_Page_Left").getString("rotation");
		if (rotPageRight != null)
			rotationPageRight = Integer.parseInt(rotPageRight);
		if (rotPageLeft != null)
			rotationPageLeft = Integer.parseInt(rotPageLeft);
		System.out.println("rotation of camera for page right " + rotPageRight);
		System.out.println("rotation of camera for page left " + rotPageLeft);

		G2P5.killAllGphotoProcess();
		G2P5Manager.init(0);
		gphotoPageRightAdapter = createG2P5(serialCameraPageRight, PAGE_RIGHT_NAME);
		gphotoPageLeftAdapter = createG2P5(serialCameraPageLeft, PAGE_LEFT_NAME);
		gphotoPageRight = gphotoPageRightAdapter.g2p5;
		gphotoPageLeft = gphotoPageLeftAdapter.g2p5;
		gphotoPageRight.stopLiveView();
		gphotoPageLeft.stopLiveView();
		gphotoPageRight.setNormalConfig();
		gphotoPageLeft.setNormalConfig();
		gphotoPageRight.setActive(true);
		gphotoPageLeft.setActive(true);
		gphotoPageRight.listener = gphotoPageRightAdapter;
		gphotoPageLeft.listener = gphotoPageLeftAdapter;
		gphotoPageRightAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		gphotoPageLeftAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		if (gphotoPageLeftAdapter.g2p5.mock) {
			MAX_TIME_CAPTURE_MACHINE_STATE = 15000;
		}		
				
		liveViewLeft = new Movie(this, gphotoPageLeft.getLiveViewStreamPath());
		liveViewRight = new Movie(this, gphotoPageRight.getLiveViewStreamPath()); 
		
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
	
	public void settings() {
		  size(1920, 1080, "processing.opengl.PGraphics2D");
	}

	public void post() {
		if (frameCount < 10) {
			background(backgroundColor);
			text("LOADING...", width / 2, height / 3);
		}
	}

	public synchronized void newPhotoEvent(G2P5Event event) {

		println("New photo Event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", event.content);
		if (event.g2p5 == gphotoPageRight) {
			if (ignoreNextPageRight) {
				println("Ignoring incoming right page");
				ignoreNextPageRight = false;
			} else {
				int ic = G2P5Manager.getImageCount();
				gphotoPageRightAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				Formatter fmt = new Formatter();
				fmt.format("%04d", ic);
				if (chartStateMachine == 1) {
					gphotoPageRightAdapter.setFullTargetPath(fmt.toString());
				} else {
					gphotoPageRightAdapter.setFullTargetPath(fmt.toString());
				}

				fmt.close();
				moveFile(event.content, gphotoPageRightAdapter.getFullTargetPath());
				newPageRightPath = gphotoPageRightAdapter.getFullTargetPath();
			}
		} else if (event.g2p5 == gphotoPageLeft) {
			if (ignoreNextPageLeft) {
				println("Ignoring incoming left page");
				ignoreNextPageLeft = false;
			} else {
				int ic = G2P5Manager.getImageCount();
				gphotoPageLeftAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				Formatter fmt = new Formatter();
				fmt.format("%04d", ic);
				if (chartStateMachine == 2) {
					gphotoPageLeftAdapter.setFullTargetPath(fmt.toString());
				} else {
					gphotoPageLeftAdapter.setFullTargetPath(fmt.toString());
				}
				fmt.close();
				moveFile(event.content, gphotoPageLeftAdapter.getFullTargetPath());
				newPageLeftPath = gphotoPageLeftAdapter.getFullTargetPath();
			}
		}
		// Adding new Item!! Here
		if ((gphotoPageRight.isConnected() && gphotoPageLeft.isConnected()
				&& (!newPageRightPath.equals("") && !newPageLeftPath.equals("")))
				// this allows use only one camera if the other is inactive
				|| (gphotoPageRight.isConnected() && !gphotoPageLeft.isConnected() && !newPageRightPath.equals(""))
				// this allows use only one camera if the other is inactive
				|| (!gphotoPageRight.isConnected() && gphotoPageLeft.isConnected() && !newPageLeftPath.equals(""))) {
			if (shutterMode == NORMAL_SHUTTER) {
				doNormalShutter(Item.TYPE_ITEM);
				G2P5Manager.addImageCount();
			} else if (shutterMode == REPEAT_SHUTTER) {
				if (project.items.size() > 0) {
					float newPageNum = project.selectedItem.pagNum;
					Item newItem = initNewItem(project.selectedItem.type, newPageNum);
					newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					clearPaths();
					project.selectItem(project.selectedItemIndex);
					shutterMode = ManuCapture_v1_1.NORMAL_SHUTTER;
					chartStateMachine = 0;
					gui.btnRepeat.setState(0);
					G2P5Manager.addImageCount();
					// TODO Restore gui repeat button to off
				}
			} else if (shutterMode == CALIB_SHUTTER) {
				println("Calib shutter");
				if (chartStateMachine == 1) {
					// we do background and first photo normally
					doNormalShutter(Item.TYPE_CHART);
					contentGUI.updateLastPreviews();
					gui.btnColorChart.setEnabled(false);
					gui.btnRepeat.setEnabled(false);
					chartStateMachine++;
					G2P5Manager.addImageCount();
				} else if (chartStateMachine == 2) {
					// project.cleanImageCaches();
					float newPageNum = project.selectedItem.pagNum;
					Item newItem = initNewItem(Item.TYPE_CHART, newPageNum);
					contentGUI.imgPreviewLeft = newItem.loadLeftPreview(project.projectDirectory,
							project.projectDirectory + newItem.mImageLeft.imagePath);
					contentGUI.lastLeftPreview = contentGUI.imgPreviewLeft;
					// newItem.mImageLeft.remove();
					newItem.mImageRight.imagePath = "";
					// newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					project.selectItem(project.selectedItemIndex);
					gui.btnTrigger.setEnabled(false);
					clearPaths();
					newItem.saveMetadata();
					chartStateMachine++;
					G2P5Manager.addImageCount();
				}
			}
			saveLastSessionData();
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
		} else if (getStateApp() == STATE_APP_EDITING_PROJECT) {
			fill(155, 255);
			rect(0, 0, width, height);
			fill(255, 0, 0);
			gui.project_info.setText("PROJECT INFO " + proyectsRepositoryFolder + project.projectCode);
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
				guiController.calibration_shutter_click(null, null);
				gui.btnColorChart.setState(1);
			}
		}
	}

	private void drawApp() {

		camerasStateWatchDog();

		fill(backgroundColor);

		itemsGUI.drawItemsViewPort();
		contentGUI.draw();
		
		//project labels 
		stroke(255);
		textAlign(LEFT);
		pushStyle();
		fill(255);
		textSize(18);
		text("Project code " + project.projectCode, 40, 30);
		text("Camera status", width - 155, 30);
		popStyle();
		textSize(16);
		fill(255, 0, 0);
		
		// Liveview
		liveViewStateMachine();
		
		// Datos de cámara
		if (!gphotoPageLeft.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(width - 54, 69, 30, 30);
		
		if (!gphotoPageRight.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(width - 125, 69, 30, 30);

		// trigger button color
		if (isAllMirrorsReady()) {
			fill(0, 255, 0);
		} else {
			fill(255, 0, 0);
		}
		ellipse(width - 88, 962, 50, 50);
	}

	private void liveViewStateMachine() {
		
		if (liveViewState == START_LIVEVIEW) {
			G2P5.killAllGphotoProcess();
			// SHUTTERSPEED
			gphotoPageRight.setLiveViewConfig();
			gphotoPageLeft.setLiveViewConfig();
			gphotoPageRight.startLiveView();
			gphotoPageLeft.startLiveView();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			liveViewLeft.play();
			liveViewRight.play();
			liveViewState = ON_LIVEVIEW;
		} else if(liveViewState == STOP_LIVEVIEW) {
			// set normal speed and reconnect tethered capture
			// Kill the background process
			G2P5.killAllGphotoProcess();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			liveViewRight.stop();
			liveViewLeft.stop();
			gphotoPageRight.stopLiveView();
			gphotoPageLeft.stopLiveView();
			gphotoPageRight.setNormalConfig();
			gphotoPageLeft.setNormalConfig();
			if (!gphotoPageRight.isConnected()) {
				guiController.camera_page_right_active_button_click(null, null);
			}
			if (!gphotoPageLeft.isConnected()) {
				guiController.camera_page_left_active_button_click(null, null);
			}
			gphotoPageRightAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			gphotoPageLeftAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			exitCropMode();
			gui.btnCrop.setEnabled(true);
			liveViewState = NO_LIVEVIEW;
		} else if(liveViewState == ENABLING_LIVEVIEW) {
			fill(0, 200);
			rect(0, 0, width, height);
			textSize(32);
			fill(255, 0, 0);
			text(msg("sw.liveviewenable"), width / 2 - 100, height / 2);
			liveViewState = START_LIVEVIEW;
		} else if(liveViewState == DISSABLING_LIVEVIEW) {
			fill(0, 200);
			rect(0, 0, width, height);
			textSize(32);
			fill(255, 0, 0);
			text(msg("sw.liveviewdisable"), width / 2 - 100, height / 2);
			liveViewState = STOP_LIVEVIEW;
		}
	}

	public void mouseMoved() {
		itemsGUI.mouseMoved();
		contentGUI.mouseMoved();
	}

	public void toggleCropMode() {
		cropMode = !cropMode;
		if (!cropMode) {
			contentGUI.stopCropMode();
		} else {
			contentGUI.startCropMode();
		}
	}
	
	public void enterCropMode() {
		gui.btnCrop.setState(1);
		cropMode = true;
		contentGUI.startCropMode();
	}
	
	public void exitCropMode() {
		gui.btnCrop.setState(0);
		cropMode = false;
		contentGUI.stopCropMode();
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
		contentGUI.mouseReleased();
	}

	private void doNormalShutter(String type) {
		float newPageNum;
		project.cleanTempImages();
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
		String relNewPageRightPath = "";
		if (!newPageRightPath.equals("")) {
			relNewPageRightPath = newPageRightPath.substring(project.projectDirectory.length());
		}
		String relNewPageLeftPath = "";
		if (!newPageLeftPath.equals(""))
			relNewPageLeftPath = newPageLeftPath.substring(project.projectDirectory.length());
		Item newItem = new Item(this, relNewPageLeftPath, relNewPageRightPath, newPageNum, "", type);
		newItem.removeCache();
		return newItem;
	}

	public void load_click() {
		guiController.load_click(null, null);
	}

	public void loadLastSessionData() {
		String value;
		try {
			XML lastSessionData = loadXML("lastSession.xml");
			File projectFile = new File(lastSessionData.getChild("Project").getContent());
			if (projectFile.exists())
				loadProject(projectFile.getPath());
			else
				println("Error loading the project: Project file doesn't exist");
			project.selectedItemIndex = new Integer(lastSessionData.getChild("Current_Item").getContent());
			value = lastSessionData.getChild("Camera_Page_Right_Active").getContent();
			if (value.equals("1") && !gphotoPageRight.isConnected())
				guiController.camera_page_right_active_button_click(null, null);
			value = lastSessionData.getChild("Camera_Page_Left_Active").getContent();
			if (value.equals("1") && !gphotoPageLeft.isConnected())
				guiController.camera_page_left_active_button_click(null, null);
			project.forceSelectedItem(project.selectedItemIndex, false);
			gui.grpAll.setVisible(1, true);
			loading = false;
			contentGUI.noZoom();
			if (project.items.isEmpty()) {
				contentGUI.initCropGuides();
			}
		} catch (Exception e) {
			_println("lastSession.xml not found");
			e.printStackTrace();
			G4P.showMessage(this, msg("sw.errorloadingproject"), "", G4P.WARNING);
			loading = false;
		}
		shutterMode = NORMAL_SHUTTER;
		setCaptureState(CAMERAS_IDLE);
		chartStateMachine = 0;
		contentGUI.noZoom();
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
		XML lastCameraPageRightActive = new XML("Camera_Page_Right_Active");
		if (gphotoPageRight.isConnected())
			value = "1";
		else
			value = "0";
		lastCameraPageRightActive.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraPageRightActive);
		if (gphotoPageLeft.isConnected())
			value = "1";
		else
			value = "0";
		XML lastCameraPageLeftActive = new XML("Camera_Page_Left_Active");
		lastCameraPageLeftActive.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraPageLeftActive);
		saveXML(lastSessionData, "data/lastSession.xml");
	}

	/*
	 * Project management
	 * 
	 */

	public synchronized boolean createProject(String projectFolderPath) {

		boolean ret = true;

		// We will check if is posible using this name
		String tempProjectPath = proyectsRepositoryFolder + projectFolderPath;

		File file = new File(tempProjectPath);

		if (file.exists()) {
			// we have a problem
			ret = false;
		}

		if (!file.mkdirs()) {
			// we have a problem
			ret = false;
			G4P.showMessage(this, messageContainer.getText("sw.failsrepository") + tempProjectPath, "", G4P.WARNING);

		}

		projectFolderPath = tempProjectPath;

		if (ret) {
			XML projectDataXML = loadXML("project_template.xml");
			project.loadProjectMetadata(projectDataXML);

			project.rotationPageRight = rotationPageRight;
			project.rotationPageLeft = rotationPageLeft;

			project.serialCameraPageRight = serialCameraPageRight;
			project.serialCameraPageLeft = serialCameraPageLeft;

			saveXML(projectDataXML, projectFolderPath + "/project.xml");

			project.projectDirectory = projectFolderPath;
			project.projectFilePath = projectFolderPath + "/project.xml";
			project.selectedItemIndex = -1;
			project.thumbnailsLoaded = true;

			project.createFolderIfNotExist();

			gphotoPageRightAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			gphotoPageLeftAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			setCaptureState(CAMERAS_IDLE);

			saveLastSessionData();

			gui.project_info.setText("PROJECT INFO " + project.projectFilePath);
		}
		return ret;
	}

	public void loadProject(String projectPath) {
		project.selectedItem = null;
		project.loadProjectMethod(projectPath);
		String errors = "";
		if (project.rotationPageRight != rotationPageRight) {
			errors += msg("sw.rotationAChanged") + project.rotationPageRight + "->" + rotationPageRight + "\n";
		}
		if (project.rotationPageLeft != rotationPageLeft) {
			errors += msg("sw.rotationBChanged") + project.rotationPageLeft + "->" + rotationPageLeft + "\n";
		}
		if (!project.serialCameraPageRight.equals(serialCameraPageRight)) {
			errors += msg("sw.serialAChanged") + project.serialCameraPageRight + "->" + serialCameraPageRight + "\n";
		}
		if (!project.serialCameraPageLeft.equals(serialCameraPageLeft)) {
			errors += msg("sw.serialBChanged") + project.serialCameraPageLeft + "->" + serialCameraPageLeft + "\n";
		}
		if (!errors.equals("")) {
			G4P.showMessage(this, errors, "", G4P.WARNING);
		}
		initSelectedItem = true;
		gphotoPageRightAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		gphotoPageLeftAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		setCaptureState(CAMERAS_IDLE);
		project.forceSelectedItem(project.items.size(), false);
		if (project.items.isEmpty()) {
			contentGUI.initCropGuides();
		}
		saveLastSessionData();
		project.removeUnusedImages();
		setStateApp(STATE_APP_PROJECT);
		project.cleanTempImages();

	}

	public String homeDirectory() {
		String pathApp = System.getProperty("user.home") + "/.manucapture";
		return pathApp;
	}



	public void oscEvent(OscMessage theOscMessage) {
		print("### received an osc message.");
		print(" addrpattern: " + theOscMessage.addrPattern());
		println(" typetag: " + theOscMessage.typetag());
		if (theOscMessage.addrPattern().equals("/error")) {
			arduinoConnected = false;
			println("Problem opening Serial Port");
		} else if (chartStateMachine != 3){
			capture();
		}
	}

	public G2P5ManucaptureAdapter createG2P5(String serial, String name) {
		G2P5 g2p5 = null;
		
		if (cameraModel.equals("canon_700D")) {
			g2p5 = Canon700D_G2P5.create(homeDirectory(), serial, name);
		}

		if (cameraModel.equals("canon_EOS_5DS_R")) {
			g2p5 = CanonEOS5DSR_G2P5.create(homeDirectory(), serial, name);
		}

		if (g2p5 == null) {
			println("ERROR, de cameraModel doesn't exist " + cameraModel + " .. creating default Canon 700D");

			g2p5 = Canon700D_G2P5.create(homeDirectory(), serial, name);
		}
		// here is the place to be if you want to add more cameras with different msgs
		G2P5ManucaptureAdapter adapter = new G2P5ManucaptureAdapter(this, g2p5);
		adapter.setTargetFile(project.projectDirectory + "raw", project.projectCode);
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
		newPageRightPath = "";
		newPageLeftPath = "";
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

			p.waitFor(1000, TimeUnit.MILLISECONDS);
			// Thread.sleep(500);
			return true;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (iStream != null)
				try {
					iStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (bReader != null)
				try {
					bReader.close();
				} catch (IOException e) {
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
			pr = Runtime.getRuntime()
					.exec("md5sum " + project.projectDirectory + id + "_" + side + MImage.RAW_EXTENSION);
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
		File file = new File(project.projectDirectory + id + "_" + side + MImage.RAW_EXTENSION);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		timeStamp = sdf.format(file.lastModified());
		return timeStamp;
	}

	public void capture() {
		if (getCaptureState() == CAMERAS_IDLE) {
			if (gphotoPageRight.isConnected() && gphotoPageLeft.isConnected()) {
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

		// Check general timing in Capture State to solve the triggering
		if (getCaptureState() == CAMERAS_INACTIVE && gphotoPageRight.active && gphotoPageLeft.active) {
			setCaptureState(CAMERAS_IDLE);
		} else if (getCaptureState() == CAMERAS_FOCUSSING) {
			if (gphotoPageRightAdapter.mirrorUp && gphotoPageLeftAdapter.mirrorUp) {
				setCaptureState(CAMERAS_MIRROR_UP);
			} else if (millis() > lastCaptureMillis + MAX_TIME_CAPTURE_MACHINE_STATE) {
				setCaptureState(CAMERAS_RECOVERING);
			}
		} else if (getCaptureState() == CAMERAS_MIRROR_UP) {
			if (!gphotoPageRightAdapter.mirrorUp && !gphotoPageLeftAdapter.mirrorUp) {
				setCaptureState(CAMERAS_IDLE);
			}
		} else if (getCaptureState() == CAMERAS_RECOVERING) {
			if (!gphotoPageRightAdapter.mirrorUp && !gphotoPageLeftAdapter.mirrorUp) {
				setCaptureState(CAMERAS_IDLE);
			} else if (millis() > lastCaptureMillis + MAX_TIME_CAPTURE_MACHINE_STATE) {
				if (gphotoPageRightAdapter.mirrorUp) {
					clickCameraPageRight();
					ignoreNextPageRight = true;
				}
				if (gphotoPageLeftAdapter.mirrorUp) {
					clickCameraPageLeft();
					ignoreNextPageLeft = true;
				}
				setCaptureState(CAMERAS_IDLE);
			}
		} else if (getCaptureState() == CAMERAS_IDLE) {
		}
	}

	int getCaptureState() {
		return captureState;
	}

	void setCaptureState(int captureState) {
		if (captureState == CAMERAS_RECOVERING) {
			restoreCamerasStateAfterFailure();
		}
		if (captureState == CAMERAS_FOCUSSING) {
			pressCameras();
		} else if (captureState == CAMERAS_MIRROR_UP) {
			releaseAndShutterCameras();
		} else if (captureState == CAMERAS_IDLE) {

		}
		lastCaptureMillis = millis();
		this.captureState = captureState;
	}

	private void restoreCamerasStateAfterFailure() {
		if (!gphotoPageRightAdapter.mirrorUp && !gphotoPageLeftAdapter.mirrorUp) {
			releaseCameras();
			G4P.showMessage(this, messageContainer.getText("sw.fails"), "", G4P.WARNING);
		} else if (!gphotoPageRightAdapter.mirrorUp && gphotoPageLeftAdapter.mirrorUp) {
			clickCameraPageLeft();
			ignoreNextPageLeft = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsA"), "", G4P.WARNING);
		} else if (gphotoPageRightAdapter.mirrorUp && !gphotoPageLeftAdapter.mirrorUp) {
			clickCameraPageRight();
			ignoreNextPageRight = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsB"), "", G4P.WARNING);
		}
	}

	public void pressCameras() {
		println("Press Cameras by OSC");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('P');
		oscP5.send(myMessage, arduinoDriverLocation);
		gphotoPageLeft.doTriggerEvent();
		gphotoPageRight.doTriggerEvent();
	}

	public void releaseCameras() {
		println("Release Cameras by OSC");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('R');
		oscP5.send(myMessage, arduinoDriverLocation);
		gphotoPageLeft.doTriggerEvent();
		gphotoPageRight.doTriggerEvent();
	}

	public void releaseAndShutterCameras() {
		println("shutter cameras");
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('W');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Release And Shutters Cameras by OSC");
		gphotoPageLeft.doTriggerEvent();
		gphotoPageRight.doTriggerEvent();
	}

	public void clickCamera() {
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('S');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Shutter Cameras by OSC");
		gphotoPageLeft.doTriggerEvent();
		gphotoPageRight.doTriggerEvent();
	}

	public void clickCameraPageLeft() {

		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Y');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Reset Camera A failing by OSC");
		gphotoPageLeft.doTriggerEvent();
	}

	public void clickCameraPageRight() {
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Z');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Reset Camera B failing by OSC");
		gphotoPageRight.doTriggerEvent();
	}

	public void handleMessageDialog(String title, String message, int type) {
		// Determine message type
		G4P.showMessage(this, message, title, type);
	}

	public boolean isAllMirrorsReady() {
		return !gphotoPageRightAdapter.mirrorUp && !gphotoPageLeftAdapter.mirrorUp;
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
		if (stateApp == STATE_APP_NO_PROJECT) {
			gui.grpProject.setVisible(1, false);
			gui.grpAll.setVisible(1, false);
		} else if (stateApp == STATE_APP_PROJECT) {
			gui.grpProject.setVisible(1, false);
			gui.grpAll.setVisible(1, true);
		}
		if (stateApp == STATE_APP_EDITING_PROJECT) {
			gui.grpProject.setVisible(1, true);
			gui.grpAll.setVisible(1, false);
		}
	}

	@Override
	public void keyReleased() {
		if (key == BACKSPACE) {
			String cad = gui.code_text.getText();
			if (cad != null && cad.length() > 0 && guiController.editing) {
				String newCode = cad.substring(0, cad.length() - 1);
				gui.code_text.setText(newCode);
				project.projectCode = newCode;
				gui.code_text.setFocus(false);
				gui.code_text.setFocus(true);		
			}
		}
	}
		
	public void movieEvent(Movie m) {
		  //println("new liveview frame ");
		  //println(m);
		  //println();
		  m.read();
	}
	
	
	static public void main(String[] passedArgs) {
		try {
			String[] appletArgs = new String[] { "ManuCapture_v1_1"};
			if (passedArgs != null) {
				PApplet.main(concat(appletArgs, passedArgs));
			} else {
				PApplet.main(appletArgs);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("End of programmm");
		}
		
		ArduinoDriverRunnerExportPDE pde = new ArduinoDriverRunnerExportPDE();
		pde.main(passedArgs);
	}
}
