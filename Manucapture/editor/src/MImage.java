import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
