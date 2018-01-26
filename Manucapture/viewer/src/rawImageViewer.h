#ifndef RAWIMAGEVIEWER_H
#define RAWIMAGEVIEWER_H

#include "ofMain.h"
#include "FreeImage.h"

class RawImageViewer
{
public:
    RawImageViewer(int width, int height){
        this->width = width;
        this->height = height;
    }

    void loadPage(string absolute_path, bool imgRight){

        long initMillis;
        long finalMillis;

        if(dib != NULL){
            FreeImage_Unload(dib);
        }
        loaded = false;
        FREE_IMAGE_FORMAT fif = FreeImage_GetFileType(absolute_path.c_str());
        dib = FreeImage_Load(fif, absolute_path.c_str(), RAW_DISPLAY);
        FreeImage_FlipVertical(dib);
        FIBITMAP *rotated;
        if(imgRight)
        {
           rotated = FreeImage_Rotate(dib,90);
        } else {
           rotated = FreeImage_Rotate(dib,270);
        }
        FreeImage_Unload(dib);
        dib = rotated;

        finalMillis = ofGetElapsedTimeMillis();
        dibWidth=FreeImage_GetWidth(dib);
        dibHeight=FreeImage_GetHeight(dib);
    }


    void updateFullImage(){
        getViewFullSize(dib,&imgScreen);
        loaded = true;
    }

    void unload(){
        loaded = false;
    }

    void drawFullView(float x, float y) {
        if(loaded)
           imgScreen.drawSubsection(x,y, width, height,0,0,imgScreen.getWidth(),imgScreen.getHeight());
    }

    void drawZoomView(float x, float y) {
        if(loaded)
            imgZoom.drawSubsection(x,y, width, height,0,0,imgZoom.getWidth(),imgZoom.getHeight());
    }

    void setZoomView(int selectX, int selectY, int selectWidth){
        if(loaded)
            getView(dib, &imgZoom, selectX, selectY, selectWidth);
    }

    ofImage getFullImage() {
        return imgScreen;
    }

    float getRawWidth(){
        return dibWidth;
    }

    float getRawHeight() {
        return dibHeight;
    }

    int width;
    int height;
    int dibWidth;
    int dibHeight;

private:

    void getViewFullSize(FIBITMAP*  dib, ofImage* imgP){

        if (dib == NULL){
            ofLog(OF_LOG_ERROR, "RAW image is not initialized!!");
            return;
        }

        long initMillis = ofGetElapsedTimeMillis();

        int w=FreeImage_GetWidth(dib);
        int h=FreeImage_GetHeight(dib);

        int bytespp = FreeImage_GetLine(dib) / w;
        int downSample = 8;
        imgP->allocate(w/downSample, h/downSample, OF_IMAGE_COLOR);

        int i = 0;
        for(unsigned y = 0; y < imgP->getHeight()*downSample; y += downSample) {
        BYTE *bits = FreeImage_GetScanLine(dib, y);
        for(unsigned x = 0; x < imgP->getWidth()*downSample; x += downSample) {
          // jump to next pixel
          int indexBase = bytespp * x;

          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_RED];
          i++;
          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_GREEN];
          i++;
          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_BLUE];
          i++;

          }
         }
        imgP->update();

        long finalMillis = ofGetElapsedTimeMillis();
        cout << "generating view takes " << (finalMillis - initMillis)/1000.0 << " seconds " << endl;

    }


    void getView(FIBITMAP*  dib, ofImage* imgP, int screenX, int screenY, int screenWidth){

        if (dib == NULL){
            ofLog(OF_LOG_ERROR, "RAW image is not initialized!!");
            return;
        }

        if(ofGetElapsedTimeMillis()-lastMillisViewLoaded > periodMillisViewLoaded){
            lastMillisViewLoaded = ofGetElapsedTimeMillis();
        }
        else {
           return;
        }

        long initMillis = ofGetElapsedTimeMillis();

        int w=FreeImage_GetWidth(dib);
        int h=FreeImage_GetHeight(dib);

        int screenHeight = (int)((screenWidth/(float)w)*h);
        float rawWidth =  ofMap(screenWidth, 0, width, 0, w);
        float rawHeight = ofMap(screenHeight, 0, height, 0, h);


        // Minimum scale is pixel perfect 1:1 to the target size
        if(rawHeight < height){
            rawHeight = height;
        }
        if(rawWidth < width){
            rawWidth = width;
        }

        int rawX = ofMap(screenX, 0, width, 0, w);
        int rawY = ofMap(screenY, 0, height, 0,h);

        int bytespp = FreeImage_GetLine(dib) / w;
        int downSample = rawWidth/width;
        int imageWidth = rawWidth/downSample;
        int imageHeight = rawHeight/downSample;


        imgP->allocate(imageWidth, imageHeight, OF_IMAGE_COLOR);

        int i = 0;
        for(unsigned y = rawY; y < imgP->getHeight()*downSample + rawY; y += downSample) {
        BYTE *bits = FreeImage_GetScanLine(dib, y);
        for(unsigned x = rawX; x < imgP->getWidth()*downSample + rawX; x += downSample) {
          int indexBase = bytespp * x;
          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_RED];
          i++;
          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_GREEN];
          i++;
          imgP->getPixels()[i] = (int)bits[indexBase + FI_RGBA_BLUE];
          i++;
          }
         }
        imgP->update();
    }


    // Atributes
    bool loaded = false;

    ulong lastMillisViewLoaded;
    ulong periodMillisViewLoaded = 50;
    FIBITMAP *  dib = NULL;
    ofImage imgScreen;
    ofImage imgZoom;


};



#endif // RAWIMAGEVIEWER_H
