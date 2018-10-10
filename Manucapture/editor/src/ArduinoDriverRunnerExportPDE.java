

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import processing.core.PApplet;

public class ArduinoDriverRunnerExportPDE {

	ArduinoDriverRunnerExportPDE(){
		killAllArduinoDrivers();
	}

	private void killAllArduinoDrivers() {
		PApplet.println("Killing other existing instances of Arduino driver");
		String commandToRun = "ps aux | grep -ie ArduinoDriver | awk '{print $2}' | xargs kill -9";
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandToRun };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				PApplet.println("killAllArduinoDrivers process : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}	
	
public static void main(String[] args) {
		
		String homeUSer = System.getProperty("user.home");
		
		Process process;
		try {
			
			process = Runtime.getRuntime()
					.exec(homeUSer+"/git/bookScanner/Manucapture/ArduinoDriver/application.linux64/ArduinoDriver");

			Thread.sleep(6000);
			
			InputStream error = process.getErrorStream();
			String err = "Error:";
			for (int i = 0; i < error.available(); i++) {
				err += (char) error.read();
			}
			System.out.println(err);

			InputStream out = process.getInputStream();
			
			String info = "info:";
			for (int i = 0; i < out.available(); i++) {
				info += (char) out.read();
			}
			System.out.println(info);
			
			process.waitFor();

		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
