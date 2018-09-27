import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import processing.core.PApplet;

public class TetheredCaptureRunnable implements RunnableTetheredInterface{

	G2P5 g2p5;
	
	Thread thread;
	
	@Override
	public void run() {

		System.setOut(new TracingPrintStream(System.out));

		String fullPath = g2p5.getFilePath();
		String commandToRun = "stdbuf -oL  gphoto2 --capture-tethered --port " + g2p5.port + " --force-overwrite --filename " + fullPath;
		PApplet.println(commandToRun);

		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// PApplet.println(port + " Tethered message : " + line);
				g2p5.processLogLine(line);
			}

			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	@Override
	public void doTriggerEvent(boolean process) {
		
	}

}
