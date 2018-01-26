#pragma once

#include "ofMain.h"
#include "FreeImage.h"
#include "ofxOsc.h"
#include "rawImageViewer.h"
#include "threadedRawLoader.h"
#include "ofxOpenCv.h"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

using namespace cv;


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
    void cropPages();
    void findGlassPlate();
    void findConvexityDefects(vector<Point> contour, vector<Point>& convexDefects);


    RawImageViewer* viewerLeft;
    RawImageViewer* viewerRight;

    float selectX = 0;
    float selectY = 0;
    int selectWidth = 0;
    int selectHeight = 0;
    bool selectLeft;

    bool zoomModeLeft = false;
    bool zoomModeRight = false;

    int lastMouseDraggX;
    int lastMouseDraggY;
    int initMousePressX;
    int initMousePressY;

    ofxOscReceiver receiver;
    ofxOscSender sender;
    ofFpsCounter frameCounter;

    ThreadedRawLoader loaderLeft;
    ThreadedRawLoader loaderRight;
    bool loading=false;

    string leftRawImageURL = "";
    string rightRawImageURL = "";
    ofTrueTypeFont	urlFont;
    ofTrueTypeFont	camIDFont;
    ofColor textColor;
    ofColor textBackgroundColor;

    ofImage transitionBackground;
    ofImage gifImg;
    int gifSpinFrameCount = 0;


    ofxCvColorImage			colorImg;
    ofImage                 colorBg;
    ofxCvGrayscaleImage 	grayImage;
    ofxCvGrayscaleImage 	grayBg;
    ofxCvGrayscaleImage 	grayDiff;

    ofxCvContourFinder 	contourFinder;
    int 				threshold;
    ofRectangle         pageBBLeft;
    ofRectangle         pageBBRight;
    ofRectangle         screenPageBBLeft;
    ofRectangle         screenPageBBRight;

    vector<Point> convexDefects;

    int pointIndex = -1;
    int pointIndex2 = -1;
     vector<Point> countour;

     int indexUp = 0;
     int indexDown = 0;
     bool clockwise = false;
     int bIndex = 0;
     float bufer[5] = {0,0,0,0,0};


};
