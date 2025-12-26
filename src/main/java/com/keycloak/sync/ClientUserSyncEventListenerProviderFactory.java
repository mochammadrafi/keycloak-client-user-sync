package com.keycloak.sync;

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating ClientUserSyncEventListenerProvider instances
 */
public class ClientUserSyncEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final String PROVIDER_ID = "client-user-sync";
    
    private Map<String, String> globalConfig = new HashMap<>();

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        Map<String, String> configMap = new HashMap<>(globalConfig);
        
        RealmModel realm = session.getContext().getRealm();
        if (realm != null) {
            String apiEndpoint = realm.getAttribute("client-user-sync.apiEndpoint");
            if (apiEndpoint != null) configMap.put("apiEndpoint", apiEndpoint);
            
            String apiToken = realm.getAttribute("client-user-sync.apiToken");
            if (apiToken != null) configMap.put("apiToken", apiToken);
            
            String clientIds = realm.getAttribute("client-user-sync.clientIds");
            if (clientIds != null) configMap.put("clientIds", clientIds);
            
            String eventTypes = realm.getAttribute("client-user-sync.eventTypes");
            if (eventTypes != null) configMap.put("eventTypes", eventTypes);
            
            String additionalAttrs = realm.getAttribute("client-user-sync.additionalAttributes");
            if (additionalAttrs != null) configMap.put("additionalAttributes", additionalAttrs);
            
            String apiHeaders = realm.getAttribute("client-user-sync.apiHeaders");
            if (apiHeaders != null) configMap.put("apiHeaders", apiHeaders);
            
            String connectionTimeout = realm.getAttribute("client-user-sync.connectionTimeout");
            if (connectionTimeout != null) configMap.put("connectionTimeout", connectionTimeout);
            
            String readTimeout = realm.getAttribute("client-user-sync.readTimeout");
            if (readTimeout != null) configMap.put("readTimeout", readTimeout);
            
            String threadPoolSize = realm.getAttribute("client-user-sync.threadPoolSize");
            if (threadPoolSize != null) configMap.put("threadPoolSize", threadPoolSize);
            
            String retryEnabled = realm.getAttribute("client-user-sync.retryEnabled");
            if (retryEnabled != null) configMap.put("retryEnabled", retryEnabled);
            
            String maxRetries = realm.getAttribute("client-user-sync.maxRetries");
            if (maxRetries != null) configMap.put("maxRetries", maxRetries);
            
            String retryDelay = realm.getAttribute("client-user-sync.retryDelay");
            if (retryDelay != null) configMap.put("retryDelay", retryDelay);
        }
        
        ClientUserSyncConfig config = ClientUserSyncConfig.fromMap(configMap);
        return new ClientUserSyncEventListenerProvider(session, config);
    }

    @Override
    public void init(Config.Scope config) {
        if (config != null) {
            globalConfig.put("apiEndpoint", config.get("apiEndpoint"));
            globalConfig.put("apiToken", config.get("apiToken"));
            globalConfig.put("clientIds", config.get("clientIds"));
            globalConfig.put("eventTypes", config.get("eventTypes"));
            globalConfig.put("additionalAttributes", config.get("additionalAttributes"));
            globalConfig.put("apiHeaders", config.get("apiHeaders"));
            globalConfig.put("connectionTimeout", config.get("connectionTimeout", "10"));
            globalConfig.put("readTimeout", config.get("readTimeout", "30"));
            globalConfig.put("threadPoolSize", config.get("threadPoolSize", "5"));
            globalConfig.put("retryEnabled", config.get("retryEnabled", "true"));
            globalConfig.put("maxRetries", config.get("maxRetries", "3"));
            globalConfig.put("retryDelay", config.get("retryDelay", "5"));
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}

