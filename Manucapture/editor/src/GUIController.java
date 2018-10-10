import g4p_controls.G4P;
import g4p_controls.GButton;
import g4p_controls.GCScheme;
import g4p_controls.GEvent;
import g4p_controls.GImageButton;
import g4p_controls.GImageToggleButton;
import g4p_controls.GTextField;
import g4p_controls.GWinData;
import g4p_controls.GWindow;
import netP5.StringUtils;
import processing.core.PApplet;
import processing.event.MouseEvent;
import processing.video.Movie;

public class GUIController {

	ManuCapture_v1_1 context;
	public boolean editing = false;

	public GUIController(ManuCapture_v1_1 context) {
		super();
		this.context = context;
	}

	public void first_page_button_click(GImageButton source, GEvent event) {
		PApplet.println("SHUTTER CONTROL SET NOTMAL MODE");
		if (context.chartStateMachine != 3) {
			context.project.forceSelectedItem(0, false);
			context.itemsGUI.forceSelectedItem(0, false);
		}
	}

	public void last_page_button_click(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			context.project.forceSelectedItem(context.project.items.size() - 1, false);
			context.itemsGUI.forceSelectedItem(context.project.items.size() - 1, false);
		}
	}

	public void code_text_change(GTextField source, GEvent event) {
		context.project.projectCode = source.getText();
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}
	}

	public void repeat_shutter_click(GImageToggleButton source, GEvent event) {
		PApplet.println("SHUTTER CONTROL SET REPEAT MODE");
		if(context.shutterMode != ManuCapture_v1_1.CALIB_SHUTTER) {
			if (source.getState() == 1) {
				context.shutterMode = ManuCapture_v1_1.REPEAT_SHUTTER;
			} else {
				context.shutterMode = ManuCapture_v1_1.NORMAL_SHUTTER;
			}
		} 
		context.contentGUI.noZoom();
	}

	public void close_popup_project_window(GWindow window) {
		close_popup_project(null, null);
	}

	public void mouse_popUp(PApplet applet, GWinData windata) {
	}

	public void mouse_popUp(PApplet applet, GWinData windata, MouseEvent ouseevent) {
	}

	public void close_popup_project(GButton source, GEvent event) {
		boolean someError = false;

		if (context.project.projectCode == null || context.project.projectCode.trim().equals("")) {
			someError = true;
		}

		String codeTemp = context.project.projectCode;

		if (!someError) {
			// first check if the proyect is just created
			boolean ret = true;
			if (context.creatingProyect) {
				// we have to create the folder and especify path of project

				ret = context.createProject(context.project.projectCode);
				if (ret) {
					context.creatingProyect = false;
					context.project.projectCode = codeTemp;
				}

			}

			if (ret) {
				context.setStateApp(ManuCapture_v1_1.STATE_APP_PROJECT);
				context.gui.grpAll.setVisible(1, true);

				context.gui.grpProject.setVisible(1, false);

				context.project.saveProjectXML();
			}
		} else {
			G4P.showMessage(context, "Missing name or code", "", G4P.WARNING);
		}
		editing = false;
		PApplet.println("close window edit project data");
	}

	public void calibration_shutter_click(GImageToggleButton source, GEvent event) {
		if (source == null || source.getState() == 1) {
			PApplet.println("SHUTTER CONTROL SET CALIBRATION MODE");
			context.shutterMode = ManuCapture_v1_1.CALIB_SHUTTER;
			context.cameraState = ManuCapture_v1_1.STATE_CHART;
			context.chartStateMachine = 1;
			context.gui.btnCrop.setEnabled(false);
			context.gui.btnRepeat.setEnabled(false);
			context.gui.btnRepeat.setState(0);
		} else {
			context.shutterMode = context.NORMAL_SHUTTER;
			context.chartStateMachine = 0;
			context.contentGUI.noZoom();
			context.setCaptureState(ManuCapture_v1_1.NORMAL_SHUTTER);
			context.gui.btnCrop.setEnabled(true);
			context.gui.btnRepeat.setEnabled(true);
		}
		context.contentGUI.noZoom();
	}

	public void trigger_button_click(GImageButton source, GEvent event) {
		PApplet.println("SHUTTER TRIGGERED");
		if (context.isAllMirrorsReady() && context.chartStateMachine != 3) {
			context.project.cleanTempImages();
			context.capture();
			context.clearPaths();
		} else {
			G4P.showMessage(context, "Mirrors are not ready", "", G4P.WARNING);
		}
	}

	public void camera_page_right_connect_button_click(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			if (!context.gphotoPageRight.isConnected())
				camera_page_right_active_button_click(null, null);
			else
				camera_page_right_inactive_button_click(null, null);
			context.contentGUI.noZoom();
		}
	}

	public void camera_page_right_active_button_click(GButton source, GEvent event) {
		GUI gui = context.gui;
		context.gphotoPageRight.setActive(true);
		if (context.gphotoPageRight.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoPageRight.active = true;
		}
	}

	public void camera_page_right_inactive_button_click(GButton source, GEvent event) {
		GUI gui = context.gui;
		context.gphotoPageRight.setActive(false);
	}

	public void camera_page_left_connect_button_click(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			if (!context.gphotoPageLeft.isConnected())
				camera_page_left_active_button_click(null, null);
			else
				camera_page_left_inactive_button_click(null, null);
			context.contentGUI.noZoom();
		}
	}

	public void camera_page_left_active_button_click(GButton source, GEvent event) {
		PApplet.println("camera_B_active_button - GButton >> GEvent." + event + " @ " + context.millis());
		context.gphotoPageLeft.setActive(true);
		if (context.gphotoPageLeft.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoPageLeft.active = true;
		}
	}

	public void camera_page_left_inactive_button_click(GButton source, GEvent event) {
		PApplet.println("camera_B_inactive_button - GButton >> GEvent." + event + " @ " + context.millis());
		context.gphotoPageLeft.setActive(false);
	}

	public void parameters_click(GButton source, GEvent event) {
		PApplet.println("parameters_button - GButton >> GEvent." + event + " @ " + context.millis());
	}

	public void load_click(GButton source, GEvent event) {
		String documentFileName = G4P.selectInput("Load XML");
		if (documentFileName != null) {
			context.project.selectedItem = null;
			context.loading = true;
			context.loadProject(documentFileName);
			context.loading = false;
			context.shutterMode = ManuCapture_v1_1.NORMAL_SHUTTER;
			context.contentGUI.noZoom();
		} else {
			context.loading = false;
		}
	}

	public void edit_click(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			context.setStateApp(context.STATE_APP_EDITING_PROJECT);
			context.gui.code_text.setText(context.project.projectCode);
			context.gui.code_text.setEnabled(false);
			
		}
	}

	public void close_click(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			context.setStateApp(ManuCapture_v1_1.STATE_APP_NO_PROJECT);
		}
	}

	public void new_button_click(GButton source, GEvent event) {
		// String projectFolderPath = G4P.selectFolder("Select the project folder for
		// NEW PROJECT");
		// if (projectFolderPath != null) {
		context.gui.code_text.setEnabled(true);
		editing = true;
		context.creatingProyect = true;
		context.contentGUI.initCropGuides();
		context.contentGUI.resetPreviews();
		context.gui.project_info.setText("PROJECT INFO " + context.proyectsRepositoryFolder);
		context.project.thumbnailsLoaded = false;
		context.project.selectedItem = null;
		context.project.projectCode = "";
		context.project.items.clear();
		context.gui.code_text.setText(context.project.projectCode);
		context.setStateApp(context.STATE_APP_EDITING_PROJECT);
		// }
	}

	public void page_search_text_change(GTextField source, GEvent event) {
		PApplet.println("textfield2 - GTextField >> GEvent." + event + " @ " + context.millis());
	}

	public void crop_click(GImageToggleButton source, GEvent event) {
		PApplet.println("start crop editing");
		context.toggleCropMode();
	}

	public void liveView_button_click(GImageToggleButton source, GEvent event) {
		
		if(source.getState()==1) {
			G2P5.killAllGphotoProcess();
			// SHUTTERSPEED
			context.gphotoPageRight.setLiveViewConfig();
			context.gphotoPageLeft.setLiveViewConfig();
			context.gphotoPageRight.startLiveView();
			context.gphotoPageLeft.startLiveView();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(context.liveViewLeft==null) {
				context.liveViewLeft = new Movie(context, context.gphotoPageLeft.getLiveViewStreamPath());
			}
			  
			if(context.liveViewRight==null) {
				context.liveViewRight = new Movie(context, context.gphotoPageRight.getLiveViewStreamPath()); 
			}
			context.println(context.gphotoPageRight.getLiveViewStreamPath());
			context.liveViewLeft.play();
			context.liveViewRight.play();
			
		} else {
			// set normal speed and reconnect tethered capture
			if(context.liveViewRight!=null)
				context.liveViewRight.stop();
			if(context.liveViewLeft==null)
				context.liveViewLeft.stop();
			
			context.gphotoPageRight.stopLiveView();
			context.gphotoPageLeft.stopLiveView();
			context.gphotoPageRight.setNormalConfig();
			context.gphotoPageLeft.setNormalConfig();
			if (!context.gphotoPageRight.isConnected()) {
				context.guiController.camera_page_right_active_button_click(null, null);
			}
			if (!context.gphotoPageLeft.isConnected()) {
				context.guiController.camera_page_left_active_button_click(null, null);
			}
			context.gphotoPageRightAdapter.setTargetFile(context.project.projectDirectory + "/raw", context.project.projectCode);
			context.gphotoPageLeftAdapter.setTargetFile(context.project.projectDirectory + "/raw", context.project.projectCode);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	public void openViewer_1(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			try {
				String cmd = "rawtherapee " + context.project.projectDirectory
						+ context.project.selectedItem.mImageRight.imagePath;
				context.println(cmd);
				Runtime.getRuntime().exec(cmd);
			} catch (Exception e) {
				context._println("Couldn't create raw directory permisions");
			}
			context.contentGUI.noZoom();
		}
	}

	public void openViewer_2(GImageButton source, GEvent event) {
		if (context.chartStateMachine != 3) {
			try {
				Runtime.getRuntime().exec(
						"rawtherapee " + context.project.projectDirectory + context.project.selectedItem.mImageLeft.imagePath);
			} catch (Exception e) {
				context._println("Couldn't create raw directory permisions");
			}
			context.contentGUI.noZoom();
		}
	}

}
