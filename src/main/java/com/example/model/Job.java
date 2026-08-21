package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String frequency;
    private Integer intervalMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecuted;
    private String status; // "ACTIVE", "PAUSED", "STOPPED"
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Job() {}

    public Job(String name,
               String description,
               String frequency,
               Integer interval) {

        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.intervalMinutes = interval;
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public Job(String name,
               String description,
               String frequency,
               Integer interval,
               LocalDateTime startTime,
               LocalDateTime endTime) {

        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.intervalMinutes = interval;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public String getFrequencyDescription() {

        String unit = frequency.toLowerCase();
        if (intervalMinutes == 1) {
            unit = unit.substring(0, unit.length() - 1);
        }
        return "Every " + intervalMinutes + " " + unit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getInterval() { return intervalMinutes; }
    public void setInterval(Integer interval) { this.intervalMinutes = interval; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(LocalDateTime lastExecuted) { this.lastExecuted = lastExecuted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
