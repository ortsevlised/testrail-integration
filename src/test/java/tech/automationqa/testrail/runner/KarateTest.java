package tech.automationqa.testrail.runner;

import tech.automationqa.testrail.testrail.apiClient.APIClient;
import tech.automationqa.testrail.testrail.services.ReportGenerator;
import tech.automationqa.testrail.testrail.services.TestRailService;
import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tech.automationqa.testrail.testrail.TestrailProperty;

import static tech.automationqa.testrail.testrail.Configuration.getConfigurationBoolean;
import static tech.automationqa.testrail.testrail.Configuration.getConfigurationString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code KarateTest} class is designed to run Karate tests in parallel and manage the results.
 * It includes methods to execute tests, generate reports, and optionally send results to a TestRail instance.
 */
public class KarateTest {

    public static Results results; // Holds the results of the Karate tests after execution.
    private static final Boolean addResults = getConfigurationBoolean(TestrailProperty.ADD_RESULTS).orElse(false); // Configuration flag to determine if test results should be added to TestRail.
    private static final ReportGenerator reportGenerator = new ReportGenerator("target"); // Initializes a new ReportGenerator instance to generate reports.

    /**
     * Executes the Karate tests in parallel based on the specified tags and outputs Cucumber JSON results.
     * It then calls the report generator to create HTML reports from these results and asserts that there are no test failures.
     */
    @Test
    public void testParallel() {
        // Runs Karate tests in parallel and saves the results.
        results = Runner.path("classpath:").tags("@this").outputCucumberJson(true).parallel(4);
        // Generates the HTML report using the report directory from the results.
        reportGenerator.generateReport(results.getReportDir());
        // Asserts that the number of failed tests is zero, throwing an exception with error messages if there are any failures.
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

    /**
     * After all tests have been run, this method is invoked to optionally upload the test results to TestRail.
     * This operation is contingent upon the {@code addResults} configuration.
     */
    @AfterAll
    public static void testAfter() {
        if (addResults) {
            String baseUrl = getConfigurationString(TestrailProperty.BASE_URL).orElseThrow(() -> new IllegalStateException("Base URL for TestRail is not set."));
            APIClient client = new APIClient(baseUrl);
            TestRailService testRailService = new TestRailService(client);
            testRailService.addResultsToTestRail(results);
            testRailService.closeTestPlanIfRequired();
        }
    }
}
