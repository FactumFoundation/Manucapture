import java.awt.Font;

import g4p_controls.G4P;
import g4p_controls.GAlign;
import g4p_controls.GButton;
import g4p_controls.GCScheme;
import g4p_controls.GGroup;
import g4p_controls.GImageButton;
import g4p_controls.GImageToggleButton;
import g4p_controls.GLabel;
import g4p_controls.GPanel;
import g4p_controls.GTextArea;
import g4p_controls.GTextField;
import g4p_controls.GWindow;
import processing.core.PApplet;

public class GUI {
	

	ManuCapture_v1_1 context;
	GUIController guiController;

	GGroup grpAll;
	GImageButton btnTrigger;
	GImageButton btnTriggerNormal;
	GImageButton btnTriggerCrop;
	GImageButton btnTriggerOpenSOViewer1;
	GImageButton btnTriggerOpenSOViewer2;
	GImageButton btnTriggerRepeat;
	GImageButton btnTriggerChartColor;
	GImageButton btnConnectedA;
	GImageButton btnConnectedB;
	GImageButton btnEdit;
	GImageButton btnClose;
	GImageButton btnFirstPage;
	GImageButton btnLastPage;
	GImageButton btnLiveView;
	
	GGroup grpProject;
	GTextField name_text;
	GLabel project_info;
	GLabel name;
	GLabel code_label;
	GTextField code_text;
	GLabel author_label;
	GLabel project_comments_label;
	GTextField author_text;
	GTextField project_comments_text;
	GButton btnOK;
	
	GWindow window;

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

		files = new String[] { "btn_live_view.png", "btn_live_view.png", "btn_live_view.png" };
		btnLiveView = new GImageButton(context, context.width-168, context.height - 5*buttonsDeltaY, files);
		btnLiveView.addEventHandler(guiController, "liveView_button_click");
		grpAll.addControls(btnLiveView);

		files = new String[] { "btn_calibrate.png", "btn_calibrate.png", "btn_calibrate.png" };
		btnTriggerChartColor = new GImageButton(context, context.width-168, context.height - 4*buttonsDeltaY, files);
		btnTriggerChartColor.addEventHandler(guiController,"calibration_shutter_click");
		grpAll.addControls(btnTriggerChartColor);
		
		files = new String[] { "btn_crop.png", "btn_crop.png", "btn_crop.png" };
		btnTriggerCrop = new GImageButton(context, context.width-168, context.height - 3*buttonsDeltaY, files);
		btnTriggerCrop.addEventHandler(guiController, "crop_click");
		grpAll.addControls(btnTriggerCrop);
				
		files = new String[] { "btn_normal.png", "btn_normal.png", "btn_normal.png" };
		btnTriggerNormal = new GImageButton(context, context.width-168, context.height - 2*buttonsDeltaY, files);
		btnTriggerNormal.addEventHandler(guiController, "repeat_shutter_click");
		grpAll.addControls(btnTriggerNormal);
		
		files = new String[] { "btn_repeat.png", "btn_repeat.png", "btn_repeat.png" };
		btnTriggerRepeat = new GImageButton(context, context.width-168, context.height - 2*buttonsDeltaY, files);
		btnTriggerRepeat.addEventHandler(guiController,"normal_shutter_click1");
		btnTriggerRepeat.setVisible(false);
		grpAll.addControls(btnTriggerRepeat);
	
		files = new String[] { "btn_trigger_small.png", "btn_trigger_small.png", "btn_trigger_small.png" };
		btnTrigger = new GImageButton(context, context.width-168, context.height - buttonsDeltaY, files);
		btnTrigger.addEventHandler(guiController, "trigger_button_click");
		grpAll.addControls(btnTrigger);

		
	
		files = new String[] { "btn_connected_narrow.png", "btn_connected_narrow.png", "btn_connected_narrow.png" };
		btnConnectedA = new GImageButton(context, context.width-158, 40, files);
		btnConnectedA.addEventHandler(guiController,"camera_A_connect_button_click");
		grpAll.addControls(btnConnectedA);

		files = new String[] { "btn_connected_narrow.png", "btn_connected_narrow.png", "btn_connected_narrow.png" };
		btnConnectedB = new GImageButton(context, context.width-88, 40, files);
		btnConnectedB.addEventHandler(guiController,"camera_B_connect_button_click");
		grpAll.addControls(btnConnectedB);

		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnTriggerOpenSOViewer1 = new GImageButton(context, context.contentGUI.marginLeftViewer, 5,90,90,files);
		btnTriggerOpenSOViewer1.addEventHandler(guiController, "openViewer_1");
		grpAll.addControls(btnTriggerOpenSOViewer1);
		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnTriggerOpenSOViewer2 = new GImageButton(context, context.contentGUI.marginRightViewer, 5,90,90,files);
		btnTriggerOpenSOViewer2.addEventHandler(guiController, "openViewer_2");
		grpAll.addControls(btnTriggerOpenSOViewer2);
		
		
		
		
		files = new String[] { "btn_edit.png", "btn_edit.png", "btn_edit.png" };
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
		btnTriggerCrop.setAlpha(200);
		btnTrigger.setAlpha(220);
		btnTriggerChartColor.setAlpha(180);
		btnTriggerNormal.setAlpha(180);
		btnTriggerRepeat.setAlpha(180);
		btnConnectedB.setAlpha(180);
		btnConnectedA.setAlpha(180);
		
		
		Font font = new Font("Verdana", Font.BOLD, 10);

		createProjectControls();
		
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.YELLOW_SCHEME);
		G4P.setCursor(context.ARROW);

	}

	private void createProjectControls() {
		
		btnOK = new GButton(context, context.width / 2 - 250, 500, 300, 130);
		btnOK.setText("OK!");
		btnOK.addEventHandler(guiController, "close_popup_project");

		int fieldW = 500;
		int fieldH = 40;

		int labelX = context.width / 3;
		int labelY = 40;

		int labelW = 200;
		int labelH = 40;

		int fieldX = labelX + labelW + 10;
		int fieldY = 140;

		name_text = new GTextField(context, fieldX, 106 + fieldY, fieldW, fieldH, G4P.SCROLLBARS_NONE);
		name_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		name_text.setOpaque(true);
		name_text.addEventHandler(guiController, "name_text_change");
		name_text.setText(context.project.projectName);
		project_info = new GLabel(context, labelX - labelW / 2, fieldY, fieldW * 2, labelH);
		project_info.setText("PROJECT INFO ");
		project_info.setTextBold();
		project_info.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_info.setOpaque(true);

		name = new GLabel(context, labelX, 106 + fieldY, labelW, labelH);
		name.setText("Name:");
		name.setTextBold();
		name.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		name.setOpaque(true);
		code_label = new GLabel(context, labelX, 156 + fieldY, labelW, labelH);
		code_label.setText("Code:");
		code_label.setTextBold();
		code_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_label.setOpaque(true);
		code_text = new GTextField(context, fieldX, 156 + fieldY, fieldW, fieldH, G4P.SCROLLBARS_NONE);
		code_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		code_text.setOpaque(true);
		code_text.setText(context.project.projectCode);
		code_text.addEventHandler(guiController, "code_text_change");
		author_label = new GLabel(context, labelX, 206 + fieldY, labelW, labelH);
		author_label.setText("Author:");
		author_label.setTextBold();
		author_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		author_label.setOpaque(true);

		author_text = new GTextField(context, fieldX, 206 + fieldY, fieldW, fieldH, G4P.SCROLLBARS_NONE);
		author_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		author_text.setOpaque(true);
		author_text.setText(context.project.projectAuthor);
		author_text.addEventHandler(guiController, "author_text_change");

		project_comments_text = new GTextField(context, fieldX, 256 + fieldY, fieldW, fieldH, G4P.SCROLLBARS_NONE);
		project_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_comments_text.setOpaque(true);
		project_comments_text.setText(context.project.projectComment);
		project_comments_text.addEventHandler(guiController, "project_comments_change");

		project_comments_label = new GLabel(context, labelX, 256 + fieldY, labelW, labelH);
		project_comments_label.setText("Comments:");
		project_comments_label.setTextBold();
		project_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		project_comments_label.setOpaque(true);

		Font font = new Font("Verdana", Font.BOLD, 25);
		name.setFont(font);
		code_label.setFont(font);
		author_label.setFont(font);
		project_comments_label.setFont(font);

		project_comments_text.setFont(font);
		author_text.setFont(font);
		code_text.setFont(font);
		name_text.setFont(font);

		btnOK.setFont(font);

		Font sectionFont = new Font("Verdana", Font.BOLD, 18);
		project_info.setFont(sectionFont);

		grpProject.addControls(btnOK, name_text, project_info, name, code_label, code_text, author_label);
		grpProject.addControls(author_text, project_comments_text, project_comments_label);
		grpProject.setVisible(0, false);
	}

}