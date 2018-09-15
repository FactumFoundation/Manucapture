import java.util.List;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

public class G2P5ManucaptureView {

	G2P5 g2p5;
	ManuCapture_v1_1 context;

	private void drawImagePreview(PImage imgPreview, PVector lastPressedR, int marginLeftViewer, int marginTopViewer,
			List<HotArea> areas, PGraphics canvas) {
		if (lastPressedR != null) {
			// pimero quiero saber pos en la imagen
			float imgScale = imgPreview.width / (float) context.hImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedR, new PVector(marginLeftViewer, marginTopViewer));

			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);

			int portviewSizeX = (int) (context.hImageViewerSize);
			int portviewSizeY = (int) (context.wImageViewerSize);

			int portviewStartX = (int) (virtualPosScaled.x - portviewSizeX / 2);
			int portviewStartY = (int) (virtualPosScaled.y - portviewSizeY / 2);

			if (portviewStartX + portviewSizeX > imgPreview.width) {
				portviewStartX = imgPreview.width - portviewSizeX;
			}

			if (portviewStartY + portviewSizeY > imgPreview.height) {
				portviewStartY = imgPreview.height - portviewSizeY;
			}

			if (portviewStartX < 0) {
				portviewStartX = 0;
			}

			if (portviewStartY < 0) {
				portviewStartY = 0;
			}

			canvas.image(imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {

			canvas.image(imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, 0, 0,
					imgPreview.width, imgPreview.height);
		}

	}

}
