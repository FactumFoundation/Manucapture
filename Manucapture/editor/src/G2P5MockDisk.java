import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class G2P5MockDisk extends G2P5 {

	int index = 0;

	ManuCaptureContext context;
	
	String nameDataSetPath = "dataset2";

	public G2P5MockDisk() {
		// TODO Auto-generated constructor stub
	}

	public G2P5MockDisk(ManuCapture_v1_1 parent, String eosSerial, String port, String id) {
		super.parent = parent;
		this.id = id;
		this.context = parent.context;
	}

	@Override
	public boolean capture() {
		String fullPath = parent.homeDirectory();

		String datasetPath = context.appPath + "/"+nameDataSetPath+"/" + index + "/";
		File file = new File(datasetPath);

		for (File tempFile : file.listFiles()) {
			if (tempFile.getName().contains("_" + id + "_"))
				targetFileName = tempFile.getName();
		}

		String commandGenerate = "cp " + datasetPath + targetFileName + " " + getFullTargetPath();
		context.parent.println(commandGenerate);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandGenerate };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// println("InputStreamReader : " + line);
			}
			inputStream.close();
			bufferedReader.close();
			invokePhotoEvent();
			
			if (index < file.getParentFile().listFiles().length-1)
				index++;
			else
				index = 0;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public String getFullTargetPath() {

		fullTargetPath = folderPath + "/" + targetFileName;
		return fullTargetPath;
		// return super.getFullTargetPath();
	}

	public static G2P5 create(ManuCapture_v1_1 parent, String eosSerial, String id) {
		G2P5 camera = new G2P5MockDisk(parent, eosSerial, null, id);
		return camera;
	}

	@Override
	public boolean isConnected() {
		return true;
	}

}
