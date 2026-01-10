package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestCase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Example: Find all flaky tests (we can add logic here later)
    List<TestCase> findByStatus(String status);

    // We select the Failure Entity and the Count
    @Query("SELECT tc.testFailure, COUNT(tc) FROM TestCase tc " +
            "JOIN tc.testRun tr " +
            "WHERE tc.testFailure IS NOT NULL " +
            "AND tr.executionDate > :since " +
            "AND tr.project.id = :projectId " +
            "GROUP BY tc.testFailure " +
            "ORDER BY COUNT(tc) DESC")
    List<Object[]> findTopFailures(LocalDateTime since, @Param("projectId") Long projectId, Pageable pageable);

    /**
     * Finds tests that have > 1 distinct status (e.g., both PASSED and FAILED)
     * within the given timeframe.
     */
    @Query("SELECT tc.testName, tc.className, COUNT(tc), " +
            "SUM(CASE WHEN tc.status = 'FAILED' THEN 1 ELSE 0 END) " +
            "FROM TestCase tc " +
            "JOIN tc.testRun tr " +
            "WHERE tr.executionDate > :since " +
            "AND tr.project.id = :projectId " +
            "GROUP BY tc.testName, tc.className " +
            "HAVING COUNT(DISTINCT tc.status) > 1")
    List<Object[]> findFlakyTests(LocalDateTime since, @Param("projectId") Long projectId);

    // Count UNIQUE test cases that failed in the period
    @Query("SELECT COUNT(DISTINCT CONCAT(tc.className, '.', tc.testName)) " +
            "FROM TestCase tc JOIN tc.testRun tr " +
            "WHERE tr.executionDate > :since AND tc.status = 'FAILED'" +
            "AND tr.project.id = :projectId")
    Long countUniqueFailures(LocalDateTime since, @Param("projectId") Long projectId);
}
