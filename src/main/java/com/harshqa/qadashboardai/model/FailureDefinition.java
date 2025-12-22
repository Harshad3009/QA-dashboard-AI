package com.harshqa.qadashboardai.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FailureDefinition {

    private String id;              // e.g., "ERR_1"
    private String message;         // e.g., "NullPointerException..."
    private String stackTrace;      // The full error text
    private int occurrenceCount;    // How many tests had this error?

}
