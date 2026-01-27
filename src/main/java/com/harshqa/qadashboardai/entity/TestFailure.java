package com.harshqa.qadashboardai.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "test_failures")
public class TestFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // We use a SHA-256 Hash to quickly check "Have we seen this error before?"
    // Indexing a 5000-char stack trace is slow; indexing a 64-char hash is fast.
    @Column(unique = true, nullable = false)
    private String failureHash;

    @Column(columnDefinition = "TEXT") // Use Postgres TEXT type
    private String message;

    @Column(columnDefinition = "TEXT") // Use Postgres TEXT type
    private String stackTrace;
}