package com.keycloak.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for sending user data to external systems via HTTP API
 */
public class UserSyncService {

    private static final Logger logger = Logger.getLogger(UserSyncService.class);
    
    private final ClientUserSyncConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;

    public UserSyncService(ClientUserSyncConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        
        @SuppressWarnings("deprecation")
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(config.getConnectionTimeout()))
                .setResponseTimeout(Timeout.ofSeconds(config.getReadTimeout()))
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        this.executorService = Executors.newFixedThreadPool(config.getThreadPoolSize());
    }

    /**
     * Sends user data to external API endpoint asynchronously
     */
    public void syncUserData(UserSyncData syncData) {
        if (config.getApiEndpoint() == null || config.getApiEndpoint().trim().isEmpty()) {
            logger.warn("API endpoint not configured, skipping sync");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                sendToExternalApi(syncData);
            } catch (Exception e) {
                logger.errorf(e, "Failed to sync user data for user: %s", syncData.getUserId());
                
                if (config.isRetryEnabled() && config.getMaxRetries() > 0) {
                    retrySync(syncData, config.getMaxRetries());
                }
            }
        }, executorService);
    }

    /**
     * Sends data to external API
     */
    private void sendToExternalApi(UserSyncData syncData) throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(syncData);
        
        HttpPost httpPost = new HttpPost(config.getApiEndpoint());
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");

        if (config.getApiHeaders() != null && !config.getApiHeaders().isEmpty()) {
            config.getApiHeaders().forEach(httpPost::setHeader);
        }

        if (config.getApiToken() != null && !config.getApiToken().trim().isEmpty()) {
            String authHeader = config.getApiAuthType().equals("Bearer") 
                    ? "Bearer " + config.getApiToken()
                    : config.getApiToken();
            httpPost.setHeader("Authorization", authHeader);
        }

        httpPost.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));

        @SuppressWarnings("deprecation")
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try (response) {
            int statusCode = response.getCode();
            
            if (statusCode >= 200 && statusCode < 300) {
                logger.debugf("Successfully synced user data: userId=%s, statusCode=%d", 
                        syncData.getUserId(), statusCode);
            } else {
                throw new Exception(String.format("API returned status code: %d", statusCode));
            }
        }
    }

    /**
     * Retry mechanism to resend data if failed
     */
    private void retrySync(UserSyncData syncData, int remainingRetries) {
        if (remainingRetries <= 0) {
            logger.errorf("Max retries reached for user sync: userId=%s", syncData.getUserId());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(config.getRetryDelay() * 1000L);
                sendToExternalApi(syncData);
                logger.infof("Retry successful for user sync: userId=%s", syncData.getUserId());
            } catch (Exception e) {
                logger.warnf(e, "Retry failed for user sync: userId=%s, remainingRetries=%d", 
                        syncData.getUserId(), remainingRetries - 1);
                retrySync(syncData, remainingRetries - 1);
            }
        }, executorService);
    }

    /**
     * Cleanup resources
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
            if (executorService != null) {
                executorService.shutdown();
            }
        } catch (Exception e) {
            logger.errorf(e, "Error closing UserSyncService resources");
        }
    }
}

