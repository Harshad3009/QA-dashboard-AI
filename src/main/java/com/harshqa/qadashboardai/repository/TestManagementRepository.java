package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestManagementRepository extends JpaRepository<TestManagement, Long> {
    Optional<TestManagement> findByClassNameAndTestName(String className, String testName);
}