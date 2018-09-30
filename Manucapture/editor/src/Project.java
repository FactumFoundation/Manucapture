import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import g4p_controls.G4P;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.XML;

public class Project {

	String projectFilePath = "";
	String projectDirectory = "";
	// metadata
	//String projectName = "";
	//String projectComment = "";
	String projectCode = "";
	//String projectAuthor = "";

	// new metadata
	String timestamp = "";
	String targetDirectory = "";
	String uploaded = "";
	String source;

	String serialCameraPageRight;
	String serialCameraPageLeft;

	int rotationPageRight;
	int rotationPageLeft;

	ArrayList<Item> items = new ArrayList<Item>();

	boolean thumbnailsLoaded = false;

	ManuCapture_v1_1 context;

	int NO_SCROLL_TRANSITION = 0;
	int SCROLL_TRANSITION = 1;
	int scrollTransitionState = NO_SCROLL_TRANSITION;

	String nextLeftImagePath = "";
	String nextRightImagePath = "";

	int selectedItemIndex = -1;

	/*
	 * Items
	 */

	Item selectedItem = null;

	public synchronized void loadProjectMethod(String projectPath) {

		if (!projectFilePath.equals("")) {
			closeProject();
		}

		if (projectPath.equals("")) {
			G4P.showMessage(context, "ERROR: Problem opening last session  Please load the folder manually.",
					"Save project", G4P.ERROR);
			return;
		}

		projectFilePath = projectPath;
		XML projectDataXML = context.loadXML(projectPath);
		loadProjectMetadata(projectDataXML);
		loadProjectSerials(projectDataXML);
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
				newItem.loadMetadata();
				items.add(newItem);
			} catch (Exception e) {
				e.printStackTrace();
				context.println("ERROR loading item", i);
			}
		}
		File projectFile = new File(projectPath);
		projectDirectory = projectFile.getParent();

		createFolder(projectDirectory + "/thumbnails");
		createFolder(projectDirectory + "/previews");
		createFolder(projectDirectory + "/preview_left");
		createFolder(projectDirectory + "/preview_right");

		// File projectDirectoryFile = new File(projectDirectory);
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			item.loadThumbnails();
		}
		thumbnailsLoaded = true;

	}

	private void createFolder(String previewsFolderPath) {
		File newFolder = new File(previewsFolderPath);
		if (!newFolder.exists()) {
			context.println("creating previews folder " + newFolder.getPath());
			if (newFolder.mkdir()) {
				try {
					Runtime.getRuntime().exec("chmod -R ugo+rw " + newFolder.getPath());
				} catch (Exception e) {
					context.println("Couldn't create previews directory permisions");
				}
			} else {
				context.println("Failed to create " + previewsFolderPath + " directory!");
			}
		}
	}

	public void loadProjectSerials(XML projectDataXML) {
		XML serialsXML = projectDataXML.getChild("serials");
		XML a = serialsXML.getChild("A");
		serialCameraPageRight = a.getContent();
		serialCameraPageLeft = serialsXML.getChild("B").getContent();

		rotationPageRight = serialsXML.getChild("A").getInt("rotation");
		rotationPageLeft = serialsXML.getChild("B").getInt("rotation");
	}

	public void loadProjectMetadata(XML projectDataXML) {

	//	projectName = projectDataXML.getChild("metadata").getChild("name").getContent();
	//	context.gui.name_text.setText(projectName);
		context.gui.project_info.setText("PROJECT INFO " + context.project.projectFilePath);

	//	projectComment = projectDataXML.getChild("metadata").getChild("comment").getContent();
	//	context.gui.project_comments_text.setText(projectComment);
		projectCode = projectDataXML.getChild("metadata").getChild("code").getContent();
		context.gui.code_text.setText(projectCode);
	//	projectAuthor = projectDataXML.getChild("metadata").getChild("author").getContent();
	//	context.gui.author_text.setText(projectAuthor);
		context.println(projectDataXML.getChild("image_counter"));
		G2P5Manager.setImageCount(new Integer(projectDataXML.getChild("image_counter").getContent()));

		XML timestampXML = projectDataXML.getChild("metadata").getChild("timestamp");
		XML targetDirectoryXML = projectDataXML.getChild("metadata").getChild("targetDirectory");
		XML uploadedXML = projectDataXML.getChild("metadata").getChild("uploaded");

		if (timestampXML != null)
			timestamp = timestampXML.getContent("");
		else
			timestamp = "";

		if (targetDirectoryXML != null)
			targetDirectory = targetDirectoryXML.getContent("");
		else
			targetDirectory = "";

		if (uploadedXML != null)
			uploaded = uploadedXML.getContent("");
		else
			uploaded = "";

	}

	public void saveProjectXML() {
		if (!projectCode.equals("")) {
			XML projectXML = new XML("project");
			XML metadataXML = new XML("metadata");
			XML nameXML = new XML("name");
			metadataXML.addChild(nameXML);
			XML codeXML = new XML("code");
			codeXML.setContent(projectCode);
			metadataXML.addChild(codeXML);
			XML projectCommentXML = new XML("comment");
			metadataXML.addChild(projectCommentXML);
			XML authorXML = new XML("author");
			metadataXML.addChild(authorXML);
			// timestamp
			XML timestampXML = new XML("timestamp");
			timestampXML.setContent(targetDirectory);
			metadataXML.addChild(timestampXML);
			// target directory
			XML targetDirectoryXML = new XML("targetDirectory");
			targetDirectoryXML.setContent(targetDirectory);
			metadataXML.addChild(targetDirectoryXML);
			// uploaded
			XML uploadedXML = new XML("uploaded");
			uploadedXML.setContent(uploaded);
			metadataXML.addChild(uploadedXML);

			projectXML.addChild(metadataXML);

			XML serialsXML = new XML("serials");

			XML AXML = new XML("A");
			AXML.setContent(serialCameraPageRight);
			AXML.setInt("rotation", rotationPageRight);
			serialsXML.addChild(AXML);

			XML BXML = new XML("B");
			BXML.setContent(serialCameraPageLeft);
			BXML.setInt("rotation", rotationPageLeft);
			serialsXML.addChild(BXML);

			projectXML.addChild(serialsXML);

			XML itemsXML = new XML("items");
			for (int i = 0; i < items.size(); i++) {
				Item item = items.get(i);
				if (!(item.mImageLeft.imagePath.equals("") && item.mImageRight.imagePath.equals(""))) {
					XML itemXML = new XML("item");
					XML imageLeftXML = new XML("image_left");
					if (item.mImageLeft.imagePath != null) {
						imageLeftXML.setContent(item.mImageLeft.imagePath);
					} else {
						imageLeftXML.setContent("");
					}
					itemXML.addChild(imageLeftXML);
					XML imageRightXML = new XML("image_right");
					if (item.mImageRight.imagePath != null) {
						imageRightXML.setContent(item.mImageRight.imagePath);
					} else {
						imageRightXML.setContent("");
					}
					imageRightXML.setContent(item.mImageRight.imagePath);
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
			imageCounterXML.setContent(String.valueOf(G2P5Manager.getImageCount()));
			projectXML.addChild(imageCounterXML);

			File fileProject = new File(projectFilePath);
			if (fileProject.exists()) {
				String commandGenerate = "mv " + projectFilePath + " " + projectDirectory + "/project_backup.xml";
				context.println(commandGenerate);
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

			context.saveXML(projectXML, projectFilePath);
			context.println("Saved project to ", projectFilePath);
			
			//VERIFY INTEGRITY XML -> FILES
			verify();
		} else {
			context.println("ERROR: No project info, no data is saved. Please write the project name and code");
			G4P.showMessage(context,
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
		projectCode = "";
		projectDirectory = "";

		// TODO: Check everything to close in the Reset viewer

	}

	private void verify() {

		File folder = new File(projectDirectory + "/raw");
		String[] files = folder.list();

		List<String> unusedFiles = getUnusedFiles(files);
		List<String> unusedImages = getImagesXmlNoFile(files);
		
		if(unusedFiles.size()>0) {
			//TENEMOS MAS FICHEROS QUE ELEMENTOS EN XML
			PApplet.println("ERROR , more files than xml elements " +unusedFiles);
		}
		
		if(unusedImages.size()>0) {
			//TENEMOS MAS ELEMENTOS EN XML QUE FICHEROS
			PApplet.println("ERROR, more xml elements than files "+unusedImages);
		}

	}

	/*
	 * Garbage Image Collector
	 */

	public List<String> getUnusedFiles(String[] files) {
		// File folder = new File(projectDirectory + "/raw");
		ArrayList<String> usedImgsAndXMPS = getAllFileNames();

		if (files != null) {
			ArrayList<String> unusedFiles = new ArrayList<>();
			for (int index = 0; index < files.length; index++) {
				boolean found = false;
				for (int index2 = 0; index2 < usedImgsAndXMPS.size(); index2++) {
					// println(usedImg.getName(),files[index]);
					if (usedImgsAndXMPS.get(index2).equals(files[index])) {
						found = true;
						break;
					}
				}
				if (!found) {
					unusedFiles.add(files[index]);
				}
			}
			// unusedFiles.removeAll(usedImgs);
			return unusedFiles;
		} else {
			return new ArrayList<>();
		}

	}

	private ArrayList<String> getAllFileNames() {
		ArrayList<String> usedImgsAndXMPS = new ArrayList<String>();
		for (int index = 0; index < items.size(); index++) {
			Item item = items.get(index);
			if (!item.mImageLeft.imagePath.equals("")) {
			//	usedImgsAndXMPS.add(item.mImageLeft);
				String nameTemp = item.mImageLeft.getName().substring(1);
				String nameTempXMP = nameTemp.replace(".cr2", ".xmp");
				usedImgsAndXMPS.add(nameTemp);
				usedImgsAndXMPS.add(nameTempXMP);
			}
			if (!item.mImageRight.imagePath.equals("")) {
				String nameTemp = item.mImageRight.getName().substring(1);
				String nameTempXMP = nameTemp.replace(".cr2", ".xmp");
				usedImgsAndXMPS.add(nameTemp);
				usedImgsAndXMPS.add(nameTempXMP);
			}

		}
		return usedImgsAndXMPS;
	}

	/*
	 * Garbage Image Collector
	 */

	public List<String> getImagesXmlNoFile(String[] files) {
		// File folder = new File(projectDirectory + "/raw");
		ArrayList<String> usedImgsAndXMPS = getAllFileNames();

		if (files != null) {
			ArrayList<String> unusedFiles = new ArrayList<>();
			for (int index2 = 0; index2 < usedImgsAndXMPS.size(); index2++) {

				boolean found = false;

				
				for (int index = 0; index < files.length; index++) {
					if (usedImgsAndXMPS.get(index2).equals(files[index])) {
						found = true;
						break;
					}
				}

				if (!found) {
					unusedFiles.add(usedImgsAndXMPS.get(index2));
				}
			}
			return unusedFiles;
		} else {
			return new ArrayList<>();
		}

	}

	public synchronized void removeUnusedImages() {
		// Garbage collection
		// List<MImageE> unusedImgs = getUnusedImage();
		// if (unusedImgs != null && unusedImgs.length > 0 && unusedImgs.length
		// < items.size() * 2) {
		// for (int index = 0; index < unusedImgs.length; index++) {
		// unusedImgs.remove();
		// }
		// }
	}

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
			String leftImagePath = "";
			String rightImagePath = "";
			if (selectedItem.mImageRight.imagePath != null && selectedItem.mImageRight.imagePath.length() != 0) {
				rightImagePath = projectDirectory + "" + selectedItem.mImageRight.imagePath;
			}
			if (selectedItem.mImageLeft.imagePath != null && (selectedItem.mImageLeft.imagePath.length() != 0)) {
				leftImagePath = projectDirectory + "" + selectedItem.mImageLeft.imagePath;
			}
			// Now we do the preview on app
			context.contentGUI.imgPreviewRight = selectedItem.loadRightPreview(projectDirectory,rightImagePath);
			context.contentGUI.imgPreviewLeft = selectedItem.loadLeftPreview(projectDirectory,leftImagePath);
			//selectedItem.loadMetadata();
			context.contentGUI.guidesLeft = selectedItem.mImageLeft.copyGuides(context.contentGUI.guidesLeft);
			context.contentGUI.guidesRight = selectedItem.mImageRight.copyGuides(context.contentGUI.guidesRight);
		}
	}

	// Image preview

	public void removeItem(int index) {
		Item itemToRemove = items.get(index);
		float pageNum = itemToRemove.pagNum;
		items.remove(index);
		// if (itemToRemove.type.equals("Item")) {
		if (index < items.size()) {
			for (int i = index; i < items.size(); i++) {
				items.get(i).pagNum--;
			}
		}
		/*
		 * } else { if (index < items.size() - 1) { for (int i = index; i < items.size()
		 * - 1; i++) { if ((int) items.get(i).pagNum == (int) pageNum) { if
		 * (items.get(i).pagNum - (int) items.get(i).pagNum > pageNum - (int) pageNum) {
		 * float newPageNum = PApplet.round((items.get(i).pagNum - 0.1f) * 10) / 10.0f;
		 * items.get(i).pagNum = newPageNum; } }
		 * 
		 * } } }
		 */
		selectedItemIndex = PApplet.min(index, items.size() - 1);
		if (selectedItemIndex >= 0 && items.size() > 0) {
			selectItem(selectedItemIndex);
		}
		// forceSelectedItem(selectedItemIndex, true);
		saveProjectXML();
		itemToRemove.remove();
		// removeUnusedImages();
	}

	public void forceSelectedItem(int index, boolean transition) {
		selectedItemIndex = PApplet.min(index, items.size() - 1);
		if (transition) {
			scrollTransitionState = SCROLL_TRANSITION;
		}
		if (selectedItemIndex >= 0 && items.size() > 0) {
			// Update gui list
			context.itemsGUI.forceSelectedItem(index, transition);
			selectItem(selectedItemIndex);
		}
	}

	public synchronized void addItem(int index, Item newItem) {
		if (index >= 0) {
			if (index < items.size()) {
				if (index > 0) {
					Item targetItem = items.get(index - 1);
					if (targetItem.mImageLeft.imagePath.equals("") && targetItem.mImageRight.imagePath.equals("")) {
						newItem.pagNum = targetItem.pagNum;
						items.set(index - 1, newItem);
						selectedItemIndex = PApplet.min(index, items.size());
						if ((selectedItemIndex != items.size())
								|| (items.get(selectedItemIndex).pagNum != newItem.pagNum + 1)) {
							Item emptyItem = new Item(context, "", "", (int) (newItem.pagNum) + 1, "", "Item");
							items.add(selectedItemIndex, emptyItem);
							forceSelectedItem(index, true);
						} else if (selectedItemIndex != items.size()) {
							forceSelectedItem(selectedItemIndex, true);
						}
					} else {
						items.add(index, newItem);
						forceSelectedItem(index, true);
					}
				} else {
					items.add(index, newItem);
					forceSelectedItem(index, true);
				}
			} else {
				if (items.size() == 0) {
					items.add(newItem);
					forceSelectedItem(index, true);
				} else {
					Item targetItem = items.get(index - 1);
					if (targetItem.mImageLeft.imagePath.equals("") && targetItem.mImageRight.imagePath.equals("")) {
						newItem.pagNum = targetItem.pagNum;
						items.set(index - 1, newItem);
						selectedItemIndex = PApplet.min(index, items.size());
						forceSelectedItem(selectedItemIndex, true);
					} else {
						items.add(newItem);
						forceSelectedItem(index, true);
					}
				}
			}
			for (int i = index + 1; i < items.size(); i++) {
				items.get(i).pagNum++;
			}
			saveProjectXML();
			context._println("item added");
		}
	}

	
	public void replaceItem(int index, Item newItem) {
		if (index >= 0 && index < items.size()) {
			// si viene vacío rellenenamos con el item anterior
			if (newItem.mImageLeft.imagePath.equals("")) {
				newItem.mImageLeft.imagePath = items.get(index).mImageLeft.imagePath;
				newItem.mImageLeft.guides = items.get(index).mImageLeft.guides;
				newItem.loadThumbnails();
			} else {
				// sino borramos el anterior
				items.get(index).mImageLeft.remove();
			}
			if (newItem.mImageRight.imagePath.equals("")) {
				newItem.mImageRight.imagePath = items.get(index).mImageRight.imagePath;
				newItem.mImageRight.guides = items.get(index).mImageRight.guides;
				newItem.loadThumbnails();
			} else {
				// sino borramos el anterior
				items.get(index).mImageRight.remove();
			}
			items.remove(index);
			items.add(index, newItem);
			saveProjectXML();
		}
	}
	
}
