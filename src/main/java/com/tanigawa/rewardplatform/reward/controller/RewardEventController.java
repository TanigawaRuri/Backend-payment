package com.tanigawa.rewardplatform.reward.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tanigawa.rewardplatform.reward.dto.request.RewardEventRequest;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.dto.response.RewardEventResponse;
import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
import com.tanigawa.rewardplatform.reward.service.RewardEventService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reward-events")
@Tag(name = "Rewards", description = "Reward event management endpoints")
public class RewardEventController {

    private final RewardEventService rewardEventService;

    @GetMapping
    public List<RewardEventResponse> findAllEvents() {
        return rewardEventService.findAllEvents();
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RewardEventResponse createEvent(
        @Valid @RequestBody RewardEventRequest request
    ) {
        return rewardEventService.createEvent(request);
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public RewardHistoryResponse claimReward(
        @AuthenticationPrincipal Long userId,
        @Valid @RequestBody RewardHistoryRequest request
    ) {
        return rewardEventService.claimReward(userId, request);
    }
}