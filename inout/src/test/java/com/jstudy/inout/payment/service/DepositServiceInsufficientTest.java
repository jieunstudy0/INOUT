package com.jstudy.inout.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.payment.entity.DepositAccount;
import com.jstudy.inout.payment.repository.DepositAccountRepository;
import com.jstudy.inout.payment.repository.DepositHistoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepositServiceInsufficientTest {

    @InjectMocks
    private DepositService depositService;

    @Mock private DepositAccountRepository accountRepository;
    @Mock private DepositHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private StoreRepository storeRepository;

    @Test
    @DisplayName("예치금 차감 실패 - 잔액 부족 시 INSUFFICIENT_BALANCE(400)")
    void deductDeposit_fail_insufficientBalance() {
        // given
        Store store = Store.builder().id(10L).name("지점1").address("서울").build();
        User user = User.builder().id(1L).email("emp@test.com").name("직원").store(store).build();
        DepositAccount account = DepositAccount.builder().store(store).balance(1_000L).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(accountRepository.findByStoreIdForUpdate(10L)).willReturn(Optional.of(account));

        // when & then
        assertThatThrownBy(() -> depositService.deductDeposit(1L, 99L, 50_000L, "주문 결제", 100L))
                .isInstanceOf(InoutException.class)
                .extracting("resultCode")
                .isEqualTo("INSUFFICIENT_BALANCE");

        verify(historyRepository, Mockito.never()).save(Mockito.any());
    }
}
