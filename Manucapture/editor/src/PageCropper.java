import java.util.ArrayList;
import java.util.List;

import boofcv.alg.feature.detect.line.LineImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughFootSubimage;
import boofcv.processing.Boof;
import boofcv.processing.SimpleGray;
import boofcv.struct.image.ImageDataType;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import processing.core.PImage;
import processing.core.PVector;

public class PageCropper {
	
	ManuCapture_v1_1 context;
	
	List<LineParametric2D_F32> leftLinesFootSub;
	List<LineParametric2D_F32> rightLinesFootSub;
	
	List<Guide> previousGuidesLeft = new ArrayList<>();
	List<Guide> previousGuidesRight = new ArrayList<>();
	
	List<Guide> newGuidesLeft = new ArrayList<>();
	List<Guide> newGuidesRight = new ArrayList<>();
	
	int maxLines = 5;
	int leftImgWidth, leftImgHeight, rightImgWidth, rightImgHeight;
	int margin = 10;
	
	PageCropper(ManuCapture_v1_1 context){
		this.context=context;
	}
	
	public void initGuides(List<Guide> leftGuides, List<Guide> rightGuides) {
		previousGuidesLeft = context.contentGUI.copyGuides(leftGuides);
		previousGuidesRight = context.contentGUI.copyGuides(rightGuides);
		newGuidesLeft = previousGuidesLeft;
		newGuidesRight = previousGuidesRight;
	}
	
	public List<Guide>[] estimateGuides(PImage imgLeft, PImage imgRight) {

		previousGuidesLeft = newGuidesLeft;
		previousGuidesRight = newGuidesRight;
		List<Guide>[] newGuides = new ArrayList[2];
		

		if(imgLeft != null) {
			SimpleGray gray = Boof.gray(imgLeft,ImageDataType.F32);
			leftLinesFootSub = gray.linesHoughFootSub(new ConfigHoughFootSubimage(maxLines));
			leftImgWidth = imgLeft.width;
			leftImgHeight = imgLeft.height;
			
			for (int i = 0; i < previousGuidesLeft.size(); i++) {
				Guide guide = previousGuidesLeft.get(i);
				Guide newGuide = new Guide(guide.pos.copy(), guide.translatePos.copy(), guide.threshold, guide.name);
				if(guide.name.equals("LL")) {
					float minimum = 5000.0f;
					for( LineParametric2D_F32 p : leftLinesFootSub ) {
						if(p.getSlopeX()==0.0) {
							if(p.getP().x<minimum) {
								float newPos = context.contentGUI.wImageViewerSize * p.getP().x/(float)imgLeft.width - margin;
								if(context.abs(newPos - guide.pos.x)<context.contentGUI.wImageViewerSize/4) {
									newGuide.pos.x = newPos;
									minimum = p.getP().x;
								}
							}
						}
					}
				} else if(guide.name.equals("LT")) {
					float minimum = 5000.0f;
					for( LineParametric2D_F32 p : leftLinesFootSub ) {
						if(p.getSlopeY()==0.0) {
							if(p.getP().y<minimum) {
								float newPos = context.contentGUI.hImageViewerSize * p.getP().y/(float)imgLeft.height - margin;
								if(context.abs(newPos - guide.pos.y)<context.contentGUI.hImageViewerSize/4) {
									newGuide.pos.y = newPos;
									minimum = p.getP().y;
								}
							}
						}
					}
				} else if (guide.name.equals("LR")) {
					newGuide.pos.x = context.contentGUI.wImageViewerSize;
				} else if(guide.name.equals("LB")) {
					float maximum = 0.0f;
					for( LineParametric2D_F32 p : leftLinesFootSub ) {
						if(p.getSlopeY()==0.0) {
							if(p.getP().y>maximum) {
								float newPos = context.contentGUI.hImageViewerSize * p.getP().y/(float)imgLeft.height + margin;
								if(context.abs(newPos - guide.pos.y)<context.contentGUI.hImageViewerSize/4) {
									newGuide.pos.y = newPos;
									maximum = p.getP().y;	
								}
							}
						}
					}					
				} 
				newGuidesLeft.set(i, newGuide);
			}
			
			newGuides[0] = newGuidesLeft;
		}
		else {
			context.println("estimateGuides: Left preview not available");
		}
		
		if(imgRight != null) {
			SimpleGray gray = Boof.gray(imgRight,ImageDataType.F32);
			rightLinesFootSub = gray.linesHoughFootSub(new ConfigHoughFootSubimage(maxLines));
			rightImgWidth = imgRight.width;
			rightImgHeight = imgRight.height;
			
			for (int i = 0; i < previousGuidesRight.size(); i++) {
				Guide guide = previousGuidesRight.get(i);
				Guide newGuide = new Guide(guide.pos.copy(), guide.translatePos.copy(), guide.threshold, guide.name);
				if(guide.name.equals("RL")) {
					newGuide.pos.x = 0.1f;
				} else if(guide.name.equals("RT")) {
					float minimum = 5000.0f;
					for( LineParametric2D_F32 p : rightLinesFootSub ) {
						if(p.getSlopeY()==0.0) {
							if(p.getP().y<minimum) {
								float newPos = context.contentGUI.hImageViewerSize * p.getP().y/(float)imgRight.height - margin;
								if(context.abs(newPos-guide.pos.y)<context.contentGUI.hImageViewerSize/4) {
									newGuide.pos.y = context.contentGUI.hImageViewerSize * p.getP().y/(float)imgRight.height - margin;
									minimum = p.getP().y;	
								}
							}
						}
					}
				} else if (guide.name.equals("RR")) {
					float maximum = -10.0f;
					for( LineParametric2D_F32 p : rightLinesFootSub ) {
						if(p.getSlopeX()==0.0) {
							if(p.getP().x>maximum) {
								float newPos = context.contentGUI.wImageViewerSize * p.getP().x/(float)imgRight.width + margin;
								if(context.abs(newPos-guide.pos.x)<context.contentGUI.wImageViewerSize/4) {
									newGuide.pos.x = newPos;
									maximum = p.getP().x;
								}		
							}
						}
					}
				} else if(guide.name.equals("RB")) {
					float maximum = 0.0f;
					for( LineParametric2D_F32 p : rightLinesFootSub ) {
						if(p.getSlopeY()==0.0) {
							if(p.getP().y>maximum) {
								float newPos =  context.contentGUI.hImageViewerSize * p.getP().y/(float)imgRight.height + margin;
								if(context.abs(newPos-guide.pos.y)<context.contentGUI.hImageViewerSize/4) {
									newGuide.pos.y = newPos;
									maximum = p.getP().y;
								}
							}
						}
					}					
				} 
				newGuidesRight.set(i, newGuide);
			}
			
			
			newGuides[1] = newGuidesRight;
		}
		else {
			context.println("estimateGuides: Right preview not available");
		}
		
		return newGuides;
	}
	
	public void draw(int x_leftOffset, int y_leftOffset, int x_rightOffset, int y_rightOffset, int width, int height) {
		
		// Draw left lines
		if(leftLinesFootSub!=null) {
			context.pushMatrix();
			context.translate(x_leftOffset,y_leftOffset);
			drawLines(leftLinesFootSub,leftImgWidth,leftImgHeight,(float)width/leftImgWidth, (float)height/leftImgHeight);
			context.popMatrix();			
		}
		
		// Draw right lines
		if(rightLinesFootSub!=null) {
			context.pushMatrix();
			context.translate(x_rightOffset,y_rightOffset);
			drawLines(rightLinesFootSub,rightImgWidth,rightImgHeight,(float)width/rightImgWidth, (float)height/rightImgHeight);		
			context.popMatrix();
		}
		
	}
	
	void drawLines( List<LineParametric2D_F32> lines, int width, int height, float scaleX, float scaleY) {
		
		context.noFill();
		context.strokeWeight(3);
		context.stroke(0xFF,50,50, 100);

		int lineCount = 0;
		for( LineParametric2D_F32 p : lines ) {
			lineCount++;
		    LineSegment2D_F32 ls = LineImageOps.convert(p,width,height);
		    if(ls != null) {
		    	context.line((float)ls.a.x * scaleX, (float)ls.a.y * scaleY, (float)ls.b.x * scaleX, (float)ls.b.y * scaleY); 
		    } else {
		    	context.println("Linea nula!!!!!!!!!!!!!!!!!!!");
		    	context.println(p);
		    }
		}
	}


}
