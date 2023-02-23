package nl.praegus.fitnesse.slim.fixtures.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import fitnesse.slim.StatementExecutorConsumer;
import fitnesse.slim.StatementExecutorInterface;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

/**
 * This fixture class is used to get Azure Key Vault secrets and set them to slim symbols using the StatementExecutor interface
 * This ensures that these secrets are not displayed in test results, as long as they are not explicitly echoed or reassigned to
 * a slim symbol wiki-side.
 * Do not echo, 'check' or 'show' symbols that start with SECRET_ as this will still expose their values.
 */
public class KeyVaultSecrets extends SlimFixture implements StatementExecutorConsumer {
    private final SecretClient secretClient;
    private StatementExecutorInterface context;

    /**
     * Instantiate this fixture and create a key vault SecretClient for a named vault.
     * To authenticate, a default Azure credential is used. (see: <a href="https://docs.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable">...</a>)
     * To use Environment Credential, set the following environment variables:
     * AZURE_CLIENT_ID: [CLIENT_ID]
     * AZURE_TENANT_ID: [TENANT_ID]
     * AZURE_CLIENT_SECRET: [CLIENT_SECRET]
     * Access to the vault should be governed using an app registration for this fixture in your Azure AD.
     * Client- and tenant id are on the app registration page. A client secret can be generated there.
     *
     * @param vaultName The name of the key vault as configured in Azure
     */
    public KeyVaultSecrets(String vaultName) {
        String keyVaultUri = "https://" + vaultName + ".vault.azure.net";
        secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    /**
     * Retrieve a secret by its name and set it to slim symbol $SECRET_[secretName]
     *
     * @param secretName The name of the secret to retrieve
     * @return true if secret can be retrieved. Throws a slimFixtureException with the error message otherwise.
     */
    public boolean retrieveSecret(String secretName) {
        try {
            KeyVaultSecret retrievedSecret = secretClient.getSecret(secretName);
            context.assign("SECRET_" + secretName, retrievedSecret.getValue());
            return true;
        } catch (Exception e) {
            throw new SlimFixtureException(false, e.getMessage(), e);
        }
    }


    /**
     * This method is used to provide the statement executor context and should never be called from your test
     *
     * @param statementExecutor --
     */
    @Override
    public void setStatementExecutor(StatementExecutorInterface statementExecutor) {
        context = statementExecutor;
    }
}
