package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobInstanceRepository;
import com.example.repository.JobRepository;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;

@Service
public class JobScheduler {

    private final JobInstanceRepository repository;
    private final JobRepository jobRepository;

    public JobScheduler(JobInstanceRepository repository, JobRepository jobRepository) {
        this.repository = repository;
        this.jobRepository = jobRepository;
    }

    public void initJobs() {
        // Vérifier si la DB est vide
        if (repository.count() == 0) {

            // Créer un job test si nécessaire
            Job job;
            if (jobRepository.count() == 0) {
                job = new Job("Job Test", "Job de test pour scheduler", "MINUTES", 5);
                jobRepository.save(job);
            } else {
                job = jobRepository.findAll().get(0); // prendre le premier job existant
            }

            LocalDateTime baseTime = LocalDateTime.now()
                    .withSecond(0)
                    .withNano(0);

            // Créer 6 instances pour tester
            for (int i = 0; i < 6; i++) {
                JobInstance instance = new JobInstance(job, baseTime.plusMinutes(i));
                instance.setStatus("ACTIVE");
                repository.save(instance);
            }

            System.out.println("6 jobs créés pour le job : " + job.getName());
        }
    }
}
