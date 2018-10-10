
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import processing.core.*;

/**
 * Camera console wrapper
 * 
 * @author veronica1
 *
 */

public abstract class G2P5 {

	private String eosSerial;
	public String port;
	protected String id;
	public boolean active = false;
	private boolean tethering = false;

	private int actionCode;

	public static int CAMERA_IDLE = 0;
	public static int CAMERA_CAPTURE = 1;
	public static int CAMERA_INACTIVE = -1;

	RunnableTetheredInterface captureRunnable;
	Thread thread;
	G2P5Listener listener;
	String homeDirectory;
	List<G2P5Event> events = new ArrayList<>();
	boolean mock = false;
	boolean ignoreEventProperty = true;
	String shutterSpeedNormal = "1/30";
	String shutterSpeedLiveview = "1";

	public G2P5() {

	}

	public G2P5(String homeDirectory, String eosSerial, String port, String id) {
		this.eosSerial = eosSerial;
		this.id = id;
		this.port = port;
		this.tethering = true;
		this.homeDirectory = homeDirectory;
		
		setNormalConfig();
		
		setActive(true);
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
				this.captureRunnable = captureRunnable;
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
			} else {
				if (!mock) {
					// captureRunnable = new TetheredCaptureRunnable();
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

	protected void invokeEventMask(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_MASK, "mask", cad));
	}

	protected void invokeEventCode(String cad, String content) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_CODE, cad, content));
	}

	protected void invokeEventPTP(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_PTP, "ptp", cad));
	}

	protected void invokeEventCamera(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_CAMERA, "camera", cad));
	}

	protected void invokeEventButton(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_BUTTON, "button", cad));
	}

	protected void invokeEventExposure(String cad) {
		sendEvent(new G2P5Event(G2P5Event.EVENT_EXPOSURE, "exposure", cad));
	}

	private void sendEvent(G2P5Event event) {
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
	
	public String getLiveViewStreamPath() {
		String fullPath = homeDirectory + "/" + id + ".mjpg";
		return fullPath;	
	}

	public void setLiveViewConfig() {
		String commandToRun = "gphoto2 --port " + port + " --set-config-value /main/capturesettings/shutterspeed=0.3";
		PApplet.println(commandToRun);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// wait for end of process
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void setNormalConfig() {
		String commandToRun = "gphoto2 --port " + port + " --set-config-value /main/capturesettings/shutterspeed=1/30";
		PApplet.println(commandToRun);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// wait for end of process
				PApplet.println("Normal : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void startLiveView() {
		//gphoto2 --port usb:001,009 --capture-movie --stdout> fifo.mjpg
		String fifoPath = getLiveViewStreamPath();
		String commandToRun = "gphoto2 --port " + port + " --capture-movie --stdout> " + fifoPath ;
		PApplet.println(commandToRun);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void stopLiveView() {
		// Kill the background process
		killAllGphotoProcess();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Turn off view finder
		String commandToRun = "gphoto2 --port " + port + " --reset";
		PApplet.println(commandToRun);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// wait for end of process
				PApplet.println("Stop liveview : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Turn off view finder
		commandToRun = "gphoto2 --port " + port + " --set-config /main/actions/viewfinder=0";
		PApplet.println(commandToRun);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// wait for end of process
				PApplet.println("Stop liveview : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void doTriggerEvent() {
		if (captureRunnable != null)
			captureRunnable.doTriggerEvent(true);
	}

	abstract public void processLogLine(String line);

}
