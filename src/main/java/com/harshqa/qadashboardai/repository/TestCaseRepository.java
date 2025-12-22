package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Example: Find all flaky tests (we can add logic here later)
    List<TestCase> findByStatus(String status);
}
