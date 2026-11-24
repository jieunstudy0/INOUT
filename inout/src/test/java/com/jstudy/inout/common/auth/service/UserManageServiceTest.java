package com.jstudy.inout.common.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.dto.UserDetailDto.BasicInfoRequest;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import com.jstudy.inout.stock.testsupport.StoreStockFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/** 직원 상세·기본 정보(사진 포함) 수정: 점주는 소속 매장 직원만 보고 고칠 수 있다. */
@SpringBootTest
@ActiveProfiles("test")
class UserManageServiceTest {

    @MockBean private MailComponent mailComponent;

    @Autowired private UserManageService service;
    @Autowired private PlatformTransactionManager tm;
    @Autowired private StoreRepository storeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ItemCategoryRepository categoryRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private StoreStockRepository storeStockRepository;
    @Autowired private StoreStockHistoryRepository historyRepository;
    @Autowired private StockCountSheetRepository sheetRepository;
    @Autowired private OrderRequestRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private DeliveryRepository deliveryRepository;

    private StoreStockFixture f;
    private User owner;
    private User employee;
    private User otherOwner;

    @BeforeEach
    void setUp() {
        f = new StoreStockFixture(tm, storeRepository, userRepository, categoryRepository, itemRepository,
                storeStockRepository, historyRepository, sheetRepository, orderRepository, orderDetailRepository,
                deliveryRepository);
        Store store = f.store("사진매장");
        Store other = f.store("사진다른매장");
        owner = f.user(store, "photo-owner");
        employee = f.user(store, "photo-emp");
        otherOwner = f.user(other, "photo-other-owner");
    }

    @AfterEach
    void tearDown() {
        f.cleanUp();
    }

    @Test
    @DisplayName("점주가 소속 직원의 이름·연락처·사진을 바꾸면 상세 조회에 그대로 보인다")
    void ownerUpdatesOwnEmployee() {
        service.updateBasicInfoByOwner(owner.getId(), employee.getId(),
                new BasicInfoRequest("  새이름 ", "010-9999-0000", "/uploads/emp.jpg"));

        var detail = service.getDetailForOwner(owner.getId(), employee.getId());
        assertThat(detail.getName()).isEqualTo("새이름");
        assertThat(detail.getPhone()).isEqualTo("010-9999-0000");
        assertThat(detail.getProfileImageUrl()).isEqualTo("/uploads/emp.jpg");
    }

    @Test
    @DisplayName("다른 매장 점주는 직원을 보거나 고칠 수 없다 (CROSS_STORE_FORBIDDEN)")
    void otherStoreOwner_forbidden() {
        assertThatThrownBy(() -> service.getDetailForOwner(otherOwner.getId(), employee.getId()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
        assertThatThrownBy(() -> service.updateBasicInfoByOwner(otherOwner.getId(), employee.getId(),
                new BasicInfoRequest("x", "010", "/uploads/x.jpg")))
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);

        assertThat(userRepository.findById(employee.getId()).orElseThrow().getProfileImageUrl()).isNull();
    }
}
