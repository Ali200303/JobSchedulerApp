package com.example.repository;

import com.example.model.JobInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobInstanceRepository extends JpaRepository<JobInstance, Long> {

    List<JobInstance> findByJob_IdAndScheduledTimeBetween(Long jobId, LocalDateTime start, LocalDateTime end);

    List<JobInstance> findByJob_IdOrderByScheduledTimeAsc(Long jobId);

    void deleteByJob_Id(Long jobId);

    void deleteByJob_IdAndScheduledTimeAfter(
            Long jobId,
            LocalDateTime scheduledTime
    );

}
