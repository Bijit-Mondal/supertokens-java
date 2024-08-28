package com.supertoken

import groovy.util.logging.Slf4j
import java.text.SimpleDateFormat

import static com.supertoken.VersionInfo.version as VERSION

@Slf4j
class SuperTokensDebug {

    private static final String SUPERTOKENS_DEBUG_NAMESPACE = "com.supertokens"

    /*
     The debug logger below can be used to log debug messages in the following format:
        com.supertokens {t: "2022-03-18T11:15:24.608Z", message: Your message, file: "/home/supertokens-node/lib/build/supertokens.js:231:18" sdkVer: "9.2.0"} +0m
    */
    private static String getCurrentTimestamp() {
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        return dateFormat.format(new Date())
    }

    static void logDebugMessage(String message) {
        if (isDebugEnabled()) {
            String timestamp = getCurrentTimestamp()
            String fileLocation = getFileLocation()
            log.debug("{t: \"${timestamp}\", message: \"${message}\", file: \"${fileLocation}\", sdkVer: \"${VERSION}\"}")
            println()
        }
    }

    static void enableDebugLogs() {
        // No direct equivalent of debug.enable in Groovy/Java, but you can configure logging levels here
        System.setProperty("groovy.util.logging.Slf4j", SUPERTOKENS_DEBUG_NAMESPACE)
    }

    private static boolean isDebugEnabled() {
        // Check if debug is enabled. You can replace this with appropriate logic depending on your logging setup.
        return log.isDebugEnabled()
    }

    private static String getFileLocation() {
        def errorObject = new Throwable()
        def stackTrace = errorObject.stackTrace

        // find the first trace which doesn't have the logger.groovy file
        stackTrace.find { !it.fileName?.contains("SuperTokensDebug.groovy") }?.toString() ?: "N/A"
    }
}