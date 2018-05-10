import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FilenameUtils;

import processing.core.PApplet;
import processing.core.PImage;

public class Thumbnail {

	
	PApplet parent;


	
	int thumbMargin = 6;
	
	public PImage generateThumbnail(ManuCaptureContext context,File rawImgFile, boolean rightImg) {

		String thumbnailPath = getThumbnailPath(context.projectDirectory,rawImgFile);
		String commandGenerate = "exiftool -b -ThumbnailImage " + rawImgFile.getPath() + " > " + thumbnailPath;
		parent.println(commandGenerate);
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
		} catch (Exception e) {
			e.printStackTrace();
			parent.print("ERROR generating thumbnail!");
			return null;
		}

		int angle = 270;
		boolean mirrorThumbnail = false;
		if (rightImg)
			angle = 270;
		else
			angle = 90;

		String commandRotate;
		if (mirrorThumbnail) {
			commandRotate = "convert -rotate " + angle + " -flop " + thumbnailPath + " " + thumbnailPath;
		} else {
			commandRotate = "convert -rotate " + angle + " " + thumbnailPath + " " + thumbnailPath;
		}
		parent.println(commandRotate);
		try {
			String[] commands = new String[] { "/bin/sh", "-c", commandRotate };
			Process process = new ProcessBuilder(commands).start();
			InputStream inputStream = process.getInputStream();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				// println("InputStreamReader : " + line);
			}
			inputStream.close();
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		parent.delay(100);
		PImage thumbImg = parent.loadImage(thumbnailPath);
		if (rightImg) {
			thumbImg = thumbImg.get(0, 0, thumbImg.width - thumbMargin, thumbImg.height);
		} else {
			thumbImg = thumbImg.get(thumbMargin, 0, thumbImg.width - thumbMargin, thumbImg.height);
		}

		context.parent.println("Thumbnail Generated : " + thumbnailPath);
		return thumbImg;
	}

	
	/*
	 *  Thumbnail processing
	 * 
	 */
	
	
	public String getThumbnailPath(String projectDirectory,File imageFile){
		String rawImgName = FilenameUtils.removeExtension(imageFile.getName());
		return projectDirectory + "/thumbnails/" + rawImgName + "_thumb.jpg";
	}

	

}
