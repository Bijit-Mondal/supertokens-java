package com.supertoken

import static com.supertoken.Constants.*

class Querier {
    private static boolean initCalled = false
    private static List<String> hosts = []
    private static String apiKey = null
    private static String apiVersion = null
    private static int lastTriedIndex = 0
    private static Set<String> hostsAliveForTesting = [] as Set
    private static Closure networkInterceptor = null
    private static long globalCacheTag = System.currentTimeMillis()
    private static boolean disableCache = false
    private String ridToCore = null

    Querier(List<String> hosts, String ridToCore = null) {
        this.hosts = hosts
        this.ridToCore = ridToCore
        globalCacheTag = System.currentTimeMillis()
    }

    static void reset() {
        if (System.getenv("SUPERTOKENS_ENV") != "testing") {
            throw new IllegalStateException("calling testing function in non testing env")
        }
        initCalled = false
    }
    static Set<String> getHostsAliveForTesting() {
        if (System.getenv("SUPERTOKENS_ENV") != "testing") {
            throw new IllegalStateException("calling testing function in non testing env")
        }
        return Querier.hostsAliveForTesting
    }
    private static HttpURLConnection createConnection(String url, String method) {
        URL urlObj = new URL(url)
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection()
        connection.requestMethod = method
        return connection
    }

}
