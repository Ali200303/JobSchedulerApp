# 📅 Job Scheduler Web Application

This project is a **Spring Boot web application** developed as a practice project to learn and apply **Cucumber (BDD testing)**, along with core backend concepts such as scheduling, job management, and instance tracking.

---

## 🚀 Features

- 🔐 Simple login system (session-based authentication)
- 📋 Create and manage jobs
- ⏱️ Define job frequency (minutes, hours, days)
- 🧩 Automatic generation of job instances over a time range
- ✔️ Track job instances status:
    - ACTIVE
    - DONE
    - SKIPPED
- 🔄 Restore skipped instances (if still valid)
- 🗑️ Delete jobs and their related instances
- 📊 View job execution planning per job

---

## 🧪 Testing (Cucumber)

This project was built to practice **Behavior Driven Development (BDD)** using Cucumber.

### Goals:
- Write human-readable test scenarios
- Validate rules (job scheduling logic)
- Test job instance generation and status transitions
- Improve code reliability and structure

### Example Feature:

```gherkin
Feature: Job scheduling

  Scenario: Generate job instances for a job
    Given a job with frequency "MINUTES" and interval "5"
    When I generate job instances between "2026-04-28T10:00" and "2026-04-28T10:30"
    Then multiple job instances should be created every 5 minutes