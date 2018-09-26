import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class ItemsGUI {

	int overedItemIndex;
	int overedCancelButtonIndex = -1;
	int selectItemColor = 0xffB7691F;
	int selectItemColorStroke = 0xffEA780C;
	PGraphics itemsViewPort;
	int scrollBarWidth = 15;
	int scrollBarColor = 0xff62605E;
	int scrollHandleColorIdle = 0xff0F99CE;
	int scrollHandleColorOver = 0xffD1F0FC;
	int scrollHandleColorPressed = 0xffFA8728;
	float scrollHandleHeight = 200; // Test
	float scrollHandleY = 0;
	int scrollHandleState;
	int SCROLL_HANDLE_IDLE = 0;
	int SCROLL_HANDLE_OVER = 1;
	int lastDrawedItems = -1;
	int SCROLL_HANDLE_PRESSED = 2;

	PImage removeItemIcon;
	PImage chartItemIcon;
	int marginX = 2;
	int marginInfo = 10;
	int marginY = 10;
	int itemThumbHeight = 160;
	int removeIconSize = 20;
	int overItemColor = 0xff4B4949;

	int itemListViewPortX = 35;
	int itemListViewPortY = 90;
	int itemListViewPortWidth = 285;
	int itemListViewPortHeight = 900;

	ManuCapture_v1_1 context;
	
	int ADDING_ITEM_TRANSITION = 1;
	int REMOVING_ITEM_TRANSITION = 2;
	int NO_TRANSITION = 0;
	int itemsViewTransition = NO_TRANSITION;
	float transitionPhase = 0.0f;
	float itemBaseY = 0;
	
	PImage bookIcon;
	
	ItemsGUI(ManuCapture_v1_1 context) {
		this.context = context;

		itemsViewPort = context.createGraphics(itemListViewPortWidth, itemListViewPortHeight,
				context.P2D);
		scrollHandleState = SCROLL_HANDLE_IDLE;

		removeItemIcon = context.loadImage("cross_inv_20x20.jpeg");
		chartItemIcon = context.loadImage("chart-item.png");
		
		bookIcon = context.loadImage("bookIcon.png");
		bookIcon.resize(bookIcon.width / 6, bookIcon.height / 6);

	}
	
	public void drawItemsViewPort() {

		// ITEM LIST VIEW PORT SECTION
		// /////////////////////////////////////////////////
		// Items view port
		int itemHeight = itemThumbHeight + marginY;

		// update item list related values
		float targetItemBaseY;
		int fullListHeight = itemHeight * context.project.items.size();

		if (context.project.items.size() == 0) {
			targetItemBaseY = 0.0f;
		} else {
			targetItemBaseY = context.map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
		}

		if (context.project.scrollTransitionState == context.project.NO_SCROLL_TRANSITION)
			itemBaseY = targetItemBaseY;
		else {
			if (PApplet.abs(targetItemBaseY - itemBaseY) < 20 || context.project.items.size() <= 1) {
				context.project.scrollTransitionState = context.project.NO_SCROLL_TRANSITION;
				itemBaseY = targetItemBaseY;
			} else {
				if (targetItemBaseY > itemBaseY)
					itemBaseY += 10f;
				else {
					itemBaseY -= 10f;
				}
			}

		}

		if (fullListHeight == 0) {
			scrollHandleHeight = itemsViewPort.height;
		} else {
			scrollHandleHeight = PApplet.map(itemsViewPort.height, 0, fullListHeight, 0, itemsViewPort.height);

		}

		itemsViewPort.beginDraw();
		itemsViewPort.background(0);
		itemsViewPort.noStroke();
		//Barra de desplazamiento
		itemsViewPort.fill(scrollBarColor);
		itemsViewPort.rect(itemsViewPort.width - scrollBarWidth, 0, scrollBarWidth, itemsViewPort.height);
		if (scrollHandleState == SCROLL_HANDLE_IDLE) {
			itemsViewPort.fill(scrollHandleColorIdle);
		} else if (scrollHandleState == SCROLL_HANDLE_OVER) {
			itemsViewPort.fill(scrollHandleColorOver);
		} else if (scrollHandleState == SCROLL_HANDLE_PRESSED) {
			// println("PRESSED",scrollHandleColorPressed);
			itemsViewPort.fill(scrollHandleColorPressed);
		}
		itemsViewPort.rect(itemsViewPort.width - scrollBarWidth, scrollHandleY, scrollBarWidth, scrollHandleHeight);

		if (context.project.items.size() == 0 && !context.project.projectDirectory.equals("")) {
			itemsViewPort.stroke(selectItemColorStroke);
			itemsViewPort.fill(selectItemColor);
			int iconWidht = itemsViewPort.width - scrollBarWidth - 1;
			itemsViewPort.rect(0, -marginY / 2, itemsViewPort.width - scrollBarWidth - 1, itemHeight);
			itemsViewPort.image(bookIcon, (iconWidht - bookIcon.width) / 2, (itemHeight - bookIcon.height) / 2);
		} else {
			// items
			if (context.project.thumbnailsLoaded) {

				// TODO: Add transition when adding
				if (lastDrawedItems != context.project.items.size() && lastDrawedItems != -1
						&& (context.project.selectedItemIndex != context.project.items.size() - 1)) {
					if (lastDrawedItems < context.project.items.size()) {
						itemsViewTransition = ADDING_ITEM_TRANSITION;
						transitionPhase = 0.0f;
					} else {
						itemsViewTransition = REMOVING_ITEM_TRANSITION;
						transitionPhase = 1.0f;
					}

				}

				if (itemsViewTransition == ADDING_ITEM_TRANSITION) {
					transitionPhase += 0.1f;
					if (transitionPhase >= 1.0f) {
						itemsViewTransition = NO_TRANSITION;
					}
				} else if (itemsViewTransition == REMOVING_ITEM_TRANSITION) {
					transitionPhase -= 0.1f;
					if (transitionPhase <= 0.0f) {
						itemsViewTransition = NO_TRANSITION;
					}
				}

				int heightInc = itemThumbHeight + marginY;
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += heightInc) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					heightInc = itemThumbHeight + marginY;
					if (i < context.project.items.size() && viewPortRelativeHeight > -itemHeight
							&& viewPortRelativeHeight < itemsViewPort.height) {
						Item item = context.project.items.get(i);
						if (i == overedItemIndex && i != context.project.selectedItemIndex) {
							itemsViewPort.stroke(scrollBarColor);
							itemsViewPort.fill(overItemColor);
							itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
									itemsViewPort.width - scrollBarWidth - 1, itemHeight);
						} else if (i == context.project.selectedItemIndex) {
							if (itemsViewTransition == NO_TRANSITION) {
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, itemHeight);
							} else if (itemsViewTransition == ADDING_ITEM_TRANSITION) {
								int emptyHeight = (int) (transitionPhase * (itemThumbHeight + marginY));
								heightInc = emptyHeight;
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, heightInc);

							} else if (itemsViewTransition == REMOVING_ITEM_TRANSITION) {
								int emptyHeight = (int) (transitionPhase * (itemThumbHeight + marginY));
								heightInc = emptyHeight + itemThumbHeight + marginY;
								itemsViewPort.stroke(selectItemColorStroke);
								itemsViewPort.fill(selectItemColor);
								itemsViewPort.rect(0, viewPortRelativeHeight - marginY / 2,
										itemsViewPort.width - scrollBarWidth - 1, heightInc);
								viewPortRelativeHeight += emptyHeight;
							}
						}

						if ((i != context.project.selectedItemIndex) || (itemsViewTransition != ADDING_ITEM_TRANSITION)) {
							if (item.mImageLeft.imgThumb != null) {
								itemsViewPort.image(item.mImageLeft.imgThumb, marginX,
										viewPortRelativeHeight);
							}
							if (item.mImageRight.imgThumb != null) {
								itemsViewPort.image(item.mImageRight.imgThumb, marginX + item.mImageLeft.imgThumb.width, viewPortRelativeHeight);
							}
							itemsViewPort.noFill();
							itemsViewPort.stroke(255);
							if (i == overedCancelButtonIndex) {
								itemsViewPort.tint(255, 20, 0);
							} else {
								itemsViewPort.tint(200, 0, 0);
							}

							itemsViewPort.image(removeItemIcon,
									itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize,
									viewPortRelativeHeight + marginY);

							itemsViewPort.noTint();
							if (item.type != null && item.type.equals(Item.TYPE_CHART))
								itemsViewPort.image(chartItemIcon,
										itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize,
										viewPortRelativeHeight + marginY * 4, 20, 20);

							String page = String.valueOf(item.pagNum);
							page = page.replace(".0", "");
							float pageNumberWidth = context.textWidth(page) + 10;
							itemsViewPort.noStroke();
							itemsViewPort.fill(0);
							itemsViewPort.rect(itemsViewPort.width - scrollBarWidth - marginInfo - pageNumberWidth - 8,
									viewPortRelativeHeight + itemThumbHeight - marginY - 18, pageNumberWidth, 21);
							itemsViewPort.fill(200);
							itemsViewPort.textSize(18);
							itemsViewPort.text(page,
									itemsViewPort.width - scrollBarWidth - marginInfo - pageNumberWidth - 5,
									viewPortRelativeHeight + itemThumbHeight - marginY);
							itemsViewPort.stroke(0xff585757);
							int spacerMargin = 2;
							int spacerHeight = viewPortRelativeHeight + itemHeight + marginY / 2;
							itemsViewPort.line(spacerMargin, spacerHeight,
									itemsViewPort.width - scrollBarWidth - 2 * spacerMargin, spacerHeight);
						}
					} else {
						heightInc = itemThumbHeight + marginY;
					}
				}
				lastDrawedItems = context.project.items.size();
			}

		}
		itemsViewPort.endDraw();
		context.image(itemsViewPort, itemListViewPortX, itemListViewPortY);
	}

	public void mouseMoved() {

		overedItemIndex = -1;
		overedCancelButtonIndex = -1;

		int scrollHandleX = itemsViewPort.width - scrollBarWidth;
		if ((context.mouseX > (itemListViewPortX + scrollHandleX))
				&& (context.mouseX < (itemListViewPortX + scrollHandleX + scrollBarWidth))) {
			if ((context.mouseY > (itemListViewPortY + scrollHandleY))
					&& (context.mouseY < (itemListViewPortY + scrollHandleY + scrollHandleHeight))) {
				scrollHandleState = SCROLL_HANDLE_OVER;
				return;
			}
		}

		if ((context.mouseX > itemListViewPortX)
				&& (context.mouseX < (itemsViewPort.width + itemListViewPortX))) {
			if ((context.mouseY > itemListViewPortY)
					&& (context.mouseY < (itemListViewPortY + itemsViewPort.height))) {
				int itemHeight = itemThumbHeight + marginY;
				int fullListHeight = itemHeight * context.project.items.size();
				float itemBaseY = context.map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += itemHeight) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					if (viewPortRelativeHeight > -itemHeight && viewPortRelativeHeight < itemsViewPort.height) {
						// check item overed
						if ((context.mouseY > (viewPortRelativeHeight + itemListViewPortY))
								&& (context.mouseY < (viewPortRelativeHeight + itemHeight
										+ itemListViewPortY))) {
							overedItemIndex = i;
							// check over remove button
							float cancelButtonX = itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize
									+ itemListViewPortX;
							float cancelButtonY = viewPortRelativeHeight + marginY + itemListViewPortY;
							if ((context.mouseX > cancelButtonX)
									&& (context.mouseX < (cancelButtonX + removeIconSize))) {
								if ((context.mouseY > cancelButtonY)
										&& (context.mouseY < (cancelButtonY + removeIconSize))) {
									overedCancelButtonIndex = i;
									break;
								}
							}
						}
					}
				}
			}
		}
		scrollHandleState = SCROLL_HANDLE_IDLE;

	}

	public void forceSelectedItem(int index, boolean transition) {
		// Update gui list
		int itemHeight = itemThumbHeight + marginY;
		int fullListHeight = itemHeight * context.project.items.size();
		scrollHandleHeight = context.map(itemsViewPort.height, 0, fullListHeight, 0, itemsViewPort.height);
		if (scrollHandleHeight > itemsViewPort.height) {
			scrollHandleHeight = itemsViewPort.height;
		}
		if (scrollHandleHeight < 0)
			scrollHandleHeight = 0;
		try {
			if (context.project.items.size() == 1)
				scrollHandleY = 0;
			else
				scrollHandleY = context.map(index, 0, context.project.items.size() - 1, 0,
						itemsViewPort.height - scrollHandleHeight);
		} catch (Exception e) {
			scrollHandleY = 0;
		}
		// Update selected item
		if (scrollHandleY < 0) {
			scrollHandleY = 0;
		}
		if ((scrollHandleY + scrollHandleHeight) > itemsViewPort.height) {
			scrollHandleY = itemsViewPort.height - scrollHandleHeight;
		}

	}

	public void mousePressed() {

		int scrollHandleX = itemsViewPort.width - scrollBarWidth;
		if ((context.mouseX > (itemListViewPortX + scrollHandleX))
				&& (context.mouseX < (itemListViewPortX + scrollHandleX + scrollBarWidth))) {
			if ((context.mouseY > (itemListViewPortY + scrollHandleY))
					&& (context.mouseY < (itemListViewPortY + scrollHandleY + scrollHandleHeight))) {
				scrollHandleState = SCROLL_HANDLE_PRESSED;
				return;
			}
		}
		if ((context.mouseX > itemListViewPortX) && (context.mouseX < (itemsViewPort.width + itemListViewPortX))) {
			if ((context.mouseY > itemListViewPortY) && (context.mouseY < (itemListViewPortY + itemsViewPort.height))) {
				int itemHeight = itemThumbHeight + marginY;
				int fullListHeight = itemHeight * context.project.items.size();
				float itemBaseY = context.map(scrollHandleY, 0, itemsViewPort.height, 0, fullListHeight);
				for (int i = 0, itemY = 0; itemY < fullListHeight; i++, itemY += itemHeight) {
					int viewPortRelativeHeight = (int) (itemY - itemBaseY);
					if (viewPortRelativeHeight > -itemHeight && viewPortRelativeHeight < itemsViewPort.height) {
						// check item overed
						if ((context.mouseY > (viewPortRelativeHeight + itemListViewPortY))
								&& (context.mouseY < (viewPortRelativeHeight + itemHeight + itemListViewPortY))) {
							context.	project.selectedItemIndex = i;
							// check over remove button
							float cancelButtonX = itemsViewPort.width - scrollBarWidth - marginInfo - removeIconSize
									+ itemListViewPortX;
							float cancelButtonY = viewPortRelativeHeight + marginY + itemListViewPortY;
							if ((context.mouseX > cancelButtonX) && (context.mouseX < (cancelButtonX + removeIconSize))) {
								if ((context.mouseY > cancelButtonY) && (context.mouseY < (cancelButtonY + removeIconSize))) {
									// if (shutterMode != REPEAT_SHUTTER) {
									context.project.removeItem(i);
									forceSelectedItem(i, true);
									// } else {
									// removeItem(i);
									// }
									break;
								}
							}
							context.project.selectItem(context.project.selectedItemIndex);
						}
					}
				}
			}
		}
		scrollHandleState = SCROLL_HANDLE_IDLE;
	}
	
	public void mouseDragged() {
		if (scrollHandleState == SCROLL_HANDLE_PRESSED) {
			if (scrollHandleHeight < itemsViewPort.height) {
				scrollHandleY += context.mouseY - context.pmouseY;
				if (scrollHandleY < 0) {
					scrollHandleY = 0;
				} else if ((scrollHandleY + scrollHandleHeight) > itemsViewPort.height) {
					scrollHandleY = itemsViewPort.height - scrollHandleHeight;
				}
			} else
				scrollHandleY = 0;
		}
	}

	public void mouseReleased() {
		scrollHandleState = SCROLL_HANDLE_IDLE;
	}

}
