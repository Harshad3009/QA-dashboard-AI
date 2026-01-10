package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.Project;
import com.harshqa.qadashboardai.entity.TestRun;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // Dashboard Widget (Latest 5 for a Project)
    List<TestRun> findTop5ByProjectOrderByExecutionDateDesc(Project project);

    // History Page (Filter by Date for a Project)
    List<TestRun> findAllByProjectAndExecutionDateAfterOrderByExecutionDateDesc(Project project, LocalDateTime date);

    // Get All for a Project
    List<TestRun> findAllByProject(Project project, Sort sort);

    // Trend Calculation (Previous Period for a Project)
    List<TestRun> findAllByProjectAndExecutionDateBetween(Project project, LocalDateTime start, LocalDateTime end);

    // UPDATED: Find run by Date AND Project (for the merge logic)
    List<TestRun> findAllByExecutionDateBetweenAndProject(
            LocalDateTime start,
            LocalDateTime end,
            Project project
    );
}