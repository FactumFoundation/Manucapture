import processing.core.PApplet;

public class Canon700D_G2P5 extends G2P5{

	public Canon700D_G2P5() {
		super();
	}

	public Canon700D_G2P5(String homeDirectory, String eosSerial, String port, String id) {
		super(homeDirectory, eosSerial, port, id);
	}

	public void processLogLine(String line) {
		if (line.contains("Camera")) {
			int index = line.indexOf("Camera");
			String cad = line.substring(index, line.length());
			invokeEventCamera(cad);
		} else if (line.contains("PTP")) {
			// UNKNOWN PTP Property d1d3 changed
			int index = line.indexOf("PTP");
			String cad = line.substring(index, line.length());
			invokeEventPTP(cad);
		} else if (line.contains("Button")) {
			// UNKNOWN Button 1032
			int index = line.indexOf("Button");
			String cad = line.substring(index + 6, line.length());
			invokeEventButton(cad);
		} else if (line.contains("OLCInfo")) {
			// UNKNOWN OLCInfo event 0x0800 content 0000000000000000
			// UNKNOWN OLCInfo event mask=900
			if (line.contains("OLCInfo event")) {
				if (line.contains("0x")) {
					int index = line.indexOf("0x");
					int indexContent = line.indexOf("content");
					String cad = line.substring(index, indexContent - 1);
					String content = line.substring(indexContent + 8, line.length());
					invokeEventCode(cad, content);
				} else if (line.contains("mask")) {
					int index = line.indexOf("mask=");
					String cad = line.substring(index + 5, line.length());
					invokeEventMask(cad);
				}
			} else if (line.contains("OLCInfo exposure")) {
				int index = line.indexOf("exposure indicator");
				String cad = line.substring(index + 18, line.length());
				invokeEventExposure(cad);
			} else {
				// aquí los no reconocidos
			}
		} else if (line.contains(".cr2") && line.contains(id)) {
			// something about the file
			if (!line.contains("LANG=C")) {
				try {
					if (active) {
						Thread.sleep(600);
						int index = line.lastIndexOf(" ");
						String cad = line.substring(index + 1, line.length());
						invokePhotoEvent(cad);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			} else if (line.contains("LANG=C")) {
				PApplet.println("Problem opening thethering on camera " + id);
				setAction(CAMERA_INACTIVE);
			}
		}
	}

	public static G2P5 create(String homeDirectory, String eosSerial, String id) {
		String port = getCameraPort(eosSerial);
		G2P5 camera = new Canon700D_G2P5(homeDirectory, eosSerial, port, id);
		return camera;
	}

}
