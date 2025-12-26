package com.keycloak.sync;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Set;

/**
 * Event Listener Provider for syncing user data to external systems.
 * Captures authentication events (registration, login) for specific clients
 * and sends data to external API endpoints.
 */
public class ClientUserSyncEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(ClientUserSyncEventListenerProvider.class);
    
    private final KeycloakSession session;
    private final ClientUserSyncConfig config;
    private final UserSyncService syncService;

    public ClientUserSyncEventListenerProvider(KeycloakSession session, ClientUserSyncConfig config) {
        this.session = session;
        this.config = config;
        this.syncService = new UserSyncService(config);
        logger.infof("[ClientUserSync] EventListenerProvider initialized with enableLogging=%s", config.isEnableLogging());
    }

    @Override
    public void onEvent(Event event) {
        try {
            logger.infof("[ClientUserSync] Event received: type=%s, eventId=%s, userId=%s, clientId=%s, realmId=%s, ipAddress=%s, enableLogging=%s",
                    event.getType(), event.getId(), event.getUserId(), event.getClientId(), event.getRealmId(), event.getIpAddress(), config.isEnableLogging());

            if (!shouldProcessEvent(event)) {
                logger.debugf("[ClientUserSync] Event skipped (not in configured event types): type=%s, eventId=%s, enableLogging=%s",
                        event.getType(), event.getId(), config.isEnableLogging());
                return;
            }

            if (config.isEnableLogging() && event.getType() == EventType.LOGIN) {
                logger.infof("[ClientUserSync] Login event triggered: eventId=%s, userId=%s, clientId=%s, ipAddress=%s",
                        event.getId(), event.getUserId(), event.getClientId(), event.getIpAddress());
            }

            if (!shouldProcessClient(event.getClientId())) {
                if (config.isEnableLogging()) {
                    logger.debugf("[ClientUserSync] Event skipped for client: %s (not in configured client list)", event.getClientId());
                }
                return;
            }

            UserSyncData syncData = extractUserData(event);
            
            if (syncData == null) {
                if (config.isEnableLogging()) {
                    logger.warnf("[ClientUserSync] Failed to extract user data from event: %s", event.getId());
                }
                return;
            }

            syncService.syncUserData(syncData);
            
            if (config.isEnableLogging()) {
                logger.infof("[ClientUserSync] User sync event processed: eventId=%s, userId=%s, eventType=%s, clientId=%s",
                        event.getId(), syncData.getUserId(), event.getType(), event.getClientId());
            }

        } catch (Exception e) {
            if (config.isEnableLogging()) {
                logger.errorf(e, "[ClientUserSync] Error processing event: %s", event.getId());
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    }

    /**
     * Checks if event should be processed based on configured event types
     */
    private boolean shouldProcessEvent(Event event) {
        Set<EventType> configuredEventTypes = config.getEventTypes();
        
        if (configuredEventTypes == null || configuredEventTypes.isEmpty()) {
            return event.getType() == EventType.REGISTER || event.getType() == EventType.LOGIN;
        }
        
        return configuredEventTypes.contains(event.getType());
    }

    /**
     * Checks if client ID should be processed based on configuration
     */
    private boolean shouldProcessClient(String clientId) {
        Set<String> configuredClients = config.getClientIds();
        
        if (configuredClients == null || configuredClients.isEmpty()) {
            return true;
        }
        
        return configuredClients.contains(clientId);
    }

    /**
     * Extracts user data from Keycloak event
     */
    private UserSyncData extractUserData(Event event) {
        try {
            RealmModel realm = session.realms().getRealm(event.getRealmId());
            if (realm == null) {
                if (config.isEnableLogging()) {
                    logger.warnf("[ClientUserSync] Realm not found: %s", event.getRealmId());
                }
                return null;
            }

            UserModel user = session.users().getUserById(realm, event.getUserId());
            if (user == null) {
                if (config.isEnableLogging()) {
                    logger.warnf("[ClientUserSync] User not found: %s", event.getUserId());
                }
                return null;
            }

            UserSyncData syncData = new UserSyncData();
            syncData.setEventId(event.getId());
            syncData.setEventType(event.getType().name());
            syncData.setUserId(user.getId());
            syncData.setUsername(user.getUsername());
            syncData.setEmail(user.getEmail());
            syncData.setFirstName(user.getFirstName());
            syncData.setLastName(user.getLastName());
            syncData.setRealmId(event.getRealmId());
            syncData.setRealmName(realm.getName());
            syncData.setClientId(event.getClientId());
            syncData.setIpAddress(event.getIpAddress());
            syncData.setTimestamp(event.getTime());
            syncData.setSessionId(event.getSessionId());

            if (config.getAdditionalAttributes() != null && !config.getAdditionalAttributes().isEmpty()) {
                config.getAdditionalAttributes().forEach(attr -> {
                    String value = user.getFirstAttribute(attr);
                    if (value != null) {
                        syncData.addAttribute(attr, value);
                    }
                });
            }

            return syncData;

        } catch (Exception e) {
            if (config.isEnableLogging()) {
                logger.errorf(e, "[ClientUserSync] Error extracting user data from event: %s", event.getId());
            }
            return null;
        }
    }

    @Override
    public void close() {
        syncService.close();
    }
}

