package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobRepository;
import com.example.repository.JobInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository jobInstanceRepository;

    public JobService(JobRepository jobRepository, JobInstanceRepository jobInstanceRepository) {
        this.jobRepository = jobRepository;
        this.jobInstanceRepository = jobInstanceRepository;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id).orElse(null);
    }

    public Job createJob(String name, String description, String frequency, Integer interval) {

        Job job = new Job(name, description, frequency, interval);
        return jobRepository.save(job);

    }

    @Transactional
    public void deleteJob(Long id) {

        jobInstanceRepository.deleteByJob_Id(id);
        jobRepository.deleteById(id);
    }

    public Job updateJobStatus(Long id, String status) {

        Optional<Job> optionalJob = jobRepository.findById(id);
        if (optionalJob.isPresent()) {
            Job job = optionalJob.get();
            job.setStatus(status);
            return jobRepository.save(job);
        }
        return null;
    }

    public void initializeSampleData() {

        if (jobRepository.count() == 0) {

            createJob("Backup Database", "Daily database backup", "HOURS", 24);
            createJob("Send Reports", "Email weekly reports", "DAYS", 7);
            createJob("Clean Temp Files", "Clear temporary files", "MINUTES", 5); // exemple 5 minutes
        }
    }

    public List<JobInstance> getJobInstances(Long jobId) {

        return jobInstanceRepository.findByJob_IdOrderByScheduledTimeAsc(jobId);
    }



    public void skipInstance(Long instanceId) {

        jobInstanceRepository.findById(instanceId).ifPresent(instance -> {

            if ("ACTIVE".equals(instance.getStatus())) {

                instance.setStatus("SKIPPED");
                jobInstanceRepository.save(instance);
            }
        });
    }

    public void restoreInstance(Long instanceId) {

        jobInstanceRepository.findById(instanceId).ifPresent(instance -> {
            LocalDateTime now = LocalDateTime.now();
            if (!now.isAfter(instance.getScheduledTime()) &&
                    ("SKIPPED".equals(instance.getStatus()) || "ACTIVE".equals(instance.getStatus()))) {

                instance.setStatus("ACTIVE");
                jobInstanceRepository.save(instance);
            }
        });
    }

    public List<JobInstance> generateJobInstances(Long jobId,
                                                  LocalDateTime start,
                                                  LocalDateTime end) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        int interval = job.getInterval();

        if (interval <= 0) {
            throw new IllegalArgumentException("Interval must be > 0");
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start must be before end");
        }

        List<JobInstance> existing =
                jobInstanceRepository.findByJob_IdAndScheduledTimeBetween(jobId, start, end);

        if (!existing.isEmpty()) {
            return existing;
        }

        List<JobInstance> newInstances = new ArrayList<>();
        LocalDateTime current = start;

        while (!current.isAfter(end)) {
            newInstances.add(new JobInstance(job, current));

            switch (job.getFrequency().toUpperCase()) {

                case "MINUTES":
                    current = current.plusMinutes(interval);
                    break;

                case "HOURS":
                    current = current.plusHours(interval);
                    break;

                case "DAYS":
                    current = current.plusDays(interval);
                    break;

                default:
                    throw new RuntimeException("Unknown frequency: " + job.getFrequency());
            }
        }

        jobInstanceRepository.saveAll(newInstances);
        return newInstances;
    }


}
