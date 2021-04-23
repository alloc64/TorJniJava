//
// Created by user on 18.04.2021.
//

#ifndef JNIREGISTRAR_H
#define JNIREGISTRAR_H


class JNIRegistrar {
public:
    static int registerWithJNI(JavaVM *vm, JNIEnv *env);
};


#endif //JNIREGISTRAR_H
