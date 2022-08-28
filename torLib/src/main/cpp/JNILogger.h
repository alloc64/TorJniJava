/***********************************************************************
 * Copyright (c) 2021 Milan Jaitner                                    *
 * Distributed under the MIT software license, see the accompanying    *
 * file COPYING or https://www.opensource.org/licenses/mit-license.php.*
 ***********************************************************************/

#ifndef LOGGERTYPEDEF_H
#define LOGGERTYPEDEF_H
/**
 * Android log priority values, in increasing order of priority.
 */
typedef enum LogPriority {
    /** For internal use only.  */
    LOG_UNKNOWN = 0,
    /** The default priority, for internal use only.  */
    LOG_DEFAULT, /* only for SetMinPriority() */
    /** Verbose logging. Should typically be disabled for a release apk. */
    LOG_VERBOSE,
    /** Debug logging. Should typically be disabled for a release apk. */
    LOG_DEBUG,
    /** Informational logging. Should typically be disabled for a release apk. */
    LOG_INFO,
    /** Warning logging. For use with recoverable failures. */
    LOG_WARN,
    /** Error logging. For use with unrecoverable failures. */
    LOG_ERROR,
    /** Fatal logging. For use when aborting. */
    LOG_FATAL,
    /** For internal use only.  */
    LOG_SILENT, /* only for SetMinPriority(); must be last */
} LogPriority;


typedef void(*JNILogPtr)(LogPriority priority, const char *tag, const char *msg, ...);
extern JNILogPtr JNILog;

#endif //LOGGERTYPEDEF_H
