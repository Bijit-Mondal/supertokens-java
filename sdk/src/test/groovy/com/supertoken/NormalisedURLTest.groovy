package com.supertoken

import spock.lang.Specification

class NormalisedURLTest extends Specification{
    def "test normaliseURLDomainOrThrowError with valid URLs"() {
        setup:
        def domain = new NormalisedURLDomain(inputUrl)

        when:
        def result = domain.getAsStringDangerous()

        then:
        result == expectedOutput

        where:
        inputUrl                      || expectedOutput
        "http://example.com"           || "http://example.com"
        "https://example.com"          || "https://example.com"
        "http://localhost:3000"        || "http://localhost"
    }

    def "test normaliseURLPathOrThrowError with valid URLs"() {
        setup:
        def urlPath = new NormalisedURLPath(inputUrl)

        when:
        def result = urlPath.getAsStringDangerous()

        then:
        result == expectedOutput

        where:
        inputUrl                   || expectedOutput
        "http://example.com/path"  || "/path"
        "https://example.com/path/" || "/path"
        "/path"                     || "/path"
        "path"                      || "/path"
        "example.com/path"          || "/path"
        "localhost/path"            || "/path"
    }
    def "test normaliseURLPathOrThrowError with invalid URLs"() {
        when:
        new NormalisedURLPath(inputUrl)

        then:
        def e = thrown(Exception)
        e.message == errorMessage

        where:
        inputUrl               || errorMessage
        "/example.com"          || "Please provide a valid URL path"
        "invalid-url"           || "Please provide a valid URL path"
        "."                     || "Please provide a valid URL path"
    }

    def "test isARecipePath identifies recipe paths"() {
        setup:
        def path1 = new NormalisedURLPath("/api/recipe/something")
        def path2 = new NormalisedURLPath("/api/other")

        expect:
        path1.isARecipePath() == true
        path2.isARecipePath() == false
    }



}
