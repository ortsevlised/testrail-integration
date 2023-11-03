package tech.automationqa.testrail.testrail;

import com.intuit.karate.core.StepResult;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class AnalyseResults {
    private static final String CASE_ID = "case_id";
    private static final String COMMENT = "comment";
    private static final String ELAPSED = "elapsed";
    private static final String STATUS_ID = "status_id";
    private static final int STATUS_PASSED = 1;
    private static final int STATUS_SKIPPED = 2;
    private static final int STATUS_FAILED = 5;
    private final static String NEWLINE = System.lineSeparator();

    /**
     * Uses the list of stepResults to build a message and result with the format test rail expects.
     *
     * @param stepResults the stepResults it gets from the test execution.
     * @return a map with the resultID and the message for each test
     */
    public static HashMap<String, Object> getStepsDetails(List<StepResult> stepResults) {
        StringBuilder message = new StringBuilder();
        int statusId = STATUS_FAILED;
        for (StepResult testStep : stepResults) {
            String result = testStep.getResult().toString();
            if ("passed".equals(result)) {
                statusId = STATUS_PASSED;
                message.append(testStep.getStep().toString()).append(NEWLINE);
            } else if ("skip".equals(result)) {
                statusId = STATUS_SKIPPED;
                new StringBuilder("Skipped test at").append(NEWLINE).append(testStep.getStep().toString()).append(NEWLINE);
            } else if ("failed".equals(result)) {
                statusId = STATUS_FAILED;
                new StringBuilder("Failed test at").append(NEWLINE).append(testStep.getStep().toString()).append(NEWLINE).append(testStep.getErrorMessage()).append(NEWLINE);
            }
        }

        HashMap<String, Object> stepDetails = new HashMap<>();
        stepDetails.put(COMMENT, message.toString());
        stepDetails.put(STATUS_ID, statusId);
        return stepDetails;
    }

    /**
     * @param caseId      Test Case ID of the test in test rail
     * @param statusId    The ID indicating the status of the test.
     * @param comment     Comments about the test result.
     * @param elapsedTime The time it took to run.
     * @return a JSON object with the result details.
     */
    public static JSONObject buildTestCaseResultJson(int caseId, int statusId, String comment, String elapsedTime) {
        JSONObject json = new JSONObject();
        json.put(CASE_ID, caseId);
        json.put(STATUS_ID, statusId);
        json.put(COMMENT, comment);
        json.put(ELAPSED, elapsedTime);
        return json;
    }
}