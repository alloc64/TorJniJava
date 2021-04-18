//
// Created by user on 18.04.2021.
//

#ifndef JNIAWARE_H
#define JNIAWARE_H

#include <jni.h>

class JNIAware {
public:
    JNIAware(JNIEnv *env, const char* className, JNINativeMethod *methods);

private:
    JNIEnv *env;

public:
    JNIEnv *getJNIEnv() const;

private:
    int registerNativeMethods(const char* className, JNINativeMethod *methods, int numMethods);
};


#endif //JNIAWARE_H
