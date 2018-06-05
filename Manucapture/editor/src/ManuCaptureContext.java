import java.util.ArrayList;
import java.util.List;

import netP5.NetAddress;
import oscP5.OscP5;
import processing.core.PApplet;

public class ManuCaptureContext {

	ManuCapture_v1_1 parent;

	RawFile thumbnail;

	G2P5 gphotoA;
	G2P5 gphotoB;

	OscP5 oscP5;

	GUI gui;

	NetAddress viewerLocation;
	
	Project project;
	
	boolean renderLeft = true;
	boolean renderRight = true;
	
	String appPath = "/home/factum/git/book_scanner/bookScanner/Manucapture";
	
	int rotA = 270;
	int rotB = 90;
	
	List<HotArea> pointsLeft = new ArrayList<>();
	List<HotArea> pointsRight = new ArrayList<>();
	
}
