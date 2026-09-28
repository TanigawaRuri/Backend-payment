package com.tanigawa.rewardplatform.reward.controller;

import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
import com.tanigawa.rewardplatform.reward.service.RewardHistoryService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reward-histories")    
@Tag(name = "Reward History", description = "View reward claim history")
public class RewardHistoryController {
    
    private final RewardHistoryService rewardHistoryService;

    @GetMapping("/{id}")
    public RewardHistoryResponse getHistory(
        @PathVariable Long id
    ) {
        return rewardHistoryService.getHistory(id);
    }

    @GetMapping("/users")
    public List<RewardHistoryResponse> getUserHistories(
        @AuthenticationPrincipal Long userId
    ) {
        return rewardHistoryService.getUserHistories(userId);
    }
}