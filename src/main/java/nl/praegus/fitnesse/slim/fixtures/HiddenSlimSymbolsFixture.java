package nl.praegus.fitnesse.slim.fixtures;

import fitnesse.slim.StatementExecutorConsumer;
import fitnesse.slim.StatementExecutorInterface;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;

/**
 * This fixture sets system properties or environment variables to slim symbols, but only does so on the slim executor.
 * Using this fixture allows setting external values to slim symbols without exposing them in the wiki or test report when they are used as input.
 * Do not echo, 'check' or 'show' symbols that start with SECRET_ as this will still expose their values.
 */

public class HiddenSlimSymbolsFixture extends SlimFixture implements StatementExecutorConsumer {
    private StatementExecutorInterface context;

    /**
     * Get the value from a system property and assign it to a slim symbol named $SECRET_[propertyName]
     *
     * @param systemPropertyName The name of the desired system property
     * @return true if a non-empty value was assigned
     */
    public boolean setFromSystemProperty(String systemPropertyName) {
        String value = System.getProperty(systemPropertyName);
        context.assign("SECRET_" + systemPropertyName, value);
        return null != value && !value.isEmpty();
    }

    /**
     * Get the value from an environment variable and assign it to a slim symbol named $SECRET_[variableName]
     *
     * @param variableName The name of the desired environment variable
     * @return true if a non-empty value was assigned
     */
    public boolean setFromEnvironmentVar(String variableName) {
        String value = System.getenv(variableName);
        context.assign("SECRET_" + variableName, value);
        return null != value && !value.isEmpty();
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
