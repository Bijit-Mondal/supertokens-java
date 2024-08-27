package com.supertoken

import groovy.transform.ToString

@ToString
class User implements Serializable {
    String recipeId
    String userId
    String email
    int timeJoined
    String phoneNumber
    ThirdPartyInfo thirdPartyInfo
    List<String> tenantIds

    User(String recipeId, String userId, int timeJoined, String email = null,
         String phoneNumber = null, ThirdPartyInfo thirdPartyInfo = null,
         List<String> tenantIds) {
        this.recipeId = recipeId
        this.userId = userId
        this.email = email
        this.timeJoined = timeJoined
        this.thirdPartyInfo = thirdPartyInfo
        this.phoneNumber = phoneNumber
        this.tenantIds = tenantIds
    }

    Map<String, Object> toJson() {
        def res = [
                recipeId: recipeId,
                user: [
                        id: userId,
                        timeJoined: timeJoined,
                        tenantIds: tenantIds
                ]
        ]

        if (email) {
            res.user.email = email
        }
        if (phoneNumber) {
            res.user.phoneNumber = phoneNumber
        }
        if (thirdPartyInfo) {
            res.user.thirdParty = thirdPartyInfo.properties
        }

        return res
    }
}