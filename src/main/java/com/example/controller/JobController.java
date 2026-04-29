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
public class JobController {

    @Autowired
    private JobService jobService;
    @Autowired
    private JobInstanceService jobInstanceService;


    // vérifie si un utilisateur est connecté
    private boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") != null;
    }

    @GetMapping("/jobs")
    public String showJobsPage(HttpSession session, Model model) {
        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.initializeSampleData();
        model.addAttribute("jobs", jobService.getAllJobs());
        model.addAttribute("username", session.getAttribute("loggedInUser"));
        return "jobs";
    }

    @GetMapping("/jobs/create")
    public String showCreateJobPage(HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";
        return "create-job";
    }

    @PostMapping("/jobs/create")
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

        // appel de la méthode pour générer les instances selon l’intervalle
        jobService.generateJobInstances(job.getId(), start, end);

        return "redirect:/jobs";
    }



    @PostMapping("/jobs/{id}/delete")
    public String deleteJob(@PathVariable Long id, HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.deleteJob(id);
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/{id}/status")
    public String updateJobStatus(@PathVariable Long id,
                                  @RequestParam String status,
                                  HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.updateJobStatus(id, status);
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/{jobId}/skip/{instanceId}")
    public String skipJobInstance(@PathVariable Long jobId,
                                  @PathVariable Long instanceId,
                                  HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.skipInstance(instanceId);
        return "redirect:/jobs/" + jobId + "/planning";
    }

    @PostMapping("/jobs/{jobId}/restore/{instanceId}")
    public String restoreJobInstance(@PathVariable Long jobId,
                                     @PathVariable Long instanceId,
                                     HttpSession session) {
        if (!isLoggedIn(session)) return "redirect:/login";

        jobService.restoreInstance(instanceId);
        return "redirect:/jobs/" + jobId + "/planning";
    }
}
