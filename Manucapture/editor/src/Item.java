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

		this.pagNum = pagNum;
		this.comment = comment;
		this.type = type;

		this.context = context;

		mImageLeft.rotation = context.rotB;
		mImageRight.rotation = context.rotA;

		mImageLeft.g2p5Adapter = context.gphotoBAdapter;
		mImageRight.g2p5Adapter = context.gphotoAAdapter;

		this.mImageLeft.imagePath = imagePathLeft;
		this.mImageRight.imagePath = imagePathRight;
		this.mImageLeft.mesh = context.copyMesh(context.pointsLeft);
		this.mImageRight.mesh = context.copyMesh(context.pointsRight);
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

	public void loadRightPreview(String projectDirectory, String nextRightImagePath) {
		mImageRight.loadPreview(projectDirectory + "/preview_right/", nextRightImagePath, "right_preview.jpg");
	}

	public void loadLeftPreview(String projectDirectory, String leftImagePath) {
		mImageLeft.loadPreview(projectDirectory + "/preview_left/", leftImagePath, "left_preview.jpg");
	}

	void loadPreviews(String projectDirectory, String leftImagePath, String rightImagePath) {

		long startMillis = context.parent.millis();

		System.out.println("start preview");

		// nextRightImagePath = rightImagePath;
		loadRightPreview(projectDirectory, rightImagePath);
		System.out.println("end preview left");
		// nextLeftImagePath = leftImagePath;
		loadLeftPreview(projectDirectory, leftImagePath);

		System.out.println("end preview rigth");

		long endMillis = context.parent.millis();
	}

}