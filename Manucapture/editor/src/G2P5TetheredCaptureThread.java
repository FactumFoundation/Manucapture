import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import processing.core.PApplet;

class G2P5TetheredCaptureThread extends Thread {

	G2P5 g2p5;

	public void run() {

//		String fullPath = g2p5.homeDirectory + "/" + g2p5.id + ".cr2";

		while (true) {
			if (g2p5.getAction() == G2P5.CAMERA_IDLE) {
				try {
//					g2p5.captureTetheredLoop();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
			if (g2p5.getAction() == G2P5.CAMERA_CAPTURE) {

				// String commandToRun = "gphoto2 --port " + g2p5.port
				// + " --capture-image-and-download --force-overwrite --filename " +
				// g2p5.getFullTargetPath();
				// PApplet.println(commandToRun);
				// try {
				// String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
				// Process process = new ProcessBuilder(commands).start();
				// InputStream inputStream = process.getInputStream();
				// BufferedReader bufferedReader = new BufferedReader(new
				// InputStreamReader(inputStream), 1);
				// process.waitFor();
				// String line;
				// while ((line = bufferedReader.readLine()) != null) {
				// // PApplet.println(port + " Capture message : " + line);
				// }
				// g2p5.addImageCount();
				// g2p5.setFullTargetPath();
				// commandToRun = "mv " + fullPath + " " + g2p5.getFullTargetPath();
				// PApplet.println(commandToRun);
				// try {
				// String[] cmds = new String[] { "/bin/sh", "-c", commandToRun };
				// Process p = new ProcessBuilder(cmds).start();
				// InputStream iStream = p.getInputStream();
				// BufferedReader bReader = new BufferedReader(new InputStreamReader(iStream),
				// 1);
				// String moveline;
				// while ((moveline = bReader.readLine()) != null) {
				// PApplet.println(g2p5.port + "MV message : " + moveline);
				// }
				//
				// } catch (Exception e) {
				// e.printStackTrace();
				// } finally {
				// if (inputStream != null)
				// inputStream.close();
				// if (bufferedReader != null)
				// bufferedReader.close();
				// }
				// } catch (Exception e) {
				// e.printStackTrace();
				// break;
				// }
			}
			// if (g2p5.getAction() == G2P5.CAMERA_INACTIVE) {
			// break;
			// }
		}
		PApplet.println("End of thread");
	}

}