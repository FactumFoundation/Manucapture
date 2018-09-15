
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
	
	G2P5 gphotoA;
	G2P5 gphotoB;
	G2P5ManucaptureAdapter gphotoAAdapter;
	G2P5ManucaptureAdapter gphotoBAdapter;
	
	GUI gui;
	GUIController guiController;
	ItemsViewport itemsViewport;
	MessageContainer messageContainer;
	HotArea hotAreaSelected = null;
	
	Project project = null;
	
	
	PImage imgPreviewLeft;
	PImage imgPreviewRight;
	
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
	OscP5 oscP5;
	NetAddress viewerLocation;

	boolean renderLeft = true;
	boolean renderRight = true;
	
	String appPath = null;

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

	int receivePort = 3334;
	int sendPort = 3333;
	int arduinoDriverPort = 13000;

	String baseDirectory = "";

	boolean initSelectedItem = false;

	int shutterMode = 0;
	static int NORMAL_SHUTTER = 0;
	static int REPEAT_SHUTTER = 1;
	// int SUBPAGE_SHUTTER = 2;
	static int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;

	PVector lastPressedR = null;
	PVector lastPressedL = null;

	int liveViewActive = -1;

	boolean mock = false;

	// Chart identification
	public static int STATE_CAPTURING = 0;
	public static int STATE_CHART = 1;

	int cameraState = STATE_CAPTURING;

	int chartStateMachine = 0;
	// *********************

	boolean cropMode = false;

	int guideHeight_1 = 200;
	int guideHeight_2 = 600;

	int marginTopViewer = 100;
	int marginLeftViewerRight = 1170;
	int marginLeftViewerLeft = 400;

	public static int STATE_APP_NO_PROJECT = 0;
	public static int STATE_APP_PROJECT = 1;

	int stateApp = STATE_APP_NO_PROJECT;

	boolean loading = false;
	boolean editingProject = false;

	PImage cameraIcon;
	PImage zoomImg = null;

	boolean arduinoConnected = true;


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
		captureState = CAMERAS_IDLE;

		// Init GUI

		surface.setTitle("ManuCapture v1");
		if (surface != null) {
			surface.setLocation(0, 0);
		}
		
		messageContainer = new MessageContainer();
		messageContainer.init();

		guiController = new GUIController(this);
		
		gui = new GUI();
		gui.createGUI(this);
		
		gui.grpAll.setVisible(0, false);
		gui.grpProject.setVisible(0, false);
		gui.grpAll.setVisible(0, false);
			
		initCropHotAreas();

		itemsViewport = new ItemsViewport();
		itemsViewport.setup(this);
		textMode(MODEL);

		cameraIcon = loadImage("cameraIcon.png");
		zoomImg = loadImage("zoom.png");

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

		if (project.projectName == null || project.projectName.equals("")) {
			handleMessageDialog("Error", "Can't capture photos without project name", G4P.ERROR);
			return;
		}

		println("New photo Event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", event.content);
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

		if ((gphotoA.isConnected() && gphotoB.isConnected()
				&& (!newImagePathA.equals("") && !newImagePathB.equals("")))
				// this allows use only one camera if the other is inactive
				|| (gphotoA.isConnected() && !gphotoB.isConnected()
						&& !newImagePathA.equals(""))
				// this allows use only one camera if the other is inactive
				|| (!gphotoA.isConnected() && gphotoB.isConnected()
						&& !newImagePathB.equals(""))) {
			// if(captureState ==
			// ManuCaptureCAMERAS_PROCESSING){

			// cameraAProcessingNewPhoto = true;
			// cameraBProcessingNewPhoto = true;
			// delay(3000);

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

				// if (chartState < 2) {
				// doNormalShutter(Item.TYPE_BACKGROUND);
				// } else
				//
				if (chartStateMachine == 1) {
					// we do background and first photo normally
					doNormalShutter(Item.TYPE_CHART);
					lastRightPreview = imgPreviewRight;
					lastLeftPreview = imgPreviewLeft;
					chartStateMachine++;
				} else if (chartStateMachine == 2) {

					float newPageNum = project.selectedItem.pagNum;

					newItem = initNewItem(Item.TYPE_CHART, newPageNum);

					imgPreviewLeft = newItem.loadLeftPreview(project.projectDirectory,
							project.projectDirectory + newItem.mImageLeft.imagePath);

					lastLeftPreview = imgPreviewLeft;

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
			captureState = CAMERAS_IDLE;
			// cameraAProcessingNewPhoto = false;
			// cameraBProcessingNewPhoto = false;

		}
	}

	Item newItem;

	public void draw() {

		marginTopViewer = 100;

		gui.btnLiveView.setAlpha(200);
		gui.btnTriggerCrop.setAlpha(200);
		gui.btnTrigger.setAlpha(220);
		gui.btnTriggerChartColor.setAlpha(180);
		gui.btnTriggerNormal.setAlpha(180);
		gui.btnTriggerRepeat.setAlpha(180);
		gui.btnConnectedB.setAlpha(180);

		gui.btnConnectedA.setAlpha(180);
//		 gui.btnConnectedB.moveTo(marginLeftViewerRight +
//		 hImageViewerSize - 280, 0);

		background(75);

		if (stateApp == STATE_APP_NO_PROJECT) {
			if (!arduinoConnected) {
				drawErrorWindow();
			} else {
				drawInitWindow();
			}
		} else {
			// gui.grpAll.setVisible(1, true);
			drawApp();
			if (gui.grpProject.isVisible()) {
				// rect(0, 0, width, height);
			}

			fill(255);
			textAlign(CENTER);
			textSize(18);
			// text("Factum Foundation Version 2.0", width / 2, height - 10);
		}

		fill(255);
		// text("thisstate " + captureState + " state" + cameraState
		// + "\n " + "\nstateChart "
		// + chartStateMachine + "\n " + frameRate +
		// gui.grpProject.isVisible(), 350, height-10);

		if (loading) {
			fill(0, 200);
			rect(0, 0, width, height);
			fill(255, 0, 0);
			text("LOADING...", width / 2, height / 3);
		}

		if (editingProject) {
			fill(155, 255);
			rect(0, 0, width, height);
			fill(255, 0, 0);
			// text("LOADING...", width / 2, height / 3);
		}

		// if (lastLeftPreview != null )
		// image(lastLeftPreview, 200, 10, 200, 200);

		// if (newItem != null && newItem.mImageLeft != null &&
		// newItem.mImageLeft.imgPreview != null) {
		// image(newItem.mImageLeft.imgPreview, 400, 10, 200, 200);
		// }

	}

	public void load_click() { // _CODE_:load_button:841968:
		guiController.load_click();
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
		gui.grpAll.setVisible(1, false);
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

		camerasStateMachineLoop();

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
					guiController.camera_B_active_click(null, null);
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

		itemsViewport.drawItemsViewPort();

		image(itemsViewport.itemsViewPort, itemsViewport.itemListViewPortX, itemsViewport.itemListViewPortY);
		if (renderRight) {
			drawRight();
			renderRight = true;
			// println("renderizamos imagen derecha");
		}

		if (renderLeft) {
			drawLeft();
			renderLeft = true;
			// println("renderizamos imagen izquierda");
		}

		if (hotAreaSelected != null) {
			stroke(255, 0, 0);
			// ellipse(hotAreaSelected.pos.x, hotAreaSelected.pos.y,
			// hotAreaSelected.threshold, hotAreaSelected.threshold);
			hotAreaSelected.draw(g);
		}

		if (lastPressedR == null)
			for (int i = 1; i <= pointsLeft.size(); i++) {
				PVector areaPos1 = pointsLeft.get(i - 1).getRealPosition();
				PVector areaPos2 = pointsLeft.get(i % pointsLeft.size()).getRealPosition();
				stroke(255, 0, 0);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}

		if (lastPressedL == null)
			for (int i = 1; i <= pointsRight.size(); i++) {
				PVector areaPos1 = pointsRight.get(i - 1).getRealPosition();
				PVector areaPos2 = pointsRight.get(i % pointsRight.size()).getRealPosition();
				stroke(255, 0, 0);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}

		stroke(255);

		textAlign(LEFT);
		pushStyle();
		textSize(24);
		fill(255);
		text("Project " + project.projectName, 40, 35);
		textSize(18);
		text("Code " + project.projectCode, 40, 65);
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
		} else if (liveViewActive == -1) {
			// gui.gui.liveView_button.setEnabled(true);
			// gui.liveView_button.setVisible(true);

		}

		// stroke(0,0,255);
		// line(0,guideHeight_1,width,guideHeight_1);
		// stroke(0,255,0);
		// line(0,guideHeight_2,width,guideHeight_2);

		// rect(marginLeftViewerLeft, marginTopViewer, 100, 100);

		if (shutterMode == NORMAL_SHUTTER && gui.btnTriggerRepeat.isVisible()) {
			gui.btnTriggerRepeat.setVisible(false);
		}

		if (shutterMode == REPEAT_SHUTTER && gui.btnTriggerNormal.isVisible()) {
			gui.btnTriggerNormal.setVisible(false);
		}

		if (cameraState == STATE_CHART) {
			pushMatrix();
			pushStyle();

			if (chartStateMachine == 0) {
				// textAlign(CENTER);
				// fill(255, 0, 0, 100);
				// rect(marginLeftViewerLeft, 0, hImageViewerSize * 2,
				// wImageViewerSize);
				// fill(255);
				// textSize(24);
				// text("CALIBRATING, PLEASE CAPTURE \n THIS BACKGROUND
				// \nWITHOUT ANY DOCUMENT", marginLeftViewerRight,
				// 200);
			} else if (chartStateMachine == 1 || chartStateMachine == 2) {
				textAlign(CENTER);
				String msg;

				if (chartStateMachine == 2) {
					translate(marginLeftViewerLeft, 0);
					msg = msg("sw.calibration3");
				} else {
					msg = msg("sw.calibration1");
					translate(marginLeftViewerRight, 0);
				}

				fill(255, 0, 0, 100);
				rect(0, marginTopViewer, hImageViewerSize, wImageViewerSize);
				fill(255);
				textSize(24);
				text(msg, hImageViewerSize / 2, 200);
			} else {
				translate(marginLeftViewerLeft, 0);
				fill(255, 0, 0, 100);
				// rect(0, 0, hImageViewerSize,
				// wImageViewerSize);
				// rect(hImageViewerSize, 0, hImageViewerSize,
				// wImageViewerSize);
				rect(0, marginTopViewer, hImageViewerSize * 2 + 100, wImageViewerSize);
				textSize(24);
				fill(255, 255, 0);
				String cad = msg("sw.calibration2");
				text(cad, hImageViewerSize / 2 - 1, 200 - 1);

				fill(255);
				text(cad, hImageViewerSize / 2, 200);
			}

			popStyle();
			popMatrix();

		}

		// trigger button color
		if (isAllMirrorsReady()) {
			// gui.trigger_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);

			fill(0, 255, 0);
		} else {
			fill(255, 0, 0);
			// gui.trigger_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		ellipse(1120, 938, 50, 50);
	}

	private void drawLeft() {

		if (project.selectedItem != null && imgPreviewLeft != null) {

			pushStyle();
			pushMatrix();
			translate(marginLeftViewerRight, marginTopViewer);

			drawImagePreview(imgPreviewLeft, lastPressedL, marginLeftViewerRight, pointsRight,
					scaleA);

			if (lastPressedL != null) {
				tint(255, 200);
			} else {
				tint(255, 20);
			}
			image(zoomImg, hImageViewerSize - 70, 20, 50, 50);
			// pintamos en blending la imagen de calibración para puntos de crop
			if (chartStateMachine == 3 && lastLeftPreview != null) {
				pushStyle();
				tint(255, 125);
				image(lastLeftPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0,
						lastLeftPreview.width, lastLeftPreview.height);
				popStyle();
			}

			showPhotoMetaData();

			text(project.selectedItem.mImageLeft.imagePath, 0, -10);

			popMatrix();
			stroke(255, 0, 0);
			if (chartStateMachine == 3) {
				stroke(map(sin(100 + millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}

			if (lastPressedL == null) {
				for (HotArea area : pointsRight) {
					if (chartStateMachine == 3 || cropMode)
						area.draw(g);
				}
			}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(marginLeftViewerRight, marginTopViewer, hImageViewerSize, wImageViewerSize);
		}

		// datos de cámara
		if (!gphotoB.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(marginLeftViewerRight + 225, 78, 30, 30);
		// pushMatrix();
		//
		// translate(0, 1015);
		// // fill(255);
		// text("exposure: " + gphotoBAdapter.exposure,
		// marginLeftViewerRight + 75, 40);
		// text("focusing: ", marginLeftViewerRight + 300, 40);
		// text(gphotoBAdapter.g2p5.id, 840, 40);
		fill(255);
		text("mirroUp " + gphotoBAdapter.g2p5.id + " " + gphotoBAdapter.mirrorUp,
				marginLeftViewerRight + 375, 20);
		//
		// fill(0, 200, 0);
		//
		// if (gphotoBAdapter.focus) {
		// fill(255, 0, 0);
		// } else {
		// fill(0, 255, 0);
		// }
		// ellipse(marginLeftViewerRight + 370, 35, 15, 15);
		// popMatrix();
		if (captureState == CAMERAS_FOCUSSING
				|| captureState == CAMERAS_MIRROR_UP
				|| captureState == CAMERAS_PROCESSING) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerRight, marginTopViewer);
			pushStyle();
			noStroke();
			int alpha = 150;
			if (captureState == CAMERAS_FOCUSSING) {
				fill(40, alpha);
			} else if (captureState == CAMERAS_MIRROR_UP) {
				fill(120, alpha);
			} else if (captureState == CAMERAS_PROCESSING) {
				fill(0, 50, 0, alpha);
			}
			rect(0, 0, hImageViewerSize, wImageViewerSize);
			imageMode(CENTER);
			image(cameraIcon, hImageViewerSize / 2, wImageViewerSize / 2, 256, 256);
			imageMode(CORNER);
			popMatrix();
			popStyle();
		}

	}

	private void drawRight() {

		if (project.selectedItem != null && imgPreviewRight != null) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerLeft, marginTopViewer);
			imageMode(CORNER);

			drawImagePreview(imgPreviewRight, lastPressedR, marginLeftViewerLeft, pointsLeft,
					scaleB);

			// if (gphotoAAdapter.cameraWaitingForPicture ||
			// cameraAProcessingNewPhoto) {

			if (lastPressedR != null) {
				tint(255, 200);
			} else {
				tint(255, 20);
			}
			image(zoomImg, hImageViewerSize - 70, 20, 50, 50);

			if (chartStateMachine == 3 && lastRightPreview != null) {
				pushStyle();
				tint(255, 125);
				image(lastRightPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0,
						lastRightPreview.width, lastRightPreview.height);
				popStyle();
			}

			showPhotoMetaData();
			text(project.selectedItem.mImageRight.imagePath, 0, -10);
			popMatrix();
			stroke(255, 0, 0);
			if (chartStateMachine == 3) {
				stroke(map(sin(millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}
			if (lastPressedR == null)
				for (HotArea area : pointsLeft) {
					if (chartStateMachine == 3 || cropMode)
						area.draw(g);
				}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(marginLeftViewerLeft, marginTopViewer, hImageViewerSize, wImageViewerSize);
		}
		// datos de cámara

		if (!gphotoA.isConnected()) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(marginLeftViewerLeft + 227, 78, 30, 30);

		fill(255);
		// pushMatrix();
		// translate(0, 1015);

		//
		// text(" focusing: ", 890, 40);
		fill(255);
		text("mirroUp " + gphotoAAdapter.g2p5.id + " " + gphotoAAdapter.mirrorUp,
				marginLeftViewerLeft + 430, 40);
		// text(gphotoAAdapter.g2p5.id, 840, 40);
		// if (gphotoAAdapter.focus) {
		// fill(255, 0, 0);
		// } else {
		// fill(0, 255, 0);
		// }
		// ellipse(960, 35, 15, 15);
		// popMatrix();

		if (captureState == CAMERAS_FOCUSSING
				|| captureState == CAMERAS_MIRROR_UP
				|| captureState == CAMERAS_PROCESSING) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerLeft, marginTopViewer);

			noStroke();
			int alpha = 150;
			if (captureState == CAMERAS_FOCUSSING) {
				fill(40, alpha);
			} else if (captureState == CAMERAS_MIRROR_UP) {
				fill(120, alpha);
			} else if (captureState == CAMERAS_PROCESSING) {
				fill(0, 50, 0, alpha);
			}
			rect(0, 0, hImageViewerSize, wImageViewerSize);
			imageMode(CENTER);
			image(cameraIcon, hImageViewerSize / 2, wImageViewerSize / 2, 256, 256);
			imageMode(CORNER);

			popMatrix();
			popStyle();
		}

	}

	private void showPhotoMetaData() {
		fill(255);
		textSize(24);
		textAlign(LEFT);
		// @TODO add info from the readed image

		text("ISO: " + gphotoAAdapter.exposure, 10, wImageViewerSize - 20);
		text("exposure: " + gphotoAAdapter.exposure, 200, wImageViewerSize - 20);
		text("f: " + gphotoAAdapter.exposure, 450, wImageViewerSize - 20);
	}

	private void drawImagePreview(PImage imgPreview, PVector lastPressedR, int marginLeftViewer, List<HotArea> areas,
			float scale) {

		if (lastPressedR != null) {
			// pimero quiero saber pos en la imagen
			float imgScale = imgPreview.width / (float) hImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedR, new PVector(marginLeftViewer, marginTopViewer));

			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);

			int portviewSizeX = (int) ((hImageViewerSize) / scale);
			int portviewSizeY = (int) ((wImageViewerSize) / scale);

			int portviewStartX = (int) (virtualPosScaled.x - portviewSizeX / 2);
			int portviewStartY = (int) (virtualPosScaled.y - portviewSizeY / 2);

			if (portviewStartX + portviewSizeX > imgPreview.width) {
				portviewStartX = imgPreview.width - portviewSizeX;
			}
			if (portviewStartY + portviewSizeY > imgPreview.height) {
				portviewStartY = imgPreview.height - portviewSizeY;
			}
			if (portviewStartX < 0) {
				portviewStartX = 0;
			}
			if (portviewStartY < 0) {
				portviewStartY = 0;
			}
			image(imgPreview, 0, 0, hImageViewerSize, wImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {

			image(imgPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0, imgPreview.width,
					imgPreview.height);
		}
	}

	public void mouseMoved() {

		itemsViewport.mouseMoved();

		if (hotAreaSelected != null) {
			// hotAreaSelected.pos = hotAreafloat scaleA =
			// 1;Selected.pos.add(mouseX -
			// dmouseX,mouseY
			// -dmouseY);
			if (hotAreaSelected.name.startsWith("L")) {
				if (isMouseInsideRight())
					hotAreaSelected.setRealPosition(mouseX, mouseY);
			}

			if (hotAreaSelected.name.startsWith("R")) {
				if (isMouseInsideLeft())
					hotAreaSelected.setRealPosition(mouseX, mouseY);
			}
			if (project != null && project.selectedItem != null) {
				project.selectedItem.mImageLeft.mesh = copyMesh(pointsLeft);
				project.selectedItem.mImageRight.mesh = copyMesh(pointsRight);
			}
		}

		if (lastPressedR != null) {
			updateZoomRight();

		}

		if (lastPressedL != null) {
			updateZoomLeft();

		}

	}

	public void startCropMode() {

		cropMode = !cropMode;
	}

	public void mousePressed() {

		itemsViewport.mousePressed();

		if (hotAreaSelected == null) {
			if (chartStateMachine == 3 || cropMode) {
				for (HotArea area : pointsLeft) {
					if (area.isInArea(mouseX, mouseY)) {
						hotAreaSelected = area;
						break;
					}
				}

				for (HotArea area : pointsRight) {
					if (area.isInArea(mouseX, mouseY)) {
						hotAreaSelected = area;
						break;
					}
				}
			}

			if (hotAreaSelected == null) {
				if (hotAreaSelected == null && project != null && project.selectedItem != null) {
					if (mouseButton == LEFT) {
						if (!editingProject && lastPressedL == null && project.selectedItem.mImageLeft != null
								&& imgPreviewLeft != null) {
							updateZoomLeft();
						}
						// else
						// lastPressedL = null;

						if (!editingProject && lastPressedR == null && project.selectedItem.mImageRight != null
								&& imgPreviewRight != null)
							updateZoomRight();
						// else
						// lastPressedR = null;

					}
					if (mouseButton == RIGHT) {
						lastPressedR = null;
						lastPressedL = null;
					}
				}
			}
		} else {
			hotAreaSelected = null;
			project.selectedItem.saveMetadata();
		}

		println("mouseX:" + mouseX + " mouseY:" + mouseY);
	}

	private boolean isMouseInsideLeft() {

		if (mouseY > marginTopViewer && mouseY < height - marginTopViewer) {
			// Estamos en y
			if (mouseX > marginLeftViewerRight && mouseX < marginLeftViewerRight + hImageViewerSize) {
				return true;
			}

		}
		return false;
	}

	private boolean isMouseInsideRight() {

		if (mouseY > marginTopViewer && mouseY < height - marginTopViewer) {
			// Estamos en y
			if (mouseX > marginLeftViewerLeft && mouseX < marginLeftViewerLeft + hImageViewerSize) {

				if (mouseX > marginLeftViewerRight) {

				} else {
					return true;
				}
			}
		}
		return false;
	}

	private void updateZoomLeft() {
		if (isMouseInsideLeft() && chartStateMachine != 3)
			lastPressedL = new PVector(mouseX, mouseY);
	}

	private void updateZoomRight() {
		if (isMouseInsideRight() && chartStateMachine != 3)
			lastPressedR = new PVector(mouseX, mouseY);
	}

	public void mouseWheel(MouseEvent event) {
		float e = event.getCount();
		if (mouseX > marginLeftViewerRight && mouseX < marginLeftViewerRight + hImageViewerSize) {
			if (mouseY > 20 && mouseY < 20 + wImageViewerSize) {
				scaleA -= e / 10;
				scaleA = max(scaleA, 1);
				scaleA = min(scaleA, 4);
			}
		}
		if (mouseX > 580 && mouseX < 580 + hImageViewerSize) {
			if (mouseY > 20 && mouseY < 20 + wImageViewerSize) {
				scaleB -= e / 10;
				scaleB = max(scaleB, 1);
				scaleB = min(scaleB, 4);
			}
		}
	}

	public void keyPressed() {
		if (key == ' ') {
			guiController.trigger_button_click(null, null);
		}
	}

	public void mouseDragged() {

		itemsViewport.mouseDragged();

		if (hotAreaSelected != null && project.selectedItem != null) {
			hotAreaSelected.setRealPosition(mouseX, mouseY);
			project.selectedItem.mImageLeft.mesh = copyMesh(pointsLeft);
			project.selectedItem.mImageRight.mesh = copyMesh(pointsRight);
		}
	}

	public void mouseReleased() {
		itemsViewport.mouseReleased();
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
			// relNewImagePathA = getNewPathImage(project.projectDirectory,
			// newImagePathA);
			relNewImagePathA = newImagePathA.substring(project.projectDirectory.length());
		}
		String relNewImagePathB = "";
		if (!newImagePathB.equals(""))
			// relNewImagePathB = getNewPathImage(project.projectDirectory,
			// newImagePathB);
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
			// int reply = G4P.selectOption(this, "Load previous session?", "",
			// G4P.PLAIN,
			// G4P.YES_NO);
			// if (reply == 0) {

			// TODO: Load project here
			File projectFile = new File(lastSessionData.getChild("Project").getContent());
			if (projectFile.exists())
				loadProject(projectFile.getPath());
			else
				println("Error loading the project: Project file doesn't exist");

			project.selectedItemIndex = new Integer(lastSessionData.getChild("Current_Item").getContent());

			value = lastSessionData.getChild("Camera_A_Active").getContent();
			if (value.equals("1") && !gphotoA.isConnected())
				guiController.camera_A_active_button_click(null, null);
			// cameraActiveA = true;
			// else
			// cameraActiveA = false;

			value = lastSessionData.getChild("Camera_B_Active").getContent();
			if (value.equals("1") && !gphotoB.isConnected())
				guiController.camera_B_active_click(null, null);
			// cameraActiveB = true;
			// else
			// cameraActiveB = false;

			project.forceSelectedItem(project.selectedItemIndex, false);

			gui.grpAll.setVisible(1, true);

			loading = false;

			noZoom();

			if (project.items.isEmpty()) {
				initCropHotAreas();
			}

			// } else {
			//// new_button_click(null, null);
			// }
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

		if (cameraActiveA)
			value = "1";
		else
			value = "0";
		lastCameraActiveA.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraActiveA);

		if (cameraActiveB)
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
		captureState = CAMERAS_IDLE;

		saveLastSessionData();

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
		captureState = CAMERAS_IDLE;
		G2P5Manager.setImageCount(project.items.size());
		project.forceSelectedItem(project.items.size(), false);
		if (project.items.isEmpty()) {
			initCropHotAreas();
		}
		saveLastSessionData();
		project.removeUnusedImages();
		stateApp = STATE_APP_PROJECT;
		gui.grpAll.setVisible(1, true);
	}

	public String homeDirectory() {
		String pathApp = System.getProperty("user.home") + "/.manucapture";
		return pathApp;
	}

	private void noZoom() {
		lastPressedL = null;
		lastPressedR = null;
	}

	public void handleButtonEvents(GImageButton button, GEvent event) {

		if (button == gui.btnTriggerRepeat) {
			guiController.normal_shutter_click1(null, null);
			noZoom();
		}

		if (button == gui.btnTriggerNormal) {
			guiController.repeat_shutter_click(null, null);
			noZoom();
		}

		if (chartStateMachine != 3) {

			if (button == gui.btnTrigger) {
				guiController.trigger_button_click(null, null);
			}

			if (button == gui.btnTriggerChartColor) {
				guiController.calibration_shutter_click(null, null);
				noZoom();
			}

			if (button == gui.btnLiveView) {
				guiController.liveView_button_click(null, null);
			}

			if (button == gui.btnTriggerCrop) {
				guiController.crop_click(null, null);
			}

			if (button == gui.btnTriggerOpenSOViewer1) {

				try {
					String cmd = "rawtherapee " + project.projectDirectory + project.selectedItem.mImageRight.imagePath;
					println(cmd);
					Runtime.getRuntime().exec(cmd);
				} catch (Exception e) {
					_println("Couldn't create raw directory permisions");
				}
				noZoom();
			}

			if (button == gui.btnTriggerOpenSOViewer2) {
				try {
					Runtime.getRuntime().exec(
							"rawtherapee " + project.projectDirectory + project.selectedItem.mImageLeft.imagePath);
				} catch (Exception e) {
					_println("Couldn't create raw directory permisions");
				}
				noZoom();
			}

			if (button == gui.btnClose) {
				guiController.close_click(null, null);
			}

			if (button == gui.btnEdit) {
				guiController.edit_click(null, null);
			}

			if (button == gui.btnFirstPage) {
				guiController.first_page_button_click(null, null);
			}

			if (button == gui.btnLastPage) {
				guiController.last_page_button_click(null, null);
			}

			if (button == gui.btnConnectedA) {
				if (!cameraActiveA)
					guiController.camera_A_active_button_click(null, null);
				else
					guiController.camera_A_inactive_button_click(null, null);
				noZoom();
			}
			if (button == gui.btnConnectedB) {
				if (!cameraActiveB)
					guiController.camera_B_active_click(null, null);
				else
					guiController.camera_B_inactive_click(null, null);
				noZoom();
			}

		}
	}

	public void settings() {
		size(1920, 1080, P2D);
//		 fullScreen(P2D);
	}

	public void oscEvent(OscMessage theOscMessage) {
		/*
		 * print the address pattern and the typetag of the received OscMessage
		 */
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
	
	
	public void initCropHotAreas() {
		
		pointsLeft = new ArrayList<>();
		pointsRight = new ArrayList<>();

		int size = 50;

		PVector translatePos1 = new PVector(marginLeftViewerLeft, marginTopViewer);
		PVector translatePos2 = new PVector(marginLeftViewerRight, marginTopViewer);

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
		G2P5 g2p5 = G2P5.create(homeDirectory(), serial, name);
		G2P5ManucaptureAdapter adapter = new G2P5ManucaptureAdapter(this, g2p5);
		adapter.setTargetFile(project.projectDirectory + "raw", project.projectName);
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
		if (captureState == CAMERAS_IDLE) {
			if (gphotoA.isConnected() && gphotoB.isConnected()) {
				captureState = CAMERAS_FOCUSSING;
				pressCameras();
				lastCaptureMillis = millis();
			} else {
				G4P.showMessage(this, messageContainer.getText("sw.notconnected"), "", G4P.WARNING);
			}
		} else {
			if (captureState == CAMERAS_INACTIVE) {
				G4P.showMessage(this, messageContainer.getText("sw.notready"), "", G4P.WARNING);
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
			println("FAILED A");
		}
		//
		if (lastCameraBAction > 0 && gphotoBAdapter.lastEventMillis > lastCameraBAction + MAX_TIME_TO_EVENT) {
			// // we have a failing state, pulse lost
			// resetCamerasFailingA();
			// ignoreNextPhotoB = true;
			 lastCameraBAction = -1;
			failedB = true;
			println("FAILED B");
		}
		// if we are out from Idle more than a time we break
		if (captureState != CAMERAS_IDLE && millis() - lastCaptureMillis > MAX_TIME_CAPTURE_MACHINE_STATE) {
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
				G4P.showMessage(this, messageContainer.getText("sw.noeventA"), "", G4P.WARNING);
			}
			if (failedB) {
				// resetCamerasFailingA();
				// ignoreNextPhotoB = true;
				 lastCameraBAction = -1;
				 restoreCamerasStateAfterFailure();
				G4P.showMessage(this, messageContainer.getText("sw.noeventB"), "", G4P.WARNING);
			}
			if (gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
				releaseAndShutterCameras();
				captureState = CAMERAS_MIRROR_UP;
				// if (state == CHART) {
				// }
			} else {
				if (millis() - lastCaptureMillis > MAX_TIME_FOCUSSING) {
					println("Lsa dos cámaras no están dispuestas a poner el mirror en up");
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
			G4P.showMessage(this, messageContainer.getText("sw.fails"), "", G4P.WARNING);
		} else if (!gphotoAAdapter.mirrorUp && gphotoBAdapter.mirrorUp) {
//			resetCamerasFailingA();
//			ignoreNextPhotoB = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsA"), "", G4P.WARNING);
		} else if (gphotoAAdapter.mirrorUp && !gphotoBAdapter.mirrorUp) {
//			resetCamerasFailingB();
//			ignoreNextPhotoA = true;
			G4P.showMessage(this, messageContainer.getText("sw.failsB"), "", G4P.WARNING);
		}
		captureState = CAMERAS_IDLE;
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

	public void resetCamerasFailingA() {
		// COMMENT THIS BECAUSE CAN ENTER IN LOOP, IF FAIL THIS WE TRY TO RESET
		// IF NEEDED HERE ADD A COUNTER TO STOP
		// lastCameraAAction = millis();
		OscMessage myMessage = new OscMessage("/shutterAction");
		myMessage.add('Y');
		oscP5.send(myMessage, arduinoDriverLocation);
		println("Reset Camera A failing by OSC");
	}

	public void resetCamerasFailingB() {
		// lastCameraBAction = millis();
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

	public void clearPreviews() {
	}

	public String msg(String key) {
		return messageContainer.getText(key);
	}

	static public void main(String[] passedArgs) {

		// String location = "--location=0,0";

		/*
		 * GraphicsEnvironment environment =
		 * GraphicsEnvironment.getLocalGraphicsEnvironment(); GraphicsDevice devices[] =
		 * environment.getScreenDevices();
		 * 
		 * if(devices.length>1 ){ //we have a 2nd display/projector
		 * 
		 * primary_width = devices[0].getDisplayMode().getWidth(); location =
		 * "--location="+primary_width+",0";
		 * 
		 * }else{//leave on primary display location = "--location=0,0";
		 * 
		 * }
		 */
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
