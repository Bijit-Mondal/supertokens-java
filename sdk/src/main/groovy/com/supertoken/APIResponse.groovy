package com.supertoken

interface APIResponse extends Serializable {
    Map<String, Object> toJson()
}