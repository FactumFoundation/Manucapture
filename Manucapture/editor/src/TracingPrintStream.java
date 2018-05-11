

import java.io.PrintStream;
import java.util.Calendar;

public class TracingPrintStream extends PrintStream {
	public TracingPrintStream(PrintStream original) {
		super(original);
	}

	// You'd want to override other methods too, of course.
	@Override
	public void println(String line) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		// Element 0 is getStackTrace
		// Element 1 is println
		// Element 2 is the caller
		StackTraceElement caller = stack[3];
		Calendar calendar = Calendar.getInstance();
		int y = calendar.get(Calendar.YEAR);
		int m = calendar.get(Calendar.MONTH);
		int d = calendar.get(Calendar.DAY_OF_MONTH);

		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int mi = calendar.get(Calendar.MINUTE);
		int seg = calendar.get(Calendar.SECOND);
		int millis  = calendar.get(Calendar.MILLISECOND);
		super.println(caller.getClassName() + ":"+caller.getLineNumber()+" (" + y + "/" + m + "/" + d
				+ "/ " + h + ":" + mi + ":"+ seg + "."+ millis +  ") " + line);
	}
}
