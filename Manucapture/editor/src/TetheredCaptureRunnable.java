import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import processing.core.PApplet;

public class TetheredCaptureRunnable implements Runnable{

	G2P5 g2p5;
	
	Thread thread;
	
	@Override
	public void run() {

		System.setOut(new TracingPrintStream(System.out));

		String fullPath =  g2p5.homeDirectory + "/" + g2p5.id + ".cr2";
		String commandToRun = "gphoto2 --capture-tethered --port " + g2p5.port + " --force-overwrite --filename " + fullPath;
		PApplet.println(commandToRun);

		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// PApplet.println(port + " Tethered message : " + line);
				if (line.contains(g2p5.id + ".cr2") && !line.contains("LANG=C")) {

					try {
						if (g2p5.active) {
							Thread.sleep(600);
							g2p5.invokePhotoEvent();
						}
					} catch (Throwable t) {
						PApplet.println(t);
					}
				} else if (line.contains("LANG=C")) {
					PApplet.println("Problem opening thethering on camera " + g2p5.id);
					inputStream.close();
					bufferedReader.close();
					g2p5.setAction(g2p5.CAMERA_INACTIVE);
				}
			}

			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
//		return true;

	}

	
	
}
