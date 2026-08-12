package com.jstudy.inout.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiInsightResponse {

    private String report;
    private String generatedAt;
    private String model;
}
