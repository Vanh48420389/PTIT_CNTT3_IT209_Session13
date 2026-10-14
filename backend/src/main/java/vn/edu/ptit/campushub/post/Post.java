package vn.edu.ptit.campushub.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import vn.edu.ptit.campushub.category.Category;
import vn.edu.ptit.campushub.user.User;

@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.PUBLISHED;

    @Column(name = "is_recruiting", nullable = false)
    private boolean isRecruiting = false;

    @Column(name = "target_members")
    private Integer targetMembers;

    @Column(name = "skills_required", length = 255)
    private String skillsRequired;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false, length = 20)
    private RecruitmentStatus recruitmentStatus = RecruitmentStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Post() {
    }

    public Post(String title, String content, Category category, User author) {
        this(title, content, category, author, false, null, null, null);
    }

    public Post(String title, String content, Category category, User author,
                boolean isRecruiting, Integer targetMembers, String skillsRequired, String contactInfo) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.author = author;
        this.isRecruiting = isRecruiting;
        this.targetMembers = targetMembers;
        this.skillsRequired = skillsRequired;
        this.contactInfo = contactInfo;
        this.recruitmentStatus = RecruitmentStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String title, String content, Category category) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.updatedAt = Instant.now();
    }

    public void updateRecruitment(boolean isRecruiting, Integer targetMembers, String skillsRequired, String contactInfo) {
        this.isRecruiting = isRecruiting;
        this.targetMembers = targetMembers;
        this.skillsRequired = skillsRequired;
        this.contactInfo = contactInfo;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(PostStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void changeRecruitmentStatus(RecruitmentStatus recruitmentStatus) {
        this.recruitmentStatus = recruitmentStatus;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public PostStatus getStatus() { return status; }
    public boolean isRecruiting() { return isRecruiting; }
    public Integer getTargetMembers() { return targetMembers; }
    public String getSkillsRequired() { return skillsRequired; }
    public String getContactInfo() { return contactInfo; }
    public RecruitmentStatus getRecruitmentStatus() { return recruitmentStatus; }
    public Category getCategory() { return category; }
    public User getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
