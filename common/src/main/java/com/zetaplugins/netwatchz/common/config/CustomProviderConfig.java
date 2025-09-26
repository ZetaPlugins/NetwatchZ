package com.zetaplugins.netwatchz.common.config;

import java.util.Map;

/**
 * Configuration for a custom IP info provider
 * @param apiUrl the API URL with %ip% as a placeholder for the IP address
 * @param headers the headers to include in the request
 * @param parseFields the fields to parse from the response, mapping standard field names to JSON keys
 */
public record CustomProviderConfig(String apiUrl, Map<String, String> headers, Map<String, String> parseFields) {
    public CustomProviderConfig(String apiUrl, Map<String, String> headers, Map<String, String> parseFields) {
        this.apiUrl = apiUrl;
        this.headers = Map.copyOf(headers);
        this.parseFields = Map.copyOf(parseFields);
    }
}
