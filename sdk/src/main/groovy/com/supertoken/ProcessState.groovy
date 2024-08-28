package com.supertoken

import java.util.concurrent.CompletableFuture

enum PROCESS_STATE {
    CALLING_SERVICE_IN_VERIFY,
    CALLING_SERVICE_IN_GET_API_VERSION,
    CALLING_SERVICE_IN_REQUEST_HELPER,
    MULTI_JWKS_VALIDATION,

    IS_SIGN_IN_UP_ALLOWED_NO_PRIMARY_USER_EXISTS,
    IS_SIGN_UP_ALLOWED_CALLED,
    IS_SIGN_IN_ALLOWED_CALLED,
    IS_SIGN_IN_UP_ALLOWED_HELPER_CALLED,

    ADDING_NO_CACHE_HEADER_IN_FETCH
}

class ProcessState {
    List<PROCESS_STATE> history = []
    private static ProcessState instance

    private ProcessState() {}

    static ProcessState getInstance() {
        if (instance == null) {
            instance = new ProcessState()
        }
        return instance
    }

    void addState(PROCESS_STATE state) {
        if (System.getenv('TEST_MODE') == 'testing') {
            history.add(state)
        }
    }

    private PROCESS_STATE getEventByLastEventByName(PROCESS_STATE state) {
        history.reverse().find { it == state }
    }

    void reset() {
        history.clear()
    }

//    CompletableFuture<PROCESS_STATE> waitForEvent(PROCESS_STATE state, long timeInMS = 7000) {
//        long startTime = System.currentTimeMillis()
//        CompletableFuture<PROCESS_STATE> future = new CompletableFuture<>()
//        Runnable tryAndGet = {
//            PROCESS_STATE result = getEventByLastEventByName(state)
//            if (result == null) {
//                if (System.currentTimeMillis() - startTime > timeInMS) {
//                    future.complete(null)
//                } else {
//                    new Timer().schedule({ tryAndGet.run() } as TimerTask, 1000)
//                }
//            } else {
//                future.complete(result)
//            }
//        }
//        tryAndGet.run()
//        return future
//    }
}
