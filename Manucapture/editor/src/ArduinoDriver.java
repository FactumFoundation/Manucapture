import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import oscP5.*;
import netP5.*;
import processing.serial.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ArduinoDriver extends PApplet {

	/**
	 * Serial Call-Response by Tom Igoe.
	 * 
	 * Sends a byte out the serial port, and reads 3 bytes in. Sets foregound
	 * color, xpos, and ypos of a circle onstage using the values returned from
	 * the serial port. Thanks to Daniel Shiffman and Greg Shakar for the
	 * improvements.
	 * 
	 * Note: This sketch assumes that the device on the other end of the serial
	 * port is going to send a single byte of value 65 (ASCII A) on startup. The
	 * sketch waits for that byte, then sends an ASCII A whenever it wants more
	 * data.
	 */

	OscP5 oscP5;
	NetAddress myRemoteLocation;

	Serial myPort; // The serial port
	int[] serialInArray = new int[3]; // Where we'll put what we receive
	int serialCount = 0; // A count of how many bytes we receive
	boolean firstContact = false; // Whether we've heard from the
									// microcontroller
	boolean arduinoConnected = true;
	
	public void setup() {
		// Stage size
		noStroke(); // No border on the next thing drawn

		// Print a list of the serial ports, for debugging purposes:
		try {
			printArray(Serial.list());
		} catch(Exception e) {
			System.out.println("Problem accessing Serial devices");
			e.printStackTrace();
			arduinoConnected=false;
		}

		// I know that the first port in the serial list on my mac
		// is always my FTDI adaptor, so I open Serial.list()[0].
		// On Windows machines, this generally opens COM1.
		// Open whatever port is the one you're using.
		String portName = Serial.list()[0];
		try {
		myPort = new Serial(this, portName, 9600);
		} catch(Exception e) {
			System.out.println("Problem opening serial port " + portName);
			e.printStackTrace();
			arduinoConnected=false	;		
		}
		
		oscP5 = new OscP5(this, 13000);
		myRemoteLocation = new NetAddress("127.0.0.1", 3334);
		if(!arduinoConnected) {
			OscMessage myMessage = new OscMessage("/error");
			myMessage.add(-1);
			oscP5.send(myMessage, myRemoteLocation);			
		}
	}

	public void draw() {
		if(arduinoConnected) {
			background(0);
		} else {
			background(255,0,0);
		}
		
	}

	public void serialEvent(Serial myPort) {
		// read a byte from the serial port:

		int inByte = myPort.read();
		println((char) inByte);
		if ((char) inByte == 'F') {
			println("Pedal Pressed");
			OscMessage myMessage = new OscMessage("/footswitchPressed");
			myMessage.add("");
			oscP5.send(myMessage, myRemoteLocation);
		} else {
			println("caracter no manejado desde arduino" + (char) inByte);
		}
	}

	int cameraState = 0;

	public void mouseClicked() {
		// Send a capital T to Trigger
		if (cameraState == 0) {
			myPort.write('P');
			println("Sent P");
			cameraState = 1;
		} else {
			myPort.write('W');
			println("Sent W");
			cameraState = 0;
		}
		// myPort.write('R');
	}

	/* incoming osc message are forwarded to the oscEvent method. */
	public void oscEvent(OscMessage theOscMessage) {
		/*
		 * print the address pattern and the typetag of the received OscMessage
		 */
		print("### received an osc message.");
		print(" addrpattern: " + theOscMessage.addrPattern());
		println(" typetag: " + theOscMessage.get(0).charValue());

		myPort.write(theOscMessage.get(0).charValue());
	}

	/*
	 * 
	 * // Serial Call and Response // by Tom Igoe // Language: Wiring/Arduino
	 * 
	 * // This program sends an ASCII A (byte of value 65) on startup // and
	 * repeats that until it gets some data in. // Then it waits for a byte in
	 * the serial port, and // sends three sensor values whenever it gets a byte
	 * in.
	 * 
	 * // Thanks to Greg Shakar for the improvements
	 * 
	 * // Created 26 Sept. 2005 // Updated 18 April 2008
	 * 
	 * 
	 * int firstSensor = 0; // first analog sensor int secondSensor = 0; //
	 * second analog sensor int thirdSensor = 0; // digital sensor int inByte =
	 * 0; // incoming serial byte
	 * 
	 * void setup() { // start serial port at 9600 bps: Serial.begin(9600);
	 * pinMode(2, INPUT); // digital sensor is on digital pin 2
	 * establishContact(); // send a byte to establish contact until Processing
	 * responds }
	 * 
	 * void loop() { // if we get a valid byte, read analog ins: if
	 * (Serial.available() > 0) { // get incoming byte: inByte = Serial.read();
	 * // read first analog input, divide by 4 to make the range 0-255:
	 * firstSensor = analogRead(0)/4; // delay 10ms to let the ADC recover:
	 * delay(10); // read second analog input, divide by 4 to make the range
	 * 0-255: secondSensor = analogRead(1)/4; // read switch, multiply by 155
	 * and add 100 // so that you're sending 100 or 255: thirdSensor = 100 +
	 * (155 * digitalRead(2)); // send sensor values: Serial.write(firstSensor);
	 * Serial.write(secondSensor); Serial.write(thirdSensor); } }
	 * 
	 * void establishContact() { while (Serial.available() <= 0) {
	 * Serial.write('A'); // send a capital A delay(300); } }
	 * 
	 * 
	 */
	public void settings() {
		size(256, 256);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "ArduinoDriver" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
