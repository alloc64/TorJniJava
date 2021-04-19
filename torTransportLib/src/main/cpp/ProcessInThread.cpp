#include "Logger.h"
#include "ProcessInThread.h"

#define TAG "zeroprism/Process"

void ProcessInThread::start() {
    if(running) {
        Logger::e(TAG, "Unable to start already running process in thread.");
        return;
    }

    this->running = true;
    std::thread t(&ProcessInThread::run, this);
}

void ProcessInThread::terminate() {
    this->running = false;
}
