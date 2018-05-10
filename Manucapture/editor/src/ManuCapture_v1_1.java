
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import g4p_controls.*;
import java.io.*;
import java.text.SimpleDateFormat;

import org.apache.commons.io.FilenameUtils;
import java.awt.Font;
import netP5.*;
import oscP5.*;
import java.util.*;

import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.IOException;

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

	/*
	 * 
	 * Todo:
	 * 
	 * Viewer: Left image correspons to the left -> should be on the right
	 * Switch images Left is upside down and right is mirrored Botón para
	 * extraer snapshots
	 * 
	 * 
	 * Editor: Procesar lista de thumbnails pendientes en un thread aparte y
	 * metadatos en un thread aparte Calibration: Definir con Pereira Al iniciar
	 * no se situa en el útimo elemento Cajas de texto adaptables al tamaño, no
	 * funcionansaltos de linea Activar/Desactiva camaras Feedback visual de que
	 * se está capturando foto (cambiar background a anaranjado)
	 * 
	 * Al añadir subpágina no se suman todas las páginas sub siguientes AL
	 * eliminar subpágina que no se renombren las siguientes
	 * 
	 */

	// Need install G4P, OSC, Apache.Commons.io library

	PrintWriter logOutput;

	OscP5 oscP5;
	NetAddress viewerLocation;
	int receivePort = 3334;
	int sendPort = 3333;

	String documentFileName = "";

	int val = 0;
	boolean reversed = false;

	long millisToCheckPairs = 9000;
	long millisLastSaveMessage = 2000;
	boolean firstSessionSaveMessage = true;

	int COMPUTER_STATE_DISCONNECTED = 0;
	int COMPUTER_STATE_CONNECTING = 1;
	int COMPUTER_STATE_CONNECTED = 2;
	int COMPUTER_STATE_DISCONNECTING = 3;
	int computerState = COMPUTER_STATE_DISCONNECTED;
	int lastComputerState = COMPUTER_STATE_DISCONNECTED;
	boolean disconnect = false;
	long lastMillisConnected = 0;
	long disconnectTime = 45000;

	int wheelLength = 20;
	int wheelPosX = 300;
	int wheelPosY = 465;
	float wheelAngle = 0.0f;
	float wheelAngleSpeed = PI / 80.0f;

	String IDToOpen;
	int lastTimeImageReady;
	int millisToViewImage = 3300;
	boolean imageOpened = true;

	int numPhotosUp = 1;
	int numPhotosDown = 1;

	String baseDirectory = "";
	String dpi = "300";

	long lastTimeUpMsg = 0;
	long lastTimeDownMsg = 0;
	long maxTimeWithoutMsg = 3000;

	boolean computerConnected_A = false;
	boolean computerConnected_B = false;

	PApplet parent;

	int itemListViewPortX = 5;
	int itemListViewPortY = 120;
	int itemListViewPortWidth = 285;
	int itemListViewPortHeight = 900;

	String projectFilePath = "";
	String projectDirectory = "";
	String projectName = "";
	String projectComment = "";
	String projectCode = "";
	String projectAuthor = "";
	ArrayList<Item> items = new ArrayList<Item>();

	int selectedItemIndex = -1;
	int overedItemIndex;
	int overedCancelButtonIndex = -1;
	int selectItemColor = 0xffB7691F;
	int selectItemColorStroke = 0xffEA780C;
	PGraphics itemsViewPort;
	int scrollBarWidth = 15;
	int scrollBarColor = 0xff62605E;
	int scrollHandleColorIdle = 0xff0F99CE;
	int scrollHandleColorOver = 0xffD1F0FC;
	int scrollHandleColorPressed = 0xffFA8728;
	float scrollHandleHeight = 200; // Test
	float scrollHandleY = 0;
	int scrollHandleState;
	int SCROLL_HANDLE_IDLE = 0;
	int SCROLL_HANDLE_OVER = 1;
	int SCROLL_HANDLE_PRESSED = 2;

	boolean thumbnailsLoaded = false;
	boolean initSelectedItem = false;

	PImage removeItemIcon;
	int marginX = 2;
	int marginSubpgX = 8;
	int marginInfo = 10;
	int marginY = 10;
	int itemThumbHeight = 160;
	int removeIconSize = 20;
	int overItemColor = 0xff4B4949;

	int shutterMode = 0;
	int NORMAL_SHUTTER = 0;
	int REPEAT_SHUTTER = 1;
	int SUBPAGE_SHUTTER = 2;
	int CALIB_SHUTTER = 3;

	int backgroundColor = 0xff000C12;
	PImage bookIcon;

	G2P5 gphotoA;
	G2P5 gphotoB;
	boolean cameraActiveA = false;
	boolean cameraActiveB = false;

	String serialCameraA;
	String serialCameraB;

	PImage previewImgLeft = null;
	PImage previewImgRight = null;

	String newImagePathA = "";
	String newImagePathB = "";

	ManuCaptureContext context = new ManuCaptureContext();

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

	public void setup() {

		context.parent = this;
		context.thumbnail = new Thumbnail();
		
		if (surface != null) {
			surface.setLocation(0, 0);
		}
		XML serialXML = loadXML("cameraSerials.xml");
		serialCameraA = serialXML.getChild("Camera_A").getContent();
		serialCameraB = serialXML.getChild("Camera_B").getContent();

		G2P5.init(0);
		gphotoA = G2P5.create(this, serialCameraA, "A");
		gphotoA.setTargetFile(sketchPath(), "test");
		gphotoB = G2P5.create(this, serialCameraB, "B");
		gphotoB.setTargetFile(sketchPath(), "test");

		createGUI();
		customGUI();

		itemsViewPort = createGraphics(itemListViewPortWidth, itemListViewPortHeight);
		scrollHandleState = SCROLL_HANDLE_IDLE;
		textMode(MODEL);

		removeItemIcon = loadImage("cross_inv_20x20.jpeg");

		oscP5 = new OscP5(this, receivePort);
		viewerLocation = new NetAddress("127.0.0.1", sendPort);

		loadLastSessionData();
		bookIcon = loadImage("bookIcon.png");
		bookIcon.resize(bookIcon.width / 6, bookIcon.height / 6);

		
	}

	int lastDrawedItems = -1;
	int ADDING_ITEM_TRANSITION = 1;
	int REMOVING_ITEM_TRANSITION = 2;
	int NO_TRANSITION = 0;
	int itemsViewTransition = NO_TRANSITION;
	float transitionPhase = 0.0f;
	float itemBaseY = 0;

	public void draw() {

		if (surface != null) {
			surface.setLocation(0, 0);
		}

		background(backgroundColor);

		// CAMERA STATE SECTION
		// ///////////////////////////////////////////////////////

		if (gphotoA.isConnected()) {
			camera_A_connected_button.setText("CONNECTED");
			camera_A_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			camera_A_connected_button.setText("DISCONNECTED");
			camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		if (gphotoB.isConnected()) {
			camera_B_connected_button.setText("CONNECTED");
			camera_B_connected_button.setLocalColorScheme(GCScheme.GREEN_SCHEME);
		} else {
			camera_B_connected_button.setText("DISCONNECTED");
			camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		}

		// ITEM LIST VIEW PORT SECTION
		// /////////////////////////////////////////////////
		// Items view port
		int itemHeight = itemThumbHeight + marginY;

		// update item list related values
		float targetItemBaseY;
		int fullListHeight = itemHeight * items.size();

		if (items.size() == 0) {
			targetItemBaseY = 0.0f;
		} else {
			targetItemBaseY = map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
		}

		if (scrollTransitionState == NO_SCROLL_TRANSITION)
			itemBaseY = targetItemBaseY;
		else {
			if (abs(targetItemBaseY - itemBaseY) < 20 || items.size() <= 1) {
				scrollTransitionState = NO_SCROLL_TRANSITION;
				itemBaseY = targetItemBaseY;
			} else {
				if (targetItemBaseY > itemBaseY)
					itemBaseY += 10f;
				else {
					itemBaseY -= 10f;
				}
			}

		}

		if (fullListHeight == 0) {
			scrollHandleHeight = itemsViewPort.height;
		} else {
			scrollHandleHeight = map(itemsViewPort.height, 0, fullListHeight, 0, itemsViewPort.height);

		}

		itemsViewPort.beginDraw();
		itemsViewPort.background(0);
		itemsViewPort.noStroke();
		itemsViewPort.fill(scrollBarColor);
		itemsViewPort.rect(itemsViewPort.width - scrollBarWidth, 0, scrollBarWidth, itemsViewPort.height);
		if (scrollHandleState == SCROLL_HANDLE_IDLE) {
			itemsViewPort.fill(scrollHandleColorIdle);
		} else if (scrollHandleState == SCROLL_HANDLE_OVER) {
			itemsViewPort.fill(scrollHandleColorOver);
		} else if (scrollHandleState == SCROLL_HANDLE_PRESSED) {
			// println("PRESSED",scrollHandleColorPressed);
			itemsViewPort.fill(scrollHandleColorPressed);
		}
		itemsViewPort.rect(itemsViewPort.width - scrollBarWidth, scrollHandleY, scrollBarWidth, scrollHandleHeight);

		if (items.size() == 0 && !projectDirectory.equals("")) {
			itemsViewPort.stroke(selectItemColorStroke);
			itemsViewPort.fill(selectItemColor);
			int iconWidht = itemsViewPort.width - scrollBarWidth - 1;
			itemsViewPort.rect(0, -marginY / 2, itemsViewPort.width - scrollBarWidth - 1, itemHeight);
			itemsViewPort.image(bookIcon, (iconWidht - bookIcon.width) / 2, (itemHeight - bookIcon.height) / 2);
		} else {
			// items
			if (thumbnailsLoaded) {

				// TODO: Add transition when adding
				if (lastDrawedItems != items.size() && lastDrawedItems != -1
						&& (selectedItemIndex != items.size() - 1)) {
					if (lastDrawedItems < items.size()) {
						itemsViewTransition = ADDING_ITEM_TRANSITION;
						transitionPhase = 0.0f;
					} else {
						itemsViewTransition = REMOVING_ITEM_TRANSITION;
						transitionPhase = 1.0f;
					}

				}

				if (itemsViewTransition == ADDING_ITEM_TRANSITION) {
					transitionPhase += 0.1f;
					if (transitionPhase >= 1.0f) {
						itemsViewTransition = NO_TRANSITION;
					}
				} else if (itemsViewTransition == REMOVING_ITEM_TRANSITION) {
					transitionPhase -= 0.1f;
					if (transitionPhase <= 0.0f) {
						itemsViewTransition = NO_TRANSITION;
					}
				}

				int heightInc = itemThumbHeight + marginY;
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += heightInc) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					heightInc = itemThumbHeight + marginY;
					if (i < items.size() && viewPortRelativeHeight > -itemHeight
							&& viewPortRelativeHeight < itemsViewPort.height) {
						Item item = items.get(i);
						if (i == overedItemIndex && i != selectedItemIndex) {
							itemsViewPort.stroke(scrollBarColor);
							itemsViewPort.fill(overItemColor);
							itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
									itemsViewPort.width - scrollBarWidth - 1, itemHeight);
						} else if (i == selectedItemIndex) {
							if (itemsViewTransition == NO_TRANSITION) {
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, itemHeight);
							} else if (itemsViewTransition == ADDING_ITEM_TRANSITION) {
								int emptyHeight = (int) (transitionPhase * (itemThumbHeight + marginY));
								heightInc = emptyHeight;
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, heightInc);

							} else if (itemsViewTransition == REMOVING_ITEM_TRANSITION) {
								int emptyHeight = (int) (transitionPhase * (itemThumbHeight + marginY));
								heightInc = emptyHeight + itemThumbHeight + marginY;
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, heightInc);
								viewPortRelativeHeight += emptyHeight;
							}
						}

						if ((i != selectedItemIndex) || (itemsViewTransition != ADDING_ITEM_TRANSITION)) {
							if (item.imgThumbRight != null) {
								itemsViewPort.image(item.imgThumbRight, marginX + item.imgThumbLeft.width,
										viewPortRelativeHeight);
							}
							if (item.imgThumbLeft != null) {
								itemsViewPort.image(item.imgThumbLeft, marginX, viewPortRelativeHeight);
							}
							itemsViewPort.noFill();
							itemsViewPort.stroke(255);
							if (i == overedCancelButtonIndex) {
								itemsViewPort.tint(255, 20, 0);
							} else {
								itemsViewPort.tint(200, 0, 0);
							}

							itemsViewPort.image(removeItemIcon,
									itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize,
									viewPortRelativeHeight + marginY);

							itemsViewPort.noTint();
							String page = String.valueOf(item.pagNum);
							page = page.replace(".0", "");
							float pageNumberWidth = textWidth(page) + 10;
							itemsViewPort.noStroke();
							itemsViewPort.fill(0);
							itemsViewPort.rect(itemsViewPort.width - scrollBarWidth - marginInfo - pageNumberWidth - 8,
									viewPortRelativeHeight + itemThumbHeight - marginY - 18, pageNumberWidth, 21);
							itemsViewPort.fill(200);
							itemsViewPort.textSize(18);
							itemsViewPort.text(page,
									itemsViewPort.width - scrollBarWidth - marginInfo - pageNumberWidth - 5,
									viewPortRelativeHeight + itemThumbHeight - marginY);
							itemsViewPort.stroke(0xff585757);
							int spacerMargin = 2;
							int spacerHeight = viewPortRelativeHeight + itemHeight + marginY / 2;
							itemsViewPort.line(spacerMargin, spacerHeight,
									itemsViewPort.width - scrollBarWidth - 2 * spacerMargin, spacerHeight);
						}
					} else {
						heightInc = itemThumbHeight + marginY;
					}
				}
				lastDrawedItems = items.size();
			}

		}
		itemsViewPort.endDraw();
		image(itemsViewPort, itemListViewPortX, itemListViewPortY);

		if (previewImgLeft != null) {
			pushMatrix();
			translate(580 + previewImgLeft.height / 2, 20 + previewImgLeft.width / 2);
			rotate(PI / 2);
			imageMode(CENTER);
			image(previewImgLeft, 0, 0);
			imageMode(CORNER);
			popMatrix();
		} else {
			stroke(255);
			fill(50);
			rect(580, 20, 667, 1000);
		}

		if (previewImgRight != null) {
			pushMatrix();
			translate(1250 + previewImgRight.height / 2, 20 + previewImgRight.width / 2);
			rotate(3 * PI / 2);
			imageMode(CENTER);
			image(previewImgRight, 0, 0);
			imageMode(CORNER);
			popMatrix();
		} else {
			stroke(255);
			fill(50);
			rect(1250, 20, 667, 1000);
		}

	}

	public void mouseMoved() {

		overedItemIndex = -1;
		overedCancelButtonIndex = -1;

		int scrollHandleX = itemsViewPort.width - scrollBarWidth;
		if ((mouseX > (itemListViewPortX + scrollHandleX))
				&& (mouseX < (itemListViewPortX + scrollHandleX + scrollBarWidth))) {
			if ((mouseY > (itemListViewPortY + scrollHandleY))
					&& (mouseY < (itemListViewPortY + scrollHandleY + scrollHandleHeight))) {
				scrollHandleState = SCROLL_HANDLE_OVER;
				return;
			}
		}

		if ((mouseX > itemListViewPortX) && (mouseX < (itemsViewPort.width + itemListViewPortX))) {
			if ((mouseY > itemListViewPortY) && (mouseY < (itemListViewPortY + itemsViewPort.height))) {
				int itemHeight = itemThumbHeight + marginY;
				int fullListHeight = itemHeight * items.size();
				float itemBaseY = map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += itemHeight) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					if (viewPortRelativeHeight > -itemHeight && viewPortRelativeHeight < itemsViewPort.height) {
						// check item overed
						if ((mouseY > (viewPortRelativeHeight + itemListViewPortY))
								&& (mouseY < (viewPortRelativeHeight + itemHeight + itemListViewPortY))) {
							overedItemIndex = i;
							// check over remove button
							float cancelButtonX = itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize
									+ itemListViewPortX;
							float cancelButtonY = viewPortRelativeHeight + marginY + itemListViewPortY;
							if ((mouseX > cancelButtonX) && (mouseX < (cancelButtonX + removeIconSize))) {
								if ((mouseY > cancelButtonY) && (mouseY < (cancelButtonY + removeIconSize))) {
									overedCancelButtonIndex = i;
									break;
								}
							}
						}
					}
				}
			}
		}
		scrollHandleState = SCROLL_HANDLE_IDLE;
	}

	public void mousePressed() {

		int scrollHandleX = itemsViewPort.width - scrollBarWidth;
		if ((mouseX > (itemListViewPortX + scrollHandleX))
				&& (mouseX < (itemListViewPortX + scrollHandleX + scrollBarWidth))) {
			if ((mouseY > (itemListViewPortY + scrollHandleY))
					&& (mouseY < (itemListViewPortY + scrollHandleY + scrollHandleHeight))) {
				scrollHandleState = SCROLL_HANDLE_PRESSED;
				return;
			}
		}
		if ((mouseX > itemListViewPortX) && (mouseX < (itemsViewPort.width + itemListViewPortX))) {
			if ((mouseY > itemListViewPortY) && (mouseY < (itemListViewPortY + itemsViewPort.height))) {
				int itemHeight = itemThumbHeight + marginY;
				int fullListHeight = itemHeight * items.size();
				float itemBaseY = map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += itemHeight) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					if (viewPortRelativeHeight > -itemHeight && viewPortRelativeHeight < itemsViewPort.height) {
						// check item overed
						if ((mouseY > (viewPortRelativeHeight + itemListViewPortY))
								&& (mouseY < (viewPortRelativeHeight + itemHeight + itemListViewPortY))) {
							selectedItemIndex = i;
							// check over remove button
							float cancelButtonX = itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize
									+ itemListViewPortX;
							float cancelButtonY = viewPortRelativeHeight + marginY + itemListViewPortY;
							if ((mouseX > cancelButtonX) && (mouseX < (cancelButtonX + removeIconSize))) {
								if ((mouseY > cancelButtonY) && (mouseY < (cancelButtonY + removeIconSize))) {
									if (shutterMode != REPEAT_SHUTTER) {
										removeItem(i);
									} else {
										clearItem(i);
									}
									break;
								}
							}
							selectItem(selectedItemIndex);
						}
					}
				}
			}
		}
		scrollHandleState = SCROLL_HANDLE_IDLE;
	}

	public void mouseDragged() {
		if (scrollHandleState == SCROLL_HANDLE_PRESSED) {
			if (scrollHandleHeight < itemsViewPort.height) {
				scrollHandleY += mouseY - pmouseY;
				if (scrollHandleY < 0) {
					scrollHandleY = 0;
				} else if ((scrollHandleY + scrollHandleHeight) > itemsViewPort.height) {
					scrollHandleY = itemsViewPort.height - scrollHandleHeight;
				}
			} else
				scrollHandleY = 0;
		}
	}

	public void mouseReleased() {
		scrollHandleState = SCROLL_HANDLE_IDLE;

	}

	public void newPhotoEvent(G2P5 gphoto, String absoluteFilePath) {

		println("New photo!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", absoluteFilePath);
		if (gphoto == gphotoA) {
			newImagePathA = absoluteFilePath;

		} else if (gphoto == gphotoB) {
			newImagePathB = absoluteFilePath;
		}

		if ((gphotoA.isConnected() && gphotoB.isConnected() && (!newImagePathA.equals("") && !newImagePathB.equals("")))
				|| (gphotoA.isConnected() && !gphotoB.isConnected() && !newImagePathA.equals(""))
				|| (!gphotoA.isConnected() && gphotoB.isConnected() && !newImagePathB.equals(""))) {
			// delay(3000);
			if (shutterMode == NORMAL_SHUTTER) {

				float newPageNum;
				if (selectedItemIndex < 0) {
					newPageNum = 1;
				} else {
					newPageNum = (int) items.get(selectedItemIndex).pagNum + 1;
				}
				// TODO add new item: PRoblem when only one image arrives

				String relNewImagePathA = "";

				if (projectDirectory.equals("")) {
					newImagePathA = "";
					newImagePathB = "";
				}

				if (!newImagePathA.equals(""))
					relNewImagePathA = newImagePathA.substring(projectDirectory.length() + 1, newImagePathA.length());
				String relNewImagePathB = "";
				if (!newImagePathB.equals(""))
					relNewImagePathB = newImagePathB.substring(projectDirectory.length() + 1, newImagePathB.length());

				Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "", "Item");
				newItem.loadThumbnails(context, newImagePathA, newImagePathB);

				addItem(selectedItemIndex + 1, newItem);
				newImagePathA = "";
				newImagePathB = "";

			} else if (shutterMode == SUBPAGE_SHUTTER) {

				if (items.size() > 0) {
					float currentPN = selectedItem.pagNum;
					float newPageNum = round((currentPN + 0.1f) * 10) / 10.0f;
					// TODO add new item

					if (projectDirectory.equals("")) {
						newImagePathA = "";
						newImagePathB = "";
					}

					String relNewImagePathA = "";
					if (newImagePathA != null && (newImagePathA.length() > projectDirectory.length() + 1))
						relNewImagePathA = newImagePathA.substring(projectDirectory.length() + 1,
								newImagePathA.length());
					String relNewImagePathB = "";
					if (newImagePathB != null && (newImagePathB.length() > projectDirectory.length() + 1))
						relNewImagePathB = newImagePathB.substring(projectDirectory.length() + 1,
								newImagePathB.length());

					Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "", "SubItem");
					newItem.loadThumbnails(context, newImagePathA, newImagePathB);
					addSubItem(selectedItemIndex + 1, newItem);
					newImagePathA = "";
					newImagePathB = "";
				}

			} else if (shutterMode == REPEAT_SHUTTER) {
				if (items.size() > 0) {
					float newPageNum = selectedItem.pagNum;

					if (projectDirectory.equals("")) {
						newImagePathA = "";
						newImagePathB = "";
					}

					String relNewImagePathA = "";
					if (!newImagePathA.equals(""))
						relNewImagePathA = newImagePathA.substring(projectDirectory.length() + 1,
								newImagePathA.length());
					String relNewImagePathB = "";
					if (!newImagePathB.equals(""))
						relNewImagePathB = newImagePathB.substring(projectDirectory.length() + 1,
								newImagePathB.length());

					Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "",
							selectedItem.type);
					newItem.loadThumbnails();
					replaceItem(selectedItemIndex, newItem);
					newImagePathA = "";
					newImagePathB = "";
				}

			} else if (shutterMode == CALIB_SHUTTER) {
				println("Calib shutter");
			}
		}

	}

	public void loadLastSessionData() {

		String value;
		try {
			XML lastSessionData = loadXML("lastSession.xml");
			int reply = G4P.selectOption(this, "Load previous session?", "", G4P.PLAIN, G4P.YES_NO);
			if (reply == 0) {
				// TODO: Load project here
				File projectFile = new File(lastSessionData.getChild("Project").getContent());
				if (projectFile.exists())
					loadProject(projectFile.getPath());
				else
					println("Error loading the project: Project file doesn't exist");

				selectedItemIndex = new Integer(lastSessionData.getChild("Current_Item").getContent());

				value = lastSessionData.getChild("Camera_A_Active").getContent();
				if (value.equals("1"))
					cameraActiveA = true;
				else
					cameraActiveA = false;

				value = lastSessionData.getChild("Camera_B_Active").getContent();
				if (value.equals("1"))
					cameraActiveB = true;
				else
					cameraActiveB = false;

				forceSelectedItem(selectedItemIndex, false);

			}

		} catch (Exception e) {
			_println("lastSession.xml not found");
			// txtLog.insertText("Error reconstructing last session: check the
			// integrity of your session folder ");
			e.printStackTrace();
		}
	}

	public void saveLastSessionData() {

		String value;

		XML lastSessionData = new XML("Session");

		XML lastdocumentFileName = new XML("Project");
		lastdocumentFileName.setContent(projectFilePath);
		lastSessionData.addChild(lastdocumentFileName);

		XML lastCurrentItem = new XML("Current_Item");
		lastCurrentItem.setContent(String.valueOf(selectedItemIndex));
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
		if (!projectFilePath.equals("")) {
			closeProject();
			page_comments_text.setText("");
			page_num_text.setText("0");

		}
		XML projectDataXML = loadXML("project_template.xml");
		loadProjectMetadata(projectDataXML);
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
		projectDirectory = projectFolderPath;
		projectFilePath = projectFolderPath + "/project.xml";
		selectedItemIndex = -1;
		thumbnailsLoaded = true;
		gphotoA.setTargetFile(projectDirectory + "/raw", projectCode);
		gphotoB.setTargetFile(projectDirectory + "/raw", projectCode);

		saveLastSessionData();

	}

	public synchronized void loadProject(String projectPath) {

		if (!projectFilePath.equals("")) {
			closeProject();
		}

		if (projectPath.equals("")) {
			G4P.showMessage(this, "ERROR: Problem opening last session project. Please load the folder manually.",
					"Save project", G4P.ERROR);
			return;
		}

		projectFilePath = projectPath;
		XML projectDataXML = loadXML(projectPath);
		loadProjectMetadata(projectDataXML);
		XML[] itemsXML = projectDataXML.getChild("items").getChildren("item");
		for (int i = 0; i < itemsXML.length; i++) {
			try {
				XML itemXML = itemsXML[i];
				String imagePathLeft = itemXML.getChild("image_left").getContent();
				String imagePathRight = itemXML.getChild("image_right").getContent();
				String pageNumStr = itemXML.getChild("page_num").getContent();

				float pageNum = Float.parseFloat(pageNumStr);
				String comment = itemXML.getChild("comment").getContent();
				String type = itemXML.getChild("type").getContent();
				Item newItem = new Item(context, imagePathLeft, imagePathRight, pageNum, comment, type);
				items.add(newItem);
			} catch (Exception e) {
				println("ERROR loading item", i);
			}
		}
		File projectFile = new File(projectPath);
		projectDirectory = projectFile.getParent();

		File thumbnailsFolder = new File(projectDirectory + "/thumbnails");
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

		File projectDirectoryFile = new File(projectDirectory);
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (!item.imagePathLeft.equals("")) {
				File itemImgLeft = new File(projectDirectory + "/" + item.imagePathLeft);
				if (itemImgLeft.exists()) {
					String fileName = itemImgLeft.getName();
					String thumbnailPath = projectDirectoryFile.getPath() + "/thumbnails/"
							+ FilenameUtils.removeExtension(fileName) + "_thumb.jpg";
					println("Left " + thumbnailPath);
					File thumbFile = new File(thumbnailPath);
					if (!thumbFile.exists()) {
						item.imgThumbLeft = context.thumbnail.generateThumbnail(context, itemImgLeft, false);
					} else {
						PImage thumbImg = loadImage(thumbnailPath);
						if (thumbImg == null) {
							println("ni pudimos cargar la imagen " + thumbnailPath);
						} else {

						}
						thumbImg = thumbImg.get(context.thumbnail.thumbMargin, 0,
								thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
						item.imgThumbLeft = thumbImg;
					}
				} else {
					item.imgThumbLeft = null;
					println("Left ERROR", itemImgLeft.getPath(), "image not found");
				}
			} else {
				item.imgThumbLeft = null;
			}
			if (!item.imagePathRight.equals("")) {
				File itemImgRight = new File(projectDirectory + "/" + item.imagePathRight);
				if (itemImgRight.exists()) {
					String fileName = itemImgRight.getName();
					String thumbnailPath = projectDirectoryFile.getPath() + "/thumbnails/"
							+ FilenameUtils.removeExtension(fileName) + "_thumb.jpg";
					println("Right " + thumbnailPath);
					File thumbFile = new File(thumbnailPath);
					if (!thumbFile.exists()) {
						item.imgThumbRight = context.thumbnail.generateThumbnail(context, itemImgRight, true);
					} else {
						PImage thumbImg = loadImage(thumbnailPath);
						thumbImg = thumbImg.get(0, 0, thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
						item.imgThumbRight = thumbImg;
					}
				} else {
					item.imgThumbRight = null;
					println("Right ERROR", itemImgRight.getPath(), "image not found");
				}
			} else {
				item.imgThumbRight = null;
			}
		}
		try {
			G2P5.setImageCount(new Integer(projectDataXML.getChild("image_counter").getContent()));
		} catch (Exception e) {
			println("ERROR loading image counter, seting to list size");
			G2P5.setImageCount(items.size());
		}
		thumbnailsLoaded = true;
		initSelectedItem = true;
		gphotoA.setTargetFile(projectDirectory + "/raw", projectCode);
		gphotoB.setTargetFile(projectDirectory + "/raw", projectCode);
		forceSelectedItem(items.size(), false);
		saveLastSessionData();
		removeUnusedImages();
	}

	public void loadProjectMetadata(XML projectDataXML) {

		projectName = projectDataXML.getChild("metadata").getChild("name").getContent();
		name_text.setText(projectName);
		projectComment = projectDataXML.getChild("metadata").getChild("comment").getContent();
		project_comments_text.setText(projectComment);
		projectCode = projectDataXML.getChild("metadata").getChild("code").getContent();
		code_text.setText(projectCode);
		projectAuthor = projectDataXML.getChild("metadata").getChild("author").getContent();
		author_text.setText(projectAuthor);
		println(projectDataXML.getChild("image_counter"));
		G2P5.setImageCount(new Integer(projectDataXML.getChild("image_counter").getContent()));

	}

	public boolean createDirectory(String folderName) {

		File file = new File(baseDirectory + folderName);
		if (!file.exists()) {
			if (file.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + baseDirectory + folderName);
				} catch (Exception e) {
					_println("Couldn't create directory permisions");
				}
			} else {
				_println("Failed to create directory!");
			}
			return true;
		} else {
			return false;
		}
	}

	public void saveProjectXML() {
		println(projectName, projectCode, projectAuthor, projectComment);
		if (!(projectName.equals("") && projectCode.equals("") && projectAuthor.equals("")
				&& projectComment.equals(""))) {
			XML projectXML = new XML("project");

			XML metadataXML = new XML("metadata");
			XML nameXML = new XML("name");
			nameXML.setContent(projectName);
			metadataXML.addChild(nameXML);
			XML codeXML = new XML("code");
			codeXML.setContent(projectCode);
			metadataXML.addChild(codeXML);
			XML projectCommentXML = new XML("comment");
			projectCommentXML.setContent(projectComment);
			metadataXML.addChild(projectCommentXML);
			XML authorXML = new XML("author");
			authorXML.setContent(projectAuthor);
			metadataXML.addChild(authorXML);
			projectXML.addChild(metadataXML);

			XML itemsXML = new XML("items");
			for (int i = 0; i < items.size(); i++) {
				Item item = items.get(i);
				if (!(item.imagePathLeft.equals("") && item.imagePathRight.equals(""))) {
					XML itemXML = new XML("item");
					XML imageLeftXML = new XML("image_left");
					if (item.imagePathLeft != null) {
						imageLeftXML.setContent(item.imagePathLeft);
					} else {
						imageLeftXML.setContent("");
					}
					itemXML.addChild(imageLeftXML);
					XML imageRightXML = new XML("image_right");
					if (item.imagePathRight != null) {
						imageRightXML.setContent(item.imagePathRight);
					} else {
						imageRightXML.setContent("");
					}
					imageRightXML.setContent(item.imagePathRight);
					itemXML.addChild(imageRightXML);
					XML pageNumXML = new XML("page_num");
					pageNumXML.setContent(String.valueOf(item.pagNum));
					itemXML.addChild(pageNumXML);
					XML pageCommentXML = new XML("comment");
					pageCommentXML.setContent(item.comment);
					itemXML.addChild(pageCommentXML);
					XML typeXML = new XML("type");
					typeXML.setContent(item.type);
					itemXML.addChild(typeXML);
					itemsXML.addChild(itemXML);
				}
			}
			projectXML.addChild(itemsXML);

			XML imageCounterXML = new XML("image_counter");
			imageCounterXML.setContent(String.valueOf(G2P5.getImageCount()));
			projectXML.addChild(imageCounterXML);

			File fileProject = new File(projectFilePath);
			if (fileProject.exists()) {
				String commandGenerate = "mv " + projectFilePath + " " + projectDirectory + "/project_backup.xml";
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

			saveXML(projectXML, projectFilePath);
			println("Saved project to ", projectFilePath);
		} else {
			println("ERROR: No project info, no data is saved. Please write the project name and code");
			G4P.showMessage(this, "ERROR: No project info, no data is saved.\nPlease write the project name and code. ",
					"Save project", G4P.ERROR);

		}
	}

	public void closeProject() {

		removeUnusedImages();
		saveProjectXML();
		items = new ArrayList<Item>();
		thumbnailsLoaded = false;
		projectFilePath = "";
		projectAuthor = "";
		projectCode = "";
		projectComment = "";
		projectDirectory = "";

		// TODO: Check everything to close in the project. Reset viewer

	}

	/*
	 * Items
	 */

	Item selectedItem = null;

	public int findItemIndexByPagNum(float pgNum) {
		int index = -1;
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).pagNum == pgNum) {
				index = i;
				break;
			}
		}
		return index;
	}

	public void selectItem(int index) {
		if (index >= 0 && index < items.size()) {
			selectedItem = items.get(index);
			OscMessage myMessage = new OscMessage("/load/item");
			String leftImagePath = "";
			String rightImagePath = "";
			if (selectedItem.imagePathRight != null && selectedItem.imagePathRight.length() != 0) {
				rightImagePath = projectDirectory + "/" + selectedItem.imagePathRight;
				myMessage.add(rightImagePath);
			} else {
				myMessage.add("");
			}
			if (selectedItem.imagePathLeft != null && (selectedItem.imagePathLeft.length() != 0)) {
				leftImagePath = projectDirectory + "/" + selectedItem.imagePathLeft;
				myMessage.add(leftImagePath);
			} else {
				myMessage.add("");
			}

			println("send the message to viewer");

			// View message for viewer
			oscP5.send(myMessage, viewerLocation);
			// Now we do the preview on app
			loadPreviews(leftImagePath, rightImagePath);

			page_comments_text.setText(selectedItem.comment);
			page_num_text.setText(String.valueOf(selectedItem.pagNum));
			gphotoA.setTargetFile(projectDirectory + "/raw", projectCode);
			gphotoB.setTargetFile(projectDirectory + "/raw", projectCode);

		}
	}

	int NO_SCROLL_TRANSITION = 0;
	int SCROLL_TRANSITION = 1;
	int scrollTransitionState = NO_SCROLL_TRANSITION;

	public void forceSelectedItem(int index, boolean transition) {
		selectedItemIndex = min(index, items.size() - 1);
		if (transition) {
			scrollTransitionState = SCROLL_TRANSITION;
		}
		if (selectedItemIndex >= 0 && items.size() > 0) {
			// Update gui list
			int itemHeight = itemThumbHeight + marginY;
			int fullListHeight = itemHeight * items.size();
			scrollHandleHeight = map(itemsViewPort.height, 0, fullListHeight, 0, itemsViewPort.height);
			if (scrollHandleHeight > itemsViewPort.height) {
				scrollHandleHeight = itemsViewPort.height;
			}
			if (scrollHandleHeight < 0)
				scrollHandleHeight = 0;
			try {
				if (items.size() == 1)
					scrollHandleY = 0;
				else
					scrollHandleY = map(index, 0, items.size() - 1, 0, itemsViewPort.height - scrollHandleHeight);
			} catch (Exception e) {
				scrollHandleY = 0;
			}
			// Update selected item
			if (scrollHandleY < 0) {
				scrollHandleY = 0;
			}
			if ((scrollHandleY + scrollHandleHeight) > itemsViewPort.height) {
				scrollHandleY = itemsViewPort.height - scrollHandleHeight;
			}

			selectItem(selectedItemIndex);
		}
	}

	public void clearItem(int index) {
		Item itemToClear = items.get(index);
		itemToClear.clear();
		selectedItemIndex = min(index, items.size());
		if (selectedItemIndex >= 0 && items.size() > 0) {
			selectItem(selectedItemIndex);
		}
		forceSelectedItem(selectedItemIndex, true);
		saveProjectXML();
		removeUnusedImages();

	}

	public void removeItem(int index) {
		Item itemToRemove = items.get(index);
		String type = itemToRemove.type;
		float pageNum = itemToRemove.pagNum;

		items.remove(index);

		if (itemToRemove.type.equals("Item")) {
			if (index < items.size()) {
				for (int i = index; i < items.size(); i++) {
					items.get(i).pagNum--;
				}
			}
		} else {
			if (index < items.size() - 1) {
				for (int i = index; i < items.size() - 1; i++) {
					if ((int) items.get(i).pagNum == (int) pageNum) {
						if (items.get(i).pagNum - (int) items.get(i).pagNum > pageNum - (int) pageNum) {
							float newPageNum = round((items.get(i).pagNum - 0.1f) * 10) / 10.0f;
							items.get(i).pagNum = newPageNum;
						}
					}

				}
			}
		}
		selectedItemIndex = min(index, items.size() - 1);
		if (selectedItemIndex >= 0 && items.size() > 0) {
			selectItem(selectedItemIndex);
		}
		forceSelectedItem(selectedItemIndex, true);
		saveProjectXML();
		removeUnusedImages();

	}

	public synchronized void addItem(int index, Item newItem) {

		if (index >= 0) {
			if (index < items.size()) {
				if (index > 0) {
					Item targetItem = items.get(index - 1);
					if (targetItem.imagePathLeft.equals("") && targetItem.imagePathRight.equals("")) {
						newItem.pagNum = targetItem.pagNum;
						items.set(index - 1, newItem);
						selectedItemIndex = min(index, items.size());
						if ((selectedItemIndex != items.size())
								|| (items.get(selectedItemIndex).pagNum != newItem.pagNum + 1)) {
							Item emptyItem = new Item(context, "", "", (int) (newItem.pagNum) + 1, "", "Item");
							items.add(selectedItemIndex, emptyItem);
							forceSelectedItem(index, true);
						} else if (selectedItemIndex != items.size()) {
							forceSelectedItem(selectedItemIndex, true);
						}
					} else {
						items.add(index, newItem);
						forceSelectedItem(index, true);
					}
				} else {
					items.add(index, newItem);
					forceSelectedItem(index, true);
				}
			} else {
				if (items.size() == 0) {
					items.add(newItem);
					forceSelectedItem(index, true);
				} else {
					Item targetItem = items.get(index - 1);
					if (targetItem.imagePathLeft.equals("") && targetItem.imagePathRight.equals("")) {
						newItem.pagNum = targetItem.pagNum;
						items.set(index - 1, newItem);
						selectedItemIndex = min(index, items.size());
						forceSelectedItem(selectedItemIndex, true);
					} else {
						items.add(newItem);
						forceSelectedItem(index, true);
					}

				}
			}

			for (int i = index + 1; i < items.size(); i++) {
				items.get(i).pagNum++;
			}

			saveProjectXML();
			println("item added");
		}
	}

	public synchronized void addSubItem(int index, Item newItem) {
		if (index >= 0 && index <= items.size()) {
			items.add(index, newItem);
			selectedItemIndex = min(index, items.size());
			if (selectedItemIndex >= 0 && items.size() > 0) {
				forceSelectedItem(selectedItemIndex, true);
			}
			saveProjectXML();
		}
	}

	public synchronized void replaceItem(int index, Item newItem) {

		if (index >= 0 && index < items.size()) {
			if (newItem.imagePathLeft.equals("")) {
				newItem.imagePathLeft = items.get(index).imagePathLeft;
				newItem.loadThumbnails();
			}
			if (newItem.imagePathRight.equals("")) {
				newItem.imagePathRight = items.get(index).imagePathRight;
				newItem.loadThumbnails();
			}
			items.remove(index);
			items.add(index, newItem);
			selectedItemIndex = min(index + 1, items.size());
			if (!newItem.type.equals("SubItem")) {
				if ((selectedItemIndex == items.size())
						|| (items.get(selectedItemIndex).pagNum != newItem.pagNum + 1)) {
					Item emptyItem = new Item(context, "", "", newItem.pagNum + 1, "", "Item");
					items.add(selectedItemIndex, emptyItem);
				}
			}
			forceSelectedItem(selectedItemIndex, true);
			saveProjectXML();
		}
	}

	/*
	 * Garbage Image Collector
	 */

	public synchronized String[] getUnusedImage() {
		ArrayList<String> usedImgs = new ArrayList<String>();
		for (int index = 0; index < items.size(); index++) {
			Item item = items.get(index);
			if (!item.imagePathLeft.equals(""))
				usedImgs.add(projectDirectory + "/" + item.imagePathLeft);
			if (!item.imagePathRight.equals(""))
				usedImgs.add(projectDirectory + "/" + item.imagePathRight);

		}

		File folder = new File(projectDirectory + "/raw");
		String[] files = folder.list();
		if (files != null) {
			ArrayList<String> unusedFiles = new ArrayList<String>();
			for (int index = 0; index < files.length; index++) {
				boolean found = false;
				for (int index2 = 0; index2 < usedImgs.size(); index2++) {
					File usedImg = new File(usedImgs.get(index2));
					// println(usedImg.getName(),files[index]);
					if (usedImg.getName().equals(files[index])) {
						found = true;
						break;
					}
				}
				if (!found) {
					String targetPath = folder.getPath() + "/" + files[index];
					unusedFiles.add(targetPath);
				}
			}
			unusedFiles.removeAll(usedImgs);
			String[] unusedArr = new String[unusedFiles.size()];
			for (int index = 0; index < unusedArr.length; index++) {
				unusedArr[index] = unusedFiles.get(index);
			}
			return unusedArr;
		} else {
			return null;
		}

	}

	public synchronized void removeUnusedImages() {
		// Garbage collection
		String[] unusedImgs = getUnusedImage();
		if (unusedImgs != null && unusedImgs.length > 0 && unusedImgs.length < items.size() * 2) {
			for (int index = 0; index < unusedImgs.length; index++) {
				String targetFilePath = unusedImgs[index];
				deleteFile(targetFilePath);
				String thumbnailPath = context.thumbnail.getThumbnailPath(context.projectDirectory,
						new File(targetFilePath));
				deleteFile(thumbnailPath);
			}
		}
	}

	private void deleteFile(String targetFilePath) {
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

	// Image preview

	String lastLeftImagePath = "";
	String lastRightImagePath = "";

	void loadPreviews(String leftImagePath, String rightImagePath) {

		long startMillis = millis();

		if (!leftImagePath.equals("")) {

			String previewFolder = sketchPath() + "/data/preview_left/";

			deleteFile(previewFolder + "*.jpg");

			File itemImgLeft = new File(leftImagePath);
			String fileName = FilenameUtils.removeExtension(itemImgLeft.getName());
			String resizedImage = "left_preview.jpg";
			String previewName = fileName + "-preview3.jpg";
			String previewFullPath = previewFolder + previewName;
			String resizedImageFullPath = previewFolder + resizedImage;
			String command = "exiv2 -ep3 -l " + previewFolder + " " + leftImagePath;
			lastLeftImagePath = previewFullPath;
			try {
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			command = "convert " + previewFullPath + " -resize 1000x667 " + resizedImageFullPath;
			try {
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				previewImgLeft = loadImage(resizedImageFullPath);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		if (!rightImagePath.equals("")) {

			String previewFolder = sketchPath() + "/data/preview_right/";

			// Clear preview folder
			deleteFile(previewFolder + "*.jpg");

			File itemImgRight = new File(rightImagePath);
			String fileName = FilenameUtils.removeExtension(itemImgRight.getName());
			String resizedImage = "right_preview.jpg";
			String previewName = fileName + "-preview3.jpg";
			String previewFullPath = previewFolder + previewName;
			String resizedImageFullPath = previewFolder + resizedImage;
			lastRightImagePath = previewFullPath;
			String command = "exiv2 -ep3 -l " + previewFolder + " " + rightImagePath;
			try {
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			command = "convert " + previewFullPath + " -resize 1000x667 " + resizedImageFullPath;
			try {
				Process process = Runtime.getRuntime().exec(command);
				InputStream error = process.getErrorStream();

				process.waitFor();
				String err = "Error:";
				for (int i = 0; i < error.available(); i++) {
					err += (char) error.read();
				}
				if (!err.equals("Error:")) {
					println(err);
				}

				previewImgRight = loadImage(resizedImageFullPath);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		long endMillis = millis();

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
			pr = Runtime.getRuntime().exec("md5sum " + projectDirectory + id + "_" + side + ".cr2");
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
			PrintWriter writer = new PrintWriter(projectDirectory + id + "_" + side + ".md5", "UTF-8");
			writer.println(md5);
			writer.close();
		} catch (Exception e) {
			_println("Problem saving md5");
		}

		return md5;
	}

	public String generateTimeStamp(String id, String side) {
		String timeStamp = "";
		File file = new File(projectDirectory + id + "_" + side + ".cr2");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		timeStamp = sdf.format(file.lastModified());
		return timeStamp;
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

	// Use this method to add additional statements
	// to customise the GUI controls
	public void customGUI() {

		Font font = new Font("Verdana", Font.BOLD, 10);
		name.setFont(font);
		code_label.setFont(font);
		author_label.setFont(font);
		project_comments_label.setFont(font);
		page_comments_label.setFont(font);
		number_label.setFont(font);
		camera_A_label.setFont(font);
		camera_B_label.setFont(font);
		page_search_label.setFont(font);

		Font sectionFont = new Font("Verdana", Font.BOLD, 12);
		project_info.setFont(sectionFont);
		page_info_label.setFont(sectionFont);
		shutter_control_label.setFont(sectionFont);
		camera_config_label.setFont(sectionFont);

		normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);

	}

	/*
	 * ========================================================= ==== WARNING
	 * === ========================================================= The code in
	 * this tab has been generated from the GUI form designer and care should be
	 * taken when editing this file. Only add/edit code inside the event
	 * handlers i.e. only use lines between the matching comment tags. e.g.
	 * 
	 * void myBtnEvents(GButton button) { //_CODE_:button1:12356: // It is safe
	 * to enter your event code here } //_CODE_:button1:12356:
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
		projectName = source.getText();
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			saveProjectXML();
		}
	} // _CODE_:name_text:702135:

	public void code_text_change(GTextField source, GEvent event) { // _CODE_:code_text:779005:
		projectCode = source.getText();
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			saveProjectXML();
		}
	} // _CODE_:code_text:779005:

	public void author_text_change(GTextField source, GEvent event) { // _CODE_:author_text:873016:
		projectAuthor = source.getText();
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			saveProjectXML();
		}
	} // _CODE_:author_text:873016:

	public void project_comments_change(GTextField source, GEvent event) { // _CODE_:project_comments_text:337734:
		projectComment = source.getText();
		println();
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			saveProjectXML();
		}

	} // _CODE_:project_comments_text:337734:

	public void page_comments_text_change(GTextField source, GEvent event) { // _CODE_:page_comments_text:397499:
		if (selectedItem != null) {
			selectedItem.comment = source.getText();
		}
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			saveProjectXML();
		}
	} // _CODE_:page_comments_text:397499:

	public void page_num_text_change(GTextField source, GEvent event) { // _CODE_:textfield1:363899:
		if (event.toString() == "ENTERED" && !projectDirectory.equals("")) {
			try {
				float pageNumber = Float.parseFloat(source.getText());
				int itemIndex = findItemIndexByPagNum(pageNumber);
				if (itemIndex != -1) {
					forceSelectedItem(itemIndex, true);
				}
				saveProjectXML();
			} catch (NumberFormatException ex) {
				println("wrong page number");
			}
		}

	} // _CODE_:textfield1:363899:

	public void normal_shutter_click1(GButton source, GEvent event) { // _CODE_:normal_shutter_button:563899:
		println("SHUTTER CONTROL SET NORMAL MODE!!!!!");
		shutterMode = NORMAL_SHUTTER;
		normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:normal_shutter_button:563899:

	public void repeat_shutter_click(GButton source, GEvent event) { // _CODE_:repeat_shutter_button:591981:
		println("SHUTTER CONTROL SET REPEAT MODE");
		shutterMode = REPEAT_SHUTTER;
		normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		repeat_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:repeat_shutter_button:591981:

	public void subpage_shutter_click(GButton source, GEvent event) { // _CODE_:subpage_shutter_button:295319:
		println("SHUTTER CONTROL SET SUBPAGE MODE");
		shutterMode = SUBPAGE_SHUTTER;
		normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		subpage_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:subpage_shutter_button:295319:

	public void calibration_shutter_click(GButton source, GEvent event) { // _CODE_:calibration_shutter_button:835827:
		println("SHUTTER CONTROL SET CALIBRATION MODE");
		shutterMode = CALIB_SHUTTER;
		normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		calibration_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
	} // _CODE_:calibration_shutter_button:835827:

	public void trigger_button_click(GButton source, GEvent event) { // _CODE_:trigger_button:381491:
		println("SHUTTER TRIGGERED");
		gphotoA.capture();
		gphotoB.capture();
		newImagePathA = "";
		newImagePathB = "";
	} // _CODE_:trigger_button:381491:

	public void camera_A_connected_click(GButton source, GEvent event) { // _CODE_:camera_A_connected_button:265149:
		println("button1 - GButton >> GEvent." + event + " @ " + millis());
		if (!gphotoA.isConnected()) {
			gphotoA = G2P5.create(this, serialCameraA, "A");
			gphotoA.setTargetFile(sketchPath(), "test");
		}
	} // _CODE_:camera_A_connected_button:265149:

	public void camera_A_active_button_click(GButton source, GEvent event) { // _CODE_:camera_A_active_button:906773:
		println("camera_A_active_button - GButton >> GEvent." + event + " @ " + millis());
		camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		cameraActiveA = true;
		gphotoA.setActive(true);
	} // _CODE_:camera_A_active_button:906773:

	public void camera_A_inactive_button_click(GButton source, GEvent event) { // _CODE_:camera_A_inactive_button:493860:
		println("inactive_camera_A_button - GButton >> GEvent." + event + " @ " + millis());
		camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		cameraActiveA = false;
		gphotoA.setActive(false);

	} // _CODE_:camera_A_inactive_button:493860:

	public void camera_B_connected_click(GButton source, GEvent event) { // _CODE_:camera_B_connected_button:564189:
		println("camera_B_connected_button - GButton >> GEvent." + event + " @ " + millis());
		if (!gphotoB.isConnected()) {
			gphotoB = G2P5.create(this, serialCameraB, "B");
			gphotoB.setTargetFile(sketchPath(), "test");
		}
	} // _CODE_:camera_B_connected_button:564189:

	public void camera_B_active_click(GButton source, GEvent event) { // _CODE_:camera_B_active_button:640605:
		println("camera_B_active_button - GButton >> GEvent." + event + " @ " + millis());
		camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		cameraActiveB = true;
		gphotoB.setActive(true);

	} // _CODE_:camera_B_active_button:640605:

	public void camera_B_inactive_click(GButton source, GEvent event) { // _CODE_:camera_B_inactive_button:780199:
		println("camera_B_inactive_button - GButton >> GEvent." + event + " @ " + millis());
		camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		cameraActiveB = false;
		gphotoB.setActive(false);

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

	public void new_button_click(GButton source, GEvent event) { // _CODE_:new_button:324180:
		String projectFolderPath = G4P.selectFolder("Select the project folder");
		if (projectFolderPath != null) {
			thumbnailsLoaded = false;
			createProject(projectFolderPath);
		}
	} // _CODE_:new_button:324180:

	public void page_search_text_change(GTextField source, GEvent event) { // _CODE_:page_search_text:741750:
		println("textfield2 - GTextField >> GEvent." + event + " @ " + millis());
	} // _CODE_:page_search_text:741750:

	public void liveView_button_click(GButton source, GEvent event) { // _CODE_:export_button:581416:
		println("export_button - GButton >> GEvent." + event + " @ " + millis());
	} // _CODE_:export_button:581416:

	// Create all the GUI controls.
	// autogenerated do not edit
	public void createGUI() {
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.YELLOW_SCHEME);
		G4P.setCursor(ARROW);
		surface.setTitle("ManuCapture v1");
		first_page_button = new GButton(this, 11, 90, 122, 24);
		first_page_button.setText("FIRST PAGE");
		first_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		first_page_button.addEventHandler(this, "first_page_button_click");
		last_page_button = new GButton(this, 141, 90, 122, 24);
		last_page_button.setText("LAST PAGE");
		last_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		last_page_button.addEventHandler(this, "last_page_button_click");
		name_text = new GTextField(this, 380, 106, 200, 20, G4P.SCROLLBARS_NONE);
		name_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		name_text.setOpaque(true);
		name_text.addEventHandler(this, "name_text_change");
		project_info = new GLabel(this, 300, 56, 132, 24);
		project_info.setText("PROJECT INFO");
		project_info.setTextBold();
		project_info.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_info.setOpaque(true);
		name = new GLabel(this, 302, 106, 80, 20);
		name.setText("Name:");
		name.setTextBold();
		name.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		name.setOpaque(true);
		code_label = new GLabel(this, 302, 130, 80, 20);
		code_label.setText("Code:");
		code_label.setTextBold();
		code_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_label.setOpaque(true);
		code_text = new GTextField(this, 380, 130, 200, 20, G4P.SCROLLBARS_NONE);
		code_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_text.setOpaque(true);
		code_text.addEventHandler(this, "code_text_change");
		author_label = new GLabel(this, 302, 154, 80, 20);
		author_label.setText("Author:");
		author_label.setTextBold();
		author_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		author_label.setOpaque(true);
		project_comments_label = new GLabel(this, 302, 178, 80, 20);
		project_comments_label.setText("Comments:");
		project_comments_label.setTextBold();
		project_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_comments_label.setOpaque(true);
		page_info_label = new GLabel(this, 300, 276, 80, 20);
		page_info_label.setText("PAGE INFO");
		page_info_label.setTextBold();
		page_info_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_info_label.setOpaque(true);
		page_comments_label = new GLabel(this, 302, 326, 80, 20);
		page_comments_label.setText("Comments:");
		page_comments_label.setTextBold();
		page_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_comments_label.setOpaque(true);
		number_label = new GLabel(this, 302, 410, 80, 20);
		number_label.setText("Number:");
		number_label.setTextBold();
		number_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		number_label.setOpaque(true);
		shutter_control_label = new GLabel(this, 300, 450, 200, 24);
		shutter_control_label.setText("SHUTTER CONTROL:");
		shutter_control_label.setTextBold();
		shutter_control_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		shutter_control_label.setOpaque(true);
		camera_config_label = new GLabel(this, 300, 684, 200, 20);
		camera_config_label.setText("CAMERA CONFIGURATION:");
		camera_config_label.setTextBold();
		camera_config_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_config_label.setOpaque(true);
		camera_A_label = new GLabel(this, 407, 719, 80, 20);
		camera_A_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		camera_A_label.setText("CAMERA A");
		camera_A_label.setTextBold();
		camera_A_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_label.setOpaque(true);
		author_text = new GTextField(this, 380, 154, 200, 20, G4P.SCROLLBARS_NONE);
		author_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		author_text.setOpaque(true);
		author_text.addEventHandler(this, "author_text_change");
		project_comments_text = new GTextField(this, 380, 178, 200, 80, G4P.SCROLLBARS_NONE);
		project_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_comments_text.setOpaque(true);
		project_comments_text.addEventHandler(this, "project_comments_change");
		camera_B_label = new GLabel(this, 407, 852, 80, 20);
		camera_B_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		camera_B_label.setText("CAMERA B");
		camera_B_label.setTextBold();
		camera_B_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_label.setOpaque(true);
		page_comments_text = new GTextField(this, 380, 326, 200, 80, G4P.SCROLLBARS_NONE);
		page_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_comments_text.setOpaque(true);
		page_comments_text.addEventHandler(this, "page_comments_text_change");
		page_num_text = new GTextField(this, 380, 410, 200, 20, G4P.SCROLLBARS_NONE);
		page_num_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_num_text.setOpaque(true);
		page_num_text.addEventHandler(this, "page_num_text_change");
		normal_shutter_button = new GButton(this, 386, 488, 122, 24);
		normal_shutter_button.setText("NORMAL");
		normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		normal_shutter_button.addEventHandler(this, "normal_shutter_click1");
		repeat_shutter_button = new GButton(this, 386, 517, 122, 24);
		repeat_shutter_button.setText("REPEAT");
		repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		repeat_shutter_button.addEventHandler(this, "repeat_shutter_click");
		subpage_shutter_button = new GButton(this, 386, 547, 122, 24);
		subpage_shutter_button.setText("SUBPAGE");
		subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		subpage_shutter_button.addEventHandler(this, "subpage_shutter_click");
		calibration_shutter_button = new GButton(this, 386, 577, 122, 24);
		calibration_shutter_button.setText("CALIBRATION");
		calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		calibration_shutter_button.addEventHandler(this, "calibration_shutter_click");
		trigger_button = new GButton(this, 386, 619, 122, 48);
		trigger_button.setText("TRIGGER");
		trigger_button.setLocalColorScheme(GCScheme.PURPLE_SCHEME);
		trigger_button.addEventHandler(this, "trigger_button_click");
		camera_A_connected_button = new GButton(this, 386, 746, 122, 24);
		camera_A_connected_button.setText("DISCONNECTED");
		camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		camera_A_connected_button.addEventHandler(this, "camera_A_connected_click");
		camera_A_active_button = new GButton(this, 385, 779, 122, 24);
		camera_A_active_button.setText("ACTIVE");
		camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_active_button.addEventHandler(this, "camera_A_active_button_click");
		camera_A_inactive_button = new GButton(this, 386, 809, 122, 24);
		camera_A_inactive_button.setText("INACTIVE");
		camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_inactive_button.addEventHandler(this, "camera_A_inactive_button_click");
		camera_B_connected_button = new GButton(this, 386, 879, 122, 24);
		camera_B_connected_button.setText("DISCONNECTED");
		camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		camera_B_connected_button.addEventHandler(this, "camera_B_connected_click");
		camera_B_active_button = new GButton(this, 386, 915, 122, 24);
		camera_B_active_button.setText("ACTIVE");
		camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_active_button.addEventHandler(this, "camera_B_active_click");
		camera_B_inactive_button = new GButton(this, 386, 945, 122, 24);
		camera_B_inactive_button.setText("INACTIVE");
		camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_inactive_button.addEventHandler(this, "camera_B_inactive_click");
		parameters_button = new GButton(this, 386, 994, 122, 24);
		parameters_button.setText("PARAMETERS");
		parameters_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		parameters_button.addEventHandler(this, "parameters_click");
		load_button = new GButton(this, 11, 11, 80, 24);
		load_button.setText("LOAD");
		load_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		load_button.addEventHandler(this, "load_click");
		new_button = new GButton(this, 99, 11, 122, 24);
		new_button.setText("NEW PROJECT");
		new_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		new_button.addEventHandler(this, "new_button_click");
		page_search_text = new GTextField(this, 90, 57, 175, 20, G4P.SCROLLBARS_NONE);
		page_search_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_search_text.setOpaque(true);
		page_search_text.addEventHandler(this, "page_search_text_change");
		page_search_label = new GLabel(this, 10, 57, 80, 20);
		page_search_label.setText("Page search:");
		page_search_label.setTextBold();
		page_search_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_search_label.setOpaque(true);
		liveView_button = new GButton(this, 490, 11, 80, 24);
		liveView_button.setText("LIVEVIEW");
		liveView_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		liveView_button.addEventHandler(this, "liveView_button_click");
	}

	// Variable declarations
	// autogenerated do not edit
	GButton first_page_button;
	GButton last_page_button;
	GTextField name_text;
	GLabel project_info;
	GLabel name;
	GLabel code_label;
	GTextField code_text;
	GLabel author_label;
	GLabel project_comments_label;
	GLabel page_info_label;
	GLabel page_comments_label;
	GLabel number_label;
	GLabel shutter_control_label;
	GLabel camera_config_label;
	GLabel camera_A_label;
	GTextField author_text;
	GTextField project_comments_text;
	GLabel camera_B_label;
	GTextField page_comments_text;
	GTextField page_num_text;
	GButton normal_shutter_button;
	GButton repeat_shutter_button;
	GButton subpage_shutter_button;
	GButton calibration_shutter_button;
	GButton trigger_button;
	GButton camera_A_connected_button;
	GButton camera_A_active_button;
	GButton camera_A_inactive_button;
	GButton camera_B_connected_button;
	GButton camera_B_active_button;
	GButton camera_B_inactive_button;
	GButton parameters_button;
	GButton load_button;
	GButton new_button;
	GTextField page_search_text;
	GLabel page_search_label;
	GButton liveView_button;

	public void settings() {
		// size(595, 1030);
		size(1920, 1030);
	}

	static public void main(String[] passedArgs) {

		String location = "--location=0,0";

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
