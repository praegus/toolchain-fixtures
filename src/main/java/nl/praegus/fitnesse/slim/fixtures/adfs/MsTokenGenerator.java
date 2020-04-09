package nl.praegus.fitnesse.slim.fixtures.adfs;


import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import javax.naming.ServiceUnavailableException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MsTokenGenerator {

    private String CLIENT_ID = "607f22ad-6a7c-4bf9-b7c4-31cb38cc5dd8";
    private String RESOURCE_URI = "api://e484142b-94f5-411e-8759-d7f1d98f288d";
    private String AUTHORITY = "https://login.microsoftonline.com/e22802fd-166f-46cb-89db-dc0e37b86361";
    private AuthenticationResult lastResult;

    /**
     * Generate access & refresh tokens using microsoft login.
     * To use this fixture, the allowPublicClient key in your application's manifest must be set to true.
     * Set clientId, resourceUri and authority befaore authenticating.
     */
    public MsTokenGenerator() {}

    /**
     * Generate access & refresh tokens using microsoft login.
     * To use this fixture, the allowPublicClient key in your application's manifest must be set to true.
     * @param clientId The clientID of the app to authenticate against
     * @param resourceUri The resourceURI of the api you want a token for
     * @param authority The authority url (usually https://login.microsoftonline.com/[tenantId] or a default authority
     *                  such as https://login.microsoftonline.com/organizations
     */
    public MsTokenGenerator(String clientId, String resourceUri, String authority) {
        CLIENT_ID = clientId;
        RESOURCE_URI = resourceUri;
        AUTHORITY = authority;
    }

    public void authority(String authority) {
        AUTHORITY = authority;
    }

    public void clientId(String clientId) {
        CLIENT_ID = clientId;
    }

    public void resourceUri(String resourceUri) {
        RESOURCE_URI = resourceUri;
    }

    public String clientId() {
        return CLIENT_ID;
    }

    public String resourceUri() {
        return RESOURCE_URI;
    }

    public String authority() {
        return AUTHORITY;
    }

    public String accessToken() {
        if(lastResult == null) {
            throw new SlimFixtureException(false, "Pleas call authenticate with user <username> password <password> before requesting tokens.");
        }
        return lastResult.getAccessToken();
    }

    public String refreshToken() {
        if(lastResult == null) {
            throw new SlimFixtureException(false, "Pleas call authenticate with user <username> password <password> before requesting tokens.");
        }
        return lastResult.getRefreshToken();
    }

    public boolean authenticateWithUsernamePassword(String username, String password) {
       try {
           getAccessTokenFromUserCredentials(username, password);
           return true;
       } catch (Exception e) {
           return false;
       }

    }

    private void getAccessTokenFromUserCredentials(String username, String password) throws Exception {
        AuthenticationContext context;
        AuthenticationResult result;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    RESOURCE_URI, CLIENT_ID, username, password,
                    null);
            result = future.get();
        } finally {
            assert service != null;
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException(
                    "authentication result was null");
        }

        lastResult = result;
    }
}
