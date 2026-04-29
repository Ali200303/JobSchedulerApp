package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobRepository;
import com.example.repository.JobInstanceRepository;
import org.springframework.stereotype.Service;

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
        Job savedJob = jobRepository.save(job);

        // Créer les instances si fréquence = MINUTES
        if (interval != null && interval > 0) {

            LocalDateTime now = LocalDateTime.now();
            List<JobInstance> instances = new ArrayList<>();
            for (int i = 0; i <= interval; i++) {

                LocalDateTime scheduledTime = now.plusMinutes(i);
                JobInstance instance = new JobInstance(job, scheduledTime);
                instances.add(instance);
            }
            jobInstanceRepository.saveAll(instances);

        }

        return savedJob;
    }

    public void deleteJob(Long id) {

        jobRepository.deleteById(id);
        // supprimer aussi les instances liées
        jobInstanceRepository.deleteByJob_Id(id);
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
            // restaurer seulement si le temps prévu n'est pas passé
            if (!now.isAfter(instance.getScheduledTime()) &&
                    ("SKIPPED".equals(instance.getStatus()) || "ACTIVE".equals(instance.getStatus()))) {

                instance.setStatus("ACTIVE");
                jobInstanceRepository.save(instance);
            }
        });
    }

    public List<JobInstance> generateJobInstances(Long jobId, LocalDateTime start, LocalDateTime end, int intervalMinutes) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));

        // vérifier s’il existe déjà des instances dans cet intervalle pour éviter les doublons
        List<JobInstance> existing = jobInstanceRepository.findByJob_IdAndScheduledTimeBetween(jobId, start, end);
        if (!existing.isEmpty()) return existing;

        List<JobInstance> newInstances = new ArrayList<>();
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            JobInstance instance = new JobInstance(job, current);
            newInstances.add(instance);
            switch (job.getFrequency().toUpperCase()) {

                case "MINUTES":
                    current = current.plusMinutes(intervalMinutes);
                    break;

                case "HOURS":
                    current = current.plusHours(intervalMinutes);
                    break;

                case "DAYS":
                    current = current.plusDays(intervalMinutes);
                    break;

                default:
                    throw new RuntimeException("Unknown frequency");
            }
        }

        jobInstanceRepository.saveAll(newInstances);
        return jobInstanceRepository.findByJob_IdAndScheduledTimeBetween(jobId, start, end);
    }


}
