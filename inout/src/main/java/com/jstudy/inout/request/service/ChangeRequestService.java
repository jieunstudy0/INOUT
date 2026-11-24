package com.jstudy.inout.request.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.jstudy.inout.common.auth.entity.User;
import com.jstudy.inout.common.auth.repository.UserRepository;
import com.jstudy.inout.common.exception.ErrorCode;
import com.jstudy.inout.common.exception.InoutException;
import com.jstudy.inout.leave.entity.AnnualLeave;
import com.jstudy.inout.leave.repository.AnnualLeaveRepository;
import com.jstudy.inout.notification.entity.NotificationType;
import com.jstudy.inout.notification.service.NotificationEvent.Target;
import com.jstudy.inout.notification.service.Notifier;
import com.jstudy.inout.order.entity.OrderRequest;
import com.jstudy.inout.order.repository.OrderRequestRepository;
import com.jstudy.inout.request.dto.ChangeRequestDtos.CreateRequest;
import com.jstudy.inout.request.dto.ChangeRequestDtos.Response;
import com.jstudy.inout.request.dto.ChangeRequestDtos.StatusRequest;
import com.jstudy.inout.request.entity.ChangeRequest;
import com.jstudy.inout.request.entity.ChangeRequestCategory.Handler;
import com.jstudy.inout.request.entity.ChangeRequestRefType;
import com.jstudy.inout.request.entity.ChangeRequestStatus;
import com.jstudy.inout.request.repository.ChangeRequestRepository;
import com.jstudy.inout.stock.entity.StoreStock;
import com.jstudy.inout.stock.repository.StoreStockRepository;
import com.jstudy.inout.stock.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 현장 수정 요청.
 * - 받는 쪽(점주/본사)은 요청 종류로 서버가 정한다.
 * - 요청 대상(발주·매장 재고·연차)은 요청한 사람의 소속 매장 것이어야 한다.
 * - 처리는 상태만 바꾼다. 실제 데이터 변경은 처리하는 사람이 기존 기능으로 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChangeRequestService {

    private static final int PAGE_SIZE = 20;
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;

    private final ChangeRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final OrderRequestRepository orderRequestRepository;
    private final StoreStockRepository storeStockRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final ImageService imageService;
    private final Notifier notifier;

    @Transactional
    public Long create(Long userId, ActorRole role, CreateRequest req, MultipartFile photo) {
        User user = loadUser(userId);
        if (user.getStore() == null || role == ActorRole.ADMIN) {
            throw new InoutException(ErrorCode.STORE_REQUIRED, "매장 소속 직원·점주만 수정 요청을 남길 수 있습니다.");
        }
        if (role == ActorRole.OWNER && req.getCategory().getHandler() == Handler.OWNER) {
            throw new InoutException(ErrorCode.FORBIDDEN, "점주는 본사에 보내는 요청(배송 이상·상품 정보 오류)만 남길 수 있습니다. 매장 일은 직접 처리해 주세요.");
        }
        Long storeId = user.getStore().getId();
        ChangeRequestRefType refType = req.getRefType() == null ? ChangeRequestRefType.NONE : req.getRefType();
        String refLabel = resolveRefLabel(storeId, refType, req.getRefId());

        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            if (photo.getSize() > MAX_PHOTO_BYTES) {
                throw new InoutException(ErrorCode.FILE_TOO_LARGE);
            }
            photoUrl = imageService.uploadImage(photo);
        }

        ChangeRequest saved = requestRepository.save(ChangeRequest.builder()
                .store(user.getStore())
                .requester(user)
                .requestedByOwner(role == ActorRole.OWNER)
                .category(req.getCategory())
                .issueType(req.getIssueType())
                .refType(refType)
                .refId(refType == ChangeRequestRefType.NONE ? null : req.getRefId())
                .refLabel(refLabel)
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .photoUrl(photoUrl)
                .build());

        String kind = saved.getIssueType() == null
                ? req.getCategory().getLabel()
                : req.getCategory().getLabel() + " · " + saved.getIssueType().getLabel();
        String title = "[" + kind + "] " + user.getName() + " 님의 요청이 도착했습니다";
        Target target = saved.getHandler() == Handler.OWNER
                ? Target.storeOwners(storeId, "/owner/requests")
                : Target.admins("/admin/requests");
        notifier.send(NotificationType.CHANGE_REQUEST_CREATED, title, saved.getTitle(), userId, target);
        log.info("[수정 요청] 생성: id={}, storeId={}, category={}", saved.getId(), storeId, saved.getCategory());
        return saved.getId();
    }

    /** 대상이 요청한 사람의 매장 것인지 확인하고, 목록에 보여 줄 이름을 만든다. */
    private String resolveRefLabel(Long storeId, ChangeRequestRefType refType, Long refId) {
        if (refType == ChangeRequestRefType.NONE) {
            return null;
        }
        if (refId == null) {
            throw new InoutException(ErrorCode.INVALID_REQUEST_TARGET);
        }
        switch (refType) {
            case ORDER -> {
                OrderRequest order = orderRequestRepository.findById(refId)
                        .orElseThrow(() -> new InoutException(ErrorCode.INVALID_REQUEST_TARGET));
                User requester = order.getRequestUser();
                assertStore(storeId, requester.getStore() == null ? null : requester.getStore().getId());
                return "발주 #" + order.getId();
            }
            case STORE_STOCK -> {
                StoreStock stock = storeStockRepository.findById(refId)
                        .orElseThrow(() -> new InoutException(ErrorCode.INVALID_REQUEST_TARGET));
                assertStore(storeId, stock.getStore().getId());
                return stock.getItem().getName();
            }
            case LEAVE -> {
                AnnualLeave leave = annualLeaveRepository.findByIdWithUser(refId)
                        .orElseThrow(() -> new InoutException(ErrorCode.INVALID_REQUEST_TARGET));
                User owner = leave.getUser();
                assertStore(storeId, owner.getStore() == null ? null : owner.getStore().getId());
                return "연차 " + leave.getStartDate() + " ~ " + leave.getEndDate();
            }
            default -> {
                return null;
            }
        }
    }

    // ── 조회 ─────────────────────────────────────────────────────────

    public Page<Response> getMine(Long userId, int page) {
        return requestRepository.findMine(userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(r -> Response.from(r, false));
    }

    /** 받은 요청함: 점주는 소속 매장의 점주 처리 요청, 본사는 모든 매장의 본사 처리 요청 */
    public Page<Response> getInbox(Long userId, ActorRole role, ChangeRequestStatus status, int page) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        if (role == ActorRole.ADMIN) {
            return requestRepository.findHqInbox(Handler.HQ, status, pageable).map(r -> Response.from(r, true));
        }
        if (role == ActorRole.OWNER) {
            User owner = loadUser(userId);
            if (owner.getStore() == null) {
                throw new InoutException(ErrorCode.STORE_REQUIRED);
            }
            return requestRepository.findStoreInbox(owner.getStore().getId(), Handler.OWNER, status, pageable)
                    .map(r -> Response.from(r, true));
        }
        throw new InoutException(ErrorCode.FORBIDDEN);
    }

    public Response getDetail(Long userId, ActorRole role, Long requestId) {
        ChangeRequest r = requestRepository.findById(requestId)
                .orElseThrow(() -> new InoutException(ErrorCode.CHANGE_REQUEST_NOT_FOUND));
        User user = loadUser(userId);
        if (!canView(user, role, r)) {
            throw new InoutException(ErrorCode.FORBIDDEN);
        }
        return Response.from(r, canHandle(user, role, r));
    }

    // ── 처리 ─────────────────────────────────────────────────────────

    @Transactional
    public void changeStatus(Long userId, ActorRole role, Long requestId, StatusRequest req) {
        ChangeRequest r = requestRepository.findByIdWithLock(requestId)
                .orElseThrow(() -> new InoutException(ErrorCode.CHANGE_REQUEST_NOT_FOUND));
        User handler = loadUser(userId);
        if (!canHandle(handler, role, r)) {
            throw new InoutException(r.getHandler() == Handler.OWNER && role == ActorRole.OWNER
                    ? ErrorCode.CROSS_STORE_FORBIDDEN : ErrorCode.FORBIDDEN);
        }
        r.changeStatus(handler, req.getStatus(), req.getMessage());

        String link = r.isRequestedByOwner() ? "/owner/requests" : "/emp/requests";
        notifier.send(NotificationType.CHANGE_REQUEST_UPDATED,
                "요청이 '" + r.getStatus().getLabel() + "' 상태가 되었습니다",
                r.getTitle(), userId, Target.user(r.getRequester().getId(), link));
        log.info("[수정 요청] 상태 변경: id={}, status={}, by={}", requestId, r.getStatus(), userId);
    }

    // ──────────────────────────────────────────────────────────────────

    /** 요청한 본인, 같은 매장 점주, (본사 처리 요청이면) 본사가 볼 수 있다. */
    private static boolean canView(User user, ActorRole role, ChangeRequest r) {
        if (r.getRequester().getId().equals(user.getId())) return true;
        if (role == ActorRole.OWNER) return sameStore(user, r);
        return role == ActorRole.ADMIN && r.getHandler() == Handler.HQ;
    }

    /** 점주 처리 요청은 같은 매장 점주만, 본사 처리 요청은 본사만 상태를 바꿀 수 있다. */
    private static boolean canHandle(User user, ActorRole role, ChangeRequest r) {
        if (r.getHandler() == Handler.OWNER) {
            return role == ActorRole.OWNER && sameStore(user, r);
        }
        return role == ActorRole.ADMIN;
    }

    private static boolean sameStore(User user, ChangeRequest r) {
        return user.getStore() != null && user.getStore().getId().equals(r.getStore().getId());
    }

    private static void assertStore(Long expected, Long actual) {
        if (actual == null || !actual.equals(expected)) {
            throw new InoutException(ErrorCode.CROSS_STORE_FORBIDDEN);
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InoutException(ErrorCode.USER_NOT_FOUND));
    }
}
