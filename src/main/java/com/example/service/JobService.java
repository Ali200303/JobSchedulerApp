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

    @Transactional
    public List<Job> getAllJobs() {

        List<Job> jobs = jobRepository.findAll();

        LocalDateTime now = LocalDateTime.now();

        for (Job job : jobs) {

            boolean hasFutureInstances =
                    jobInstanceRepository
                            .existsByJob_IdAndScheduledTimeAfter(
                                    job.getId(),
                                    now
                            );

            if (hasFutureInstances) {
                job.setStatus("ACTIVE");
            } else {
                job.setStatus("INACTIVE");
            }
        }

        return jobs;
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Job not found: " + id));
    }

    @Transactional
    public Job createScheduledJob(String name,
                                  String description,
                                  String frequency,
                                  Integer interval,
                                  LocalDateTime start,
                                  LocalDateTime end) {

        validateJob(name, frequency, interval);
        validateUniqueJobName(name);
        validateScheduleDates(start, end);

        Job job = new Job(
                name.trim(),
                description != null ? description.trim() : "",
                frequency.toUpperCase(),
                interval,
                start,
                end
        );

        job = jobRepository.save(job);

        List<JobInstance> instances = new ArrayList<>();

        LocalDateTime current = start;

        while (!current.isAfter(end)) {

            instances.add(
                    new JobInstance(job, current)
            );

            current = calculateNextExecution(
                    current,
                    job.getFrequency(),
                    job.getInterval()
            );
        }

        jobInstanceRepository.saveAll(instances);

        return job;
    }

    @Transactional
    public Job updateJob(Long jobId,
                         String name,
                         String description,
                         String frequency,
                         Integer interval,
                         LocalDateTime start,
                         LocalDateTime end) {

        Job job = getJobById(jobId);

        validateJob(name, frequency, interval);
        validateUniqueJobNameForUpdate(name, jobId);
        validateUpdatedScheduleDates(start, end);

        job.setName(name.trim());

        job.setDescription(
                description != null
                        ? description.trim()
                        : ""
        );

        job.setFrequency(frequency.toUpperCase());
        job.setInterval(interval);
        job.setStartTime(start);
        job.setEndTime(end);

        LocalDateTime now = LocalDateTime.now();

        /*
         * Preserve historical instances.
         * Only future instances are regenerated.
         */
        jobInstanceRepository
                .deleteByJob_IdAndScheduledTimeAfter(
                        jobId,
                        now
                );

        /*
         * If the edited start date is already in the past,
         * start generating from the next execution after now.
         */
        LocalDateTime current = start;

        while (!current.isAfter(now)) {

            current = calculateNextExecution(
                    current,
                    job.getFrequency(),
                    job.getInterval()
            );
        }

        List<JobInstance> newInstances =
                new ArrayList<>();

        while (!current.isAfter(end)) {

            newInstances.add(
                    new JobInstance(job, current)
            );

            current = calculateNextExecution(
                    current,
                    job.getFrequency(),
                    job.getInterval()
            );
        }

        if (!newInstances.isEmpty()) {
            jobInstanceRepository.saveAll(newInstances);
        }

        return job;
    }

    @Transactional
    public void deleteJob(Long id) {

        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job not found: " + id);
        }

        jobInstanceRepository.deleteByJob_Id(id);
        jobRepository.deleteById(id);
    }

    private void validateUniqueJobName(String name) {

        if (jobRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException(
                    "A job with this name already exists"
            );
        }
    }

    private void validateUniqueJobNameForUpdate(String name, Long jobId) {

        if (jobRepository.existsByNameIgnoreCaseAndIdNot(
                name.trim(),
                jobId
        )) {
            throw new IllegalArgumentException(
                    "A job with this name already exists"
            );
        }
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

    private void validateSchedule(Job job,
                                  LocalDateTime start,
                                  LocalDateTime end) {

        if (job == null) {
            throw new IllegalArgumentException("Job cannot be null");
        }

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (start.isBefore(now)) {
            throw new IllegalArgumentException(
                    "Start time cannot be in the past"
            );
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (job.getInterval() == null || job.getInterval() <= 0) {
            throw new IllegalArgumentException(
                    "Interval must be greater than 0"
            );
        }
    }

    private void validateScheduleDates(LocalDateTime start,
                                       LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (start.isBefore(now)) {
            throw new IllegalArgumentException(
                    "Start time cannot be in the past"
            );
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }
    }

    private void validateUpdatedScheduleDates(LocalDateTime start,
                                              LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        if (end.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "End time cannot be in the past"
            );
        }
    }
}