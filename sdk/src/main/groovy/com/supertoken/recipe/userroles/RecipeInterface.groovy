package com.supertoken.recipe.userroles

import groovy.transform.CompileStatic

@CompileStatic
class AddRoleToUserOkResult {
    boolean didUserAlreadyHaveRole

    AddRoleToUserOkResult(boolean didUserAlreadyHaveRole) {
        this.didUserAlreadyHaveRole = didUserAlreadyHaveRole
    }
}

@CompileStatic
class UnknownRoleError {
}

@CompileStatic
class RemoveUserRoleOkResult {
    boolean didUserHaveRole

    RemoveUserRoleOkResult(boolean didUserHaveRole) {
        this.didUserHaveRole = didUserHaveRole
    }
}

@CompileStatic
class GetRolesForUserOkResult {
    List<String> roles

    GetRolesForUserOkResult(List<String> roles) {
        this.roles = roles
    }
}

@CompileStatic
class GetUsersThatHaveRoleOkResult {
    List<String> users

    GetUsersThatHaveRoleOkResult(List<String> users) {
        this.users = users
    }
}

@CompileStatic
class CreateNewRoleOrAddPermissionsOkResult {
    boolean createdNewRole

    CreateNewRoleOrAddPermissionsOkResult(boolean createdNewRole) {
        this.createdNewRole = createdNewRole
    }
}

@CompileStatic
class GetPermissionsForRoleOkResult {
    List<String> permissions

    GetPermissionsForRoleOkResult(List<String> permissions) {
        this.permissions = permissions
    }
}

@CompileStatic
class RemovePermissionsFromRoleOkResult {
}

@CompileStatic
class GetRolesThatHavePermissionOkResult {
    List<String> roles

    GetRolesThatHavePermissionOkResult(List<String> roles) {
        this.roles = roles
    }
}

@CompileStatic
class DeleteRoleOkResult {
    boolean didRoleExist

    DeleteRoleOkResult(boolean didRoleExist) {
        this.didRoleExist = didRoleExist
    }
}

@CompileStatic
class GetAllRolesOkResult {
    List<String> roles

    GetAllRolesOkResult(List<String> roles) {
        this.roles = roles
    }
}

// Define abstract RecipeInterface class
@CompileStatic
abstract class RecipeInterface {

    abstract AddRoleToUserOkResult addRoleToUser(
            String userId,
            String role,
            String tenantId,
            Map<String, Object> userContext
    ) throws UnknownRoleError

    abstract RemoveUserRoleOkResult removeUserRole(
            String userId,
            String role,
            String tenantId,
            Map<String, Object> userContext
    ) throws UnknownRoleError

    abstract GetRolesForUserOkResult getRolesForUser(
            String userId,
            String tenantId,
            Map<String, Object> userContext
    )

    abstract GetUsersThatHaveRoleOkResult getUsersThatHaveRole(
            String role,
            String tenantId,
            Map<String, Object> userContext
    ) throws UnknownRoleError

    abstract CreateNewRoleOrAddPermissionsOkResult createNewRoleOrAddPermissions(
            String role,
            List<String> permissions,
            Map<String, Object> userContext
    )

    abstract GetPermissionsForRoleOkResult getPermissionsForRole(
            String role,
            Map<String, Object> userContext
    ) throws UnknownRoleError

    abstract RemovePermissionsFromRoleOkResult removePermissionsFromRole(
            String role,
            List<String> permissions,
            Map<String, Object> userContext
    ) throws UnknownRoleError

    abstract GetRolesThatHavePermissionOkResult getRolesThatHavePermission(
            String permission,
            Map<String, Object> userContext
    )

    abstract DeleteRoleOkResult deleteRole(
            String role,
            Map<String, Object> userContext
    )

    abstract GetAllRolesOkResult getAllRoles(Map<String, Object> userContext)
}

// Define abstract APIInterface class
@CompileStatic
abstract class APIInterface {
    // Implement API methods when needed
}
