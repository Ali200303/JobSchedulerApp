package com.example.repository;

import com.example.model.JobInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobInstanceRepository extends JpaRepository<JobInstance, Long> {

    // récupérer toutes les instances d'un job donné entre deux dates
    List<JobInstance> findByJob_IdAndScheduledTimeBetween(Long jobId, LocalDateTime start, LocalDateTime end);

    List<JobInstance> findByJob_IdOrderByScheduledTimeAsc(Long jobId);

    // supprimer toutes les instances d'un job
    void deleteByJob_Id(Long jobId);

}
