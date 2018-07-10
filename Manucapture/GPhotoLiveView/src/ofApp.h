#pragma once

#include "ofMain.h"
#include "FreeImage.h"
#include "ofxOsc.h"
#include "fixedqueue.h"
#include <gphoto2/gphoto2-camera.h>
#include <gphoto2/gphoto2-port-info-list.h>


//#include "ofxOpenCv.h"
//#include "opencv2/highgui/highgui.hpp"
//#include "opencv2/imgproc/imgproc.hpp"

//using namespace cv;


#define RAW_VIEWER_WIDTH 660
#define RAW_VIEWER_HEIGHT 990


class ofApp : public ofBaseApp{


public:

    void setup();
    void update();
    void draw();

    void keyPressed(int key);
    void keyReleased(int key);
    void mouseMoved(int x, int y);
    void mouseDragged(int x, int y, int button);
    void mousePressed(int x, int y, int button);
    void mouseReleased(int x, int y, int button);
    void mouseEntered(int x, int y);
    void mouseExited(int x, int y);
    void windowResized(int w, int h);
    void dragEvent(ofDragInfo dragInfo);
    void gotMessage(ofMessage msg);
    void exit();

    ofxOscReceiver receiver;
    ofxOscSender sender;
    ofFpsCounter frameCounter;

    ofTrueTypeFont	urlFont;
    ofTrueTypeFont	camIDFont;

    char* name_left;
    char* value_left;
    char* name_right;
    char* value_right;

    int init_cameras();
    int init_cameras_with_serial_numbers();
    int canon_preview();
    int start_preview();
    int stop_preview();

    /*
     Live view data is read from the camera into liveBufferBack when DownloadEvfData()
     is called. Then the class is locked, and liveBufferBack is quickly pushed
     onto the liveBufferMiddle queue. When update() is called, the class is
     also locked to quickly pop from liveBufferMiddle into liveBufferFront.
     At this point, the pixels are decoded into livePixels and uploaded to liveTexture.
     */
    ofBuffer** liveBufferBack;
    FixedQueue<ofBuffer*>* liveBufferMiddle;
    ofBuffer** liveBufferFront;
    mutable ofPixels** livePixels;
    mutable ofTexture** liveTexture;

    bool liveDataReady = false;
    int frameNum = 100000;
    Camera		**cams;
    int cameraCount = 0;
    GPContext *canoncontext;

    ofImage patternImage;

};
