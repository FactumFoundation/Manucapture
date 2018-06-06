import java.io.File;
import java.util.ArrayList;
import java.util.List;

import processing.core.PApplet;
import processing.core.PImage;

public class MImage {

	String imagePath;
	String thumbPath;

	PImage imgThumb;
	PImage imgPreview;
	ManuCaptureContext context;

	List<HotArea> mesh = new ArrayList<>();

	void remove() {
		// imagePath = "";
		// thumbPath = "";
		imgThumb = null;
		mesh = new ArrayList<>();
		String pathI = context.project.projectDirectory + "/" + imagePath;
		new File(pathI).delete();
		String pathT =  thumbPath;
		new File(pathT).delete();
		// PReview
		// Metadata
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

	public void loadTumbnail() {
		if (imagePath != null && !imagePath.equals("")) {
			File itemImg = new File(context.project.projectDirectory + "/" + imagePath);

			if (itemImg.exists()) {
				String thumbnailPath = context.thumbnail.getThumbnailPath(context.project.projectDirectory, itemImg);
				context.parent.println("itemImage " + thumbnailPath);
				File thumbFile = new File(thumbnailPath);
				thumbPath = thumbnailPath;
				if (!thumbFile.exists()) {
					imgThumb = context.thumbnail.generateThumbnail(context, itemImg, false);
				} else {
					PImage thumbImg = context.parent.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(context.thumbnail.thumbMargin, 0,
							thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
					imgThumb = thumbImg;
				}
			} else {
				PApplet.println("item ERROR", itemImg.getPath(), "image not found");
			}
		}
	}

	public void saveMetadata() {

	}

	public void loadMetadata() {

	}

}
