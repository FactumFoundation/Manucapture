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

	ManuCapture_v1_1 context;

	public Item(ManuCapture_v1_1 context, String imagePathLeft, String imagePathRight, float pagNum, String comment,
			String type) {

		this.pagNum = pagNum;
		this.comment = comment;
		this.type = type;

		this.context = context;

		mImageLeft.rotation = context.rotationB;
		mImageRight.rotation = context.rotationA;

		mImageLeft.g2p5Adapter = context.gphotoBAdapter;
		mImageRight.g2p5Adapter = context.gphotoAAdapter;

		this.mImageLeft.imagePath = imagePathLeft;
		this.mImageRight.imagePath = imagePathRight;
		this.mImageLeft.mesh = context.contentGUI.copyMesh(context.contentGUI.pointsLeft);
		this.mImageRight.mesh = context.contentGUI.copyMesh(context.contentGUI.pointsRight);
		this.mImageRight.context = context;
		this.mImageLeft.context = context;
	}

	void remove() {

		comment = "";
		mImageLeft.remove();
		mImageRight.remove();
	}

	void loadThumbnails() {
		mImageLeft.loadTumbnail();
		mImageRight.loadTumbnail();
	}

	public void loadMetadata() {
		try {
			this.mImageLeft.loadMetadata();
			this.mImageRight.loadMetadata();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveMetadata() {
		this.mImageLeft.saveMetadata();
		this.mImageRight.saveMetadata();
	}

	public PImage loadRightPreview(String projectDirectory, String nextRightImagePath) {
		return mImageRight.loadPreview(projectDirectory + "/preview_right/", nextRightImagePath, "right_preview.jpg");
	}

	public PImage loadLeftPreview(String projectDirectory, String leftImagePath) {
		return mImageLeft.loadPreview(projectDirectory + "/preview_left/", leftImagePath, "left_preview.jpg");
	}
/*
	void loadPreviews(String projectDirectory, String leftImagePath, String rightImagePath) {
		
		loadRightPreview(projectDirectory, rightImagePath);
		loadLeftPreview(projectDirectory, leftImagePath);
	}
*/
}