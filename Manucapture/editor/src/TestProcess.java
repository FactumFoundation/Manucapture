import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

class StreamGobbler implements Runnable {
	private final InputStream is;
	private final PrintStream os;

	StreamGobbler(InputStream is, PrintStream os) {
		this.is = is;
		this.os = os;
	}

	public void run() {
		while (true)
			try {
				int c;
				while ((c = is.read()) != -1) {
					os.print((char) c);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} catch (IOException x) {
				// Handle error
			}
	}
}

public class TestProcess {
	public static void main(String[] args) throws IOException, InterruptedException {

		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(
				"gphoto2 --capture-tethered --port usb:002,011 --force-overwrite --filename /home/veronica1/.manucapture/A.cr2");

		// Any error message?
		Thread errorGobbler = new Thread(new StreamGobbler(proc.getErrorStream(), System.err));

		// Any output?
		Thread outputGobbler = new Thread(new StreamGobbler(proc.getInputStream(), System.out));

		errorGobbler.start();
		outputGobbler.start();

		// Any error?
		int exitVal = proc.waitFor();
		errorGobbler.join(); // Handle condition where the
		outputGobbler.join(); // process ends before the threads finish
	}
}
