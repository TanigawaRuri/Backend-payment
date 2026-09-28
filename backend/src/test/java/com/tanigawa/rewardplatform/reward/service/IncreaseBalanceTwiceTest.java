package com.tanigawa.rewardplatform.reward.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tanigawa.rewardplatform.exception.WalletConflictException;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.entity.Wallet;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

@SpringBootTest
class IncreaseBalanceTwiceTest {
    @Autowired
    private RewardEventService rewardEventService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RewardEventRepository rewardEventRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private RewardEvent eventA;
    private RewardEvent eventB;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
            User.builder()
                .nickname("concurrency_tester")
                .email("concurrency-test-" + UUID.randomUUID() + "@test.com")
                .encodedPassword("password")
                .build()
        );

        walletRepository.save(new Wallet(user));

        eventA = rewardEventRepository.save(
            RewardEvent.builder()
                .name("CONCURRENT_TEST_A_" + UUID.randomUUID())
                .description("Concurrency test event A")
                .rewardAmount(100L)
                .enabled(true)
                .quantityType(RewardQuantityType.UNLIMITED)
                .build()
        );

        eventB = rewardEventRepository.save(
            RewardEvent.builder()
                .name("CONCURRENT_TEST_B_" + UUID.randomUUID())
                .description("Concurrency test event B")
                .rewardAmount(200L)
                .enabled(true)
                .quantityType(RewardQuantityType.UNLIMITED)
                .build()
        );
    }

    @Test
    void concurrentClaimsShouldNotLoseUpdates() throws InterruptedException {
        Long userId = user.getId();
        Long eventIdA = eventA.getId();
        Long eventIdB = eventB.getId();

        RewardHistoryRequest rewardHistoryRequestA = new RewardHistoryRequest(eventIdA, UUID.randomUUID());
        RewardHistoryRequest rewardHistoryRequestB = new RewardHistoryRequest(eventIdB, UUID.randomUUID());

        Wallet walletBefore = walletRepository.findByUserId(userId).orElseThrow();

        long initialBalance = walletBefore.getBalance();

        long rewardAmountA = eventA.getRewardAmount();
        long rewardAmountB = eventB.getRewardAmount();

        int threadCount = 2;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        Runnable taskA = () -> {
            readyLatch.countDown();
            awaitStart(startLatch);
            try {
                rewardEventService.claimReward(userId, rewardHistoryRequestA);
                successCount.incrementAndGet();
            } catch (WalletConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable taskB = () -> {
            readyLatch.countDown();
            awaitStart(startLatch);
            try {
                rewardEventService.claimReward(userId, rewardHistoryRequestB);
                successCount.incrementAndGet();
            } catch (WalletConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        };

        try {
            executor.submit(taskA);
            executor.submit(taskB);

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
        } finally {
            executor.shutdown();
        }

        Wallet walletAfter = walletRepository.findByUserId(userId).orElseThrow();

        long finalBalance = walletAfter.getBalance();
        long expectedBalance = initialBalance + rewardAmountA + rewardAmountB;

        if (successCount.get() == 2) {
            // both succeeded — this is fine IF the balance reflects both
            assertEquals(expectedBalance, finalBalance,
                "If both claims succeeded, balance must reflect both rewards (no lost update)");
        } else if (successCount.get() == 1 && conflictCount.get() == 1) {
            // one succeeded, one conflicted and was NOT retried — also fine
            boolean matchesA = finalBalance == initialBalance + rewardAmountA;
            boolean matchesB = finalBalance == initialBalance + rewardAmountB;
            assertTrue(matchesA || matchesB, "Balance should reflect exactly the one successful claim");
        } else {
            fail("Unexpected outcome: successCount=" + successCount.get() + ", conflictCount=" + conflictCount.get());
        }
        
    }

    private void awaitStart(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}