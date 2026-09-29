package com.example.sports.repository;

import com.example.sports.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchRepository extends JpaRepository<MatchEntity, String> {
    List<MatchEntity> findByStatus(String status);
}