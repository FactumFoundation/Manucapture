import java.util.ArrayList;
import java.util.List;

import processing.core.PImage;

public class MImage {

	String imagePath;
	String thumbPath;

	PImage imgThumb;
	PImage imgPreview;

	List<HotArea> mesh = new ArrayList<>();

	void clear() {
		imagePath = "";
		thumbPath = "";
		imgThumb = null;
		mesh = new ArrayList<>();
	}

	public List<HotArea> copyMesh(List<HotArea> defaultValue) {

		if (mesh.isEmpty()) {
			return defaultValue;
		} else {
			List<HotArea> temp = new ArrayList<>();
			for (int i = 0; i < mesh.size(); i++) {
				HotArea ha = mesh.get(i);
				temp.add(new HotArea(ha.pos.copy(), ha.translatePos.copy(), ha.id, ha.threshold, ha.name));
			}

			return temp;
		}
	}

}
