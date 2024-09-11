package com.supertoken

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

abstract class RecipeModule {
    String recipeId
    AppInfo appInfo
    static Closure<String> getTenantId = null

    RecipeModule(String recipeId, AppInfo appInfo) {
        this.recipeId = recipeId
        this.appInfo = appInfo
    }

    String getRecipeId() {
        return recipeId
    }

    AppInfo getAppInfo() {
        return appInfo
    }

   def returnApiIdIfCanHandleRequest(NormalisedURLPath path, String method, Map<String, Object> userContext) {
        def DEFAULT_TENANT_ID = "public" // replace with actual value
        List<APIHandled> apisHandled = getApisHandled()

        String basePathStr = appInfo.apiBasePath.getAsStringDangerous()
        String pathStr = path.getAsStringDangerous()
        String regex = '^' + Pattern.quote(basePathStr) + '(?:/([a-zA-Z0-9-]+))?(/.*)$'

        def matcher = Pattern.compile(regex).matcher(pathStr)
        String matchGroup1 = matcher.find() ? matcher.group(1) : null
        String matchGroup2 = matcher.find() ? matcher.group(2) : null

        String tenantId = DEFAULT_TENANT_ID
        NormalisedURLPath remainingPath = null

        if (matchGroup1 && matchGroup2) {
            tenantId = matchGroup1
            remainingPath = new NormalisedURLPath(matchGroup2)
        }

        assert getTenantId != null
        assert getTenantId instanceof Closure

        def future = CompletableFuture.supplyAsync {
            def finalResult = null
            for (currentApi in apisHandled) {
                if (!currentApi.disabled && currentApi.method == method) {
                    if (appInfo.apiBasePath.appendPath(currentApi.pathWithoutApiBasePath) == path) {
                        // Ensure getTenantId returns a CompletableFuture
                        finalResult = CompletableFuture.supplyAsync {
                            getTenantId.call(DEFAULT_TENANT_ID, userContext)
                        }.thenApply { finalTenantId ->
                            new ApiIdWithTenantId(currentApi.requestId, finalTenantId)
                        }
                        break
                    }

                    if (remainingPath != null && appInfo.apiBasePath.appendPath(currentApi.pathWithoutApiBasePath) == appInfo.apiBasePath.appendPath(remainingPath)) {
                        finalResult = CompletableFuture.supplyAsync {
                            getTenantId.call(tenantId, userContext)
                        }.thenApply { finalTenantId ->
                            new ApiIdWithTenantId(currentApi.requestId, finalTenantId)
                        }
                        break
                    }
                }
            }
            return finalResult ?: CompletableFuture.completedFuture(null)
        }

        return future.thenCompose { it }

    }


    abstract boolean isErrorFromThisRecipeBasedOnInstance(Exception err)

    abstract List<APIHandled> getApisHandled()

    abstract CompletableFuture<HttpServletResponse> handleApiRequest(String requestId, String tenantId, HttpServletRequest request,
                                                                     NormalisedURLPath path, String method, HttpServletResponse response,
                                                                     Map<String, Object> userContext)

    abstract CompletableFuture<HttpServletResponse> handleError(HttpServletRequest request, SuperTokensError err, HttpServletResponse response,
                                                         Map<String, Object> userContext)

    abstract List<String> getAllCorsHeaders()
}

class APIHandled {
    NormalisedURLPath pathWithoutApiBasePath
    String method
    String requestId
    boolean disabled

    APIHandled(NormalisedURLPath pathWithoutApiBasePath, String method, String requestId, boolean disabled) {
        this.pathWithoutApiBasePath = pathWithoutApiBasePath
        this.method = method
        this.requestId = requestId
        this.disabled = disabled
    }
}

class ApiIdWithTenantId {
    String apiId
    String tenantId

    ApiIdWithTenantId(String apiId, String tenantId) {
        this.apiId = apiId
        this.tenantId = tenantId
    }
}
