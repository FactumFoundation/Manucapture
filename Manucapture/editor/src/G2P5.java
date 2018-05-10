
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
	
    private PApplet parent = null;
    private String eosSerial;
    private String port;
    private String id;
    private boolean active = false;
    private boolean tethering = false;
    private String targetFileName;
    private String folderPath;
    private int actionCode;
    
    private int  CAMERA_IDLE = 0;
    private int CAMERA_CAPTURE = 1;
    private int CAMERA_INACTIVE = -1;
    
    
    TetheredCaptureThread t;
    
    static int imageCounter;
    
    
    private G2P5(){
    	
    }
    
    private G2P5(PApplet parent, String eosSerial, String port, String id){
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
    
    private String fullTargetPath = "";
    
    public void setFullTargetPath(){
    	fullTargetPath = folderPath + "/" + targetFileName + "_" + id + "_" + getImageCount()+ ".cr2";
    }
    
    public String getFullTargetPath(){
    	return fullTargetPath;
    }
    
    
    private synchronized void setAction(int actionCode){
    	this.actionCode = actionCode;
    }
    
    private synchronized int getAction(){
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
    
    
	
	private boolean captureTetheredLoop() {

		String fullPath = parent.sketchPath() + "/data/" + id + ".cr2";
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
								commandToRun = "mv " + fullPath + " " + getFullTargetPath();
								PApplet.println(commandToRun);
								try {
									String[] cmds = new String[] { "/bin/sh", "-c", commandToRun };
									Process p = new ProcessBuilder(cmds).start();
									InputStream iStream = p.getInputStream();
									BufferedReader bReader = new BufferedReader(new InputStreamReader(iStream), 1);
									String moveline;
									while ((moveline = bReader.readLine()) != null) {
										PApplet.println(port + "MV message : " + moveline);
									}
									//Thread.sleep(500);
									invokePhotoEvent();
									iStream.close();
									bReader.close();
								} catch (IOException ioe) {
									ioe.printStackTrace();
									return false;
								}
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

	
	class TetheredCaptureThread extends Thread {

		public void run() {	
			
			while(true){
				if(getAction()==CAMERA_IDLE){
					try{
						captureTetheredLoop();
					} catch(Exception e){
						e.printStackTrace();
						break;
					}
				}
				if(getAction()==CAMERA_CAPTURE){
					
					addImageCount();
					setFullTargetPath();
					String commandToRun = "gphoto2 --port " + port
							+ " --capture-image-and-download --force-overwrite --filename " + getFullTargetPath();
					PApplet.println(commandToRun);
					try {
						String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
						Process process = new ProcessBuilder(commands).start();
						InputStream inputStream = process.getInputStream();
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
						process.waitFor();
						String line;
						while ((line = bufferedReader.readLine()) != null) {
							// PApplet.println(port + " Capture message : " + line);
						}
						invokePhotoEvent();	
						setAction(CAMERA_IDLE);
						inputStream.close();
						bufferedReader.close();
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}				
				}
				if(getAction()==CAMERA_INACTIVE){
					break;
				}
			}
			PApplet.println("End of thread");
		}
		
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

	
	private static synchronized void addImageCount(){
		imageCounter++;
	}
	
	public static synchronized void setImageCount(int imageCount){
		imageCounter = imageCount;
	}
	
	public static synchronized int getImageCount() {
	   return imageCounter;	
	}
	
    public static G2P5 create(PApplet parent, String eosSerial, String id) {
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


