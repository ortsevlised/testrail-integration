package tech.automationqa.testrail.testrail.services;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The {@code ReportGenerator} class is responsible for generating reports from JSON files
 * that contain test results. It utilizes the Masterthought Cucumber Reporting library to create
 * detailed reports in HTML format.
 */
public class ReportGenerator {
    private final String reportOutputDirectory;

    /**
     * Constructs a {@code ReportGenerator} with the specified report output directory.
     *
     * @param reportOutputDirectory The directory path where the generated reports will be saved.
     */
    public ReportGenerator(String reportOutputDirectory) {
        this.reportOutputDirectory = reportOutputDirectory;

    }

    /**
     * Generates HTML reports from JSON files in the specified Karate output path. The method identifies
     * all JSON files recursively in the given path, processes them, and generates the report in the output
     * directory specified at the construction time.
     *
     * @param karateOutputPath The path to the directory containing the JSON files with test results.
     */
    public void generateReport(String karateOutputPath) {
        List<String> jsonPaths = FileUtils.listFiles(new File(karateOutputPath), new String[]{"json"}, true)
                .stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        net.masterthought.cucumber.Configuration config = new Configuration(new File(reportOutputDirectory), "regression");
        new ReportBuilder(jsonPaths, config).generateReports();
    }
}

