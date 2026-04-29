package com.example;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "classpath:features/job_management.feature",
        glue = "com.example.test.steps",   // ← ton dossier "steps"
        plugin = {"pretty", "html:target/cucumber-reports.html"}
)
public class JobManagementTest {
}
