import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FilenameUtils;

import processing.core.PApplet;
import processing.core.PImage;

public class Item {

	public static String TYPE_ITEM = "Item";
	public static String TYPE_CHART = "Chart";
	public static String TYPE_BACKGROUND = "background";

	MImage mImageLeft = new MImage();
	MImage mImageRight = new MImage();
	float pagNum;
	String comment;
	String type;

	ManuCaptureContext context;

	public Item(ManuCaptureContext context, String imagePathLeft, String imagePathRight, float pagNum, String comment,
			String type) {

		this.mImageLeft.imagePath = imagePathLeft;
		this.mImageRight.imagePath = imagePathRight;
		this.pagNum = pagNum;
		this.comment = comment;
		this.type = type;

		this.context = context;
	}

	void clear() {

		comment = "";
		mImageLeft.clear();
		mImageRight.clear();
	}

	void loadThumbnails(Project project) {
		loadTumbnails(project, mImageLeft);
		loadTumbnails(project, mImageRight);
	}

	private void loadTumbnails(Project project, MImage mImage) {
		if (mImage.imagePath != null && !mImage.imagePath.equals("")) {
			File itemImg = new File(project.projectDirectory + "/" + mImage.imagePath);

			if (itemImg.exists()) {
				String thumbnailPath = context.thumbnail.getThumbnailPath(project.projectDirectory, itemImg);
				context.parent.println("itemImage " + thumbnailPath);
				File thumbFile = new File(thumbnailPath);
				if (!thumbFile.exists()) {
					mImage.imgThumb = context.thumbnail.generateThumbnail(context, itemImg, false);
				} else {
					PImage thumbImg = context.parent.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(context.thumbnail.thumbMargin, 0,
							thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
					mImage.imgThumb = thumbImg;
				}
			} else {
				PApplet.println("item ERROR", itemImg.getPath(), "image not found");
			}
		}
	}

}