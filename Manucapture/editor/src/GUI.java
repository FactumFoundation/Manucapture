import java.awt.Font;

import g4p_controls.G4P;
import g4p_controls.GButton;
import g4p_controls.GCScheme;
import g4p_controls.GGroup;
import g4p_controls.GImageButton;
import g4p_controls.GImageToggleButton;
import g4p_controls.GLabel;
import g4p_controls.GTextArea;
import g4p_controls.GTextField;

public class GUI {
	

	ManuCapture_v1_1 context;
	GUIController guiController;

	GGroup grpAll;
	GImageButton btnTrigger;
	GImageButton btnOpenSOViewerLeft;
	GImageButton btnOpenSOViewerRight;
	GImageButton btnConnectedCameraPageRight;
	GImageButton btnConnectedCameraPageLeft;
	GImageButton btnEdit;
	GImageButton btnClose;
	GImageButton btnFirstPage;
	GImageButton btnLastPage;
	
	GImageToggleButton btnCrop;
	GImageToggleButton btnRepeat;
	GImageToggleButton btnColorChart;
	GImageToggleButton btnLiveView;
	GTextArea t;

	GGroup grpProject;
	GLabel project_info;
	GLabel code_label;
	GTextField code_text;
	GButton btnOK;
	
	public GUI() {
	}

	// Create all the GUI controls.
	public void createGUI(ManuCapture_v1_1 context) {
		this.context = context;
		this.guiController = context.guiController;
		this.context = context;

		grpAll = new GGroup(context);
		grpProject = new GGroup(context);

		String[] files;

		int buttonsDeltaY = 190;

		btnLiveView = new GImageToggleButton(context, context.width-168, context.height - 5*buttonsDeltaY, "btn_live_view_toggle.png", 2, 1);
		btnLiveView.addEventHandler(guiController, "liveView_button_click");
		grpAll.addControls(btnLiveView);

		btnColorChart = new GImageToggleButton(context, context.width-168, context.height - 4*buttonsDeltaY, "btn_calibrate_toggle.png", 2, 1);
		btnColorChart.addEventHandler(guiController,"calibration_shutter_click");
		grpAll.addControls(btnColorChart);
		
		btnCrop = new GImageToggleButton(context, context.width-168, context.height - 3*buttonsDeltaY, "btn_crop_toggle.png", 2, 1);
		btnCrop.addEventHandler(guiController, "crop_click");
		grpAll.addControls(btnCrop);
		
		btnRepeat = new GImageToggleButton(context, context.width-168, context.height - 2*buttonsDeltaY, "btn_repeat_toggle.png", 2, 1);
		btnRepeat.addEventHandler(guiController,"repeat_shutter_click");
		btnRepeat.setVisible(false);
		grpAll.addControls(btnRepeat);
	
		files = new String[] { "btn_trigger_small.png", "btn_trigger_small.png", "btn_trigger_small.png" };
		btnTrigger = new GImageButton(context, context.width-168, context.height - buttonsDeltaY, files);
		btnTrigger.addEventHandler(guiController, "trigger_button_click");
		grpAll.addControls(btnTrigger);

		files = new String[] { "btn_connected_narrow.png", "btn_connected_narrow.png", "btn_connected_narrow.png" };
		btnConnectedCameraPageRight = new GImageButton(context, context.width-158, 40, files);
		btnConnectedCameraPageRight.addEventHandler(guiController,"camera_page_right_connect_button_click");
		grpAll.addControls(btnConnectedCameraPageRight);

		files = new String[] { "btn_connected_narrow.png", "btn_connected_narrow.png", "btn_connected_narrow.png" };
		btnConnectedCameraPageLeft = new GImageButton(context, context.width-88, 40, files);
		btnConnectedCameraPageLeft.addEventHandler(guiController,"camera_page_left_connect_button_click");
		grpAll.addControls(btnConnectedCameraPageLeft);
		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnOpenSOViewerLeft = new GImageButton(context, context.contentGUI.leftImageMarginLeft, 5,90,90,files);
		btnOpenSOViewerLeft.addEventHandler(guiController, "openViewer_Left");
		grpAll.addControls(btnOpenSOViewerLeft);
		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnOpenSOViewerRight = new GImageButton(context, context.contentGUI.rightImageMarginLeft, 5,90,90,files);
		btnOpenSOViewerRight.addEventHandler(guiController, "openViewer_Right");
		grpAll.addControls(btnOpenSOViewerRight);
				
		files = new String[] { "btn_info.png", "btn_info.png", "btn_info.png" };
		btnEdit = new GImageButton(context, 148 +30,40, files);
		btnEdit.addEventHandler(guiController,"edit_click");
		grpAll.addControls(btnEdit);
		
		files = new String[] { "btn_close.png", "btn_close.png", "btn_close.png" };
		btnClose = new GImageButton(context, -15 + 30,40, files);
		btnClose.addEventHandler(guiController,"close_click");
		grpAll.addControls(btnClose);
		
		files = new String[] { "btn_first_page.png", "btn_first_page.png", "btn_first_page.png" };
		btnFirstPage = new GImageButton(context,-2 +30,980, files);
		btnFirstPage.addEventHandler(guiController,"first_page_button_click");
		grpAll.addControls(btnFirstPage);
		
		files = new String[] { "btn_last_page.png", "btn_last_page.png", "btn_last_page.png" };
		btnLastPage = new GImageButton(context,127+30,980, files);
		btnLastPage.addEventHandler(guiController,"last_page_button_click");
		grpAll.addControls(btnLastPage);
		
		btnLiveView.setAlpha(200);
		btnCrop.setAlpha(200);
		btnTrigger.setAlpha(220);
		btnColorChart.setAlpha(180);
		btnRepeat.setAlpha(180);
		btnConnectedCameraPageLeft.setAlpha(180);
		btnConnectedCameraPageRight.setAlpha(180);
		
		createProjectControls();
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.YELLOW_SCHEME);
		G4P.setCursor(G4P.ARROW);
	}

	private void createProjectControls() {

		int fieldW = 500;
		int fieldH = 40;
		int labelX = context.width / 3 -90;
		int labelW = 200;
		int labelH = 40;
		int fieldX = labelX + labelW + 10;
		int fieldY = 140;

		project_info = new GLabel(context, labelX - labelW / 2, fieldY, fieldW * 2, labelH);
		project_info.setText("PROJECT INFO ");
		project_info.setTextBold();
		project_info.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_info.setOpaque(true);
		
		code_label = new GLabel(context, labelX+40, 70 + fieldY, labelW, labelH);
		code_label.setText("Code:");
		code_label.setTextBold();
		code_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_label.setOpaque(true);
		code_text = new GTextField(context, fieldX+40, 70 + fieldY, fieldW, fieldH, G4P.SCROLLBARS_NONE);
		code_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_text.setOpaque(true);
		code_text.setText(context.project.projectCode);
		code_text.addEventHandler(guiController, "code_text_change");
		btnOK = new GButton(context, fieldX+40, 140+fieldY, 300, 130);
		btnOK.setText("OK!");
		btnOK.addEventHandler(guiController, "close_popup_project");

		Font font = new Font("Verdana", Font.BOLD, 25);
		code_label.setFont(font);
		code_text.setFont(font);
		btnOK.setFont(font);
		Font sectionFont = new Font("Verdana", Font.BOLD, 18);
		project_info.setFont(sectionFont);
		grpProject.addControls(btnOK, project_info, code_label, code_text);
		grpProject.setVisible(0, false);
	}

}