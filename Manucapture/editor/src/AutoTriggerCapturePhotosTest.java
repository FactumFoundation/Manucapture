import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.serial.*;

import java.util.HashMap;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;

import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class AutoTriggerCapturePhotosTest extends PApplet {
	OscP5 oscP5;
	NetAddress myRemoteLocation;

	public void setup() {
		oscP5 = new OscP5(this, 13003);
		myRemoteLocation = new NetAddress("127.0.0.1", 3334);
	}

	public void draw() {
		
		println("Pedal Pressed");
		OscMessage myMessage = new OscMessage("/footswitchPressed");
		myMessage.add("");
		oscP5.send(myMessage, myRemoteLocation);
		try {
			Thread.sleep(9000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void settings() {
		size(200, 200);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "AutoTriggerCapturePhotosTest" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
