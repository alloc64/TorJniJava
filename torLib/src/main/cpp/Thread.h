/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef PROCESSINTHREAD_H
#define PROCESSINTHREAD_H

#include <thread>

class Thread {

public:
    virtual void start();
    virtual void terminate();

    bool isRunning(){
        return thread != nullptr;
    }

protected:
    virtual void run();
    virtual void cleanup();

private:
    std::thread *thread = nullptr;
};


#endif //PROCESSINTHREAD_H
