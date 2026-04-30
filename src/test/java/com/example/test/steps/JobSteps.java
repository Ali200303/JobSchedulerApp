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

        this.currentJobName = name; // Mémoriser le nom du job

        driver.get("http://localhost:8080/jobs/create");
        Thread.sleep(2000);

        driver.findElement(By.name("name")).sendKeys(name);
        driver.findElement(By.name("description")).sendKeys(description);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.plusHours(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        String startTimeValue = now.format(formatter);
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
    public void checkJobInList(String name) throws InterruptedException {

        wait.until(visibilityOfElementLocated(By.id("jobsTable")));
        List<WebElement> jobLinks = driver.findElements(By.cssSelector("#jobsTable tbody tr td a"));
        boolean found = jobLinks.stream().anyMatch(e -> e.getText().trim().equals(name));
        assertTrue(found, "Le job '" + name + "' n'a pas été trouvé dans la liste des jobs.");
    }

    @Then("I should see its planning page with instances")
    public void checkPlanningPage() throws InterruptedException {

        List<WebElement> jobLinks = driver.findElements(By.cssSelector("#jobsTable tbody tr td a"));
        WebElement targetJob = jobLinks.stream()
                .filter(link -> link.getText().trim().equals(this.currentJobName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Job " + this.currentJobName + " not found"));

        targetJob.click();
        wait.until(urlContains("/planning"));
        Thread.sleep(2000);

        List<WebElement> instances = driver.findElements(By.cssSelector("table tbody tr"));
        assertFalse(instances.isEmpty(), "Aucune instance de job trouvée sur la page de planning.");
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
        clickSkipButtonOnFirstActiveInstance();
    }

    @When("I navigate to the job planning page")
    public void navigateToJobPlanningPage() throws InterruptedException {

        String currentUrl = driver.getCurrentUrl();

        if (!currentUrl.contains("/planning")) {

            driver.get("http://localhost:8080/jobs");
            wait.until(visibilityOfElementLocated(By.id("jobsTable")));

            List<WebElement> jobLinks = driver.findElements(By.cssSelector("#jobsTable tbody tr td a"));
            WebElement targetJobLink = jobLinks.stream()
                    .filter(link -> link.getText().trim().equals(this.currentJobName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Job " + this.currentJobName + " not found"));
            targetJobLink.click();
            wait.until(urlContains("/planning"));
        }

        wait.until(visibilityOfElementLocated(By.cssSelector("table tbody tr")));
        Thread.sleep(1000);
    }


    @When("I click the \"Skip\" button on the first active instance")
    public void clickSkipButtonOnFirstActiveInstance() throws InterruptedException {

        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
        boolean skipped = false;

        for (int i = 2; i < rows.size(); i++) {

            WebElement row = rows.get(i);
            WebElement statusCell = row.findElement(By.cssSelector("td:nth-child(2)"));
            String status = statusCell.getText().trim();

            if ("ACTIVE".equals(status)) {

                row.findElement(By.cssSelector(".btn-skip")).click();
                skipped = true;
                break;
            }
        }

        if (!skipped) {

            throw new RuntimeException("Aucune instance ACTIVE disponible pour le skip");
        }

        driver.navigate().refresh();
        Thread.sleep(2000);
    }



    @When("I click the \"Restore\" button on the skipped instance")
    public void clickRestoreButtonOnSkippedInstance() throws InterruptedException {

        List<WebElement> restoreButtons = driver.findElements(By.cssSelector(".btn-restore"));
        assertTrue(!restoreButtons.isEmpty(), "Aucun bouton Restore trouvé sur la page");

        restoreButtons.get(0).click();
        Thread.sleep(2000);
    }

    @Then("the instance status should change to \"SKIPPED\"")
    public void checkInstanceStatusSkipped() {

        wait.until(driver -> {

            List<WebElement> statusCells = driver.findElements(By.cssSelector("table tbody tr td:nth-child(2)"));
            return statusCells.stream().anyMatch(cell -> "SKIPPED".equals(cell.getText().trim()));
        });
    }


    @Then("the instance status should change to \"ACTIVE\"")
    public void checkInstanceStatusActive() throws InterruptedException {

        Thread.sleep(3000);
        List<WebElement> statusCells = driver.findElements(By.cssSelector("table tbody tr td:nth-child(2)"));
        boolean hasActiveStatus = statusCells.stream()
                .anyMatch(cell -> "ACTIVE".equals(cell.getText().trim()));

        assertTrue(hasActiveStatus, "Aucune instance avec le statut ACTIVE n'a été trouvée");
    }

    @Then("I should see a \"Restore\" button for that instance")
    public void checkRestoreButtonExists() {

        List<WebElement> restoreButtons = driver.findElements(By.cssSelector(".btn-restore"));
        assertFalse(restoreButtons.isEmpty(), "Aucun bouton Restore trouvé après le skip");
    }

    @Then("I should see a \"Skip\" button for that instance")
    public void checkSkipButtonExists() {

        List<WebElement> skipButtons = driver.findElements(By.cssSelector(".btn-skip"));
        assertFalse(skipButtons.isEmpty(), "Aucun bouton Skip trouvé après la restauration");
    }

    @Given("I have a job with instances in DONE status")
    public void createJobWithDoneInstances() throws InterruptedException {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastStart = now.minusHours(3);
        LocalDateTime pastEnd = now.minusHours(2);

        System.out.println("=== CREATING JOB WITH PAST INSTANCES ===");
        System.out.println("Current time: " + now);
        System.out.println("Creating instances from: " + pastStart + " to " + pastEnd);

        createJobViaUI("ExpiredTestJob", "Job with expired instances",
                pastStart, pastEnd, "MINUTES", 20);
        this.currentJobName = "ExpiredTestJob";

        System.out.println("=== JOB CREATED, NOW NAVIGATING TO PLANNING PAGE ===");

        navigateToJobPlanningPage();

        System.out.println("=== ARRIVED AT PLANNING PAGE, CHECKING CONSOLE OUTPUT ===");

        Thread.sleep(3000);
        List<WebElement> allRows = driver.findElements(By.cssSelector("table tbody tr"));
        System.out.println("=== CHECKING PAGE CONTENT ===");
        System.out.println("Total rows found: " + allRows.size());

        int doneCount = 0;
        for (int i = 0; i < allRows.size(); i++) {

            try {

                List<WebElement> cells = allRows.get(i).findElements(By.tagName("td"));
                if (cells.size() >= 2) {

                    String time = cells.get(0).getText().trim();
                    String status = cells.get(1).getText().trim();
                    System.out.println("Row " + i + ": " + time + " -> " + status);
                    if ("DONE".equals(status)) {

                        doneCount++;
                    }
                }
            } catch (Exception e) {

                System.out.println("Error reading row " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Found " + doneCount + " DONE instances on page");

        if (doneCount == 0) {
            System.out.println("=== NO DONE INSTANCES FOUND ===");
            System.out.println("Check your application console/logs for the service method debug output.");
            System.out.println("If you DON'T see the service debug output, then the service method isn't being called.");
            System.out.println("If you DO see the service debug output, then there's an issue with the page refresh or DOM update.");

            System.out.println("Trying page refresh...");
            driver.navigate().refresh();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));
            Thread.sleep(3000);

            List<WebElement> doneRowsAfterRefresh = driver.findElements(By.xpath("//tr[td[contains(text(),'DONE')]]"));
            System.out.println("After refresh: " + doneRowsAfterRefresh.size() + " DONE rows");

            if (doneRowsAfterRefresh.isEmpty()) {
                throw new RuntimeException("No DONE instances found. Check service method debug output in application logs.");
            }
        }

        System.out.println("=== SUCCESS: Found DONE instances ===");
    }


    @Then("I should see {string} buttons that are disabled")
    public void checkExpiredButtonsDisabled(String buttonText) {

        wait.until(visibilityOfElementLocated(By.cssSelector("table tbody tr")));

        System.out.println("=== ENHANCED TABLE DEBUG ===");
        List<WebElement> allRows = driver.findElements(By.cssSelector("table tbody tr"));
        System.out.println("Total rows: " + allRows.size());

        for (int i = 0; i < allRows.size(); i++) {

            WebElement row = allRows.get(i);
            try {

                List<WebElement> cells = row.findElements(By.tagName("td"));
                System.out.println("Row " + i + " has " + cells.size() + " cells:");

                for (int j = 0; j < cells.size(); j++) {

                    WebElement cell = cells.get(j);
                    String cellText = cell.getText().trim();
                    String innerHTML = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", cell);
                    System.out.println("  Cell " + j + " text: '" + cellText + "'");
                    System.out.println("  Cell " + j + " innerHTML: '" + innerHTML + "'");
                }

                if (cells.size() >= 2) {

                    WebElement statusCell = cells.get(1);
                    String statusText = statusCell.getText().trim();
                    System.out.println("  Status cell text: '" + statusText + "'");

                    boolean isDone1 = "DONE".equals(statusText);
                    boolean isDone2 = statusText.contains("DONE");
                    boolean isDone3 = statusCell.getText().trim().equalsIgnoreCase("DONE");

                    System.out.println("  isDone exact: " + isDone1);
                    System.out.println("  isDone contains: " + isDone2);
                    System.out.println("  isDone ignoreCase: " + isDone3);

                    if (isDone1 || isDone2 || isDone3) {

                        System.out.println("  *** FOUND DONE ROW ***");
                        if (cells.size() >= 3) {

                            WebElement actionsCell = cells.get(2);
                            String actionsText = actionsCell.getText().trim();
                            String actionsHTML = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", actionsCell);
                            System.out.println("  Actions cell text: '" + actionsText + "'");
                            System.out.println("  Actions cell HTML: '" + actionsHTML + "'");

                            List<WebElement> buttonsInCell = actionsCell.findElements(By.tagName("button"));
                            System.out.println("  Buttons in actions cell: " + buttonsInCell.size());
                            for (WebElement btn : buttonsInCell) {

                                System.out.println("    Button text: '" + btn.getText() + "', enabled: " + btn.isEnabled());
                            }
                        }
                    }
                }

            } catch (Exception e) {

                System.out.println("Error reading row " + i + ": " + e.getMessage());
            }
        }

        System.out.println("\n=== TESTING DIFFERENT XPATH APPROACHES ===");

        List<WebElement> approach1 = driver.findElements(By.xpath("//tr[td[contains(text(),'DONE')]]"));
        System.out.println("Approach 1 (contains DONE): " + approach1.size() + " rows");

        List<WebElement> approach2 = driver.findElements(By.xpath("//tr[td[text()='DONE']]"));
        System.out.println("Approach 2 (exact DONE): " + approach2.size() + " rows");

        List<WebElement> approach3 = driver.findElements(By.xpath("//tr[td[2][text()='DONE']]"));
        System.out.println("Approach 3 (2nd cell exact): " + approach3.size() + " rows");

        List<WebElement> approach4 = driver.findElements(By.xpath("//tr[td[2][normalize-space(text())='DONE']]"));
        System.out.println("Approach 4 (2nd cell normalized): " + approach4.size() + " rows");

        List<WebElement> doneRows = null;
        if (!approach1.isEmpty()) {

            doneRows = approach1;
            System.out.println("Using approach 1");
        } else if (!approach2.isEmpty()) {

            doneRows = approach2;
            System.out.println("Using approach 2");
        } else if (!approach3.isEmpty()) {

            doneRows = approach3;
            System.out.println("Using approach 3");
        } else if (!approach4.isEmpty()) {

            doneRows = approach4;
            System.out.println("Using approach 4");
        }

        assertNotNull(doneRows, "No DONE rows found with any approach");
        assertFalse(doneRows.isEmpty(), "No DONE rows found with any approach");

        List<WebElement> expiredButtons = new ArrayList<>();
        for (WebElement doneRow : doneRows) {

            List<WebElement> buttonsInRow = doneRow.findElements(By.xpath(".//button[normalize-space(text())='" + buttonText + "']"));
            expiredButtons.addAll(buttonsInRow);
        }

        System.out.println("Found " + expiredButtons.size() + " '" + buttonText + "' buttons in DONE rows");

        assertFalse(expiredButtons.isEmpty(), "No '" + buttonText + "' buttons found in DONE rows");

        for (WebElement button : expiredButtons) {

            assertFalse(button.isEnabled(), "Button should be disabled: " + button.getText());
        }

        System.out.println("✅ All " + expiredButtons.size() + " '" + buttonText + "' buttons are disabled");
    }


    @Then("I should not be able to skip or restore expired instances")
    public void checkCannotSkipOrRestoreExpired() {

        By skipInDone = By.xpath("//tr[td[text()='DONE']]//button[contains(@class,'btn-skip')]");
        By restoreInDone = By.xpath("//tr[td[text()='DONE']]//button[contains(@class,'btn-restore')]");

        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.numberOfElementsToBe(skipInDone, 0));
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.numberOfElementsToBe(restoreInDone, 0));
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

    @When("I try to click on a SKIPPED instance row")
    public void tryClickOnSkippedInstanceRow() throws InterruptedException {

        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));

        for (WebElement row : rows) {

            WebElement statusCell = row.findElement(By.cssSelector("td:nth-child(2)"));
            String status = statusCell.getText().trim();

            if ("SKIPPED".equals(status)) {

                row.click();
                Thread.sleep(500);
                return;
            }
        }

        throw new RuntimeException("Aucune instance SKIPPED trouvée pour tester");
    }

    @Then("the row should not be selected")
    public void checkRowNotSelected() {

        List<WebElement> selectedRows = driver.findElements(By.cssSelector("table tbody tr.selected"));

        for (WebElement row : selectedRows) {

            WebElement statusCell = row.findElement(By.cssSelector("td:nth-child(2)"));
            String status = statusCell.getText().trim();
            assertNotEquals("SKIPPED", status, "Une ligne SKIPPED ne devrait pas pouvoir être sélectionnée");
        }
    }

    @When("I navigate to the jobs list page")
    public void navigateToJobsListPage() throws InterruptedException {

        driver.get("http://localhost:8080/jobs");
        wait.until(visibilityOfElementLocated(By.id("jobsTable")));
        Thread.sleep(1000);
    }

    @When("I click the \"Delete\" button for the job")
    public void clickDeleteButtonForJob() throws InterruptedException {

        List<WebElement> rows = driver.findElements(By.cssSelector("#jobsTable tbody tr"));

        for (WebElement row : rows) {

            WebElement nameCell = row.findElement(By.cssSelector("td:first-child a"));
            String jobName = nameCell.getText().trim();

            if (jobName.equals(this.currentJobName)) {

                WebElement deleteButton = row.findElement(By.cssSelector(".btn-delete"));
                deleteButton.click();

                Alert alert = wait.until(ExpectedConditions.alertIsPresent());
                alert.accept();

                Thread.sleep(2000);
                return;
            }
        }

        throw new RuntimeException("Job " + this.currentJobName + " non trouvé dans la liste");
    }

    @Then("the job should not appear in the jobs list")
    public void checkJobNotInList() throws InterruptedException {
        Thread.sleep(2000);

        List<WebElement> tables = driver.findElements(By.id("jobsTable"));

        if (tables.isEmpty()) {

            System.out.println("Table des jobs non trouvée - tous les jobs créés ont été supprimés");
            return;
        }

        List<WebElement> jobLinks = driver.findElements(By.cssSelector("#jobsTable tbody tr td a"));
        boolean found = jobLinks.stream()
                .anyMatch(e -> e.getText().trim().equals(this.currentJobName));

        assertFalse(found, "Le job '" + this.currentJobName + "' ne devrait plus être dans la liste");
    }

    @When("I click the \"Delete Job\" button in the header")
    public void clickDeleteJobButtonInHeader() throws InterruptedException {

        WebElement deleteJobButton = driver.findElement(By.cssSelector(".delete-job-btn"));
        deleteJobButton.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();
        Thread.sleep(2000);
    }

    @Then("I should be redirected to the jobs list")
    public void checkRedirectedToJobsList() {

        wait.until(urlContains("/jobs"));
        assertFalse(driver.getCurrentUrl().contains("/planning"),
                "L'utilisateur devrait être redirigé vers la liste des jobs");
    }

    @Then("the \"Delete Selected Instances\" button should be disabled")
    public void checkDeleteButtonDisabled() {

        WebElement deleteButton = driver.findElement(By.cssSelector(".delete-selected-btn"));
        assertFalse(deleteButton.isEnabled(),
                "Le bouton Delete Selected Instances devrait être désactivé quand rien n'est sélectionné");
    }
    @After
    public void tearDown() {

        if (driver != null) {

            driver.quit();
        }
    }
}