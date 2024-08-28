package com.supertoken

import groovy.transform.CompileStatic

@CompileStatic
class NormalisedURLPath {
    private String value

    NormalisedURLPath(String url) {
        this.value = normaliseURLPathOrThrowError(url)
    }

    boolean startsWith(NormalisedURLPath other) {
        return this.value.startsWith(other.value)
    }

    NormalisedURLPath appendPath(NormalisedURLPath other) {
        return new NormalisedURLPath(this.value + other.value)
    }

    String getAsStringDangerous() {
        return this.value
    }

    boolean equals(NormalisedURLPath other) {
        return this.value == other.value
    }

    boolean isARecipePath() {
        def parts = this.value.split("/")
        return parts.size() > 1 && (parts[1] == "recipe" || (parts.size() > 2 && parts[2] == "recipe"))
    }

    private static String normaliseURLPathOrThrowError(String input) {
        input = input.trim().toLowerCase()

        try {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                throw new Exception("converting to proper URL")
            }
            def urlObj = new URL(input)
            input = urlObj.getPath()

            if (input.endsWith("/")) {
                return input.substring(0, input.length() - 1)
            }

            return input
        } catch (Exception ignored) {
            // Handle exception
        }

        // not a valid URL

        // If the input contains a dot, it means they have given a domain name.
        // So we try assuming that they have given a domain name + path
        if ((domainGiven(input) || input.startsWith("localhost")) &&
                !input.startsWith("http://") &&
                !input.startsWith("https://")) {
            input = "http://" + input
            return normaliseURLPathOrThrowError(input)
        }

        if (!input.startsWith("/")) {
            input = "/" + input
        }

        // at this point, we should be able to convert it into a fake URL and recursively call this function.
        try {
            // test that we can convert this to prevent an infinite loop
            new URL("http://example.com" + input)

            return normaliseURLPathOrThrowError("http://example.com" + input)
        } catch (Exception e) {
            throw new Exception("Please provide a valid URL path")
        }
    }

    private static boolean domainGiven(String input) {
        // If no dot, return false.
        if (!input.contains(".") || input.startsWith("/")) {
            return false
        }

        try {
            def url = new URL(input)
            return url.getHost().contains(".")
        } catch (Exception ignored) {
            // Handle exception
        }

        try {
            def url = new URL("http://" + input)
            return url.getHost().contains(".")
        } catch (Exception ignored) {
            // Handle exception
        }

        return false
    }
}