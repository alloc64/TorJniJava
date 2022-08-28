/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

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
