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
	public GButton first_page_button;
	public GButton last_page_button;
	public GTextField name_text;
	public GLabel project_info;
	public GLabel name;
	public GLabel code_label;
	public GTextField code_text;
	public GLabel author_label;
	public GLabel project_comments_label;
	public GLabel page_info_label;
	public GLabel page_comments_label;
	public GLabel number_label;
	public GLabel shutter_control_label;
	public GLabel camera_config_label;
	public GLabel camera_A_label;
	public GTextField author_text;
	public GTextField project_comments_text;
	public GLabel camera_B_label;
	public GTextField page_comments_text;
	public GTextField page_num_text;
	public GButton normal_shutter_button;
	public GButton repeat_shutter_button;
	// public GButton subpage_shutter_button;
	public GButton calibration_shutter_button;
	public GButton trigger_button;
	public GButton camera_A_connected_button;
	public GButton camera_A_active_button;
	public GButton camera_A_inactive_button;
	public GButton camera_B_connected_button;
	public GButton camera_B_active_button;
	public GButton camera_B_inactive_button;
	public GButton parameters_button;
	public GButton load_button;
	public GButton edit_button;
	public GButton close_button;
	public GButton new_button;
	public GTextField page_search_text;
	public GLabel page_search_label;
	public GButton liveView_button;

	// Group 2 controls
	GWindow window;
	GImageToggleButton grp2_a;
	GButton grp2_b;
	GPanel grp2_c;
	GTextArea grp2_textArea;
	GLabel grp2_d;
	GImageButton grp2_e;

	ManuCapture_v1_1 context;

	GGroup grpAll;
	GGroup grpProject;

	GUIController guiController;

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

	public GUI() {
	}

	// Create all the GUI controls.
	public void createGUI(ManuCapture_v1_1 context) {
		
		this.context = context;
		this.guiController = context.guiController;
		this.context = context;

		grpAll = new GGroup(context);
		grpProject = new GGroup(context);

		first_page_button = new GButton(context, 11, 90, 122, 24);
		first_page_button.setText("FIRST PAGE");
		first_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		first_page_button.addEventHandler(guiController, "first_page_button_click");
		first_page_button.setVisible(false);
		last_page_button = new GButton(context, 141, 90, 122, 24);
		last_page_button.setText("LAST PAGE");
		last_page_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		last_page_button.addEventHandler(guiController, "last_page_button_click");
		last_page_button.setVisible(false);
		page_info_label = new GLabel(context, 300, 276, 80, 20);
		page_info_label.setText("PAGE INFO");
		page_info_label.setTextBold();
		page_info_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_info_label.setOpaque(true);
		page_info_label.setVisible(false);
		page_comments_label = new GLabel(context, 302, 326, 80, 20);
		page_comments_label.setText("Comments:");
		page_comments_label.setTextBold();
		page_comments_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_comments_label.setOpaque(true);
		page_comments_label.setVisible(false);
		number_label = new GLabel(context, 302, 410, 80, 20);
		number_label.setText("Number:");
		number_label.setTextBold();
		number_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		number_label.setOpaque(true);
		number_label.setVisible(false);
		shutter_control_label = new GLabel(context, 300, 450, 200, 24);
		shutter_control_label.setText("SHUTTER CONTROL:");
		shutter_control_label.setTextBold();
		shutter_control_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		shutter_control_label.setOpaque(true);
		shutter_control_label.setVisible(false);
		camera_config_label = new GLabel(context, 300, 684, 200, 20);
		camera_config_label.setText("CAMERA CONFIGURATION:");
		camera_config_label.setTextBold();
		camera_config_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_config_label.setOpaque(true);
		camera_config_label.setVisible(false);
		camera_A_label = new GLabel(context, 407, 719, 80, 20);
		camera_A_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		camera_A_label.setText("CAMERA A");
		camera_A_label.setTextBold();
		camera_A_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_label.setOpaque(true);
		camera_A_label.setVisible(false);
		camera_B_label = new GLabel(context, 407, 852, 80, 20);
		camera_B_label.setTextAlign(GAlign.CENTER, GAlign.MIDDLE);
		camera_B_label.setText("CAMERA B");
		camera_B_label.setTextBold();
		camera_B_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_label.setOpaque(true);
		camera_B_label.setVisible(false);
		page_comments_text = new GTextField(context, 380, 326, 200, 80, G4P.SCROLLBARS_NONE);
		page_comments_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_comments_text.setOpaque(true);
		page_comments_text.addEventHandler(guiController, "page_comments_text_change");
		page_comments_text.setVisible(false);
		page_num_text = new GTextField(context, 380, 410, 200, 20, G4P.SCROLLBARS_NONE);
		page_num_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_num_text.setOpaque(true);
		page_num_text.addEventHandler(guiController, "page_num_text_change");
		page_num_text.setVisible(false);
		normal_shutter_button = new GButton(context, 386, 488, 122, 24);
		normal_shutter_button.setText("NORMAL");
		normal_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		normal_shutter_button.addEventHandler(guiController, "normal_shutter_click1");
		normal_shutter_button.setVisible(false);
		repeat_shutter_button = new GButton(context, 386, 517, 122, 24);
		repeat_shutter_button.setText("REPEAT");
		repeat_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		repeat_shutter_button.addEventHandler(guiController, "repeat_shutter_click");
		repeat_shutter_button.setVisible(false);
		calibration_shutter_button = new GButton(context, 386, 577, 122, 24);
		calibration_shutter_button.setText("CALIBRATION");
		calibration_shutter_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		calibration_shutter_button.addEventHandler(guiController, "calibration_shutter_click");
		calibration_shutter_button.setVisible(false);
		trigger_button = new GButton(context, 386, 619, 122, 48);
		trigger_button.setText("TRIGGER");
		trigger_button.setLocalColorScheme(GCScheme.PURPLE_SCHEME);
		trigger_button.addEventHandler(guiController, "trigger_button_click");
		trigger_button.setVisible(false);
		camera_A_connected_button = new GButton(context, 386, 746, 122, 24);
		camera_A_connected_button.setText("DISCONNECTED");
		camera_A_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		camera_A_connected_button.addEventHandler(guiController, "camera_A_connected_click");
		camera_A_connected_button.setVisible(false);
		camera_A_connected_button.setVisible(false);
		camera_A_active_button = new GButton(context, 385, 779, 122, 24);
		camera_A_active_button.setText("ACTIVE");
		camera_A_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_active_button.addEventHandler(guiController, "camera_A_active_button_click");
		camera_A_active_button.setVisible(false);
		camera_A_inactive_button = new GButton(context, 386, 809, 122, 24);
		camera_A_inactive_button.setText("INACTIVE");
		camera_A_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_A_inactive_button.addEventHandler(guiController, "camera_A_inactive_button_click");
		camera_A_inactive_button.setVisible(false);
		camera_B_connected_button = new GButton(context, 386, 879, 122, 24);
		camera_B_connected_button.setText("DISCONNECTED");
		camera_B_connected_button.setLocalColorScheme(GCScheme.RED_SCHEME);
		camera_B_connected_button.addEventHandler(guiController, "camera_B_connected_click");
		camera_B_connected_button.setVisible(false);
		camera_B_active_button = new GButton(context, 386, 915, 122, 24);
		camera_B_active_button.setText("ACTIVE");
		camera_B_active_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_active_button.addEventHandler(guiController, "camera_B_active_click");
		camera_B_active_button.setVisible(false);
		camera_B_inactive_button = new GButton(context, 386, 945, 122, 24);
		camera_B_inactive_button.setText("INACTIVE");
		camera_B_inactive_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		camera_B_inactive_button.addEventHandler(guiController, "camera_B_inactive_click");
		camera_B_inactive_button.setVisible(false);
		parameters_button = new GButton(context, 386, 994, 122, 24);
		parameters_button.setText("PARAMETERS");
		parameters_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		parameters_button.addEventHandler(guiController, "parameters_click");
		parameters_button.setVisible(false);
		load_button = new GButton(context, -111, 11, 80, 24);
		load_button.setText("LOAD");
		load_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		load_button.addEventHandler(guiController, "load_click");
		edit_button = new GButton(context, 219, 11, 80, 24);
		edit_button.setText("EDIT");
		edit_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		edit_button.addEventHandler(guiController, "edit_click");
		edit_button.setVisible(false);
		
		close_button = new GButton(context, 289, 11, 80, 24);
		close_button.setText("CLOSE");
		close_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		close_button.addEventHandler(guiController, "close_click");
		close_button.setVisible(false);
		
		new_button = new GButton(context, -199, 11, 122, 24);
		new_button.setText("NEW PROJECT");
		new_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		new_button.addEventHandler(guiController, "new_button_click");
		page_search_text = new GTextField(context, 90, 57, 175, 20, G4P.SCROLLBARS_NONE);
		page_search_text.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_search_text.setOpaque(true);
		page_search_text.addEventHandler(guiController, "page_search_text_change");
		page_search_text.setVisible(false);
		page_search_label = new GLabel(context, 10, 57, 80, 20);
		page_search_label.setText("Page search:");
		page_search_label.setTextBold();
		page_search_label.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		page_search_label.setOpaque(true);
		page_search_label.setVisible(false);
		liveView_button = new GButton(context, 490, 11, 80, 24);
		liveView_button.setText("LIVEVIEW");
		liveView_button.setLocalColorScheme(GCScheme.CYAN_SCHEME);
		liveView_button.addEventHandler(guiController, "liveView_button_click");
		liveView_button.setVisible(false);

		grpAll.addControls( load_button, 
				new_button);/* page_search_text, page_search_label, liveView_button edit_button,parameters_button,*/

		String[] files;
		files = new String[] { "btn_trigger.png", "btn_trigger.png", "btn_trigger.png" };
		btnTrigger = new GImageButton(context, context.marginLeftViewerRight - 170, context.height - 200, files);
		grpAll.addControls(btnTrigger);

		files = new String[] { "btn_live_view.png", "btn_live_view.png", "btn_live_view.png" };
		btnLiveView = new GImageButton(context, context.marginLeftViewerRight - 130, 680, files);
		grpAll.addControls(btnLiveView);
		
		files = new String[] { "btn_crop.png", "btn_crop.png", "btn_crop.png" };
		btnTriggerCrop = new GImageButton(context, context.marginLeftViewerRight - 130, 540, files);
		grpAll.addControls(btnTriggerCrop);
		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnTriggerOpenSOViewer1 = new GImageButton(context, context.marginLeftViewerLeft+ 270, 35,90,90,files);
		grpAll.addControls(btnTriggerOpenSOViewer1);
		
		files = new String[] { "btn_viewer.png", "btn_viewer.png", "btn_viewer.png" };
		btnTriggerOpenSOViewer2 = new GImageButton(context, context.marginLeftViewerRight + 270, 35,90,90,files);
		grpAll.addControls(btnTriggerOpenSOViewer2);
		

		files = new String[] { "btn_normal.png", "btn_normal.png", "btn_normal.png" };
		btnTriggerNormal = new GImageButton(context, context.marginLeftViewerRight + 50, context.height - 120, files);
		grpAll.addControls(btnTriggerNormal);
		
		files = new String[] { "btn_repeat.png", "btn_repeat.png", "btn_repeat.png" };
		btnTriggerRepeat = new GImageButton(context, context.marginLeftViewerRight + 50, context.height - 120, files);
		grpAll.addControls(btnTriggerRepeat);
		btnTriggerRepeat.setVisible(false);

		files = new String[] { "btn_calibrate.png", "btn_calibrate.png", "btn_calibrate.png" };
		btnTriggerChartColor = new GImageButton(context, context.marginLeftViewerRight - 310, context.height - 120, files);
		grpAll.addControls(btnTriggerChartColor);

		files = new String[] { "btn_connected.png", "btn_connected.png", "btn_connected.png" };
		btnConnectedA = new GImageButton(context, context.marginLeftViewerLeft, 0, files);
		grpAll.addControls(btnConnectedA);

		files = new String[] { "btn_connected.png", "btn_connected.png", "btn_connected.png" };
		btnConnectedB = new GImageButton(context, context.marginLeftViewerRight, 0, files);
		grpAll.addControls(btnConnectedB);
		
		files = new String[] { "btn_edit.png", "btn_edit.png", "btn_edit.png" };
		btnEdit = new GImageButton(context, 148 +30,70, files);
		grpAll.addControls(btnEdit);
		
		files = new String[] { "btn_close.png", "btn_close.png", "btn_close.png" };
		btnClose = new GImageButton(context, -15 + 30,70, files);
		grpAll.addControls(btnClose);
		
		files = new String[] { "btn_first_page.png", "btn_first_page.png", "btn_first_page.png" };
		btnFirstPage = new GImageButton(context,-2 +30,1010, files);
		grpAll.addControls(btnFirstPage);
		
		files = new String[] { "btn_last_page.png", "btn_last_page.png", "btn_last_page.png" };
		btnLastPage = new GImageButton(context,127+30,1010, files);
		grpAll.addControls(btnLastPage);
		
		Font font = new Font("Verdana", Font.BOLD, 10);

		page_comments_label.setFont(font);
		number_label.setFont(font);
		camera_A_label.setFont(font);
		camera_B_label.setFont(font);
		page_search_label.setFont(font);

		Font sectionFont = new Font("Verdana", Font.BOLD, 12);
		page_info_label.setFont(sectionFont);
		shutter_control_label.setFont(sectionFont);
		camera_config_label.setFont(sectionFont);

		normal_shutter_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_A_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		camera_B_active_button.setLocalColorScheme(GCScheme.ORANGE_SCHEME);
		
		createGroup2Controls();
		
		G4P.messagesEnabled(false);
		G4P.setGlobalColorScheme(GCScheme.YELLOW_SCHEME);
		G4P.setCursor(context.ARROW);

	}

	private void createGroup2Controls() {
		
		grp2_b = new GButton(context, context.width / 2 - 250, 500, 300, 130);
		grp2_b.setText("OK!");
		grp2_b.addEventHandler(guiController, "close_popup_project");

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

		grp2_b.setFont(font);

		Font sectionFont = new Font("Verdana", Font.BOLD, 18);
		project_info.setFont(sectionFont);

		grpProject.addControls(grp2_b, name_text, project_info, name, code_label, code_text, author_label);
		grpProject.addControls(author_text, project_comments_text, project_comments_label);
		grpProject.setVisible(0, false);
	}

}