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

}
