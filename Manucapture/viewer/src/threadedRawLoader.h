#ifndef THREADEDRAWLOADER_H
#define THREADEDRAWLOADER_H


#include "ofThread.h"
#include "ofMain.h"
#include "FreeImage.h"
#include "rawImageViewer.h"


class ThreadedRawLoader: public ofThread
{
public:
    /// Create a ThreadedObject and initialize the member
    /// variable in an initialization list.
    ThreadedRawLoader()
    {

    }

    /// Start the thread.
    void start(RawImageViewer* viewer, string url, bool imgRight)
    {
        // Mutex blocking is set to true by default
        // It is rare that one would want to use startThread(false).
        this->viewer = viewer;
        this->url = url;
        this->imgRight = imgRight;
        startThread();
    }

    /// Signal the thread to stop.  After calling this method,
    /// isThreadRunning() will return false and the while loop will stop
    /// next time it has the chance to.
    void stop()
    {
        stopThread();
    }

    /// Our implementation of threadedFunction.
    void threadedFunction()
    {

        if(lock())
        {
            viewer->loadPage(url,imgRight);
            unlock();
        }
        else
        {
            // If we reach this else statement, it means that we could not
            // lock our mutex, and so we do not need to call unlock().
            // Calling unlock without locking will lead to problems.
            ofLogWarning("threadedFunction()") << "Unable to lock mutex.";
        }

        if(shouldThrowTestException > 0)
        {
            shouldThrowTestException = 0;

            // Throw an exception to test the global ofBaseThreadErrorHandler.
            // Users that require more specialized exception handling,
            // should make sure that their threaded objects catch all
            // exceptions. ofBaseThreadErrorHandler is only used as a
            // way to provide better debugging / logging information in
            // the event of an uncaught exception.
            throw Poco::ApplicationException("We just threw a test exception!");
        }
    }

    void throwTestException()
    {
        shouldThrowTestException = 1;
    }


protected:
    // A flag to check and see if we should throw a test exception.
    Poco::AtomicCounter shouldThrowTestException;

    RawImageViewer* viewer;
    string url;
    bool imgRight;


};





#endif // THREADEDRAWLOADER_H
