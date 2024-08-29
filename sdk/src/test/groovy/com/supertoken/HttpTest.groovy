package com.supertoken

import spock.lang.Specification

class HttpTest extends Specification {

    def "check HTTPClient"() {
        setup:
        URL url = new URL("https://jsonplaceholder.typicode.com/todos/1")
        HttpURLConnection connection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("GET")
        connection.setConnectTimeout(30000)
        connection.setReadTimeout(30000)

        when:
        int responseCode = connection.getResponseCode()

        then:
        responseCode == 200

        when:
        def inputStream = connection.inputStream
        def responseText = inputStream.text
        inputStream.close()

        then:
        responseText != null
        println "Response Body: $responseText"
    }
}