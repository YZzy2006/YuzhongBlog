package com.ticketingsystem.yuzhonblog.repository;

import com.ticketingsystem.yuzhonblog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderBySortOrderDesc();
    boolean existsByName(String name);
}
