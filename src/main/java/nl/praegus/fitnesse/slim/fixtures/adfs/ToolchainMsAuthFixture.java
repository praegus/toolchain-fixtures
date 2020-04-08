package nl.praegus.fitnesse.slim.fixtures.adfs;

import com.microsoft.aad.msal4j.*;
import nl.hsac.fitnesse.fixture.slim.StopTestException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

public class ToolchainMsAuthFixture {
    private String CLIENT_ID;
    private final String AUTHORITY = "https://login.microsoftonline.com/organizations/";
    private final Set<String> SCOPE = Collections.singleton("");
    private String USER_NAME;
    private String USER_PASSWORD;

    private String accessToken;
    private String idToken;
    private Date tokenExpiry;

    /**
     * Instantiate using your Client_id, username and password
     * Usage: | toolchain ms auth fixture | _client_id_ | _username_ | _password_ |
     * @param client_id The Client-id of the app to login to
     * @param username The username (email address) to send
     * @param password The password to send
     */
    public ToolchainMsAuthFixture(String client_id, String username, String password) {
        CLIENT_ID = client_id;
        USER_NAME = username;
        USER_PASSWORD = password;
        authenticate();
    }

    public String accessToken() {
        return accessToken;
    }

    public String idToken() {
        return idToken;
    }

    public String tokenExpireDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(tokenExpiry);
    }


    private void authenticate() {
        try {
            IAuthenticationResult result = acquireTokenUsernamePassword();
            accessToken = result.accessToken();
            idToken = result.idToken();
            tokenExpiry = result.expiresOnDate();
        } catch (Exception e) {
            throw new StopTestException(false, "Unable to Authenticate: " + e.getMessage());
        }
    }

    private IAuthenticationResult acquireTokenUsernamePassword() throws Exception {

       PublicClientApplication app = PublicClientApplication.builder(CLIENT_ID)
                .authority(AUTHORITY)
                .build();

        UserNamePasswordParameters parameters =
                UserNamePasswordParameters
                        .builder(SCOPE, USER_NAME, USER_PASSWORD.toCharArray())
                        .build();

        return app.acquireToken(parameters).join();
    }
}
