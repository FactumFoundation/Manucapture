#include "ofApp.h"
//--------------------------------------------------------------


#define OFX_EDSDK_BUFFER_SIZE 1


static void
ctx_error_func (GPContext *context, const char *str, void *data)
{
        fprintf  (stderr, "\n*** Contexterror ***              \n%s\n",str);
        fflush   (stderr);
}

static void
ctx_status_func (GPContext *context, const char *str, void *data)
{
        fprintf  (stderr, "%s\n", str);
        fflush   (stderr);
}

GPContext* sample_create_context() {
    GPContext *context;

    /* This is the mandatory part */
    context = gp_context_new();

    /* All the parts below are optional! */
        gp_context_set_error_func (context, ctx_error_func, NULL);
        gp_context_set_status_func (context, ctx_status_func, NULL);

    return context;
}

static GPPortInfoList		*portinfolist = NULL;
static CameraAbilitiesList	*abilities = NULL;

int sample_autodetect (CameraList *list, GPContext *context) {
    gp_list_reset (list);
        return gp_camera_autodetect (list, context);
}

/*
 * This function opens a camera depending on the specified model and port.
 */
int
sample_open_camera (Camera ** camera, const char *model, const char *port, GPContext *context) {
    int		ret, m, p;
    CameraAbilities	a;
    GPPortInfo	pi;

    ret = gp_camera_new (camera);
    if (ret < GP_OK) return ret;

    if (!abilities) {
        /* Load all the camera drivers we have... */
        ret = gp_abilities_list_new (&abilities);
        if (ret < GP_OK) return ret;
        ret = gp_abilities_list_load (abilities, context);
        if (ret < GP_OK) return ret;
    }

    /* First lookup the model / driver */
        m = gp_abilities_list_lookup_model (abilities, model);
    if (m < GP_OK) return ret;
        ret = gp_abilities_list_get_abilities (abilities, m, &a);
    if (ret < GP_OK) return ret;
        ret = gp_camera_set_abilities (*camera, a);
    if (ret < GP_OK) return ret;

    if (!portinfolist) {
        /* Load all the port drivers we have... */
        ret = gp_port_info_list_new (&portinfolist);
        if (ret < GP_OK) return ret;
        ret = gp_port_info_list_load (portinfolist);
        if (ret < 0) return ret;
        ret = gp_port_info_list_count (portinfolist);
        if (ret < 0) return ret;
    }

    /* Then associate the camera with the specified port */
        p = gp_port_info_list_lookup_path (portinfolist, port);
        switch (p) {
        case GP_ERROR_UNKNOWN_PORT:
                fprintf (stderr, "The port you specified "
                        "('%s') can not be found. Please "
                        "specify one of the ports found by "
                        "'gphoto2 --list-ports' and make "
                        "sure the spelling is correct "
                        "(i.e. with prefix 'serial:' or 'usb:').",
                                port);
                break;
        default:
                break;
        }
        if (p < GP_OK) return p;

        ret = gp_port_info_list_get_info (portinfolist, p, &pi);
        if (ret < GP_OK) return ret;
        ret = gp_camera_set_port_info (*camera, pi);
        if (ret < GP_OK) return ret;
    return GP_OK;
}

/*
 * This function looks up a label or key entry of
 * a configuration widget.
 * The functions descend recursively, so you can just
 * specify the last component.
 */

static int
_lookup_widget(CameraWidget*widget, const char *key, CameraWidget **child) {
    int ret;
    ret = gp_widget_get_child_by_name (widget, key, child);
    if (ret < GP_OK)
        ret = gp_widget_get_child_by_label (widget, key, child);
    return ret;
}

/* calls the Nikon DSLR or Canon DSLR autofocus method. */
int
camera_eosviewfinder(Camera *camera, GPContext *context, int onoff) {
    CameraWidget		*widget = NULL, *child = NULL;
    CameraWidgetType	type;
    int			ret,val;

    ret = gp_camera_get_config (camera, &widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_get_config failed: %d\n", ret);
        return ret;
    }
    ret = _lookup_widget (widget, "viewfinder", &child);
    if (ret < GP_OK) {
        fprintf (stderr, "lookup 'eosviewfinder' failed: %d\n", ret);
        goto out;
    }

    /* check that this is a toggle */
    ret = gp_widget_get_type (child, &type);
    if (ret < GP_OK) {
        fprintf (stderr, "widget get type failed: %d\n", ret);
        goto out;
    }
    switch (type) {
        case GP_WIDGET_TOGGLE:
        break;
    default:
        fprintf (stderr, "widget has bad type %d\n", type);
        ret = GP_ERROR_BAD_PARAMETERS;
        goto out;
    }

    ret = gp_widget_get_value (child, &val);
    if (ret < GP_OK) {
        fprintf (stderr, "could not get widget value: %d\n", ret);
        goto out;
    }
    val = onoff;
    ret = gp_widget_set_value (child, &val);
    if (ret < GP_OK) {
        fprintf (stderr, "could not set widget value to 1: %d\n", ret);
        goto out;
    }

    ret = gp_camera_set_config (camera, widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "could not set config tree to eosviewfinder: %d\n", ret);
        goto out;
    }
out:
    gp_widget_free (widget);
    return ret;
}

int
camera_auto_focus(Camera *camera, GPContext *context, int onoff) {
    CameraWidget		*widget = NULL, *child = NULL;
    CameraWidgetType	type;
    int			ret,val;

    ret = gp_camera_get_config (camera, &widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_get_config failed: %d\n", ret);
        return ret;
    }
    ret = _lookup_widget (widget, "autofocusdrive", &child);
    if (ret < GP_OK) {
        fprintf (stderr, "lookup 'autofocusdrive' failed: %d\n", ret);
        goto out;
    }

    /* check that this is a toggle */
    ret = gp_widget_get_type (child, &type);
    if (ret < GP_OK) {
        fprintf (stderr, "widget get type failed: %d\n", ret);
        goto out;
    }
    switch (type) {
        case GP_WIDGET_TOGGLE:
        break;
    default:
        fprintf (stderr, "widget has bad type %d\n", type);
        ret = GP_ERROR_BAD_PARAMETERS;
        goto out;
    }

    ret = gp_widget_get_value (child, &val);
    if (ret < GP_OK) {
        fprintf (stderr, "could not get widget value: %d\n", ret);
        goto out;
    }

    val = onoff;

    ret = gp_widget_set_value (child, &val);
    if (ret < GP_OK) {
        fprintf (stderr, "could not set widget value to 1: %d\n", ret);
        goto out;
    }

    ret = gp_camera_set_config (camera, widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "could not set config tree to autofocus: %d\n", ret);
        goto out;
    }
out:
    gp_widget_free (widget);
    return ret;
}


/* Manual focusing a camera...
 * xx is -3 / -2 / -1 / 0 / 1 / 2 / 3
 */
int
camera_manual_focus (Camera *camera, int xx, GPContext *context) {
    CameraWidget		*widget = NULL, *child = NULL;
    CameraWidgetType	type;
    int			ret;
    float			rval;
    char			*mval;

    ret = gp_camera_get_config (camera, &widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_get_config failed: %d\n", ret);
        return ret;
    }
    ret = _lookup_widget (widget, "manualfocusdrive", &child);
    if (ret < GP_OK) {
        fprintf (stderr, "lookup 'manualfocusdrive' failed: %d\n", ret);
        goto out;
    }

    /* check that this is a toggle */
    ret = gp_widget_get_type (child, &type);
    if (ret < GP_OK) {
        fprintf (stderr, "widget get type failed: %d\n", ret);
        goto out;
    }
    switch (type) {
        case GP_WIDGET_RADIO: {
        int choices = gp_widget_count_choices (child);

        ret = gp_widget_get_value (child, &mval);
        if (ret < GP_OK) {
            fprintf (stderr, "could not get widget value: %d\n", ret);
            goto out;
        }
        if (choices == 7) { /* see what Canon has in EOS_MFDrive */
            ret = gp_widget_get_choice (child, xx+4, (const char**)&mval);
            if (ret < GP_OK) {
                fprintf (stderr, "could not get widget choice %d: %d\n", xx+2, ret);
                goto out;
            }
            fprintf(stderr,"manual focus %d -> %s\n", xx, mval);
        }
        ret = gp_widget_set_value (child, mval);
        if (ret < GP_OK) {
            fprintf (stderr, "could not set widget value to 1: %d\n", ret);
            goto out;
        }
        break;
    }
        case GP_WIDGET_RANGE:
        ret = gp_widget_get_value (child, &rval);
        if (ret < GP_OK) {
            fprintf (stderr, "could not get widget value: %d\n", ret);
            goto out;
        }

        switch (xx) { /* Range is on Nikon from -32768 <-> 32768 */
        case -3:	rval = -1024;break;
        case -2:	rval =  -512;break;
        case -1:	rval =  -128;break;
        case  0:	rval =     0;break;
        case  1:	rval =   128;break;
        case  2:	rval =   512;break;
        case  3:	rval =  1024;break;

        default:	rval = xx;	break; /* hack */
        }

        fprintf(stderr,"manual focus %d -> %f\n", xx, rval);

        ret = gp_widget_set_value (child, &rval);
        if (ret < GP_OK) {
            fprintf (stderr, "could not set widget value to 1: %d\n", ret);
            goto out;
        }
        break;
    default:
        fprintf (stderr, "widget has bad type %d\n", type);
        ret = GP_ERROR_BAD_PARAMETERS;
        goto out;
    }
    ret = gp_camera_set_config (camera, widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "could not set config tree to autofocus: %d\n", ret);
        goto out;
    }
out:
    gp_widget_free (widget);
    return ret;
}

/*
 * This enables/disables the specific canon capture mode.
 *
 * For non canons this is not required, and will just return
 * with an error (but without negative effects).
 */
int
canon_enable_capture (Camera *camera, int onoff, GPContext *context) {
    CameraWidget		*widget = NULL, *child = NULL;
    CameraWidgetType	type;
    int			ret;

    ret = gp_camera_get_config (camera, &widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_get_config failed: %d\n", ret);
        return ret;
    }
    ret = _lookup_widget (widget, "capture", &child);
    if (ret < GP_OK) {
        /*fprintf (stderr, "lookup widget failed: %d\n", ret);*/
        goto out;
    }

    ret = gp_widget_get_type (child, &type);
    if (ret < GP_OK) {
        fprintf (stderr, "widget get type failed: %d\n", ret);
        goto out;
    }
    switch (type) {
        case GP_WIDGET_TOGGLE:
        break;
    default:
        fprintf (stderr, "widget has bad type %d\n", type);
        ret = GP_ERROR_BAD_PARAMETERS;
        goto out;
    }
    /* Now set the toggle to the wanted value */
    ret = gp_widget_set_value (child, &onoff);
    if (ret < GP_OK) {
        fprintf (stderr, "toggling Canon capture to %d failed with %d\n", onoff, ret);
        goto out;
    }
    /* OK */
    ret = gp_camera_set_config (camera, widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_set_config failed: %d\n", ret);
        return ret;
    }

out:
    gp_widget_free (widget);
    return ret;
}


/* Gets a string configuration value.
 * This can be:
 *  - A Text widget
 *  - The current selection of a Radio Button choice
 *  - The current selection of a Menu choice
 *
 * Sample (for Canons eg):
 *   get_config_value_string (camera, "owner", &ownerstr, context);
 */
int
get_config_value_string (Camera *camera, const char *key, char **str, GPContext *context) {
    CameraWidget		*widget = NULL, *child = NULL;
    CameraWidgetType	type;
    int			ret;
    char			*val;

    ret = gp_camera_get_config (camera, &widget, context);
    if (ret < GP_OK) {
        fprintf (stderr, "camera_get_config failed: %d\n", ret);
        return ret;
    }
    ret = _lookup_widget (widget, key, &child);
    if (ret < GP_OK) {
        fprintf (stderr, "lookup widget failed: %d\n", ret);
        goto out;
    }

    /* This type check is optional, if you know what type the label
     * has already. If you are not sure, better check. */
    ret = gp_widget_get_type (child, &type);
    if (ret < GP_OK) {
        fprintf (stderr, "widget get type failed: %d\n", ret);
        goto out;
    }
    switch (type) {
        case GP_WIDGET_MENU:
        case GP_WIDGET_RADIO:
        case GP_WIDGET_TEXT:
        break;
    default:
        fprintf (stderr, "widget has bad type %d\n", type);
        ret = GP_ERROR_BAD_PARAMETERS;
        goto out;
    }

    /* This is the actual query call. Note that we just
     * a pointer reference to the string, not a copy... */
    ret = gp_widget_get_value (child, &val);
    if (ret < GP_OK) {
        fprintf (stderr, "could not query widget value: %d\n", ret);
        goto out;
    }
    /* Create a new copy for our caller. */
    *str = strdup (val);
out:
    gp_widget_free (widget);
    return ret;
}

int ofApp::init_cameras(){

    CameraList	*list;
    int		ret, i;
    const char	*name, *value;

    canoncontext = sample_create_context ();

    /* Detect all the cameras that can be autodetected... */
    ret = gp_list_new (&list);
    if (ret < GP_OK) return 1;
    cameraCount = sample_autodetect (list, canoncontext);
    if (cameraCount < GP_OK) {
        printf("No cameras detected.\n");
        return 1;
    }

    /* Now open all cameras we autodected for usage */
    printf("Number of cameras: %d\n", cameraCount);
    cams = (Camera **) calloc (sizeof (Camera*),cameraCount);
    liveBufferMiddle = (FixedQueue<ofBuffer*>*) calloc (sizeof(FixedQueue<ofBuffer*>),cameraCount);
    liveBufferFront = (ofBuffer**) calloc (sizeof(ofBuffer*),cameraCount);
    liveBufferBack = (ofBuffer**) calloc (sizeof(ofBuffer*),cameraCount);
    livePixels = (ofPixels** ) calloc (sizeof(ofPixels*),cameraCount);
    liveTexture = (ofTexture** ) calloc (sizeof(ofTexture*),cameraCount);

    for (i = 0; i < cameraCount; i++) {
        gp_list_get_name  (list, i, &name);
        gp_list_get_value (list, i, &value);
        ret = sample_open_camera (&cams[i], name, value, canoncontext);
        if (ret < GP_OK) fprintf(stderr,"Camera %s on port %s failed to open\n", name, value);
        liveBufferMiddle[i].resize(OFX_EDSDK_BUFFER_SIZE);
        for(int j = 0; j < liveBufferMiddle[i].maxSize(); j++) {
            liveBufferMiddle[i][j] = new ofBuffer();
        }
        liveBufferFront[i] = new ofBuffer();
        liveBufferBack[i] = new ofBuffer();
        livePixels[i] = new ofPixels();
        liveTexture[i] = new ofTexture();
    }
}


int ofApp::init_cameras_with_serial_numbers(){

    CameraList	*list;
    int		ret, i;
    const char	*name, *value;

    canoncontext = sample_create_context ();

    /* Detect all the cameras that can be autodetected... */
    ret = gp_list_new (&list);
    if (ret < GP_OK) return 1;
    cameraCount = sample_autodetect (list, canoncontext);
    if (cameraCount < GP_OK) {
        printf("No cameras detected.\n");
        return 1;
    }

    /* Now open all cameras we autodected for usage */
    printf("Number of cameras: %d\n", cameraCount);
    cams = (Camera **) calloc (sizeof (Camera*),cameraCount);
    liveBufferMiddle = (FixedQueue<ofBuffer*>*) calloc (sizeof(FixedQueue<ofBuffer*>),cameraCount);
    liveBufferFront = (ofBuffer**) calloc (sizeof(ofBuffer*),cameraCount);
    liveBufferBack = (ofBuffer**) calloc (sizeof(ofBuffer*),cameraCount);
    livePixels = (ofPixels** ) calloc (sizeof(ofPixels*),cameraCount);
    liveTexture = (ofTexture** ) calloc (sizeof(ofTexture*),cameraCount);

    // Open first camera (LEFT)
    ret = sample_open_camera (&cams[0], name_left, value_left, canoncontext);
    if (ret < GP_OK) fprintf(stderr,"Camera %s on port %s failed to open\n", name_left, value_left);
    liveBufferMiddle[0].resize(OFX_EDSDK_BUFFER_SIZE);
    for(int j = 0; j < liveBufferMiddle[0].maxSize(); j++) {
        liveBufferMiddle[0][j] = new ofBuffer();
    }
    liveBufferFront[0] = new ofBuffer();
    liveBufferBack[0] = new ofBuffer();
    livePixels[0] = new ofPixels();
    liveTexture[0] = new ofTexture();

    // Open secoind camera (RIGHT)
    ret = sample_open_camera (&cams[1], name_right, value_right, canoncontext);
    if (ret < GP_OK) fprintf(stderr,"Camera %s on port %s failed to open\n", name_right, value_right);
    liveBufferMiddle[1].resize(OFX_EDSDK_BUFFER_SIZE);
    for(int j = 0; j < liveBufferMiddle[1].maxSize(); j++) {
        liveBufferMiddle[1][j] = new ofBuffer();
    }
    liveBufferFront[1] = new ofBuffer();
    liveBufferBack[1] = new ofBuffer();
    livePixels[1] = new ofPixels();
    liveTexture[1] = new ofTexture();

}

int ofApp::start_preview(){

    int		ret, i;

    /* Now call a simple function in each of those cameras. */
    for (i = 0; i < cameraCount; i++) {
        printf("Camera init.\n");
        ret = gp_camera_init(cams[i], canoncontext);
        if (ret != GP_OK) {
            printf("  Retval: %d\n", ret);
            return -1;
        }
        canon_enable_capture(cams[i], TRUE, canoncontext);

        ret = camera_eosviewfinder(cams[i],canoncontext,1);
        if (ret != GP_OK) {
            fprintf(stderr,"camera_eosviewfinder(1): %d\n", ret);
            return -1;
        }
    }
    liveDataReady = true;

    return 0;
}


int ofApp::stop_preview(){
    for (int i = 0; i < cameraCount; i++) {
        int retval = camera_eosviewfinder(cams[i],canoncontext,0);
        if (retval != GP_OK) {
            fprintf(stderr,"camera_eosviewfinder(0): %d\n", retval);
            return -1;
        }
        sleep(1);
        gp_camera_exit(cams[i], canoncontext);
    }
    liveDataReady = false;
    return 0;
}



int ofApp::canon_preview(){

    bool reverse = true;
    int index;
    int	i, retval;

    if(liveDataReady){
        frameNum++;
        CameraFile *file;

        for(i=0; i<cameraCount; i++){

            //fprintf(stderr,"preview %d\n", i);
            retval = gp_file_new(&file);
            if (retval != GP_OK) {
                fprintf(stderr,"gp_file_new: %d\n", retval);
                return -1;
            }
            retval = gp_camera_capture_preview(cams[i], file, canoncontext);
            if (retval != GP_OK) {
                fprintf(stderr,"gp_camera_capture_preview(%d): %d\n", i, retval);
                return -1;
            }
            const char* data;
            unsigned long int length = 0;
            retval = gp_file_get_data_and_size(file, &data, &length);
            if (retval != GP_OK) {
                fprintf(stderr,"Error reading live view file to buffer\n", i, retval);
                return -1;
            }
            liveBufferBack[i]->set(data,length);
            swap(liveBufferBack[i], liveBufferMiddle[i].back());
            liveBufferMiddle[i].push();
            if(liveBufferMiddle[i].size() > 0) {
                // decoding the jpeg in the main thread allows the capture thread to run in a tighter loop.
                swap(liveBufferFront[i], liveBufferMiddle[i].front());
                liveBufferMiddle[i].pop();
                ofLoadImage(*livePixels[i], *liveBufferFront[i]);
                //livePixels.rotate90(orientationMode);
                if(liveTexture[i]->getWidth() != livePixels[i]->getWidth() ||
                   liveTexture[i]->getHeight() != livePixels[i]->getHeight()) {
                    liveTexture[i]->allocate(livePixels[i]->getWidth(), livePixels[i]->getHeight(), GL_RGB8);
                }
                liveTexture[i]->loadData(*livePixels[i]);
                ofPushMatrix();

                /*
                if(reverse){

                } else {

                }
                */


                if(!reverse){
                    ofTranslate(i*(liveTexture[i]->getHeight()+5) + liveTexture[i]->getHeight()/2,liveTexture[i]->getWidth()/2);
                    if(i==0){
                        ofRotate(90);
                    } else {
                        ofRotate(270);
                    }
                } else {
                    ofTranslate((1-i)*(liveTexture[i]->getHeight()+5) + liveTexture[i]->getHeight()/2,liveTexture[i]->getWidth()/2);
                    if(i==0){
                        ofRotate(270);
                    } else {
                        ofRotate(90);
                    }
                }


                liveTexture[i]->draw(-liveTexture[i]->getWidth()/2,-liveTexture[i]->getHeight()/2);
                ofPopMatrix();
            }
            if (retval != GP_OK) {
                fprintf(stderr,"gp_camera_capture_preview(%d): %d\n", i, retval);
                return -1;
            }
            gp_file_unref(file);
        }
        return 0;
    }
    else {
        fprintf(stderr,"live view not initializated\n", i, retval);
        return -1;
    }
    return 0;
}



void ofApp::setup(){


    cout << "Starting app " << endl;
    ofSetWindowTitle("ManuCaptureViewer");
    ofSetWindowShape(1285,960);
    ofBackground(50,50,50);

    receiver.setup(3333);
    // open an outgoing connection to HOST:PORT
    sender.setup("127.0.0.1", 3334);
    //sender.sendMessage(m);

    ofTrueTypeFont::setGlobalDpi(72);
    urlFont.load("verdana.ttf", 12, true, true);
    camIDFont.load("verdana.ttf", 36, true, true);

   // name_left = "Canon EOS 700D";
    name_left = "USB PTP Class Camera";
    value_left = "usb:002,011";
    name_right = "Canon EOS 700D";
    value_right = "usb:002,014";

    // Wait to port names sended from mainApp (JAVA)
    // check for waiting messages


    while(true){
        // get the next message
        ofxOscMessage m;
        receiver.getNextMessage(m);
        if(m.getAddress() == "/cameraPorts"){
            cout << "message received" << endl;
            value_left = (char*)m.getArgAsString(0).c_str();
            value_right = (char*)m.getArgAsString(1).c_str();
            break;
        }
    }


    init_cameras_with_serial_numbers();
    start_preview();
    //start_preview_with_serial_numbers();

    patternImage.load("images/pattern.png");

    ofSetWindowPosition(300, 30);
    ofSetWindowShape(1285,960);

}


//--------------------------------------------------------------
void ofApp::update(){

    frameCounter.update();

    // check for waiting messages
    while(receiver.hasWaitingMessages()){
        // get the next message
        ofxOscMessage m;
        receiver.getNextMessage(m);
        cout << "message received" << endl;
        // check for mouse moved message
        if(m.getAddress() == "/stopPreview"){
            stop_preview();
            std::exit(0);
        }
    }
}


//--------------------------------------------------------------
void ofApp::draw(){

    if(liveDataReady){
        canon_preview();
    }

    patternImage.draw(0, 0,ofGetWindowWidth(),ofGetWindowHeight());
}

//--------------------------------------------------------------
void ofApp::keyPressed(int key){
    if(key == 'p'){
        if(!liveDataReady){
            cout << "Starting liveview" << endl;
            start_preview();
        } else {
            cout << "Stopping liveview" << endl;
            stop_preview();
        }
    }

}

//--------------------------------------------------------------
void ofApp::keyReleased(int key){

}

//--------------------------------------------------------------
void ofApp::mouseMoved(int x, int y){

}

// Button 0 : left click, Button 1 : wheel, Button 2 : right click
//--------------------------------------------------------------
void ofApp::mouseDragged(int x, int y, int button){

}

//--------------------------------------------------------------
void ofApp::mousePressed(int x, int y, int button){

}

//--------------------------------------------------------------
void ofApp::mouseReleased(int x, int y, int button){



}

//--------------------------------------------------------------
void ofApp::mouseEntered(int x, int y){

}

//--------------------------------------------------------------
void ofApp::mouseExited(int x, int y){

}

//--------------------------------------------------------------
void ofApp::windowResized(int w, int h){

}

//--------------------------------------------------------------
void ofApp::gotMessage(ofMessage msg){

}

//--------------------------------------------------------------
void ofApp::dragEvent(ofDragInfo dragInfo){

}


void ofApp::exit(){
 stop_preview();
 cout << 'Exiting app throw the door' << endl;
 sleep(1);
}

/*
 * On exit:
 *
        if(connected) {
            ofLogError() << "You must call close() before destroying the camera.";
        }
        for(int i = 0; i < liveBufferMiddle.maxSize(); i++) {
            delete liveBufferMiddle[i];
        }
        delete liveBufferFront;
        delete liveBufferBack;
 *
 *
 *
*/
