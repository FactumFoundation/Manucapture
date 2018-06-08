import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.XML;

public class MImage {

	String imagePath;
	String thumbPath;

	PImage imgThumb;
	PImage imgPreview;
	ManuCaptureContext context;

	List<HotArea> mesh = new ArrayList<>();

	int rotation;

	String lastLeftImagePath = "";
	String lastRightImagePath = "";

	PImage previewImg;

	void remove() {
		// imagePath = "";
		// thumbPath = "";
		imgThumb = null;
		mesh = new ArrayList<>();
		String pathI = context.project.projectDirectory + "/" + imagePath;
		if (pathI != null && new File(pathI).exists())
			new File(pathI).delete();
		String pathT = thumbPath;
		if (pathT != null && new File(pathT).exists())
			new File(pathT).delete();

		if (getXmpPath() != null && new File(getXmpPath()).exists())
			new File(getXmpPath()).delete();

		// PReview
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
				String thumbnailPath = getThumbnailPath(context.project.projectDirectory, itemImg);
				context.parent.println("itemImage " + thumbnailPath);
				File thumbFile = new File(thumbnailPath);
				thumbPath = thumbnailPath;
				if (!thumbFile.exists()) {
					imgThumb = generateThumbnail(context, itemImg);
				} else {
					PImage thumbImg = context.parent.loadImage(thumbnailPath);
					thumbImg = thumbImg.get(thumbMargin, 0, thumbImg.width - thumbMargin, thumbImg.height);
					imgThumb = thumbImg;
				}
			} else {
				PApplet.println("item ERROR", itemImg.getPath(), "image not found");
			}
		}
	}

	int thumbMargin = 6;

	public PImage generateThumbnail(ManuCaptureContext context, File rawImgFile) {

		PApplet parent = context.parent;

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
		// if (rightImg) {
		thumbImg = thumbImg.get(0, 0, thumbImg.width - thumbMargin, thumbImg.height);
		// } else {
		// thumbImg = thumbImg.get(thumbMargin, 0, thumbImg.width - thumbMargin,
		// thumbImg.height);
		// }

		context.parent.println("Thumbnail Generated : " + thumbnailPath);
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

	public void loadPreview(String previewFolder, String nextRightImagePath, String resizedImage) {

		PImage img = null;

		if (!nextRightImagePath.equals("")) {

			// Clear preview folder
			context.deleteAllFiles(previewFolder, ".jpg");

			String previewFile = nextRightImagePath.replace(".cr2", ".jpg").replace("/raw/", "/previews/");

			if (new File(previewFile).exists()) {
				imgPreview = context.parent.loadImage(previewFile);
			} else {

				File itemImgRight = new File(nextRightImagePath);
				String fileName = FilenameUtils.removeExtension(itemImgRight.getName());
				String previewName = fileName + "-preview3.jpg";
				String previewFullPath = previewFolder + previewName;
				String resizedImageFullPath = previewFolder + resizedImage;
				lastRightImagePath = previewFullPath;

				String command = "exiv2 -ep3 -l " + previewFolder + " " + nextRightImagePath;
				System.out.println("comando " + command);
				try {
					Process process = Runtime.getRuntime().exec(command);
					process.waitFor();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// command = "convert " + previewFullPath + " -resize 1000x667 "
				// +
				// resizedImageFullPath;
				command = context.appPath + "/epeg-master/src/bin/epeg -w " + context.viewerWidthResolution
						+ " -p -q 100 " + previewFullPath + " " + resizedImageFullPath.replace(".jpg", "-rot.jpg");
				System.out.println("end command exiv2, start resize " + command);
				try {
					Process process = Runtime.getRuntime().exec(command);
					InputStream error = process.getErrorStream();

					process.waitFor();
					String err = "Error:";
					for (int i = 0; i < error.available(); i++) {
						err += (char) error.read();
					}
					if (!err.equals("Error:")) {
						context.parent.println(err);
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

					// String newPath = previewFullPath.replace(".jpg",
					// "_IMGP.jpg");

					// Files.move(Paths.get(newPath),
					// Paths.get(resizedImageFullPath),
					// StandardCopyOption.REPLACE_EXISTING);
					img = context.parent.loadImage(resizedImageFullPath);
					context.renderRight = true;
					System.out.println("end loadimage, FINISH loadRightPreview " + resizedImageFullPath);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		this.previewImg = img;
		// return img;
	}

	public void saveMetadata() {

		XML projectXML = new XML("xmp");

		XML manu = projectXML.addChild("name");
		XML mesh = projectXML.addChild("mesh");

		String projectFilePath = getXmpPath();
		manu.setString("filename", imagePath.replaceAll("raw/", ""));
		manu.setInt("rotation", rotation);

		mesh.setFloat("TLx", this.mesh.get(0).pos.x / context.hImageViewerSize);
		mesh.setFloat("TLy", this.mesh.get(0).pos.y / context.wImageViewerSize);

		mesh.setFloat("TRx", this.mesh.get(1).pos.x / context.hImageViewerSize);
		mesh.setFloat("TRy", this.mesh.get(1).pos.y / context.wImageViewerSize);

		mesh.setFloat("BRx", this.mesh.get(2).pos.x / context.hImageViewerSize);
		mesh.setFloat("BRy", this.mesh.get(2).pos.y / context.wImageViewerSize);

		mesh.setFloat("BLx", this.mesh.get(3).pos.x / context.hImageViewerSize);
		mesh.setFloat("BLy", this.mesh.get(3).pos.y / context.wImageViewerSize);

		context.parent.saveXML(projectXML, projectFilePath);
	}

	private String getXmpPath() {
		return (context.project.projectDirectory + "/" + imagePath).replace(".cr2", ".xmp");
	}

	public String getAbsolutePath() {
		return (context.project.projectDirectory + "/" + imagePath);
	}

	public String getName() {
		return (imagePath.replace("raw/", ""));
	}

	public void loadMetadata() {
		if (new File(getXmpPath()).exists()) {

			XML projectDataXML = context.parent.loadXML(getXmpPath());

			XML mesh = projectDataXML.getChild("mesh");

			float tlx = mesh.getFloat("TLx") * context.hImageViewerSize;
			float tly = mesh.getFloat("TLy") * context.wImageViewerSize;

			float trx = mesh.getFloat("TRx") * context.hImageViewerSize;
			float tryy = mesh.getFloat("TRy") * context.wImageViewerSize;

			float brx = mesh.getFloat("BRx") * context.hImageViewerSize;
			float bry = mesh.getFloat("BRy") * context.wImageViewerSize;

			float blx = mesh.getFloat("BLx") * context.hImageViewerSize;
			float bly = mesh.getFloat("BLy") * context.wImageViewerSize;

			this.mesh.get(0).pos.x = tlx;
			this.mesh.get(0).pos.y = tly;

			this.mesh.get(1).pos.x = trx;
			this.mesh.get(1).pos.y = tryy;

			this.mesh.get(2).pos.x = brx;
			this.mesh.get(2).pos.y = bry;

			this.mesh.get(3).pos.x = blx;
			this.mesh.get(3).pos.y = bly;

		}
	}

}
