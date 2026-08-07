package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);

    Optional<UserPermission> findByUserIdAndPermission(Long userId, String permission);

    @Modifying
    @Query("DELETE FROM UserPermission up WHERE up.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.userId = :userId AND up.permission = :permission AND up.enabled = true")
    boolean isPermissionEnabled(@Param("userId") Long userId, @Param("permission") String permission);

    @Query("SELECT COUNT(up) > 0 FROM UserPermission up WHERE up.userId = :userId AND up.permission = :permission AND up.enabled = false")
    boolean isPermissionDisabled(@Param("userId") Long userId, @Param("permission") String permission);
}
