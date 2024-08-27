package com.supertoken

import groovy.transform.ToString

@ToString
class UsersResponse implements Serializable {
    List<User> users
    String nextPaginationToken

    UsersResponse(List<User> users, String nextPaginationToken = null) {
        this.users = users
        this.nextPaginationToken = nextPaginationToken
    }
}
