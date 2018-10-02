import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExecCommand {
	private Semaphore outputSem;
	private String output;
	private Semaphore errorSem;
	private String error;
	private Process p;

	private class InputWriter extends Thread {
		private String input;

		public InputWriter(String input) {
			this.input = input;
		}

		public void run() {
			PrintWriter pw = new PrintWriter(p.getOutputStream());
			pw.println(input);
			pw.flush();
		}
	}

	private class OutputReader extends Thread {
		public OutputReader() {
			try {
				outputSem = new Semaphore(1);
				outputSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String buff = new String();
				// try {
				while (true) {
					// if (p.getInputStream().available() > 0) {
					buff = isr.readLine();
					if (buff != null) {
						readBuffer.append(buff);
						System.out.println(buff + " ::::::::::::::." + p.getInputStream().available());
					} else {
						break;
					}
					// } else {

					// Thread.sleep(5);
					//
					// }

				}
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				output = readBuffer.toString();
				outputSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ErrorReader extends Thread {
		public ErrorReader() {
			try {
				errorSem = new Semaphore(1);
				errorSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				StringBuffer readBuffer = new StringBuffer();
				BufferedReader isr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String buff = new String();
				while ((buff = isr.readLine()) != null) {
					readBuffer.append(buff);
				}
				error = readBuffer.toString();
				errorSem.release();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (error.length() > 0)
				System.out.println(error);
		}
	}

	public ExecCommand(String command, String input) {
		try {
			p = Runtime.getRuntime().exec(makeArray(command));
			new InputWriter(input).start();
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ExecCommand(String command) {
		try {
			p = Runtime.getRuntime().exec(makeArray(command));
			new OutputReader().start();
			new ErrorReader().start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String getOutput() {
		try {
			outputSem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String value = output;
		outputSem.release();
		return value;
	}

	public String getError() {
		try {
			errorSem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String value = error;
		errorSem.release();
		return value;
	}

	private String[] makeArray(String command) {
		ArrayList<String> commandArray = new ArrayList<String>();
		String buff = "";
		boolean lookForEnd = false;
		for (int i = 0; i < command.length(); i++) {
			if (lookForEnd) {
				if (command.charAt(i) == '\"') {
					if (buff.length() > 0)
						commandArray.add(buff);
					buff = "";
					lookForEnd = false;
				} else {
					buff += command.charAt(i);
				}
			} else {
				if (command.charAt(i) == '\"') {
					lookForEnd = true;
				} else if (command.charAt(i) == ' ') {
					if (buff.length() > 0)
						commandArray.add(buff);
					buff = "";
				} else {
					buff += command.charAt(i);
				}
			}
		}
		if (buff.length() > 0)
			commandArray.add(buff);

		String[] array = new String[commandArray.size()];
		for (int i = 0; i < commandArray.size(); i++) {
			array[i] = commandArray.get(i);
		}

		return array;
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