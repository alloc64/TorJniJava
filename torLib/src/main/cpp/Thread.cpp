/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

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

void Thread::run() {
    if(this->isRunning())
        this->cleanup();
}

void Thread::terminate() {
    if(this->isRunning())
        this->cleanup();
}

void Thread::cleanup() {
    this->thread = nullptr;
}
