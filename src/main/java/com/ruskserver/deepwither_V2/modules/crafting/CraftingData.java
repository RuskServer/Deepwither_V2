package com.ruskserver.deepwither_V2.modules.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CraftingData {

    private List<CraftingJob> jobs = new ArrayList<>();

    public List<CraftingJob> getJobs() {
        ensureJobs();
        return Collections.unmodifiableList(jobs);
    }

    public void addJob(CraftingJob job) {
        ensureJobs();
        jobs.add(job);
    }

    public boolean removeJob(UUID jobId) {
        ensureJobs();
        return jobs.removeIf(job -> jobId.equals(job.getJobId()));
    }

    private void ensureJobs() {
        if (jobs == null) {
            jobs = new ArrayList<>();
        }
    }
}
