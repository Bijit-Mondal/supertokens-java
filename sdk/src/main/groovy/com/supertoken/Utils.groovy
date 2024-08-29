package com.supertoken

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.time.Instant

import static com.supertoken.Constants.RID_KEY_HEADER

@CompileStatic
class Utils {
    static boolean isAnIpAddress(String ipAddress) {
        return ipAddress ==~ /^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
    }
    static String normaliseHttpMethod(String method) {
        return method.toLowerCase()
    }
    static String normaliseEmail(String email) {
        return email.trim().toLowerCase()
    }
    static String getTopLevelDomainForSameSiteResolution(String url) {
        def urlObj = new URL(url)
        def hostname = urlObj.host

        if (hostname.startsWith("localhost") || isAnIpAddress(hostname)) {
            return "localhost"
        }

        def domainParts = hostname.split('\\.')
        if (domainParts.length > 1) {
            return "${domainParts[-2]}.${domainParts[-1]}"
        }

        throw new IllegalArgumentException("Invalid domain: $hostname")
    }
    static String getRidFromHeader(HttpServletRequest request) {
        return getHeader(request, RID_KEY_HEADER)
    }
    static String getHeader(HttpServletRequest request, String key) {
        return request.getHeader(key)
    }
    static String findMaxVersion(List<String> versions1, List<String> versions2) {
        def versions = versions1.intersect(versions2)
        if (versions.isEmpty()) {
            return null
        }

        String maxV = versions[0]
        for (int i = 1; i < versions.size(); i++) {
            String version = versions[i]
            maxV = getMaxVersion(maxV, version)
        }

        return maxV
    }
    static boolean isVersionGte(String version, String minimumVersion) {
        return getMaxVersion(version, minimumVersion) == version
    }
    static String getMaxVersion(String v1, String v2) {
        def v1Split = v1.split("\\.")
        def v2Split = v2.split("\\.")
        int maxLoop = Math.min(v1Split.length, v2Split.length)

        for (int i = 0; i < maxLoop; i++) {
            if (v1Split[i].toInteger() > v2Split[i].toInteger()) {
                return v1
            }
            if (v2Split[i].toInteger() > v1Split[i].toInteger()) {
                return v2
            }
        }

        if (v1Split.length > v2Split.length) {
            return v1
        }

        return v2
    }

    static void sendNon200ResponseWithMessage(HttpServletResponse res, String message, int statusCode) {
        sendNon200Response(res, statusCode, [message: message] as Map<String, Object>)
    }
    static void sendNon200Response(HttpServletResponse res, int statusCode, Map<String, Object> body) {
        if (statusCode < 300) {
            throw new Exception("Calling sendNon200Response with status code < 300")
        }
        SuperTokensDebug.logDebugMessage("Sending response to client with status code: $statusCode")
        res.setStatus(statusCode)
        res.setContentType("application/json")
        res.getWriter().println(new JsonBuilder(body).toPrettyString())
    }
    static void send200Response(HttpServletResponse res, Map<String, Object> responseJson) {
        SuperTokensDebug.logDebugMessage("Sending response to client with status code: 200")
        res.setStatus(200)
        res.setContentType("application/json")
        res.getWriter().println(new JsonBuilder(responseJson).toPrettyString())
    }
    static boolean is4xxError(int statusCode) {
        return statusCode / 100 == 4
    }

    static boolean is5xxError(int statusCode) {
        return statusCode / 100 == 5
    }
    static long getTimestampMs() {
        return Instant.now().toEpochMilli()
    }
    static String utfBase64encode(String s, boolean urlsafe) {
        if (urlsafe) {
            return Base64.getUrlEncoder().encodeToString(s.getBytes("UTF-8"))
        }
        return Base64.getEncoder().encodeToString(s.getBytes("UTF-8"))
    }

    static String humanizeTime(int ms) {
        def t = Math.floor(ms / 1000 as double)
        String suffix = ""

        if (t < 60) {
            if (t > 1) {
                suffix = "s"
            }
            return "${t} second${suffix}"
        } else if (t < 3600) {
            def m = Math.floor(t / 60 as double)
            if (m > 1) {
                suffix = "s"
            }
            return "${m} minute${suffix}"
        } else {
            def h = Math.floor(t / 360 as double) / 10.0
            if (h > 1) {
                suffix = "s"
            }
            if (h % 1 == 0) {
                h = (int) h
            }
            return "${h} hour${suffix}"
        }
    }

}
