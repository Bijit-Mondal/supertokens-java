package com.supertoken.recipe.userroles

import com.supertoken.AppInfo

class InputOverrideConfig {
    Closure<RecipeInterface> functions
    Closure<APIInterface> apis

    InputOverrideConfig(Closure<RecipeInterface> functions = null, Closure<APIInterface> apis = null) {
        this.functions = functions
        this.apis = apis
    }
}

class UserRolesConfig {
    boolean skipAddingRolesToAccessToken
    boolean skipAddingPermissionsToAccessToken
    InputOverrideConfig override

    UserRolesConfig(boolean skipAddingRolesToAccessToken, boolean skipAddingPermissionsToAccessToken, InputOverrideConfig override) {
        this.skipAddingRolesToAccessToken = skipAddingRolesToAccessToken
        this.skipAddingPermissionsToAccessToken = skipAddingPermissionsToAccessToken
        this.override = override
    }

    static UserRolesConfig validateAndNormaliseUserInput(UserRolesRecipe recipe, AppInfo appInfo, boolean skipAddingRolesToAccessToken = false, boolean skipAddingPermissionsToAccessToken = false, InputOverrideConfig override = null) {
        if (override != null && !(override instanceof InputOverrideConfig)) {
            throw new IllegalArgumentException("override must be an instance of InputOverrideConfig or null")
        }

        if (override == null) {
            override = new InputOverrideConfig()
        }

        return new UserRolesConfig(
                skipAddingRolesToAccessToken,
                skipAddingPermissionsToAccessToken,
                override
        )
    }
}
