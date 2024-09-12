package com.supertoken.recipe.userroles

import com.supertoken.NormalisedURLPath
import com.supertoken.Querier

class RecipeImplementation extends RecipeInterface {
    Querier querier

    RecipeImplementation(Querier querier) {
        this.querier = querier
    }

    AddRoleToUserOkResult addRoleToUser(String userId, String role, String tenantId, Map userContext) {
        Map params = [userId: userId, role: role]
        def response = querier.sendPutRequest(new NormalisedURLPath("${tenantId}/recipe/user/role"), params, userContext)
        if (response.getStatus() == 200) {
            return new AddRoleToUserOkResult(response.didUserAlreadyHaveRole)
        }
        return new UnknownRoleError()
    }

    RemoveUserRoleOkResult removeUserRole(String userId, String role, String tenantId, Map userContext) {
        Map params = [userId: userId, role: role]
        def response = querier.sendPostRequest(new NormalisedURLPath("${tenantId}/recipe/user/role/remove"), params, userContext)
        if (response.getStatus() == 200) {
            return new RemoveUserRoleOkResult(response.didUserHaveRole)
        }
        return new UnknownRoleError()
    }

    GetRolesForUserOkResult getRolesForUser(String userId, String tenantId, Map userContext) {
        Map params = [userId: userId]
        def response = querier.sendGetRequest(new NormalisedURLPath("${tenantId}/recipe/user/roles"), params, userContext)
        return new GetRolesForUserOkResult(response.roles)
    }

    GetUsersThatHaveRoleOkResult getUsersThatHaveRole(String role, String tenantId, Map userContext) {
        Map params = [role: role]
        def response = querier.sendGetRequest(new NormalisedURLPath("${tenantId}/recipe/role/users"), params, userContext)
        if (response.getStatus() == 200) {
            return new GetUsersThatHaveRoleOkResult(response.users)
        }
        return new UnknownRoleError()
    }

    CreateNewRoleOrAddPermissionsOkResult createNewRoleOrAddPermissions(String role, List<String> permissions, Map<String, Object> userContext) {
        Map params = [role: role, permissions: permissions]
        def response = querier.sendPutRequest(new NormalisedURLPath("/recipe/role"), params, userContext)
        return new CreateNewRoleOrAddPermissionsOkResult(response.createdNewRole)
    }

    GetPermissionsForRoleOkResult getPermissionsForRole(String role, Map userContext) {
        Map params = [role: role]
        def response = querier.sendGetRequest(new NormalisedURLPath("/recipe/role/permissions"), params, userContext)
        if (response.getStatus() == 200) {
            return new GetPermissionsForRoleOkResult(response.permissions)
        }
        return new UnknownRoleError()
    }

    RemovePermissionsFromRoleOkResult removePermissionsFromRole(String role, List<String> permissions, Map<String, Object> userContext) {
        Map params = [role: role, permissions: permissions]
        def response = querier.sendPostRequest(new NormalisedURLPath("/recipe/role/permissions/remove"), params, userContext)
        if (response.getStatus() == 200) {
            return new RemovePermissionsFromRoleOkResult()
        }
        return new UnknownRoleError()
    }

    GetRolesThatHavePermissionOkResult getRolesThatHavePermission(String permission, Map<String, Object> userContext) {
        Map params = [permission: permission]
        def response = querier.sendGetRequest(new NormalisedURLPath("/recipe/permission/roles"), params, userContext)
        return new GetRolesThatHavePermissionOkResult(response.roles)
    }

    DeleteRoleOkResult deleteRole(String role, Map userContext) {
        Map params = [role: role]
        def response = querier.sendPostRequest(new NormalisedURLPath("/recipe/role/remove"), params, userContext)
        return new DeleteRoleOkResult(response.didRoleExist)
    }


    GetAllRolesOkResult getAllRoles(Map<String, Object> userContext) {
        Map<String, Object> params = [:]
        def response = querier.sendGetRequest(new NormalisedURLPath("/recipe/roles"), params, userContext)
        return new GetAllRolesOkResult(response.roles)
    }
}
