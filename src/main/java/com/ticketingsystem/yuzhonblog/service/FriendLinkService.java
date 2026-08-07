package com.ticketingsystem.yuzhonblog.service;

import com.ticketingsystem.yuzhonblog.common.BusinessException;
import com.ticketingsystem.yuzhonblog.common.ErrorCode;
import com.ticketingsystem.yuzhonblog.dto.friendlink.FriendLinkRequest;
import com.ticketingsystem.yuzhonblog.dto.friendlink.FriendLinkResponse;
import com.ticketingsystem.yuzhonblog.entity.FriendLink;
import com.ticketingsystem.yuzhonblog.repository.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendLinkService {

    private final FriendLinkRepository friendLinkRepository;

    @Transactional(readOnly = true)
    public List<FriendLinkResponse> list() {
        return friendLinkRepository.findAllByOrderBySortOrderAscCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FriendLinkResponse getById(Long id) {
        FriendLink link = friendLinkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_LINK_NOT_FOUND));
        return toResponse(link);
    }

    @Transactional
    public FriendLinkResponse create(FriendLinkRequest request) {
        FriendLink link = new FriendLink();
        link.setName(request.getName());
        link.setUrl(request.getUrl());
        link.setDescription(request.getDescription());
        link.setAvatar(request.getAvatar());
        link.setThemeColor(request.getThemeColor());
        link.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        return toResponse(friendLinkRepository.save(link));
    }

    @Transactional
    public FriendLinkResponse update(Long id, FriendLinkRequest request) {
        FriendLink link = friendLinkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_LINK_NOT_FOUND));
        link.setName(request.getName());
        link.setUrl(request.getUrl());
        link.setDescription(request.getDescription());
        link.setAvatar(request.getAvatar());
        link.setThemeColor(request.getThemeColor());
        if (request.getSortOrder() != null) {
            link.setSortOrder(request.getSortOrder());
        }
        return toResponse(friendLinkRepository.save(link));
    }

    @Transactional
    public void delete(Long id) {
        FriendLink link = friendLinkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FRIEND_LINK_NOT_FOUND));
        friendLinkRepository.delete(link);
    }

    private FriendLinkResponse toResponse(FriendLink link) {
        FriendLinkResponse resp = new FriendLinkResponse();
        resp.setId(link.getId());
        resp.setName(link.getName());
        resp.setUrl(link.getUrl());
        resp.setDescription(link.getDescription());
        resp.setAvatar(link.getAvatar());
        resp.setThemeColor(link.getThemeColor());
        resp.setSortOrder(link.getSortOrder());
        resp.setCreatedAt(link.getCreatedAt());
        return resp;
    }
}
