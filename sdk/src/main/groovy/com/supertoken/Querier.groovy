package com.supertoken

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.ToString
import jakarta.servlet.http.HttpServletRequest

import static com.supertoken.Constants.*


@ToString
class Host {
    NormalisedURLDomain domain
    NormalisedURLPath basePath
}

class Querier {
    private static boolean initCalled = false
    private static List<Host> hosts = null

    private static String apiKey = null
    private static String apiVersion = null
    private static int lastTriedIndex = 0
    private static Set<String> hostsAliveForTesting = [] as Set
    private static Closure networkInterceptor = null
    private static long globalCacheTag = System.currentTimeMillis()
    private static boolean disableCache = false
    private String ridToCore = null

    Querier(List<Host> hosts, String ridToCore = null) {
        this.hosts = hosts
        this.ridToCore = ridToCore
        this.globalCacheTag = System.currentTimeMillis()
    }

    static void reset() {
        if (System.getenv("SUPERTOKENS_ENV") != "testing") {
            throw new IllegalStateException("calling testing function in non testing env")
        }
        Querier.initCalled = false
    }

    static Set<String> getHostsAliveForTesting() {
        if (System.getenv("SUPERTOKENS_ENV") != "testing") {
            throw new IllegalStateException("calling testing function in non testing env")
        }
        return Querier.hostsAliveForTesting
    }

    static HttpURLConnection apiRequest(String url, String method, int attemptsRemaining, Map args = [:], Map kwargs = [:]) {
        if (attemptsRemaining == 0) {
            throw new Exception("Retry request failed")
        }
        try {
            URL urlObj = new URL(url)
            HttpURLConnection connection = urlObj.openConnection() as HttpURLConnection
            connection.setRequestMethod(method)
            connection.setConnectTimeout(30000)
            connection.setReadTimeout(30000)
            if (method == "GET") {
                connection.setDoOutput(false)
            } else if (method == "POST" || method == "PUT" || method == "DELETE") {
                connection.setDoOutput(true)
            } else {
                throw new Exception("Unsupported HTTP method: $method")
            }
            connection.connect()
            return connection
        } catch (Exception ignored) {
            // Retry
            return apiRequest(url, method, attemptsRemaining - 1, args, kwargs)
        }
    }

    static String getApiVersion(Map<String, Object> userContext = [:]) {
        if (Querier.apiVersion != null) {
            return Querier.apiVersion
        }
        ProcessState.getInstance().addState(PROCESS_STATE.CALLING_SERVICE_IN_GET_API_VERSION)
        def f = { String url, String method ->
            def headers = [:]
            if (Querier.apiKey != null) {
                headers[API_KEY_HEADER] = Querier.apiKey
            }

            // Get app info
            AppInfo appInfo = SuperTokens.getInstance().appInfo
            HttpServletRequest req = SuperTokens.getRequestFromUserContext(userContext)
            NormalisedURLDomain websiteDomain = appInfo.getOrigin(req, userContext)

            // Prepare query parameters
            def queryParams = [
                    apiDomain   : appInfo.apiDomain.getAsStringDangerous(),
                    websiteDomain: websiteDomain.getAsStringDangerous()
            ]

            if (Querier.networkInterceptor != null) {
                def ( newUrl, newMethod, newHeaders, newQueryParams ) = Querier.networkInterceptor.call(url, method, headers, queryParams, [:], userContext)
                url = newUrl
                method = newMethod
                headers = newHeaders
                queryParams = newQueryParams
            }

            return apiRequest(url, method, 2, [headers: headers], [params: queryParams])
        }

        def response = sendRequestHelper(new NormalisedURLPath(API_VERSION), "GET", f, this.hosts.size())
        def cdiSupportedByServer = response["versions"]
        def apiVersion = Utils.findMaxVersion(cdiSupportedByServer as List<String>, SUPPORTED_CDI_VERSIONS)

        if (apiVersion == null) {
            throw new Exception("The running SuperTokens core version is not compatible with this SDK. Please visit https://supertokens.io/docs/community/compatibility-table to find the right versions")
        }

        Querier.apiVersion = apiVersion
        return Querier.apiVersion

    }

    static Querier getInstance(String ridToCore = null) {
        if (!initCalled) {
            throw new Exception("Please call the supertokens.init function before using SuperTokens")
        }
        return new Querier(hosts, ridToCore)
    }

    static void init(List<Host> hosts, String apiKey = null,
                     Closure<Tuple5<String, String, Map<String, Object>, Map<String, Object>, Map<String, Object>>> networkInterceptor = null,
                     boolean disableCache = false) {
        if (!initCalled) {
            initCalled = true
            Querier.hosts = hosts
            Querier.apiKey = apiKey
            Querier.apiVersion = null
            Querier.lastTriedIndex = 0
            Querier.hostsAliveForTesting = new HashSet<>()
            Querier.networkInterceptor = networkInterceptor
            Querier.disableCache = disableCache
        }
    }

    Map<String, Object> getHeadersWithApiVersion(NormalisedURLPath path, Map<String, Object> userContext = [:]) {
        Map<String, Object> headers = [:]
        headers[API_VERSION_HEADER] =  getApiVersion(userContext)
        if (Querier.apiKey != null) {
            headers[API_KEY_HEADER] = Querier.apiKey
        }
        if (path.isARecipePath() && this.ridToCore != null) {
            headers[RID_KEY_HEADER] = this.ridToCore
        }
        return headers
    }

    static List<String> getAllCoreUrlsForPath(String path) {
        // Normalize the path
        def normalizedPath = new NormalisedURLPath(path)

        // Initialize the result list
        List<String> result = []

        // Iterate through the hosts and construct the full URL for each
        this.hosts.each { h ->
            def currentDomain = h.domain.getAsStringDangerous()
            def currentBasePath = h.basePath.getAsStringDangerous()
            result.add(currentDomain + currentBasePath + normalizedPath.getAsStringDangerous())
        }

        // Return the list of URLs
        return result
    }

    static def invalidateCoreCallCache(Map<String, Object> userContext, boolean updGlobalCacheTagIfNecessary = true) {
        if (userContext == null) {
            userContext = [:]
        }

        if (updGlobalCacheTagIfNecessary && !(userContext.get("_default", [:]).get("keep_cache_alive", false))) {
            this.globalCacheTag = Utils.getTimestampMs()
        }

        userContext["_default"] = userContext.get("_default", [:]) + [
                "core_call_cache": [:]
        ]
    }


    static def sendGetRequest(NormalisedURLPath path, Map<String, Object> params, Map<String, Object> userContext) {
        if (params == null) {
            params = [:]
        }
        def coreCallCache
        def f = { String url, String method ->
            Map<String, Object> headers = getHeadersWithApiVersion(path, userContext)

            // Sort the keys for deterministic order
            List<String> sortedKeys = params.keySet().sort()
            List<String> sortedHeaderKeys = headers.keySet().sort()

            // Start with the path as the unique key
            String uniqueKey = path.getAsStringDangerous()

            // Append sorted params to the unique key
            sortedKeys.each { key ->
                def value = params[key]
                uniqueKey += ";${key}=${value}"
            }

            // Append a separator for headers
            uniqueKey += ";hdrs"

            // Append sorted headers to the unique key
            sortedHeaderKeys.each { key ->
                def value = headers[key]
                uniqueKey += ";${key}=${value}"
            }

            if (userContext != null) {
                def globalCacheTag = userContext.get("_default", [:]).get("global_cache_tag", -1)
                if (globalCacheTag != this.globalCacheTag) {
                    invalidateCoreCallCache(userContext, false)
                }

                coreCallCache = userContext.get("_default", [:]).get("core_call_cache", [:])
                if (!Querier.disableCache && coreCallCache.containsKey(uniqueKey)) {
                    return coreCallCache[uniqueKey]
                }
            }

            if (Querier.networkInterceptor != null) {
                def (newUrl, newMethod, newHeaders, newParams) = Querier.networkInterceptor(url, method, headers, params, [:], userContext)
                url = newUrl
                method = newMethod
                headers = newHeaders
                params = newParams
            }

            HttpURLConnection response = apiRequest(url, method, 2, [headers: headers, params: params])

            if (response.getResponseCode() == 200 && !Querier.disableCache && userContext != null) {
                userContext["_default"] = userContext.get("_default", [:]) + [
                        "core_call_cache" : coreCallCache + [(uniqueKey): response],
                        "global_cache_tag": this.globalCacheTag
                ]
            }

            return response
        }

        return sendRequestHelper(path, "GET", f, hosts.size())
    }

    static def sendPostRequest(NormalisedURLPath path, Map<String, Object> data = [:], Map<String, Object> userContext = [:], boolean test = false) {
        invalidateCoreCallCache(userContext)

        if (data == null) {
            data = [:]
        }

        if (System.getenv("SUPERTOKENS_ENV") == "testing" && test) {
            return data
        }


        def f = { String url, String method ->
            Map<String, Object> headers = getHeadersWithApiVersion(path, userContext)
            headers["content-type"] = "application/json; charset=utf-8"
            if (Querier.networkInterceptor != null) {
                def (newUrl, newMethod, newHeaders, newData) = Querier.networkInterceptor.call(url, method, headers, [:], data, userContext)
                url = newUrl
                method = newMethod
                headers = newHeaders
                data = newData
            }
            return apiRequest(url, method, 2, [headers: headers, json: JsonOutput.toJson(data)])
        }

        return sendRequestHelper(path, "POST", f, this.hosts.size())
    }

    static def sendDeleteRequest(NormalisedURLPath path, Map<String, Object> params = [:], Map<String, Object> userContext = [:]) {
        this.invalidateCoreCallCache(userContext)

        if (params == null) {
            params = [:]
        }

        def f = { String url, String method ->
            Map<String, Object> headers = getHeadersWithApiVersion(path, userContext)

            if (Querier.networkInterceptor != null) {
                def (newUrl, newMethod, newHeaders, newParams)  = Querier.networkInterceptor.call(url, method, headers, params, [:], userContext)
                url = newUrl
                method = newMethod
                headers = newHeaders
                params = newParams
            }
            return apiRequest(url, method, 2, headers: headers, params: params)
        }

        return sendRequestHelper(path, "DELETE", f, this.hosts.size())
    }

    static def sendPutRequest(NormalisedURLPath path, Map<String, Object> data = [:], Map<String, Object> userContext = [:]) {
        this.invalidateCoreCallCache(userContext)
        if (data == null) {
            data = [:]
        }
        def f = { String url, String method ->
            Map<String,Object> headers = getHeadersWithApiVersion(path, userContext)
            headers["content-type"] = "application/json; charset=utf-8"

            if (Querier.networkInterceptor != null) {
                def (newUrl, newMethod, newHeaders, newData) = Querier.networkInterceptor.call(url, method, headers, [:], data, userContext)
                url = newUrl
                method = newMethod
                headers = newHeaders
                data = newData
            }
            return apiRequest(url, method, 2, [headers: headers, json: data])
        }

    }


    private static def sendRequestHelper(NormalisedURLPath path, String method, Closure<Object> httpFunction, int noOfTries, Map<String, Integer> retryInfoMap = [:]) {
        if (noOfTries <= 0) {
            throw new Exception("No SuperTokens core available to query")
        }
        String currentHostDomain = this.hosts[Querier.lastTriedIndex].domain.getAsStringDangerous()
        String currentHostBasePath = this.hosts[Querier.lastTriedIndex].basePath.getAsStringDangerous()
        def currentHost = currentHostDomain + currentHostBasePath
        Querier.lastTriedIndex = (Querier.lastTriedIndex + 1) % this.hosts.size()
        def url = currentHost + path.getAsStringDangerous()

        int maxRetries = 5

        if (retryInfoMap == null) {
            retryInfoMap = [:]
        }

        if (!retryInfoMap.containsKey(url)) {
            retryInfoMap[url] = maxRetries
        }

        ProcessState.getInstance().addState(PROCESS_STATE.CALLING_SERVICE_IN_REQUEST_HELPER)
        HttpURLConnection response = httpFunction.call(url, method) as HttpURLConnection

        if (System.getenv("SUPERTOKENS_ENV") == "testing") {
            Querier.hostsAliveForTesting.add(currentHost)
        }

        if (response.getResponseCode() == RATE_LIMIT_STATUS_CODE) {
            def retriesLeft = retryInfoMap[url]

            if (retriesLeft > 0) {
                retryInfoMap[url] = retriesLeft - 1
                def attemptsMade = maxRetries - retriesLeft
                def delay = (10 + attemptsMade * 250) / 1000.0

                sleep((long) (delay * 1000))
                return sendRequestHelper(path, method, httpFunction, noOfTries, retryInfoMap)
            }
        }

        if (Utils.is4xxError(response.getResponseCode()) || Utils.is5xxError(response.getResponseCode())) {
            throw new Exception("SuperTokens core threw an error for a ${method} request to path: ${path.getAsStringDangerous()} with status code: ${response.statusCode} and message: ${response.text}")
        }

        def res = ["_headers": response.getHeaderFields()]
        // Reading and parsing the JSON response
        String responseText = response.getInputStream().text
        try {
            JsonSlurper jsonSlurp = new JsonSlurper()
            def jsonResponse = jsonSlurp.parseText(responseText)
            res.putAll(jsonResponse as Map)
        } catch (Exception ignored) {
            res["_text"] = responseText

            return res
        }
    }
}