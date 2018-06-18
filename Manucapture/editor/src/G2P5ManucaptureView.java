import java.util.List;

import processing.core.PGraphics;
import processing.core.PVector;

public class G2P5ManucaptureView {

	G2P5 g2p5;
	ManuCaptureContext context;

	private void drawImagePreview(MImage img, PVector lastPressedR, int marginLeftViewer, int marginTopViewer,
			List<HotArea> areas, PGraphics canvas) {
		if (lastPressedR != null) {
			// pimero quiero saber pos en la imagen
			float imgScale = img.imgPreview.width / (float) context.hImageViewerSize;
			PVector virtualPos = PVector.sub(lastPressedR, new PVector(marginLeftViewer, marginTopViewer));

			PVector virtualPosScaled = PVector.mult(virtualPos, imgScale);

			int portviewSizeX = (int) (context.hImageViewerSize);
			int portviewSizeY = (int) (context.wImageViewerSize);

			int portviewStartX = (int) (virtualPosScaled.x - portviewSizeX / 2);
			int portviewStartY = (int) (virtualPosScaled.y - portviewSizeY / 2);

			if (portviewStartX + portviewSizeX > img.imgPreview.width) {
				portviewStartX = img.imgPreview.width - portviewSizeX;
			}

			if (portviewStartY + portviewSizeY > img.imgPreview.height) {
				portviewStartY = img.imgPreview.height - portviewSizeY;
			}

			if (portviewStartX < 0) {
				portviewStartX = 0;
			}

			if (portviewStartY < 0) {
				portviewStartY = 0;
			}

			canvas.image(img.imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, portviewStartX,
					portviewStartY, portviewStartX + portviewSizeX, portviewStartY + portviewSizeY);
		} else {

			canvas.image(img.imgPreview, 0, 0, context.hImageViewerSize, context.wImageViewerSize, 0, 0,
					img.imgPreview.width, img.imgPreview.height);
		}

	}

}
