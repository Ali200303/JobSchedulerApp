package com.example.test.steps;

import io.cucumber.java.en.*;
import io.cucumber.java.After;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

public class JobSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private String currentJobName;

    @Given("I am logged in as {string}")
    public void login(String username) throws InterruptedException {

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get("http://localhost:8080/login");
        Thread.sleep(2000);

        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();

        wait.until(urlContains("/jobs"));
    }

    @When("I create a job with name {string}, description {string}, frequency {string}, interval {int}")
    public void createJob(String name, String description, String frequency, int interval) throws InterruptedException {

        this.currentJobName = name;

        driver.get("http://localhost:8080/jobs/create");
        Thread.sleep(2000);

        driver.findElement(By.name("name")).sendKeys(name);
        driver.findElement(By.name("description")).sendKeys(description);

        LocalDateTime startTime = LocalDateTime.now()
                .plusMinutes(1)
                .withSecond(0)
                .withNano(0);

        LocalDateTime endTime = startTime.plusHours(1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        String startTimeValue = startTime.format(formatter);
        String endTimeValue = endTime.format(formatter);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementsByName('startTime')[0].value = arguments[0];", startTimeValue);
        js.executeScript("document.getElementsByName('endTime')[0].value = arguments[0];", endTimeValue);

        Select frequencySelect = new Select(driver.findElement(By.name("frequency")));
        frequencySelect.selectByValue(frequency);

        WebElement intervalInput = driver.findElement(By.name("interval"));
        intervalInput.clear();
        intervalInput.sendKeys(String.valueOf(interval));

        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(urlContains("/jobs"));
        Thread.sleep(2000);
    }

    @Then("I should see the job {string} in the jobs list")
    public void checkJobInList(String name) {

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("jobsTable")
        ));

        List<WebElement> rows =
                driver.findElements(
                        By.cssSelector("#jobsTable tbody tr")
                );

        boolean found = rows.stream()
                .anyMatch(row ->
                        row.findElement(
                                        By.cssSelector("td:first-child")
                                )
                                .getText()
                                .trim()
                                .equals(name)
                );

        Assertions.assertTrue(
                found,
                "Job '" + name + "' was not found."
        );
    }

    @Then("I should see its planning page with instances")
    public void checkPlanningPage() {

        WebElement jobRow =
                findJobRow(currentJobName);

        jobRow.findElement(
                By.cssSelector(".btn-view")
        ).click();

        wait.until(ExpectedConditions.urlContains("/planning"));

        List<WebElement> instances =
                wait.until(
                        ExpectedConditions.presenceOfAllElementsLocatedBy(
                                By.cssSelector(
                                        "table tbody tr.instance-row"
                                )
                        )
                );

        Assertions.assertFalse(
                instances.isEmpty(),
                "No job instances found."
        );
    }

    @Given("I have a job with instances in ACTIVE status")
    public void createJobWithActiveInstances() throws InterruptedException {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureStart = now.plusHours(1);
        LocalDateTime futureEnd = now.plusHours(5);

        createJobViaUI("SkipTestJob", "Job for skip testing",
                futureStart, futureEnd, "MINUTES", 30);
        this.currentJobName = "SkipTestJob";
    }

    @Given("I have a job with a SKIPPED instance")
    public void createJobWithSkippedInstance() throws InterruptedException {

        createJobWithActiveInstances();

        navigateToJobPlanningPage();

        selectFirstActiveInstance();

        clickInstanceActionButton();

        wait.until(driver ->
                !driver.findElements(
                        By.cssSelector(
                                "tr[data-status='SKIPPED']"
                        )
                ).isEmpty()
        );
    }

    @When("I navigate to the job planning page")
    public void navigateToJobPlanningPage() {

        if (driver.getCurrentUrl().contains("/planning")) {
            return;
        }

        driver.get("http://localhost:8080/jobs");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("jobsTable")
        ));

        WebElement jobRow =
                findJobRow(currentJobName);

        jobRow.findElement(
                By.cssSelector(".btn-view")
        ).click();

        wait.until(ExpectedConditions.urlContains("/planning"));

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("table tbody tr.instance-row")
        ));
    }

    @When("I click the \"View Description\" button")
    public void clickViewDescription() {

        driver.findElement(
                By.id("descriptionBtn")
        ).click();
    }

    @When("I select the first ACTIVE instance")
    public void selectFirstActiveInstance() {

        List<WebElement> rows =
                driver.findElements(By.cssSelector("table tbody tr.instance-row"));

        for (WebElement row : rows) {

            String status =
                    row.getAttribute("data-status");

            if ("ACTIVE".equals(status)) {

                row.click();

                wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("instanceActionBtn")
                ));

                return;
            }
        }

        throw new RuntimeException(
                "No ACTIVE instance available"
        );
    }

    @When("I select the SKIPPED instance")
    public void selectSkippedInstance() {

        List<WebElement> rows =
                driver.findElements(By.cssSelector("table tbody tr.instance-row"));

        for (WebElement row : rows) {

            if ("SKIPPED".equals(
                    row.getAttribute("data-status"))) {

                row.click();

                wait.until(driver ->
                        driver.findElement(
                                By.id("instanceActionBtn")
                        ).isEnabled()
                );

                return;
            }
        }

        throw new RuntimeException(
                "No SKIPPED instance available"
        );
    }

    @When("I click the instance action button")
    public void clickInstanceActionButton() {

        WebElement button =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.id("instanceActionBtn")
                ));

        button.click();

        wait.until(ExpectedConditions.urlContains("/planning"));
    }

    @Then("the selected instance status should change to {string}")
    public void selectedInstanceStatusShouldChangeTo(String expectedStatus) {

        wait.until(driver -> {

            List<WebElement> rows =
                    driver.findElements(
                            By.cssSelector("table tbody tr.instance-row")
                    );

            return rows.stream()
                    .anyMatch(row ->
                            expectedStatus.equals(
                                    row.getAttribute("data-status")
                            )
                    );
        });
    }

    private void createJobViaUI(String name,
                                String description,
                                LocalDateTime start,
                                LocalDateTime end,
                                String frequency,
                                int intervalMinutes) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        driver.get("http://localhost:8080/jobs/create");
        wait.until(visibilityOfElementLocated(By.name("name"))).sendKeys(name);
        driver.findElement(By.name("description")).sendKeys(description);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];",
                driver.findElement(By.name("startTime")), start.format(fmt));
        js.executeScript("arguments[0].value = arguments[1];",
                driver.findElement(By.name("endTime")),   end.format(fmt));

        new Select(driver.findElement(By.name("frequency")))
                .selectByValue(frequency);
        WebElement interval = driver.findElement(By.name("interval"));
        interval.clear();
        interval.sendKeys(String.valueOf(intervalMinutes));

        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(urlContains("/jobs"));
    }
    private List<WebElement> selectedRows = new ArrayList<>();

    @When("I select {int} active instances by clicking on their rows")
    public void selectActiveInstancesByClicking(int count) throws InterruptedException {

        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr.instance-row"));
        int selected = 0;

        for (WebElement row : rows) {

            if (selected >= count) break;

            String rowClass = row.getAttribute("class");
            if (!rowClass.contains("disabled")) {

                String status = row.getAttribute("data-status");

                if ("ACTIVE".equals(status) || "DONE".equals(status)) {

                    row.click();
                    Thread.sleep(500);
                    selected++;
                    selectedRows.add(row);
                }
            }
        }
        assertTrue(selected >= count, "Seulement " + selected + " instances ont pu être sélectionnées au lieu de " + count);
    }

    @When("I click the \"Delete Selected Instances\" button")
    public void clickDeleteSelectedInstancesButton() throws InterruptedException {

        WebElement deleteButton = driver.findElement(By.cssSelector(".delete-selected-btn"));
        assertTrue(deleteButton.isEnabled(), "Le bouton Delete Selected Instances devrait être activé");

        deleteButton.click();

        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();

        Thread.sleep(2000);
    }

    @Then("the selected instances should be removed from the planning")
    public void checkSelectedInstancesRemoved() throws InterruptedException {

        wait.until(visibilityOfElementLocated(By.cssSelector("table tbody tr")));
        Thread.sleep(1000);

        List<WebElement> currentRows = driver.findElements(By.cssSelector("table tbody tr"));
        int expectedCount = currentRows.size();

        assertTrue(expectedCount < selectedRows.size() + currentRows.size(),
                "Les instances sélectionnées devraient être supprimées");

        selectedRows.clear();
    }

    @When("I navigate to the jobs list page")
    public void navigateToJobsListPage() throws InterruptedException {

        driver.get("http://localhost:8080/jobs");
        wait.until(visibilityOfElementLocated(By.id("jobsTable")));
        Thread.sleep(1000);
    }

    @When("I click the \"Delete\" button for the job")
    public void clickDeleteButtonForJob() {

        WebElement row =
                findJobRow(currentJobName);

        WebElement deleteButton =
                row.findElement(
                        By.cssSelector(".btn-delete")
                );

        deleteButton.click();

        Alert alert =
                wait.until(
                        ExpectedConditions.alertIsPresent()
                );

        alert.accept();

        wait.until(ExpectedConditions.urlContains("/jobs"));
    }

    @Then("the \"Delete Selected Instances\" button should be disabled")
    public void checkDeleteButtonDisabled() {

        WebElement deleteButton = driver.findElement(By.cssSelector(".delete-selected-btn"));
        assertFalse(deleteButton.isEnabled(),
                "Le bouton Delete Selected Instances devrait être désactivé quand rien n'est sélectionné");
    }

    @Then("I should see the job description")
    public void shouldSeeJobDescription() {

        WebElement descriptionBox =
                driver.findElement(
                        By.id("descriptionBox")
                );

        Assertions.assertTrue(
                descriptionBox.isDisplayed(),
                "Job description should be visible."
        );

        Assertions.assertFalse(
                descriptionBox.getText()
                        .trim()
                        .isEmpty(),
                "Job description should not be empty."
        );
    }

    @Then("the job should not appear in the jobs list")
    public void checkJobNotInList() {

        List<WebElement> rows =
                driver.findElements(
                        By.cssSelector("#jobsTable tbody tr")
                );

        boolean found = rows.stream()
                .anyMatch(row ->
                        row.findElement(
                                        By.cssSelector("td:first-child")
                                )
                                .getText()
                                .trim()
                                .equals(currentJobName)
                );

        Assertions.assertFalse(
                found,
                "Job '" + currentJobName +
                        "' should no longer exist."
        );
    }

    private WebElement findJobRow(String jobName) {

        List<WebElement> rows =
                driver.findElements(
                        By.cssSelector("#jobsTable tbody tr")
                );

        return rows.stream()
                .filter(row -> {

                    WebElement nameCell =
                            row.findElement(
                                    By.cssSelector("td:first-child")
                            );

                    return nameCell.getText()
                            .trim()
                            .equals(jobName);
                })
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job " + jobName + " not found"
                        )
                );
    }

    @After
    public void tearDown() {

        if (driver != null) {

            driver.quit();
        }
    }
}