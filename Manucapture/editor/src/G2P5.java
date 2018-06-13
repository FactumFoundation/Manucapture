
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;

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

	TetheredCaptureRunnable captureRunnable;

	G2P5Listener listener;

	String homeDirectory;

	public G2P5() {

	}

	public G2P5(String homeDirectory, String eosSerial, String port, String id) {
		this.eosSerial = eosSerial;
		this.id = id;
		this.port = port;
		this.tethering = true;
		this.homeDirectory = homeDirectory;
		setActive(true);

	}

	public synchronized String getPort() {
		return port;
	}

	public synchronized String getSerial() {
		return eosSerial;
	}

	public void setActive(boolean active) {
		this.active = active;
		if (port != null) {
			if (active) {
				setAction(CAMERA_IDLE);
				TetheredCaptureRunnable captureRunnable = new TetheredCaptureRunnable();
				Thread thread = new Thread(captureRunnable);
				captureRunnable.g2p5 = this;
				captureRunnable.thread = thread;
				thread.start();
			} else {
				setAction(CAMERA_INACTIVE);
				killAllProcessByName(id + ".cr2");
				actionCode = CAMERA_INACTIVE;
			}
		} else {
			captureRunnable = new TetheredCaptureRunnable();
			this.active = false;
		}

	}

	public boolean isConnected() {
		return captureRunnable.thread != null && captureRunnable.thread.isAlive();
	}

	public synchronized void setAction(int actionCode) {
		this.actionCode = actionCode;
	}

	public synchronized int getAction() {
		return actionCode;
	}

	public boolean capture() {

		setAction(CAMERA_CAPTURE);

		if (active) {
			if (tethering) {
				killAllProcessByName(id + ".cr2");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		return false;
	}

	public String[] getParameters() {
		killAllProcessByName(id + ".cr2");
		return null;
	}

	public boolean setParameters(String iso, String shutterSpeed, String fnumber) {
		killAllProcessByName(id + ".cr2");
		return false;
	}

	public void invokePhotoEvent() {
		if (listener != null) {
			listener.newEvent(new G2P5Event(G2P5Event.NEW_PHOTO));
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

}
