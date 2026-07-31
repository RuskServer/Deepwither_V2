package com.ruskserver.deepwither_V2.modules.crafting;

import java.util.UUID;

public class CraftingJob {

    private UUID jobId;
    private String recipeId;
    private String resultItemId;
    private int resultAmount;
    private long completionTimeMillis;
    private long professionExperience;

    public CraftingJob() {
    }

    public CraftingJob(
            UUID jobId,
            String recipeId,
            String resultItemId,
            int resultAmount,
            long completionTimeMillis,
            long professionExperience) {
        this.jobId = jobId;
        this.recipeId = recipeId;
        this.resultItemId = resultItemId;
        this.resultAmount = resultAmount;
        this.completionTimeMillis = completionTimeMillis;
        this.professionExperience = professionExperience;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public String getResultItemId() {
        return resultItemId;
    }

    public int getResultAmount() {
        return resultAmount;
    }

    public long getCompletionTimeMillis() {
        return completionTimeMillis;
    }

    public long getProfessionExperience() {
        return professionExperience;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() >= completionTimeMillis;
    }
}
