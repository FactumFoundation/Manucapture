import java.awt.event.KeyEvent;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class Guide {
	
	PVector pos;
	PVector translatePos;
	float threshold = 70.0f;
	String name;

	public Guide(PVector pos, PVector translatePos, float threshold, String name) {
		super();
		this.pos = pos;
		this.threshold = threshold;
		this.name = name;
		this.translatePos = translatePos;
	}
	
	public boolean isHorizontal() {
		return (pos.x == 0.0);
	}

	public boolean isOver(float x, float y) {
		float dist;
		PVector screenPos = getScreenPosition();
		if(isHorizontal()) {
			 dist = PApplet.abs(y-screenPos.y);
		} else {
			 dist = PApplet.abs(x-screenPos.x);
		}
		if (dist < threshold) {
			return true;
		} else {
			return false;
		}
	}
	
	void setImagePosition(int x, int y) {
		if(this.pos.x == 0) {
			this.pos.y = y - translatePos.y;
		} else if(this.pos.y == 0) {
			this.pos.x = x - translatePos.x;
		}
		else {
			PApplet.println("This is strange: A guide with pos.x and pos.y different than 0");
		}
	}
	
	PVector getScreenPosition() {
		return PVector.add(pos,translatePos);
	}
	
	
}
