package com.contrast.demo.repository;

import com.contrast.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    User findByUsername(String username);
    
    // Deliberately vulnerable to SQL injection for testing
    @Query(value = "SELECT * FROM users WHERE username = '" + ":username" + "' AND password = '" + ":password" + "'", nativeQuery = true)
    User findByUsernameAndPasswordVulnerable(@Param("username") String username, @Param("password") String password);
}
