package com.supertoken

class UserIdMappingResults {
    static class UnknownSupertokensUserIDError {
    }

    static class CreateUserIdMappingOkResult {
    }

    static class UserIdMappingAlreadyExistsError {
        boolean doesSuperTokensUserIdExist
        String doesExternalUserIdExist

        UserIdMappingAlreadyExistsError(boolean doesSuperTokensUserIdExist, String doesExternalUserIdExist) {
            this.doesSuperTokensUserIdExist = doesSuperTokensUserIdExist
            this.doesExternalUserIdExist = doesExternalUserIdExist
        }
    }

// Enum equivalent for the UserIDTypes Literal
    enum UserIDTypes {
        SUPERTOKENS, EXTERNAL, ANY
    }

    static class GetUserIdMappingOkResult {
        String supertokensUserId
        String externalUserId
        String externalUserInfo

        GetUserIdMappingOkResult(String supertokensUserId, String externalUserId, String externalUserInfo = null) {
            this.supertokensUserId = supertokensUserId
            this.externalUserId = externalUserId
            this.externalUserInfo = externalUserInfo
        }
    }

    static class UnknownMappingError {
    }

    static class DeleteUserIdMappingOkResult {
        boolean didMappingExist

        DeleteUserIdMappingOkResult(boolean didMappingExist) {
            this.didMappingExist = didMappingExist
        }
    }

    static class UpdateOrDeleteUserIdMappingInfoOkResult {
    }
}
