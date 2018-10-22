import java.util.ArrayList;
import java.util.List;

import boofcv.alg.sfm.d3.VisOdomDualTrackPnP.LeftTrackInfo;
import processing.core.PImage;
import processing.core.PVector;

public class ContentGUI {

	ManuCapture_v1_1 context;
	boolean renderRightImage = true;
	boolean renderLeftImage = true;
	PVector rightZoomOffset = null;
	PVector leftZoomOffset = null;
	List<Guide> guidesLeft = new ArrayList<>();
	List<Guide> guidesRight = new ArrayList<>();
	int imageMarginTop = 40;
	int rightImageMarginLeft = 1070;
	int leftImageMarginLeft = 350;
	// size view on screen
	float ratioAspect = 1f;
	int hImageViewerSize = (int) (1000 * ratioAspect);
	int wImageViewerSize = (int) (667 * ratioAspect);
	// width size of the preview
	public int viewerWidthResolution = 2000;
	PImage imgPreviewLeft;
	PImage imgPreviewRight;
	Guide selectedGuide = null;
	Guide overedGuide = null;
	PImage lastLeftPreview = null;
	PImage lastRightPreview = null;
	PImage cameraIcon;
	PImage zoomImg = null;
	float rightImageScale = 1.0f;
	float leftImageScale = 1.0f;
	float baseZoom = 2f;

	public ContentGUI(ManuCapture_v1_1 context) {
		this.context = context;
		initCropGuides();
		cameraIcon = context.loadImage("cameraIcon.png");
		zoomImg = context.loadImage("zoom.png");
	}

	public void initCropGuides() {
		guidesLeft = new ArrayList<>();
		guidesRight = new ArrayList<>();
		int size = 50;
		PVector translatePos1 = new PVector(leftImageMarginLeft, imageMarginTop);
		PVector translatePos2 = new PVector(rightImageMarginLeft, imageMarginTop);
		guidesLeft.add(new Guide(new PVector(size, 0), translatePos1, size, "LL"));
		guidesLeft.add(new Guide(new PVector(0, size), translatePos1, size, "LT"));
		guidesLeft.add(new Guide(new PVector(wImageViewerSize - size, 0), translatePos1, size, "LR"));
		guidesLeft.add(new Guide(new PVector(0, hImageViewerSize - size), translatePos1, size, "LB"));
		guidesRight.add(new Guide(new PVector(size, 0), translatePos2, size, "RL"));
		guidesRight.add(new Guide(new PVector(0, size), translatePos2, size, "RT"));
		guidesRight.add(new Guide(new PVector(wImageViewerSize - size, 0), translatePos2, size, "RR"));
		guidesRight.add(new Guide(new PVector(0, hImageViewerSize - size), translatePos2, size, "RB"));
	}

	public void draw() {
		if (renderRightImage) {
			drawRightImage();
		}
		if (renderLeftImage) {
			drawLeftImage();
		}
		if(context.cropMode)
			context.pageCropper.draw(leftImageMarginLeft, imageMarginTop, rightImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize);
		drawCropGuides();
		drawCalibrationInfo();
		drawCameraStatusOverlay();
	}

	private void drawLeftImage() {
		
		if(context.liveViewState == ManuCapture_v1_1.ON_LIVEVIEW) {
			if(context.liveViewLeft!=null) {
				context.pushStyle();
				context.pushMatrix();
				context.translate(leftImageMarginLeft, imageMarginTop);
				drawLiveView(context.liveViewLeft,leftImageMarginLeft,context.rotationPageLeft);
				context.fill(255);
				context.textSize(18);
				context.textAlign(context.LEFT);
				context.text(context.msg("sw.liveviewlefttitle"), 100, -10);
				context.popMatrix();
				context.popStyle();
			}
			else {
				//context.println("Error: Liveview image is null");
			}
		}
		else if(context.liveViewState == ManuCapture_v1_1.NO_LIVEVIEW) {
			if (context.project.selectedItem != null && imgPreviewLeft != null) {
				if (imgPreviewLeft.width > 0) {
					context.pushStyle();
					context.pushMatrix();
					context.translate(leftImageMarginLeft, imageMarginTop);
					drawImagePreview(imgPreviewLeft, leftZoomOffset, leftImageMarginLeft, leftImageScale);
					if (leftZoomOffset != null) {
						context.tint(255, 200);
					} else {
						context.tint(255, 20);
					}
					context.image(zoomImg, wImageViewerSize - 70, 20, 50, 50);
					context.fill(255);
					context.textSize(18);
					context.textAlign(context.LEFT);
					context.text(context.project.selectedItem.mImageLeft.imagePath, 100, -10);
					context.popMatrix();
					context.popStyle();
				}
			} else {
				context.stroke(255);
				context.fill(50);
				context.rect(leftImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize);
				context.fill(255);
				context.textAlign(context.LEFT);
				if (context.project.selectedItem != null && context.project.selectedItem.mImageLeft != null
						&& context.project.selectedItem.mImageLeft.imagePath != null)
					context.text(context.project.selectedItem.mImageLeft.imagePath, leftImageMarginLeft + 100,
							imageMarginTop - 10);
			}
			
		}
			
	}

	private void drawRightImage() {
		if(context.liveViewState == ManuCapture_v1_1.ON_LIVEVIEW) {
			if(context.liveViewRight!=null) {
				context.pushStyle();
				context.pushMatrix();
				context.translate(rightImageMarginLeft, imageMarginTop);
				drawLiveView(context.liveViewRight,rightImageMarginLeft,context.rotationPageRight);
				context.fill(255);
				context.textSize(18);
				context.textAlign(context.LEFT);
				context.text(context.msg("sw.liveviewrighttitle"), 100, -10);
				context.
				popMatrix();
				context.popStyle();
			}
			else {
				//context.println("Error: Liveview image is null");
			}
		}
		else if(context.liveViewState == ManuCapture_v1_1.NO_LIVEVIEW) { 
			
			if (context.project.selectedItem != null && imgPreviewRight != null) {
				context.pushStyle();
				context.pushMatrix();
				context.translate(rightImageMarginLeft, imageMarginTop);
				context.imageMode(context.CORNER);
				drawImagePreview(imgPreviewRight, rightZoomOffset, rightImageMarginLeft, rightImageScale);
				if (rightZoomOffset != null) {
					context.tint(255, 200);
				} else {
					context.tint(255, 20);
				}
				context.image(zoomImg, wImageViewerSize - 70, 20, 50, 50);
				context.fill(255);
				context.textSize(18);
				context.textAlign(context.LEFT);
				context.text(context.project.selectedItem.mImageRight.imagePath, 100, -10);
				context.popMatrix();
				context.popStyle();
			} else {
				context.stroke(255);
				context.fill(50);
				context.rect(rightImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize);
				context.fill(255);
				context.textAlign(context.LEFT);
				if (context.project.selectedItem != null && context.project.selectedItem.mImageRight != null
						&& context.project.selectedItem.mImageRight.imagePath != null)
					context.text(context.project.selectedItem.mImageRight.imagePath, rightImageMarginLeft + 100,
							imageMarginTop - 10);
			}
		}
	}
	
	private void drawLiveView(PImage imgLiveview, int marginLeft, float angle) {
		context.pushMatrix();
		context.imageMode(context.CENTER);
		context.translate(wImageViewerSize/2,hImageViewerSize/2);
		context.rotate(context.radians(angle));
		context.image(imgLiveview, 0, 0,hImageViewerSize,wImageViewerSize);
		context.popMatrix();
		
	}

	private void drawImagePreview(PImage imgPreview, PVector lastPressed, int marginLeft, float scale) {
		if (lastPressed != null) {
			// pimero quiero saber pos en la imagen
			float imgScale = imgPreview.width / (float) wImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressed, new PVector(marginLeft, imageMarginTop));
			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);
			int portviewSizeX = (int) ((wImageViewerSize) / scale);
			int portviewSizeY = (int) ((hImageViewerSize) / scale);
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
			context.image(imgPreview, 0, 0, wImageViewerSize, hImageViewerSize, portviewStartX, portviewStartY,
					portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {
			context.image(imgPreview, 0, 0, wImageViewerSize, hImageViewerSize, 0, 0, imgPreview.width,
					imgPreview.height);
		}
	}

	int guideColorIdle = 0xff0F99CE;
	int guideColorOver = 0xffD1F0FC;
	int guideColorPressed = 0xffFA8728;
	
	public void drawCropGuides() {
		
		// TODO: Draw guides
		if (rightZoomOffset == null) {
			for (Guide guide : guidesRight) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(guideColorPressed);
				} else {
					context.stroke(guideColorIdle);
				}
				PVector screenPos = guide.getScreenPosition();
				if (guide.isHorizontal()) {
					context.line(screenPos.x, screenPos.y, screenPos.x + wImageViewerSize, screenPos.y);
				} else {
					context.line(screenPos.x, screenPos.y, screenPos.x, screenPos.y + hImageViewerSize);
				}
				context.popStyle();
			}
		} else {
			float imgScale = imgPreviewRight.width / (float) wImageViewerSize;
			PVector virtualPos = PVector.sub(rightZoomOffset, new PVector(rightImageMarginLeft, imageMarginTop));
			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);
			float portviewSizeX = wImageViewerSize / rightImageScale;
			float portviewSizeY = hImageViewerSize / rightImageScale;
			float portviewStartX = virtualPosScaled.x - portviewSizeX / 2;
			float portviewStartY = virtualPosScaled.y - portviewSizeY / 2;
			if (portviewStartX + portviewSizeX > imgPreviewRight.width) {
				portviewStartX = imgPreviewRight.width - portviewSizeX;
			}
			if (portviewStartY + portviewSizeY > imgPreviewRight.height) {
				portviewStartY = imgPreviewRight.height - portviewSizeY;
			}
			if (portviewStartX < 0) {
				portviewStartX = 0;
			}
			if (portviewStartY < 0) {
				portviewStartY = 0;
			}
			for (Guide guide : guidesRight) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(guideColorPressed);
				} else {
					context.stroke(guideColorIdle);
				}
				if (guide.isHorizontal()) {
					float screenY = context.map(baseZoom * guide.pos.y, portviewStartY, portviewStartY + portviewSizeY,
							(float) imageMarginTop, (float) imageMarginTop + hImageViewerSize * rightImageScale);
					if (screenY >= imageMarginTop && screenY <= imageMarginTop + hImageViewerSize)
						context.line(rightImageMarginLeft, screenY, rightImageMarginLeft + wImageViewerSize, screenY);
				} else {
					float screenX = context.map(baseZoom * guide.pos.x, portviewStartX, portviewStartX + portviewSizeX,
							(float) rightImageMarginLeft,
							(float) rightImageMarginLeft + wImageViewerSize * rightImageScale);
					if (screenX >= rightImageMarginLeft && screenX <= rightImageMarginLeft + wImageViewerSize)
						context.line(screenX, imageMarginTop, screenX, imageMarginTop + hImageViewerSize);
				}
				context.popStyle();
			}
		}
		if (leftZoomOffset == null) {
			for (Guide guide : guidesLeft) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(guideColorPressed);
				} else {
					context.stroke(guideColorIdle);
				}
				PVector screenPos = guide.getScreenPosition();
				if (guide.isHorizontal()) {
					context.line(screenPos.x, screenPos.y, screenPos.x + wImageViewerSize, screenPos.y);
				} else {
					context.line(screenPos.x, screenPos.y, screenPos.x, screenPos.y + hImageViewerSize);
				}
				context.popStyle();
			}
		} else {
			float imgScale = imgPreviewLeft.width / (float) wImageViewerSize;
			PVector virtualPos = PVector.sub(leftZoomOffset, new PVector(leftImageMarginLeft, imageMarginTop));
			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);
			int portviewSizeX = (int) ((wImageViewerSize) / leftImageScale);
			int portviewSizeY = (int) ((hImageViewerSize) / leftImageScale);
			int portviewStartX = (int) (virtualPosScaled.x - portviewSizeX / 2);
			int portviewStartY = (int) (virtualPosScaled.y - portviewSizeY / 2);
			if (portviewStartX + portviewSizeX > imgPreviewLeft.width) {
				portviewStartX = imgPreviewLeft.width - portviewSizeX;
			}
			if (portviewStartY + portviewSizeY > imgPreviewLeft.height) {
				portviewStartY = imgPreviewLeft.height - portviewSizeY;
			}
			if (portviewStartX < 0) {
				portviewStartX = 0;
			}
			if (portviewStartY < 0) {
				portviewStartY = 0;
			}
			for (Guide guide : guidesLeft) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(guideColorPressed);
				} else {
					context.stroke(guideColorIdle);
				}
				if (guide.isHorizontal()) {
					float screenY = context.map(baseZoom * guide.pos.y, portviewStartY, portviewStartY + portviewSizeY,
							(float) imageMarginTop, (float) imageMarginTop + hImageViewerSize * leftImageScale);
					if (screenY >= imageMarginTop && screenY <= imageMarginTop + hImageViewerSize)
						context.line(leftImageMarginLeft, screenY, leftImageMarginLeft + wImageViewerSize, screenY);
				} else {
					float screenX = context.map(baseZoom * guide.pos.x, portviewStartX, portviewStartX + portviewSizeX,
							(float) leftImageMarginLeft,
							(float) leftImageMarginLeft + wImageViewerSize * leftImageScale);
					if (screenX >= leftImageMarginLeft && screenX <= leftImageMarginLeft + wImageViewerSize)
						context.line(screenX, imageMarginTop, screenX, imageMarginTop + hImageViewerSize);
				}
				context.popStyle();
			}
		}
		if (selectedGuide != null) {
			context.pushStyle();
			context.strokeWeight(3);
			context.stroke(guideColorPressed);
			PVector screenPos = selectedGuide.getScreenPosition();
			if (selectedGuide.isHorizontal()) {
				context.line(screenPos.x, screenPos.y, screenPos.x + wImageViewerSize, screenPos.y);
			} else {
				context.line(screenPos.x, screenPos.y, screenPos.x, screenPos.y + hImageViewerSize);
			}
			context.popStyle();
		}
		if (overedGuide != null) {
			context.pushStyle();
			context.strokeWeight(3);
			context.stroke(guideColorOver);
			PVector screenPos = overedGuide.getScreenPosition();
			if (overedGuide.isHorizontal()) {
				context.line(screenPos.x, screenPos.y, screenPos.x + wImageViewerSize, screenPos.y);
			} else {
				context.line(screenPos.x, screenPos.y, screenPos.x, screenPos.y + hImageViewerSize);
			}
			context.popStyle();
		}
	}

	public void drawCalibrationInfo() {
		if (context.cameraState == context.STATE_CHART) {

			if (context.chartStateMachine == 0) {
				// textAlign(CENTER);
				// fill(255, 0, 0, 100);
				// rect(marginLeftViewerLeft, 0, hImageViewerSize * 2,
				// wImageViewerSize);
				// fill(255);
				// textSize(24);
				// text("CALIBRATING, PLEASE CAPTURE \n THIS BACKGROUND
				// \nWITHOUT ANY DOCUMENT", mrginLeftViewerRight,
				// 200);
			} else if (context.chartStateMachine == 1 || context.chartStateMachine == 2) {
				context.pushMatrix();
				context.pushStyle();
				context.textAlign(context.CENTER);
				String msg;
				if (context.chartStateMachine == 2) {
					context.translate(leftImageMarginLeft, imageMarginTop);
					msg = context.msg("sw.calibration3");
				} else {
					msg = context.msg("sw.calibration1");
					context.translate(rightImageMarginLeft, imageMarginTop);
				}
				context.fill(255, 0, 0, 100);
				context.rect(0, 0, wImageViewerSize, hImageViewerSize);
				context.fill(255);
				context.textSize(24);
				context.text(msg, wImageViewerSize / 2, 150);
				context.popMatrix();
				context.popStyle();
			} else if (context.chartStateMachine == 3) {
				context.pushStyle();
				if (lastLeftPreview != null) {
					context.tint(255, 125);
					context.image(lastLeftPreview, leftImageMarginLeft, imageMarginTop, wImageViewerSize,
							hImageViewerSize, 0, 0, lastLeftPreview.width, lastLeftPreview.height);
				}
				if (lastRightPreview != null) {
					context.tint(255, 125);
					context.image(lastRightPreview, rightImageMarginLeft, imageMarginTop, wImageViewerSize,
							hImageViewerSize, 0, 0, lastRightPreview.width, lastRightPreview.height);
				}
				context.pushMatrix();
				context.translate(leftImageMarginLeft, 0);
				context.fill(255, 0, 0, 100);
				context.rect(0, imageMarginTop, wImageViewerSize * 2 + 100, hImageViewerSize);
				context.textSize(24);
				context.textAlign(context.CENTER);
				context.fill(255);
				String cad = context.msg("sw.calibration2");
				context.text(cad, wImageViewerSize / 2 + leftImageMarginLeft, 150);
				context.popMatrix();
				context.popStyle();
			}
			// context.popStyle();
			// context.popMatrix();
		}
	}

	public void drawCameraStatusOverlay() {
		if (context.getCaptureState() == context.CAMERAS_FOCUSSING
				|| context.getCaptureState() == context.CAMERAS_MIRROR_UP
				|| context.getCaptureState() == context.CAMERAS_PROCESSING) {
			context.pushStyle();
			context.noStroke();
			int alpha = 150;
			if (context.getCaptureState() == context.CAMERAS_FOCUSSING) {
				context.fill(40, alpha);
			} else if (context.getCaptureState() == context.CAMERAS_MIRROR_UP) {
				context.fill(120, alpha);
			} else if (context.getCaptureState() == context.CAMERAS_PROCESSING) {
				context.fill(0, 50, 0, alpha);
			}
			context.pushMatrix();
			context.translate(leftImageMarginLeft, imageMarginTop);
			context.rect(0, 0, wImageViewerSize, hImageViewerSize);
			context.imageMode(context.CENTER);
			context.image(cameraIcon, wImageViewerSize / 2, hImageViewerSize / 2, 256, 256);
			context.imageMode(context.CORNER);
			context.popMatrix();
			context.pushMatrix();
			context.translate(rightImageMarginLeft, imageMarginTop);
			context.rect(0, 0, wImageViewerSize, hImageViewerSize);
			context.imageMode(context.CENTER);
			context.image(cameraIcon, wImageViewerSize / 2, hImageViewerSize / 2, 256, 256);
			context.imageMode(context.CORNER);
			context.popMatrix();
			context.popStyle();
		}
	}

	public void updateLastPreviews() {
		lastRightPreview = imgPreviewRight;
		lastLeftPreview = imgPreviewLeft;
	}

	public void startCropMode() {
	/*
		context.gui.btnOpenSOViewerLeft.setEnabled(false);
		context.gui.btnOpenSOViewerLeft.setVisible(false);
		context.gui.btnOpenSOViewerRight.setEnabled(false);
		context.gui.btnOpenSOViewerRight.setVisible(false);
		noZoom();
	*/
	}

	public void stopCropMode() {
		selectedGuide = null;
		/*
		context.gui.btnOpenSOViewerLeft.setEnabled(true);
		context.gui.btnOpenSOViewerLeft.setVisible(true);
		context.gui.btnOpenSOViewerRight.setEnabled(true);
		context.gui.btnOpenSOViewerRight.setVisible(true);
		*/
	}

	public void mouseMoved() {
	
		overedGuide = null;
	
		if(context.gui.btnOpenSOViewerLeft.isOver(context.mouseX, context.mouseY))
			return;
		
		if(context.gui.btnOpenSOViewerRight.isOver(context.mouseX, context.mouseY))
			return;
		
		if (isMouseInsideLeftImage()) {
			if(leftZoomOffset==null) {
				for (Guide guide : guidesLeft) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						overedGuide = guide;
						break;
					}
				}
			}
		} else if (isMouseInsideRightImage()) {
			if(rightZoomOffset==null) {
				for (Guide guide : guidesRight) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						overedGuide = guide;
						break;
					}
				}	
			}
		}
		
	}

	public void mousePressed() {
		overedGuide = null;
		
		if(context.gui.btnOpenSOViewerLeft.isOver(context.mouseX, context.mouseY))
			return;
		
		if(context.gui.btnOpenSOViewerRight.isOver(context.mouseX, context.mouseY))
			return;
		
		if (isMouseInsideLeftImage()) {
			if(leftZoomOffset==null) {
				for (Guide guide : guidesLeft) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						selectedGuide = guide;
						break;
					}
				}
			}
		} else if (isMouseInsideRightImage()) {
			if(rightZoomOffset==null) {
				for (Guide guide : guidesRight) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						selectedGuide = guide;
						break;
					}
				}
			}
		}
		if (context.chartStateMachine == 0 && selectedGuide==null) {
			if (context.project != null && context.project.selectedItem != null) {
				if (context.mouseButton == context.LEFT) {
					if (context.getStateApp() == context.STATE_APP_PROJECT && leftZoomOffset == null
							&& context.project.selectedItem.mImageLeft != null && imgPreviewLeft != null) {
						initZoomLeft();
					}
					if (context.getStateApp() == context.STATE_APP_PROJECT && rightZoomOffset == null
							&& context.project.selectedItem.mImageRight != null && imgPreviewRight != null)
						initZoomRight();
				}
				if (context.mouseButton == context.RIGHT) {
					exitZoomLeft();
					exitZoomRight();
				}
			}
		}
	}

	public void mouseDragged() {
		if (selectedGuide != null) {
			if (selectedGuide.name.startsWith("L")) {
				if (isMouseInsideLeftImage())
					selectedGuide.setImagePosition(context.mouseX, context.mouseY);
			}
			if (selectedGuide.name.startsWith("R")) {
				if (isMouseInsideRightImage())
					selectedGuide.setImagePosition(context.mouseX, context.mouseY);
			}
			if (context.project != null && context.project.selectedItem != null) {
				context.project.selectedItem.mImageLeft.guides = copyGuides(guidesLeft);
				context.project.selectedItem.mImageRight.guides = copyGuides(guidesRight);
			}
		} else if (rightZoomOffset != null) {
			updateZoomRight();
		} else if (leftZoomOffset != null) {
			updateZoomLeft();
		}
	}

	public void mouseReleased() {

		if(selectedGuide!=null) {
			context.project.selectedItem.mImageLeft.saveMetadata();
			context.project.selectedItem.mImageRight.saveMetadata();
		}
		
		selectedGuide = null;
		if (context.chartStateMachine == 3) {
			context.shutterMode = context.NORMAL_SHUTTER;
			context.setCaptureState(context.CAMERAS_IDLE);
			context.chartStateMachine = 0;
			//context.insertCalibItemPrevious = false;
			context.gui.btnRepeat.setState(0);
			context.gui.btnColorChart.setState(0);
			context.gui.btnRepeat.setEnabled(true);
			context.gui.btnColorChart.setEnabled(true);
			context.gui.btnCrop.setEnabled(true);
			context.gui.btnTrigger.setEnabled(true);
			context.gui.btnOpenSOViewerLeft.setVisible(true);
			context.gui.btnOpenSOViewerLeft.setEnabled(true);
			context.gui.btnOpenSOViewerRight.setVisible(true);
			context.gui.btnOpenSOViewerRight.setEnabled(true);
			noZoom();
		}
	}

	public void mouseWheel(float count) {
		if (isMouseInsideRightImage()) {
			rightImageScale -= count / 10;
			rightImageScale = context.max(rightImageScale, 1);
			rightImageScale = context.min(rightImageScale, 4);
		}
		if (isMouseInsideLeftImage()) {
			leftImageScale -= count / 10;
			leftImageScale = context.max(leftImageScale, 1);
			leftImageScale = context.min(leftImageScale, 4);
		}
	}

	private boolean isMouseInsideRightImage() {
		if (context.mouseY > imageMarginTop && context.mouseY < context.height - imageMarginTop) {
			if (context.mouseX > rightImageMarginLeft && context.mouseX < rightImageMarginLeft + wImageViewerSize) {
				return true;
			}
		}
		return false;
	}

	private boolean isMouseInsideLeftImage() {
		if (context.mouseY > imageMarginTop && context.mouseY < context.height - imageMarginTop) {
			if (context.mouseX > leftImageMarginLeft && context.mouseX < leftImageMarginLeft + wImageViewerSize) {
				return true;
			}
		}
		return false;
	}

	private void initZoomLeft() {
		if (isMouseInsideLeftImage()) {
			if (leftZoomOffset == null)
				leftZoomOffset = new PVector(context.mouseX, context.mouseY);
		}
	}

	private void updateZoomLeft() {
		if (isMouseInsideLeftImage()) {
			if (leftZoomOffset != null) {
				leftZoomOffset.x += context.mouseX - context.pmouseX;
				if(leftZoomOffset.x<leftImageMarginLeft)
					leftZoomOffset.x = leftImageMarginLeft;
				else if(leftZoomOffset.x>leftImageMarginLeft+wImageViewerSize)
					leftZoomOffset.x=leftImageMarginLeft+wImageViewerSize;
				
				leftZoomOffset.y += context.mouseY - context.pmouseY;
				if(leftZoomOffset.y<imageMarginTop)
					leftZoomOffset.y = imageMarginTop;
				else if(leftZoomOffset.y>imageMarginTop+hImageViewerSize)
					leftZoomOffset.y=imageMarginTop+hImageViewerSize;
			}
		}
	}

	private void exitZoomLeft() {
		if (isMouseInsideLeftImage()) {
			leftZoomOffset = null;
		}
	}

	private void initZoomRight() {
		if (isMouseInsideRightImage()) {
			if (rightZoomOffset == null)
				rightZoomOffset = new PVector(context.mouseX, context.mouseY);
		}
	}

	private void updateZoomRight() {
		if (isMouseInsideRightImage()) {
			if (rightZoomOffset != null) {
				rightZoomOffset.x += context.mouseX - context.pmouseX;
				if(rightZoomOffset.x<rightImageMarginLeft)
					rightZoomOffset.x = rightImageMarginLeft;
				else if(rightZoomOffset.x>rightImageMarginLeft+wImageViewerSize)
					rightZoomOffset.x=rightImageMarginLeft+wImageViewerSize;
				
				rightZoomOffset.y += context.mouseY - context.pmouseY;
				if(rightZoomOffset.y<imageMarginTop)
					rightZoomOffset.y = imageMarginTop;
				else if(rightZoomOffset.y>imageMarginTop+hImageViewerSize)
					rightZoomOffset.y=imageMarginTop+hImageViewerSize;
			}
		}
	}

	private void exitZoomRight() {
		if (isMouseInsideRightImage()) {
			rightZoomOffset = null;
		}
	}

	public List<Guide> copyGuides(List<Guide> guides) {
		List<Guide> temp = new ArrayList<>();
		for (int i = 0; i < guides.size(); i++) {
			Guide ha = guides.get(i);
			temp.add(new Guide(ha.pos.copy(), ha.translatePos.copy(), ha.threshold, ha.name));
		}
		return temp;
	}

	public void noZoom() {
		leftZoomOffset = null;
		rightZoomOffset = null;
	}

	public void resetPreviews() {
		// TODO Auto-generated method stub
		imgPreviewLeft = null;
		imgPreviewRight = null;
	}
}
