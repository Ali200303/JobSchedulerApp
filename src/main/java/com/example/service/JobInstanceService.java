package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobInstanceRepository;
import com.example.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobInstanceService {

    private final JobInstanceRepository repository;
    private final JobRepository jobRepository;

    public JobInstanceService(JobInstanceRepository repository, JobRepository jobRepository) {

        this.repository = repository;
        this.jobRepository = jobRepository;
    }



    public JobInstance skipInstance(Long id) {

        JobInstance instance = repository.findById(id).orElse(null);
        if (instance != null) {

            LocalDateTime now = LocalDateTime.now();
            if ("ACTIVE".equals(instance.getStatus()) && !now.isAfter(instance.getScheduledTime())) {

                instance.setStatus("SKIPPED");
                repository.save(instance);
            }
        }
        return instance;
    }


    public List<JobInstance> getInstancesForJob(Long jobId) {

        List<JobInstance> instances = repository.findByJob_IdOrderByScheduledTimeAsc(jobId);
        LocalDateTime now = LocalDateTime.now();

        System.out.println("Current time: " + now);

        for (JobInstance instance : instances) {

            System.out.println("Instance " + instance.getId() +
                    " - Scheduled: " + instance.getScheduledTime() +
                    " - Status: " + instance.getStatus() +
                    " - Is after now: " + instance.getScheduledTime().isAfter(now));

            if ("ACTIVE".equals(instance.getStatus())) {

                if (instance.getScheduledTime().isBefore(now) || instance.getScheduledTime().isEqual(now)) {

                    System.out.println("Marking instance " + instance.getId() + " as DONE");
                    instance.setStatus("DONE");
                    repository.save(instance);
                }
            }
        }

        return instances;
    }


    public JobInstance restoreInstance(Long id) {

        JobInstance instance = repository.findById(id).orElse(null);
        if (instance != null) {
            LocalDateTime now = LocalDateTime.now();
            if ("SKIPPED".equals(instance.getStatus()) && !now.isAfter(instance.getScheduledTime())) {
                instance.setStatus("ACTIVE");
                repository.save(instance);
            }
        }
        return instance;
    }

    public String getJobName(Long jobId) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        return job.getName();
    }

}
