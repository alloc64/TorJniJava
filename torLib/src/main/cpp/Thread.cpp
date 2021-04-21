#include "Logger.h"
#include "Thread.h"

#define TAG "tl/Process"

void Thread::start() {
    if(isRunning()) {
        Logger::e(TAG, "Unable to start already running process in thread.");
        return;
    }

    this->thread = new std::thread(&Thread::run, this);
}

void Thread::terminate() {
    this->thread = nullptr;
}
