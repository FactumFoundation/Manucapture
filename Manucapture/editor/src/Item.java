
import processing.core.PImage;

public class Item {

	public static String TYPE_ITEM = "Item";
	public static String TYPE_ITEM_NO_PAGE = "noPage";
	public static String TYPE_CHART = "Chart";
	public static String TYPE_BACKGROUND = "background";
	MImage mImageLeft = new MImage();
	MImage mImageRight = new MImage();
	float pagNum;
	String comment;
	String type;
	ManuCapture_v1_1 context;

	public Item(ManuCapture_v1_1 context, String pageLeftPath, String pageRightPath, float pagNum, String comment,
			String type) {
		this.pagNum = pagNum;
		this.comment = comment;
		this.type = type;
		this.context = context;
		mImageLeft.rotation = context.rotationPageLeft;
		mImageRight.rotation = context.rotationPageRight;
		mImageLeft.g2p5Adapter = context.gphotoPageLeftAdapter;
		mImageRight.g2p5Adapter = context.gphotoPageRightAdapter;
		this.mImageLeft.imagePath = pageLeftPath;
		this.mImageRight.imagePath = pageRightPath;
		this.mImageLeft.guides = context.contentGUI.copyGuides(context.contentGUI.guidesLeft);
		this.mImageRight.guides = context.contentGUI.copyGuides(context.contentGUI.guidesRight);
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
			
			mImageLeft.loadMetadata();
			mImageRight.loadMetadata();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveMetadata() {
	//	if(type != TYPE_CHART)
			mImageLeft.saveMetadata();
			mImageRight.saveMetadata();
	}

	public PImage loadRightPreview(String projectDirectory, String nextRightImagePath) {
		return mImageRight.loadPreview(projectDirectory + "/preview_right/", nextRightImagePath, "right_preview.jpg");
	}

	public PImage loadLeftPreview(String projectDirectory, String leftImagePath) {
		return mImageLeft.loadPreview(projectDirectory + "/preview_left/", leftImagePath, "left_preview.jpg");
	}

	public void removeCache() {
		mImageLeft.removeCache();
		mImageRight.removeCache();
	}

}