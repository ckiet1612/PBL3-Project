package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Promotion;
import com.pbl3.project.pbl3_project.entity.PromotionDiscountType;
import com.pbl3.project.pbl3_project.entity.PromotionLifecycleStatus;
import com.pbl3.project.pbl3_project.entity.PromotionScope;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.OrderItemRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.PromotionRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PromotionService {

    public record ProductPricingPreview(
        Product product,
        Promotion promotion,
        BigDecimal originalUnitPrice,
        BigDecimal discountedUnitPrice,
        BigDecimal unitDiscountAmount
    ) {
        public boolean hasPromotion() {
            return promotion != null && MoneySupport.isPositive(unitDiscountAmount);
        }
    }

    public record OrderPromotionPreview(
        Promotion promotion,
        BigDecimal discountAmount,
        BigDecimal discountedTotal,
        String displayLabel
    ) {
    }

    private final PromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthorizationService authorizationService;
    private final OperationalAuditLogService operationalAuditLogService;

    public PromotionService(
        PromotionRepository promotionRepository,
        ProductRepository productRepository,
        UserRepository userRepository,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        AuthorizationService authorizationService,
        OperationalAuditLogService operationalAuditLogService
    ) {
        this.promotionRepository = promotionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.authorizationService = authorizationService;
        this.operationalAuditLogService = operationalAuditLogService;
    }

    @Transactional(readOnly = true)
    public Page<Promotion> searchPromotions(
        User viewer,
        String search,
        Set<PromotionScope> selectedScopes,
        Boolean enabledFilter,
        Set<PromotionLifecycleStatus> selectedStatuses,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    ) {
        authorizationService.requirePromotionsAccess(viewer);
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("id", "name", "scope", "discountValue", "enabled", "startsAt", "endsAt", "createdAt", "createdBy.fullName", "targetProduct.name")
        );
        LocalDateTime now = LocalDateTime.now();
        Specification<Promotion> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var productJoin = root.join("targetProduct", JoinType.LEFT);
            var createdByJoin = root.join("createdBy", JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.function("str", String.class, root.get("id"))), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("name"), "")), likeValue),
                    cb.like(cb.lower(root.get("scope").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(productJoin.get("name"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(createdByJoin.get("fullName"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(createdByJoin.get("username"), "")), likeValue)
                ));
            }

            if (selectedScopes != null && !selectedScopes.isEmpty()) {
                predicates.add(root.get("scope").in(selectedScopes));
            }
            if (enabledFilter != null) {
                predicates.add(enabledFilter ? cb.isTrue(root.get("enabled")) : cb.isFalse(root.get("enabled")));
            }
            if (startDate != null) {
                predicates.add(cb.or(
                    cb.isNull(root.get("endsAt")),
                    cb.greaterThanOrEqualTo(root.get("endsAt"), startDate.atStartOfDay())
                ));
            }
            if (endDate != null) {
                predicates.add(cb.or(
                    cb.isNull(root.get("startsAt")),
                    cb.lessThanOrEqualTo(root.get("startsAt"), endDate.atTime(LocalTime.MAX))
                ));
            }
            if (selectedStatuses != null && !selectedStatuses.isEmpty()) {
                var statusPredicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (selectedStatuses.contains(PromotionLifecycleStatus.DISABLED)) {
                    statusPredicates.add(cb.isFalse(root.get("enabled")));
                }
                if (selectedStatuses.contains(PromotionLifecycleStatus.SCHEDULED)) {
                    statusPredicates.add(cb.and(
                        cb.isTrue(root.get("enabled")),
                        cb.isNotNull(root.get("startsAt")),
                        cb.greaterThan(root.get("startsAt"), now)
                    ));
                }
                if (selectedStatuses.contains(PromotionLifecycleStatus.ACTIVE)) {
                    statusPredicates.add(cb.and(
                        cb.isTrue(root.get("enabled")),
                        cb.or(cb.isNull(root.get("startsAt")), cb.lessThanOrEqualTo(root.get("startsAt"), now)),
                        cb.or(cb.isNull(root.get("endsAt")), cb.greaterThanOrEqualTo(root.get("endsAt"), now))
                    ));
                }
                if (selectedStatuses.contains(PromotionLifecycleStatus.EXPIRED)) {
                    statusPredicates.add(cb.and(
                        cb.isTrue(root.get("enabled")),
                        cb.isNotNull(root.get("endsAt")),
                        cb.lessThan(root.get("endsAt"), now)
                    ));
                }
                if (!statusPredicates.isEmpty()) {
                    predicates.add(cb.or(statusPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return promotionRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Promotion getPromotion(Long promotionId, User viewer) {
        authorizationService.requirePromotionsAccess(viewer);
        return promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ValidationException("Promotion not found: " + promotionId));
    }

    @Transactional
    public Promotion createPromotion(
        User actor,
        String name,
        PromotionScope scope,
        PromotionDiscountType discountType,
        BigDecimal discountValue,
        boolean enabled,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long targetProductId,
        BigDecimal minOrderTotal
    ) {
        User persistedActor = resolveWriter(actor);
        Promotion promotion = new Promotion();
        applyPromotionFields(promotion, name, scope, discountType, discountValue, enabled, startsAt, endsAt, targetProductId, minOrderTotal);
        promotion.setCreatedBy(persistedActor);
        Promotion saved = promotionRepository.save(promotion);
        operationalAuditLogService.record(
            persistedActor,
            OperationalAuditAction.PROMOTION_CREATED,
            OperationalSubjectType.PROMOTION,
            saved.getId(),
            buildSubjectLabel(saved),
            "Promotion created"
        );
        return saved;
    }

    @Transactional
    public Promotion updatePromotion(
        User actor,
        Long promotionId,
        String name,
        PromotionScope scope,
        PromotionDiscountType discountType,
        BigDecimal discountValue,
        boolean enabled,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long targetProductId,
        BigDecimal minOrderTotal
    ) {
        User persistedActor = resolveWriter(actor);
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ValidationException("Promotion not found: " + promotionId));
        applyPromotionFields(promotion, name, scope, discountType, discountValue, enabled, startsAt, endsAt, targetProductId, minOrderTotal);
        Promotion saved = promotionRepository.save(promotion);
        operationalAuditLogService.record(
            persistedActor,
            OperationalAuditAction.PROMOTION_UPDATED,
            OperationalSubjectType.PROMOTION,
            saved.getId(),
            buildSubjectLabel(saved),
            "Promotion updated"
        );
        return saved;
    }

    @Transactional
    public Promotion setPromotionEnabled(User actor, Long promotionId, boolean enabled) {
        User persistedActor = resolveWriter(actor);
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ValidationException("Promotion not found: " + promotionId));
        promotion.setEnabled(enabled);
        Promotion saved = promotionRepository.save(promotion);
        operationalAuditLogService.record(
            persistedActor,
            enabled ? OperationalAuditAction.PROMOTION_ENABLED : OperationalAuditAction.PROMOTION_DISABLED,
            OperationalSubjectType.PROMOTION,
            saved.getId(),
            buildSubjectLabel(saved),
            enabled ? "Promotion enabled" : "Promotion disabled"
        );
        return saved;
    }

    @Transactional
    public void deletePromotion(User actor, Long promotionId) {
        User persistedActor = resolveWriter(actor);
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new ValidationException("Promotion not found: " + promotionId));
        if (isPromotionUsed(promotionId)) {
            throw new ValidationException("Promotions already used in orders cannot be deleted");
        }
        promotionRepository.delete(promotion);
    }

    @Transactional(readOnly = true)
    public boolean isPromotionUsed(Long promotionId) {
        if (promotionId == null) {
            return false;
        }
        return orderRepository.countByAppliedOrderPromotionIdSnapshot(promotionId) > 0
            || orderItemRepository.countByAppliedProductPromotionIdSnapshot(promotionId) > 0;
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductPricingPreview> previewBestProductPricing(Collection<Product> products, LocalDateTime at) {
        Map<Long, ProductPricingPreview> previews = new LinkedHashMap<>();
        if (products == null || products.isEmpty()) {
            return previews;
        }
        Map<Long, Product> byId = new LinkedHashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                byId.put(product.getId(), product);
            }
        }
        if (byId.isEmpty()) {
            return previews;
        }
        List<Promotion> promotions = promotionRepository.findActiveProductPromotionsForProductIds(byId.keySet(), at != null ? at : LocalDateTime.now());
        Map<Long, Promotion> bestPromotions = new HashMap<>();
        for (Promotion promotion : promotions) {
            if (promotion.getTargetProduct() == null || promotion.getTargetProduct().getId() == null) {
                continue;
            }
            Long productId = promotion.getTargetProduct().getId();
            Product product = byId.get(productId);
            if (product == null) {
                continue;
            }
            Promotion currentBest = bestPromotions.get(productId);
            if (currentBest == null || comparePromotionValue(product, promotion, currentBest) > 0) {
                bestPromotions.put(productId, promotion);
            }
        }
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            Promotion best = bestPromotions.get(product.getId());
            previews.put(product.getId(), toProductPricingPreview(product, best));
        }
        return previews;
    }

    @Transactional(readOnly = true)
    public ProductPricingPreview previewBestProductPricing(Product product, LocalDateTime at) {
        if (product == null || product.getId() == null) {
            return new ProductPricingPreview(product, null, MoneySupport.ZERO, MoneySupport.ZERO, MoneySupport.ZERO);
        }
        return previewBestProductPricing(java.util.List.of(product), at).getOrDefault(
            product.getId(),
            new ProductPricingPreview(product, null, MoneySupport.normalize(product.getPrice()), MoneySupport.normalize(product.getPrice()), MoneySupport.ZERO)
        );
    }

    @Transactional(readOnly = true)
    public List<OrderPromotionPreview> getEligibleOrderPromotionPreviews(BigDecimal subtotal, LocalDateTime at) {
        BigDecimal normalizedSubtotal = MoneySupport.normalize(subtotal);
        if (!MoneySupport.isPositive(normalizedSubtotal)) {
            return java.util.List.of();
        }
        List<Promotion> promotions = promotionRepository.findEligibleOrderPromotions(normalizedSubtotal, at != null ? at : LocalDateTime.now());
        return promotions.stream()
            .map(promotion -> {
                BigDecimal discountAmount = computeDiscountAmount(promotion, normalizedSubtotal);
                BigDecimal discountedTotal = MoneySupport.subtract(normalizedSubtotal, discountAmount);
                return new OrderPromotionPreview(
                    promotion,
                    discountAmount,
                    discountedTotal,
                    promotion.getName() + " • -" + formatDiscountLabel(promotion, discountAmount)
                );
            })
            .sorted(Comparator
                .comparing(OrderPromotionPreview::discountAmount, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(preview -> preview.promotion().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    @Transactional(readOnly = true)
    public Promotion resolveEligibleOrderPromotion(Long promotionId, BigDecimal subtotal, LocalDateTime at) {
        if (promotionId == null) {
            return null;
        }
        return getEligibleOrderPromotionPreviews(subtotal, at).stream()
            .map(OrderPromotionPreview::promotion)
            .filter(promotion -> promotion.getId() != null && promotion.getId().equals(promotionId))
            .findFirst()
            .orElseThrow(() -> new ValidationException("Selected order promotion is no longer eligible"));
    }

    @Transactional(readOnly = true)
    public List<Promotion> findAllActivePromotions(LocalDateTime at) {
        return promotionRepository.findAllActiveAt(at != null ? at : LocalDateTime.now());
    }

    public BigDecimal computeDiscountAmount(Promotion promotion, BigDecimal baseAmount) {
        BigDecimal normalizedBase = MoneySupport.normalize(baseAmount);
        if (promotion == null || !MoneySupport.isPositive(normalizedBase)) {
            return MoneySupport.ZERO;
        }
        BigDecimal rawDiscount;
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENT) {
            rawDiscount = normalizedBase
                .multiply(MoneySupport.normalize(promotion.getDiscountValue()))
                .divide(BigDecimal.valueOf(100), MoneySupport.MONEY_SCALE, MoneySupport.MONEY_ROUNDING);
        } else {
            rawDiscount = MoneySupport.normalize(promotion.getDiscountValue());
        }
        BigDecimal clamped = rawDiscount.min(normalizedBase);
        return MoneySupport.normalize(clamped);
    }

    public String formatDiscountLabel(Promotion promotion, BigDecimal computedDiscount) {
        if (promotion == null) {
            return "0";
        }
        if (promotion.getDiscountType() == PromotionDiscountType.PERCENT) {
            return MoneySupport.normalize(promotion.getDiscountValue()).stripTrailingZeros().toPlainString() + "%";
        }
        return String.format(java.util.Locale.US, "%,.0f VND", MoneySupport.normalize(computedDiscount));
    }

    private ProductPricingPreview toProductPricingPreview(Product product, Promotion promotion) {
        BigDecimal originalUnitPrice = MoneySupport.normalize(product != null ? product.getPrice() : MoneySupport.ZERO);
        BigDecimal unitDiscountAmount = computeDiscountAmount(promotion, originalUnitPrice);
        BigDecimal discountedUnitPrice = MoneySupport.subtract(originalUnitPrice, unitDiscountAmount);
        return new ProductPricingPreview(product, promotion, originalUnitPrice, discountedUnitPrice, unitDiscountAmount);
    }

    private int comparePromotionValue(Product product, Promotion candidate, Promotion currentBest) {
        BigDecimal productPrice = product != null ? product.getPrice() : MoneySupport.ZERO;
        BigDecimal candidateDiscount = computeDiscountAmount(candidate, productPrice);
        BigDecimal bestDiscount = computeDiscountAmount(currentBest, productPrice);
        int comparison = candidateDiscount.compareTo(bestDiscount);
        if (comparison != 0) {
            return comparison;
        }
        String candidateName = candidate != null ? candidate.getName() : "";
        String bestName = currentBest != null ? currentBest.getName() : "";
        return bestName.compareToIgnoreCase(candidateName) < 0 ? 1 : -1;
    }

    private User resolveWriter(User actor) {
        authorizationService.requirePromotionWrite(actor);
        if (actor == null || actor.getId() == null) {
            throw new ValidationException("User not found");
        }
        return userRepository.findById(actor.getId())
            .orElseThrow(() -> new ValidationException("User not found"));
    }

    private void applyPromotionFields(
        Promotion promotion,
        String name,
        PromotionScope scope,
        PromotionDiscountType discountType,
        BigDecimal discountValue,
        boolean enabled,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long targetProductId,
        BigDecimal minOrderTotal
    ) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new ValidationException("Promotion name is required");
        }
        if (scope == null) {
            throw new ValidationException("Promotion scope is required");
        }
        if (discountType == null) {
            throw new ValidationException("Discount type is required");
        }
        BigDecimal normalizedDiscountValue = MoneySupport.normalize(discountValue);
        if (!MoneySupport.isPositive(normalizedDiscountValue)) {
            throw new ValidationException("Discount value must be greater than 0");
        }
        if (discountType == PromotionDiscountType.PERCENT && normalizedDiscountValue.compareTo(new BigDecimal("100.00")) > 0) {
            throw new ValidationException("Percent discount cannot exceed 100");
        }
        if (startsAt != null && endsAt != null && startsAt.isAfter(endsAt)) {
            throw new ValidationException("Promotion start must be before end");
        }

        promotion.setName(normalizedName);
        promotion.setScope(scope);
        promotion.setDiscountType(discountType);
        promotion.setDiscountValue(normalizedDiscountValue);
        promotion.setEnabled(enabled);
        promotion.setStartsAt(startsAt);
        promotion.setEndsAt(endsAt);

        if (scope == PromotionScope.PRODUCT) {
            if (targetProductId == null) {
                throw new ValidationException("Target product is required for product promotions");
            }
            Product targetProduct = productRepository.findById(targetProductId)
                .orElseThrow(() -> new ValidationException("Target product not found: " + targetProductId));
            if (targetProduct.isDeleted()) {
                throw new ValidationException("Target product is inactive");
            }
            promotion.setTargetProduct(targetProduct);
            promotion.setMinOrderTotal(null);
        } else {
            promotion.setTargetProduct(null);
            if (minOrderTotal != null && MoneySupport.normalize(minOrderTotal).compareTo(MoneySupport.ZERO) < 0) {
                throw new ValidationException("Minimum order total cannot be negative");
            }
            promotion.setMinOrderTotal(minOrderTotal != null ? MoneySupport.normalize(minOrderTotal) : null);
        }
    }

    private String buildSubjectLabel(Promotion promotion) {
        String name = promotion != null && promotion.getName() != null && !promotion.getName().isBlank()
            ? promotion.getName()
            : "Promotion";
        return name + " (#" + (promotion != null ? promotion.getId() : null) + ")";
    }
}
