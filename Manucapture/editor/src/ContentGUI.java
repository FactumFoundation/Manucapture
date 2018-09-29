import java.util.ArrayList;
import java.util.List;

import processing.core.PImage;
import processing.core.PVector;
import processing.event.MouseEvent;

public class ContentGUI {
	
	ManuCapture_v1_1 context;
	boolean renderRightImage = true;
	boolean renderLeftImage = true;
	PVector lastPressedR = null;
	PVector lastPressedL = null;
	List<Guide> guidesLeft = new ArrayList<>();
	List<Guide> guidesRight = new ArrayList<>();
	int imageMarginTop = 40;
	int rightImageMarginLeft = 1070;
	int leftImageMarginLeft = 350;
	// size view on screen
	float ratioAspect = 1f;
	int hImageViewerSize = (int)(1000*ratioAspect);
	int wImageViewerSize = (int)(667*ratioAspect);
	// width size of the preview
	public int viewerWidthResolution = 2000;
	PImage imgPreviewLeft;
	PImage imgPreviewRight;
	Guide GuideSelected = null;
	PImage lastLeftPreview = null;
	PImage lastRightPreview = null;
	PImage cameraIcon;
	PImage zoomImg = null;
	float rightImageScale = 1.0f;
	float leftImageScale = 1.0f;
	float baseZoom = 2f; 
	
	public ContentGUI(ManuCapture_v1_1 context){
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
		guidesLeft.add(new Guide(new PVector(size,0), translatePos1, size, "LL"));
		guidesLeft.add(new Guide(new PVector(0,size), translatePos1, size, "LT"));
		guidesLeft.add(new Guide(new PVector(wImageViewerSize-size,0), translatePos1, size, "LR"));
		guidesLeft.add(new Guide(new PVector(0, hImageViewerSize-size), translatePos1, size, "LB"));
		guidesRight.add(new Guide(new PVector(size,0), translatePos2, size, "RL"));
		guidesRight.add(new Guide(new PVector(0,size), translatePos2, size, "RT"));
		guidesRight.add(new Guide(new PVector(wImageViewerSize-size,0), translatePos2, size, "RR"));
		guidesRight.add(new Guide(new PVector(0, hImageViewerSize-size), translatePos2, size, "RB"));		
	}

	public void draw() {
		if (renderRightImage) {
			drawRightImage();
		}
		if (renderLeftImage) {
			drawLeftImage();
		}		
		drawCropGuides();
		drawCalibrationInfo();
		drawCameraStatusOverlay();
	}
		
	private void drawLeftImage() {
		if (context.project.selectedItem != null && imgPreviewLeft != null) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(leftImageMarginLeft, imageMarginTop);
			drawImagePreview(imgPreviewLeft, lastPressedL, leftImageMarginLeft, leftImageScale);
			if (lastPressedL != null) {
				context.tint(255, 200);
			} else {
				context.tint(255, 20);
			}
			context.image(zoomImg, wImageViewerSize - 70, 20, 50, 50);
			context.fill(255);
			context.textSize(18);
			context.text(context.project.selectedItem.mImageLeft.imagePath, 210, -10);
			context.popMatrix();			
			context.popStyle();
		} else {
			context.stroke(255);
			context.fill(50);
			context.rect(rightImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize);
		}
	}

	private void drawRightImage() {
		if (context.project.selectedItem != null && imgPreviewRight != null) {
			context.pushStyle();
			context.pushMatrix();
			context.translate(rightImageMarginLeft, imageMarginTop);
			context.imageMode(context.CORNER);
			drawImagePreview(imgPreviewRight, lastPressedR, rightImageMarginLeft, rightImageScale);
			if (lastPressedR != null) {
				context.tint(255, 200);
			} else {
				context.tint(255, 20);
			}
			context.image(zoomImg, wImageViewerSize - 70, 20, 50, 50);
			context.fill(255);
			context.textSize(18);
			context.text(context.project.selectedItem.mImageRight.imagePath, 210, -10);
			context.popMatrix();				
			context.popStyle();
		} else {
			context.stroke(255);
			context.fill(50);
			context.rect(leftImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize);
		}
	}
	
	private void drawImagePreview(PImage imgPreview, PVector lastPressed, int marginLeft,float scale) {
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
			context.image(imgPreview, 0, 0, wImageViewerSize, hImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {
			context.image(imgPreview, 0, 0, wImageViewerSize, hImageViewerSize, 0, 0, imgPreview.width,
					imgPreview.height);
		}
	}
	
	public void drawCropGuides() {
		// TODO: Draw guides				
		if (lastPressedR == null) {
			for (Guide guide : guidesRight) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(255,0,0);
				} else {
					context.stroke(93,183,255);
				}
				PVector screenPos = guide.getScreenPosition();
				if(guide.isHorizontal()) {
					context.line(screenPos.x, screenPos.y,screenPos.x+wImageViewerSize,screenPos.y);
				} else {
					context.line(screenPos.x, screenPos.y,screenPos.x,screenPos.y+hImageViewerSize);
				}
				context.popStyle();
			}
		} else {
			float imgScale = imgPreviewRight.width / (float) wImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedR, new PVector(rightImageMarginLeft, imageMarginTop));
			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);
			float portviewSizeX =  wImageViewerSize / rightImageScale;
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
				if (context.cropMode) {
					context.stroke(255,150,0);
				} else {
					context.stroke(255,0,0);
				}
				if(guide.isHorizontal()) {
					float screenY = context.map(baseZoom*guide.pos.y,portviewStartY,portviewStartY+portviewSizeY,(float)imageMarginTop,(float)imageMarginTop+hImageViewerSize*rightImageScale);
					if(screenY >= imageMarginTop && screenY<=imageMarginTop+hImageViewerSize)
						context.line(rightImageMarginLeft, screenY,rightImageMarginLeft+wImageViewerSize,screenY);
				} else {
					float screenX = context.map(baseZoom*guide.pos.x,portviewStartX,portviewStartX+portviewSizeX,(float)rightImageMarginLeft,(float)rightImageMarginLeft+wImageViewerSize*rightImageScale);
					if(screenX >= rightImageMarginLeft && screenX<=rightImageMarginLeft+wImageViewerSize)
						context.line(screenX, imageMarginTop,screenX,imageMarginTop+hImageViewerSize);
				}
			}
		}
		if (lastPressedL == null) {
			for (Guide guide : guidesLeft) {
				context.pushStyle();
				context.strokeWeight(3);
				if (context.cropMode) {
					context.stroke(255,0,0);
				} else {
					context.stroke(93,183,255);
				}
				PVector screenPos = guide.getScreenPosition();
				if(guide.isHorizontal()) {
					context.line(screenPos.x, screenPos.y,screenPos.x+wImageViewerSize,screenPos.y);
				} else {
					context.line(screenPos.x, screenPos.y,screenPos.x,screenPos.y+hImageViewerSize);
				}
				context.popStyle();
			}
		} else {
			float imgScale = imgPreviewLeft.width / (float) wImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedL, new PVector(leftImageMarginLeft, imageMarginTop));
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
				if (context.cropMode) {
					context.stroke(255,150,0);
				} else {
					context.stroke(255,0,0);
				}
				if(guide.isHorizontal()) {
					float screenY = context.map(baseZoom*guide.pos.y,portviewStartY,portviewStartY+portviewSizeY,(float)imageMarginTop,(float)imageMarginTop+hImageViewerSize*leftImageScale);
					if(screenY >= imageMarginTop && screenY<=imageMarginTop+hImageViewerSize)
						context.line(leftImageMarginLeft, screenY,leftImageMarginLeft+wImageViewerSize,screenY);
				} else {
					float screenX = context.map(baseZoom*guide.pos.x,portviewStartX,portviewStartX+portviewSizeX,(float)leftImageMarginLeft,(float)leftImageMarginLeft+wImageViewerSize*leftImageScale);
					if(screenX >= leftImageMarginLeft && screenX<=leftImageMarginLeft+wImageViewerSize)
						context.line(screenX, imageMarginTop,screenX,imageMarginTop+hImageViewerSize);
				}
			}
		}
		if (GuideSelected != null) {
			context.stroke(255, 255, 0);
			PVector screenPos = GuideSelected.getScreenPosition();
			if(GuideSelected.isHorizontal()) {
				context.line(screenPos.x, screenPos.y,screenPos.x+wImageViewerSize,screenPos.y);
			} else {
				context.line(screenPos.x, screenPos.y,screenPos.x,screenPos.y+hImageViewerSize);
			}	
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
					context.image(lastLeftPreview, leftImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize, 0, 0,
							lastLeftPreview.width, lastLeftPreview.height);
				}
				if (lastRightPreview != null) {
					context.tint(255, 125);
					context.image(lastRightPreview, rightImageMarginLeft, imageMarginTop, wImageViewerSize, hImageViewerSize, 0, 0,
							lastRightPreview.width, lastRightPreview.height);
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
		//	context.popStyle();
		//	context.popMatrix();
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
		noZoom();
	}
	
	public void exitCropMode() {
		GuideSelected = null;
	}
	
	public void mouseMoved() {
	}
	
	public void mousePressed() {

		if (context.cropMode) {
			if(isMouseInsideLeftImage()) {
				for (Guide guide : guidesLeft) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						GuideSelected = guide;
						break;
					}
				}	
			}
			else if(isMouseInsideRightImage()) {
				for (Guide guide : guidesRight) {
					if (guide.isOver(context.mouseX, context.mouseY)) {
						GuideSelected = guide;
						break;
					}
				}
			}
		}
		else if(context.chartStateMachine==3){
			context.shutterMode = context.NORMAL_SHUTTER;
			context.setCaptureState(context.CAMERAS_IDLE);
			context.chartStateMachine = 0;
			context.gui.btnColorChart.setState(0);
			noZoom();
		} else if(context.chartStateMachine==0){
			if ( context.project != null && context.project.selectedItem != null) {
				if (context.mouseButton == context.LEFT) {
					if (context.getStateApp()==context.STATE_APP_PROJECT && lastPressedL == null && context.project.selectedItem.mImageLeft != null
							&& imgPreviewLeft != null) {
						initZoomLeft();
					}
					if (context.getStateApp()==context.STATE_APP_PROJECT && lastPressedR == null && context.project.selectedItem.mImageRight != null
							&& imgPreviewRight != null)
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
		if (GuideSelected != null) {
			if (GuideSelected.name.startsWith("L")) {
				if (isMouseInsideLeftImage())
					GuideSelected.setImagePosition(context.mouseX, context.mouseY);
			}
			if (GuideSelected.name.startsWith("R")) {
				if (isMouseInsideRightImage())
					GuideSelected.setImagePosition(context.mouseX, context.mouseY);
			}
			if (context.project != null && context.project.selectedItem != null) {
				context.project.selectedItem.mImageLeft.guides = copyGuides(guidesLeft);
				context.project.selectedItem.mImageRight.guides = copyGuides(guidesRight);
			}
		}
		else if (lastPressedR != null) {
			updateZoomRight();
		}
		else if (lastPressedL != null) {
			updateZoomLeft();
		}
	}
	
	public void mouseReleased() {
		GuideSelected = null;
		if(context.mouseX > leftImageMarginLeft && context.mouseX < rightImageMarginLeft + wImageViewerSize) {
			if(context.project != null && context.project.selectedItem != null)
				context.project.selectedItem.saveMetadata();			
		}
	}
	
	public void mouseWheel(float count) {
		if(!context.cropMode) {
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
			if(lastPressedL == null)
				lastPressedL = new PVector(context.mouseX, context.mouseY);
		}	
	}
	
	private void updateZoomLeft() {
		if (isMouseInsideLeftImage()) {
			if(lastPressedL != null) {
				lastPressedL.x += context.mouseX - context.pmouseX;
				/*
				if(lastPressedL.x>leftImageMarginLeft+wImageViewerSize) {
					lastPressedL.x = leftImageMarginLeft+wImageViewerSize;
				} else if(lastPressedL.x<leftImageMarginLeft) {
					lastPressedL.x=leftImageMarginLeft;
				}
				*/
				lastPressedL.y += context.mouseY - context.pmouseY;
			}
		}	
	}

	private void exitZoomLeft() {
		if (isMouseInsideLeftImage()) {
			lastPressedL = null;
		}
	}
	
	private void initZoomRight() {
		if (isMouseInsideRightImage()) {
			if(lastPressedR == null)
				lastPressedR = new PVector(context.mouseX, context.mouseY);
		}	
	}

	private void updateZoomRight() {
		if (isMouseInsideRightImage()) {
			if(lastPressedR != null) {
				lastPressedR.x += context.mouseX - context.pmouseX;
				lastPressedR.y += context.mouseY - context.pmouseY;				
			}
		}	
	}
	
	private void exitZoomRight() {
		if (isMouseInsideRightImage()) {
			lastPressedR = null;
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
		lastPressedL = null;
		lastPressedR = null;
	}
}
