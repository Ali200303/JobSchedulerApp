Feature: Job Management
  As a logged-in user
  I want to create a job and view its planning
  So that I can manage my jobs

  Scenario: Login, create job, view planning
    Given I am logged in as "admin"
    When I create a job with name "IntegrationJob", description "Test job", frequency "MINUTES", interval 5
    Then I should see the job "IntegrationJob" in the jobs list
    And I should see its planning page with instances

  # scénario pour tester la fonctionnalité Skip
  Scenario: Skip a job instance
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    And I click the "Skip" button on the first active instance
    Then the instance status should change to "SKIPPED"
    And I should see a "Restore" button for that instance

  # scénario pour tester la fonctionnalité Restore
  Scenario: Restore a skipped job instance
    Given I am logged in as "admin"
    And I have a job with a SKIPPED instance
    When I navigate to the job planning page
    And I click the "Restore" button on the skipped instance
    Then the instance status should change to "ACTIVE"
    And I should see a "Skip" button for that instance

  # scénario pour tester les instances expirées
  Scenario: Cannot skip expired instances
    Given I am logged in as "admin"
    And I have a job with instances in DONE status
    When I navigate to the job planning page
    Then I should see "Expired" buttons that are disabled
    And I should not be able to skip or restore expired instances