#include "ofApp.h"
//--------------------------------------------------------------



 using namespace cv;


void ofApp::setup(){

    ofSetWindowTitle("ManuCaptureViewer");
    ofSetWindowShape(1320,1070);

    receiver.setup(3333);
    // open an outgoing connection to HOST:PORT
    sender.setup("127.0.0.1", 3334);
    //sender.sendMessage(m);

    viewerLeft = new RawImageViewer(RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT);
    viewerRight = new RawImageViewer(RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT);

    ofTrueTypeFont::setGlobalDpi(72);
    urlFont.load("verdana.ttf", 12, true, true);
    camIDFont.load("verdana.ttf", 36, true, true);
    textColor.r =0;
    textColor.g =57;
    textColor.b =82;
    textBackgroundColor.r=219;
    textBackgroundColor.g=235;
    textBackgroundColor.b=243;

    gifImg.loadImage("spinWheel.gif");

    threshold = 80;
    //colorBg.load("manucapture_background.png");
    colorBg.allocate(724,1086,OF_IMAGE_COLOR);
    for(int j=0; j<colorBg.getHeight(); j++){
        for(int i=0; i<colorBg.getWidth(); i++){
            colorBg.getPixels()[(j*colorBg.getWidth()+i)*3] = 40;
            colorBg.getPixels()[(j*colorBg.getWidth()+i)*3+1] = 40;
            colorBg.getPixels()[(j*colorBg.getWidth()+i)*3+2] = 60;
        }
    }
    colorBg.update();

    colorImg = colorBg;
    grayBg.setFromColorImage(colorImg);

}

//--------------------------------------------------------------
void ofApp::update(){
    frameCounter.update();
    if(frameCounter.getNumFrames()<20){
      ofSetWindowPosition(ofGetScreenWidth()-ofGetWidth()-3, 0);
      ofSetWindowShape(1320,1057);
    }

    // check for waiting messages
    while(receiver.hasWaitingMessages()){
        // get the next message
        ofxOscMessage m;
        receiver.getNextMessage(m);
        // check for mouse moved message
        if(m.getAddress() == "/load/item"){
            transitionBackground.grabScreen(0,0,RAW_VIEWER_WIDTH*2,RAW_VIEWER_HEIGHT);
            if(leftRawImageURL != m.getArgAsString(0)){
                leftRawImageURL = m.getArgAsString(0);
                if(leftRawImageURL!="")
                    loaderLeft.start(viewerLeft,leftRawImageURL, false);
                else
                    viewerLeft->unload();
            }
            if(rightRawImageURL != m.getArgAsString(1)){
                rightRawImageURL = m.getArgAsString(1);
                if(rightRawImageURL!="")
                    loaderRight.start(viewerRight,rightRawImageURL, true);
                else
                    viewerRight->unload();
            }
            loading=true;
        }

    }

    if(loading){
        if(!loaderLeft.isThreadRunning() && !loaderRight.isThreadRunning()){
                viewerLeft->updateFullImage();
                viewerRight->updateFullImage();
                cropPages();
                pointIndex = indexUp;
                loading=false;
        }
    }


}


//--------------------------------------------------------------
void ofApp::draw(){

    ofBackground(0,12,18);
    if(!loading){
        ofSetColor(255);
        if(!zoomModeLeft){
        //  viewerLeft->drawZoomView(0,0);
         viewerLeft->drawFullView(0,0);
        // contourFinder.draw(0,0);
        } else {
          viewerLeft->drawZoomView(0,0);
        }
        if(!zoomModeRight){
          viewerRight->drawFullView(RAW_VIEWER_WIDTH,0);
          //  viewerRight->drawZoomView(RAW_VIEWER_WIDTH,0);

        } else {
          viewerRight->drawZoomView(RAW_VIEWER_WIDTH,0);
        }

        if(selectWidth!=0 && selectHeight!=0){
           if(selectLeft){
               if(!zoomModeLeft){
                   ofPushStyle();
                   ofSetColor(0,100,100);
                   ofNoFill();
                   ofRect(selectX, selectY, selectWidth, selectHeight);
                   ofPopStyle();
               }
           } else {
               if(!zoomModeRight){
                   ofPushStyle();
                   ofSetColor(0,100,100);
                   ofNoFill();
                   ofRect(selectX+RAW_VIEWER_WIDTH, selectY, selectWidth, selectHeight);
                   ofPopStyle();
               }
           }
       }
    }
    else {
        transitionBackground.draw(0,0);
        ofPushStyle();
        ofSetColor(0,0,0,120);
        ofRect(0,0,2*RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT);
        ofPopStyle();
        ofPushMatrix();
        ofTranslate(RAW_VIEWER_WIDTH, RAW_VIEWER_HEIGHT/2);
        ofRotate(gifSpinFrameCount);
        gifImg.draw(-gifImg.getWidth()/2,-gifImg.getHeight()/2);
        ofPopMatrix();
        gifSpinFrameCount++;
        if(gifSpinFrameCount<0)
            gifSpinFrameCount=0;

    }

    ofPushStyle();
    ofNoFill();
    ofSetColor(ofColor(200,200,200));
    ofRect(-1,RAW_VIEWER_HEIGHT+1,ofGetWidth()+2,ofGetHeight()-RAW_VIEWER_HEIGHT-2);
    ofPopStyle();

    ofPushStyle();
    ofSetColor(textBackgroundColor);
    ofRect(5,RAW_VIEWER_HEIGHT+15,40,40);
    ofSetColor(textColor);
    camIDFont.drawString("B",12,RAW_VIEWER_HEIGHT+48);
    ofPopStyle();

    ofPushStyle();
    ofSetColor(textBackgroundColor);
    ofRect(5 + RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT+15,40,40);
    ofSetColor(textColor);
    camIDFont.drawString("A",12 + RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT+48);
    ofPopStyle();

    if(leftRawImageURL!=""){
        ofPushStyle();
        ofSetColor(textBackgroundColor);
        ofRect(50,RAW_VIEWER_HEIGHT+25,RAW_VIEWER_WIDTH-80,20);
        ofSetColor(textColor);
        urlFont.drawString(leftRawImageURL, 55, RAW_VIEWER_HEIGHT+40);
        ofPopStyle();
    }

    if(rightRawImageURL!=""){
        ofPushStyle();
        ofSetColor(textBackgroundColor);
        ofRect(50+RAW_VIEWER_WIDTH,RAW_VIEWER_HEIGHT+25,RAW_VIEWER_WIDTH-80,20);
        ofSetColor(textColor);
        urlFont.drawString(rightRawImageURL, 55 + RAW_VIEWER_WIDTH, RAW_VIEWER_HEIGHT+40);
        ofPopStyle();
    }

    /*
    if(!loading){
        ofPushStyle();
        ofNoFill();
        ofSetColor(0,255,255);
//        if(!zoomModeLeft)
//            ofDrawRectangle(screenPageBBLeft.getX(), screenPageBBLeft.getY(), 0, screenPageBBLeft.getWidth(),screenPageBBLeft.getHeight());
        if(!zoomModeRight)
            ofDrawRectangle(screenPageBBRight.getX()+RAW_VIEWER_WIDTH, screenPageBBRight.getY(), 0, screenPageBBRight.getWidth(),screenPageBBRight.getHeight());
        ofPopStyle();

        if(countour.size()>0){
            ofPushStyle();
            ofSetColor(255,0,0);
            ofPoint c(countour[indexUp].x,countour[indexUp].y);
            ofCircle(c,10);
            ofSetColor(255,250,0);
            ofPoint d(countour[indexDown].x,countour[indexDown].y);
            ofCircle(d,10);
            ofSetColor(255,0,255);
            ofPoint p(countour[pointIndex].x,countour[pointIndex].y);
            ofCircle(p,10);
            ofSetColor(0,255,255);
            ofPoint p2(countour[pointIndex2].x,countour[pointIndex2].y);
            ofCircle(p2,10);
            ofSetColor(0,0,255);
            ofLine(p,p2);
            ofPopStyle();

            pointIndex = indexUp;
            findGlassPlate();


        }

//        int indexUp = 0;
//        int indexDown = 0;
//        bool clockwise = false;

    }
*/


}

//--------------------------------------------------------------
void ofApp::keyPressed(int key){
    switch (key){
        case ' ':
            //bLearnBakground = true;
            break;
        case '+':
            threshold ++;
            if (threshold > 255) threshold = 255;
            break;
        case '-':
            threshold --;
            if (threshold < 0) threshold = 0;
            break;
        case '1':
            pointIndex--;
            if(pointIndex < 0){
                pointIndex = countour.size()-1;
            }
            break;
        case '2':
            pointIndex++;
            if(pointIndex >= countour.size()){
                pointIndex = 0;
            }
        break;

    }

    findGlassPlate();

}

//--------------------------------------------------------------
void ofApp::keyReleased(int key){

   switch(key){
    case OF_KEY_RIGHT:
     //   zoomModeLeft = !zoomModeLeft;
        break;
    case OF_KEY_UP:
       // eventString += "UP";
        break;
    }
}

//--------------------------------------------------------------
void ofApp::mouseMoved(int x, int y){

}

// Button 0 : left click, Button 1 : wheel, Button 2 : right click
//--------------------------------------------------------------
void ofApp::mouseDragged(int x, int y, int button){

    if(button==0){
        if(selectLeft){
            if(zoomModeLeft){
                float amount = -(x - lastMouseDraggX);
                selectX += amount;
                if(selectX<0){
                    selectX = 0;
                } else if((selectX+selectWidth)>RAW_VIEWER_WIDTH){
                    selectX = RAW_VIEWER_WIDTH - selectWidth;
                }
                amount = -(y - lastMouseDraggY);
                selectY += amount;
                if(selectY<0){
                    selectY = 0;
                } else if(selectY+selectHeight>RAW_VIEWER_HEIGHT){
                    selectY = RAW_VIEWER_HEIGHT - selectHeight;
                }
                viewerLeft->setZoomView(selectX,selectY,selectWidth);
            } else {
                selectWidth = (int)abs(x - initMousePressX);
                if(x < initMousePressX)
                    selectX = x;
                else
                    selectX = initMousePressX;
                selectHeight = selectWidth*3/2;
                if(y < initMousePressY)
                    selectY = y;
                else
                    selectY = initMousePressY;
                if(selectX<0){
                    selectX = 0;
                } else if((selectX+selectWidth)>RAW_VIEWER_WIDTH){
                    selectX = RAW_VIEWER_WIDTH - selectWidth;
                }
                if(selectY<0){
                    selectY = 0;
                } else if(selectY+selectHeight>RAW_VIEWER_HEIGHT){
                    selectY = RAW_VIEWER_HEIGHT - selectHeight;
                }
            }
        } else {
            if(zoomModeRight){
                float amount = -(x - lastMouseDraggX);
                selectX += amount;
                if(selectX<0){
                    selectX = 0;
                } else if((selectX+selectWidth)>RAW_VIEWER_WIDTH){
                    selectX = RAW_VIEWER_WIDTH - selectWidth;
                }
                amount = -(y - lastMouseDraggY);
                selectY += amount;
                if(selectY<0){
                    selectY = 0;
                } else if(selectY+selectHeight>RAW_VIEWER_HEIGHT){
                    selectY = RAW_VIEWER_HEIGHT - selectHeight;
                }
                viewerRight->setZoomView(selectX,selectY,selectWidth);
            } else {
                selectWidth = (int)abs(x - initMousePressX);
                if(x < initMousePressX)
                    selectX = x-RAW_VIEWER_WIDTH;
                else
                    selectX = initMousePressX-RAW_VIEWER_WIDTH;
                selectHeight = selectWidth*3/2;
                if(y < initMousePressY)
                    selectY = y;
                else
                    selectY = initMousePressY;
                if(selectX<0){
                    selectX = 0;
                } else if((selectX+selectWidth)>RAW_VIEWER_WIDTH){
                    selectX = RAW_VIEWER_WIDTH - selectWidth;
                }
                if(selectY<0){
                    selectY = 0;
                } else if(selectY+selectHeight>RAW_VIEWER_HEIGHT){
                    selectY = RAW_VIEWER_HEIGHT - selectHeight;
                }
            }
        }
    }

    lastMouseDraggX = x;
    lastMouseDraggY = y;

}

//--------------------------------------------------------------
void ofApp::mousePressed(int x, int y, int button){

    lastMouseDraggX = x;
    lastMouseDraggY = y;
    initMousePressX = x;
    initMousePressY = y;
    if(x < RAW_VIEWER_WIDTH){
       selectLeft = true;
    } else {
       selectLeft = false;
    }

}

//--------------------------------------------------------------
void ofApp::mouseReleased(int x, int y, int button){

    if(selectLeft){
        if(!zoomModeLeft && selectWidth > 2){
            zoomModeLeft = true;
        } else if(button == 2 && zoomModeLeft){
            zoomModeLeft = false;
            selectHeight = 0;
            selectWidth = 0;
        }

        if(zoomModeLeft){
            viewerLeft->setZoomView(selectX,selectY,selectWidth);
        }
    }
    else {
        if(!zoomModeRight&& selectWidth > 2){
            zoomModeRight = true;
        } else if(button == 2 && zoomModeRight){
            zoomModeRight = false;
            selectHeight = 0;
            selectWidth = 0;
        }
        if(zoomModeRight){
            viewerRight->setZoomView(selectX,selectY,selectWidth);
        }
    }
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


void ofApp::cropPages(){

    // Contour finder

    // Left
    colorImg = viewerLeft->getFullImage();
    grayImage.setFromColorImage(colorImg);
    // take the abs value of the difference between background and incoming and then threshold:
    grayDiff.absDiff(grayImage,grayBg);
    grayDiff.threshold(threshold);
    // find contours which are between the size of 20 pixels and 1/3 the w*h pixels.
    // also, find holes is set to true so we will get interior contours as well....
    contourFinder.findContours(grayDiff, 20, colorImg.getWidth()*colorImg.getHeight(), 10, true, true);	// find holes

    pageBBLeft.setX(0);
    pageBBLeft.setY(0);
    pageBBLeft.setWidth(1);
    pageBBLeft.setHeight(1);

    // or, instead we can draw each blob individually from the blobs vector,
    // this is how to get access to them:
    for (int i = 0; i < contourFinder.nBlobs; i++){
        ofRectangle blobBB = contourFinder.blobs[i].boundingRect;
        if(!contourFinder.blobs[i].hole){
            if(blobBB.width > pageBBLeft.width){
                pageBBLeft.x = ofMap(blobBB.x,0,colorImg.getWidth(),0,viewerLeft->getRawWidth());
                pageBBLeft.y = ofMap(blobBB.y,0,colorImg.getHeight(),0,viewerLeft->getRawHeight());
                pageBBLeft.width = ofMap(blobBB.width,0,colorImg.getWidth(),0,viewerLeft->getRawWidth());
                pageBBLeft.height = ofMap(blobBB.height,0,colorImg.getHeight(),0,viewerLeft->getRawHeight());
            }
        }

    }

    // Right
    colorImg = viewerRight->getFullImage();
    grayImage.setFromColorImage(colorImg);
    // take the abs value of the difference between background and incoming and then threshold:
    grayDiff.absDiff(grayImage,grayBg);
    grayDiff.threshold(threshold);
    // find contours which are between the size of 20 pixels and 1/3 the w*h pixels.
    // also, find holes is set to true so we will get interior contours as well....
    contourFinder.findContours(grayDiff, 20, colorImg.getWidth()*colorImg.getHeight(), 10, true, true);	// find holes

    pageBBRight.setX(0);
    pageBBRight.setY(0);
    pageBBRight.setWidth(1);
    pageBBRight.setHeight(1);

    // or, instead we can draw each blob individually from the blobs vector,
    // this is how to get access to them:

    for (int i = 0; i < contourFinder.nBlobs; i++){
        ofRectangle blobBB = contourFinder.blobs[i].boundingRect;
        if(!contourFinder.blobs[i].hole){
            if(blobBB.width > pageBBRight.width){
                countour.clear();
                for(int j=0; j<contourFinder.blobs[i].pts.size();j++){
                    Point p = {contourFinder.blobs[i].pts[j].x,contourFinder.blobs[i].pts[j].y };
                    countour.push_back(p);
                }

                pageBBRight.setX(ofMap(blobBB.getX(),0,colorImg.getWidth(),0,viewerRight->getRawWidth()));
                pageBBRight.setY(ofMap(blobBB.getY(),0,colorImg.getHeight(),0,viewerRight->getRawHeight()));
                pageBBRight.setWidth(ofMap(blobBB.getWidth(),0,colorImg.getWidth(),0,viewerRight->getRawWidth()));
                pageBBRight.setHeight(ofMap(blobBB.getHeight(),0,colorImg.getHeight(),0,viewerRight->getRawHeight()));
            }
        }
    }

    // findConvexityDefects(countour, convexDefects);
    /*
    Mat image= cv::imread("open_1a.jpg");
    Mat contours;
    Mat gray_image;
    cvtColor( image, gray_image, CV_RGB2GRAY );
    std::vector<cv::Mat> channels;
    cv::Mat hsv;
    cv::cvtColor( image, hsv, CV_RGB2HSV );
    cv::split(hsv, channels);
    gray_image = channels[0];
    Canny(gray_image,contours,10,350);
    */

    for(int i = 0; i < countour.size(); i++){
        Point currentPoint = countour[i];
        if(currentPoint.x < 300){
            int prevIndex = i-1;
            if(prevIndex<0)
                prevIndex = countour.size()-1;
            if(abs(currentPoint.y-countour[prevIndex].y)>300){
                if(currentPoint.y > countour[prevIndex].y){
                    clockwise = false;
                    indexUp = prevIndex;
                    indexDown = i;
                }
                else {
                    clockwise = true;
                    indexUp = i;
                    indexDown = prevIndex;
                }
                break;
            }
        }
    }

    pointIndex = indexUp;
    findGlassPlate();


    screenPageBBLeft.setX(ofMap(pageBBLeft.getX(),0,viewerLeft->getRawWidth(),0,viewerLeft->width));
    screenPageBBLeft.setY(ofMap(pageBBLeft.getY(),0,viewerLeft->getRawHeight(),0,viewerLeft->height));
    screenPageBBLeft.setWidth(ofMap(pageBBLeft.getWidth(),0,viewerLeft->getRawWidth(),0,viewerLeft->width));
    screenPageBBLeft.setHeight(ofMap(pageBBLeft.getHeight(),0,viewerLeft->getRawHeight(),0,viewerLeft->height));

    screenPageBBRight.setX(ofMap(pageBBRight.getX(),0,viewerRight->getRawWidth(),0,viewerRight->width));
    screenPageBBRight.setY(ofMap(pageBBRight.getY(),0,viewerRight->getRawHeight(),0,viewerRight->height));
    screenPageBBRight.setWidth(ofMap(pageBBRight.getWidth(),0,viewerRight->getRawWidth(),0,viewerRight->width));
    screenPageBBRight.setHeight(ofMap(pageBBRight.getHeight(),0,viewerRight->getRawHeight(),0,viewerRight->height));

   // viewerLeft->setZoomView(screenPageBBLeft.getX(),screenPageBBLeft.getY(),screenPageBBLeft.getWidth());
   // viewerRight->setZoomView(screenPageBBRight.getX(),screenPageBBRight.getY(),screenPageBBRight.getWidth());

}


void ofApp::findGlassPlate() {

    if(clockwise){
        int nextIndex  = pointIndex + 1;
        if(nextIndex >= countour.size())
            nextIndex = 0;
        bufer[bIndex] = countour[nextIndex].y - countour[pointIndex].y;
        bIndex++;
        if(bIndex>=5){
            bIndex=0;
        }
        float sum = 0;
        for(int i = 0 ; i < 5; i++){
            sum += bufer[i];
        }
        cout << "clockwise factor " << sum << " index " << pointIndex << endl;

        pointIndex2 = pointIndex + 5;
        if(pointIndex2 >= countour.size()){
            pointIndex2 = pointIndex2 - countour.size();
        }

    } else {
        int iterCount = 0;
        bool stopProcess = false;
        while(iterCount<countour.size() && !stopProcess){
            pointIndex2 = pointIndex - 5;
            if(pointIndex2 < 0){
                pointIndex2 = pointIndex2 + countour.size();
            }

            ofSetColor(255,0,255);
            ofPoint p(countour[pointIndex].x,countour[pointIndex].y);
            ofCircle(p,10);
            ofSetColor(0,255,255);
            ofPoint p2(countour[pointIndex2].x,countour[pointIndex2].y);
            ofCircle(p2,10);
            ofSetColor(0,0,255);
            ofLine(p,p2);
            ofPopStyle();


            if(abs(countour[pointIndex2].y-countour[pointIndex].y)<2){
                stopProcess = true;
              //  indexUp = pointIndex;
            }
            pointIndex++;
            iterCount++;
        }
    }

}

 void ofApp::findConvexityDefects(vector<Point> contour, vector<Point>& convexDefects){
    if(contour.size() > 0){
        std::vector<int> ihull;
        convexHull( contour, ihull ); //convexHull is smart and fills in contourIndices

        std::vector<Vec4i> defects;
        convexityDefects( contour, ihull, defects ); //expects indexed hull (internal assertion mat.channels()==1)

        for(int i=0; i < ihull.size(); i++){
            int index = ihull[i];
            convexDefects.push_back(contour[index]);
        }

    }
}

