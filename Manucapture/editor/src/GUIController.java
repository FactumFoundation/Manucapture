import g4p_controls.G4P;
import g4p_controls.GButton;
import g4p_controls.GCScheme;
import g4p_controls.GEvent;
import g4p_controls.GTextField;
import g4p_controls.GWinData;
import g4p_controls.GWindow;
import netP5.StringUtils;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class GUIController {

	ManuCaptureContext context;
	
	public GUIController( ManuCaptureContext context) {
		super();
		this.context = context;
	}

	/*
	 * ========================================================= ==== WARNING
	 * === ========================================================= The code in
	 * this tab has been generated from the GUI form designer and care should be
	 * taken when editing this file. Only add/edit code inside the event
	 * handlers i.e. only use lines between the matching comment tags. e.g.
	 * 
	 * void myBtnEvents(GButton button) { //_CODE_:button1:12356: // It is safe
	 * to enter your event code here } //_CODE_:button1:12356:
	 * 
	 * Do not rename this tab!
	 * =========================================================
	 */

	public void first_page_button_click(GButton source, GEvent event) { // _CODE_:first_page_button:431616:
		PApplet.println("SHUTTER CONTROL SET NOTMAL MODE");
	} // _CODE_:first_page_button:431616:

	public void last_page_button_click(GButton source, GEvent event) { // _CODE_:last_page_button:647539:
		PApplet.println("button2 - GButton >> GEvent." + event + " @ " + context.parent.millis());

	} // _CODE_:last_page_button:647539:

	public void name_text_change(GTextField source, GEvent event) { // _CODE_:name_text:702135:
		context.project.projectName = source.getText();
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}
	} // _CODE_:name_text:702135:

	public void code_text_change(GTextField source, GEvent event) { // _CODE_:code_text:779005:
		context.project.projectCode = source.getText();
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}
	} // _CODE_:code_text:779005:

	public void author_text_change(GTextField source, GEvent event) { // _CODE_:author_text:873016:
		context.project.projectAuthor = source.getText();
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}
	} // _CODE_:author_text:873016:

	public void project_comments_change(GTextField source, GEvent event) { // _CODE_:project_comments_text:337734:
		context.project.projectComment = source.getText();
		PApplet.println();
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}

	} // _CODE_:project_comments_text:337734:

	public void page_comments_text_change(GTextField source, GEvent event) { // _CODE_:page_comments_text:397499:
		if (context.project.selectedItem != null) {
			context.project.selectedItem.comment = source.getText();
		}
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			context.project.saveProjectXML();
		}
	} // _CODE_:page_comments_text:397499:

	public void page_num_text_change(GTextField source, GEvent event) { // _CODE_:textfield1:363899:
		if (event.toString() == "ENTERED" && !context.project.projectDirectory.equals("")) {
			try {
				float pageNumber = Float.parseFloat(source.getText());
				int itemIndex = context.project.findItemIndexByPagNum(pageNumber);
				if (itemIndex != -1) {
					context.project.forceSelectedItem(itemIndex, true);
				}
				context.project.saveProjectXML();
			} catch (NumberFormatException ex) {
				PApplet.println("wrong page number");
			}
		}

	} // _CODE_:textfield1:363899:

	public void normal_shutter_click1(GButton source, GEvent event) { // _CODE_:normal_shutter_button:563899:
		PApplet.println("SHUTTER CONTROL SET NORMAL MODE!!!!!");
		context.parent.shutterMode = ManuCapture_v1_1.NORMAL_SHUTTER;
		GUI gui = context.gui;
		context.parent.chartStateMachine = 0;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:normal_shutter_button:563899:

	public void repeat_shutter_click(GButton source, GEvent event) { // _CODE_:repeat_shutter_button:591981:
		PApplet.println("SHUTTER CONTROL SET REPEAT MODE");
		context.parent.shutterMode = ManuCapture_v1_1.REPEAT_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	} // _CODE_:repeat_shutter_button:591981:

	// public void subpage_shutter_click(GButton source, GEvent event) { //
	// _CODE_:subpage_shutter_button:295319:
	// PApplet.println("SHUTTER CONTROL SET SUBPAGE MODE");
	// shutterMode = SUBPAGE_SHUTTER;
	// GUI gui = context.gui;
	// gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// gui.calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
	// } // _CODE_:subpage_shutter_button:295319:

	public void close_popup_project_window(GWindow window) {
		close_popup_project(null, null);
	}

	public void mouse_popUp(PApplet applet, GWinData windata) {
		PApplet.println("holle2" + windata);
	}

	public void mouse_popUp(PApplet applet, GWinData windata, MouseEvent ouseevent) {
		PApplet.println("holl1e" + ouseevent.getAction());
	}

	public void close_popup_project(GButton source, GEvent event) {

		boolean someError = false;
		if (StringUtils.isEmpty(context.project.projectName)) {
			someError = true;
		}

		if (StringUtils.isEmpty(context.project.projectCode)) {
			someError = true;
		}

		if (!someError) {
			context.parent.stateApp = ManuCapture_v1_1.STATE_APP_PROJECT;
			context.gui.grpAll.setVisible(1, true);
			context.gui.grpProject.setVisible(1, false);
			context.parent.editingProject = false;
			context.project.saveProjectXML();

		} else {
			// showwhat error
			G4P.showMessage(context.parent, "Missing name or code", "", G4P.WARNING);
		}

		PApplet.println("close window edit project data");
	}

	public void calibration_shutter_click(GButton source, GEvent event) { // _CODE_:calibration_shutter_button:835827:

		// if (state == CAPTURING) {
		PApplet.println("SHUTTER CONTROL SET CALIBRATION MODE");
		context.parent.shutterMode = ManuCapture_v1_1.CALIB_SHUTTER;
		GUI gui = context.gui;
		gui.normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.calibration_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);

		context.parent.cameraState = ManuCapture_v1_1.STATE_CHART;
		context.parent.chartStateMachine = 1;
		// }
	} // _CODE_:calibration_shutter_button:835827:

	public void trigger_button_click(GButton source, GEvent event) { // _CODE_:trigger_button:381491:
		PApplet.println("SHUTTER TRIGGERED");
		context.capture();
		context.clearPaths();

	} // _CODE_:trigger_button:381491:

	public void camera_A_connected_click(GButton source, GEvent event) { // _CODE_:camera_A_connected_button:265149:
		PApplet.println("button1 - GButton >> GEvent." + event + " @ " + context.parent.millis());
		if (!context.gphotoA.isConnected()) {
			context.gphotoAAdapter = context.createG2P5(context.serialCameraA, "A");
		}
	} // _CODE_:camera_A_connected_button:265149:

	public void camera_A_active_button_click(GButton source, GEvent event) { // _CODE_:camera_A_active_button:906773:
		PApplet.println("camera_A_active_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		context.cameraActiveA = true;
		context.gphotoA.setActive(true);
		if (context.gphotoA.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoA.active = true;
		}
	} // _CODE_:camera_A_active_button:906773:

	public void camera_A_inactive_button_click(GButton source, GEvent event) { // _CODE_:camera_A_inactive_button:493860:
		PApplet.println("inactive_camera_A_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
		GUI gui = context.gui;
		gui.camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_A_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		context.cameraActiveA = false;
		context.gphotoA.setActive(false);

	} // _CODE_:camera_A_inactive_button:493860:

	public void camera_B_connected_click(GButton source, GEvent event) { // _CODE_:camera_B_connected_button:564189:
		PApplet.println("camera_B_connected_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
		if (!context.gphotoB.isConnected()) {
			context.gphotoBAdapter = context.createG2P5(context.serialCameraB, "B");
		}
	} // _CODE_:camera_B_connected_button:564189:

	public void camera_B_active_click(GButton source, GEvent event) { // _CODE_:camera_B_active_button:640605:
		PApplet.println("camera_B_active_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		context.cameraActiveB = true;
		context.gphotoB.setActive(true);

		if (context.gphotoB.captureRunnable instanceof TetheredMockCaptureRunnable) {
			context.gphotoB.active = true;
		}

	} // _CODE_:camera_B_active_button:640605:

	public void camera_B_inactive_click(GButton source, GEvent event) { // _CODE_:camera_B_inactive_button:780199:
		PApplet.println("camera_B_inactive_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
		GUI gui = context.gui;
		gui.camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		gui.camera_B_inactive_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		context.cameraActiveB = false;
		context.gphotoB.setActive(false);

	} // _CODE_:camera_B_inactive_button:780199:

	public void parameters_click(GButton source, GEvent event) { // _CODE_:parameters_button:465510:
		PApplet.println("parameters_button - GButton >> GEvent." + event + " @ " + context.parent.millis());
	} // _CODE_:parameters_button:465510:

	public void load_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		load_click();

	} // _CODE_:load_button:841968:

	public void load_click() { // _CODE_:load_button:841968:
		String documentFileName = G4P.selectInput("Load XML");
		if (documentFileName != null) {
			context.parent.loading = true;
			context.parent.loadProject(documentFileName);
			context.parent.loading = false;
		} else {
			context.parent.loading = false;
		}

	} // _CODE_:load_button:841968:

	public void edit_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		context.gui.grpProject.setVisible(1, true);
		context.gui.grpAll.setVisible(1, false);
		context.parent.editingProject = true;
		

	} // _CODE_:load_button:841968:

	public void close_click(GButton source, GEvent event) { // _CODE_:load_button:841968:
		context.parent.stateApp = ManuCapture_v1_1.STATE_APP_NO_PROJECT;

	} // _CODE_:load_button:841968:

	public void new_button_click(GButton source, GEvent event) { // _CODE_:new_button:324180:
		String projectFolderPath = G4P.selectFolder("Select the project folder for NEW PROJECT");
		if (projectFolderPath != null) {
			context.project.thumbnailsLoaded = false;
			context.gui.grpProject.setVisible(1, true);
			context.parent.editingProject = true;
			context.parent.createProject(projectFolderPath);
			context.gui.project_info.setText("PROJECT INFO " + context.project.projectFilePath);
		}
	} // _CODE_:new_button:324180:

	public void page_search_text_change(GTextField source, GEvent event) { // _CODE_:page_search_text:741750:
		PApplet.println("textfield2 - GTextField >> GEvent." + event + " @ " + context.parent.millis());
	} // _CODE_:page_search_text:741750:

	public void liveView_button_click(GButton source, GEvent event) { // _CODE_:export_button:581416:
		// PApplet.println("export_button - GButton >> GEvent." + event + " @ "
		// +
		// millis());
		context.parent.liveViewActive = 0;

	} // _CODE_:export_button:581416:

}
