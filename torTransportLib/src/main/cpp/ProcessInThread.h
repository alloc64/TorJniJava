#ifndef PROCESSINTHREAD_H
#define PROCESSINTHREAD_H

#include <thread>

class ProcessInThread {

public:
    virtual void start();
    virtual void terminate();

    bool isRunning(){
        return running;
    }

protected:
    virtual void run() = 0;

private:
    bool running;
};


#endif //PROCESSINTHREAD_H
