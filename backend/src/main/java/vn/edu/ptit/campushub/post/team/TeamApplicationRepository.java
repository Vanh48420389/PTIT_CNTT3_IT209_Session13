package vn.edu.ptit.campushub.post.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamApplicationRepository extends JpaRepository<TeamApplication, Long> {
    List<TeamApplication> findByPostIdOrderByCreatedAtDesc(Long postId);

    List<TeamApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    Optional<TeamApplication> findByPostIdAndApplicantId(Long postId, Long applicantId);

    long countByPostIdAndStatus(Long postId, ApplicationStatus status);

    @Modifying
    @Query("DELETE FROM TeamApplication a WHERE a.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
