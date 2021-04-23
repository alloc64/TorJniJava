//
// Created by user on 18.04.2021.
//

#ifndef JNIAWARE_H
#define JNIAWARE_H

#include <jni.h>
#include <vector>

class JNIAware {
public:
    JNIAware(JavaVM *vm, const char *className, std::vector<JNINativeMethod> methods, JNIEnv *env);

private:
    JavaVM *vm;
    JNIEnv *env;

public:
    JavaVM *getVM() const;
    JNIEnv *getJNIEnv() const;
    const char* getClassName() const;

private:
    int registerNativeMethods(const char* className, std::vector<JNINativeMethod> methods);

    const char *className;
};


#endif //JNIAWARE_H
