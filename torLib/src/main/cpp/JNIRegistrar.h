/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef JNIREGISTRAR_H
#define JNIREGISTRAR_H


class JNIRegistrar {
public:
    static int registerWithJNI(JavaVM *vm, JNIEnv *env);
};


#endif //JNIREGISTRAR_H
