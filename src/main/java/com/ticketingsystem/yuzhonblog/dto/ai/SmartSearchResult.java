package com.ticketingsystem.yuzhonblog.dto.ai;

import lombok.Data;

@Data
public class SmartSearchResult {
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private String sortBy; // "newest", "views", "likes"

    public boolean isEmpty() {
        return (keyword == null || keyword.isBlank())
                && categoryId == null
                && tagId == null
                && (sortBy == null || sortBy.isBlank());
    }
}
