package com.harshqa.qadashboardai.repository;

import com.harshqa.qadashboardai.entity.ApiKey;
import com.harshqa.qadashboardai.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findBySecretKey(String secretKey);
    List<ApiKey> findByProject(Project project);
}