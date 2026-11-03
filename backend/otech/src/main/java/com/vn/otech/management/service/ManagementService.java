package com.vn.otech.management.service;

import com.vn.otech.auth.repository.RoleRepository;
import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Group;
import com.vn.otech.entity.Post;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.Report;
import com.vn.otech.entity.ReportStatus;
import com.vn.otech.entity.ReportTargetType;
import com.vn.otech.entity.Role;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.User;
import com.vn.otech.management.dto.ChangeUserRoleRequest;
import com.vn.otech.management.dto.CreateManagerRequest;
import com.vn.otech.management.dto.ManagementDashboardSummary;
import com.vn.otech.management.dto.ManagementPage;
import com.vn.otech.management.dto.ManagedGroupResponse;
import com.vn.otech.management.dto.ManagedPostResponse;
import com.vn.otech.management.dto.ManagedProductResponse;
import com.vn.otech.management.dto.ManagedReportResponse;
import com.vn.otech.management.dto.ManagedUserResponse;
import com.vn.otech.management.repository.GroupManagementRepository;
import com.vn.otech.management.repository.ReportManagementRepository;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.post.repository.PostRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ManagementService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final PostRepository postRepository;
    private final GroupManagementRepository groupRepository;
    private final ReportManagementRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            ProductRepository productRepository,
            PostRepository postRepository,
            GroupManagementRepository groupRepository,
            ReportManagementRepository reportRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.productRepository = productRepository;
        this.postRepository = postRepository;
        this.groupRepository = groupRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ManagementDashboardSummary getSummary() {
        return new ManagementDashboardSummary(
                userRepository.countByRole_Name(RoleName.USER),
                userRepository.countByRole_NameAndStatus(RoleName.USER, AccountStatus.ACTIVE),
                userRepository.countByRole_NameAndStatus(RoleName.USER, AccountStatus.LOCKED),
                productRepository.count(),
                productRepository.countByStatus(ProductStatus.HIDDEN),
                postRepository.count(),
                postRepository.countByHiddenTrue(),
                groupRepository.count(),
                groupRepository.countByHiddenTrue(),
                reportRepository.countByStatus(ReportStatus.PENDING),
                userRepository.countByRole_Name(RoleName.MANAGER));
    }

    public ManagementPage<ManagedUserResponse> listUsers(
            String search, RoleName role, AccountStatus status, int page, int size) {
        String normalizedSearch = normalizeSearch(search);
        Specification<User> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)));
            }
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.join("role").get("name"), role));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return ManagementPage.from(userRepository.findAll(specification, pageable(page, size))
                .map(this::toUserResponse));
    }

    public ManagementPage<ManagedProductResponse> listProducts(
            String search, ProductStatus status, int page, int size) {
        String normalizedSearch = normalizeSearch(search);
        Specification<Product> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                root.join("seller", JoinType.LEFT).get("fullName")), pattern)));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return ManagementPage.from(productRepository.findAll(specification, pageable(page, size))
                .map(this::toProductResponse));
    }

    public ManagementPage<ManagedPostResponse> listPosts(
            String search, Boolean hidden, int page, int size) {
        String normalizedSearch = normalizeSearch(search);
        Specification<Post> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                criteriaBuilder.coalesce(
                                        root.join("author", JoinType.LEFT).get("fullName"), "")), pattern)));
            }
            if (hidden != null) {
                predicates.add(criteriaBuilder.equal(root.get("hidden"), hidden));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return ManagementPage.from(postRepository.findAll(specification, pageable(page, size))
                .map(this::toPostResponse));
    }

    public ManagementPage<ManagedGroupResponse> listGroups(
            String search, Boolean hidden, int page, int size) {
        String normalizedSearch = normalizeSearch(search);
        Specification<Group> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(
                                criteriaBuilder.coalesce(
                                        root.join("owner", JoinType.LEFT).get("fullName"), "")), pattern)));
            }
            if (hidden != null) {
                predicates.add(criteriaBuilder.equal(root.get("hidden"), hidden));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return ManagementPage.from(groupRepository.findAll(specification, pageable(page, size))
                .map(this::toGroupResponse));
    }

    public ManagementPage<ManagedReportResponse> listReports(
            ReportStatus status, int page, int size) {
        Specification<Report> specification = (root, query, criteriaBuilder) -> status == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("status"), status);
        return ManagementPage.from(reportRepository.findAll(specification, pageable(page, size))
                .map(this::toReportResponse));
    }

    @Transactional
    public ManagedUserResponse updateUserStatus(UUID userId, AccountStatus status) {
        if (status != AccountStatus.ACTIVE && status != AccountStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Users can only be activated or locked");
        }
        User user = findUser(userId);
        requireRole(user, RoleName.USER);
        if (user.getStatus() == AccountStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot be reactivated");
        }
        user.setStatus(status);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public ManagedProductResponse updateProductVisibility(UUID productId, boolean hidden) {
        Product product = productRepository.findWithSellerAndCategoryById(productId)
                .orElseThrow(() -> notFound("Product"));
        if (product.getStatus() == ProductStatus.SOLD && !hidden) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sold products cannot be restored");
        }
        product.setStatus(hidden ? ProductStatus.HIDDEN : ProductStatus.SELLING);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ManagedPostResponse updatePostVisibility(UUID postId, boolean hidden) {
        Post post = postRepository.findById(postId).orElseThrow(() -> notFound("Post"));
        post.setHidden(hidden);
        return toPostResponse(postRepository.save(post));
    }

    @Transactional
    public ManagedGroupResponse updateGroupVisibility(UUID groupId, boolean hidden) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> notFound("Group"));
        group.setHidden(hidden);
        return toGroupResponse(groupRepository.save(group));
    }

    @Transactional
    public ManagedReportResponse decideReport(UUID reportId, ReportStatus status, String actorEmail) {
        if (status != ReportStatus.APPROVED && status != ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A report must be approved or rejected");
        }
        Report report = reportRepository.findById(reportId).orElseThrow(() -> notFound("Report"));
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report has already been handled");
        }
        if (status == ReportStatus.APPROVED) {
            hideReportedTarget(report);
        }
        report.setStatus(status);
        report.setHandledBy(findActor(actorEmail));
        return toReportResponse(reportRepository.save(report));
    }

    @Transactional
    public ManagedUserResponse createManager(CreateManagerRequest request, String actorEmail) {
        requireAdmin(actorEmail);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "MANAGER role is missing"));
        User manager = new User();
        manager.setFullName(request.fullName().trim());
        manager.setEmail(email);
        manager.setPasswordHash(passwordEncoder.encode(request.password()));
        manager.setRole(managerRole);
        manager.setStatus(AccountStatus.ACTIVE);
        manager.setEmailVerified(true);
        return toUserResponse(userRepository.save(manager));
    }

    @Transactional
    public ManagedUserResponse updateManagerStatus(
            UUID managerId, AccountStatus status, String actorEmail) {
        requireAdmin(actorEmail);
        if (status != AccountStatus.ACTIVE && status != AccountStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Managers can only be activated or locked");
        }
        User actor = findActor(actorEmail);
        User manager = findUser(managerId);
        requireRole(manager, RoleName.MANAGER);
        preventSelfManagement(actor, manager);
        if (manager.getStatus() == AccountStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted managers cannot be reactivated");
        }
        manager.setStatus(status);
        return toUserResponse(userRepository.save(manager));
    }

    @Transactional
    public ManagedUserResponse deleteManager(UUID managerId, String actorEmail) {
        requireAdmin(actorEmail);
        User actor = findActor(actorEmail);
        User manager = findUser(managerId);
        requireRole(manager, RoleName.MANAGER);
        preventSelfManagement(actor, manager);
        manager.setStatus(AccountStatus.DELETED);
        return toUserResponse(userRepository.save(manager));
    }

    @Transactional
    public ManagedUserResponse changeUserRole(
            UUID userId, RoleName roleName, String actorEmail) {
        requireAdmin(actorEmail);
        if (roleName != RoleName.USER && roleName != RoleName.MANAGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin roles cannot be assigned here");
        }
        User actor = findActor(actorEmail);
        User user = findUser(userId);
        if (user.getRole().getName() == RoleName.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be changed here");
        }
        if (user.getStatus() == AccountStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot change roles");
        }
        preventSelfManagement(actor, user);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Requested role is missing"));
        user.setRole(role);
        return toUserResponse(userRepository.save(user));
    }

    private void hideReportedTarget(Report report) {
        UUID targetId = report.getTargetId();
        ReportTargetType targetType = report.getTargetType();
        switch (targetType) {
            case USER -> {
                User user = findUser(targetId);
                requireRole(user, RoleName.USER);
                if (user.getStatus() == AccountStatus.DELETED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted accounts cannot be locked");
                }
                user.setStatus(AccountStatus.LOCKED);
                userRepository.save(user);
            }
            case PRODUCT -> {
                Product product = productRepository.findById(targetId).orElseThrow(() -> notFound("Reported product"));
                product.setStatus(ProductStatus.HIDDEN);
                productRepository.save(product);
            }
            case POST -> {
                Post post = postRepository.findById(targetId).orElseThrow(() -> notFound("Reported post"));
                post.setHidden(true);
                postRepository.save(post);
            }
            case GROUP -> {
                Group group = groupRepository.findById(targetId).orElseThrow(() -> notFound("Reported group"));
                group.setHidden(true);
                groupRepository.save(group);
            }
        }
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalizeSearch(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> notFound("User"));
    }

    private User findActor(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
    }

    private void requireAdmin(String actorEmail) {
        if (findActor(actorEmail).getRole().getName() != RoleName.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
    }

    private void requireRole(User user, RoleName role) {
        if (user.getRole().getName() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account cannot be managed here");
        }
    }

    private void preventSelfManagement(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own management account");
        }
    }

    private ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resource + " not found");
    }

    private ManagedUserResponse toUserResponse(User user) {
        return new ManagedUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName(),
                user.getStatus(), user.getCreatedAt());
    }

    private ManagedProductResponse toProductResponse(Product product) {
        return new ManagedProductResponse(
                product.getId(), product.getTitle(),
                product.getSeller() == null ? "Otech member" : product.getSeller().getFullName(),
                product.getPrice(), product.getStatus(), product.getCreatedAt());
    }

    private ManagedPostResponse toPostResponse(Post post) {
        return new ManagedPostResponse(
                post.getId(), post.getContent(),
                post.getAuthor() == null ? "Otech member" : post.getAuthor().getFullName(),
                post.isHidden(), post.getCreatedAt());
    }

    private ManagedGroupResponse toGroupResponse(Group group) {
        return new ManagedGroupResponse(
                group.getId(), group.getName(),
                group.getOwner() == null ? "Otech member" : group.getOwner().getFullName(),
                group.isHidden(), group.getCreatedAt());
    }

    private ManagedReportResponse toReportResponse(Report report) {
        return new ManagedReportResponse(
                report.getId(), report.getTargetType(), report.getTargetId(), report.getReason(),
                report.getReporter() == null ? "Otech member" : report.getReporter().getFullName(),
                report.getStatus(), report.getCreatedAt());
    }
}
