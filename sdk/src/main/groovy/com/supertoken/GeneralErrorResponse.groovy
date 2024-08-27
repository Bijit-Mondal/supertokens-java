package com.supertoken

import groovy.transform.ToString

@ToString
class GeneralErrorResponse implements APIResponse {
    String status = "GENERAL_ERROR"
    String message

    GeneralErrorResponse(String message) {
        this.message = message
    }

    @Override
    Map<String, Object> toJson() {
        return [status: status, message: message]
    }
}