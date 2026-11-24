package com.jstudy.inout.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.entity.Role;
import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.entity.UserRole;
import com.jstudy.inout.common.auth.repository.RoleRepository;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.auth.repository.UserRoleRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.notification.entity.Notification;
import com.jstudy.inout.notification.entity.NotificationType;
import com.jstudy.inout.notification.repository.NotificationRepository;
import com.jstudy.inout.notification.service.NotificationService;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.dto.count.StockCountSubmitRequest;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import com.jstudy.inout.stock.service.StockCountService;
import com.jstudy.inout.stock.testsupport.StoreStockFixture;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 알림은 업무가 커밋된 뒤에만, 정해진 사람에게만 저장된다.
 * 실사 제출을 예로 검사한다: 같은 매장 점주만 받고, 제출한 본인·다른 매장 점주는 받지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationFlowTest {

    @MockBean private MailComponent mailComponent;

    @Autowired private StockCountService stockCountService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
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
    private final List<UserRole> grants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        f = new StoreStockFixture(tm, storeRepository, userRepository, categoryRepository, itemRepository,
                storeStockRepository, historyRepository, sheetRepository, orderRepository, orderDetailRepository,
                deliveryRepository);
        Store store = f.store("알림매장");
        Store otherStore = f.store("알림다른매장");
        owner = grant(f.user(store, "noti-owner"), "ROLE_OWNER");
        employee = grant(f.user(store, "noti-emp"), "ROLE_EMPLOYEE");
        otherOwner = grant(f.user(otherStore, "noti-other-owner"), "ROLE_OWNER");
        f.storeStock(store, f.item("알림상품", 10), 1, 5, null);
    }

    @AfterEach
    void tearDown() {
        f.tx.executeWithoutResult(t -> {
            for (User u : List.of(owner, employee, otherOwner)) {
                notificationRepository.deleteAll(notificationRepository.findAllByRecipientId(u.getId()));
            }
            userRoleRepository.deleteAll(grants);
        });
        f.cleanUp();
    }

    @Test
    @DisplayName("실사를 제출하면 같은 매장 점주에게만 알림이 가고, 제출한 직원·다른 매장 점주는 받지 않는다")
    void submit_notifiesOwnStoreOwnerOnly() {
        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);

        submitAll(sheetId, 3);

        List<Notification> ownerInbox = NotificationAwait.inbox(notificationRepository, owner.getId(), 1);
        assertThat(ownerInbox).singleElement().satisfies(n -> {
            assertThat(n.getType()).isEqualTo(NotificationType.STOCK_COUNT_SUBMITTED);
            assertThat(n.getLinkUrl()).isEqualTo("/owner/stock-counts");
            assertThat(n.isRead()).isFalse();
        });
        assertThat(NotificationAwait.inboxAfterQuietPeriod(notificationRepository, employee.getId())).isEmpty();
        assertThat(notificationRepository.findAllByRecipientId(otherOwner.getId())).isEmpty();
    }

    @Test
    @DisplayName("업무가 실패해 롤백되면 알림도 저장되지 않는다")
    void failedSubmit_noNotification() {
        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);

        assertThatThrownBy(() -> submitAll(sheetId, null)).isInstanceOf(InoutException.class);

        assertThat(NotificationAwait.inboxAfterQuietPeriod(notificationRepository, owner.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 사람의 알림은 읽음 처리할 수 없고, 모두 읽음은 내 알림만 바꾼다")
    void markRead_ownOnly() {
        Long sheetId = stockCountService.createSheet(employee.getId());
        f.sheetIds.add(sheetId);
        submitAll(sheetId, 3);
        Long notificationId = NotificationAwait.inbox(notificationRepository, owner.getId(), 1).get(0).getId();

        assertThatThrownBy(() -> notificationService.markRead(employee.getId(), notificationId))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_OWNER);
        assertThat(notificationService.getUnreadCount(owner.getId())).isEqualTo(1);

        notificationService.markAllRead(owner.getId());
        assertThat(notificationService.getUnreadCount(owner.getId())).isZero();
    }

    private void submitAll(Long sheetId, Integer qty) {
        List<StockCountSubmitRequest.Line> lines = stockCountService.getSheet(employee.getId(), sheetId).getLines().stream()
                .map(l -> new StockCountSubmitRequest.Line(l.getLineId(), qty))
                .toList();
        stockCountService.submit(sheetId, employee.getId(), new StockCountSubmitRequest(lines));
    }

    private User grant(User user, String roleName) {
        f.tx.executeWithoutResult(t -> {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().roleName(roleName).build()));
            grants.add(userRoleRepository.save(UserRole.builder()
                    .user(userRepository.findById(user.getId()).orElseThrow())
                    .role(role)
                    .build()));
        });
        return user;
    }
}
