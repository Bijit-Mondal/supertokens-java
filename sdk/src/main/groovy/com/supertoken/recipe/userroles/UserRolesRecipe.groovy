package com.supertoken.recipe.userroles

import com.supertoken.APIHandled
import com.supertoken.AppInfo
import com.supertoken.NormalisedURLPath
import com.supertoken.PostSTInitCallbacks
import com.supertoken.Querier
import com.supertoken.RecipeModule
import com.supertoken.SuperTokensError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import java.util.concurrent.CompletableFuture

class UserRolesRecipe extends RecipeModule {

    static final String recipeId = "userroles"
    private static UserRolesRecipe instance

    UserRolesRecipe(String recipeId, AppInfo appInfo, Boolean skipAddingRolesToAccessToken = null,
                    Boolean skipAddingPermissionsToAccessToken = null, InputOverrideConfig override = null) {
        super(recipeId, appInfo)
        def config = UserRolesConfig.validateAndNormaliseUserInput(
                this, appInfo, skipAddingRolesToAccessToken, skipAddingPermissionsToAccessToken, override
        )
        def recipeImplementation = new RecipeImplementation(Querier.getInstance(recipeId))
        recipeImplementation = config.override?.functions ?
                config.override.functions(recipeImplementation) : recipeImplementation

//        def callback = {
//            if (!config.skipAddingRolesToAccessToken) {
//                SessionRecipe.getInstance().addClaimFromOtherRecipe(UserRoleClaim)
//            }
//            if (!config.skipAddingPermissionsToAccessToken) {
//                SessionRecipe.getInstance().addClaimFromOtherRecipe(PermissionClaim)
//            }
//        }
//        PostSTInitCallbacks.addPostInitCallback(callback)
    }

    static boolean isErrorFromThisRecipeBasedOnInstance(Exception err) {
        return err instanceof SuperTokensError && (err instanceof SuperTokensUserRolesError)
    }

    static UserRolesRecipe init(Boolean skipAddingRolesToAccessToken = null, Boolean skipAddingPermissionsToAccessToken = null, InputOverrideConfig override = null) {
        return { AppInfo appInfo ->
            if (instance == null) {
                instance = new UserRolesRecipe(recipeId, appInfo, skipAddingRolesToAccessToken, skipAddingPermissionsToAccessToken, override)
                return instance
            }
            throw new Exception("UserRoles recipe has already been initialised. Please check your code for bugs.")
        } as UserRolesRecipe
    }

    static void reset() {
        if (!System.getenv("SUPERTOKENS_ENV")?.equalsIgnoreCase("testing")) {
            throw new Exception("calling testing function in non testing env")
        }
        instance = null
    }

    static UserRolesRecipe getInstance() {
        if (instance != null) {
            return instance
        }
        throw new Exception("Initialisation not done. Did you forget to call the SuperTokens.init or UserRoles.init function?")
    }

    List<APIHandled> getApisHandled() {
        return []
    }

    CompletableFuture<HttpServletResponse> handleApiRequest(String requestId, String tenantId, HttpServletRequest request, NormalisedURLPath path, String method, HttpServletResponse response, Map userContext) {
        throw new Exception("Should never come here")
    }

    CompletableFuture<HttpServletResponse> handleError(HttpServletRequest request, SuperTokensError err, HttpServletResponse response, Map userContext) {
        throw new Exception("Should never come here")
    }

    List<String> getAllCorsHeaders() {
        return []
    }
}
