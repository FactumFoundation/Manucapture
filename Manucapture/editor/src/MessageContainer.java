import java.util.HashMap;
import java.util.Map;

public class MessageContainer {

	Map<String, String> content = new HashMap<>();

	public void init() {
		content.put("factum.name", "Factum Foundation");
		content.put("sw.version", "Version 2.0");
		content.put("sw.name", "MANUCAPTURE");
		content.put("sw.newproject", "NEW PROJECT");
		content.put("sw.openproject", "LOAD PROJECT");
		content.put("sw.lastproject", "LOAD PREVIOUS");

		content.put("sw.nocamera", "The cameras was not detected, please connect, turn on and restart de application");
		content.put("sw.liveviewenable", "LIVEVIEW MODE ENABLED");

		content.put("sw.calibration1", "CAPTURING CHART COLOR\n PLEASE OPEN THE MANUSCRIPT AT THE END,\n AND PUT THE CHART COLOR IN THIS SIDE");
		content.put("sw.calibration2", "DRAG POINTS TO COVER ALL DE MANUSCRIPT, AND PRESS BUTTON NORMAL");
		content.put("sw.calibration3", "CAPTURING CHART COLOR\n PLEASE OPEN THE MANUSCRIPT AT THE START,\n AND PUT THE CHART COLOR IN THIS SIDE");

		content.put("sw.errorloadingproject", "Can't load project");

		content.put("sw.rotationAChanged", "Rotation A in serials has changed ");
		content.put("sw.rotationBChanged", "Rotation B in serials has changed ");
		content.put("sw.serialAChanged", "Serial A in serials has changed ");
		content.put("sw.serialBChanged", "Serial B in serials has changed ");

		content.put("sw.notconnected", "Can't capture, cameras are not connected, check connection and camera state");
		content.put("sw.notready", "Can't Trigger, cameras are not ready");
		content.put("sw.noeventA", "Camera A Fails, no event after action");
		content.put("sw.noeventB", "Camera B Fails, no event after action");

		content.put("sw.fails", "Camera A And B Fails");
		content.put("sw.failsA", "Camera A Fails");
		content.put("sw.failsB", "Camera B Fails");

		content.put("sw.", "");

	}

	public String getText(String key) {
		return content.get(key);
	}

}
