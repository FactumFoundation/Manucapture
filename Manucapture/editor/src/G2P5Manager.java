
/**
 * G2P5 Concurrent Manager
 *  
 * @author edumo
 *
 */

public class G2P5Manager {

	protected static int imageCounter;

	public static synchronized int addImageCount() {
		imageCounter++;
		return imageCounter;
	}

	public static int getImageCount() {
		return imageCounter;
	}

	public static void setImageCount(int imageCount) {
		imageCounter = imageCount;
	}

	public static void init(int initialImageCount) {
		G2P5Manager.setImageCount(initialImageCount);
		G2P5.killAllGphotoProcess();
	}

}
