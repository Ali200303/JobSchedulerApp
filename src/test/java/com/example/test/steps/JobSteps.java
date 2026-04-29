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
    private String currentJobName; // Pour suivre le job en cours de test

    // ===== MÉTHODES EXISTANTES =====
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

        // Format datetime-local
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
        if (!jobLinks.isEmpty()) {
            jobLinks.get(0).click();
        }

        wait.until(urlContains("/planning"));
        Thread.sleep(2000);

        List<WebElement> instances = driver.findElements(By.cssSelector("table tbody tr"));
        assertFalse(instances.isEmpty(), "Aucune instance de job trouvée sur la page de planning.");
    }

    // ===== NOUVELLES MÉTHODES POUR SKIP/RESTORE =====

    @Given("I have a job with instances in ACTIVE status")
    public void createJobWithActiveInstances() throws InterruptedException {
        // Créer un job avec des instances actives (dans le futur proche)
        createJob("SkipTestJob", "Job for skip testing", "MINUTES", 30);
        this.currentJobName = "SkipTestJob";
    }

    @Given("I have a job with a SKIPPED instance")
    public void createJobWithSkippedInstance() throws InterruptedException {
        // D'abord créer un job avec des instances
        createJobWithActiveInstances();
        // Puis naviguer vers le planning et skip une instance
        navigateToJobPlanningPage();
        clickSkipButtonOnFirstActiveInstance();
    }

    @When("I navigate to the job planning page")
    public void navigateToJobPlanningPage() throws InterruptedException {
        String currentUrl = driver.getCurrentUrl();

        if (!currentUrl.contains("/planning")) {
            // Retourner à la liste des jobs
            driver.get("http://localhost:8080/jobs");
            wait.until(visibilityOfElementLocated(By.id("jobsTable")));

            // Cliquer sur le job actuel
            List<WebElement> jobLinks = driver.findElements(By.cssSelector("#jobsTable tbody tr td a"));
            WebElement targetJobLink = jobLinks.stream()
                    .filter(link -> link.getText().trim().equals(this.currentJobName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Job " + this.currentJobName + " not found"));
            targetJobLink.click();
            wait.until(urlContains("/planning"));
        }

        // Attendre que la table des instances soit visible
        wait.until(visibilityOfElementLocated(By.cssSelector("table tbody tr")));
        Thread.sleep(1000);
    }


    @When("I click the \"Skip\" button on the first active instance")
    public void clickSkipButtonOnFirstActiveInstance() throws InterruptedException {
        List<WebElement> rows = driver.findElements(By.cssSelector("table tbody tr"));
        boolean skipped = false;

        // Commencer à la 3ᵉ instance pour éviter les expirations rapides
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

        // Attendre que le DOM se mette à jour ou recharger la page
        driver.navigate().refresh();
        Thread.sleep(2000);
    }



    @When("I click the \"Restore\" button on the skipped instance")
    public void clickRestoreButtonOnSkippedInstance() throws InterruptedException {
        // Trouver le bouton "Restore" (instance SKIPPED)
        List<WebElement> restoreButtons = driver.findElements(By.cssSelector(".btn-restore"));
        assertTrue(!restoreButtons.isEmpty(), "Aucun bouton Restore trouvé sur la page");

        restoreButtons.get(0).click();
        Thread.sleep(2000); // Attendre que la page se recharge
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
        Thread.sleep(3000); // Attendre que la page se recharge après l'action

        // Vérifier qu'il y a au moins une instance avec le statut ACTIVE
        List<WebElement> statusCells = driver.findElements(By.cssSelector("table tbody tr td:nth-child(2)"));
        boolean hasActiveStatus = statusCells.stream()
                .anyMatch(cell -> "ACTIVE".equals(cell.getText().trim()));

        assertTrue(hasActiveStatus, "Aucune instance avec le statut ACTIVE n'a été trouvée");
    }

    @Then("I should see a \"Restore\" button for that instance")
    public void checkRestoreButtonExists() {
        // Vérifier qu'il y a au moins un bouton Restore
        List<WebElement> restoreButtons = driver.findElements(By.cssSelector(".btn-restore"));
        assertFalse(restoreButtons.isEmpty(), "Aucun bouton Restore trouvé après le skip");
    }

    @Then("I should see a \"Skip\" button for that instance")
    public void checkSkipButtonExists() {
        // Vérifier qu'il y a au moins un bouton Skip
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

        // Create job with past instances
        createJobViaUI("ExpiredTestJob", "Job with expired instances",
                pastStart, pastEnd, "MINUTES", 20);
        this.currentJobName = "ExpiredTestJob";

        System.out.println("=== JOB CREATED, NOW NAVIGATING TO PLANNING PAGE ===");

        // Navigate to planning page - this should trigger the service method
        navigateToJobPlanningPage();

        System.out.println("=== ARRIVED AT PLANNING PAGE, CHECKING CONSOLE OUTPUT ===");

        // Look for the service method debug output in your application logs
        // The service should have printed:
        // "Current time: ..."
        // "Instance X - Scheduled: ... - Status: ... - Is after now: ..."
        // "Marking instance X as DONE"

        Thread.sleep(3000); // Give time for any async processing

        // Check what we actually have on the page
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

            // Try one refresh
            System.out.println("Trying page refresh...");
            driver.navigate().refresh();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table tbody tr")));
            Thread.sleep(3000);

            // Check again
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
        // Wait for table to be visible
        wait.until(visibilityOfElementLocated(By.cssSelector("table tbody tr")));

        System.out.println("=== ENHANCED TABLE DEBUG ===");
        List<WebElement> allRows = driver.findElements(By.cssSelector("table tbody tr"));
        System.out.println("Total rows: " + allRows.size());

        // Debug each row with multiple approaches
        for (int i = 0; i < allRows.size(); i++) {
            WebElement row = allRows.get(i);
            try {
                // Get all cells in this row
                List<WebElement> cells = row.findElements(By.tagName("td"));
                System.out.println("Row " + i + " has " + cells.size() + " cells:");

                for (int j = 0; j < cells.size(); j++) {
                    WebElement cell = cells.get(j);
                    String cellText = cell.getText().trim();
                    String innerHTML = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", cell);
                    System.out.println("  Cell " + j + " text: '" + cellText + "'");
                    System.out.println("  Cell " + j + " innerHTML: '" + innerHTML + "'");
                }

                // Specifically check the status cell (should be cell 1, 0-indexed)
                if (cells.size() >= 2) {
                    WebElement statusCell = cells.get(1); // Status is 2nd column
                    String statusText = statusCell.getText().trim();
                    System.out.println("  Status cell text: '" + statusText + "'");

                    // Try different ways to check for DONE
                    boolean isDone1 = "DONE".equals(statusText);
                    boolean isDone2 = statusText.contains("DONE");
                    boolean isDone3 = statusCell.getText().trim().equalsIgnoreCase("DONE");

                    System.out.println("  isDone exact: " + isDone1);
                    System.out.println("  isDone contains: " + isDone2);
                    System.out.println("  isDone ignoreCase: " + isDone3);

                    if (isDone1 || isDone2 || isDone3) {
                        System.out.println("  *** FOUND DONE ROW ***");
                        // Check actions cell
                        if (cells.size() >= 3) {
                            WebElement actionsCell = cells.get(2);
                            String actionsText = actionsCell.getText().trim();
                            String actionsHTML = (String) ((JavascriptExecutor) driver).executeScript("return arguments[0].innerHTML;", actionsCell);
                            System.out.println("  Actions cell text: '" + actionsText + "'");
                            System.out.println("  Actions cell HTML: '" + actionsHTML + "'");

                            // Look for buttons in actions cell
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

        // Try different XPath approaches
        System.out.println("\n=== TESTING DIFFERENT XPATH APPROACHES ===");

        // Approach 1: Look for any cell containing DONE
        List<WebElement> approach1 = driver.findElements(By.xpath("//tr[td[contains(text(),'DONE')]]"));
        System.out.println("Approach 1 (contains DONE): " + approach1.size() + " rows");

        // Approach 2: Look for exact DONE in any cell
        List<WebElement> approach2 = driver.findElements(By.xpath("//tr[td[text()='DONE']]"));
        System.out.println("Approach 2 (exact DONE): " + approach2.size() + " rows");

        // Approach 3: Look for DONE in 2nd cell specifically
        List<WebElement> approach3 = driver.findElements(By.xpath("//tr[td[2][text()='DONE']]"));
        System.out.println("Approach 3 (2nd cell exact): " + approach3.size() + " rows");

        // Approach 4: Look for DONE in 2nd cell with normalize-space
        List<WebElement> approach4 = driver.findElements(By.xpath("//tr[td[2][normalize-space(text())='DONE']]"));
        System.out.println("Approach 4 (2nd cell normalized): " + approach4.size() + " rows");

        // Find the working approach and use it
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

        // Now look for expired buttons in the DONE rows
        List<WebElement> expiredButtons = new ArrayList<>();
        for (WebElement doneRow : doneRows) {
            List<WebElement> buttonsInRow = doneRow.findElements(By.xpath(".//button[normalize-space(text())='" + buttonText + "']"));
            expiredButtons.addAll(buttonsInRow);
        }

        System.out.println("Found " + expiredButtons.size() + " '" + buttonText + "' buttons in DONE rows");

        assertFalse(expiredButtons.isEmpty(), "No '" + buttonText + "' buttons found in DONE rows");

        // Verify all buttons are disabled
        for (WebElement button : expiredButtons) {
            assertFalse(button.isEnabled(), "Button should be disabled: " + button.getText());
        }

        System.out.println("✅ All " + expiredButtons.size() + " '" + buttonText + "' buttons are disabled");
    }


    @Then("I should not be able to skip or restore expired instances")
    public void checkCannotSkipOrRestoreExpired() {
        By skipInDone = By.xpath("//tr[td[text()='DONE']]//button[contains(@class,'btn-skip')]");
        By restoreInDone = By.xpath("//tr[td[text()='DONE']]//button[contains(@class,'btn-restore')]");

        // wait until no skip/restore buttons remain in DONE rows
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

        // Injecter start + end
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


    // Méthode de nettoyage après chaque test
    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}