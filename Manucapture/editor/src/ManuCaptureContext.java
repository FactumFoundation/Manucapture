import netP5.NetAddress;
import oscP5.OscP5;
import processing.core.PApplet;

public class ManuCaptureContext {

	PApplet parent;

	RawFile thumbnail;

	G2P5 gphotoA;
	G2P5 gphotoB;

	OscP5 oscP5;

	GUI gui;

	NetAddress viewerLocation;
	
	Project project;
	
	boolean renderLeft = true;
	boolean renderRight = true;
	
}
