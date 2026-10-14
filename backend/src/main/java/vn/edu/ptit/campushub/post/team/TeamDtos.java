package vn.edu.ptit.campushub.post.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import vn.edu.ptit.campushub.post.PostDtos.AuthorView;

public final class TeamDtos {
    private TeamDtos() {
    }

    public record ApplyRequest(
            @NotBlank(message = "Lời nhắn giới thiệu không được để trống")
            @Size(max = 500, message = "Lời nhắn tối đa 500 ký tự") String message,
            @NotBlank(message = "Thông tin liên hệ không được để trống (SĐT/Zalo/Email)")
            @Size(max = 120, message = "Thông tin liên hệ tối đa 120 ký tự") String contactInfo) {
    }

    public record ApplicationStatusRequest(
            @NotNull(message = "Trạng thái không được để trống") ApplicationStatus status) {
    }

    public record ApplicationView(
            Long id,
            Long postId,
            String postTitle,
            AuthorView applicant,
            String message,
            String contactInfo,
            ApplicationStatus status,
            Instant createdAt,
            Instant updatedAt) {
        public static ApplicationView from(TeamApplication app) {
            return new ApplicationView(
                    app.getId(),
                    app.getPost().getId(),
                    app.getPost().getTitle(),
                    new AuthorView(app.getApplicant().getId(), app.getApplicant().getUsername(), app.getApplicant().getDisplayName()),
                    app.getMessage(),
                    app.getContactInfo(),
                    app.getStatus(),
                    app.getCreatedAt(),
                    app.getUpdatedAt()
            );
        }
    }
}
