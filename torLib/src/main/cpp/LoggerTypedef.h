
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


__attribute__((weak)) void JNILog(LogPriority priority, const char *tag, const char *msg, ...);

#endif //LOGGERTYPEDEF_H
