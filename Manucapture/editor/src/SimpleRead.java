import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

import processing.serial.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class SimpleRead extends PApplet {

	/**
	 * Simple Read
	 * 
	 * Read data from the serial port and change the color of a rectangle when a
	 * switch connected to a Wiring or Arduino board is pressed and released. This
	 * example works with the Wiring / Arduino program that follows below.
	 */

	Serial myPort; // Create object from Serial class
	int val; // Data received from the serial port

	public void setup() {

		// I know that the first port in the serial list on my mac
		// is always my FTDI adaptor, so I open Serial.list()[0].
		// On Windows machines, this generally opens COM1.
		// Open whatever port is the one you're using.
		String portName = Serial.list()[0];
		myPort = new Serial(this, portName, 9600);
	}

	public void draw() {
		if (myPort.available() > 0) { // If data is available,
			val = myPort.read(); // read it and store it in val
		}
		background(255); // Set background to white
		if (val == 0) { // If the serial value is 0,
			fill(0); // set fill to black
		} else { // If the serial value is not 0,
			fill(204); // set fill to light gray
		}
		rect(50, 50, 100, 100);
	}

	/*
	 * 
	 * // Wiring / Arduino Code // Code for sensing a switch status and writing the
	 * value to the serial port.
	 * 
	 * int switchPin = 4; // Switch connected to pin 4
	 * 
	 * void setup() { pinMode(switchPin, INPUT); // Set pin 0 as an input
	 * Serial.begin(9600); // Start serial communication at 9600 bps }
	 * 
	 * void loop() { if (digitalRead(switchPin) == HIGH) { // If switch is ON,
	 * Serial.write(1); // send 1 to Processing } else { // If the switch is not ON,
	 * Serial.write(0); // send 0 to Processing } delay(100); // Wait 100
	 * milliseconds }
	 * 
	 */
	public void settings() {
		size(200, 200);
	}

	static public void main(String[] passedArgs) {
		String[] appletArgs = new String[] { "SimpleRead" };
		if (passedArgs != null) {
			PApplet.main(concat(appletArgs, passedArgs));
		} else {
			PApplet.main(appletArgs);
		}
	}
}
