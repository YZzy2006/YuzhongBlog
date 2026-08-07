package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.PhoneBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhoneBindingRepository extends JpaRepository<PhoneBinding, Long> {
    Optional<PhoneBinding> findByPhone(String phone);
    Optional<PhoneBinding> findByUserId(Long userId);
    boolean existsByPhone(String phone);
}
