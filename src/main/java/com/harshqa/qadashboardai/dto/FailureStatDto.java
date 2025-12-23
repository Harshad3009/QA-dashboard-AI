package com.harshqa.qadashboardai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailureStatDto {
    private Long failureId;
    private String errorMessage; // The short message
    private String hash;         // The unique ID
    private Long count;          // How many times it happened
}