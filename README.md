# Keycloak Client User Sync Listener

A Keycloak Event Listener SPI plugin that captures user authentication events (like registration and login) in real-time for specific clients, extracts user identity data, and automatically sends it to external systems via API endpoints.

## Features

- ✅ **Event-driven**: Captures authentication events in real-time without polling
- ✅ **Client-specific**: Filters events by specific client IDs
- ✅ **Configurable**: Flexible configuration per-realm or globally
- ✅ **Secure**: Follows official Keycloak security lifecycle
- ✅ **Scalable**: Async processing with thread pool
- ✅ **Retry mechanism**: Automatic retry with configurable delay
- ✅ **Extensible**: Supports extraction of custom user attributes

## Data Sent

The plugin sends the following data to external API endpoints:

```json
{
  "eventId": "abc-123-def",
  "eventType": "LOGIN",
  "userId": "user-uuid",
  "username": "john.doe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "realmId": "my-realm",
  "realmName": "My Realm",
  "clientId": "my-client",
  "ipAddress": "192.168.1.1",
  "timestamp": 1234567890,
  "sessionId": "session-uuid",
  "additionalAttributes": {
    "department": "IT",
    "employeeId": "EMP001"
  }
}
```

## Build

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Keycloak 22.0.0 (or compatible version)

### Build JAR

```bash
mvn clean package
```

The JAR file will be available at `target/keycloak-client-user-sync-1.0.0.jar`

## Installation

### 1. Copy JAR to Keycloak

Copy the JAR file to Keycloak providers directory:

```bash
cp target/keycloak-client-user-sync-1.0.0.jar $KEYCLOAK_HOME/providers/
```

### 2. Build Keycloak with Provider

Run Keycloak build command to register the provider:

```bash
cd $KEYCLOAK_HOME
bin/kc.sh build
```

### 3. Restart Keycloak

Restart the Keycloak server:

```bash
bin/kc.sh start
```

## Configuration

### Global Configuration (Optional)

Global configuration can be defined in `conf/keycloak.conf` or via environment variables:

```properties
# Event Listener Configuration
spi-events-listener-client-user-sync-apiEndpoint=https://api.example.com/users/sync
spi-events-listener-client-user-sync-apiToken=your-api-token
spi-events-listener-client-user-sync-clientIds=client1,client2
spi-events-listener-client-user-sync-eventTypes=LOGIN,REGISTER
spi-events-listener-client-user-sync-connectionTimeout=10
spi-events-listener-client-user-sync-readTimeout=30
spi-events-listener-client-user-sync-threadPoolSize=5
spi-events-listener-client-user-sync-retryEnabled=true
spi-events-listener-client-user-sync-maxRetries=3
spi-events-listener-client-user-sync-retryDelay=5
```

### Per-Realm Configuration (Recommended)

1. Login to Keycloak Admin Console
2. Select the realm to configure
3. Navigate to **Realm Settings** → **Attributes**
4. Add the following attributes:

| Attribute Key | Value | Description |
|--------------|-------|-------------|
| `client-user-sync.apiEndpoint` | `https://api.example.com/users/sync` | External API endpoint |
| `client-user-sync.apiToken` | `your-api-token` | API authentication token |
| `client-user-sync.clientIds` | `client1,client2` | Comma-separated list of client IDs (leave empty for all clients) |
| `client-user-sync.eventTypes` | `LOGIN,REGISTER` | Comma-separated event types (default: LOGIN,REGISTER) |
| `client-user-sync.additionalAttributes` | `department,employeeId` | Comma-separated user attributes to extract |
| `client-user-sync.apiHeaders` | `X-Custom-Header:Value1,X-Another:Value2` | Custom HTTP headers (format: Header:Value) |
| `client-user-sync.connectionTimeout` | `10` | Connection timeout in seconds (default: 10) |
| `client-user-sync.readTimeout` | `30` | Read timeout in seconds (default: 30) |
| `client-user-sync.threadPoolSize` | `5` | Thread pool size for async processing (default: 5) |
| `client-user-sync.retryEnabled` | `true` | Enable/disable retry mechanism (default: true) |
| `client-user-sync.maxRetries` | `3` | Maximum retry attempts (default: 3) |
| `client-user-sync.retryDelay` | `5` | Delay between retries in seconds (default: 5) |

### Enable Event Listener

1. In Keycloak Admin Console, select the realm
2. Navigate to **Realm Settings** → **Events**
3. In the **Event Listeners** section, select **client-user-sync**
4. Click **Save**

## Supported Event Types

The plugin can be configured to capture the following event types:

- `LOGIN` - User login
- `REGISTER` - User registration
- `LOGOUT` - User logout
- `UPDATE_PASSWORD` - Password update
- `UPDATE_PROFILE` - Profile update
- All other Keycloak event types

If not configured, defaults are `LOGIN` and `REGISTER`.

## Example External API Endpoint

The external API endpoint must accept POST requests with JSON payload:

```java
@RestController
@RequestMapping("/api/users/sync")
public class UserSyncController {
    
    @PostMapping
    public ResponseEntity<?> syncUser(@RequestBody UserSyncData data) {
        // Process user sync data
        // Save to database, send to another service, etc.
        
        return ResponseEntity.ok().build();
    }
}
```

## Troubleshooting

### Event Listener Not Detected

1. Ensure JAR file exists in `$KEYCLOAK_HOME/providers/`
2. Ensure you ran `bin/kc.sh build`
3. Restart Keycloak
4. Check Keycloak logs for error messages

### Events Not Being Sent

1. Check `apiEndpoint` configuration is correct
2. Check `apiToken` if required
3. Check Keycloak logs for error details:
   ```bash
   tail -f $KEYCLOAK_HOME/data/log/keycloak.log | grep "client-user-sync"
   ```
4. Ensure client ID is in `clientIds` configuration (if configured)

### Performance Issues

1. Adjust `threadPoolSize` based on load
2. Adjust timeout values (`connectionTimeout`, `readTimeout`)
3. Consider disabling retry if not needed (`retryEnabled=false`)

## Security

- **API Token**: Always use HTTPS for API endpoints
- **Sensitive Data**: Consider not sending sensitive data through event listener
- **Network Security**: Ensure Keycloak can access external API endpoint
- **Rate Limiting**: Consider implementing rate limiting at API endpoint

## Development

### Project Structure

```
keycloak-client-user-sync/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/keycloak/sync/
│   │   │       ├── ClientUserSyncEventListenerProvider.java
│   │   │       ├── ClientUserSyncEventListenerProviderFactory.java
│   │   │       ├── ClientUserSyncConfig.java
│   │   │       ├── UserSyncData.java
│   │   │       └── UserSyncService.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── services/
│   │               └── org.keycloak.events.EventListenerProviderFactory
│   └── test/
├── pom.xml
└── README.md
```

### Testing

For local testing, use Keycloak in development mode:

```bash
bin/kc.sh start-dev
```

## License

MIT License

## Contributing

Pull requests and issues are welcome!
