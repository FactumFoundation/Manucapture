import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FilenameUtils;

import processing.core.PApplet;
import processing.core.PImage;

public class Item {

	String imagePathLeft;
	String imagePathRight;
	String thumbPathLeft;
	String thumbPathRight;
	float pagNum;
	String comment;
	String type;
	PImage imgThumbLeft;
	PImage imgThumbRight;

	ManuCaptureContext context;

	public Item(ManuCaptureContext context, String imagePathLeft, String imagePathRight, float pagNum, String comment,
			String type) {
		this.imagePathLeft = imagePathLeft;
		this.imagePathRight = imagePathRight;
		this.pagNum = pagNum;
		this.comment = comment;
		this.type = type;

		this.context = context;
	}

	void clear() {
		imagePathLeft = "";
		imagePathRight = "";
		thumbPathLeft = "";
		thumbPathRight = "";
		comment = "";
		imgThumbLeft = null;
		imgThumbRight = null;
	}

	void loadThumbnails(Project project ) {
		loadThumbnails(project,imagePathLeft,imagePathRight);
	}

	void loadThumbnails(Project project, String newImagePathA, String newImagePathB) {
		if (imagePathLeft != null && !imagePathLeft.equals("")) {
			File itemImgLeft = new File(project.projectDirectory + "/" + imagePathLeft);

			if (itemImgLeft.exists()) {
				String thumbnailPath = context.thumbnail.getThumbnailPath(project.projectDirectory, itemImgLeft);
				context.parent.println("Left " + thumbnailPath);
				File thumbFile = new File(thumbnailPath);
				if (!thumbFile.exists()) {
					imgThumbLeft = context.thumbnail.generateThumbnail(context, itemImgLeft, false);
				} else {
					PImage thumbImg = context.parent.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(context.thumbnail.thumbMargin, 0,
							thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
					imgThumbLeft = thumbImg;
				}
			} else {
				PApplet.println("Left ERROR", itemImgLeft.getPath(), "image not found");
			}
		}
		if (imagePathRight != null && !imagePathRight.equals("")) {
			File itemImgRight = new File(project.projectDirectory + "/" + imagePathRight);
			if (itemImgRight.exists()) {
				String thumbnailPath = context.thumbnail.getThumbnailPath(project.projectDirectory, itemImgRight);
				File thumbFile = new File(thumbnailPath);
				if (!thumbFile.exists()) {
					imgThumbRight = context.thumbnail.generateThumbnail(context, itemImgRight, true);
				} else {
					PImage thumbImg = context.parent.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(0, 0, thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
					imgThumbRight = thumbImg;
				}
			} else {
				context.parent.println("Right ERROR", itemImgRight.getPath(), "image not found");
			}
		}
	}

}