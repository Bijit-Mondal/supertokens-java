package com.supertoken

class PostSTInitCallbacks {
    static List<Closure<Void>> callbacks = []

    static void addPostInitCallback(Closure<Void> cb) {
        callbacks << cb
    }

    static void runPostInitCallbacks() {
        callbacks.each { it.call() }
        callbacks.clear()
    }
}