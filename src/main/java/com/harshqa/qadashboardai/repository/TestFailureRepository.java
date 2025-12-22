package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.TestFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestFailureRepository extends JpaRepository<TestFailure, Long> {
    // Fast lookup by fingerprint
    Optional<TestFailure> findByFailureHash(String failureHash);
}
