package vn.edu.ptit.campushub.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.campushub.category.Category;
import vn.edu.ptit.campushub.category.CategoryRepository;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.post.PostRepository;
import vn.edu.ptit.campushub.post.team.TeamApplication;
import vn.edu.ptit.campushub.post.team.TeamApplicationRepository;
import vn.edu.ptit.campushub.user.Role;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;

@Component
public class DemoDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final PostRepository posts;
    private final TeamApplicationRepository teamApplications;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, CategoryRepository categories, PostRepository posts,
                          TeamApplicationRepository teamApplications,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.categories = categories;
        this.posts = posts;
        this.teamApplications = teamApplications;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category events = createCategoryIfMissing("Sự kiện", "su-kien");
        Category lost = createCategoryIfMissing("Đồ thất lạc", "do-that-lac");
        createCategoryIfMissing("Hỏi đáp học tập", "hoi-dap");
        createCategoryIfMissing("Trao đổi đồ dùng", "trao-doi");
        createCategoryIfMissing("Phản ánh", "phan-anh");
        Category teamCategory = createCategoryIfMissing("Tìm nhóm", "tim-nhom");

        User admin = createUserIfMissing("admin", "Quản trị viên", "admin123", Role.ADMIN);
        User student = createUserIfMissing("student", "Nguyễn Minh Anh", "student123", Role.USER);
        User student2 = createUserIfMissing("haianh", "Trần Hải Anh", "student123", Role.USER);

        if (posts.count() == 0) {
            posts.save(new Post("Ngày hội câu lạc bộ sinh viên",
                    "Các câu lạc bộ sẽ gặp gỡ sinh viên mới tại sân A2 vào chiều thứ Sáu.", events, admin));
            posts.save(new Post("Tìm ví màu đen gần thư viện",
                    "Mình làm rơi một chiếc ví màu đen vào sáng nay. Bạn nào nhặt được vui lòng bình luận giúp mình.",
                    lost, student));

            Post teamPost = posts.save(new Post(
                    "Tuyển 2 bạn làm đồ án tốt nghiệp mảng Cloud & DevOps",
                    "Nhóm mình đang nghiên cứu đề tài triển khai CI/CD và Kubernetes cho vi dịch vụ. Cần tìm thêm 2 bạn có nền tảng Java/Docker cùng nghiên cứu và hoàn thành sản phẩm.",
                    teamCategory,
                    student,
                    true,
                    2,
                    "Docker, Java Spring Boot, React, Git",
                    "Zalo: 0987654321 / minhanh.ptit@gmail.com"
            ));

            teamApplications.save(new TeamApplication(
                    teamPost,
                    student2,
                    "Chào bạn, mình đã có kinh nghiệm làm đồ án Spring Boot và Docker, mong muốn được tham gia nhóm để cùng nghiên cứu mảng Cloud!",
                    "haianh.tran@ptit.edu.vn - 0912345678"
            ));
        }
    }

    private Category createCategoryIfMissing(String name, String slug) {
        return categories.findBySlug(slug)
                .orElseGet(() -> categories.save(new Category(name, slug)));
    }

    private User createUserIfMissing(String username, String displayName, String password, Role role) {
        return users.findByUsername(username)
                .orElseGet(() -> users.save(new User(username, displayName,
                        passwordEncoder.encode(password), role)));
    }
}
