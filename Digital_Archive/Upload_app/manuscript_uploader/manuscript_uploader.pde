// Need G4P library
import g4p_controls.*;
import oscP5.*;
import netP5.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


XML archiveXML;
String[] folders;
String[] subfolders;
int selectedFolderIndex = 0;
int selectedSubfolderIndex = 0;

String ftpHost = "192.168.2.201";
String ftpUser = "factum";
String ftpPass = "1234asdf$";

boolean doUpload = false;

int APP_IDLE_STATE = 0; 
int APP_UPLOADING_STATE = 1;
int appState = APP_IDLE_STATE;

OscP5 oscP5;
NetAddress uploaderLocation;
Process uProcess;

public void setup(){
  
  size(480, 480, JAVA2D);
  createGUI();
  customGUI();
  // Place your setup code here
  oscP5 = new OscP5(this,5005);
  uploaderLocation = new NetAddress("127.0.0.1",5008);
  loadSettings();
  loadLastSessionState();
  initFolders();
  dlFolder.setItems(folders, selectedFolderIndex);
  dlSubfolder.setItems(subfolders,selectedSubfolderIndex);

}

public void draw(){
  background(230); 
  if(appState==APP_IDLE_STATE){
    if(doUpload){
        appState=APP_UPLOADING_STATE;
        enableUploadControlButtons(true);
        thread("uploadProcess");
    }    
  } else if(appState==APP_UPLOADING_STATE) {
    if(!doUpload){
      appState=APP_IDLE_STATE;
      enableUploadControlButtons(false);
      
    }
  }
}

// Use this method to add additional statements
// to customise the GUI controls
public void customGUI(){
  
  // Image buttons:
  /*
  btnPlayPause = new GImageToggleButton(this, 380, 170,"play_pause_2.png", "play_pause_1.png",2,1);
  btnPlayPause.addEventHandler(this, "btnPlayPause_click");
  btnStop = new GImageButton(this, 430, 170, 40, 40, new String[] { "stop_2.png", "stop_1.png", "stop_0.png" } );
  btnStop.addEventHandler(this, "btnStop_click");
  */
  setBatchUpload(false);
  enableUploadControlButtons(false);

}

void initFolders(){
  archiveXML = loadXML("archive_folders.xml");
  XML[] foldersXML = archiveXML.getChildren("Folder");
  folders = new String[foldersXML.length];
  for (int i = 0; i < foldersXML.length; i++) {
    folders[i] = foldersXML[i].getString("ascii_name");
  } 
  subfolders=getSubfolders(selectedSubfolderIndex);
}

String[] getSubfolders(int folderIndex){
  XML[] foldersXML = archiveXML.getChildren("Folder");
  if(folderIndex<foldersXML.length && folderIndex>=0){
    XML[] subFoldersXML = foldersXML[folderIndex].getChildren("Subfolder");
    String[] subFolders = new String[subFoldersXML.length];
    for (int i = 0; i < subFoldersXML.length; i++) {
      subFolders[i] = subFoldersXML[i].getString("ascii_name");
    }
    return subFolders;
  } else 
    return null;
}

void setBatchUpload(boolean batch){
  if(!batch){
     btnSelectScan.setEnabled(true);
     btnSelectScan.setAlpha(255);
     txfdScan.setEnabled(true);
     txfdScan.setAlpha(255);
     btnFolder.setEnabled(false);
     btnFolder.setAlpha(100);
     txfdFolder.setEnabled(false);
     txfdFolder.setAlpha(100);
  } else {
     btnSelectScan.setEnabled(false);
     btnSelectScan.setAlpha(100);
     txfdScan.setEnabled(false);
     txfdScan.setAlpha(100);
     btnFolder.setEnabled(true);
     btnFolder.setAlpha(255);
     txfdFolder.setEnabled(true);
     txfdFolder.setAlpha(255);
  }
}

void enableUploadControlButtons(boolean enable){
 if(enable){
  btnUpload.setEnabled(false);
  btnUpload.setAlpha(100);
  btnStop.setAlpha(255);
  btnStop.setEnabled(true);
 } else {
  btnUpload.setEnabled(true);
  btnUpload.setAlpha(255);
  btnStop.setAlpha(100);
  btnStop.setEnabled(false);
 }
}

void scanSelected(File selection){
  txfdScan.setText(selection.getAbsolutePath());
}

void folderSelected(File selection){
  txfdFolder.setText(selection.getAbsolutePath());
}

void loadLastSessionState(){
  XML sessionState = loadXML("lastSession.xml");
  
}

void loadSettings(){
  XML settings = loadXML("lastSession.xml");
  
}

void uploadProcess() {
  println("Start Upload!!!");
  String commandToRun = "python3.6 " + sketchPath() +  "/../scripts/upload_to_server.py 213 131 13";
  PApplet.println(commandToRun);
  try {
    String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
    uProcess = new ProcessBuilder(commands).start();
    uProcess.waitFor();
  } catch (Exception ioe) {
    ioe.printStackTrace();
  }
  doUpload = false;
}

/* incoming osc message are forwarded to the oscEvent method. */
void oscEvent(OscMessage theOscMessage) {
  /* print the address pattern and the typetag of the received OscMessage */
  print("### received an osc message.");
  print(" addrpattern: "+theOscMessage.addrPattern());
  if(theOscMessage.addrPattern().equals("/uploaded")){
    txtLog.appendText("Uploaded file\n");
  } else if(theOscMessage.addrPattern().equals("/end")){
    txtLog.appendText("Uploaded complete\n");
  } 
  println(" typetag: "+theOscMessage.typetag());
}
