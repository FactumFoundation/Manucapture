import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import g4p_controls.G4P;
import oscP5.OscMessage;
import processing.core.PImage;
import processing.data.XML;

public class Project {

	String projectFilePath = "";
	String projectDirectory = "";
	// metadata
	String projectName = "";
	String projectComment = "";
	String projectCode = "";
	String projectAuthor = "";

	// new metadata
	String timestamp = "";
	String targetDirectory = "";
	String targetSubdirectory = "";
	String uploaded = "";
	String source;

	ArrayList<Item> items = new ArrayList<Item>();

	boolean thumbnailsLoaded = false;

	ManuCaptureContext context;

	int NO_SCROLL_TRANSITION = 0;
	int SCROLL_TRANSITION = 1;
	int scrollTransitionState = NO_SCROLL_TRANSITION;

	PImage previewImgLeft = null;
	PImage previewImgRight = null;

	String lastLeftImagePath = "";
	String lastRightImagePath = "";

	String nextLeftImagePath = "";
	String nextRightImagePath = "";
	
	private int resW = 3000;

	public synchronized void loadProjectMethod(String projectPath) {

		if (!projectFilePath.equals("")) {
			closeProject();
		}

		if (projectPath.equals("")) {
			G4P.showMessage(context.parent, "ERROR: Problem opening last session  Please load the folder manually.",
					"Save project", G4P.ERROR);
			return;
		}

		projectFilePath = projectPath;
		XML projectDataXML = context.parent.loadXML(projectPath);
		loadProjectMetadata(projectDataXML);
		XML[] itemsXML = projectDataXML.getChild("items").getChildren("item");
		for (int i = 0; i < itemsXML.length; i++) {
			try {
				XML itemXML = itemsXML[i];
				String imagePathLeft = itemXML.getChild("image_left").getContent();
				String imagePathRight = itemXML.getChild("image_right").getContent();
				String pageNumStr = itemXML.getChild("page_num").getContent();

				float pageNum = Float.parseFloat(pageNumStr);
				String comment = itemXML.getChild("comment").getContent();
				String type = itemXML.getChild("type").getContent();
				Item newItem = new Item(context, imagePathLeft, imagePathRight, pageNum, comment, type);
				items.add(newItem);
			} catch (Exception e) {
				context.parent.println("ERROR loading item", i);
			}
		}
		File projectFile = new File(projectPath);
		projectDirectory = projectFile.getParent();

		File thumbnailsFolder = new File(projectDirectory + "/thumbnails");
		if (!thumbnailsFolder.exists()) {
			if (thumbnailsFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + thumbnailsFolder.getPath());
				} catch (Exception e) {
					context.parent.println("Couldn't create thumbnail directory permisions");
				}
			} else {
				context.parent.println("Failed to create thumbnail directory!");
			}
		}

		File projectDirectoryFile = new File(projectDirectory);
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (!item.imagePathLeft.equals("")) {
				File itemImgLeft = new File(projectDirectory + "/" + item.imagePathLeft);
				if (itemImgLeft.exists()) {
					String fileName = itemImgLeft.getName();
					String thumbnailPath = projectDirectoryFile.getPath() + "/thumbnails/"
							+ FilenameUtils.removeExtension(fileName) + "_thumb.jpg";
					context.parent.println("Left " + thumbnailPath);
					File thumbFile = new File(thumbnailPath);
					if (!thumbFile.exists()) {
						item.imgThumbLeft = context.thumbnail.generateThumbnail(context, itemImgLeft, false);
					} else {
						PImage thumbImg = context.parent.loadImage(thumbnailPath);
						if (thumbImg == null) {
							context.parent.println("ni pudimos cargar la imagen " + thumbnailPath);
						} else {

						}
						thumbImg = thumbImg.get(context.thumbnail.thumbMargin, 0,
								thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
						item.imgThumbLeft = thumbImg;
					}
				} else {
					item.imgThumbLeft = null;
					context.parent.println("Left ERROR", itemImgLeft.getPath(), "image not found");
				}
			} else {
				item.imgThumbLeft = null;
			}
			if (!item.imagePathRight.equals("")) {
				File itemImgRight = new File(projectDirectory + "/" + item.imagePathRight);
				if (itemImgRight.exists()) {
					String fileName = itemImgRight.getName();
					String thumbnailPath = projectDirectoryFile.getPath() + "/thumbnails/"
							+ FilenameUtils.removeExtension(fileName) + "_thumb.jpg";
					context.parent.println("Right " + thumbnailPath);
					File thumbFile = new File(thumbnailPath);
					if (!thumbFile.exists()) {
						item.imgThumbRight = context.thumbnail.generateThumbnail(context, itemImgRight, true);
					} else {
						PImage thumbImg = context.parent.loadImage(thumbnailPath);
						thumbImg = thumbImg.get(0, 0, thumbImg.width - context.thumbnail.thumbMargin, thumbImg.height);
						item.imgThumbRight = thumbImg;
					}
				} else {
					item.imgThumbRight = null;
					context.parent.println("Right ERROR", itemImgRight.getPath(), "image not found");
				}
			} else {
				item.imgThumbRight = null;
			}
		}
		try {
			G2P5.setImageCount(new Integer(projectDataXML.getChild("image_counter").getContent()));
		} catch (Exception e) {
			context.parent.println("ERROR loading image counter, seting to list size");
			G2P5.setImageCount(items.size());
		}
		thumbnailsLoaded = true;

	}

	public void loadProjectMetadata(XML projectDataXML) {

		projectName = projectDataXML.getChild("metadata").getChild("name").getContent();
		context.gui.name_text.setText(projectName);
		projectComment = projectDataXML.getChild("metadata").getChild("comment").getContent();
		context.gui.project_comments_text.setText(projectComment);
		projectCode = projectDataXML.getChild("metadata").getChild("code").getContent();
		context.gui.code_text.setText(projectCode);
		projectAuthor = projectDataXML.getChild("metadata").getChild("author").getContent();
		context.gui.author_text.setText(projectAuthor);
		context.parent.println(projectDataXML.getChild("image_counter"));
		G2P5.setImageCount(new Integer(projectDataXML.getChild("image_counter").getContent()));

		XML timestampXML = projectDataXML.getChild("metadata").getChild("timestamp");
		XML targetDirectoryXML = projectDataXML.getChild("metadata").getChild("targetDirectory");
		XML targetSubdirectoryXML = projectDataXML.getChild("metadata").getChild("targetSubdirectory");
		XML uploadedXML = projectDataXML.getChild("metadata").getChild("uploaded");

		if (timestampXML != null)
			timestamp = timestampXML.getContent("");
		else
			timestamp = "";

		if (targetDirectoryXML != null)
			targetDirectory = targetDirectoryXML.getContent("");
		else
			targetDirectory = "";

		if (targetSubdirectoryXML != null)
			targetSubdirectory = targetSubdirectoryXML.getContent("");
		else
			targetSubdirectory = "";

		if (uploadedXML != null)
			uploaded = uploadedXML.getContent("");
		else
			uploaded = "";

	}

	public void saveProjectXML() {
		context.parent.println(projectName, projectCode, projectAuthor, projectComment);
		if (!(projectName.equals("") && projectCode.equals("") && projectAuthor.equals("")
				&& projectComment.equals(""))) {
			XML projectXML = new XML("project");

			XML metadataXML = new XML("metadata");
			XML nameXML = new XML("name");
			nameXML.setContent(projectName);
			metadataXML.addChild(nameXML);
			XML codeXML = new XML("code");
			codeXML.setContent(projectCode);
			metadataXML.addChild(codeXML);
			XML projectCommentXML = new XML("comment");
			projectCommentXML.setContent(projectComment);
			metadataXML.addChild(projectCommentXML);
			XML authorXML = new XML("author");
			authorXML.setContent(projectAuthor);
			metadataXML.addChild(authorXML);
			// timestamp
			XML timestampXML = new XML("timestamp");
			timestampXML.setContent(targetDirectory);
			metadataXML.addChild(timestampXML);
			// targetSubdirectory
			XML targetSubdirectoryXML = new XML("targetSubdirectory");
			targetSubdirectoryXML.setContent(targetSubdirectory);
			metadataXML.addChild(targetSubdirectoryXML);
			// target directory
			XML targetDirectoryXML = new XML("targetDirectory");
			targetDirectoryXML.setContent(targetDirectory);
			metadataXML.addChild(targetDirectoryXML);
			// uploaded
			XML uploadedXML = new XML("uploaded");
			uploadedXML.setContent(uploaded);
			metadataXML.addChild(uploadedXML);

			projectXML.addChild(metadataXML);

			XML itemsXML = new XML("items");
			for (int i = 0; i < items.size(); i++) {
				Item item = items.get(i);
				if (!(item.imagePathLeft.equals("") && item.imagePathRight.equals(""))) {
					XML itemXML = new XML("item");
					XML imageLeftXML = new XML("image_left");
					if (item.imagePathLeft != null) {
						imageLeftXML.setContent(item.imagePathLeft);
					} else {
						imageLeftXML.setContent("");
					}
					itemXML.addChild(imageLeftXML);
					XML imageRightXML = new XML("image_right");
					if (item.imagePathRight != null) {
						imageRightXML.setContent(item.imagePathRight);
					} else {
						imageRightXML.setContent("");
					}
					imageRightXML.setContent(item.imagePathRight);
					itemXML.addChild(imageRightXML);
					XML pageNumXML = new XML("page_num");
					pageNumXML.setContent(String.valueOf(item.pagNum));
					itemXML.addChild(pageNumXML);
					XML pageCommentXML = new XML("comment");
					pageCommentXML.setContent(item.comment);
					itemXML.addChild(pageCommentXML);
					XML typeXML = new XML("type");
					typeXML.setContent(item.type);
					itemXML.addChild(typeXML);
					itemsXML.addChild(itemXML);
				}
			}
			projectXML.addChild(itemsXML);

			XML imageCounterXML = new XML("image_counter");
			imageCounterXML.setContent(String.valueOf(G2P5.getImageCount()));
			projectXML.addChild(imageCounterXML);

			File fileProject = new File(projectFilePath);
			if (fileProject.exists()) {
				String commandGenerate = "mv " + projectFilePath + " " + projectDirectory + "/project_backup.xml";
				context.parent.println(commandGenerate);
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
				}
			}

			context.parent.saveXML(projectXML, projectFilePath);
			context.parent.println("Saved project to ", projectFilePath);
		} else {
			context.parent.println("ERROR: No project info, no data is saved. Please write the project name and code");
			G4P.showMessage(context.parent,
					"ERROR: No project info, no data is saved.\nPlease write the project name and code. ",
					"Save project", G4P.ERROR);

		}
	}

	public void closeProject() {

		removeUnusedImages();
		saveProjectXML();
		items = new ArrayList<Item>();
		thumbnailsLoaded = false;
		projectFilePath = "";
		projectAuthor = "";
		projectCode = "";
		projectComment = "";
		projectDirectory = "";

		// TODO: Check everything to close in the Reset viewer

	}

	private void deleteFile(String targetFilePath) {
		String commandGenerate = "rm " + targetFilePath;
		context.parent.println(commandGenerate);
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
		}
	}

	private void deleteAllFiles(String targetFilePath, String suf) {
		context.parent.println("delete all " + suf + "files " + targetFilePath);
		File storageDir = new File(targetFilePath);
		for (File tempFile : storageDir.listFiles()) {
			if (tempFile.getName().endsWith(suf))
				tempFile.delete();
		}
		context.parent.println("end delete all " + suf + "files " + targetFilePath);
	}

	/*
	 * Garbage Image Collector
	 */

	public synchronized String[] getUnusedImage() {
		ArrayList<String> usedImgs = new ArrayList<String>();
		for (int index = 0; index < items.size(); index++) {
			Item item = items.get(index);
			if (!item.imagePathLeft.equals(""))
				usedImgs.add(projectDirectory + "/" + item.imagePathLeft);
			if (!item.imagePathRight.equals(""))
				usedImgs.add(projectDirectory + "/" + item.imagePathRight);

		}

		File folder = new File(projectDirectory + "/raw");
		String[] files = folder.list();
		if (files != null) {
			ArrayList<String> unusedFiles = new ArrayList<String>();
			for (int index = 0; index < files.length; index++) {
				boolean found = false;
				for (int index2 = 0; index2 < usedImgs.size(); index2++) {
					File usedImg = new File(usedImgs.get(index2));
					// println(usedImg.getName(),files[index]);
					if (usedImg.getName().equals(files[index])) {
						found = true;
						break;
					}
				}
				if (!found) {
					String targetPath = folder.getPath() + "/" + files[index];
					unusedFiles.add(targetPath);
				}
			}
			unusedFiles.removeAll(usedImgs);
			String[] unusedArr = new String[unusedFiles.size()];
			for (int index = 0; index < unusedArr.length; index++) {
				unusedArr[index] = unusedFiles.get(index);
			}
			return unusedArr;
		} else {
			return null;
		}

	}

	public synchronized void removeUnusedImages() {
		// Garbage collection
		String[] unusedImgs = getUnusedImage();
		if (unusedImgs != null && unusedImgs.length > 0 && unusedImgs.length < items.size() * 2) {
			for (int index = 0; index < unusedImgs.length; index++) {
				String targetFilePath = unusedImgs[index];
				deleteFile(targetFilePath);
				String thumbnailPath = context.thumbnail.getThumbnailPath(projectDirectory, new File(targetFilePath));
				deleteFile(thumbnailPath);
			}
		}
	}

	int selectedItemIndex = -1;

	/*
	 * Items
	 */

	Item selectedItem = null;

	public int findItemIndexByPagNum(float pgNum) {
		int index = -1;
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).pagNum == pgNum) {
				index = i;
				break;
			}
		}
		return index;
	}

	public void selectItem(int index) {
		if (index >= 0 && index < items.size()) {
			selectedItem = items.get(index);
			OscMessage myMessage = new OscMessage("/load/item");
			String leftImagePath = "";
			String rightImagePath = "";
			if (selectedItem.imagePathRight != null && selectedItem.imagePathRight.length() != 0) {
				rightImagePath = projectDirectory + "/" + selectedItem.imagePathRight;
				myMessage.add(rightImagePath);
			} else {
				myMessage.add("");
			}
			if (selectedItem.imagePathLeft != null && (selectedItem.imagePathLeft.length() != 0)) {
				leftImagePath = projectDirectory + "/" + selectedItem.imagePathLeft;
				myMessage.add(leftImagePath);
			} else {
				myMessage.add("");
			}

			context.parent.println("send the message to viewer");

			// View message for viewer
			context.oscP5.send(myMessage, context.viewerLocation);
			// Now we do the preview on app
			loadPreviews(leftImagePath, rightImagePath);

			context.gui.page_comments_text.setText(selectedItem.comment);
			context.gui.page_num_text.setText(String.valueOf(selectedItem.pagNum));
			context.gphotoA.setTargetFile(projectDirectory + "/raw", projectCode);
			context.gphotoB.setTargetFile(projectDirectory + "/raw", projectCode);

		}
	}

	// Image preview

	void loadPreviews(String leftImagePath, String rightImagePath) {

		long startMillis = context.parent.millis();

		System.out.println("start preview");

		nextRightImagePath = rightImagePath;
		loadRightPreview();
		System.out.println("end preview left");
		nextLeftImagePath = leftImagePath;
		loadLeftPreview();
		// context.parent.thread("loadLeftPreview");

		// context.parent.thread("loadRightPreview");

		System.out.println("end preview rigth");

		long endMillis = context.parent.millis();
	}

	public void loadRightPreview() {
		if (!nextRightImagePath.equals("")) {

			String previewFolder = context.parent.sketchPath() + "/data/preview_right/";

			// Clear preview folder
			deleteAllFiles(previewFolder, ".jpg");

			File itemImgRight = new File(nextRightImagePath);
			String fileName = FilenameUtils.removeExtension(itemImgRight.getName());
			String resizedImage = "right_preview.jpg";
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

			// command = "convert " + previewFullPath + " -resize 1000x667 " +
			// resizedImageFullPath;
			command = "/home/factum/git/book_scanner/bookScanner/Manucapture/epegbin/epeg -w "+resW+" -p -q 100 "
					+ previewFullPath + " " + resizedImageFullPath.replace(".jpg", "-rot.jpg");
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

				command = "convert " + resizedImageFullPath.replace(".jpg", "-rot.jpg") + " -rotate 270 "
						+ resizedImageFullPath;
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
				previewImgRight = context.parent.loadImage(resizedImageFullPath);
				context.renderRight = true;
				System.out.println("end loadimage, FINISH loadRightPreview " + resizedImageFullPath);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public void loadLeftPreview() {
		if (!nextLeftImagePath.equals("")) {

			String previewFolder = context.parent.sketchPath() + "/data/preview_left/";

			deleteAllFiles(previewFolder, ".jpg");

			File itemImgLeft = new File(nextLeftImagePath);
			String fileName = FilenameUtils.removeExtension(itemImgLeft.getName());
			String resizedImage = "left_preview.jpg";
			String previewName = fileName + "-preview3.jpg";
			String previewFullPath = previewFolder + previewName;
			String resizedImageFullPath = previewFolder + resizedImage;
			String command = "exiv2 -ep3 -l " + previewFolder + " " + nextLeftImagePath;
			lastLeftImagePath = previewFullPath;
			try {
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			// command = "convert " + previewFullPath + " -resize 1000x667 " +
			// resizedImageFullPath;
			command = "/home/factum/git/book_scanner/bookScanner/Manucapture/epegbin/epeg -w "+resW+" -p -q 100 "
					+ previewFullPath + " " + resizedImageFullPath.replace(".jpg", "-rot.jpg");
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

				command = "convert " + resizedImageFullPath.replace(".jpg", "-rot.jpg") + " -rotate 90 "
						+ resizedImageFullPath;
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
				previewImgLeft = context.parent.loadImage(resizedImageFullPath);
				context.renderLeft = true;
				System.out.println("end loadimage, FINISH loadLeftPreview " + resizedImageFullPath);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
