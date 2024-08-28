package com.supertoken

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
        def commonVersions = versions1.intersect(versions2)
        if (commonVersions.isEmpty()) return null
        return commonVersions.max { v1, v2 -> compareVersions(v1, v2) }
    }
    static int compareVersions(String v1, String v2) {
        def v1Parts = v1.tokenize('.').collect { it as Integer }
        def v2Parts = v2.tokenize('.').collect { it as Integer }
        return v1Parts <=> v2Parts
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
}
