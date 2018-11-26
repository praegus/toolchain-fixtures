package nl.praegus.fitnesse.junit.listeners;
import java.util.*;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ToolchainRunningContext {
//    public static ByteArrayOutputStream baos = new ByteArrayOutputStream();
//    public static PrintStream testoutput = new PrintStream(baos);

    private HashMap<String, String> tests = new HashMap<>();
    private Set<String> finishedTests = new HashSet<>();
    private HashMap<String, String> suites = new HashMap<>();
    private String launchId = "";
    private HashMap<String, String> testStatuses = new HashMap<>();
    private String runningSuite;


    public void addFinishedTest(String testName) {
        finishedTests.add(testName);
    }

    public Set<String> getFinishedTests(String testName) {
        return finishedTests;
    }

    public void addTest(String testName, String id) {
        tests.put(testName, id);
    }

    public String getTestId(String testName) {
        return tests.get(testName);
    }

    public void addSuite(String suiteName, String id) {
        suites.put(suiteName, id);
    }

    public String getSuiteId(String suiteName) {
        return suites.get(suiteName);
    }

    public String getLaunchId() {
        return launchId;
    }

    public void setLaunchId(String launchId) {
        this.launchId = launchId;
    }

    public void addStatus(String testName, String status) {
        testStatuses.put(testName, status);
    }

    public String getStatus(String testName) {
        return testStatuses.get(testName);
    }

    public String getRunningSuite() {
        return runningSuite;
    }

    public void setRunningSuite(String runningSuite) {
        this.runningSuite = runningSuite;
    }

    public List<String> getAllSuiteIds() {
        List<String> result = new ArrayList<>();
        for(String suiteId : suites.values()) {
            result.add(suiteId);
        }
        return result;
    }
    public String getSuiteName(String suiteId) {
        for(Map.Entry<String, String> suite : suites.entrySet()) {
            if (suite.getValue().equals(suiteId)) {
                return suite.getKey();
            }
        }
        return null;
    }
}
