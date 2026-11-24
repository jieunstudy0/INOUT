package com.jstudy.inout.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.common.mail.config.MailComponent;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.notification.repository.NotificationRepository;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.request.dto.ChangeRequestDtos.CreateRequest;
import com.jstudy.inout.request.entity.DeliveryIssueType;
import com.jstudy.inout.request.dto.ChangeRequestDtos.Response;
import com.jstudy.inout.request.dto.ChangeRequestDtos.StatusRequest;
import com.jstudy.inout.request.entity.ChangeRequestCategory;
import com.jstudy.inout.request.entity.ChangeRequestRefType;
import com.jstudy.inout.request.entity.ChangeRequestStatus;
import com.jstudy.inout.request.repository.ChangeRequestRepository;
import com.jstudy.inout.request.service.ActorRole;
import com.jstudy.inout.request.service.ChangeRequestService;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
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
 * 수정 요청: 받는 쪽은 종류로 정해지고, 대상은 소속 매장 것만, 처리는 정해진 사람만 할 수 있다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChangeRequestServiceTest {

    @MockBean private MailComponent mailComponent;

    @Autowired private ChangeRequestService service;
    @Autowired private ChangeRequestRepository requestRepository;
    @Autowired private NotificationRepository notificationRepository;
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
    private User otherEmployee;
    private User admin;
    private StoreStock stock;
    private StoreStock otherStoresStock;
    private final List<Long> requestIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        f = new StoreStockFixture(tm, storeRepository, userRepository, categoryRepository, itemRepository,
                storeStockRepository, historyRepository, sheetRepository, orderRepository, orderDetailRepository,
                deliveryRepository);
        Store store = f.store("요청매장");
        Store otherStore = f.store("요청다른매장");
        owner = f.user(store, "req-owner");
        employee = f.user(store, "req-emp");
        otherOwner = f.user(otherStore, "req-other-owner");
        otherEmployee = f.user(otherStore, "req-other-emp");
        admin = f.user(null, "req-admin");
        stock = f.storeStock(store, f.item("요청상품", 10), 5, 1, null);
        otherStoresStock = f.storeStock(otherStore, f.item("남의상품", 10), 5, 1, null);
    }

    @AfterEach
    void tearDown() {
        f.tx.executeWithoutResult(t -> {
            requestRepository.deleteAllById(requestIds);
            for (User u : List.of(owner, employee, otherOwner, otherEmployee, admin)) {
                notificationRepository.deleteAll(notificationRepository.findAllByRecipientId(u.getId()));
            }
        });
        f.cleanUp();
    }

    @Test
    @DisplayName("재고 정정 요청은 같은 매장 점주의 받은 요청함에만 들어가고, 다른 매장 점주에게는 보이지 않는다")
    void stockCorrection_goesToOwnStoreOwner() {
        Long id = create(employee, ActorRole.EMPLOYEE, ChangeRequestCategory.STOCK_CORRECTION,
                ChangeRequestRefType.STORE_STOCK, stock.getId());

        assertThat(service.getInbox(owner.getId(), ActorRole.OWNER, null, 0).getContent())
                .extracting(Response::getRequestId).contains(id);
        assertThat(service.getInbox(otherOwner.getId(), ActorRole.OWNER, null, 0).getContent())
                .extracting(Response::getRequestId).doesNotContain(id);
        Response detail = service.getDetail(owner.getId(), ActorRole.OWNER, id);
        assertThat(detail.getHandler()).isEqualTo(ChangeRequestCategory.Handler.OWNER);
        assertThat(detail.getRefLabel()).contains("요청상품");
        assertThat(detail.isCanHandle()).isTrue();
    }

    @Test
    @DisplayName("다른 매장의 재고를 대상으로 요청하면 CROSS_STORE_FORBIDDEN이고 저장되지 않는다")
    void otherStoresTarget_forbidden() {
        long before = requestRepository.count();

        assertThatThrownBy(() -> create(employee, ActorRole.EMPLOYEE, ChangeRequestCategory.STOCK_CORRECTION,
                ChangeRequestRefType.STORE_STOCK, otherStoresStock.getId()))
                .isInstanceOf(InoutException.class)
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
        assertThat(requestRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("점주 처리 요청은 다른 매장 점주·직원·본사가 처리할 수 없다")
    void ownerRequest_onlyOwnStoreOwnerHandles() {
        Long id = create(employee, ActorRole.EMPLOYEE, ChangeRequestCategory.ORDER_CHANGE, ChangeRequestRefType.NONE, null);
        StatusRequest done = new StatusRequest(ChangeRequestStatus.DONE, "수정했습니다");

        assertThatThrownBy(() -> service.changeStatus(otherOwner.getId(), ActorRole.OWNER, id, done))
                .extracting("code").isEqualTo(ErrorCode.CROSS_STORE_FORBIDDEN);
        assertThatThrownBy(() -> service.changeStatus(employee.getId(), ActorRole.EMPLOYEE, id, done))
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> service.changeStatus(admin.getId(), ActorRole.ADMIN, id, done))
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);

        service.changeStatus(owner.getId(), ActorRole.OWNER, id, done);
        assertThat(service.getDetail(employee.getId(), ActorRole.EMPLOYEE, id).getStatus()).isEqualTo(ChangeRequestStatus.DONE);
    }

    @Test
    @DisplayName("반려는 사유가 필요하고, 끝난 요청은 다시 바꿀 수 없다")
    void reject_needsReason_andFinishedIsFinal() {
        Long id = create(employee, ActorRole.EMPLOYEE, ChangeRequestCategory.LEAVE_CHANGE, ChangeRequestRefType.NONE, null);

        assertThatThrownBy(() -> service.changeStatus(owner.getId(), ActorRole.OWNER, id,
                new StatusRequest(ChangeRequestStatus.REJECTED, "  ")))
                .extracting("code").isEqualTo(ErrorCode.REJECT_REASON_REQUIRED);

        service.changeStatus(owner.getId(), ActorRole.OWNER, id, new StatusRequest(ChangeRequestStatus.REJECTED, "그날은 인원이 부족합니다"));
        assertThatThrownBy(() -> service.changeStatus(owner.getId(), ActorRole.OWNER, id,
                new StatusRequest(ChangeRequestStatus.DONE, null)))
                .extracting("code").isEqualTo(ErrorCode.ALREADY_PROCESSED);
    }

    @Test
    @DisplayName("배송 이상 신고는 본사가 처리하고, 처리 결과가 요청한 직원에게 알림으로 간다")
    void deliveryIssue_handledByHq_notifiesRequester() {
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(f.item("배송상품", 10)), List.of(3));
        Long id = create(employee, ActorRole.EMPLOYEE, ChangeRequestCategory.DELIVERY_ISSUE, ChangeRequestRefType.ORDER, order.getId());

        assertThat(service.getInbox(admin.getId(), ActorRole.ADMIN, ChangeRequestStatus.REQUESTED, 0).getContent())
                .extracting(Response::getRequestId).contains(id);
        assertThatThrownBy(() -> service.changeStatus(owner.getId(), ActorRole.OWNER, id,
                new StatusRequest(ChangeRequestStatus.IN_PROGRESS, null)))
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);

        service.changeStatus(admin.getId(), ActorRole.ADMIN, id, new StatusRequest(ChangeRequestStatus.IN_PROGRESS, "재발송 준비 중"));

        assertThat(com.jstudy.inout.notification.NotificationAwait.inbox(notificationRepository, employee.getId(), 1))
                .anySatisfy(n -> assertThat(n.getLinkUrl()).isEqualTo("/emp/requests"));
    }

    @Test
    @DisplayName("배송 이상은 세부 종류(파손 등)를 저장하고, 고르지 않으면 '기타'로 받는다")
    void deliveryIssue_keepsIssueType_defaultsToOther() {
        OrderRequest order = f.shippedOrder(employee, DeliveryStatus.SHIPPING, List.of(f.item("파손상품", 10)), List.of(2));
        Long damaged = createWithIssue(ChangeRequestCategory.DELIVERY_ISSUE, ChangeRequestRefType.ORDER, order.getId(),
                DeliveryIssueType.DAMAGED);
        Long untyped = createWithIssue(ChangeRequestCategory.DELIVERY_ISSUE, ChangeRequestRefType.ORDER, order.getId(), null);

        assertThat(service.getInbox(admin.getId(), ActorRole.ADMIN, ChangeRequestStatus.REQUESTED, 0).getContent())
                .filteredOn(r -> r.getRequestId().equals(damaged) || r.getRequestId().equals(untyped))
                .extracting(Response::getRequestId, Response::getIssueType, Response::getIssueTypeLabel)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(damaged, DeliveryIssueType.DAMAGED, "파손"),
                        org.assertj.core.groups.Tuple.tuple(untyped, DeliveryIssueType.OTHER, "기타"));
    }

    @Test
    @DisplayName("배송 이상이 아닌 요청은 세부 종류를 보내도 저장하지 않는다")
    void otherCategory_ignoresIssueType() {
        Long id = createWithIssue(ChangeRequestCategory.OTHER, ChangeRequestRefType.NONE, null, DeliveryIssueType.WRONG_ITEM);

        assertThat(service.getInbox(owner.getId(), ActorRole.OWNER, ChangeRequestStatus.REQUESTED, 0).getContent())
                .filteredOn(r -> r.getRequestId().equals(id))
                .singleElement()
                .satisfies(r -> assertThat(r.getIssueType()).isNull());
    }

    @Test
    @DisplayName("점주는 매장 안 일(점주 처리 종류)로 요청을 만들 수 없다")
    void owner_cannotCreateOwnerHandledRequest() {
        assertThatThrownBy(() -> create(owner, ActorRole.OWNER, ChangeRequestCategory.STOCK_CORRECTION,
                ChangeRequestRefType.NONE, null))
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN);
    }

    private Long createWithIssue(ChangeRequestCategory category, ChangeRequestRefType refType, Long refId,
                                 DeliveryIssueType issueType) {
        Long id = service.create(employee.getId(), ActorRole.EMPLOYEE, CreateRequest.builder()
                .category(category).issueType(issueType).refType(refType).refId(refId)
                .title("테스트 요청").content("내용").build(), null);
        requestIds.add(id);
        return id;
    }

    private Long create(User user, ActorRole role, ChangeRequestCategory category, ChangeRequestRefType refType, Long refId) {
        Long id = service.create(user.getId(), role, CreateRequest.builder()
                .category(category).refType(refType).refId(refId)
                .title("테스트 요청").content("내용").build(), null);
        requestIds.add(id);
        return id;
    }
}
