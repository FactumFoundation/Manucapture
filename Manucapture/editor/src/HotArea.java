import java.awt.event.KeyEvent;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class HotArea {

	PVector pos;

	PVector translatePos = new PVector();

	int id;

	float threshold = 70.0f;

	String name;

	public HotArea(PVector pos, PVector translatePos,int id, float threshold, String name) {
		super();
		this.pos = pos;
		this.id = id;
		this.threshold = threshold;
		this.name = name;
		this.translatePos = translatePos;
	}

	public boolean isInArea(float x, float y) {
		float d = getDist(x, y);
		if (d < threshold) {
			return true;
		} else {
			return false;
		}
	}

	float getDist(float x, float y) {
		float d = PApplet.dist(translatePos.x+pos.x, translatePos.y+pos.y, x, y);
		return d;
	}
	
	void setRealPosition(float x,float y) {
		
		this.pos.x = x-translatePos.x;
		this.pos.y = y-translatePos.y;
		
	}

	PVector getRealPosition(){
		return PVector.add(pos,translatePos);
	}
	
	public void draw(PGraphics canvas) {
		draw(canvas, PVector.add(pos,translatePos));
	}


	public void draw(PGraphics canvas, PVector pos) {

//		realPos.x = canvas.screenX(pos.x, pos.y);
//		realPos.y = canvas.screenY(pos.x, pos.y);
		canvas.noFill();
		canvas.ellipse(pos.x, pos.y, threshold, threshold);
		canvas.line(pos.x-threshold/2, pos.y,pos.x+threshold/2, pos.y);
		canvas.line(pos.x, pos.y-threshold/2,pos.x
				, pos.y+threshold/2);
		
	}
}
