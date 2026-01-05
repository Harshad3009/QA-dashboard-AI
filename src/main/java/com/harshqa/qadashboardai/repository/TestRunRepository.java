package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // Magic Method! Spring automatically writes the SQL for this:
    // SELECT * FROM test_runs ORDER BY execution_date DESC LIMIT 10
    List<TestRun> findTop10ByOrderByExecutionDateDesc();

    List<TestRun> findTop5ByOrderByExecutionDateDesc();

    // Test runs history page (Filter by Date + Sort Newest First)
    // Adding "OrderByExecutionDateDesc" to ensure the UI shows the latest runs at the top.
    List<TestRun> findAllByExecutionDateAfterOrderByExecutionDateDesc(LocalDateTime date);

    // Find all runs after a specific date (e.g., 7 days ago)
    List<TestRun> findAllByExecutionDateAfter(LocalDateTime date);
}