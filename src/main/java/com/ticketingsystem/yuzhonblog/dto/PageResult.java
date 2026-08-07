package com.ticketingsystem.yuzhonblog.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number; // current page (0-based)
    private int size;

    public static <T> PageResult<T> of(org.springframework.data.domain.Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setContent(page.getContent());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setNumber(page.getNumber());
        result.setSize(page.getSize());
        return result;
    }
}
