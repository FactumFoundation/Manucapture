
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
 * Camera Interface
 * @author veronica1
 *
 */

public class G2P5 {
	
    protected ManuCapture_v1_1 parent = null;
    private String eosSerial;
    public String port;
    protected String id;
    private boolean active = false;
    private boolean tethering = false;
    protected String targetFileName;
    protected String folderPath;
    private int actionCode;
    
    public static int  CAMERA_IDLE = 0;
    public static int CAMERA_CAPTURE = 1;
    public static int CAMERA_INACTIVE = -1;
    
    protected String fullTargetPath = "";
    TetheredCaptureThread t;
    
    static int imageCounter;
    
    
    public G2P5(){
    	
    }
    
    public G2P5(ManuCapture_v1_1 parent, String eosSerial, String port, String id){
    	this.parent = parent;
    	this.eosSerial = eosSerial;
    	this.id = id;
    	this.port = port;
    	this.tethering = true;
    	setActive(true);
    	
    }

    public synchronized String getPort() {
        return port;
    }
    
    public synchronized String getSerial() {
        return eosSerial;
    }
 
    public  void setActive(boolean active) {
    	this.active = active;
    	if(port != null){
	    	if(active){
	    		setAction(CAMERA_IDLE);
	 			t = new TetheredCaptureThread();
	 			t.g2p5 = this;
				t.start();
	    	}
			else{
				setAction(CAMERA_INACTIVE);
				killAllProcessByName(id+".cr2");
				actionCode = CAMERA_INACTIVE;
			}
    	} else {
    		t = new TetheredCaptureThread();
    		this.active = false;
    	}
	
    }

    public boolean isConnected(){
    	return t.isAlive();
    }
    
    /*
    public synchronized boolean captureTethered(boolean on) {
    	if(active){
    		if(!on){
    			killAllProcessByName(id+".cr2");
    		} else {
   			  t = new TetheredCaptureThread();
    			  t.start();	
    		}	
    	}
    	tethering = on;
    	return false;
    }
    */
    
    
    public void setTargetFile(String folderPath, String targetFileName){
    	this.targetFileName = targetFileName;
    	this.folderPath = folderPath;
    }
    
    
    
    public void setFullTargetPath(){
    	fullTargetPath = folderPath + "/" + targetFileName + "_" + id + "_" + getImageCount()+ ".cr2";
    }
    
    public String getFullTargetPath(){
    	return fullTargetPath;
    }
    
    
    public synchronized void setAction(int actionCode){
    	this.actionCode = actionCode;
    }
    
    public synchronized int getAction(){
    	return actionCode;
    }
    
    public boolean capture() {
    	
    	
    	setAction(CAMERA_CAPTURE);
    	
    	if(active){
    		if(tethering){
    			killAllProcessByName(id+".cr2");
        		parent.delay(100);
    		}
    				
    	}
    	
    	return false;
    }
    
    public String[] getParameters() {
    	killAllProcessByName(id+".cr2");
    	return null;
    }
    
    public boolean setParameters(String iso, String shutterSpeed, String fnumber){
    	killAllProcessByName(id+".cr2");
    	return false;
    }
   
    
    public synchronized void invokePhotoEvent(){
       try {
    	 Method newPhotoEvent = parent.getClass().getMethod("newPhotoEvent", G2P5.class, String.class);
    	 newPhotoEvent.invoke(parent, this, getFullTargetPath());  		 
       } catch (Exception e) { e.printStackTrace();}
    }
    
	public boolean moveFile(String fullPath) throws IOException {
		String commandToRun;
		commandToRun = "mv " + fullPath + " " + getFullTargetPath();
		PApplet.println(commandToRun);
		InputStream iStream = null;
		BufferedReader bReader = null;
		try {
			String[] cmds = new String[] { "/bin/sh", "-c", commandToRun };
			Process p = new ProcessBuilder(cmds).start();
			iStream = p.getInputStream();
			bReader = new BufferedReader(new InputStreamReader(iStream), 1);
			String moveline;
			while ((moveline = bReader.readLine()) != null) {
				PApplet.println(port + "MV message : " + moveline);
			}
			// Thread.sleep(500);
			return true;

		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		} finally {
			if (iStream != null)
				iStream.close();
			if (bReader != null)
				bReader.close();
		}

	}
	
	public boolean captureTetheredLoop() {

		System.setOut(new TracingPrintStream(System.out));
		
		String fullPath = parent.homeDirectory()+"/" + id + ".cr2";
		String commandToRun = "gphoto2 --capture-tethered --port " + port + " --force-overwrite --filename " + fullPath;
		PApplet.println(commandToRun);

		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				//PApplet.println(port + " Tethered message : " + line);
				if ( line.contains(id+".cr2") && !line.contains("LANG=C")) {
				
					try {
						if (active) {
								Thread.sleep(600);
								addImageCount();
								setFullTargetPath();
								moveFile(fullPath);
								invokePhotoEvent();
							}
						}
					 catch (Throwable t) {
						PApplet.println(t);
					}
				} else if (line.contains("LANG=C")) {
					PApplet.println("Problem opening thethering on camera " + id);
					inputStream.close();
					bufferedReader.close();
					setAction(CAMERA_INACTIVE);
				}
			}

			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return true;
	}

	
	public static void init(int initialImageCount) {
		setImageCount(initialImageCount);
		 killAllGphotoProcess();
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

	
	public static synchronized void addImageCount(){
		imageCounter++;
	}
	
	public static synchronized void setImageCount(int imageCount){
		imageCounter = imageCount;
	}
	
	public static synchronized int getImageCount() {
	   return imageCounter;	
	}
	
    public static G2P5 create(ManuCapture_v1_1 parent, String eosSerial, String id) {
    	String port = getCameraPort(eosSerial);
    	G2P5 camera = new G2P5(parent,eosSerial,port,id);
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


