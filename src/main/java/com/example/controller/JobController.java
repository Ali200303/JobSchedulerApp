package com.example.controller;

import com.example.model.Job;
import com.example.model.JobInstance;
import com.example.service.JobInstanceService;
import com.example.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final JobInstanceService jobInstanceService;

    @Autowired
    public JobController(JobService jobService, JobInstanceService jobInstanceService) {

        this.jobService = jobService;
        this.jobInstanceService = jobInstanceService;
    }

    // ==================== PAGES ====================

    @GetMapping
    public String showJobsPage(HttpSession session, Model model) {

        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.initializeSampleData();
        model.addAttribute("jobs", jobService.getAllJobs());
        model.addAttribute("username", session.getAttribute("loggedInUser"));
        return "jobs";
    }

    @GetMapping("/create")
    public String showCreateJobPage(HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        return "create-job";
    }

    @GetMapping("/{id}/planning")
    public String showPlanning(@PathVariable Long id, Model model, HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";

        Job job = jobService.getJobById(id);
        List<JobInstance> instances = jobInstanceService.getInstancesForJob(id);

        model.addAttribute("job", job);
        model.addAttribute("jobId", id);
        model.addAttribute("jobName", job.getName());
        model.addAttribute("instances", instances);

        return "job-planning";
    }

    // ==================== JOB ACTIONS ====================

    @PostMapping("/create")
    public String createJob(@RequestParam String name,
                            @RequestParam String description,
                            @RequestParam String frequency,
                            @RequestParam Integer interval,
                            @RequestParam String startTime,
                            @RequestParam String endTime,
                            HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";

        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);

        Job job = jobService.createJob(name, description, frequency, interval);
        jobService.generateJobInstances(job.getId(), start, end);

        return "redirect:/jobs";
    }

    @PostMapping("/{id}/delete")
    public String deleteJob(@PathVariable Long id, HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        jobService.deleteJob(id);
        return "redirect:/jobs";
    }

    @PostMapping("/{id}/status")
    public String updateJobStatus(@PathVariable Long id,
                                  @RequestParam String status,
                                  HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        jobService.updateJobStatus(id, status);
        return "redirect:/jobs";
    }

    // ==================== INSTANCE ACTIONS ====================

    @PostMapping("/{jobId}/skip/{instanceId}")
    public String skipJobInstance(@PathVariable Long jobId,
                                  @PathVariable Long instanceId,
                                  HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        jobInstanceService.skipInstance(instanceId);
        return redirectToPlanning(jobId);
    }

    @PostMapping("/{jobId}/restore/{instanceId}")
    public String restoreJobInstance(@PathVariable Long jobId,
                                     @PathVariable Long instanceId,
                                     HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        jobInstanceService.restoreInstance(instanceId);
        return redirectToPlanning(jobId);
    }

    @PostMapping("/{jobId}/instances/{instanceId}/delete")
    public String deleteJobInstance(@PathVariable Long jobId,
                                    @PathVariable Long instanceId,
                                    HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";
        jobInstanceService.deleteInstance(instanceId);
        return redirectToPlanning(jobId);
    }

    @PostMapping("/{jobId}/instances/delete-selected")
    public String deleteSelectedInstances(@PathVariable Long jobId,
                                          @RequestParam(required = false) List<Long> instanceIds,
                                          HttpSession session) {

        if (!isLoggedIn(session)) return "redirect:/login";

        if (instanceIds != null && !instanceIds.isEmpty()) {

            jobInstanceService.deleteInstances(instanceIds);
        }

        return redirectToPlanning(jobId);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private boolean isLoggedIn(HttpSession session) {

        return session.getAttribute("loggedInUser") != null;
    }

    private String redirectToPlanning(Long jobId) {

        return "redirect:/jobs/" + jobId + "/planning";
    }
}