package vn.edu.ptit.campushub.post;

import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.campushub.category.Category;
import vn.edu.ptit.campushub.category.CategoryRepository;
import vn.edu.ptit.campushub.comment.Comment;
import vn.edu.ptit.campushub.comment.CommentRepository;
import vn.edu.ptit.campushub.common.ApiException;
import vn.edu.ptit.campushub.post.PostDtos.AuthorView;
import vn.edu.ptit.campushub.post.PostDtos.CategoryView;
import vn.edu.ptit.campushub.post.PostDtos.CommentRequest;
import vn.edu.ptit.campushub.post.PostDtos.CommentView;
import vn.edu.ptit.campushub.post.PostDtos.PostRequest;
import vn.edu.ptit.campushub.post.PostDtos.PostView;
import vn.edu.ptit.campushub.post.team.ApplicationStatus;
import vn.edu.ptit.campushub.post.team.TeamApplication;
import vn.edu.ptit.campushub.post.team.TeamApplicationRepository;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplicationView;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplyRequest;
import vn.edu.ptit.campushub.reaction.Reaction;
import vn.edu.ptit.campushub.reaction.ReactionRepository;
import vn.edu.ptit.campushub.user.Role;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final CommentRepository comments;
    private final ReactionRepository reactions;
    private final TeamApplicationRepository teamApplications;

    public PostService(PostRepository posts, CategoryRepository categories, UserRepository users,
                       CommentRepository comments, ReactionRepository reactions,
                       TeamApplicationRepository teamApplications) {
        this.posts = posts;
        this.categories = categories;
        this.users = users;
        this.comments = comments;
        this.reactions = reactions;
        this.teamApplications = teamApplications;
    }

    public PageView<PostView> search(String keyword, Long categoryId, int page, int size, Principal principal) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<Post> result = posts.searchPublished(keyword == null ? "" : keyword.trim(), categoryId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PostView> content = result.getContent().stream()
                .map(post -> toView(post, principal, false))
                .toList();
        return new PageView<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    public PostView findOne(Long id, Principal principal) {
        return toView(findVisiblePost(id, principal), principal, true);
    }

    @Transactional
    public PostView create(PostRequest request, Principal principal) {
        User author = currentUser(principal);
        Category category = findCategory(request.categoryId());
        boolean isRecruiting = Boolean.TRUE.equals(request.isRecruiting());
        Post post = new Post(
                request.title().trim(),
                request.content().trim(),
                category,
                author,
                isRecruiting,
                isRecruiting ? request.targetMembers() : null,
                isRecruiting && request.skillsRequired() != null ? request.skillsRequired().trim() : null,
                isRecruiting && request.contactInfo() != null ? request.contactInfo().trim() : null
        );
        Post saved = posts.save(post);
        return toView(saved, principal, true);
    }

    @Transactional
    public PostView update(Long id, PostRequest request, Principal principal) {
        Post post = findPost(id);
        requireOwnerOrAdmin(post, currentUser(principal));
        post.update(request.title().trim(), request.content().trim(), findCategory(request.categoryId()));
        boolean isRecruiting = Boolean.TRUE.equals(request.isRecruiting());
        post.updateRecruitment(
                isRecruiting,
                isRecruiting ? request.targetMembers() : null,
                isRecruiting && request.skillsRequired() != null ? request.skillsRequired().trim() : null,
                isRecruiting && request.contactInfo() != null ? request.contactInfo().trim() : null
        );
        return toView(post, principal, true);
    }

    @Transactional
    public PostView changeStatus(Long id, PostStatus status, Principal principal) {
        Post post = findPost(id);
        User user = currentUser(principal);
        if (status == PostStatus.HIDDEN && user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ quản trị viên được ẩn bài đăng");
        }
        requireOwnerOrAdmin(post, user);
        post.changeStatus(status);
        return toView(post, principal, true);
    }

    @Transactional
    public PostView changeRecruitmentStatus(Long id, RecruitmentStatus status, Principal principal) {
        Post post = findPost(id);
        requireOwnerOrAdmin(post, currentUser(principal));
        post.changeRecruitmentStatus(status);
        return toView(post, principal, true);
    }

    @Transactional
    public void delete(Long id, Principal principal) {
        Post post = findPost(id);
        requireOwnerOrAdmin(post, currentUser(principal));
        teamApplications.deleteByPostId(id);
        comments.deleteByPostId(id);
        reactions.deleteByPostId(id);
        posts.delete(post);
    }

    @Transactional
    public CommentView addComment(Long postId, CommentRequest request, Principal principal) {
        Post post = findVisiblePost(postId, principal);
        Comment comment = comments.save(new Comment(request.content().trim(), post, currentUser(principal)));
        return CommentView.from(comment);
    }

    @Transactional
    public boolean toggleReaction(Long postId, Principal principal) {
        Post post = findVisiblePost(postId, principal);
        User user = currentUser(principal);
        return reactions.findByPostIdAndUserId(postId, user.getId())
                .map(reaction -> {
                    reactions.delete(reaction);
                    return false;
                })
                .orElseGet(() -> {
                    reactions.save(new Reaction(post, user));
                    return true;
                });
    }

    // ================== TEAM RECRUITMENT OPERATIONS ==================

    @Transactional
    public ApplicationView applyToTeam(Long postId, ApplyRequest request, Principal principal) {
        Post post = findVisiblePost(postId, principal);
        User applicant = currentUser(principal);

        if (!post.isRecruiting()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bài đăng này không mở tuyển thành viên nhóm");
        }
        if (post.getRecruitmentStatus() != RecruitmentStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nhóm đã đủ người hoặc đã đóng tuyển thành viên");
        }
        if (post.getAuthor().getId().equals(applicant.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bạn là người đăng bài, không thể tự ứng tuyển vào nhóm của mình");
        }

        if (teamApplications.findByPostIdAndApplicantId(postId, applicant.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đã gửi đơn ứng tuyển vào nhóm này rồi");
        }

        long acceptedCount = teamApplications.countByPostIdAndStatus(postId, ApplicationStatus.ACCEPTED);
        if (post.getTargetMembers() != null && acceptedCount >= post.getTargetMembers()) {
            post.changeRecruitmentStatus(RecruitmentStatus.FULL);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nhóm đã đủ thành viên");
        }

        TeamApplication application = new TeamApplication(
                post,
                applicant,
                request.message().trim(),
                request.contactInfo().trim()
        );
        return ApplicationView.from(teamApplications.save(application));
    }

    @Transactional
    public ApplicationView changeApplicationStatus(Long postId, Long applicationId, ApplicationStatus status, Principal principal) {
        Post post = findPost(postId);
        requireOwnerOrAdmin(post, currentUser(principal));

        TeamApplication application = teamApplications.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn ứng tuyển"));

        if (!application.getPost().getId().equals(postId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Đơn ứng tuyển không thuộc bài đăng này");
        }

        application.changeStatus(status);

        // Check if full
        if (status == ApplicationStatus.ACCEPTED && post.getTargetMembers() != null) {
            long acceptedCount = teamApplications.countByPostIdAndStatus(postId, ApplicationStatus.ACCEPTED);
            if (acceptedCount >= post.getTargetMembers()) {
                post.changeRecruitmentStatus(RecruitmentStatus.FULL);
            }
        }

        return ApplicationView.from(application);
    }

    @Transactional
    public void cancelMyApplication(Long postId, Principal principal) {
        User user = currentUser(principal);
        TeamApplication app = teamApplications.findByPostIdAndApplicantId(postId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bạn chưa nộp đơn ứng tuyển cho bài đăng này"));
        teamApplications.delete(app);
    }

    public List<ApplicationView> getMyApplications(Principal principal) {
        User user = currentUser(principal);
        return teamApplications.findByApplicantIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ApplicationView::from)
                .toList();
    }

    public List<ApplicationView> getPostApplications(Long postId, Principal principal) {
        Post post = findPost(postId);
        requireOwnerOrAdmin(post, currentUser(principal));
        return teamApplications.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(ApplicationView::from)
                .toList();
    }

    // ================================================================

    private PostView toView(Post post, Principal principal, boolean includeComments) {
        User current = findCurrentUser(principal);
        List<CommentView> commentViews = includeComments
                ? comments.findByPostIdOrderByCreatedAtAsc(post.getId()).stream().map(CommentView::from).toList()
                : List.of();

        int acceptedCount = (int) teamApplications.countByPostIdAndStatus(post.getId(), ApplicationStatus.ACCEPTED);

        ApplicationView myApp = null;
        List<ApplicationView> postApps = List.of();

        if (current != null && post.isRecruiting()) {
            myApp = teamApplications.findByPostIdAndApplicantId(post.getId(), current.getId())
                    .map(ApplicationView::from)
                    .orElse(null);

            boolean isOwnerOrAdmin = post.getAuthor().getId().equals(current.getId()) || current.getRole() == Role.ADMIN;
            if (isOwnerOrAdmin && includeComments) {
                postApps = teamApplications.findByPostIdOrderByCreatedAtDesc(post.getId())
                        .stream()
                        .map(ApplicationView::from)
                        .toList();
            }
        }

        return new PostView(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                new CategoryView(post.getCategory().getId(), post.getCategory().getName(), post.getCategory().getSlug()),
                new AuthorView(post.getAuthor().getId(), post.getAuthor().getUsername(), post.getAuthor().getDisplayName()),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                reactions.countByPostId(post.getId()),
                comments.countByPostId(post.getId()),
                current != null && reactions.findByPostIdAndUserId(post.getId(), current.getId()).isPresent(),
                post.isRecruiting(),
                post.getTargetMembers(),
                acceptedCount,
                post.getSkillsRequired(),
                post.getContactInfo(),
                post.getRecruitmentStatus(),
                myApp,
                postApps,
                commentViews
        );
    }

    private Post findVisiblePost(Long id, Principal principal) {
        Post post = findPost(id);
        User current = findCurrentUser(principal);
        if (post.getStatus() == PostStatus.HIDDEN
                && (current == null || current.getRole() != Role.ADMIN)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng");
        }
        return post;
    }

    private Post findPost(Long id) {
        return posts.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"));
    }

    private Category findCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Chuyên mục không hợp lệ"));
    }

    private User currentUser(Principal principal) {
        User user = findCurrentUser(principal);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }
        return user;
    }

    private User findCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return users.findByUsername(principal.getName()).orElse(null);
    }

    private void requireOwnerOrAdmin(Post post, User user) {
        if (!post.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền thay đổi bài đăng này");
        }
    }

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
