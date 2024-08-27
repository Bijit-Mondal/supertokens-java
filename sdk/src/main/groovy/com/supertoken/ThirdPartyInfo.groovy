package com.supertoken
import groovy.transform.ToString


@ToString
class ThirdPartyInfo implements Serializable {
    String userId
    String id

    ThirdPartyInfo(String thirdPartyUserId, String thirdPartyId) {
        this.userId = thirdPartyUserId
        this.id = thirdPartyId
    }
}