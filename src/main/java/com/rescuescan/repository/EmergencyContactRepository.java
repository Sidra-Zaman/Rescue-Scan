package com.rescuescan.repository;

import com.rescuescan.model.EmergencyContact;
import com.rescuescan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {
    List<EmergencyContact> findByUser(User user);
    void deleteByUser(User user);
}
