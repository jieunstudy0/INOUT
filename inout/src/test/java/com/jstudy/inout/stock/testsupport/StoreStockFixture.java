package com.jstudy.inout.stock.testsupport;

import com.jstudy.inout.common.auth.entity.Store;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.StoreRepository;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.delivery.entity.Delivery;
import com.jstudy.inout.delivery.entity.DeliveryStatus;
import com.jstudy.inout.delivery.repository.DeliveryRepository;
import com.jstudy.inout.order.entity.OrderDetail;
import com.jstudy.inout.order.entity.OrderDetailStatus;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.entity.OrderStatus;
import com.jstudy.inout.order.repository.OrderDetailRepository;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.stock.entity.Item;
import com.jstudy.inout.stock.entity.ItemCategory;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.entity.StoreStockChangeType;
import com.jstudy.inout.stock.entity.StoreStockHistory;
import com.jstudy.inout.stock.repository.ItemCategoryRepository;
import com.jstudy.inout.stock.repository.ItemRepository;
import com.jstudy.inout.stock.repository.StockCountSheetRepository;
import com.jstudy.inout.stock.repository.StoreStockHistoryRepository;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 매장 재고 통합 테스트용 데이터 준비·정리.
 * 테스트마다 고유 접미사를 붙여 만들고, 만든 것만 지운다 (다른 테스트 데이터와 섞이지 않게).
 */
public class StoreStockFixture {

    public final TransactionTemplate tx;
    public final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ItemCategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final StoreStockRepository storeStockRepository;
    private final StoreStockHistoryRepository historyRepository;
    private final StockCountSheetRepository sheetRepository;
    private final OrderRequestRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final DeliveryRepository deliveryRepository;

    private final List<Store> stores = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<StoreStock> storeStocks = new ArrayList<>();
    private final List<OrderRequest> orders = new ArrayList<>();
    private final List<Delivery> deliveries = new ArrayList<>();
    public final List<Long> sheetIds = new ArrayList<>();
    private ItemCategory category;

    public StoreStockFixture(PlatformTransactionManager tm, StoreRepository storeRepository, UserRepository userRepository,
                             ItemCategoryRepository categoryRepository, ItemRepository itemRepository,
                             StoreStockRepository storeStockRepository, StoreStockHistoryRepository historyRepository,
                             StockCountSheetRepository sheetRepository, OrderRequestRepository orderRepository,
                             OrderDetailRepository orderDetailRepository, DeliveryRepository deliveryRepository) {
        this.tx = new TransactionTemplate(tm);
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.storeStockRepository = storeStockRepository;
        this.historyRepository = historyRepository;
        this.sheetRepository = sheetRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public Store store(String name) {
        Store s = tx.execute(t -> storeRepository.save(Store.builder()
                .name(name + "-" + suffix).address("서울시 " + name + suffix).build()));
        stores.add(s);
        return s;
    }

    public User user(Store store, String prefix) {
        User u = tx.execute(t -> userRepository.save(User.builder()
                .email(prefix + "-" + suffix + "@inout-test.com")
                .password("encoded")
                .name(prefix)
                .phone("010-1111-2222")
                .birthday(LocalDate.of(1990, 1, 1))
                .store(store)
                .build()));
        users.add(u);
        return u;
    }

    public Item item(String name, int hqStock) {
        if (category == null) {
            category = tx.execute(t -> categoryRepository.save(ItemCategory.builder()
                    .categoryName("매장재고카테고리-" + suffix).build()));
        }
        Item i = tx.execute(t -> itemRepository.save(Item.builder()
                .name("매장재고-" + name + "-" + suffix)
                .category(category)
                .unitPrice(1_000L)
                .currentStock(hqStock)
                .minStockLevel(0)
                .deleted(false)
                .build()));
        items.add(i);
        return i;
    }

    /** 매장 재고 행을 만들고 수량·안전재고·위치를 맞춘다 (점주 조정을 거치지 않은 준비 데이터). */
    public StoreStock storeStock(Store store, Item item, int qty, int min, String location) {
        StoreStock s = tx.execute(t -> {
            StoreStock created = StoreStock.create(storeRepository.findById(store.getId()).orElseThrow(),
                    itemRepository.findById(item.getItemId()).orElseThrow());
            created.changeSettings(min, location);
            if (qty > 0) {
                created.increase(qty);
            }
            return storeStockRepository.save(created);
        });
        storeStocks.add(s);
        return s;
    }

    public void track(StoreStock stock) {
        storeStocks.add(stock);
    }

    /** 사용 기록을 하나 남긴다 (실사표 '최근 사용' 대상 만들기용). */
    public void useRecord(StoreStock stock, User user) {
        tx.executeWithoutResult(t -> {
            StoreStock loaded = storeStockRepository.findById(stock.getId()).orElseThrow();
            historyRepository.save(StoreStockHistory.builder()
                    .storeStock(loaded).user(userRepository.findById(user.getId()).orElseThrow())
                    .type(StoreStockChangeType.USE).changeQty(-1).memo("테스트").build());
        });
    }

    public void setQty(StoreStock stock, int qty) {
        tx.executeWithoutResult(t -> {
            StoreStock loaded = storeStockRepository.findById(stock.getId()).orElseThrow();
            int diff = qty - loaded.getQuantity();
            if (diff > 0) loaded.increase(diff);
            if (diff < 0) loaded.decrease(-diff);
        });
    }

    public int qtyOf(StoreStock stock) {
        return storeStockRepository.findById(stock.getId()).orElseThrow().getQuantity();
    }

    public int hqStockOf(Item item) {
        return itemRepository.findById(item.getItemId()).orElseThrow().getCurrentStock();
    }

    /** 본사 승인까지 끝난 발주와 배송을 만든다. quantities[i] = items[i]의 발주 수량 */
    public OrderRequest shippedOrder(User requester, DeliveryStatus status, List<Item> orderItems, List<Integer> quantities) {
        OrderRequest order = tx.execute(t -> {
            OrderRequest o = orderRepository.save(OrderRequest.builder()
                    .requestUser(userRepository.findById(requester.getId()).orElseThrow())
                    .status(OrderStatus.APPROVED)
                    .totalPrice(1_000L)
                    .requestDate(LocalDateTime.now())
                    .receiverName("수령인")
                    .receiverPhone("010-0000-0000")
                    .destinationAddress("서울시 테스트구 1번지")
                    .build());
            for (int i = 0; i < orderItems.size(); i++) {
                orderDetailRepository.save(OrderDetail.builder()
                        .orderRequest(o)
                        .item(itemRepository.findById(orderItems.get(i).getItemId()).orElseThrow())
                        .requestQuantity(quantities.get(i))
                        .itemPriceSnapshot(1_000L)
                        .status(OrderDetailStatus.APPROVED)
                        .build());
            }
            return o;
        });
        orders.add(order);
        Delivery delivery = tx.execute(t -> deliveryRepository.save(Delivery.builder()
                .orderRequest(orderRepository.findById(order.getId()).orElseThrow())
                .status(status)
                .receiverName("수령인")
                .receiverPhone("010-0000-0000")
                .destinationAddress("서울시 테스트구 1번지")
                .shippedAt(status == DeliveryStatus.READY ? null : LocalDateTime.now().minusDays(1))
                .deliveredAt(status == DeliveryStatus.COMPLETED ? LocalDateTime.now() : null)
                .build()));
        deliveries.add(delivery);
        return order;
    }

    public List<OrderDetail> detailsOf(OrderRequest order) {
        return orderDetailRepository.findAll().stream()
                .filter(d -> d.getOrderRequest().getId().equals(order.getId()))
                .sorted((a, b) -> a.getOrderDetailId().compareTo(b.getOrderDetailId()))
                .toList();
    }

    public Delivery deliveryOf(OrderRequest order) {
        return deliveryRepository.findByOrderRequest_Id(order.getId()).orElseThrow();
    }

    public void cleanUp() {
        tx.executeWithoutResult(t -> sheetRepository.deleteAllById(sheetIds));
        tx.executeWithoutResult(t -> {
            List<Long> stockIds = new ArrayList<>(storeStocks.stream().map(StoreStock::getId).toList());
            for (Store s : stores) {
                storeStockRepository.findAllByStoreWithItem(s.getId()).forEach(x -> stockIds.add(x.getId()));
            }
            for (Long id : stockIds.stream().distinct().toList()) {
                historyRepository.deleteAll(historyRepository.findAllByStoreStock_Id(id));
            }
            storeStockRepository.deleteAllById(stockIds.stream().distinct().toList());
        });
        tx.executeWithoutResult(t -> {
            deliveryRepository.deleteAllById(deliveries.stream().map(Delivery::getId).toList());
            for (OrderRequest o : orders) {
                orderDetailRepository.deleteAll(detailsOf(o));
            }
            orderRepository.deleteAllById(orders.stream().map(OrderRequest::getId).toList());
        });
        tx.executeWithoutResult(t -> {
            itemRepository.deleteAllById(items.stream().map(Item::getItemId).toList());
            if (category != null) {
                categoryRepository.deleteById(category.getCategoryId());
            }
            userRepository.deleteAllById(users.stream().map(User::getId).toList());
            storeRepository.deleteAll(stores);
        });
    }
}
