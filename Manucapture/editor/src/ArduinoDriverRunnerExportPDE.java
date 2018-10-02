

import java.io.IOException;
import java.io.InputStream;

public class ArduinoDriverRunnerExportPDE {

	
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
