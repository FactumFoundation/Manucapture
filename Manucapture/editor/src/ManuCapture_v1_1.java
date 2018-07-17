
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
	 * This source file is part of the ManuCapture software For the latest info,
	 * see http://www.factumfoundation.org/pag/235/Digitisation-of-oriental-
	 * manuscripts-in-Daghestan
	 * 
	 * Copyright (c) 2016-2018 Jorge Cano and Enrique Esteban in Factum
	 * Foundation
	 * 
	 * This program is free software; you can redistribute it and/or modify it
	 * under the terms of the GNU General Public License as published by the
	 * Free Software Foundation; either version 2 of the License, or (at your
	 * option) any later version.
	 * 
	 * This program is distributed in the hope that it will be useful, but
	 * WITHOUT ANY WARRANTY; without even the implied warranty of
	 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
	 * Public License for more details.
	 * 
	 * You should have received a copy of the GNU General Public License along
	 * with this program; if not, write to the Free Software Foundation, Inc.,
	 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
	 */

	int receivePort = 3334;
	int sendPort = 3333;
	int arduinoDriverPort = 13000;

	String baseDirectory = "";

	PApplet parent;

	Project project = null;

	boolean initSelectedItem = false;

	int shutterMode = 0;
	static int NORMAL_SHUTTER = 0;
	static int REPEAT_SHUTTER = 1;
	// int SUBPAGE_SHUTTER = 2;
	static int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;

	ManuCaptureContext context = new ManuCaptureContext();

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

	int guideHeight_1 = 200;
	int guideHeight_2 = 600;

	int marginTopViewer = 40;
	int marginLeftViewerRight = 1170;
	int marginLeftViewerLeft = 400;

	ItemsViewport itemsViewport;

	HotArea hotAreaSelected = null;

	boolean loadData = true;

	public static int STATE_APP_NO_PROJECT = 0;
	public static int STATE_APP_PROJECT = 1;

	int stateApp = STATE_APP_NO_PROJECT;

	boolean loading = false;
	boolean editingProject = false;

	PImage cameraIcon;

	public void setup() {

		System.setOut(new TracingPrintStream(System.out));

		context.parent = this;
		context.thumbnail = new RawFile();
		context.parent = this;
		context.gui = new GUI();
		context.guiController = new GUIController(context);
		context.appPath = sketchPath() + "/..";

		int size = 50;

		PVector translatePos1 = new PVector(marginLeftViewerLeft, marginTopViewer);
		PVector translatePos2 = new PVector(marginLeftViewerRight, marginTopViewer);

		context.pointsLeft.add(new HotArea(new PVector(size, size), translatePos1, 0, size, "LTL"));
		context.pointsLeft.add(
				new HotArea(new PVector(context.hImageViewerSize - size, 0 + size), translatePos1, 1, size, "LTR"));
		context.pointsLeft
				.add(new HotArea(new PVector(context.hImageViewerSize - size, context.wImageViewerSize - size),
						translatePos1, 2, size, "LBL"));
		context.pointsLeft.add(
				new HotArea(new PVector(0 + size, context.wImageViewerSize - size), translatePos1, 3, size, "LBR"));

		context.pointsRight.add(new HotArea(new PVector(size, size), translatePos2, 0, size, "RTL"));
		context.pointsRight
				.add(new HotArea(new PVector(context.hImageViewerSize - size, size), translatePos2, 1, size, "RTR"));
		context.pointsRight
				.add(new HotArea(new PVector(context.hImageViewerSize - size, context.wImageViewerSize - size),
						translatePos2, 2, size, "RBL"));
		context.pointsRight
				.add(new HotArea(new PVector(size, context.wImageViewerSize - size), translatePos2, 3, size, "RBR"));

		project = new Project();
		project.context = context;

		String home = homeDirectory();
		File file = new File(home);
		if (!file.exists()) {
			file.mkdir();
		}
		context.project = project;

		if (surface != null) {
			surface.setLocation(0, 0);
		}

		XML serialXML = loadXML("cameraSerials.xml");
		context.serialCameraA = serialXML.getChild("Camera_A").getContent();
		context.serialCameraB = serialXML.getChild("Camera_B").getContent();

		context.serialXMLA = serialXML.getChild("Camera_A");
		context.serialXMLB = serialXML.getChild("Camera_B");

		String rotA = serialXML.getChild("Camera_A").getString("rotation");
		String rotB = serialXML.getChild("Camera_B").getString("rotation");

		if (rotA != null)
			context.rotationA = Integer.parseInt(rotA);

		if (rotB != null)
			context.rotationB = Integer.parseInt(rotB);

		System.out.println("camera A rotation" + rotA);
		System.out.println("camera B rotation" + rotB);
		context.oscP5 = new OscP5(this, receivePort);
		context.viewerLocation = new NetAddress("127.0.0.1", sendPort);
		context.arduinoDriverLocation = new NetAddress("127.0.0.1", arduinoDriverPort);

		if (mock) {
			context.gphotoA = G2P5MockDisk.create(this, context.serialCameraA, "A");
			context.gphotoAAdapter.setTargetFile(homeDirectory(), "test");
			context.gphotoB = G2P5MockDisk.create(this, context.serialCameraB, "B");
			context.gphotoBAdapter.setTargetFile(homeDirectory(), "test");
		} else {

			G2P5Manager.init(0);
			context.gphotoAAdapter = context.createG2P5(context.serialCameraA, "A");
			context.gphotoBAdapter = context.createG2P5(context.serialCameraB, "B");
			context.init();
		}

		surface.setTitle("ManuCapture v1");
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.YELLOW_SCHEME);
		G4P.setCursor(ARROW);

		context.gui = new GUI();
		context.gui.createGUI(context);
		// context.gui.createGroup2Controls();
		context.gui.customGUI();

		itemsViewport = new ItemsViewport();
		itemsViewport.setup(context);
		textMode(context.parent.MODEL);

		cameraIcon = loadImage("cameraIcon.png");

		background(backgroundColor);

		frameRate(25);
		context.gui.grpAll.setVisible(0, false);
	}

	public void newPhotoEvent(G2P5Event event, String ic) {

		if (project.projectName == null || project.projectName.equals("")) {
			context.handleMessageDialog("Error", "Can't capture photos without project name", G4P.ERROR);
			return;
		}

		println("New photo Event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", event.content);
		if (event.g2p5 == context.gphotoA) {
			if (context.ignoreNextPhotoA) {
				parent.println("Ignoring A");
				context.ignoreNextPhotoA = false;
			} else {
				context.gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				context.gphotoAAdapter.setFullTargetPath(ic);
				context.moveFile(event.content, context.gphotoAAdapter.getFullTargetPath());
				context.newImagePathA = context.gphotoAAdapter.getFullTargetPath();
			}
		} else if (event.g2p5 == context.gphotoB) {
			if (context.ignoreNextPhotoB) {
				parent.println("Ignoring B");
				context.ignoreNextPhotoB = false;
			} else {
				context.gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				context.gphotoBAdapter.setFullTargetPath(ic);
				context.moveFile(event.content, context.gphotoBAdapter.getFullTargetPath());
				context.newImagePathB = context.gphotoBAdapter.getFullTargetPath();
			}
		}

		if ((context.gphotoA.isConnected() && context.gphotoB.isConnected()
				&& (!context.newImagePathA.equals("") && !context.newImagePathB.equals("")))
				// this allows use only one camera if the other is inactive
				|| (context.gphotoA.isConnected() && !context.gphotoB.isConnected()
						&& !context.newImagePathA.equals(""))
				// this allows use only one camera if the other is inactive
				|| (!context.gphotoA.isConnected() && context.gphotoB.isConnected()
						&& !context.newImagePathB.equals(""))) {
			// if(context.captureState ==
			// ManuCaptureContext.CAMERAS_PROCESSING){

			// context.cameraAProcessingNewPhoto = true;
			// context.cameraBProcessingNewPhoto = true;
			// delay(3000);
			if (shutterMode == NORMAL_SHUTTER) {

				doNormalShutter(Item.TYPE_ITEM);

			} else if (shutterMode == REPEAT_SHUTTER) {
				if (project.items.size() > 0) {
					float newPageNum = project.selectedItem.pagNum;

					Item newItem = initNewItem(project.selectedItem.type, newPageNum);

					newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					context.clearPaths();

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

					context.lastRightPreview = project.selectedItem.mImageRight.imgPreview;
					context.lastLeftPreview = project.selectedItem.mImageLeft.imgPreview;
					chartStateMachine++;
				} else if (chartStateMachine == 2) {

					float newPageNum = project.selectedItem.pagNum;

					newItem = initNewItem(Item.TYPE_CHART, newPageNum);

					newItem.loadLeftPreview(context.project.projectDirectory,
							context.project.projectDirectory + newItem.mImageLeft.imagePath);

					context.lastLeftPreview = newItem.mImageLeft.imgPreview;

					// newItem.mImageLeft.remove();
					newItem.mImageLeft.imagePath = "";
					newItem.loadThumbnails();

					project.replaceItem(project.selectedItemIndex, newItem);
					context.clearPaths();

					chartStateMachine++;
				} else {

					context.guiController.normal_shutter_click1(null, null);
				}

			}
			context.captureState = ManuCaptureContext.CAMERAS_IDLE;
			// context.cameraAProcessingNewPhoto = false;
			// context.cameraBProcessingNewPhoto = false;

		}
	}

	Item newItem;

	public void draw() {

		context.gui.btnLiveView.setAlpha(200);
		context.gui.btnTrigger.setAlpha(220);
		context.gui.btnTriggerChartColor.setAlpha(180);
		context.gui.btnTriggerNormal.setAlpha(180);
		context.gui.btnTriggerRepeat.setAlpha(180);
		context.gui.btnConnectedB.setAlpha(180);

		context.gui.btnConnectedA.setAlpha(180);
		context.gui.btnConnectedB.moveTo(marginLeftViewerRight + context.hImageViewerSize - 280, 30);

		background(75);

		if (stateApp == STATE_APP_NO_PROJECT) {
			// MOSTRAR CARGAR O NUEVO
			context.gui.grpAll.setVisible(1, false);
			// ellipse(width/2,500,1000,1000);
			textAlign(CENTER);

			PVector m = new PVector(mouseX, mouseY);
			float dist = m.dist(new PVector(width / 2, 500));
			float dist1 = m.dist(new PVector(width / 2 + width / 4, 500));
			float dist2 = m.dist(new PVector(width / 2 - width / 4, 500));

			fill(255);
			textSize(32);
			text("MANUCAPTURE", width / 2, 250);
			textSize(18);
			text("Factum Foundation Version 2.0", width / 2, height - 30);
			textSize(20);

			if (dist2 < 100)
				fill(100);
			else
				fill(255);

			ellipse(width / 2 - width / 4, 500, 200, 200);
			fill(0);
			text("NEW PROJECT", width / 2 - width / 4, 500);

			if (dist < 100)
				fill(100);
			else
				fill(255);

			ellipse(width / 2, 500, 200, 200);
			fill(0);
			text("LOAD PREVIOUS", width / 2, 500);

			if (dist1 < 100)
				fill(100);
			else
				fill(255);
			ellipse(width / 2 + width / 4, 500, 200, 200);
			fill(0);
			text("LOAD PROJECT", width / 2 + width / 4, 500);

			if (mousePressed && !loading) {
				if (dist < 100) {
					loading = true;
					context.gui.grpAll.setVisible(0, true);

					thread("loadLastSessionData");

				}
				if (dist1 < 100) {
					loading = true;
					thread("load_click");
				}
				if (dist2 < 100) {
					context.guiController.new_button_click(null, null);
				}
			}

		} else {

			// context.gui.grpAll.setVisible(1, true);
			drawInittializedApp();
			if (context.gui.grpProject.isVisible()) {
				// rect(0, 0, width, height);
			}

			fill(255);
			textAlign(CENTER);
			textSize(18);
			text("Factum Foundation Version 2.0", width / 2, height - 10);
		}

		fill(255);
		text("contextstate " + context.captureState + " state" + cameraState + "\n " + "\nstateChart "
				+ chartStateMachine + "\n " + frameRate + context.gui.grpProject.isVisible(), 250, 10);

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

		// if (context.lastLeftPreview != null )
		// image(context.lastLeftPreview, 200, 10, 200, 200);

		// if (newItem != null && newItem.mImageLeft != null &&
		// newItem.mImageLeft.imgPreview != null) {
		// image(newItem.mImageLeft.imgPreview, 400, 10, 200, 200);
		// }
	}

	public void load_click() { // _CODE_:load_button:841968:
		context.guiController.load_click();
	}

	private void drawInittializedApp() {
		context.camerasStateMachineLoop();

		if (liveViewActive == 1) {

			context.gphotoA.setActive(false);
			context.gphotoB.setActive(false);

			G2P5.killAllGphotoProcess();

			String command = context.appPath + "/GPhotoLiveView/bin/GPhotoLiveView_debug";
			try {
				Process process = Runtime.getRuntime().exec(command);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				OscMessage myMessage = new OscMessage("/cameraPorts");
				parent.println(context.gphotoA.getPort(), context.gphotoB.getPort());
				if (context.gphotoA.getPort() == null || context.gphotoB.getPort() == null) {
					G4P.showMessage(this,
							"The cameras was not detected, please connect, turn on and restart de application", "",
							G4P.WARNING);
				} else {
					myMessage.add(context.gphotoA
							.getPort()); /* add an int to the osc message */
					myMessage.add(context.gphotoB
							.getPort()); /* add an int to the osc message */
					context.oscP5.send(myMessage, context.viewerLocation);

					process.waitFor();
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				G2P5.killAllGphotoProcess();

				// context.cameraActiveA = false;
				// context.gphotoA.setActive(false);
				//
				// context.cameraActiveB = false;
				// context.gphotoB.setActive(false);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (!context.gphotoA.isConnected()) {
					// context.gphotoAAdapter =
					// context.createG2P5(context.serialCameraA, "A");
					context.guiController.camera_A_active_button_click(null, null);
				}
				if (!context.gphotoB.isConnected()) {
					// context.gphotoBAdapter =
					// context.createG2P5(context.serialCameraB, "B");
					context.guiController.camera_B_active_click(null, null);
				}

				context.gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
				context.gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);

				// camera_A_connected_click(null, null);
				liveViewActive = -1;
			}
		}

		if (loadData) {
			thread("loadLastSessionData");
			// loadLastSessionData();
			loadData = false;
		}

		if (surface != null) {
			// surface.setLocation(0, 0);
		}

		// + background(backgroundColor);
		fill(backgroundColor);
		// rect(0, 0, 200, 200);

		// CAMERA STATE SECTION
		// ///////////////////////////////////////////////////////

		if (context.gphotoA.isConnected()) {
			context.gui.camera_A_connected_button.setText("CONNECTED");
			context.gui.camera_A_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			context.gui.camera_A_connected_button.setText("DISCONNECTED");
			context.gui.camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		if (context.gphotoB.isConnected()) {
			context.gui.camera_B_connected_button.setText("CONNECTED");
			context.gui.camera_B_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			context.gui.camera_B_connected_button.setText("DISCONNECTED");
			context.gui.camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		itemsViewport.drawItemsViewPort();

		image(itemsViewport.itemsViewPort, itemsViewport.itemListViewPortX, itemsViewport.itemListViewPortY);
		if (context.renderRight) {
			drawRight();
			context.renderRight = true;
			// println("renderizamos imagen derecha");
		}

		if (context.renderLeft) {
			drawLeft();
			context.renderLeft = true;
			// println("renderizamos imagen izquierda");
		}

		if (hotAreaSelected != null) {
			stroke(255, 0, 0);
			// ellipse(hotAreaSelected.pos.x, hotAreaSelected.pos.y,
			// hotAreaSelected.threshold, hotAreaSelected.threshold);
			hotAreaSelected.draw(g);
		}

		if (lastPressedR == null)
			for (int i = 1; i <= context.pointsLeft.size(); i++) {
				PVector areaPos1 = context.pointsLeft.get(i - 1).getRealPosition();
				PVector areaPos2 = context.pointsLeft.get(i % context.pointsLeft.size()).getRealPosition();
				stroke(255, 0, 0);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}

		if (lastPressedL == null)
			for (int i = 1; i <= context.pointsRight.size(); i++) {
				PVector areaPos1 = context.pointsRight.get(i - 1).getRealPosition();
				PVector areaPos2 = context.pointsRight.get(i % context.pointsRight.size()).getRealPosition();
				stroke(255, 0, 0);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}

		stroke(255);

		textAlign(LEFT);
		pushStyle();
		textSize(24);
		text("Project " + context.project.projectName, 10, 25);
		textSize(18);
		text("Code " + context.project.projectCode, 10, 50);
		popStyle();
		textSize(16);
		fill(255, 0, 0);

		if (liveViewActive == 0) {
			fill(0, 200);
			rect(0, 0, width, height);
			textSize(32);
			fill(255, 0, 0);
			text("LIVEVIEW MODE ENABLED", width / 2 - 100, height / 2);
			liveViewActive++;
		} else if (liveViewActive == -1) {
			// context.gui.context.gui.liveView_button.setEnabled(true);
			// context.gui.liveView_button.setVisible(true);
			context.gui.context.gui.btnLiveView.setEnabled(true);
			context.gui.btnLiveView.setVisible(true);

		}

		// stroke(0,0,255);
		// line(0,guideHeight_1,width,guideHeight_1);
		// stroke(0,255,0);
		// line(0,guideHeight_2,width,guideHeight_2);

		// rect(marginLeftViewerLeft, marginTopViewer, 100, 100);

		if (shutterMode == NORMAL_SHUTTER && context.gui.btnTriggerRepeat.isVisible()) {
			context.gui.btnTriggerRepeat.setVisible(false);
		}

		if (shutterMode == REPEAT_SHUTTER && context.gui.btnTriggerNormal.isVisible()) {
			context.gui.btnTriggerNormal.setVisible(false);
		}

		if (cameraState == STATE_CHART) {
			pushMatrix();
			pushStyle();

			if (chartStateMachine == 0) {
				// textAlign(CENTER);
				// fill(255, 0, 0, 100);
				// rect(marginLeftViewerLeft, 0, context.hImageViewerSize * 2,
				// context.wImageViewerSize);
				// fill(255);
				// textSize(24);
				// text("CALIBRATING, PLEASE CAPTURE \n THIS BACKGROUND
				// \nWITHOUT ANY DOCUMENT", marginLeftViewerRight,
				// 200);
			} else if (chartStateMachine == 1 || chartStateMachine == 2) {
				textAlign(CENTER);

				if (chartStateMachine == 2) {
					translate(marginLeftViewerLeft, 0);
				} else {
					translate(marginLeftViewerRight, 0);
				}

				fill(255, 0, 0, 100);
				rect(0, 0, context.hImageViewerSize, context.wImageViewerSize);
				fill(255);
				textSize(24);
				text("CALIBRATING, PLEASE CAPTURE  THIS CAMERA", context.hImageViewerSize / 2, 200);
			} else {
				translate(marginLeftViewerLeft, 0);
				fill(255, 0, 255, 0);
				rect(0, 0, context.hImageViewerSize, context.wImageViewerSize);
				rect(context.hImageViewerSize, 0, context.hImageViewerSize, context.wImageViewerSize);
				textSize(24);
				fill(255, 255, 0);
				String cad = "CROP POINTS, PLEASE DRAG POINTS TO COVER ALL DE MANUSCRIPT";
				text(cad, context.hImageViewerSize / 2 - 1, 200 - 1);

				fill(255);
				text(cad, context.hImageViewerSize / 2, 200);
			}

			popStyle();
			popMatrix();

		}

		// trigger button color
		if (context.isAllMirrorsReady()) {
			context.gui.trigger_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			context.gui.trigger_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}
	}

	private void drawLeft() {

		if (project.selectedItem != null && project.selectedItem.mImageLeft.imgPreview != null) {

			pushStyle();
			pushMatrix();
			translate(marginLeftViewerRight, marginTopViewer);

			drawImagePreview(project.selectedItem.mImageLeft, lastPressedL, marginLeftViewerRight, context.pointsRight,
					context.scaleA);

			// pintamos en blending la imagen de calibración para puntos de crop
			if (chartStateMachine == 3 && context.lastLeftPreview != null) {
				pushStyle();
				tint(255, 125);
				image(context.lastLeftPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, 0, 0,
						context.lastLeftPreview.width, context.lastLeftPreview.height);
				popStyle();
			}

			fill(255);
			textSize(24);
			text(project.selectedItem.mImageLeft.imagePath, 200, -10);
			popMatrix();
			stroke(255, 0, 0);
			if (chartStateMachine == 3) {
				stroke(map(sin(100 + millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}

			if (lastPressedL == null) {
				for (HotArea area : context.pointsRight) {
					if (chartStateMachine == 3)
						area.draw(g);
				}
			}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(marginLeftViewerRight, 20, context.hImageViewerSize, context.wImageViewerSize);
		}

		// datos de cámara
		if (!context.cameraActiveB) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		pushMatrix();
		ellipse(marginLeftViewerRight + 440, 78, 30, 30);
		translate(0, 1015);
		// fill(255);
		text("exposure: " + context.gphotoBAdapter.exposure, marginLeftViewerRight + 75, 40);
		text("focusing: ", marginLeftViewerRight + 300, 40);
		text(context.gphotoBAdapter.g2p5.id, 840, 40);
		text("mirroUp " + context.gphotoBAdapter.mirrorUp, marginLeftViewerRight + 75, 60);

		fill(0, 200, 0);

		if (context.gphotoBAdapter.focus) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(marginLeftViewerRight + 370, 35, 15, 15);
		popMatrix();
		if (context.captureState == ManuCaptureContext.CAMERAS_FOCUSSING
				|| context.captureState == ManuCaptureContext.CAMERAS_MIRROR_UP
				|| context.captureState == ManuCaptureContext.CAMERAS_PROCESSING) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerRight, marginTopViewer);
			pushStyle();
			noStroke();
			int alpha = 150;
			if (context.captureState == ManuCaptureContext.CAMERAS_FOCUSSING) {
				fill(40, alpha);
			} else if (context.captureState == ManuCaptureContext.CAMERAS_MIRROR_UP) {
				fill(120, alpha);
			} else if (context.captureState == ManuCaptureContext.CAMERAS_PROCESSING) {
				fill(0, 50, 0, alpha);
			}
			rect(0, 0, context.hImageViewerSize, context.wImageViewerSize);
			imageMode(CENTER);
			image(cameraIcon, context.hImageViewerSize / 2, context.wImageViewerSize / 2, 256, 256);
			imageMode(CORNER);
			popMatrix();
			popStyle();
		}

	}

	private void drawRight() {

		if (project.selectedItem != null && project.selectedItem.mImageRight.imgPreview != null) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerLeft, marginTopViewer);
			imageMode(CORNER);

			drawImagePreview(project.selectedItem.mImageRight, lastPressedR, marginLeftViewerLeft, context.pointsLeft,
					context.scaleB);

			// if (context.gphotoAAdapter.cameraWaitingForPicture ||
			// context.cameraAProcessingNewPhoto) {

			if (chartStateMachine == 3 && context.lastRightPreview != null) {
				pushStyle();
				tint(255, 125);
				image(context.lastRightPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, 0, 0,
						context.lastRightPreview.width, context.lastRightPreview.height);
				popStyle();
			}

			fill(255);
			textSize(24);
			text(project.selectedItem.mImageRight.imagePath, 200, -10);

			popMatrix();
			stroke(255, 0, 0);
			if (chartStateMachine == 3) {
				stroke(map(sin(millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}
			if (lastPressedR == null)
				for (HotArea area : context.pointsLeft) {
					if (chartStateMachine == 3)
						area.draw(g);
				}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(580, 20, context.hImageViewerSize, context.wImageViewerSize);
		}
		// datos de cámara
		pushMatrix();
		if (!context.cameraActiveA) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(marginLeftViewerLeft + 230, 78, 30, 30);

		fill(255, 0, 0);

		translate(0, 1015);
		text("exposure: " + context.gphotoAAdapter.exposure, 650, 40);

		text(" focusing: ", 890, 40);
		text("mirroUp " + context.gphotoAAdapter.mirrorUp, 650, 60);
		text(context.gphotoAAdapter.g2p5.id, 840, 40);
		if (context.gphotoAAdapter.focus) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(960, 35, 15, 15);
		popMatrix();

		if (context.captureState == ManuCaptureContext.CAMERAS_FOCUSSING
				|| context.captureState == ManuCaptureContext.CAMERAS_MIRROR_UP
				|| context.captureState == ManuCaptureContext.CAMERAS_PROCESSING) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerLeft, marginTopViewer);

			noStroke();
			int alpha = 150;
			if (context.captureState == ManuCaptureContext.CAMERAS_FOCUSSING) {
				fill(40, alpha);
			} else if (context.captureState == ManuCaptureContext.CAMERAS_MIRROR_UP) {
				fill(120, alpha);
			} else if (context.captureState == ManuCaptureContext.CAMERAS_PROCESSING) {
				fill(0, 50, 0, alpha);
			}
			rect(0, 0, context.hImageViewerSize, context.wImageViewerSize);
			imageMode(CENTER);
			image(cameraIcon, context.hImageViewerSize / 2, context.wImageViewerSize / 2, 256, 256);
			imageMode(CORNER);

			popMatrix();
			popStyle();
		}

	}

	private void drawImagePreview(MImage img, PVector lastPressedR, int marginLeftViewer, List<HotArea> areas,
			float scale) {
		if (lastPressedR != null) {
			// pimero quiero saber pos en la imagen
			float imgScale = img.imgPreview.width / (float) context.hImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedR, new PVector(marginLeftViewer, marginTopViewer));

			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);

			int portviewSizeX = (int) ((context.hImageViewerSize) / scale);
			int portviewSizeY = (int) ((context.wImageViewerSize) / scale);

			int portviewStartX = (int) (virtualPosScaled.x - portviewSizeX / 2);
			int portviewStartY = (int) (virtualPosScaled.y - portviewSizeY / 2);

			if (portviewStartX + portviewSizeX > img.imgPreview.width) {
				portviewStartX = img.imgPreview.width - portviewSizeX;
			}

			if (portviewStartY + portviewSizeY > img.imgPreview.height) {
				portviewStartY = img.imgPreview.height - portviewSizeY;
			}

			if (portviewStartX < 0) {
				portviewStartX = 0;
			}

			if (portviewStartY < 0) {
				portviewStartY = 0;
			}

			image(img.imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {

			image(img.imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, 0, 0, img.imgPreview.width,
					img.imgPreview.height);

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
			project.selectedItem.mImageLeft.mesh = context.copyMesh(context.pointsLeft);
			project.selectedItem.mImageRight.mesh = context.copyMesh(context.pointsRight);
		}

		if (lastPressedR != null) {
			updateZoomRight();

		}

		if (lastPressedL != null) {
			updateZoomLeft();

		}

	}

	public void mousePressed() {

		itemsViewport.mousePressed();

		if (hotAreaSelected == null) {
			if (chartStateMachine == 3) {
				for (HotArea area : context.pointsLeft) {
					if (area.isInArea(mouseX, mouseY)) {
						hotAreaSelected = area;
						break;
					}
				}

				for (HotArea area : context.pointsRight) {
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
								&& project.selectedItem.mImageLeft.imgPreview != null) {
							updateZoomLeft();
						}
						// else
						// lastPressedL = null;

						if (!editingProject && lastPressedR == null && project.selectedItem.mImageRight != null
								&& project.selectedItem.mImageRight.imgPreview != null)
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
			context.project.selectedItem.saveMetadata();
		}

		println("mouseX:" + mouseX + " mouseY:" + mouseY);
	}

	private boolean isMouseInsideLeft() {

		if (mouseY > marginTopViewer && mouseY < height) {
			// Estamos en y
			if (mouseX > marginLeftViewerRight) {
				return true;
			}

		}
		return false;
	}

	private boolean isMouseInsideRight() {

		if (mouseY > marginTopViewer && mouseY < height) {
			// Estamos en y
			if (mouseX > marginLeftViewerLeft && mouseX < marginLeftViewerRight) {

				if (mouseX > marginLeftViewerRight) {

				} else {
					return true;
				}
			}
		}
		return false;
	}

	private void updateZoomLeft() {
		// lastPressedR = null;
		if (isMouseInsideLeft())
			lastPressedL = new PVector(mouseX, mouseY);
	}

	private void updateZoomRight() {
		// lastPressedR = null;
		if (isMouseInsideRight())
			lastPressedR = new PVector(mouseX, mouseY);
	}

	public void mouseWheel(MouseEvent event) {
		float e = event.getCount();
		// println(e);

		if (mouseX > marginLeftViewerRight && mouseX < marginLeftViewerRight + context.hImageViewerSize) {
			if (mouseY > 20 && mouseY < 20 + context.wImageViewerSize) {
				context.scaleA -= e / 10;

				context.scaleA = max(context.scaleA, 1);
				context.scaleA = min(context.scaleA, 4);
			}
		}

		if (mouseX > 580 && mouseX < 580 + context.hImageViewerSize) {
			if (mouseY > 20 && mouseY < 20 + context.wImageViewerSize) {
				context.scaleB -= e / 10;
				context.scaleB = max(context.scaleB, 1);
				context.scaleB = min(context.scaleB, 4);
			}

		}

	}

	public void keyPressed() {
		if (key == ' ') {
			context.guiController.trigger_button_click(null, null);
		}
	}

	public void mouseDragged() {

		itemsViewport.mouseDragged();

		if (hotAreaSelected != null) {
			hotAreaSelected.setRealPosition(mouseX, mouseY);
			project.selectedItem.mImageLeft.mesh = context.copyMesh(context.pointsLeft);
			project.selectedItem.mImageRight.mesh = context.copyMesh(context.pointsRight);
		}
	}

	public void mouseReleased() {
		itemsViewport.mouseReleased();
		// hotAreaSelected = null;

	}

	// private String getNewPathImage(String projectDirectory, String
	// newImagePath)
	// {
	// String relNewImagePathA = null;
	// int start = projectDirectory.length() + 1;
	// int end = newImagePath.length();
	// if (end < start) {
	// println("Error en los " + " " + newImagePath + " \n" + projectDirectory);
	// } else {
	// relNewImagePathA = newImagePath.substring(start, end);
	// }
	// return relNewImagePathA;
	// }

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
		context.clearPaths();
	}

	private Item initNewItem(String type, float newPageNum) {

		if (project.projectDirectory.equals("")) {
			context.clearPaths();
		}

		String relNewImagePathA = "";
		if (!context.newImagePathA.equals("")) {
			// relNewImagePathA = getNewPathImage(project.projectDirectory,
			// context.newImagePathA);
			relNewImagePathA = context.newImagePathA.substring(project.projectDirectory.length());
		}
		String relNewImagePathB = "";
		if (!context.newImagePathB.equals(""))
			// relNewImagePathB = getNewPathImage(project.projectDirectory,
			// context.newImagePathB);
			relNewImagePathB = context.newImagePathB.substring(project.projectDirectory.length());

		// TODO here we decide what is in the left and the right
		Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "", type);
		return newItem;
	}

	public void loadLastSessionData() {

		context.guiController.normal_shutter_click1(null, null);

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
			if (value.equals("1"))
				context.cameraActiveA = true;
			else
				context.cameraActiveA = false;

			value = lastSessionData.getChild("Camera_B_Active").getContent();
			if (value.equals("1"))
				context.cameraActiveB = true;
			else
				context.cameraActiveB = false;

			project.forceSelectedItem(project.selectedItemIndex, false);

			context.gui.grpAll.setVisible(1, true);

			loading = false;

			// } else {
			//// new_button_click(null, null);
			// }
		} catch (Exception e) {
			context._println("lastSession.xml not found");
			// txtLog.insertText("Error reconstructing last session: check the
			// integrity of your session folder ");
			e.printStackTrace();
			G4P.showMessage(this, "Can't load project", "", G4P.WARNING);
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

		if (context.cameraActiveA)
			value = "1";
		else
			value = "0";
		lastCameraActiveA.setContent(String.valueOf(value));
		lastSessionData.addChild(lastCameraActiveA);

		if (context.cameraActiveB)
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
			// context.gui.page_comments_text.setText("");
			// context.gui.page_num_text.setText("0");

		}
		XML projectDataXML = loadXML("project_template.xml");
		project.loadProjectMetadata(projectDataXML);

		project.rotationA = context.rotationA;
		project.rotationB = context.rotationB;

		project.serialA = context.serialCameraA;
		project.serialB = context.serialCameraB;

		saveXML(projectDataXML, projectFolderPath + "/project.xml");

		File thumbnailsFolder = new File(projectFolderPath + "/thumbnails");
		if (!thumbnailsFolder.exists()) {
			if (thumbnailsFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + thumbnailsFolder.getPath());
				} catch (Exception e) {
					context._println("Couldn't create thumbnail directory permisions");
				}
			} else {
				context._println("Failed to create thumbnail directory!");
			}
		}
		File rawFolder = new File(projectFolderPath + "/raw");
		if (!rawFolder.exists()) {
			if (rawFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + rawFolder.getPath());
				} catch (Exception e) {
					context._println("Couldn't create raw directory permisions");
				}
			} else {
				context._println("Failed to create thumbnail directory!");
			}
		}
		project.projectDirectory = projectFolderPath;
		project.projectFilePath = projectFolderPath + "/project.xml";
		project.selectedItemIndex = -1;
		project.thumbnailsLoaded = true;

		context.init();

		saveLastSessionData();

	}

	public void loadProject(String projectPath) {
		project.selectedItem = null;
		project.loadProjectMethod(projectPath);
		String errors = "";
		if (project.rotationA != context.rotationA) {
			errors += "Rotation A in serials has changed " + project.rotationA + "->" + context.rotationA + "\n";
		}

		if (project.rotationB != context.rotationB) {

			errors += "Rotation B in serials has changed " + project.rotationB + "->" + context.rotationB + "\n";
		}

		if (!project.serialA.equals(context.serialCameraA)) {
			errors += "Serial A in serials has changed " + project.serialA + "->" + context.serialCameraA + "\n";
		}

		if (!project.serialB.equals(context.serialCameraB)) {
			errors += "Serial B in serials has changed " + project.serialB + "->" + context.serialCameraB + "\n";

		}

		if (!errors.equals("")) {
			G4P.showMessage(this, errors, "", G4P.WARNING);
		}

		initSelectedItem = true;
		context.init();
		// G2P5Manager.setImageCount(project.items.size());
		project.forceSelectedItem(project.items.size(), false);
		saveLastSessionData();
		project.removeUnusedImages();
		stateApp = STATE_APP_PROJECT;
		context.gui.grpAll.setVisible(1, true);
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
		if (button == context.gui.btnTrigger) {
			context.guiController.trigger_button_click(null, null);
		}

		if (button == context.gui.btnTriggerRepeat) {
			context.guiController.normal_shutter_click1(null, null);
			noZoom();
		}

		if (button == context.gui.btnTriggerNormal) {
			context.guiController.repeat_shutter_click(null, null);
			noZoom();
		}

		if (button == context.gui.btnTriggerChartColor) {
			context.guiController.calibration_shutter_click(null, null);
			noZoom();
		}

		if (button == context.gui.btnLiveView) {
			context.guiController.liveView_button_click(null, null);
		}

		if (button == context.gui.btnConnectedA) {
			context.guiController.camera_A_active_button_click(null, null);
			noZoom();
		}

		if (button == context.gui.btnConnectedB) {
			context.guiController.camera_B_active_click(null, null);
			noZoom();
		}
	}

	public void settings() {
		// size(595, 1030);
		// size(1920, 1030, P2D);
		fullScreen(P2D);
	}

	// public void loadRightPreview() {
	// project.loadRightPreview();
	// }
	//
	// public void loadLeftPreview() {
	// project.loadLeftPreview();
	// }

	public void oscEvent(OscMessage theOscMessage) {
		/*
		 * print the address pattern and the typetag of the received OscMessage
		 */
		print("### received an osc message.");
		print(" addrpattern: " + theOscMessage.addrPattern());
		println(" typetag: " + theOscMessage.typetag());

		context.capture();

	}

	static public void main(String[] passedArgs) {

		// String location = "--location=0,0";

		/*
		 * GraphicsEnvironment environment =
		 * GraphicsEnvironment.getLocalGraphicsEnvironment(); GraphicsDevice
		 * devices[] = environment.getScreenDevices();
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
