package com.jstudy.inout.common.massdata.dto;

import java.util.List;

public record MassDataGenerationResponse(
        int scale,
        long totalElapsedMs,
        long totalInserted,
        List<MassDataStepResult> steps) {

    public static MassDataGenerationResponse of(int scale, long totalElapsedMs, List<MassDataStepResult> steps) {
        long total = steps.stream().mapToLong(MassDataStepResult::insertedCount).sum();
        return new MassDataGenerationResponse(scale, totalElapsedMs, total, steps);
    }
}
