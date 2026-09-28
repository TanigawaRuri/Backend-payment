package com.tanigawa.rewardplatform.reward.repository;

import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RewardEventRepositoryTest {
    @Autowired
    private RewardEventRepository rewardEventRepository;

    @Test
    void findByName_returnsMatchingEvent() {
        rewardEventRepository.save(
            RewardEvent.builder()
                .name("SIGNUP")
                .description("d")
                .rewardAmount(100L)
                .enabled(true)
                .quantityType(RewardQuantityType.UNLIMITED)
                .build()
        );

        assertThat(rewardEventRepository.findByName("SIGNUP")).isPresent();
    }
}