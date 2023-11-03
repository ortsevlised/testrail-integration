package tech.automationqa.testrail.testrail.apiClient;


import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This class provides services to interact with the API for managing test cases, plans, results, and runs.
 */
public class APIService {

    private static final String GET_CASES = "index.php?/api/v2/get_cases/%d&suite_id=%d";
    private static final String ADD_PLAN_ENDPOINT = "index.php?/api/v2/add_plan/%d";
    private static final String CLOSE_PLAN_ENDPOINT = "index.php?/api/v2/close_plan/%d";
    private static final String ADD_RESULTS_FOR_CASES = "index.php?/api/v2/add_results_for_cases/%d";
    private static final String ADD_RUN_ENDPOINT = "index.php?/api/v2/add_run/%d";
    private static final String GET_RUN_ENDPOINT = "index.php?/api/v2/get_run/%d";
    private static final String ADD_ENTRY_PLAN_ENDPOINT = "index.php?/api/v2/add_plan_entry/%d";
    private static final String GET_SECTIONS = "index.php?/api/v2/get_sections/%d&suite_id=%d";

    private final APIClient client; // The API client used to communicate with the API server.

    /**
     * Constructs an API service using the provided APIClient.
     *
     * @param client the APIClient to be used for sending requests to the API.
     */
    public APIService(APIClient client) {
        this.client = client;
    }

    /**
     * Retrieves all test cases for a specified project and test suite.
     *
     * @param projectId the ID of the project.
     * @param suiteId   the ID of the test suite.
     * @return a JSONArray containing the test cases.
     */
    public JSONArray getCases(int projectId, int suiteId) {
        String requestUrl = String.format(GET_CASES, projectId, suiteId);
        return (JSONArray) client.sendGet(requestUrl);
    }

    /**
     * Creates a new test plan within a project.
     *
     * @param projectId the ID of the project.
     * @param data      a JSONObject containing data for the new test plan.
     * @return a JSONObject representing the newly created test plan.
     */
    public JSONObject createPlan(int projectId, JSONObject data) {
        String url = String.format(ADD_PLAN_ENDPOINT, projectId);
        return (JSONObject) client.sendPost(url, data.toString());
    }

    /**
     * Closes an existing test plan.
     *
     * @param planId the ID of the test plan to close.
     */
    public void closePlan(int planId) {
        String url = String.format(CLOSE_PLAN_ENDPOINT, planId);
        client.sendPost(url, null);
    }

    /**
     * Adds results for test cases in a test run.
     *
     * @param runId the ID of the test run.
     * @param data  a JSONObject containing test results to add.
     */
    public void addResultsForCases(int runId, JSONObject data) {
        String endpoint = String.format(ADD_RESULTS_FOR_CASES, runId);
        client.sendPost(endpoint, data.toString());
    }

    /**
     * Adds a new test run to a test plan.
     *
     * @param planId the ID of the test plan to add the run to.
     * @param data   a JSONObject containing data for the new test run.
     * @return a JSONObject representing the newly added test run.
     */
    public JSONObject addRunToTestPlan(int planId, JSONObject data) {
        String endpoint = String.format(ADD_ENTRY_PLAN_ENDPOINT, planId);
        return (JSONObject) client.sendPost(endpoint, data.toString());
    }

    /**
     * Adds a new test run to a project.
     *
     * @param projectId the ID of the project to add the run to.
     * @param data      a JSONObject containing data for the new test run.
     * @return a JSONObject representing the newly added test run.
     */
    public JSONObject addRunToProject(int projectId, JSONObject data) {
        String endpoint = String.format(ADD_RUN_ENDPOINT, projectId);
        return (JSONObject) client.sendPost(endpoint, data.toString());
    }

    /**
     * Retrieves a specific test run by its ID.
     *
     * @param runId the ID of the test run to retrieve.
     * @return a JSONObject representing the test run.
     */
    public JSONObject getTestRun(int runId) {
        String endpoint = String.format(GET_RUN_ENDPOINT, runId);
        return (JSONObject) client.sendGet(endpoint);
    }

    /**
     * Retrieves all sections for a specified project and test suite.
     *
     * @param projectId the ID of the project.
     * @param suiteId   the ID of the test suite.
     * @return a JSONArray containing all the sections.
     */
    public JSONArray getAllSections(int projectId, int suiteId) {
        String requestUrl = String.format(GET_SECTIONS, projectId, suiteId);
        return (JSONArray) client.sendGet(requestUrl);
    }
}
