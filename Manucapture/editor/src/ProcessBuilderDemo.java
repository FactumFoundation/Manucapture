
// Java code illustrating start() method
import java.lang.*;
import java.io.*;
import java.util.*;

class ProcessBuilderDemo {
	public static void main(String[] arg) throws IOException {
		// creating list of commands
		// List<String> commands = new ArrayList<String>();
		// commands.add("-l"); // command
		// commands.add("/Users/abhishekverma"); //command in Mac OS
		String[] commands = new String[] { "/bin/sh", "-i",
				"gphoto2 --capture-tethered --port usb:002,011 --force-overwrite --filename /home/veronica1/.manucapture/A.cr2" };
		// creating the process
		ProcessBuilder pb = new ProcessBuilder(commands);

		// startinf the process
		Process process = pb.start();

		// for reading the ouput from stream
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}
		
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ExecCommand command = new ExecCommand(
		// "gphoto2 --capture-tethered --port usb:002,011 --force-overwrite
		// --filename /home/veronica1/.manucapture/A.cr2");
	}
}