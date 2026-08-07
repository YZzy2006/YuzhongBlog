package com.ticketingsystem.yuzhonblog.dto.review;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewActionRequest {
    @Size(max = 500, message = "审核意见不能超过500字")
    private String comment;
}
