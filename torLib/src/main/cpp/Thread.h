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
