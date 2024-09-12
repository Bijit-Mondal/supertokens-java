package com.supertoken

import groovy.json.JsonBuilder
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.util.concurrent.CompletableFuture


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
    List<RecipeModule> recipeModules

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

        Closure<RecipeModule> makeRecipe = { recipe ->
            def recipeModule = recipe(this.appInfo)
            if (recipeModule.getRecipeId() == MultitenancyRecipe.recipeId) {
                multitenancyFound = true
            }
            return recipeModule
        }
        this.recipeModules = recipeList.collect(makeRecipe)
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
                System.getenv("SUPERTOKENS_ENV") != "testing") {
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

    def getAllCorsHeaders() {
        def headersSet = [] as Set
        headersSet.add(Constants.RID_KEY_HEADER)
        headersSet.add(Constants.FDI_KEY_HEADER)
        recipeModules.each { recipe ->
            def headers = recipe.getAllCorsHeaders()
            headers.each { header ->
                headersSet.add(header)
            }
        }
        return headersSet.toList()
    }

    static def getUserCount(
            List<String> includeRecipeIds = null,
            String tenantId = null,
            Map<String, Object> userContext = null
    ) {
        CompletableFuture.supplyAsync {
            def querier = Querier.getInstance(null)
            def includeRecipeIdsStr = includeRecipeIds ? includeRecipeIds.join(",") : null

            def response = querier.sendGetRequest(
                    new NormalisedURLPath("/${tenantId ?: 'public'}${Constants.USER_COUNT}"),
                    [
                            "includeRecipeIds": includeRecipeIdsStr,
                            "includeAllTenants": tenantId == null
                    ],
                    userContext
            )

            return response["count"]
        }
    }

    static def createUserIdMapping(
            String supertokensUserId,
            String externalUserId,
            String externalUserIdInfo = null,
            Boolean force = null,
            Map<String, Object> userContext = null
    ) {
        CompletableFuture.supplyAsync {
            def querier = Querier.getInstance(null)

            String cdiVersion = querier.getApiVersion(userContext)

            if (Utils.isVersionGte(cdiVersion, "2.15")) {
                def body = [
                        "superTokensUserId": supertokensUserId,
                        "externalUserId"   : externalUserId,
                        "externalUserIdInfo": externalUserIdInfo
                ]
                if (force != null) {
                    body["force"] = force
                }

                def res = querier.sendPostRequest(
                        new NormalisedURLPath("/recipe/userid/map"), body, userContext
                )

                switch (res["status"]) {
                    case "OK":
                        return new UserIdMappingResults.CreateUserIdMappingOkResult()
                    case "UNKNOWN_SUPERTOKENS_USER_ID_ERROR":
                        return new UserIdMappingResults.UnknownSupertokensUserIDError()
                    case "USER_ID_MAPPING_ALREADY_EXISTS_ERROR":
                        return new UserIdMappingResults.UserIdMappingAlreadyExistsError(
                                res["doesSuperTokensUserIdExist"] as boolean,
                                res["doesExternalUserIdExist"] as String
                        )
                    default:
                        throw new Exception("Unknown response")
                }
            }

            throw new Exception("Please upgrade the SuperTokens core to >= 3.15.0")
        }
    }

    static def getUserIdMapping(
            String userId,
            UserIdMappingResults.UserIDTypes userIdType = null,
            Map<String, Object> userContext = null
    ) {
        def querier = Querier.getInstance(null)

        // Assuming getApiVersion returns a String
        def cdiVersion = querier.getApiVersion(userContext)

        if (Utils.isVersionGte(cdiVersion, "2.15")) {
            Map<String, Object> body = [
                    "userId": userId
            ]
            if (userIdType) {
                body["userIdType"] = userIdType.toString()
            }

            // Use appropriate request handling here
            def res = querier.sendGetRequest(
                    new NormalisedURLPath("/recipe/userid/map"),
                    body,
                    userContext
            )

            switch (res["status"]) {
                case "OK":
                    return new UserIdMappingResults.GetUserIdMappingOkResult(
                            res["superTokensUserId"] as String,
                            res["externalUserId"] as String,
                            res("externalUserIdInfo") as String
                    )
                case "UNKNOWN_MAPPING_ERROR":
                    return new UserIdMappingResults.UnknownMappingError()
                default:
                    throw new Exception("Unknown response")
            }
        }

        throw new Exception("Please upgrade the SuperTokens core to >= 3.15.0")
    }

    static def deleteUserIdMapping(
            String userId,
            UserIdMappingResults.UserIDTypes userIdType = null,
            Boolean force = null,
            Map<String, Object> userContext = null
    ) {
        def querier = Querier.getInstance(null)

        // Assuming getApiVersion returns a String
        def cdiVersion = querier.getApiVersion(userContext)

        if (Utils.isVersionGte(cdiVersion, "2.15")) {
            def body = [
                    "userId": userId,
                    "userIdType": userIdType?.toString()
            ]
            if (force != null) {
                body["force"] = force
            }

            // Use appropriate request handling here
            def res = querier.sendPostRequest(
                    new NormalisedURLPath("/recipe/userid/map/remove"),
                    body,
                    userContext
            )

            switch (res["status"]) {
                case "OK":
                    return new UserIdMappingResults.DeleteUserIdMappingOkResult(
                            res["didMappingExist"] as boolean
                    )
                default:
                    throw new Exception("Unknown response")
            }
        }

        throw new Exception("Please upgrade the SuperTokens core to >= 3.15.0")
    }

    static def updateOrDeleteUserIdMappingInfo(
            String userId,
            UserIdMappingResults.UserIDTypes userIdType = null,
            String externalUserIdInfo = null,
            Map<String, Object> userContext = null
    ) {
        def querier = Querier.getInstance(null)

        // Assuming getApiVersion returns a String
        def cdiVersion = querier.getApiVersion(userContext)

        if (Utils.isVersionGte(cdiVersion, "2.15")) {
            def body = [
                    "userId": userId,
                    "userIdType": userIdType?.toString(),
                    "externalUserIdInfo": externalUserIdInfo
            ]

            // Use appropriate request handling here
            def res = querier.sendPostRequest(
                    new NormalisedURLPath("/recipe/userid/external-user-id-info"),
                    body,
                    userContext
            )

            switch (res["status"]) {
                case "OK":
                    return new UserIdMappingResults.UpdateOrDeleteUserIdMappingInfoOkResult()
                case "UNKNOWN_MAPPING_ERROR":
                    return new UserIdMappingResults.UnknownMappingError()
                default:
                    throw new Exception("Unknown response")
            }
        }

        throw new Exception("Please upgrade the SuperTokens core to >= 3.15.0")
    }

    static def getUsers(
            String tenantId,
            String timeJoinedOrder,
            Integer limit = null,
            String paginationToken = null,
            List<String> includeRecipeIds = null,
            Map<String, String> query = null,
            Map<String, Object> userContext = null
    ) {
        def querier = Querier.getInstance(null)

        Map<String,Object> params = [ "timeJoinedOrder": timeJoinedOrder ]
        if (limit != null) {
            params["limit"] = limit
        }
        if (paginationToken != null) {
            params["paginationToken"] = paginationToken
        }
        if (includeRecipeIds != null) {
            params["includeRecipeIds"] = includeRecipeIds.join(",")
        }
        if (query != null) {
            params.putAll(query)
        }

        // Simulate async behavior
        def response = querier.sendGetRequest(
                new NormalisedURLPath("/${tenantId}${Constants.USERS}"),
                params,
                userContext
        )

        String nextPaginationToken = response.getAt("nextPaginationToken")
        def usersList = response.getAt("users")
        List<User> users = []

        usersList.each { user ->
            String recipeId = user["recipeId"]
            def userObj = user["user"]
            ThirdPartyInfo thirdParty = userObj.getAt(userObj,"thirdParty") ? new ThirdPartyInfo(userObj["thirdParty"]["userId"], userObj["thirdParty"]["id"]) : null
            String email = userObj?.getAt("email")
            String phoneNumber = userObj?.getAt("phoneNumber")

            users << new User(
                    recipeId,
                    userObj["id"] as String,
                    userObj["timeJoined"] as Integer,
                    email,
                    phoneNumber,
                    thirdParty,
                    userObj["tenantIds"] as List<String>
            )
        }

        return new UsersResponse(users, nextPaginationToken)
    }

    static def deleteUser(String userId, Map<String, Object> userContext = [:]) {
        def querier = Querier.getInstance(null)


        def cdiVersion = querier.getApiVersion(userContext)

        if (Utils.isVersionGte(cdiVersion, "2.10")) {
            // Send post request to delete user
            querier.sendPostRequest(
                    new NormalisedURLPath(Constants.USER_DELETE),
                    [ "userId": userId ],
                    userContext
            )

            return null
        }
        throw new Exception("Please upgrade the SuperTokens core to >= 3.7.0")
    }

    static def middleware(HttpServletRequest request, HttpServletResponse response, Map<String, Object> userContext) {
        SuperTokensDebug.logDebugMessage("middleware: Started")
        NormalisedURLPath path = getInstance().appInfo.apiGatewayPath.appendPath(
                new NormalisedURLPath(request.getRequestURI())
        )
        String method = Utils.normaliseHttpMethod(request.getMethod())

        if (!path.startsWith(getInstance().appInfo.apiBasePath)) {
            SuperTokensDebug.logDebugMessage(
                    "middleware: Not handling because request path did not start with api base path. Request path: ${path.getAsStringDangerous()}"
            )
            return null
        }

        def requestRid = Utils.getRidFromHeader(request)
        SuperTokensDebug.logDebugMessage("middleware: requestRID is: ${requestRid}")

        if (requestRid != null && requestRid == "anti-csrf") {
            requestRid = null
        }

        def handleWithoutRid = {
            for (recipe in this.recipeModules) {
                SuperTokensDebug.logDebugMessage(
                        "middleware: Checking recipe ID for match: ${recipe.getRecipeId()} with path: ${path.getAsStringDangerous()} and method: $method"
                )
                def apiAndTenantId = recipe.returnApiIdIfCanHandleRequest(path, method, userContext)
                if (apiAndTenantId != null) {
                    SuperTokensDebug.logDebugMessage("middleware: Request being handled by recipe. ID is: ${apiAndTenantId?.apiId}")
                    def apiResp = recipe.handleApiRequest(
                            apiAndTenantId?.apiId, apiAndTenantId?.tenantId, request, path, method, response, userContext
                    )
                    if (apiResp == null) {
                        SuperTokensDebug.logDebugMessage("middleware: Not handled because API returned None")
                        return null
                    }
                    SuperTokensDebug.logDebugMessage("middleware: Ended")
                    return apiResp
                }
            }
            SuperTokensDebug.logDebugMessage("middleware: Not handling because no recipe matched")
            return null
        }

        if (requestRid != null) {
            def matchedRecipes = this.recipeModules.findAll { recipe ->
                recipe.getRecipeId() == requestRid ||
                        (requestRid == "thirdpartyemailpassword" && ["thirdparty", "emailpassword"].contains(recipe.getRecipeId())) ||
                        (requestRid == "thirdpartypasswordless" && ["thirdparty", "passwordless"].contains(recipe.getRecipeId()))
            }

            if (matchedRecipes.isEmpty()) {
                SuperTokensDebug.logDebugMessage("middleware: Not handling based on rid match. Trying without rid.")
                return handleWithoutRid()
            }

            matchedRecipes.each { recipe ->
                SuperTokensDebug.logDebugMessage("middleware: Matched with recipe Ids: ${recipe.getRecipeId()}")
            }

            def idResult = null
            def finalMatchedRecipe = null
            matchedRecipes.each { recipe ->
                def currentIdResult = recipe.returnApiIdIfCanHandleRequest(path, method, userContext)
                if (currentIdResult != null) {
                    if (idResult != null) {
                        throw new IllegalArgumentException(
                                "Two recipes have matched the same API path and method! This is a bug in the SDK. Please contact support."
                        )
                    }
                    finalMatchedRecipe = recipe
                    idResult = currentIdResult
                }
            }

            if (idResult == null || finalMatchedRecipe == null) {
                return handleWithoutRid()
            }

            SuperTokensDebug.logDebugMessage("middleware: Request being handled by recipe. ID is: ${idResult.apiId}")
            def requestHandled = finalMatchedRecipe.handleApiRequest(
                    idResult.apiId, idResult.tenantId, request, path, method, response, userContext
            )
            if (requestHandled == null) {
                SuperTokensDebug.logDebugMessage("middleware: Not handled because API returned request_handled as None")
                return null
            }
            SuperTokensDebug.logDebugMessage("middleware: Ended")
            return requestHandled
        }

        return handleWithoutRid()
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