package com.keycloak.sync;

import org.keycloak.events.EventType;

import java.util.*;

/**
 * Configuration for Client User Sync Event Listener
 */
public class ClientUserSyncConfig {

    private String apiEndpoint;
    private String apiToken;
    private String apiAuthType = "Bearer";
    private Map<String, String> apiHeaders;
    private Set<String> clientIds;
    private Set<EventType> eventTypes;
    private List<String> additionalAttributes;
    private int connectionTimeout = 10;
    private int readTimeout = 30;
    private int threadPoolSize = 5;
    private boolean retryEnabled = true;
    private int maxRetries = 3;
    private int retryDelay = 5;

    public ClientUserSyncConfig() {
        this.apiHeaders = new HashMap<>();
        this.clientIds = new HashSet<>();
        this.eventTypes = new HashSet<>();
        this.additionalAttributes = new ArrayList<>();
    }

    public static ClientUserSyncConfig fromMap(Map<String, String> config) {
        ClientUserSyncConfig cfg = new ClientUserSyncConfig();
        
        cfg.setApiEndpoint(config.get("apiEndpoint"));
        cfg.setApiToken(config.get("apiToken"));
        cfg.setApiAuthType(config.getOrDefault("apiAuthType", "Bearer"));
        
        String clientIdsStr = config.get("clientIds");
        if (clientIdsStr != null && !clientIdsStr.trim().isEmpty()) {
            Arrays.stream(clientIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(cfg.getClientIds()::add);
        }
        
        String eventTypesStr = config.get("eventTypes");
        if (eventTypesStr != null && !eventTypesStr.trim().isEmpty()) {
            Arrays.stream(eventTypesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(type -> {
                        try {
                            cfg.getEventTypes().add(EventType.valueOf(type.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                        }
                    });
        }
        
        String attributesStr = config.get("additionalAttributes");
        if (attributesStr != null && !attributesStr.trim().isEmpty()) {
            Arrays.stream(attributesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(cfg.getAdditionalAttributes()::add);
        }
        
        String headersStr = config.get("apiHeaders");
        if (headersStr != null && !headersStr.trim().isEmpty()) {
            Arrays.stream(headersStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(header -> {
                        String[] parts = header.split(":", 2);
                        if (parts.length == 2) {
                            cfg.getApiHeaders().put(parts[0].trim(), parts[1].trim());
                        }
                    });
        }
        
        cfg.setConnectionTimeout(parseInt(config.get("connectionTimeout"), 10));
        cfg.setReadTimeout(parseInt(config.get("readTimeout"), 30));
        cfg.setThreadPoolSize(parseInt(config.get("threadPoolSize"), 5));
        cfg.setMaxRetries(parseInt(config.get("maxRetries"), 3));
        cfg.setRetryDelay(parseInt(config.get("retryDelay"), 5));
        cfg.setRetryEnabled(parseBoolean(config.get("retryEnabled"), true));
        
        return cfg;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getApiAuthType() {
        return apiAuthType;
    }

    public void setApiAuthType(String apiAuthType) {
        this.apiAuthType = apiAuthType;
    }

    public Map<String, String> getApiHeaders() {
        return apiHeaders;
    }

    public void setApiHeaders(Map<String, String> apiHeaders) {
        this.apiHeaders = apiHeaders;
    }

    public Set<String> getClientIds() {
        return clientIds;
    }

    public void setClientIds(Set<String> clientIds) {
        this.clientIds = clientIds;
    }

    public Set<EventType> getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(Set<EventType> eventTypes) {
        this.eventTypes = eventTypes;
    }

    public List<String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(List<String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }
}

