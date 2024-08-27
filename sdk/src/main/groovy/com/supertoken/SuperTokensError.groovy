package com.supertoken

import groovy.transform.CompileStatic


@CompileStatic
class SuperTokensError extends Exception {
    private static final String ERR_MAGIC = "ndskajfasndlfkj435234krjdsa"
    static final String BAD_INPUT_ERROR = "BAD_INPUT_ERROR"

    String type
    def payload
    String fromRecipe

    private String errMagic

    // Constructor
    SuperTokensError(Map<String, Object> options) {
        super(options.message as String)
        this.type = options.type as String
        this.payload = options.payload
        this.errMagic = ERR_MAGIC
    }

    // Static method to check if an object is an instance of SuperTokensError
    static boolean isErrorFromSuperTokens(Object obj) {
        return (obj instanceof SuperTokensError) && ((SuperTokensError) obj).errMagic == ERR_MAGIC
    }
}
