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

  # scénario pour tester la sélection et suppression d'instances
  Scenario: Delete selected instances
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    And I select 2 active instances by clicking on their rows
    And I click the "Delete Selected Instances" button
    Then the selected instances should be removed from the planning

  # scénario pour tester qu'on ne peut pas sélectionner les instances SKIPPED
  Scenario: Cannot select skipped instances
    Given I am logged in as "admin"
    And I have a job with a SKIPPED instance
    When I navigate to the job planning page
    And I try to click on a SKIPPED instance row
    Then the row should not be selected

  # scénario pour tester la suppression d'un job depuis la liste
  Scenario: Delete a job from jobs list
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the jobs list page
    And I click the "Delete" button for the job
    Then the job should not appear in the jobs list

  # scénario pour tester que le bouton Delete est désactivé quand rien n'est sélectionné
  Scenario: Delete button disabled when no instances selected
    Given I am logged in as "admin"
    And I have a job with instances in ACTIVE status
    When I navigate to the job planning page
    Then the "Delete Selected Instances" button should be disabled