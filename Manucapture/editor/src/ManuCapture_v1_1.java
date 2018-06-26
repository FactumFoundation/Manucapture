
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
	 * Copyright (c) 2016-2018 Jorge Cano and Enrique Esteban in Factum Foundation
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

	int receivePort = 3334;
	int sendPort = 3333;
	int arduinoDriverPort = 13000;

	String baseDirectory = "";

	PApplet parent;

	Project project = null;

	boolean initSelectedItem = false;

	int shutterMode = 0;
	int NORMAL_SHUTTER = 0;
	int REPEAT_SHUTTER = 1;
	// int SUBPAGE_SHUTTER = 2;
	int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;

	ManuCaptureContext context = new ManuCaptureContext();

	PVector lastPressedR = null;
	PVector lastPressedL = null;

	int liveViewActive = -1;

	boolean mock = false;

	// Chart identification
	public static int CAPTURING = 0;
	public static int CHART = 1;

	int state = CAPTURING;
	int chartState = 0;
	// *********************

	int guideHeight_1 = 200;
	int guideHeight_2 = 600;

	int marginTopViewer = 20;
	int marginLeftViewerRight = 1250;
	int marginLeftViewerLeft = 583;

	ItemsViewport itemsViewport;

	HotArea hotAreaSelected = null;

	boolean loadData = true;

	private static int STATE_APP_NO_PROJECT = 0;
	private static int STATE_APP_PROJECT = 1;

	int stateApp = STATE_APP_NO_PROJECT;

	public void setup() {

		System.setOut(new TracingPrintStream(System.out));

		context.parent = this;
		context.thumbnail = new RawFile();
		context.parent = this;
		context.gui = new GUI();
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

		background(backgroundColor);

		frameRate(25);
		context.gui.grpAll.setVisible(0, false);
	}

	public void draw() {

		background(75);

		if (stateApp == STATE_APP_NO_PROJECT) {
			// MOSTRAR CARGAR O NUEVO
			context.gui.grpAll.setVisible(1, false);
			// ellipse(width/2,500,1000,1000);
			textAlign(CENTER);
			
			fill(255);
			textSize(32);
			text("MANUCAPTURE", width / 2, 250);
			textSize(18);
			text("Factum Foundation Version 2.0", width / 2, height-30);
			textSize(20);
			fill(255);
			ellipse(width / 2 - width / 4, 500, 200, 200);
			fill(0);
			text("NEW PROJECT", width / 2 - width / 4, 500);

			fill(255);
			ellipse(width / 2, 500, 200, 200);
			fill(0);
			text("LOAD PREVIOUS", width / 2, 500);

			fill(255);
			ellipse(width / 2 + width / 4, 500, 200, 200);
			fill(0);
			text("LOAD PROJECT", width / 2 + width / 4, 500);

			if (mousePressed) {
				PVector m = new PVector(mouseX, mouseY);
				float dist = m.dist(new PVector(width / 2, 500));
				if (dist < 100) {
					loadLastSessionData();
					
				}

				dist = m.dist(new PVector(width / 2 + width / 4, 500));
				if (dist < 100) {
					load_click(null, null);
				}

				dist = m.dist(new PVector(width / 2 - width / 4, 500));
				if (dist < 100) {
					new_button_click(null, null);
				}
			}

		} else {
//			context.gui.grpAll.setVisible(1, true);
			drawInittializedApp();
		}

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
				process.waitFor();

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
					context.gphotoAAdapter = context.createG2P5(context.serialCameraA, "A");
				}
				if (!context.gphotoB.isConnected()) {
					context.gphotoBAdapter = context.createG2P5(context.serialCameraB, "B");
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
		rect(0, 0, 200, 200);

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
				stroke(255);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}
		
		if (lastPressedL == null)
			for (int i = 1; i <= context.pointsRight.size(); i++) {
				PVector areaPos1 = context.pointsRight.get(i - 1).getRealPosition();
				PVector areaPos2 = context.pointsRight.get(i % context.pointsRight.size()).getRealPosition();
				stroke(255);
				line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}

		stroke(255);

		fill(255);
		text("contextstate " + context.captureState + " state" + state + "\n " + "stateChart " + chartState + "\n "
				+ frameRate, 250, 10);

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
			fill(100, 100);
			rect(0, 0, width, height);
			textSize(32);
			fill(255, 0, 0);
			text("LIVEVIEW MODE ENABLED", width / 2 - 100, height / 2);
			liveViewActive++;
		}

		// stroke(0,0,255);
		// line(0,guideHeight_1,width,guideHeight_1);
		// stroke(0,255,0);
		// line(0,guideHeight_2,width,guideHeight_2);

		// rect(marginLeftViewerLeft, marginTopViewer, 100, 100);

		if (state == CHART) {
			pushMatrix();
			pushStyle();

			if (chartState == 0) {
				textAlign(CENTER);
				fill(255, 0, 0, 100);
				rect(marginLeftViewerLeft, 0, context.hImageViewerSize * 2, context.wImageViewerSize);
				fill(255);
				textSize(24);
				text("CALIBRATING, PLEASE CAPTURE \n THIS BACKGROUND \nWITHOUT ANY DOCUMENT", marginLeftViewerRight,
						200);
			} else {
				textAlign(CENTER);
				if (chartState == 2) {
					translate(marginLeftViewerLeft, 0);
				} else {
					translate(marginLeftViewerRight, 0);
				}

				fill(255, 0, 0, 100);
				rect(0, 0, context.hImageViewerSize, context.wImageViewerSize);
				fill(255);
				textSize(24);
				text("CALIBRATING, PLEASE CAPTURE  THIS CAMERA", context.hImageViewerSize / 2, 200);
			}

			popStyle();
			popMatrix();

		}
	}

	private void drawLeft() {

		if (project.selectedItem != null && project.selectedItem.mImageLeft.imgPreview != null) {

			pushStyle();
			pushMatrix();
			translate(marginLeftViewerRight, marginTopViewer);

			drawImagePreview(project.selectedItem.mImageLeft, lastPressedL, marginLeftViewerRight, context.pointsRight,
					context.scaleA);
			fill(255);
			textSize(14);
			text(project.selectedItem.mImageLeft.imagePath, 0, 0);
			popMatrix();
			if (lastPressedL == null)
				for (HotArea area : context.pointsRight) {
					area.draw(g);
				}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(marginLeftViewerRight, 20, context.hImageViewerSize, context.wImageViewerSize);
		}

		// datos de cámara

		fill(255);
		text("exposure: " + context.gphotoBAdapter.exposure, marginLeftViewerRight + 75, 40);
		text("focusing: ", marginLeftViewerRight + 300, 40);
		text(context.gphotoBAdapter.g2p5.id, 840, 40);
		text("mirroUp " + context.gphotoBAdapter.mirrorUp, marginLeftViewerRight + 75, 60);
		if (context.gphotoBAdapter.focus) {
			fill(255, 0, 0);
		} else {
			fill(0, 255, 0);
		}
		ellipse(marginLeftViewerRight + 370, 35, 15, 15);
	}

	private void drawRight() {

		if (project.selectedItem != null && project.selectedItem.mImageRight.imgPreview != null) {
			pushStyle();
			pushMatrix();
			translate(marginLeftViewerLeft, marginTopViewer);
			imageMode(CORNER);

			drawImagePreview(project.selectedItem.mImageRight, lastPressedR, marginLeftViewerLeft, context.pointsLeft,
					context.scaleB);

			fill(255);
			textSize(14);
			text(project.selectedItem.mImageRight.imagePath, 0, 0);

			popMatrix();
			fill(0, 255, 0, 100);
			if (lastPressedR == null)
				for (HotArea area : context.pointsLeft) {
					area.draw(g);
				}

			popStyle();
		} else {
			stroke(255);
			fill(50);
			rect(580, 20, context.hImageViewerSize, context.wImageViewerSize);
		}
		// datos de cámara
		fill(255);
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
			hotAreaSelected.setRealPosition(mouseX, mouseY);
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

			if (hotAreaSelected == null) {
				if (hotAreaSelected == null) {
					if (mouseButton == LEFT) {
						if (lastPressedL == null)
							updateZoomLeft();
						// else
						// lastPressedL = null;

						if (lastPressedR == null)
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

	private void updateZoomLeft() {
		// lastPressedR = null;
		if (mouseY > marginTopViewer && mouseY < height) {
			// Estamos en y

			if (mouseX > marginLeftViewerRight) {
				lastPressedL = new PVector(mouseX, mouseY);
			} else {

			}
		} else {

		}
	}

	private void updateZoomRight() {
		// lastPressedR = null;
		if (mouseY > marginTopViewer && mouseY < height) {
			// Estamos en y
			if (mouseX > marginLeftViewerLeft && mouseX < marginLeftViewerRight) {

				if (mouseX > marginLeftViewerRight) {

				} else {
					lastPressedR = new PVector(mouseX, mouseY);
				}
			} else {

			}
		}
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

	public void newPhotoEvent(G2P5Event event, String ic) {

		if (project.projectName == null || project.projectName.equals("")) {
			context.handleMessageDialog("Error", "Can't capture photos without project name", G4P.ERROR);
			return;
		}

		println("New photo Event!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", event.content);
		if (event.g2p5 == context.gphotoA) {
			context.gphotoAAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			context.gphotoAAdapter.setFullTargetPath(ic);
			context.moveFile(event.content, context.gphotoAAdapter.getFullTargetPath());
			context.newImagePathA = context.gphotoAAdapter.getFullTargetPath();
		} else if (event.g2p5 == context.gphotoB) {

			context.gphotoBAdapter.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
			context.gphotoBAdapter.setFullTargetPath(ic);
			context.moveFile(event.content, context.gphotoBAdapter.getFullTargetPath());
			context.newImagePathB = context.gphotoBAdapter.getFullTargetPath();
		}

		if ((context.gphotoA.isConnected() && context.gphotoB.isConnected()
				&& (!context.newImagePathA.equals("") && !context.newImagePathB.equals("")))
				|| (context.gphotoA.isConnected() && !context.gphotoB.isConnected()
						&& !context.newImagePathA.equals(""))
				|| (!context.gphotoA.isConnected() && context.gphotoB.isConnected()
						&& !context.newImagePathB.equals(""))) {
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

				if (chartState < 2) {
					doNormalShutter(Item.TYPE_BACKGROUND);
				} else if (chartState < 2) {
					// we do background and first photo normally
					doNormalShutter(Item.TYPE_CHART);
				} else {
					float newPageNum = project.selectedItem.pagNum;

					Item newItem = initNewItem(Item.TYPE_CHART, newPageNum);
					newItem.loadThumbnails();
					project.replaceItem(project.selectedItemIndex, newItem);
					context.clearPaths();
				}
			}
		}
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

		String value;
		try {
			XML lastSessionData = loadXML("lastSession.xml");
			// int reply = G4P.selectOption(this, "Load previous session?", "", G4P.PLAIN,
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
			// } else {
			//// new_button_click(null, null);
			// }
		} catch (Exception e) {
			context._println("lastSession.xml not found");
			// txtLog.insertText("Error reconstructing last session: check the
			// integrity of your session folder ");
			e.printStackTrace();
			G4P.showMessage(this, "Can't load project", "", G4P.WARNING);
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
			context.gui.page_comments_text.setText("");
			context.gui.page_num_text.setText("0");

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

	public synchronized void loadProject(String projectPath) {
		project.loadProjectMethod(projectPath);
		initSelectedItem = true;
		context.init();
		G2P5Manager.setImageCount(project.items.size());
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

	/*
	 * ========================================================= ==== WARNING ===
	 * ========================================================= The code in this
	 * tab has been generated from the GUI form designer and care should be taken
	 * when editing this file. Only add/edit code inside the event handlers i.e.
	 * only use lines between the matching comment tags. e.g.
	 * 
	 * void myBtnEvents(GButton button) { //_CODE_:button1:12356: // It is safe to
	 * enter your event code here } //_CODE_:button1:12356:
	 * 
	 * Do not rename this tab!
	 * =========================================================
	 */

	public void first_page_button_click(GButton source, GEvent event) { // _CODE_:first_page_button:431616:
		println("SHUTTER CONTROL SET NOTMAL MODE");
	} // _CODE_:first_page_button:431616:

	public void last_page_button_click(GButton source, GEvent event) { // _CODE_:last_page_button:647539:
		println("button2 - GButton >> GEvent." + event + " @ " + millis());

	} // _CODE_:last_page_button:647539:

	public void name_text_change(GTextField source, GEvent event) { // _CODE_:name_text:702135:
		project.projectName = source.getText();
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			project.saveProjectXML();
		}
	} // _CODE_:name_text:702135:

	public void code_text_change(GTextField source, GEvent event) { // _CODE_:code_text:779005:
		project.projectCode = source.getText();
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			project.saveProjectXML();
		}
	} // _CODE_:code_text:779005:

	public void author_text_change(GTextField source, GEvent event) { // _CODE_:author_text:873016:
		project.projectAuthor = source.getText();
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			project.saveProjectXML();
		}
	} // _CODE_:author_text:873016:

	public void project_comments_change(GTextField source, GEvent event) { // _CODE_:project_comments_text:337734:
		project.projectComment = source.getText();
		println();
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			project.saveProjectXML();
		}

	} // _CODE_:project_comments_text:337734:

	public void page_comments_text_change(GTextField source, GEvent event) { // _CODE_:page_comments_text:397499:
		if (project.selectedItem != null) {
			project.selectedItem.comment = source.getText();
		}
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			project.saveProjectXML();
		}
	} // _CODE_:page_comments_text:397499:

	public void page_num_text_change(GTextField source, GEvent event) { // _CODE_:textfield1:363899:
		if (event.toString() == "ENTERED" && !project.projectDirectory.equals("")) {
			try {
				float pageNumber = Float.parseFloat(source.getText());
				int itemIndex = project.findItemIndexByPagNum(pageNumber);
				if (itemIndex != -1) {
					project.forceSelectedItem(itemIndex, true);
				}
				project.saveProjectXML();
			} catch (NumberFormatException ex) {
				println("wrong page number");
			}
		}

	} // _CODE_:textfield1:363899:

	public void normal_shutter_click1(GButton source, GEvent event) { // _CODE_:normal_shutter_button:563899:
		println("SHUTTER CONTROL SET NORMAL MODE!!!!!");
		shutterMode = NORMAL_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:normal_shutter_button:563899:

	public void repeat_shutter_click(GButton source, GEvent event) { // _CODE_:repeat_shutter_button:591981:
		println("SHUTTER CONTROL SET REPEAT MODE");
		shutterMode = REPEAT_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:repeat_shutter_button:591981:

	// public void subpage_shutter_click(GButton source, GEvent event) { //
	// _CODE_:subpage_shutter_button:295319:
	// println("SHUTTER CONTROL SET SUBPAGE MODE");
	// shutterMode = SUBPAGE_SHUTTER;
	// GUI gui = context.gui;
	// gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// } // _CODE_:subpage_shutter_button:295319:

	public void close_popup_project_window(GWindow window) {
		close_popup_project(null, null);
	}

	public void mouse_popUp(PApplet applet, GWinData windata) {
		println("holle2" + windata);
	}

	public void mouse_popUp(PApplet applet, GWinData windata, MouseEvent ouseevent) {
		println("holl1e" + ouseevent.getAction());
	}

	public void close_popup_project(GButton source, GEvent event) {
		// context.gui.window.close();

		boolean someError = false;
		if (StringUtils.isEmpty(context.project.projectName)) {
			someError = true;
		}

		if (StringUtils.isEmpty(context.project.projectCode)) {
			someError = true;
		}

		if (!someError) {
			stateApp = STATE_APP_PROJECT;
			context.gui.grpAll.setVisible(1,true);
			context.gui.grpProject.setVisible(1,false);
			context.project.saveProjectXML();
			
			
		} else {
			// showwhat error
			G4P.showMessage(this, "Missing name or code", "", G4P.WARNING);
		}

		println("close window edit project data");
	}

	public void calibration_shutter_click(GButton source, GEvent event) { // _CODE_:calibration_shutter_button:835827:

		// if (state == CAPTURING) {
		println("SHUTTER CONTROL SET CALIBRATION MODE");
		shutterMode = CALIB_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);

		state = CHART;
		chartState = 0;
		// }
	} // _CODE_:calibration_shutter_button:835827:

	public void trigger_button_click(GButton source, GEvent event) { // _CODE_:trigger_button:381491:
		println("SHUTTER TRIGGERED");
		context.capture();
		context.clearPaths();
		if (state == CHART) {
			chartState++;
			if (chartState == 3) {
				state = CAPTURING;
				normal_shutter_click1(source, event);
			}
		}
	} // _CODE_:trigger_button:381491:

	public void camera_A_connected_click(GButton source, GEvent event) { // _CODE_:camera_A_connected_button:265149:
		println("button1 - GButton >> GEvent." + event + " @ " + millis());
		if (!context.gphotoA.isConnected()) {
			context.gphotoAAdapter = context.createG2P5(context.serialCameraA, "A");
		}
	} // _CODE_:camera_A_connected_button:265149:

	public void camera_A_active_button_click(GButton source, GEvent event) { // _CODE_:camera_A_active_button:906773:
		println("camera_A_active_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		context.cameraActiveA = true;
		context.gphotoA.setActive(true);
		if (context.gphotoA.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoA.active = true;
		}
	} // _CODE_:camera_A_active_button:906773:

	public void camera_A_inactive_button_click(GButton source, GEvent event) { // _CODE_:camera_A_inactive_button:493860:
		println("inactive_camera_A_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		context.cameraActiveA = false;
		context.gphotoA.setActive(false);

	} // _CODE_:camera_A_inactive_button:493860:

	public void camera_B_connected_click(GButton source, GEvent event) { // _CODE_:camera_B_connected_button:564189:
		println("camera_B_connected_button - GButton >> GEvent." + event + " @ " + millis());
		if (!context.gphotoB.isConnected()) {
			context.gphotoBAdapter = context.createG2P5(context.serialCameraB, "B");
		}
	} // _CODE_:camera_B_connected_button:564189:

	public void camera_B_active_click(GButton source, GEvent event) { // _CODE_:camera_B_active_button:640605:
		println("camera_B_active_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		context.cameraActiveB = true;
		context.gphotoB.setActive(true);
		if (context.gphotoB.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoB.active = true;
		}

	} // _CODE_:camera_B_active_button:640605:

	public void camera_B_inactive_click(GButton source, GEvent event) { // _CODE_:camera_B_inactive_button:780199:
		println("camera_B_inactive_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		context.cameraActiveB = false;
		context.gphotoB.setActive(false);

	} // _CODE_:camera_B_inactive_button:780199:

	public void parameters_click(GButton source, GEvent event) { // _CODE_:parameters_button:465510:
		println("parameters_button - GButton >> GEvent." + event + " @ " + millis());
	} // _CODE_:parameters_button:465510:

	public void load_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		String documentFileName = G4P.selectInput("Load XML");
		if (documentFileName != null) {
			loadProject(documentFileName);
		}

	} // _CODE_:load_button:841968:

	public void edit_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		context.gui.grpProject.setVisible(1,true);
		context.gui.grpAll.setVisible(1,false);

	} // _CODE_:load_button:841968:

	public void close_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		stateApp = STATE_APP_NO_PROJECT;

	} // _CODE_:load_button:841968:

	public void new_button_click(GButton source, GEvent event) { // _CODE_:new_button:324180:
		String projectFolderPath = G4P.selectFolder("Select the project folder for NEW PROJECT");
		if (projectFolderPath != null) {
			project.thumbnailsLoaded = false;
			context.gui.grpProject.setVisible(1,true);
			createProject(projectFolderPath);

		}
	} // _CODE_:new_button:324180:

	public void page_search_text_change(GTextField source, GEvent event) { // _CODE_:page_search_text:741750:
		println("textfield2 - GTextField >> GEvent." + event + " @ " + millis());
	} // _CODE_:page_search_text:741750:

	public void liveView_button_click(GButton source, GEvent event) { // _CODE_:export_button:581416:
		// println("export_button - GButton >> GEvent." + event + " @ " +
		// millis());
		liveViewActive = 0;

	} // _CODE_:export_button:581416:

	public void settings() {
		// size(595, 1030);
		size(1920, 1030, P2D);
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

		String location = "--location=0,0";

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
			String[] appletArgs = new String[] { "ManuCapture_v1_1", location };
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
