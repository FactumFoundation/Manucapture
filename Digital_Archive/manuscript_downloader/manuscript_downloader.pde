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

// Settings:
String sshHost = "192.168.2.201";
String sshPort = "50022";
String sshUser = "user";
String sshPass = "1234asdf$";
String localPath = "/home/factum/0_Book_Scans";

String rawTherapeeCommand = "/home/factum/RawTherapee_5.4/RawTherapee-releases-5.4.AppImage -R /home/factum/0_Book_Scans/1001/raw/";

boolean doDownload = false;

int APP_IDLE_STATE = 0; 
int APP_DOWNLOADING_STATE = 1;
int appState = APP_IDLE_STATE;

OscP5 oscP5;
NetAddress uploaderLocation;
Process uProcess;

public void setup(){
  
  size(800, 480, JAVA2D);
  createGUI();
  customGUI();

  // Interprocess communication via OSC (Python server, manucapture clients)
  oscP5 = new OscP5(this,5005);
  uploaderLocation = new NetAddress("127.0.0.1",5008);

  loadSettings();
  loadLastSessionState();
  initFolders();
  dlFolder.setItems(folders, selectedFolderIndex);
  dlSubfolder.setItems(subfolders,selectedSubfolderIndex);

}

public void draw(){
  background(0xC6C4C4); 
  if(appState==APP_IDLE_STATE){
    if(doDownload){
        appState=APP_DOWNLOADING_STATE;
        thread("downloadProcess");
    }    
  } else if(appState==APP_DOWNLOADING_STATE) {
    if(!doDownload){
      appState=APP_IDLE_STATE;
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
    btnStop = new GImageButton(this, 430, 170, 40, 40, new String[] { "stop_2.png", "stop_1.png", "stop_0.png" } );

  */

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

void scanSelected(File selection){
  txfdScan.setText(selection.getAbsolutePath());
}

void loadLastSessionState(){
  XML sessionState = loadXML("lastSession.xml");  
}

// Required settings: 
// Base download folder
void loadSettings(){
  
  XML settings = loadXML("settings.xml");  
  XML connectionXML = settings.getChild("Connection");
  sshHost = connectionXML.getString("host");
  sshPort = connectionXML.getString("port");
  sshUser = connectionXML.getString("user");
  sshPass = connectionXML.getString("pass");
  XML localFolderXML = settings.getChild("Local_Folder");
  localPath = localFolderXML.getString("path");
  println("-- Settings loaded ------------");
  println("host:",sshHost);
  println("port:",sshPort);
  println("user:",sshUser);
  println("pass:",sshPass);
  println("local path:",localPath);
  println("------------------------------");

}

void downloadProcess() {
  println("Start Download process!!!");
  String commandToRun = "python " + sketchPath() +  "scripts/download_project_info.py 213 131 13";
  PApplet.println(commandToRun);
  try {
    String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
    uProcess = new ProcessBuilder(commands).start();
    uProcess.waitFor();
  } catch (Exception ioe) {
    ioe.printStackTrace();
  }  
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
