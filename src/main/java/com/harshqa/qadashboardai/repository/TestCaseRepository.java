package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Example: Find all flaky tests (we can add logic here later)
    List<TestCase> findByStatus(String status);

    // We select the Failure Entity and the Count
    @Query("SELECT tc.testFailure, COUNT(tc) FROM TestCase tc " +
            "WHERE tc.testFailure IS NOT NULL " +
            "GROUP BY tc.testFailure " +
            "ORDER BY COUNT(tc) DESC")
    List<Object[]> findTopFailures(Pageable pageable);
}
