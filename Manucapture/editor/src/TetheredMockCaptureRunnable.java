import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;

public class TetheredMockCaptureRunnable implements Runnable {

	G2P5 g2p5;

	Thread thread;

	public String[] readLines(String filename) throws IOException {
		FileReader fileReader = new FileReader(filename);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		List<String> lines = new ArrayList<>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			lines.add(line);
		}
		bufferedReader.close();
		return lines.toArray(new String[lines.size()]);
	}

	@Override
	public void run() {

		System.setOut(new TracingPrintStream(System.out));

		File file = new File("src/data/mocktethered.txt");

		if (file.exists()) {
			PApplet.println("hola");
		} else {
			PApplet.println("ERROR LEYENDO EL FICHERO");
		}

		String[] logs = null;
		try {
			logs = readLines(file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		int indexLogs = 0;

		String line = null;
		while (true) {
			line = logs[indexLogs];
			g2p5.processLogLine(line);
			indexLogs++;
			if (indexLogs > logs.length) {
				indexLogs = 0;
			}
			try {
				Thread.sleep(50);
				if(logs[indexLogs].trim().equals("")) {
					Thread.sleep(2000);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// PApplet.println(port + " Tethered message : " + line);

		// return true;

	}

	public static void main(String[] args) {
		TetheredMockCaptureRunnable captureRunnable = new TetheredMockCaptureRunnable();
		Thread thread = new Thread(captureRunnable);
		thread.start();
	}

}
