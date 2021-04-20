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
    virtual void run() = 0;

private:
    std::thread *thread = nullptr;
};


#endif //PROCESSINTHREAD_H
