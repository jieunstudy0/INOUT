package com.jstudy.inout.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.payment.entity.ChargeRequest;
import com.jstudy.inout.payment.entity.ChargeStatus;
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.entity.DepositHistory;
import com.jstudy.inout.payment.entity.TransactionType;
import com.jstudy.inout.payment.repository.ChargeRequestRepository;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 예치금 충전 승인 중복 회귀 테스트.
 *
 * 예전 코드는 충전 요청을 잠그지 않고 "대기 중인지"만 확인했다. 두 관리자가(또는 버튼을 두 번 눌러)
 * 같은 요청을 동시에 승인하면 둘 다 대기 중으로 보고 각각 충전해 돈이 두 번 들어갔다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChargeApproveConcurrencyTest {

    private static final long INITIAL_BALANCE = 10_000L;
    private static final long CHARGE_AMOUNT = 50_000L;
    private static final int WORKERS = 2;

    @MockBean private MailComponent mailComponent;

    @Autowired private ChargeService chargeService;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DepositAccountRepository depositAccountRepository;
    @Autowired private DepositHistoryRepository depositHistoryRepository;
    @Autowired private ChargeRequestRepository chargeRequestRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;
    private Store store;
    private User admin;
    private User owner;
    private DepositAccount account;
    private ChargeRequest charge;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactionManager);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        tx.executeWithoutResult(s -> {
            store = storeRepository.save(Store.builder().name("충전매장-" + suffix).address("서울시 " + suffix).build());
            admin = userRepository.save(user("admin-" + suffix, "관리자"));
            owner = userRepository.save(user("owner-" + suffix, "점주"));
            account = depositAccountRepository.save(DepositAccount.builder().store(store).balance(INITIAL_BALANCE).build());
            charge = chargeRequestRepository.save(ChargeRequest.builder().requestUser(owner).amount(CHARGE_AMOUNT).build());
        });
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(s -> depositHistoryRepository.deleteAll(histories()));
        tx.executeWithoutResult(s -> {
            chargeRequestRepository.deleteById(charge.getId());
            depositAccountRepository.deleteById(account.getId());
            userRepository.deleteAllById(List.of(admin.getId(), owner.getId()));
            storeRepository.delete(store);
        });
    }

    @Test
    @DisplayName("같은 충전 요청을 동시에 두 번 승인해도 한 번만 충전된다")
    void concurrentApprove_chargesOnce() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < WORKERS; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    chargeService.approveCharge(admin.getId(), charge.getId());
                    return "OK";
                } catch (InoutException e) {
                    return e.getResultCode();
                }
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) outcomes.add(f.get());
        assertThat(outcomes).containsExactlyInAnyOrder("OK", "ALREADY_PROCESSED");

        tx.executeWithoutResult(s -> {
            assertThat(depositAccountRepository.findById(account.getId()).orElseThrow().getBalance())
                    .as("충전은 한 번만 반영돼야 한다").isEqualTo(INITIAL_BALANCE + CHARGE_AMOUNT);
            assertThat(histories()).extracting(DepositHistory::getType).containsExactly(TransactionType.CHARGE);
            assertThat(chargeRequestRepository.findById(charge.getId()).orElseThrow().getStatus())
                    .isEqualTo(ChargeStatus.APPROVED);
        });
    }

    private List<DepositHistory> histories() {
        return depositHistoryRepository
                .findByDepositAccountIdOrderByCreatedAtDesc(account.getId(), Pageable.unpaged()).getContent();
    }

    private User user(String emailPrefix, String name) {
        return User.builder()
                .email(emailPrefix + "@inout-test.com")
                .password("encoded")
                .name(name)
                .phone("010-1111-2222")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .build();
    }
}
