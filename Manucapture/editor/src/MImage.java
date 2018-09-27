import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;

public class MImage {

	private static final String SIDECAR_EXTENSION = ".pp3";

	String imagePath;
	String thumbPath;
	String imagePreview;
	PImage imgThumb;

	ManuCapture_v1_1 context;
	List<Guide> guides = new ArrayList<>();

	int thumbMargin = 6;
	int rotation;

	long timestamp = -1;
	G2P5ManucaptureAdapter g2p5Adapter;

	String templatePath = "/home/dudito/git/bookScanner/Manucapture/editor/src/data/template.pp3";

	String pathMock = null;

	void remove() {
		imgThumb = null;
		guides = new ArrayList<>();
		String pathI = context.project.projectDirectory + "" + imagePath;
		if (pathI != null && new File(pathI).exists())
			new File(pathI).delete();
		String pathT = thumbPath;
		if (pathT != null && new File(pathT).exists())
			new File(pathT).delete();
		if (getSidecarPath() != null && new File(getSidecarPath()).exists())
			new File(getSidecarPath()).delete();
		if (imagePreview != null && new File(imagePreview).exists()) {
			boolean deleted = new File(imagePreview).delete();
		}
	}

	public List<Guide> copyGuides(List<Guide> defaultValue) {
		if (guides.isEmpty()) {
			return defaultValue;
		} else {
			List<Guide> temp = new ArrayList<>();
			for (int i = 0; i < guides.size(); i++) {
				Guide ha = guides.get(i);
				temp.add(new Guide(ha.pos.copy(), ha.translatePos.copy(), ha.threshold, ha.name));
			}
			return temp;
		}
	}

	public void loadTumbnail() {
		if (imagePath != null && !imagePath.equals("")) {
			File itemImg = new File(context.project.projectDirectory + "/" + imagePath);
			if (itemImg.exists()) {
				String thumbnailPath = getThumbnailPath(context.project.projectDirectory, itemImg);
				context.println("itemImage " + thumbnailPath);
				File thumbFile = new File(thumbnailPath);
				thumbPath = thumbnailPath;
				if (!thumbFile.exists()) {
					imgThumb = generateThumbnail(context, itemImg);
				} else {
					PImage thumbImg = context.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(thumbMargin, 0, thumbImg.width - thumbMargin, thumbImg.height);
					imgThumb = thumbImg;
				}
			} else {
				PApplet.println("item ERROR", itemImg.getPath(), "image not found");
			}
		}
	}

	public PImage generateThumbnail(ManuCapture_v1_1 context, File rawImgFile) {

		PApplet parent = context;
		String thumbnailPath = getThumbnailPath(context.project.projectDirectory, rawImgFile);
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
		angle = rotation;
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
		thumbImg = thumbImg.get(0, 0, thumbImg.width - thumbMargin, thumbImg.height);
		context.println("Thumbnail Generated : " + thumbnailPath);
		return thumbImg;
	}

	/*
	 * Thumbnail processing
	 * 
	 */
	public String getThumbnailPath(String projectDirectory, File imageFile) {
		String rawImgName = FilenameUtils.removeExtension(imageFile.getName());
		return projectDirectory + "/thumbnails/" + rawImgName + "_thumb.jpg";
	}

	public PImage loadPreview(String previewFolder, String rawImagePath, String resizedImage) {
		PImage img = null;
		if (!rawImagePath.equals("")) {
			// Clear preview folder
			context.deleteAllFiles(previewFolder, ".jpg");
			String previewFile = rawImagePath.replace(".cr2", ".jpg").replace("/raw/", "/previews/");
			imagePreview = previewFile;
			if (new File(previewFile).exists()) {
				img = context.loadImage(previewFile);
			} else {
				File itemImgRight = new File(rawImagePath);
				String fileName = FilenameUtils.removeExtension(itemImgRight.getName());
				String previewName = fileName + "-preview3.jpg";
				String previewFullPath = previewFolder + previewName;
				String resizedImageFullPath = previewFolder + resizedImage;
				String command = "exiv2 -ep3 -l " + previewFolder + " " + rawImagePath;
				System.out.println("comando " + command);
				try {
					Process process = Runtime.getRuntime().exec(command);
					process.waitFor();
				} catch (Exception e) {
					e.printStackTrace();
				}
				command = context.appPath + "/epeg-master/src/bin/epeg -w " + context.contentGUI.viewerWidthResolution
						+ " -p -q 100 " + previewFullPath + " " + resizedImageFullPath.replace(".jpg", "-rot.jpg");
				try {
					Process process = Runtime.getRuntime().exec(command);
					InputStream error = process.getErrorStream();
					process.waitFor();
					String err = "Error:";
					for (int i = 0; i < error.available(); i++) {
						err += (char) error.read();
					}
					if (!err.equals("Error:")) {
						context.println(err + " " + command);
					}
					command = "convert " + resizedImageFullPath.replace(".jpg", "-rot.jpg") + " -rotate " + rotation
							+ " " + resizedImageFullPath;
					System.out.println("comando " + command);
					try {
						process = Runtime.getRuntime().exec(command);
						process.waitFor();
					} catch (Exception e) {
						e.printStackTrace();
					}
					command = "cp " + resizedImageFullPath + " " + previewFile;
					System.out.println("comando " + command);
					try {
						process = Runtime.getRuntime().exec(command);
						process.waitFor();
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("end convert, start loadimage");
					img = context.loadImage(resizedImageFullPath);
					imagePreview = previewFile;
					context.contentGUI.renderRightImage = true;
					System.out.println("end loadimage, FINISH loadPreview " + resizedImageFullPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return img;
	}

	public void saveMetadata() {
		if (imagePath == null || imagePath.equals("")) {
			return;
		}

		String sideCarFilePath = getSidecarPath();
		File file = new File(sideCarFilePath);
		if (!file.exists()) {
			// load the template
			sideCarFilePath = templatePath;
			file = new File(sideCarFilePath);
		}

		String line;
		String newtext = "";

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("[Coarse Transformation]")) {
					
					// move inside rotation
					while ((!line.trim().equals(""))) {
						line = reader.readLine();
						if (line.startsWith("Rotate")) {
							newtext += "Rotate" + "=" + rotation+"\n";
						} else {
							newtext += line + "\n";
						}
					}
				}else {
					newtext += line + "\n";
				}
			}

			FileUtils.writeStringToFile(new File(getSidecarPath()), newtext, Charset.defaultCharset());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

	}

	public void saveMetadataOldXML() {
		if (imagePath == null || imagePath.equals("")) {
			return;
		}
		XML projectXML = new XML("xmp");
		XML manu = projectXML.addChild("name");
		XML crop = projectXML.addChild("crop");
		String projectFilePath = getSidecarPath();
		manu.setString("filename", imagePath.replaceAll("raw/", ""));
		manu.setInt("rotation", rotation);
		crop.setFloat("left", this.guides.get(0).pos.x / context.contentGUI.wImageViewerSize);
		crop.setFloat("top", this.guides.get(1).pos.y / context.contentGUI.hImageViewerSize);
		crop.setFloat("right", this.guides.get(2).pos.x / context.contentGUI.wImageViewerSize);
		crop.setFloat("bottom", this.guides.get(3).pos.y / context.contentGUI.hImageViewerSize);
		context.saveXML(projectXML, projectFilePath);
	}

	private String getSidecarPath() {
		if (pathMock != null) {
			// return pathMock;
			return pathMock.replace(".cr2", SIDECAR_EXTENSION);
		} else {
			return context.project.projectDirectory + (imagePath).replace(".cr2", SIDECAR_EXTENSION);
		}
	}

	public String getAbsolutePath() {
		return (context.project.projectDirectory + "/" + imagePath);
	}

	public String getName() {
		return (imagePath.replace("raw/", ""));
	}

	public void loadMetadata() {
		if (new File(getSidecarPath()).exists()) {
			XML projectDataXML = context.loadXML(getSidecarPath());
			XML crop = projectDataXML.getChild("crop");
			float left = crop.getFloat("left") * context.contentGUI.wImageViewerSize;
			float top = crop.getFloat("top") * context.contentGUI.hImageViewerSize;
			float right = crop.getFloat("right") * context.contentGUI.wImageViewerSize;
			float bottom = crop.getFloat("bottom") * context.contentGUI.hImageViewerSize;
			this.guides.get(0).pos.x = left;
			this.guides.get(0).pos.y = 0;
			this.guides.get(1).pos.x = 0;
			this.guides.get(1).pos.y = top;
			this.guides.get(2).pos.x = right;
			this.guides.get(2).pos.y = 0;
			this.guides.get(3).pos.x = 0;
			this.guides.get(3).pos.y = bottom;
		}
	}

	public static void main(String[] args) {
		MImage image = new MImage();
		image.imagePath = "/home/dudito/proyectos/book_scanner/Manucapture_Crop_Pages/dataSet/024/024_Page_Left_1.cr2";
		image.pathMock = "/home/dudito/proyectos/book_scanner/Manucapture_Crop_Pages/dataSet/024/024_Page_Left_1.cr2";

		image.saveMetadata();
	}
}
