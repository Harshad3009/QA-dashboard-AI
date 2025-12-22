package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // Magic Method! Spring automatically writes the SQL for this:
    // SELECT * FROM test_runs ORDER BY execution_date DESC LIMIT 10
    List<TestRun> findTop10ByOrderByExecutionDateDesc();
}