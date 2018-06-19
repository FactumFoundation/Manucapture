
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;

/**
 * Camera console wrapper
 * 
 * @author veronica1
 *
 */

public class G2P5 {

	private String eosSerial;
	public String port;
	protected String id;
	public boolean active = false;
	private boolean tethering = false;

	private int actionCode;

	public static int CAMERA_IDLE = 0;
	public static int CAMERA_CAPTURE = 1;
	public static int CAMERA_INACTIVE = -1;

	// G2P5TetheredCaptureThread t;

	Runnable captureRunnable;
	Thread thread;

	G2P5Listener listener;

	String homeDirectory;

	List<G2P5Event> events = new ArrayList<>();

	boolean mock = false;

	public G2P5() {

	}

	public G2P5(String homeDirectory, String eosSerial, String port, String id) {
		this.eosSerial = eosSerial;
		this.id = id;
		this.port = port;
		this.tethering = true;
		this.homeDirectory = homeDirectory;
		// setActive(true);

	}

	public synchronized String getPort() {
		return port;
	}

	public synchronized String getSerial() {
		return eosSerial;
	}

	public void setActive(boolean active) {

		if (thread != null && thread.isAlive()) {
			thread.interrupt();
		}

		this.active = active;
		if (port != null) {
			if (active) {
				setAction(CAMERA_IDLE);
				TetheredCaptureRunnable captureRunnable = new TetheredCaptureRunnable();
				Thread thread = new Thread(captureRunnable);
				captureRunnable.g2p5 = this;
				captureRunnable.thread = thread;
				thread.start();
				this.thread = thread;
			} else {
				setAction(CAMERA_INACTIVE);
				killAllProcessByName(id + ".cr2");
				actionCode = CAMERA_INACTIVE;
				if (thread != null && thread.isAlive()) {
					thread.interrupt();
				}
			}
		} else {
			if (!active) {
				setAction(CAMERA_INACTIVE);
				killAllProcessByName(id + ".cr2");
				actionCode = CAMERA_INACTIVE;

			} else if (true) {
				if (!mock) {
					captureRunnable = new TetheredCaptureRunnable();
					this.active = false;
				}
			}
		}

		if (mock && active) {
			TetheredMockCaptureRunnable captureRunnable = new TetheredMockCaptureRunnable();
			Thread thread = new Thread(captureRunnable);
			captureRunnable.g2p5 = this;
			captureRunnable.thread = thread;
			this.captureRunnable = captureRunnable;
			thread.start();
			this.thread = thread;
		}

	}

	public boolean isConnected() {
		return thread != null && thread.isAlive();
	}

	public synchronized void setAction(int actionCode) {
		this.actionCode = actionCode;
	}

	public synchronized int getAction() {
		return actionCode;
	}


	public String[] getParameters() {
		killAllProcessByName(id + ".cr2");
		return null;
	}

	public boolean setParameters(String iso, String shutterSpeed, String fnumber) {
		killAllProcessByName(id + ".cr2");
		return false;
	}

	public void invokePhotoEvent(String path) {
		sendEvent(new G2P5Event(G2P5Event.NEW_PHOTO, "new_photo", path));
	}

	private void invokeEventMask(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_MASK, "mask", cad));
	}
	
	private void invokeEventFocus(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_FOCUS, "mask", cad));
	}
	
	private void invokeEventNoFocus(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_NO_FOCUS, "mask", cad));
	}

	private void invokeEventCode(String cad, String content) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_CODE, cad, content));
	}

	private void invokeEventPTP(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_PTP, "ptp", cad));
	}

	private void invokeEventCamera(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_CAMERA, "camera", cad));
	}

	private void invokeEventButton(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_BUTTON, "button", cad));
	}

	private void invokeEventExposure(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_EXPOSURE, "exposure", cad));
	}

	private void sendEvent(G2P5Event event) {

		System.out.println("NEW EVENT " + id + " " + event.eventCode + " " + event.content);

		event.g2p5 = this;

		if (listener != null) {
			listener.newEvent(event);
		}

		events.add(event);

		if (mock)
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public static void killAllGphotoProcess() {
		String commandToRun = "ps aux | grep -ie gphoto | awk '{print $2}' | xargs kill -9";
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				PApplet.println("killAllGphotoProcess process : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void killAllProcessByName(String name) {
		String commandToRun = "ps aux | grep -ie " + name + " | awk '{print $2}' | xargs kill -9";
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				PApplet.println("killAllGphotoProcess process : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void processLogLine(String line) {

		System.out.println(line);
		if (line.contains("Camera")) {
			int index = line.indexOf("Camera");
			String cad = line.substring(index, line.length());
			invokeEventCamera(cad);

		} else if (line.contains("PTP")) {
			// UNKNOWN PTP Property d1d3 changed
			int index = line.indexOf("PTP");
			String cad = line.substring(index, line.length());
			invokeEventPTP(cad);

		} else if (line.contains("Button")) {
			// UNKNOWN Button 1032
			int index = line.indexOf("Button");
			String cad = line.substring(index+6, line.length());
			invokeEventButton(cad);

		} else if (line.contains("OLCInfo")) {
			// UNKNOWN OLCInfo event 0x0800 content 0000000000000000
			// UNKNOWN OLCInfo event mask=900
			if (line.contains("OLCInfo event")) {
				if (line.contains("0x")) {
					int index = line.indexOf("0x");
					int indexContent = line.indexOf("content");
					String cad = line.substring(index, indexContent - 1);
					String content = line.substring(indexContent + 8, line.length());
					invokeEventCode(cad, content);
				} else if (line.contains("mask")) {
					int index = line.indexOf("mask=");
					String cad = line.substring(index + 5, line.length());
					if (cad.startsWith("2")) {
						//focus things
						if(cad.equals("200")) {
							invokeEventFocus(cad);
						}else {
							invokeEventNoFocus(cad);
						}
						
					} else {
						invokeEventMask(cad);
					}
				}
			} else if (line.contains("OLCInfo exposure")) {
				int index = line.indexOf("exposure indicator");
				String cad = line.substring(index + 18, line.length());
				invokeEventExposure(cad);
			} else {
				// aquí los no reconocidos
			}

		} else if (line.contains(id + ".cr2")) {
			// something about the file
			if (!line.contains("LANG=C")) {
				try {
					if (active) {
						Thread.sleep(600);
						int index = line.lastIndexOf(" ");
						String cad = line.substring(index + 1, line.length());
						invokePhotoEvent(cad);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			} else if (line.contains("LANG=C")) {
				PApplet.println("Problem opening thethering on camera " + id);
				setAction(CAMERA_INACTIVE);
			}
		}
	}

	public static G2P5 create(String homeDirectory, String eosSerial, String id) {
		String port = getCameraPort(eosSerial);
		G2P5 camera = new G2P5(homeDirectory, eosSerial, port, id);
		return camera;
	}

	public static String getCameraPort(String eosSerial) {

		ArrayList<String> portList = new ArrayList<String>();

		String commandToRun = "gphoto2 --auto-detect";
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains("usb:")) {
					int index = line.indexOf("usb:");
					String newPort = line.substring(index, index + 11);
					portList.add(newPort);
				}
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		for (int i = 0; i < portList.size(); i++) {
			commandToRun = "gphoto2 --port " + portList.get(i) + " --get-config /main/status/eosserialnumber";
			PApplet.println(commandToRun);
			try {
				String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
				Process process = new ProcessBuilder(commands).start();
				InputStream inputStream = process.getInputStream();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					PApplet.println(line);
					if (line.contains("Current: ")) {
						int index = line.indexOf("Current: ");
						String serial = line.substring(index + 9, line.length());
						if (serial.equals(eosSerial)) {
							inputStream.close();
							bufferedReader.close();
							return portList.get(i);
						}
					}
				}
				inputStream.close();
				bufferedReader.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return null;
	}

	public String getFilePath() {
		String fullPath = homeDirectory + "/" + id + ".cr2";
		return fullPath;
	}

	/*
	 * if (line.contains(g2p5.id + ".cr2") && !line.contains("LANG=C")) {
	 * 
	 * try { if (g2p5.active) { Thread.sleep(600); g2p5.invokePhotoEvent(); } }
	 * catch (Throwable t) { PApplet.println(t); } } else if
	 * (line.contains("LANG=C")) {
	 * PApplet.println("Problem opening thethering on camera " + g2p5.id);
	 * inputStream.close(); bufferedReader.close();
	 * g2p5.setAction(g2p5.CAMERA_INACTIVE); }
	 */

}
