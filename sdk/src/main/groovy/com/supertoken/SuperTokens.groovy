package com.supertoken

import groovy.json.JsonBuilder
import jakarta.servlet.http.HttpServletRequest


class SupertokensConfig {
    String connectionUri
    String apiKey
    Closure<Tuple5<String, String, Map<String, Object>, Map<String, Object>, Map<String, Object>>> networkInterceptor
    boolean disableCoreCallCache

    SupertokensConfig(String connectionUri, String apiKey = null, Closure networkInterceptor = null, boolean disableCoreCallCache = false) {
        this.connectionUri = connectionUri
        this.apiKey = apiKey
        this.networkInterceptor = networkInterceptor
        this.disableCoreCallCache = disableCoreCallCache
    }
}

class InputAppInfo {
    String appName
    String apiDomain
    String apiGatewayPath = ""
    String apiBasePath = "/auth"
    String websiteBasePath = "/auth"
    String websiteDomain
    def origin

    InputAppInfo(String appName, String apiDomain, String apiGatewayPath = "", String apiBasePath = "/auth", String websiteBasePath = "/auth", String websiteDomain = null, def origin = null) {
        this.appName = appName
        this.apiGatewayPath = apiGatewayPath
        this.apiDomain = apiDomain
        this.websiteDomain = websiteDomain
        this.origin = origin
        this.apiBasePath = apiBasePath
        this.websiteBasePath = websiteBasePath
    }
}

class AppInfo {
    String appName
    NormalisedURLDomain apiDomain
    NormalisedURLPath apiGatewayPath
    NormalisedURLPath apiBasePath
    String topLevelApiDomain
    String framework
    String websiteBasePath
    def origin
    String websiteDomain

    AppInfo(String appName, String apiDomain, String websiteDomain, String framework, String apiGatewayPath, String apiBasePath, String websiteBasePath, def origin) {
        this.appName = appName
        this.apiGatewayPath = new NormalisedURLPath(apiGatewayPath)
        this.apiDomain = new NormalisedURLDomain(apiDomain)
        this.topLevelApiDomain = Utils.getTopLevelDomainForSameSiteResolution(this.apiDomain.getAsStringDangerous())

        if (websiteDomain == null && origin == null) {
            throw new Exception("Please provide at least one of websiteDomain or origin")
        }

        this.origin = origin
        this.websiteDomain = websiteDomain
        this.apiBasePath = this.apiGatewayPath.appendPath(new NormalisedURLPath(apiBasePath))
        this.websiteBasePath = new NormalisedURLPath(websiteBasePath)

        this.framework = framework
    }

    String getTopLevelWebsiteDomain(HttpServletRequest request, Map<String, Object> userContext) {
        return Utils.getTopLevelDomainForSameSiteResolution(getOrigin(request, userContext).getAsStringDangerous())
    }

    NormalisedURLDomain getOrigin(HttpServletRequest request, Map<String, Object> userContext) {
        String origin = this.origin
        if (origin == null) {
            origin = this.websiteDomain
        }

        // This should not be possible because we check for either origin or websiteDomain above
        if (origin == null) {
            throw new Exception("should never come here")
        }

        if (origin instanceof Closure) {
            origin = origin.call(request, userContext)
        }

        return new NormalisedURLDomain(origin)
    }

    String toJSON() {
        def defaultImpl = { o ->
            if (o instanceof NormalisedURLDomain)
                return o.getAsStringDangerous()
            if(o instanceof NormalisedURLPath)
                return o.getAsStringDangerous()
            return o.properties
        }
        return new JsonBuilder(this).toPrettyString()
    }
}

class SuperTokens {
    private static SuperTokens instance = null
    AppInfo appInfo
    SupertokensConfig supertokensConfig
    String framework
    List<Closure<RecipeModule>> recipeList
    Boolean telemetry
    String telemetryStatus = "NONE"
    Boolean debug

    SuperTokens(InputAppInfo appInfo,String framework, SupertokensConfig supertokensConfig,
                List<Closure<RecipeModule>> recipeList, Boolean telemetry, Boolean debug ){
        if(!(appInfo instanceof InputAppInfo)){
            throw new IllegalArgumentException("appInfo must be an instance of InputAppInfo")
        }
        this.appInfo = new AppInfo(
                appInfo.appName, appInfo.apiDomain, appInfo.websiteDomain, framework,
                appInfo.apiGatewayPath, appInfo.apiBasePath, appInfo.websiteBasePath, appInfo.origin
        )
        this.supertokensConfig = supertokensConfig
        if (debug)
            SuperTokensDebug.enableDebugLogs()
        this.telemetryStatus = "NONE"
        SuperTokensDebug.logDebugMessage("Started SuperTokens with debug logging (supertokens.init called)")
        SuperTokensDebug.logDebugMessage("app_info: ${this.appInfo.toJSON()}")
        SuperTokensDebug.logDebugMessage("framework: ${framework}")
        List<Host> hosts = supertokensConfig.connectionUri.split(";").findAll { it != "" }.collect {
            new Host(new NormalisedURLDomain(it.trim()), new NormalisedURLPath(it.trim()))
        }

        Querier.init(
                hosts,
                supertokensConfig.apiKey,
                supertokensConfig.networkInterceptor,
                supertokensConfig.disableCoreCallCache
        )

        if (recipeList.size() == 0) {
            throw new Exception("Please provide at least one recipe to the supertokens.init function call")
        }
        boolean multitenancyFound = false

//        Closure<RecipeModule> makeRecipe = { recipe ->
//            def recipeModule = recipe(this.appInfo)
//            if (recipeModule.getRecipeId() == MultitenancyRecipe.recipeId) {
//                multitenancyFound = true
//            }
//            return recipeModule
//        }
//        this.recipeModules = recipeList.collect(makeRecipe)
        this.telemetry = telemetry != null ? telemetry : (System.getenv("TEST_MODE") != "testing")

    }
    static void init(
            InputAppInfo appInfo,
            String framework,
            SupertokensConfig supertokensConfig,
            List<Closure<RecipeModule>> recipeList,
            Boolean telemetry = null,
            Boolean debug = null
    ) {
        if (instance == null) {
            instance = new SuperTokens(
                    appInfo,
                    framework,
                    supertokensConfig,
                    recipeList,
                    telemetry,
                    debug
            )
            PostSTInitCallbacks.runPostInitCallbacks()
        }
    }
    static void reset() {
        if (!System.getenv().containsKey("SUPERTOKENS_ENV") ||
                !System.getenv("SUPERTOKENS_ENV").equals("testing")) {
            throw new IllegalStateException("calling testing function in non testing env")
        }
        Querier.reset()
        instance = null
    }
    static SuperTokens getInstance() {
        if (instance != null) {
            return instance
        }
        throw new IllegalStateException("Initialisation not done. Did you forget to call the SuperTokens.init function?")
    }

    static HttpServletRequest getRequestFromUserContext(Map<String, Object> userContext = null) {
        if (userContext == null) {
            return null
        }

        if (!userContext.containsKey("_default")) {
            return null
        }

        if (!(userContext.get("_default") instanceof Map)) {
            return null
        }

        return ((Map) userContext.get("_default")).get("request") as HttpServletRequest
    }


}