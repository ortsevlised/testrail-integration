package tech.automationqa.testrail.testrail.services;

import tech.automationqa.testrail.testrail.apiClient.APIClient;

import tech.automationqa.testrail.testrail.apiClient.APIService;
import com.intuit.karate.Logger;
import com.intuit.karate.Results;
import com.intuit.karate.core.Scenario;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tech.automationqa.testrail.testrail.TestrailProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.automationqa.testrail.testrail.AnalyseResults.*;
import static tech.automationqa.testrail.testrail.Configuration.*;
import static tech.automationqa.testrail.testrail.Configuration.getConfigurationInteger;

/**
 * The {@code TestRailService} class is responsible for interfacing with TestRail's API client.
 * It provides functionality to initialize the API client with credentials, add test results to TestRail,
 * generate feature files based on the test cases, and close the test plan if required.
 */
public class TestRailService {
    // Logger for the service
    public static final Logger LOGGER = new Logger();

    // Configuration fields
    private static final Boolean isNewRun = getConfigurationBoolean(TestrailProperty.RUN_NEW).orElse(false);
    private static final String TEST_SUITE_ID_NOT_CONFIGURED = "Test Suite ID not configured";
    private static final String TEST_RUN_ID_NOT_CONFIGURED = "Test Run ID not configured";
    private static final String TEST_SUITE_ID_KEY = "suite_id";
    private static final String TEST_RUN_ID_KEY = "run_id";
    private static final int testPlanId = getConfigurationInteger(TestrailProperty.TEST_PLAN_ID).orElse(0);
    private static final int projectID = getConfigurationInteger(TestrailProperty.PROJECT_ID).orElseThrow(() -> new RuntimeException("Project ID not configured"));
    private static final String runName = getConfigurationString(TestrailProperty.RUN_NAME).orElse("Automated test run");
    private static final Boolean closeTestPlan = getConfigurationBoolean(TestrailProperty.CLOSE_TEST_PLAN).orElse(false);
    private static final int testSuiteId = getConfigurationInteger(TestrailProperty.TEST_SUITE_ID).orElse(0);
    private static final String featuresPath = "src/test/java/com/elavon/domainservices/eu/customerdomainservice/feature/";
    private final APIClient client;
    private final APIService api;

    /**
     * Constructs a new {@code TestRailService} and initializes the client with the necessary credentials.
     *
     * @param client The API client to be used for interacting with TestRail
     */
    public TestRailService(APIClient client) {
        this.client = client;
        this.api = new APIService(client);
        initializeClient();
    }

    /**
     * Initializes the API client with the username and password from configuration.
     */
    private void initializeClient() {
        String username = getConfigurationString(TestrailProperty.USERNAME).orElseThrow(() -> new IllegalStateException("Username not configured"));
        String password = getConfigurationString(TestrailProperty.PASSWORD).orElseThrow(() -> new IllegalStateException("Password not configured"));
        client.setUser(username).setPassword(password);
    }

    /**
     * Adds test results to TestRail by creating a new test run or using an existing one, then
     * uploading the results for each test case.
     *
     * @param results The results of test execution to be added to TestRail
     */
    public void addResultsToTestRail(Results results) {
        try {
            Map<String, Integer> runAndSuiteIds = determineRunId();
            int runId = runAndSuiteIds.get(TEST_RUN_ID_KEY);
            int suiteId = runAndSuiteIds.get(TEST_SUITE_ID_KEY);
            List<JSONObject> testCasesList = fetchTestCases(suiteId);

            if (getConfigurationBoolean(TestrailProperty.CREATE_FEATURE_FILES).orElse(false)) {
                generateFeatureFilesForSuite(suiteId, testCasesList);
                return;
            }

            JSONArray resultArray = updateTestCasesWithExecutionResults(testCasesList, results);

            LOGGER.info("Adding results to Test Run: {}", runId);
            api.addResultsForCases(runId, new JSONObject().put("results", resultArray));
        } catch (Exception e) {
            LOGGER.error("Failed to add results to TestRail: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the test plan in TestRail if the configuration specifies to do so.
     */
    public void closeTestPlanIfRequired() {
        if (closeTestPlan) {
            LOGGER.info("Closing Test Plan: {}", testPlanId);
            try {
                api.closePlan(testPlanId);
            } catch (Exception e) {
                LOGGER.error("Failed to close test plan: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Updates the JSON array of test cases with the execution results from the test suite.
     *
     * @param testCasesList A list of JSONObjects representing the test cases
     * @param results       The results of the test execution
     * @return JSONArray with the results of the test cases updated
     */
    private static JSONArray updateTestCasesWithExecutionResults(List<JSONObject> testCasesList, Results results) {
        JSONArray resultList = new JSONArray();

        results.getScenarioResults().forEach(result -> {
            Scenario scenario = result.getScenario();
            String scenarioName = scenario.getName();

            // Find the matching test case based on the scenario name
            JSONObject matchingTestCase = testCasesList.stream().filter(tc -> tc.getString("title").equals(scenarioName)).findFirst().orElseThrow(() -> new RuntimeException("The scenario " + scenarioName + " is not part of the test suite " + testSuiteId + ". Please check your configuration."));

            HashMap<String, Object> stepsDetails = getStepsDetails(result.getStepResults());

            // There's a bug in the testrail api that doesn't allow an elapsed time of 0, it's fixed in recent versions but we are using an old version.
            String elapsedTime = "1s";
            if (result.getDurationMillis() != 0) {
                elapsedTime = result.getDurationMillis() + "s";
            }

            try {
                resultList.put(buildTestCaseResultJson(matchingTestCase.getInt("id"), (Integer) stepsDetails.get("statusId"), stepsDetails.get("message").toString(), elapsedTime));
            } catch (JSONException e) {
                LOGGER.error("Failed to create JSON result for test case: " + scenarioName, e);
            }
        });
        return resultList;
    }

    /**
     * Determines the TestRail run ID, either by creating a new run or using an existing one.
     *
     * @return A map containing the run ID and suite ID to be used for adding test results
     */
    private Map<String, Integer> determineRunId() {
        Map<String, Integer> result = new HashMap<>();
        if (isNewRun) {
            int suiteId = getConfigurationInteger(TestrailProperty.TEST_SUITE_ID).orElseThrow(() -> new RuntimeException(TEST_SUITE_ID_NOT_CONFIGURED));
            JSONObject run = createNewTestRun(suiteId);
            int runId = extractRunId(run);
            result.put(TEST_RUN_ID_KEY, runId);
        } else {
            int runId = getConfigurationInteger(TestrailProperty.TEST_RUN_ID).orElseThrow(() -> new RuntimeException(TEST_RUN_ID_NOT_CONFIGURED));
            int suiteId = api.getTestRun(runId).getInt(TEST_SUITE_ID_KEY);
            result.put(TEST_RUN_ID_KEY, runId);
            result.put(TEST_SUITE_ID_KEY, suiteId);
        }
        LOGGER.info("Test Run ID: {}", result.get(TEST_RUN_ID_KEY));
        LOGGER.info("Test Suite ID: {}", result.get(TEST_SUITE_ID_KEY));
        return result;
    }

    /**
     * Extracts the run ID from a given JSON object containing TestRail run information.
     *
     * @param run The JSON object containing TestRail run information
     * @return The extracted run ID
     */
    private int extractRunId(JSONObject run) {
        if (testPlanId != 0) {
            return run.getJSONArray("runs").getJSONObject(0).getInt("id");
        } else {
            return run.getInt("id");
        }
    }

    /**
     * Generates feature files for a given test suite using the provided test cases.
     *
     * @param suiteId    The ID of the test suite for which to generate feature files
     * @param testCases  A list of JSONObjects representing the test cases
     * @throws IOException If there is an error writing the feature files
     */
    public void generateFeatureFilesForSuite(int suiteId, List<JSONObject> testCases) throws IOException {
        Map<String, List<String>> sectionScenarios = organizeScenariosBySections(testCases, getTestSuiteSections(suiteId));
        writeFeatureFiles(sectionScenarios);
    }

    /**
     * Fetches test cases from TestRail for a given test suite.
     *
     * @param suiteId The ID of the test suite for which to fetch test cases
     * @return A list of JSONObjects representing the test cases
     */
    private List<JSONObject> fetchTestCases(int suiteId) {
        JSONArray testCasesJsonArray = api.getCases(projectID, suiteId);
        return IntStream.range(0, testCasesJsonArray.length()).mapToObj(testCasesJsonArray::getJSONObject).collect(Collectors.toList());
    }


    /**
     * Retrieves the sections for a given test suite from TestRail.
     *
     * @param testSuiteId The ID of the test suite for which to retrieve sections
     * @return A list of JSONObjects representing the sections
     */
    private Map<String, Integer> getTestSuiteSections(int testSuiteId) {
        JSONArray sectionsArray = api.getAllSections(projectID, testSuiteId);

        LOGGER.info("Retrieving current suite sections");
        return IntStream.range(0, sectionsArray.length()).mapToObj(sectionsArray::getJSONObject).collect(Collectors.toMap(section -> section.getString("name"), section -> section.getInt("id")));
    }


    /**
     * Organizes scenarios by their respective sections. This method maps each test case to the section
     * it belongs to based on the section IDs provided in the test cases and the section names found in the
     * sections map.
     *
     * @param testCases   The list of test case JSONObjects to organize.
     * @param sectionsMap A map of section names to their corresponding IDs.
     * @return A map where each section name is associated with a list of scenarios that belong to it.
     */
    private Map<String, List<String>> organizeScenariosBySections(List<JSONObject> testCases, Map<String, Integer> sectionsMap) {
        // Flip the sectionsMap to map IDs back to names for easier lookup
        Map<Integer, String> idToSectionNameMap = sectionsMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        Map<String, List<String>> sectionScenarios = new HashMap<>();
        for (JSONObject testCase : testCases) {
            int sectionId = testCase.getInt("section_id");
            String sectionName = idToSectionNameMap.get(sectionId);
            if (sectionName == null) {
                LOGGER.warn("Section ID {} not found in sections map.", sectionId);
                continue; // Skip this test case if the section ID is not found
            }
            String scenario = formatScenario(testCase);
            sectionScenarios.computeIfAbsent(sectionName, k -> new ArrayList<>()).add(scenario);
        }
        return sectionScenarios;
    }


    /**
     * Formats a single scenario from the given test case JSON object. It constructs a scenario string with
     * given, when, and then steps.
     *
     * @param testCase The JSON object representing a single test case.
     * @return A formatted scenario as a String.
     */
    private String formatScenario(JSONObject testCase) {
        String title = testCase.optString("title", "Untitled");
        JSONArray stepsSeparated = testCase.optJSONArray("custom_steps_separated");
        StringBuilder scenarioBuilder = new StringBuilder();

        // Start the scenario with the title
        scenarioBuilder.append("Scenario: ").append(title).append("\n");

        if (stepsSeparated != null) {
            for (int i = 0; i < stepsSeparated.length(); i++) {
                JSONObject step = stepsSeparated.getJSONObject(i);
                String content = step.optString("content", "No action provided.").replaceAll("\\n", " ").trim();
                String expected = step.optString("expected", "").replaceAll("\\n", " ").trim();

                // Append Given/When/Then for each step
                if (i == 0) {
                    scenarioBuilder.append("Given ").append(content).append("\n");
                } else {
                    scenarioBuilder.append("And ").append(content).append("\n");
                }

                if (!expected.isEmpty()) {
                    scenarioBuilder.append("Then ").append(expected).append("\n");
                }
            }
        } else {
            scenarioBuilder.append("Given No steps defined.\n");
        }

        return scenarioBuilder.toString().trim(); // Trim to remove any trailing newlines at the end
    }

    /**
     * Writes feature files for each section with its associated scenarios to the specified features path.
     * It names the feature files based on the section names, sanitizing the file names to be filesystem-friendly.
     *
     * @param sectionScenarios A map with section names as keys and associated scenarios as values.
     * @throws IOException If an I/O error occurs writing to or creating the feature files.
     */
    private void writeFeatureFiles(Map<String, List<String>> sectionScenarios) throws IOException {
        for (Map.Entry<String, List<String>> entry : sectionScenarios.entrySet()) {
            String featureFileName = entry.getKey().trim() // Trim the text
                    .replaceAll("\\s+", "_") // Replace one or more whitespace characters with an underscore
                    .replaceAll("[^a-zA-Z0-9_]", "") // Remove any character that is not alphanumeric or an underscore
                    .toLowerCase() // Convert to lower case
                    .replaceAll("_+", "_") // Replace multiple underscores with a single underscore
                    + ".feature";
            Path featureFilePath = Paths.get(featuresPath + featureFileName);

            List<String> featureContent = new ArrayList<>();
            featureContent.add("Feature: " + entry.getKey());
            entry.getValue().forEach(scenario -> featureContent.add(scenario + "\n"));

            Files.write(featureFilePath, featureContent);
            System.out.println("Feature file created: " + featureFilePath);
        }
    }

    /**
     * Creates a new test run within TestRail. If a test plan ID is configured, the new test run is added to
     * the test plan. Otherwise, it is added to the project.
     *
     * @param suiteId The ID of the test suite for which to create a new test run.
     * @return A JSONObject containing details of the newly created test run.
     */
    private JSONObject createNewTestRun(int suiteId) {
        LOGGER.info("Creating new Test Run");
        JSONObject runBody = new JSONObject().put("name", runName + " " + Timestamp.from(Instant.now())).put(TEST_SUITE_ID_KEY, suiteId);

        if (testPlanId != 0) {
            return api.addRunToTestPlan(testPlanId, runBody);
        } else {
            return api.addRunToProject(projectID, runBody);
        }
    }
}
