package com.example.controller;

import com.example.model.JobInstance;
import com.example.service.JobInstanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/jobs")
public class JobPlanningController {

    private final JobInstanceService jobInstanceService;

    public JobPlanningController(JobInstanceService jobInstanceService) {

        this.jobInstanceService = jobInstanceService;
    }

    // affichage du planning pour un job spécifique
    @GetMapping("/{jobId}/planning")
    public String showJobPlanning(@PathVariable Long jobId, Model model) {

        List<JobInstance> instances = jobInstanceService.getInstancesForJob(jobId);

        String jobName = jobInstanceService.getJobName(jobId);

        model.addAttribute("jobId", jobId);
        model.addAttribute("jobName", jobName);
        model.addAttribute("instances", instances);
        return "job-planning";
    }



    // marquer une instance comme SKIPPED
    @PostMapping("/skip/{id}")
    public String skipInstance(@PathVariable Long id, @RequestParam Long jobId) {

        jobInstanceService.skipInstance(id);
        return "redirect:/jobs/" + jobId + "/planning";
    }

    // restaurer une instance
    @PostMapping("/restore/{id}")
    public String restoreInstance(@PathVariable Long id, @RequestParam Long jobId) {

        jobInstanceService.restoreInstance(id);
        return "redirect:/jobs/" + jobId + "/planning";
    }
}
