package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobInstanceRepository;
import com.example.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository jobInstanceRepository;

    public JobService(JobRepository jobRepository,
                      JobInstanceRepository jobInstanceRepository) {
        this.jobRepository = jobRepository;
        this.jobInstanceRepository = jobInstanceRepository;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Job not found: " + id));
    }

    public Job createJob(String name,
                         String description,
                         String frequency,
                         Integer interval) {

        validateJob(name, frequency, interval);

        Job job = new Job(
                name.trim(),
                description != null ? description.trim() : "",
                frequency.toUpperCase(),
                interval
        );

        return jobRepository.save(job);
    }

    @Transactional
    public void deleteJob(Long id) {

        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job not found: " + id);
        }

        jobInstanceRepository.deleteByJob_Id(id);
        jobRepository.deleteById(id);
    }

    @Transactional
    public Job updateJobStatus(Long id, String status) {

        Job job = getJobById(id);

        if (!List.of("ACTIVE", "PAUSED", "STOPPED")
                .contains(status.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid job status: " + status
            );
        }

        job.setStatus(status.toUpperCase());

        return job;
    }

    @Transactional
    public List<JobInstance> generateJobInstances(
            Long jobId,
            LocalDateTime start,
            LocalDateTime end) {

        Job job = getJobById(jobId);
        validateSchedule(job, start, end);

        List<JobInstance> existing =
                jobInstanceRepository
                        .findByJob_IdAndScheduledTimeBetween(jobId, start, end);

        List<LocalDateTime> existingTimes = existing.stream()
                .map(JobInstance::getScheduledTime)
                .toList();

        List<JobInstance> newInstances = new ArrayList<>();

        LocalDateTime current = start;

        while (!current.isAfter(end)) {

            if (!existingTimes.contains(current)) {
                newInstances.add(new JobInstance(job, current));
            }

            current = calculateNextExecution(
                    current,
                    job.getFrequency(),
                    job.getInterval()
            );
        }

        if (!newInstances.isEmpty()) {
            jobInstanceRepository.saveAll(newInstances);
        }

        return jobInstanceRepository
                .findByJob_IdAndScheduledTimeBetween(jobId, start, end);
    }

    private LocalDateTime calculateNextExecution(
            LocalDateTime current,
            String frequency,
            int interval) {

        return switch (frequency.toUpperCase()) {
            case "MINUTES" ->
                    current.plusMinutes(interval);

            case "HOURS" ->
                    current.plusHours(interval);

            case "DAYS" ->
                    current.plusDays(interval);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown frequency: " + frequency
                    );
        };
    }

    private void validateJob(
            String name,
            String frequency,
            Integer interval) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Job name cannot be empty"
            );
        }

        if (interval == null || interval <= 0) {
            throw new IllegalArgumentException(
                    "Interval must be greater than 0"
            );
        }

        if (frequency == null ||
                !List.of("MINUTES", "HOURS", "DAYS")
                        .contains(frequency.toUpperCase())) {

            throw new IllegalArgumentException(
                    "Invalid frequency: " + frequency
            );
        }
    }

    private void validateSchedule(
            Job job,
            LocalDateTime start,
            LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start and end dates are required"
            );
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start must be before end"
            );
        }

        if (job.getInterval() == null ||
                job.getInterval() <= 0) {

            throw new IllegalArgumentException(
                    "Interval must be greater than 0"
            );
        }
    }

    public void initializeSampleData() {

        if (jobRepository.count() == 0) {

            createJob(
                    "Backup Database",
                    "Daily database backup",
                    "HOURS",
                    24
            );

            createJob(
                    "Send Reports",
                    "Email weekly reports",
                    "DAYS",
                    7
            );

            createJob(
                    "Clean Temp Files",
                    "Clear temporary files",
                    "MINUTES",
                    5
            );
        }
    }
}