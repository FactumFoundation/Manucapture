
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

	Project project = null;
	
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

	boolean cameraActiveA = false;
	boolean cameraActiveB = false;

	String serialCameraA;
	String serialCameraB;


	String newImagePathA = "";
	String newImagePathB = "";

	ManuCaptureContext context = new ManuCaptureContext();
	
	int lastDrawedItems = -1;
	int ADDING_ITEM_TRANSITION = 1;
	int REMOVING_ITEM_TRANSITION = 2;
	int NO_TRANSITION = 0;
	int itemsViewTransition = NO_TRANSITION;
	float transitionPhase = 0.0f;
	float itemBaseY = 0;

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
		context.thumbnail = new RawFile();
		context.parent = this;
		context.gui = new GUI();
		
		
		
		project = new Project();
		project.context = context;
		
		context.project = project;
		
		if (surface != null) {
			surface.setLocation(0, 0);
		}
		XML serialXML = loadXML("cameraSerials.xml");
		serialCameraA = serialXML.getChild("Camera_A").getContent();
		serialCameraB = serialXML.getChild("Camera_B").getContent();

		G2P5.init(0);
		context.gphotoA = G2P5.create(this, serialCameraA, "A");
		context.gphotoA.setTargetFile(sketchPath(), "test");
		context.gphotoB = G2P5.create(this, serialCameraB, "B");
		context.gphotoB.setTargetFile(sketchPath(), "test");

		createGUI();
		customGUI();

		itemsViewPort = createGraphics(itemListViewPortWidth, itemListViewPortHeight);
		scrollHandleState = SCROLL_HANDLE_IDLE;
		textMode(MODEL);

		removeItemIcon = loadImage("cross_inv_20x20.jpeg");

		context.oscP5 = new OscP5(this, receivePort);
		context.viewerLocation = new NetAddress("127.0.0.1", sendPort);

		loadLastSessionData();
		bookIcon = loadImage("bookIcon.png");
		bookIcon.resize(bookIcon.width / 6, bookIcon.height / 6);

		
		
	}



	public void draw() {

		if (surface != null) {
			surface.setLocation(0, 0);
		}

		background(backgroundColor);

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


		drawItemsViewPort();
		
		image(itemsViewPort, itemListViewPortX, itemListViewPortY);
	drawLeft();

		drawRight();
		
		
		text(frameRate,10,10);

	}

	private void drawRight() {
		if (project.previewImgRight != null) {
			pushMatrix();
			translate(1250 + project.previewImgRight.height / 2, 20 + project.previewImgRight.width / 2);
			rotate(3 * PI / 2);
			imageMode(CENTER);
			image(project.previewImgRight, 0, 0);
			imageMode(CORNER);
			popMatrix();
		} else {
			stroke(255);
			fill(50);
			rect(1250, 20, 667, 1000);
		}
	}

	private void drawLeft() {
		if (project.previewImgLeft != null) {
			pushMatrix();
			translate(580 + project.previewImgLeft.height / 2, 20 + project.previewImgLeft.width / 2);
			rotate(PI / 2);
			imageMode(CENTER);
			image(project.previewImgLeft, 0, 0);
			imageMode(CORNER);
			popMatrix();
		} else {
			stroke(255);
			fill(50);
			rect(580, 20, 667, 1000);
		}
	}

	private void drawItemsViewPort() {
		
		
		// ITEM LIST VIEW PORT SECTION
		// /////////////////////////////////////////////////
		// Items view port
		int itemHeight = itemThumbHeight + marginY;

		// update item list related values
		float targetItemBaseY;
		int fullListHeight = itemHeight * project.items.size();

		if (project.items.size() == 0) {
			targetItemBaseY = 0.0f;
		} else {
			targetItemBaseY = map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
		}

		if (project.scrollTransitionState == project.NO_SCROLL_TRANSITION)
			itemBaseY = targetItemBaseY;
		else {
			if (abs(targetItemBaseY - itemBaseY) < 20 || project.items.size() <= 1) {
				project.scrollTransitionState = project.NO_SCROLL_TRANSITION;
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

		if (project.items.size() == 0 && !project.projectDirectory.equals("")) {
			itemsViewPort.stroke(selectItemColorStroke);
			itemsViewPort.fill(selectItemColor);
			int iconWidht = itemsViewPort.width - scrollBarWidth - 1;
			itemsViewPort.rect(0, -marginY / 2, itemsViewPort.width - scrollBarWidth - 1, itemHeight);
			itemsViewPort.image(bookIcon, (iconWidht - bookIcon.width) / 2, (itemHeight - bookIcon.height) / 2);
		} else {
			// items
			if (project.thumbnailsLoaded) {

				// TODO: Add transition when adding
				if (lastDrawedItems != project.items.size() && lastDrawedItems != -1
						&& (project.selectedItemIndex != project.items.size() - 1)) {
					if (lastDrawedItems < project.items.size()) {
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
					if (i < project.items.size() && viewPortRelativeHeight > -itemHeight
							&& viewPortRelativeHeight < itemsViewPort.height) {
						Item item = project.items.get(i);
						if (i == overedItemIndex && i != project.selectedItemIndex) {
							itemsViewPort.stroke(scrollBarColor);
							itemsViewPort.fill(overItemColor);
							itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
									itemsViewPort.width - scrollBarWidth - 1, itemHeight);
						} else if (i == project.selectedItemIndex) {
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

						if ((i != project.selectedItemIndex) || (itemsViewTransition != ADDING_ITEM_TRANSITION)) {
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
				lastDrawedItems = project.items.size();
			}

		}
		itemsViewPort.endDraw();
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
				int fullListHeight = itemHeight * project.items.size();
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
	
	public void forceSelectedItem(int index, boolean transition) {
		project.selectedItemIndex = min(index, project.items.size() - 1);
		if (transition) {
			project.scrollTransitionState = project.SCROLL_TRANSITION;
		}
		if (project.selectedItemIndex >= 0 && project.items.size() > 0) {
			// Update gui list
			int itemHeight = itemThumbHeight + marginY;
			int fullListHeight = itemHeight * project.items.size();
			scrollHandleHeight = map(itemsViewPort.height, 0, fullListHeight, 0, itemsViewPort.height);
			if (scrollHandleHeight > itemsViewPort.height) {
				scrollHandleHeight = itemsViewPort.height;
			}
			if (scrollHandleHeight < 0)
				scrollHandleHeight = 0;
			try {
				if (project.items.size() == 1)
					scrollHandleY = 0;
				else
					scrollHandleY = map(index, 0, project.items.size() - 1, 0, itemsViewPort.height - scrollHandleHeight);
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

			project.selectItem(project.selectedItemIndex);
		}
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
				int fullListHeight = itemHeight * project.items.size();
				float itemBaseY = map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += itemHeight) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					if (viewPortRelativeHeight > -itemHeight && viewPortRelativeHeight < itemsViewPort.height) {
						// check item overed
						if ((mouseY > (viewPortRelativeHeight + itemListViewPortY))
								&& (mouseY < (viewPortRelativeHeight + itemHeight + itemListViewPortY))) {
							project.selectedItemIndex = i;
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
							project.selectItem(project.selectedItemIndex);
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
		if (gphoto == context.gphotoA) {
			newImagePathA = absoluteFilePath;

		} else if (gphoto == context.gphotoB) {
			newImagePathB = absoluteFilePath;
		}

		if ((context.gphotoA.isConnected() && context.gphotoB.isConnected() && (!newImagePathA.equals("") && !newImagePathB.equals("")))
				|| (context.gphotoA.isConnected() && !context.gphotoB.isConnected() && !newImagePathA.equals(""))
				|| (!context.gphotoA.isConnected() && context.gphotoB.isConnected() && !newImagePathB.equals(""))) {
			// delay(3000);
			if (shutterMode == NORMAL_SHUTTER) {

				float newPageNum;
				if (project.selectedItemIndex < 0) {
					newPageNum = 1;
				} else {
					newPageNum = (int) project.items.get(project.selectedItemIndex).pagNum + 1;
				}
				// TODO add new item: PRoblem when only one image arrives

				String relNewImagePathA = "";

				if (project.projectDirectory.equals("")) {
					newImagePathA = "";
					newImagePathB = "";
				}

				if (!newImagePathA.equals(""))
					relNewImagePathA = newImagePathA.substring(project.projectDirectory.length() + 1, newImagePathA.length());
				String relNewImagePathB = "";
				if (!newImagePathB.equals(""))
					relNewImagePathB = newImagePathB.substring(project.projectDirectory.length() + 1, newImagePathB.length());

				Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "", "Item");
				newItem.loadThumbnails(project, newImagePathA, newImagePathB);

				addItem(project.selectedItemIndex + 1, newItem);
				newImagePathA = "";
				newImagePathB = "";

			} else if (shutterMode == SUBPAGE_SHUTTER) {

				if (project.items.size() > 0) {
					float currentPN = project.selectedItem.pagNum;
					float newPageNum = round((currentPN + 0.1f) * 10) / 10.0f;
					// TODO add new item

					if (project.projectDirectory.equals("")) {
						newImagePathA = "";
						newImagePathB = "";
					}

					String relNewImagePathA = "";
					if (newImagePathA != null && (newImagePathA.length() > project.projectDirectory.length() + 1))
						relNewImagePathA = newImagePathA.substring(project.projectDirectory.length() + 1,
								newImagePathA.length());
					String relNewImagePathB = "";
					if (newImagePathB != null && (newImagePathB.length() > project.projectDirectory.length() + 1))
						relNewImagePathB = newImagePathB.substring(project.projectDirectory.length() + 1,
								newImagePathB.length());

					Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "", "SubItem");
					newItem.loadThumbnails(project, newImagePathA, newImagePathB);
					addSubItem(project.selectedItemIndex + 1, newItem);
					newImagePathA = "";
					newImagePathB = "";
				}

			} else if (shutterMode == REPEAT_SHUTTER) {
				if (project.items.size() > 0) {
					float newPageNum = project.selectedItem.pagNum;

					if (project.projectDirectory.equals("")) {
						newImagePathA = "";
						newImagePathB = "";
					}

					String relNewImagePathA = "";
					if (!newImagePathA.equals(""))
						relNewImagePathA = newImagePathA.substring(project.projectDirectory.length() + 1,
								newImagePathA.length());
					String relNewImagePathB = "";
					if (!newImagePathB.equals(""))
						relNewImagePathB = newImagePathB.substring(project.projectDirectory.length() + 1,
								newImagePathB.length());

					Item newItem = new Item(context, relNewImagePathA, relNewImagePathB, newPageNum, "",
							project.selectedItem.type);
					newItem.loadThumbnails(project);
					replaceItem(project.selectedItemIndex, newItem);
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

				project.selectedItemIndex = new Integer(lastSessionData.getChild("Current_Item").getContent());

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

				forceSelectedItem(project.selectedItemIndex, false);

			}

		} catch (Exception e) {
			_println("lastSession.xml not found");
			// txtLog.insertText("Error reconstructing last session: check the
			// integrity of your session folder ");
			e.printStackTrace();
		}
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

	public void clearItem(int index) {
		Item itemToClear = project.items.get(index);
		itemToClear.clear();
		project.selectedItemIndex = min(index, project.items.size());
		if (project.selectedItemIndex >= 0 && project.items.size() > 0) {
			project.selectItem(project.selectedItemIndex);
		}
		forceSelectedItem(project.selectedItemIndex, true);
		project.saveProjectXML();
		project.removeUnusedImages();

	}

	public void removeItem(int index) {
		Item itemToRemove = project.items.get(index);
		String type = itemToRemove.type;
		float pageNum = itemToRemove.pagNum;

		ArrayList<Item> items = project.items;
		
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
		project.selectedItemIndex = min(index, items.size() - 1);
		if (project.selectedItemIndex >= 0 && items.size() > 0) {
			project.selectItem(project.selectedItemIndex);
		}
		forceSelectedItem(project.selectedItemIndex, true);
		project.saveProjectXML();
		project.removeUnusedImages();

	}

	public synchronized void addItem(int index, Item newItem) {
		ArrayList<Item> items = project.items;
		if (index >= 0) {
			if (index < items.size()) {
				if (index > 0) {
					Item targetItem = items.get(index - 1);
					if (targetItem.imagePathLeft.equals("") && targetItem.imagePathRight.equals("")) {
						newItem.pagNum = targetItem.pagNum;
						items.set(index - 1, newItem);
						project.selectedItemIndex = min(index, items.size());
						if ((project.selectedItemIndex != items.size())
								|| (items.get(project.selectedItemIndex).pagNum != newItem.pagNum + 1)) {
							Item emptyItem = new Item(context, "", "", (int) (newItem.pagNum) + 1, "", "Item");
							items.add(project.selectedItemIndex, emptyItem);
							forceSelectedItem(index, true);
						} else if (project.selectedItemIndex != items.size()) {
							forceSelectedItem(project.selectedItemIndex, true);
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
						project.selectedItemIndex = min(index, items.size());
						forceSelectedItem(project.selectedItemIndex, true);
					} else {
						items.add(newItem);
						forceSelectedItem(index, true);
					}

				}
			}

			for (int i = index + 1; i < items.size(); i++) {
				items.get(i).pagNum++;
			}

			
			project.saveProjectXML();
			println("item added");
		}
	}

	public synchronized void addSubItem(int index, Item newItem) {
		ArrayList<Item> items = project.items;
		if (index >= 0 && index <= items.size()) {
			items.add(index, newItem);
			project.selectedItemIndex = min(index, items.size());
			if (project.selectedItemIndex >= 0 && items.size() > 0) {
				forceSelectedItem(project.selectedItemIndex, true);
			}
			project.saveProjectXML();
		}
	}

	public synchronized void replaceItem(int index, Item newItem) {
		ArrayList<Item> items = project.items;
		if (index >= 0 && index < items.size()) {
			if (newItem.imagePathLeft.equals("")) {
				newItem.imagePathLeft = items.get(index).imagePathLeft;
				newItem.loadThumbnails(project);
			}
			if (newItem.imagePathRight.equals("")) {
				newItem.imagePathRight = items.get(index).imagePathRight;
				newItem.loadThumbnails(project);
			}
			items.remove(index);
			items.add(index, newItem);
			project.selectedItemIndex = min(index + 1, items.size());
			if (!newItem.type.equals("SubItem")) {
				if ((project.selectedItemIndex == items.size())
						|| (items.get(project.selectedItemIndex).pagNum != newItem.pagNum + 1)) {
					Item emptyItem = new Item(context, "", "", newItem.pagNum + 1, "", "Item");
					items.add(project.selectedItemIndex, emptyItem);
				}
			}
			forceSelectedItem(project.selectedItemIndex, true);
			project.saveProjectXML();
		}
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
		context.gphotoA.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		context.gphotoB.setTargetFile(project.projectDirectory + "/raw", project.projectCode);

		saveLastSessionData();

	}

	


	public synchronized void loadProject(String projectPath) {
		project.loadProjectMethod(projectPath);
		initSelectedItem = true;
		context.gphotoA.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		context.gphotoB.setTargetFile(project.projectDirectory + "/raw", project.projectCode);
		forceSelectedItem(project.items.size(), false);
		saveLastSessionData();
		project.removeUnusedImages();
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
		GUI gui = context.gui;
		gui.name.setFont(font);
		gui.code_label.setFont(font);
		gui.author_label.setFont(font);
		gui.project_comments_label.setFont(font);
		gui.page_comments_label.setFont(font);
		gui.number_label.setFont(font);
		gui.camera_A_label.setFont(font);
		gui.camera_B_label.setFont(font);
		gui.page_search_label.setFont(font);

		Font sectionFont = new Font("Verdana", Font.BOLD, 12);
		gui.project_info.setFont(sectionFont);
		gui.page_info_label.setFont(sectionFont);
		gui.shutter_control_label.setFont(sectionFont);
		gui.camera_config_label.setFont(sectionFont);

		gui.normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);

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
					forceSelectedItem(itemIndex, true);
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
		gui.subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:normal_shutter_button:563899:

	public void repeat_shutter_click(GButton source, GEvent event) { // _CODE_:repeat_shutter_button:591981:
		println("SHUTTER CONTROL SET REPEAT MODE");
		shutterMode = REPEAT_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:repeat_shutter_button:591981:

	public void subpage_shutter_click(GButton source, GEvent event) { // _CODE_:subpage_shutter_button:295319:
		println("SHUTTER CONTROL SET SUBPAGE MODE");
		shutterMode = SUBPAGE_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.subpage_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:subpage_shutter_button:295319:

	public void calibration_shutter_click(GButton source, GEvent event) { // _CODE_:calibration_shutter_button:835827:
		println("SHUTTER CONTROL SET CALIBRATION MODE");
		shutterMode = CALIB_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
	} // _CODE_:calibration_shutter_button:835827:

	public void trigger_button_click(GButton source, GEvent event) { // _CODE_:trigger_button:381491:
		println("SHUTTER TRIGGERED");
		context.gphotoA.capture();
		context.gphotoB.capture();
		newImagePathA = "";
		newImagePathB = "";
	} // _CODE_:trigger_button:381491:

	public void camera_A_connected_click(GButton source, GEvent event) { // _CODE_:camera_A_connected_button:265149:
		println("button1 - GButton >> GEvent." + event + " @ " + millis());
		if (!context.gphotoA.isConnected()) {
			context.gphotoA = G2P5.create(this, serialCameraA, "A");
			context.gphotoA.setTargetFile(sketchPath(), "test");
		}
	} // _CODE_:camera_A_connected_button:265149:

	public void camera_A_active_button_click(GButton source, GEvent event) { // _CODE_:camera_A_active_button:906773:
		println("camera_A_active_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		cameraActiveA = true;
		context.gphotoA.setActive(true);
	} // _CODE_:camera_A_active_button:906773:

	public void camera_A_inactive_button_click(GButton source, GEvent event) { // _CODE_:camera_A_inactive_button:493860:
		println("inactive_camera_A_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		cameraActiveA = false;
		context.gphotoA.setActive(false);

	} // _CODE_:camera_A_inactive_button:493860:

	public void camera_B_connected_click(GButton source, GEvent event) { // _CODE_:camera_B_connected_button:564189:
		println("camera_B_connected_button - GButton >> GEvent." + event + " @ " + millis());
		if (!context.gphotoB.isConnected()) {
			context.gphotoB = G2P5.create(this, serialCameraB, "B");
			context.gphotoB.setTargetFile(sketchPath(), "test");
		}
	} // _CODE_:camera_B_connected_button:564189:

	public void camera_B_active_click(GButton source, GEvent event) { // _CODE_:camera_B_active_button:640605:
		println("camera_B_active_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		cameraActiveB = true;
		context.gphotoB.setActive(true);

	} // _CODE_:camera_B_active_button:640605:

	public void camera_B_inactive_click(GButton source, GEvent event) { // _CODE_:camera_B_inactive_button:780199:
		println("camera_B_inactive_button - GButton >> GEvent." + event + " @ " + millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		cameraActiveB = false;
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

	public void new_button_click(GButton source, GEvent event) { // _CODE_:new_button:324180:
		String projectFolderPath = G4P.selectFolder("Select the project folder");
		if (projectFolderPath != null) {
			project.thumbnailsLoaded = false;
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
		GUI gui = context.gui;
		gui.first_page_button = new GButton(this, 11, 90, 122, 24);
		gui.first_page_button.setText("FIRST PAGE");
		gui.first_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.first_page_button.addEventHandler(this, "first_page_button_click");
		gui.last_page_button = new GButton(this, 141, 90, 122, 24);
		gui.last_page_button.setText("LAST PAGE");
		gui.last_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.last_page_button.addEventHandler(this, "last_page_button_click");
		gui.name_text = new GTextField(this, 380, 106, 200, 20, G4P.SCROLLBARS_NONE);
		gui.name_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.name_text.setOpaque(true);
		gui.name_text.addEventHandler(this, "name_text_change");
		gui.project_info = new GLabel(this, 300, 56, 132, 24);
		gui.project_info.setText("PROJECT INFO");
		gui.project_info.setTextBold();
		gui.project_info.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.project_info.setOpaque(true);
		gui.name = new GLabel(this, 302, 106, 80, 20);
		gui.name.setText("Name:");
		gui.name.setTextBold();
		gui.name.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.name.setOpaque(true);
		gui.code_label = new GLabel(this, 302, 130, 80, 20);
		gui.code_label.setText("Code:");
		gui.code_label.setTextBold();
		gui.code_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.code_label.setOpaque(true);
		gui.code_text = new GTextField(this, 380, 130, 200, 20, G4P.SCROLLBARS_NONE);
		gui.code_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.code_text.setOpaque(true);
		gui.code_text.addEventHandler(this, "code_text_change");
		gui.author_label = new GLabel(this, 302, 154, 80, 20);
		gui.author_label.setText("Author:");
		gui.author_label.setTextBold();
		gui.author_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.author_label.setOpaque(true);
		gui.project_comments_label = new GLabel(this, 302, 178, 80, 20);
		gui.project_comments_label.setText("Comments:");
		gui.project_comments_label.setTextBold();
		gui.project_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.project_comments_label.setOpaque(true);
		gui.page_info_label = new GLabel(this, 300, 276, 80, 20);
		gui.page_info_label.setText("PAGE INFO");
		gui.page_info_label.setTextBold();
		gui.page_info_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_info_label.setOpaque(true);
		gui.page_comments_label = new GLabel(this, 302, 326, 80, 20);
		gui.page_comments_label.setText("Comments:");
		gui.page_comments_label.setTextBold();
		gui.page_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_comments_label.setOpaque(true);
		gui.number_label = new GLabel(this, 302, 410, 80, 20);
		gui.number_label.setText("Number:");
		gui.number_label.setTextBold();
		gui.number_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.number_label.setOpaque(true);
		gui.shutter_control_label = new GLabel(this, 300, 450, 200, 24);
		gui.shutter_control_label.setText("SHUTTER CONTROL:");
		gui.shutter_control_label.setTextBold();
		gui.shutter_control_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.shutter_control_label.setOpaque(true);
		gui.camera_config_label = new GLabel(this, 300, 684, 200, 20);
		gui.camera_config_label.setText("CAMERA CONFIGURATION:");
		gui.camera_config_label.setTextBold();
		gui.camera_config_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_config_label.setOpaque(true);
		gui.camera_A_label = new GLabel(this, 407, 719, 80, 20);
		gui.camera_A_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		gui.camera_A_label.setText("CAMERA A");
		gui.camera_A_label.setTextBold();
		gui.camera_A_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_label.setOpaque(true);
		gui.author_text = new GTextField(this, 380, 154, 200, 20, G4P.SCROLLBARS_NONE);
		gui.author_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.author_text.setOpaque(true);
		gui.author_text.addEventHandler(this, "author_text_change");
		gui.project_comments_text = new GTextField(this, 380, 178, 200, 80, G4P.SCROLLBARS_NONE);
		gui.project_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.project_comments_text.setOpaque(true);
		gui.project_comments_text.addEventHandler(this, "project_comments_change");
		gui.camera_B_label = new GLabel(this, 407, 852, 80, 20);
		gui.camera_B_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		gui.camera_B_label.setText("CAMERA B");
		gui.camera_B_label.setTextBold();
		gui.camera_B_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_label.setOpaque(true);
		gui.page_comments_text = new GTextField(this, 380, 326, 200, 80, G4P.SCROLLBARS_NONE);
		gui.page_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_comments_text.setOpaque(true);
		gui.page_comments_text.addEventHandler(this, "page_comments_text_change");
		gui.page_num_text = new GTextField(this, 380, 410, 200, 20, G4P.SCROLLBARS_NONE);
		gui.page_num_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_num_text.setOpaque(true);
		gui.page_num_text.addEventHandler(this, "page_num_text_change");
		gui.normal_shutter_button = new GButton(this, 386, 488, 122, 24);
		gui.normal_shutter_button.setText("NORMAL");
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.normal_shutter_button.addEventHandler(this, "normal_shutter_click1");
		gui.repeat_shutter_button = new GButton(this, 386, 517, 122, 24);
		gui.repeat_shutter_button.setText("REPEAT");
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.addEventHandler(this, "repeat_shutter_click");
		gui.subpage_shutter_button = new GButton(this, 386, 547, 122, 24);
		gui.subpage_shutter_button.setText("SUBPAGE");
		gui.subpage_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.subpage_shutter_button.addEventHandler(this, "subpage_shutter_click");
		gui.calibration_shutter_button = new GButton(this, 386, 577, 122, 24);
		gui.calibration_shutter_button.setText("CALIBRATION");
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.addEventHandler(this, "calibration_shutter_click");
		gui.trigger_button = new GButton(this, 386, 619, 122, 48);
		gui.trigger_button.setText("TRIGGER");
		gui.trigger_button.setLocalColorScheme(GCScheme.PURPLE_SCHEME);
		gui.trigger_button.addEventHandler(this, "trigger_button_click");
		gui.camera_A_connected_button = new GButton(this, 386, 746, 122, 24);
		gui.camera_A_connected_button.setText("DISCONNECTED");
		gui.camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		gui.camera_A_connected_button.addEventHandler(this, "camera_A_connected_click");
		gui.camera_A_active_button = new GButton(this, 385, 779, 122, 24);
		gui.camera_A_active_button.setText("ACTIVE");
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_active_button.addEventHandler(this, "camera_A_active_button_click");
		gui.camera_A_inactive_button = new GButton(this, 386, 809, 122, 24);
		gui.camera_A_inactive_button.setText("INACTIVE");
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_inactive_button.addEventHandler(this, "camera_A_inactive_button_click");
		gui.camera_B_connected_button = new GButton(this, 386, 879, 122, 24);
		gui.camera_B_connected_button.setText("DISCONNECTED");
		gui.camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		gui.camera_B_connected_button.addEventHandler(this, "camera_B_connected_click");
		gui.camera_B_active_button = new GButton(this, 386, 915, 122, 24);
		gui.camera_B_active_button.setText("ACTIVE");
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_active_button.addEventHandler(this, "camera_B_active_click");
		gui.camera_B_inactive_button = new GButton(this, 386, 945, 122, 24);
		gui.camera_B_inactive_button.setText("INACTIVE");
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_inactive_button.addEventHandler(this, "camera_B_inactive_click");
		gui.parameters_button = new GButton(this, 386, 994, 122, 24);
		gui.parameters_button.setText("PARAMETERS");
		gui.parameters_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.parameters_button.addEventHandler(this, "parameters_click");
		gui.load_button = new GButton(this, 11, 11, 80, 24);
		gui.load_button.setText("LOAD");
		gui.load_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.load_button.addEventHandler(this, "load_click");
		gui.new_button = new GButton(this, 99, 11, 122, 24);
		gui.new_button.setText("NEW PROJECT");
		gui.new_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.new_button.addEventHandler(this, "new_button_click");
		gui.page_search_text = new GTextField(this, 90, 57, 175, 20, G4P.SCROLLBARS_NONE);
		gui.page_search_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_search_text.setOpaque(true);
		gui.page_search_text.addEventHandler(this, "page_search_text_change");
		gui.page_search_label = new GLabel(this, 10, 57, 80, 20);
		gui.page_search_label.setText("Page search:");
		gui.page_search_label.setTextBold();
		gui.page_search_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.page_search_label.setOpaque(true);
		gui.liveView_button = new GButton(this, 490, 11, 80, 24);
		gui.liveView_button.setText("LIVEVIEW");
		gui.liveView_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.liveView_button.addEventHandler(this, "liveView_button_click");
	}

	

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
