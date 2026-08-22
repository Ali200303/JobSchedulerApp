Feature: Job Management
  As a logged-in user
  I want to create and manage scheduled jobs
  So that I can control their execution planning

  Scenario: Login, create job, and view planning
    Given I am logged in as "admin"
    When I create a job with name "IntegrationJob", description "Test job", frequency "MINUTES", interval 5
    Then I should see the job "IntegrationJob" in the jobs list
    And I should see its planning page with instances

  Scenario: Skip an active job instance
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    And I select the first ACTIVE instance
    And I click the instance action button
    Then the selected instance status should change to "SKIPPED"

  Scenario: Restore a skipped job instance
    Given I am logged in as "admin"
    And I have a job with a SKIPPED instance
    When I navigate to the job planning page
    And I select the SKIPPED instance
    And I click the instance action button
    Then the selected instance status should change to "ACTIVE"

  Scenario: Delete selected instances
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    And I select 2 active instances by clicking on their rows
    And I click the "Delete Selected Instances" button
    Then the selected instances should be removed from the planning

  Scenario: Delete selected instances button is disabled by default
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    Then the "Delete Selected Instances" button should be disabled

  Scenario: Delete a job from jobs list
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the jobs list page
    And I click the "Delete" button for the job
    Then the job should not appear in the jobs list

  Scenario: View job description from planning
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    And I click the "View Description" button
    Then I should see the job description