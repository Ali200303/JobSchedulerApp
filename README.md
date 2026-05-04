# 📅 Job Scheduler Application

A full-stack web application built with **Spring Boot** that allows users to create, manage, and track scheduled jobs with automated instance generation and state management.

---

## 🚀 Tech Stack

- **Backend:** Java, Spring Boot
- **Frontend:** Thymeleaf
- **Database:** H2
- **Testing:** Cucumber (BDD), Selenium
- **Build Tool:** Maven

---

## ✨ Key Features

- 🔐 Session-based authentication system
- 📋 Create, view, and delete jobs
- ⏱️ Define job frequency (minutes, hours, days)
- 🧩 Automatic generation of job instances over a defined time range
- ✔️ Manage job instance states:
  - ACTIVE
  - DONE
  - SKIPPED
- 🔄 Restore skipped instances (when still valid)
- 🗑️ Cascade deletion of jobs and related instances
- 📊 Visualize job execution planning

---

## 🧠 Highlights

- Designed a **modular MVC architecture** (Controller / Service / Repository)
- Implemented **business logic with state transitions and time validation**
- Built **end-to-end automated tests** using BDD (Cucumber) and Selenium
- Focused on **code structure, maintainability, and reliability**

---

## 🧪 Testing (BDD with Cucumber)

Behavior Driven Development (BDD) is used to validate application behavior through human-readable scenarios.

### Example Scenario

```gherkin
Feature: Job scheduling

  Scenario: Generate job instances for a job
    Given a job with frequency "MINUTES" and interval "5"
    When I generate job instances between "2026-04-28T10:00" and "2026-04-28T10:30"
    Then multiple job instances should be created every 5 minutes
```
## ▶️ Run the Project

```bash
git clone https://github.com/Ali200303/JobSchedulerApp.git
cd JobSchedulerApp
mvn spring-boot:run
```
Then open your browser at:  
http://localhost:8080
