# TestRail Integration Guide

This document provides instructions on how to set up and use the TestRail integration for automated testing purposes. The integration allows for synchronizing test results with TestRail, creating test runs, closing test plans, and generating feature files based on the test cases defined in TestRail.

## Prerequisites

- An active TestRail account with the necessary permissions to create and manage test cases, runs, and plans.
- TestRail API must be enabled on your TestRail server.
- Your TestRail server should be accessible from the environment where tests are run.

## Configuration

Before running your tests, configure the `testrail-config.yaml` with the appropriate values:

```yaml
Default:
  base.url: <TestRail_Base_URL>
  username: <TestRail_Username>
  password: <TestRail_Password>
  ...
```

Replace `<TestRail_Base_URL>`, `<TestRail_Username>`, and `<TestRail_Password>` with your actual TestRail information. Ensure to keep this information secure.

## TestRail Settings Explained

- `base.url`: The base URL of your TestRail instance.
- `username` and `password`: Your TestRail login credentials (consider using environment variables for security).
- `addResult`: Set to `true` to enable the addition of test results to TestRail.
- `project.id`: The ID of your TestRail project.
- `test.plan.id`: The ID of the TestRail plan to which results will be added.
- `test.suite.id`: The ID of the TestRail suite that contains your test cases.
- `test.plan.close`: Whether to close the test plan after test execution.
- `test.run.create.new`: Whether to create a new test run for each test execution.
- `test.run.create.name`: The name for new test runs created in TestRail.
- `test.run.id`: The ID of an existing TestRail run to which results should be added.
- `create.feature.files`: Whether to generate `.feature` files from TestRail test cases.

## Usage

### Running Tests

Execute your tests as you normally would. If configured to do so, the system will communicate with TestRail to update test runs and plans accordingly.

### Generating Reports

Use the `ReportGenerator` class to generate reports post-test execution:

```java
ReportGenerator reportGenerator = new ReportGenerator("target/report-output-directory");
reportGenerator.generateReport(results.getReportDir());
```

### Adding Test Results to TestRail

If `addResult` is set to `true`, results will be added to TestRail:

```java
if (addResults) {
    TestRailService testRailService = new TestRailService(new APIClient(baseUrl));
    testRailService.addResultsToTestRail(results);
    testRailService.closeTestPlanIfRequired();
}
```

### Creating Feature Files

If `create.feature.files` is set to `true`, the integration will generate feature files based on the test cases fetched from TestRail.

## Troubleshooting

- Ensure that the TestRail API is reachable and that your credentials are correct.
- Verify that the IDs for your project, plan, suite, and run are accurate.
- Check that the TestRail server has the correct permissions set for your user.

## Support

For additional help or to report issues you can reach me at jorge@automationqa.tech