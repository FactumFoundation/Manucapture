import java.util.ArrayList;
import java.util.List;

import processing.core.PImage;
import processing.core.PVector;
import processing.event.MouseEvent;

public class ContentGUI {
	
	ManuCapture_v1_1 context;
	boolean renderRight = true;
	boolean renderLeft = true;
	PVector lastPressedR = null;
	PVector lastPressedL = null;
	List<HotArea> pointsLeft = new ArrayList<>();
	List<HotArea> pointsRight = new ArrayList<>();
	int marginTopViewer = 100;
	int marginLeftViewerRight = 1170;
	int marginLeftViewerLeft = 400;
	// size view on screen
	float ratioAspect = 1f;
	int wImageViewerSize = (int)(1000*ratioAspect);
	int hImageViewerSize = (int)(667*ratioAspect);
	// width size of the preview
	public int viewerWidthResolution = 2000;
	PImage imgPreviewLeft;
	PImage imgPreviewRight;
	HotArea hotAreaSelected = null;
	PImage lastLeftPreview = null;
	PImage lastRightPreview = null;
	PImage cameraIcon;
	PImage zoomImg = null;
	float scaleA = 1.0f;
	float scaleB = 1.0f;
	
	public ContentGUI(ManuCapture_v1_1 context){
		this.context = context;
		// Preview area
		initCropHotAreas();
		cameraIcon = context.loadImage("cameraIcon.png");
		zoomImg = context.loadImage("zoom.png");
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


	public void draw() {
		
		if (renderRight) {
			drawRight();
			renderRight = true;
		}
		if (renderLeft) {
			drawLeft();
			renderLeft = true;
		}
		if (hotAreaSelected != null) {
			context.stroke(255, 0, 0);
			hotAreaSelected.draw(context.g);
		}
		if (lastPressedR == null)
			for (int i = 1; i <= pointsLeft.size(); i++) {
				PVector areaPos1 = pointsLeft.get(i - 1).getRealPosition();
				PVector areaPos2 = pointsLeft.get(i % pointsLeft.size()).getRealPosition();
				context.stroke(255, 0, 0);
				context.line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}
		if (lastPressedL == null)
			for (int i = 1; i <= pointsRight.size(); i++) {
				PVector areaPos1 = pointsRight.get(i - 1).getRealPosition();
				PVector areaPos2 = pointsRight.get(i % pointsRight.size()).getRealPosition();
				context.stroke(255, 0, 0);
				context.line(areaPos1.x, areaPos1.y, areaPos2.x, areaPos2.y);
			}
		if (context.cameraState == context.STATE_CHART) {
			context.pushMatrix();
			context.pushStyle();
			if (context.chartStateMachine == 0) {
				// textAlign(CENTER);
				// fill(255, 0, 0, 100);
				// rect(marginLeftViewerLeft, 0, hImageViewerSize * 2,
				// wImageViewerSize);
				// fill(255);
				// textSize(24);
				// text("CALIBRATING, PLEASE CAPTURE \n THIS BACKGROUND
				// \nWITHOUT ANY DOCUMENT", marginLeftViewerRight,
				// 200);
			} else if (context.chartStateMachine == 1 || context.chartStateMachine == 2) {
				context.textAlign(context.CENTER);
				String msg;
				if (context.chartStateMachine == 2) {
					context.translate(marginLeftViewerLeft, 0);
					msg = context.msg("sw.calibration3");
				} else {
					msg = context.msg("sw.calibration1");
					context.translate(marginLeftViewerRight, 0);
				}
				context.fill(255, 0, 0, 100);
				context.rect(0, marginTopViewer, hImageViewerSize, wImageViewerSize);
				context.fill(255);
				context.textSize(24);
				context.text(msg, hImageViewerSize / 2, 200);
			} else {
				context.translate(marginLeftViewerLeft, 0);
				context.fill(255, 0, 0, 100);
				context.rect(0, marginTopViewer, hImageViewerSize * 2 + 100, wImageViewerSize);
				context.textSize(24);
				context.fill(255, 255, 0);
				String cad = context.msg("sw.calibration2");
				context.text(cad, hImageViewerSize / 2 - 1, 200 - 1);
				context.fill(255);
				context.text(cad, hImageViewerSize / 2, 200);
			}
			context.popStyle();
			context.popMatrix();
		}
	}
	
	
	private void drawLeft() {

		if (context.project.selectedItem != null && imgPreviewLeft != null) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(marginLeftViewerRight, marginTopViewer);
			drawImagePreview(imgPreviewLeft, lastPressedL, marginLeftViewerRight, pointsRight,
					scaleA);
			if (lastPressedL != null) {
				context.tint(255, 200);
			} else {
				context.tint(255, 20);
			}
			context.image(zoomImg, hImageViewerSize - 70, 20, 50, 50);
			// pintamos en blending la imagen de calibración para puntos de crop
			if (context.chartStateMachine == 3 && lastLeftPreview != null) {
				context.pushStyle();
				context.tint(255, 125);
				context.image(lastLeftPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0,
						lastLeftPreview.width, lastLeftPreview.height);
				context.popStyle();
			}

			showPhotoMetaData();
			context.text(context.project.selectedItem.mImageLeft.imagePath, 0, -10);
			context.popMatrix();
			context.stroke(255, 0, 0);
			if (context.chartStateMachine == 3) {
				context.stroke(context.map(context.sin(100 + context.millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}
			if (lastPressedL == null) {
				for (HotArea area : pointsRight) {
					if (context.chartStateMachine == 3 || context.cropMode)
						area.draw(context.g);
				}
			}
			context.popStyle();
		} else {
			context.stroke(255);
			context.fill(50);
			context.rect(marginLeftViewerRight, marginTopViewer, hImageViewerSize, wImageViewerSize);
		}

		// datos de cámara
		if (!context.gphotoB.isConnected()) {
			context.fill(255, 0, 0);
		} else {
			context.fill(0, 255, 0);
		}
		context.ellipse(marginLeftViewerRight + 225, 78, 30, 30);

		context.fill(255);
		context.text("mirroUp " + context.gphotoBAdapter.g2p5.id + " " + context.gphotoBAdapter.mirrorUp,
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
		if (context.captureState == context.CAMERAS_FOCUSSING
				|| context.captureState == context.CAMERAS_MIRROR_UP
				|| context.captureState == context.CAMERAS_PROCESSING) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(marginLeftViewerRight, marginTopViewer);
			context.pushStyle();
			context.noStroke();
			int alpha = 150;
			if (context.captureState == context.CAMERAS_FOCUSSING) {
				context.fill(40, alpha);
			} else if (context.captureState == context.CAMERAS_MIRROR_UP) {
				context.fill(120, alpha);
			} else if (context.captureState == context.CAMERAS_PROCESSING) {
				context.fill(0, 50, 0, alpha);
			}
			context.rect(0, 0, hImageViewerSize, wImageViewerSize);
			context.imageMode(context.CENTER);
			context.image(cameraIcon, hImageViewerSize / 2, wImageViewerSize / 2, 256, 256);
			context.imageMode(context.CORNER);
			context.popMatrix();
			context.popStyle();
		}

	}

	private void drawRight() {

		if (context.project.selectedItem != null && imgPreviewRight != null) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(marginLeftViewerLeft, marginTopViewer);
			context.imageMode(context.CORNER);
			drawImagePreview(imgPreviewRight, lastPressedR, marginLeftViewerLeft, pointsLeft,
					scaleB);
			if (lastPressedR != null) {
				context.tint(255, 200);
			} else {
				context.tint(255, 20);
			}
			context.image(zoomImg, hImageViewerSize - 70, 20, 50, 50);
			if (context.chartStateMachine == 3 && lastRightPreview != null) {
				context.pushStyle();
				context.tint(255, 125);
				context.image(lastRightPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0,
						lastRightPreview.width, lastRightPreview.height);
				context.popStyle();
			}
			showPhotoMetaData();
			context.text(context.project.selectedItem.mImageRight.imagePath, 0, -10);
			context.popMatrix();
			context.stroke(255, 0, 0);
			if (context.chartStateMachine == 3) {
				context.stroke(context.map(context.sin(context.millis() * 0.01f), -1, 1, 0, 255), 0, 0);
			}
			if (lastPressedR == null)
				for (HotArea area : pointsLeft) {
					if (context.chartStateMachine == 3 || context.cropMode)
						area.draw(context.g);
				}
			context.popStyle();
		} else {
			context.stroke(255);
			context.fill(50);
			context.rect(marginLeftViewerLeft, marginTopViewer, hImageViewerSize, wImageViewerSize);
		}
		// datos de cámara
		if (!context.gphotoA.isConnected()) {
			context.fill(255, 0, 0);
		} else {
			context.fill(0, 255, 0);
		}
		context.ellipse(marginLeftViewerLeft + 227, 78, 30, 30);

		context.fill(255);
		// pushMatrix();
		// translate(0, 1015);
		//
		// text(" focusing: ", 890, 40);
		context.fill(255);
		context.text("mirroUp " + context.gphotoAAdapter.g2p5.id + " " + context.gphotoAAdapter.mirrorUp,
				marginLeftViewerLeft + 430, 40);
		// text(gphotoAAdapter.g2p5.id, 840, 40);
		// if (gphotoAAdapter.focus) {
		// fill(255, 0, 0);
		// } else {
		// fill(0, 255, 0);
		// }
		// ellipse(960, 35, 15, 15);
		// popMatrix();

		if (context.captureState == context.CAMERAS_FOCUSSING
				|| context.captureState == context.CAMERAS_MIRROR_UP
				|| context.captureState == context.CAMERAS_PROCESSING) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(marginLeftViewerLeft, marginTopViewer);

			context.noStroke();
			int alpha = 150;
			if (context.captureState == context.CAMERAS_FOCUSSING) {
				context.fill(40, alpha);
			} else if (context.captureState == context.CAMERAS_MIRROR_UP) {
				context.fill(120, alpha);
			} else if (context.captureState == context.CAMERAS_PROCESSING) {
				context.fill(0, 50, 0, alpha);
			}
			context.rect(0, 0, hImageViewerSize, wImageViewerSize);
			context.imageMode(context.CENTER);
			context.image(cameraIcon, hImageViewerSize / 2, wImageViewerSize / 2, 256, 256);
			context.imageMode(context.CORNER);

			context.popMatrix();
			context.popStyle();
		}
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
			context.image(imgPreview, 0, 0, hImageViewerSize, wImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {

			context.image(imgPreview, 0, 0, hImageViewerSize, wImageViewerSize, 0, 0, imgPreview.width,
					imgPreview.height);
		}
	}
	
	private void showPhotoMetaData() {
		context.fill(255);
		context.textSize(24);
		context.textAlign(context.LEFT);
		// @TODO add info from the readed image
		context.text("ISO: " + context.gphotoAAdapter.exposure, 10, wImageViewerSize - 20);
		context.text("exposure: " + context.gphotoAAdapter.exposure, 200, wImageViewerSize - 20);
		context.text("f: " + context.gphotoAAdapter.exposure, 450, wImageViewerSize - 20);
	}

	public void updateLastPreviews() {
		lastRightPreview = imgPreviewRight;
		lastLeftPreview = imgPreviewLeft;
	}
	
	public void mouseMoved() {
		
		if (hotAreaSelected != null) {
			if (hotAreaSelected.name.startsWith("L")) {
				if (isMouseInsideRight())
					hotAreaSelected.setRealPosition(context.mouseX, context.mouseY);
			}
			if (hotAreaSelected.name.startsWith("R")) {
				if (isMouseInsideLeft())
					hotAreaSelected.setRealPosition(context.mouseX, context.mouseY);
			}
			if (context.project != null && context.project.selectedItem != null) {
				context.project.selectedItem.mImageLeft.mesh = copyMesh(pointsLeft);
				context.project.selectedItem.mImageRight.mesh = copyMesh(pointsRight);
			}
		}

		if (lastPressedR != null) {
			updateZoomRight();
		}

		if (lastPressedL != null) {
			updateZoomLeft();
		}
		
	}
	
	public void mousePressed() {
		if (hotAreaSelected == null) {
			if (context.chartStateMachine == 3 || context.cropMode) {
				for (HotArea area : pointsLeft) {
					if (area.isInArea(context.mouseX, context.mouseY)) {
						hotAreaSelected = area;
						break;
					}
				}
				for (HotArea area : pointsRight) {
					if (area.isInArea(context.mouseX, context.mouseY)) {
						hotAreaSelected = area;
						break;
					}
				}
			}

			if (hotAreaSelected == null) {
				if ( context.project != null && context.project.selectedItem != null) {
					if (context.mouseButton == context.LEFT) {
						if (context.getStateApp()==context.STATE_APP_PROJECT && lastPressedL == null && context.project.selectedItem.mImageLeft != null
								&& imgPreviewLeft != null) {
							updateZoomLeft();
						}
						if (context.getStateApp()==context.STATE_APP_PROJECT && lastPressedR == null && context.project.selectedItem.mImageRight != null
								&& imgPreviewRight != null)
							updateZoomRight();
					}
					if (context.mouseButton == context.RIGHT) {
						lastPressedR = null;
						lastPressedL = null;
					}
				}
			}
		} else {
			hotAreaSelected = null;
			context.project.selectedItem.saveMetadata();
		}
	}
	
	public void mouseDragged() {
		if (hotAreaSelected != null && context.project.selectedItem != null) {
			hotAreaSelected.setRealPosition(context.mouseX, context.mouseY);
			context.project.selectedItem.mImageLeft.mesh = copyMesh(pointsLeft);
			context.project.selectedItem.mImageRight.mesh = copyMesh(pointsRight);
		}
	}
	
	public void mouseWheel(float count) {

		if (context.mouseX > marginLeftViewerRight && context.mouseX < marginLeftViewerRight + hImageViewerSize) {
			if (context.mouseY > 20 && context.mouseY < 20 + wImageViewerSize) {
				scaleA -= count / 10;
				scaleA = context.max(scaleA, 1);
				scaleA = context.min(scaleA, 4);
			}
		}
		if (context.mouseX > 580 && context.mouseX < 580 + hImageViewerSize) {
			if (context.mouseY > 20 && context.mouseY < 20 + wImageViewerSize) {
				scaleB -= count / 10;
				scaleB = context.max(scaleB, 1);
				scaleB = context.min(scaleB, 4);
			}
		}
	}
	
	private boolean isMouseInsideLeft() {
		if (context.mouseY > marginTopViewer && context.mouseY < context.height - marginTopViewer) {
			// Estamos en y
			if (context.mouseX > marginLeftViewerRight && context.mouseX < marginLeftViewerRight + hImageViewerSize) {
				return true;
			}
		}
		return false;
	}

	private boolean isMouseInsideRight() {
		if (context.mouseY > marginTopViewer && context.mouseY < context.height - marginTopViewer) {
			// Estamos en y
			if (context.mouseX > marginLeftViewerLeft && context.mouseX < marginLeftViewerLeft + hImageViewerSize) {

				if (context.mouseX > marginLeftViewerRight) {

				} else {
					return true;
				}
			}
		}
		return false;
	}

	private void updateZoomLeft() {
		if (isMouseInsideLeft() && context.chartStateMachine != 3)
			lastPressedL = new PVector(context.mouseX, context.mouseY);
	}

	private void updateZoomRight() {
		if (isMouseInsideRight() && context.chartStateMachine != 3)
			lastPressedR = new PVector(context.mouseX, context.mouseY);
	}

	public List<HotArea> copyMesh(List<HotArea> mesh) {
		List<HotArea> temp = new ArrayList<>();
		for (int i = 0; i < mesh.size(); i++) {
			HotArea ha = mesh.get(i);
			temp.add(new HotArea(ha.pos.copy(), ha.translatePos.copy(), ha.id, ha.threshold, ha.name));
		}
		return temp;
	}
	
}
