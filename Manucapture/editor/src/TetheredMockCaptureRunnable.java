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

	private static String NEW_PHOTO_PHRASE = "Saving file as /home";

	private List<String> dataset = new ArrayList<>();
	private int indexDataSet = 0;
	public String pathDataSet = "/home/factum/Escritorio/024/";

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

	private void loadDataSet() {

	}

	@Override
	public void run() {

		System.setOut(new TracingPrintStream(System.out));

		File file = new File("src/data/mocktethered.txt");

		if (file.exists()) {
			PApplet.println("MOCK MODE ENABLED reading file " + file);
		} else {
			PApplet.println("ERROR LEYENDO EL FICHERO");
		}

		String[] logs = null;
		try {
			logs = readLines(file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// load dataset
		File file2 = new File(pathDataSet);
		if (file2.exists()) {
			String[] files = file2.list();
			for (int i = 0; i < files.length; i++) {
				String fileName = files[i];
				if (fileName.contains(g2p5.id)) {
					// then is my file
					dataset.add(fileName);
				}
			}
		}

		int indexLogs = 0;

		String line = null;
		while (true) {
			line = logs[indexLogs];
			
			// first modify the id with id of this thread, normally two A and B
			line = line.replaceAll("/A.cr2", "/" + g2p5.id + ".cr2");

			// now we want to iterate over a dataset
			// when new photo is coming, we modify the path to the dataset
			if (line.startsWith(NEW_PHOTO_PHRASE)) {
				// we have new photo

				int index = line.indexOf("/home");
				String temp = line.substring(0, index);
				line = temp + pathDataSet+dataset.get(indexDataSet);
				indexDataSet++;

				// rewind
				if (indexDataSet >= dataset.size()) {
					indexDataSet = 0;
				}

			}
			
			 PApplet.println(g2p5.id + " Tethered message : " + line);

			g2p5.processLogLine(line);
			indexLogs++;
			if (indexLogs >= logs.length) {
				indexLogs = 0;
			}
			try {
				Thread.sleep(50);
				if (logs[indexLogs].trim().equals("")) {
					Thread.sleep(2000);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}


		// return true;

	}

	public static void main(String[] args) {
		TetheredMockCaptureRunnable captureRunnable = new TetheredMockCaptureRunnable();
		captureRunnable.g2p5 = new G2P5();
		captureRunnable.g2p5.id = ManuCapture_v1_1.PAGE_LEFT_NAME;
		Thread thread = new Thread(captureRunnable);
		thread.start();
	}

}
