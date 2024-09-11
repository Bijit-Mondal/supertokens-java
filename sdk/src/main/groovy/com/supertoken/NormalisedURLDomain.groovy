package com.supertoken

import static com.supertoken.Utils.isAnIpAddress

class NormalisedURLDomain {
    private String value

    NormalisedURLDomain(String url) {
        this.value = normaliseURLDomainOrThrowError(url)
    }

    String getAsStringDangerous() {
        return this.value
    }

    String normaliseURLDomainOrThrowError(String input, boolean ignoreProtocol = false) {
        input = input.trim().toLowerCase()

        try {
            if (!input.startsWith("http://") && !input.startsWith("https://") && !input.startsWith("supertokens://")) {
                throw new IllegalArgumentException("Convert to proper URL")
            }

            URL urlObj = new URL(input)
            if (ignoreProtocol) {
                if (urlObj.host.startsWith("localhost") || isAnIpAddress(urlObj.host)) {
                    input = "http://${urlObj.host}"
                } else {
                    input = "https://${urlObj.host}"
                }
            } else {
                input = "${urlObj.protocol}://${urlObj.host}"
            }

            return input
        } catch (MalformedURLException ignored) {
        }

        // Not a valid URL
        if (input.startsWith("/")) {
            throw new IllegalArgumentException("Please provide a valid domain name")
        }

        if (input.indexOf(".") == 0) {
            input = input.substring(1)
        }

        // If the input contains a . it means they have given a domain name.
        // So we try assuming that they have given a domain name
        if ((input.indexOf(".") != -1 || input.startsWith("localhost")) &&
                !input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://${input}"

            // At this point, it should be a valid URL. So we test that before doing a recursive call
            try {
                new URL(input)
                return normaliseURLDomainOrThrowError(input, true)
            } catch (MalformedURLException ignored) {
            }
        }

        throw new IllegalArgumentException("Please provide a valid domain name")
    }
}