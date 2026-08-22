# 📅 Job Scheduler Application

A full-stack web application built with **Spring Boot** that allows users to create, manage, edit, and track scheduled jobs with automated instance generation and state management.

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
- 📋 Create, view, edit, and delete scheduled jobs
- ✏️ Edit job name, description, execution period, and frequency
- 🛡️ Enforce unique job names and validate scheduling dates
- ⏱️ Define job frequency using minutes, hours, or days
- 🧩 Automatically generate job instances over a defined time range
- 🔄 Regenerate future instances when a job schedule is updated while preserving execution history
- 🔎 Search jobs by name or description
- ⚙️ Filter jobs by status and scheduling frequency
- 🔀 Combine search and filters for more precise job discovery
- 🟢 Automatically keep jobs ACTIVE while future instances remain
- ⚪ Automatically mark jobs INACTIVE when no future instances remain
- ✔️ Manage job instance states:
  - ACTIVE
  - DONE
  - SKIPPED
- ⏭️ Skip future ACTIVE instances
- ↩️ Restore SKIPPED instances before their scheduled execution time
- 🕒 Automatically mark expired ACTIVE instances as DONE while preserving SKIPPED history
- 🗑️ Select and delete multiple job instances
- 🗑️ Cascade deletion of jobs and their related instances
- 📊 Visualize job execution planning with status-based highlighting
- 📝 View job descriptions directly from the planning page

---

## 🧠 Highlights

- Designed a **modular MVC architecture** using Controller, Service, Repository, and Model layers
- Implemented **time-based scheduling logic** with automatic instance generation
- Built **state management rules** for ACTIVE, DONE, and SKIPPED job instances
- Implemented automatic **ACTIVE/INACTIVE job lifecycle management** based on remaining future instances
- Preserved historical instances while regenerating future schedules after job updates
- Implemented **input validation**, including unique job names and schedule date validation
- Added interactive planning controls for **Skip, Restore, multi-selection, and deletion**
- Implemented **job search and dynamic filtering** by status and scheduling frequency
- Built **end-to-end automated tests** using BDD with Cucumber and Selenium
- Focused on **maintainability, separation of concerns, and reliable business logic**

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

---

## ▶️ Run the Project

```bash
git clone https://github.com/Ali200303/JobSchedulerApp.git
cd JobSchedulerApp
mvn spring-boot:run
```

Then open your browser at: `http://localhost:8080`