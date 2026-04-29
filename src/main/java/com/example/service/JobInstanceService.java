package com.example.service;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.repository.JobInstanceRepository;
import com.example.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobInstanceService {

    private final JobInstanceRepository repository;
    private final JobRepository jobRepository;

    public JobInstanceService(JobInstanceRepository repository, JobRepository jobRepository) {
        this.repository = repository;
        this.jobRepository = jobRepository;
    }

    /**
     * Récupère toutes les instances d'un job et met à jour leur statut
     */
    public List<JobInstance> getInstancesForJob(Long jobId) {
        List<JobInstance> instances = repository.findByJob_IdOrderByScheduledTimeAsc(jobId);
        updateExpiredInstances(instances);
        return instances;
    }

    /**
     * Marque une instance comme SKIPPED
     */
    public void skipInstance(Long instanceId) {
        updateInstanceStatus(instanceId, "ACTIVE", "SKIPPED");
    }

    /**
     * Restaure une instance SKIPPED vers ACTIVE
     */
    public void restoreInstance(Long instanceId) {
        updateInstanceStatus(instanceId, "SKIPPED", "ACTIVE");
    }

    /**
     * Supprime une instance
     */
    public void deleteInstance(Long instanceId) {
        repository.deleteById(instanceId);
    }

    /**
     * Supprime plusieurs instances
     */
    public void deleteInstances(List<Long> instanceIds) {
        repository.deleteAllById(instanceIds);
    }

    /**
     * Récupère un job par son ID
     */
    public Job getJobById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Met à jour le statut d'une instance (logique commune pour skip/restore)
     */
    private void updateInstanceStatus(Long instanceId, String expectedStatus, String newStatus) {
        repository.findById(instanceId).ifPresent(instance -> {
            LocalDateTime now = LocalDateTime.now();

            // Vérifier que le statut actuel est celui attendu
            if (expectedStatus.equals(instance.getStatus())) {
                // Vérifier que l'instance n'est pas expirée
                if (!now.isAfter(instance.getScheduledTime())) {
                    instance.setStatus(newStatus);
                    repository.save(instance);
                }
            }
        });
    }

    /**
     * Met à jour les instances expirées (ACTIVE → DONE)
     */
    private void updateExpiredInstances(List<JobInstance> instances) {
        LocalDateTime now = LocalDateTime.now();

        instances.stream()
                .filter(instance -> "ACTIVE".equals(instance.getStatus()))
                .filter(instance -> !instance.getScheduledTime().isAfter(now))
                .forEach(instance -> {
                    instance.setStatus("DONE");
                    repository.save(instance);
                });
    }
}