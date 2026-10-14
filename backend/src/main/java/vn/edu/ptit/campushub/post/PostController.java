package vn.edu.ptit.campushub.post;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.campushub.post.PostDtos.CommentRequest;
import vn.edu.ptit.campushub.post.PostDtos.CommentView;
import vn.edu.ptit.campushub.post.PostDtos.PostRequest;
import vn.edu.ptit.campushub.post.PostDtos.PostView;
import vn.edu.ptit.campushub.post.PostDtos.RecruitmentStatusRequest;
import vn.edu.ptit.campushub.post.PostDtos.StatusRequest;
import vn.edu.ptit.campushub.post.PostService.PageView;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplicationStatusRequest;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplicationView;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplyRequest;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @GetMapping
    public PageView<PostView> search(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Principal principal) {
        return service.search(keyword, categoryId, page, size, principal);
    }

    @GetMapping("/{id}")
    public PostView findOne(@PathVariable Long id, Principal principal) {
        return service.findOne(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostView create(@Valid @RequestBody PostRequest request, Principal principal) {
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    public PostView update(@PathVariable Long id, @Valid @RequestBody PostRequest request, Principal principal) {
        return service.update(id, request, principal);
    }

    @PatchMapping("/{id}/status")
    public PostView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                          Principal principal) {
        return service.changeStatus(id, request.status(), principal);
    }

    @PatchMapping("/{id}/recruitment-status")
    public PostView changeRecruitmentStatus(@PathVariable Long id, @Valid @RequestBody RecruitmentStatusRequest request,
                                           Principal principal) {
        return service.changeRecruitmentStatus(id, request.recruitmentStatus(), principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
                           Principal principal) {
        return service.addComment(id, request, principal);
    }

    @PostMapping("/{id}/reaction")
    public Map<String, Boolean> toggleReaction(@PathVariable Long id, Principal principal) {
        return Map.of("reacted", service.toggleReaction(id, principal));
    }

    // ================= TEAM RECRUITMENT APIS =================

    @PostMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationView applyToTeam(@PathVariable Long id, @Valid @RequestBody ApplyRequest request,
                                       Principal principal) {
        return service.applyToTeam(id, request, principal);
    }

    @DeleteMapping("/{id}/apply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelMyApplication(@PathVariable Long id, Principal principal) {
        service.cancelMyApplication(id, principal);
    }

    @GetMapping("/{id}/applications")
    public List<ApplicationView> getPostApplications(@PathVariable Long id, Principal principal) {
        return service.getPostApplications(id, principal);
    }

    @PatchMapping("/{id}/applications/{appId}/status")
    public ApplicationView changeApplicationStatus(@PathVariable Long id,
                                                   @PathVariable Long appId,
                                                   @Valid @RequestBody ApplicationStatusRequest request,
                                                   Principal principal) {
        return service.changeApplicationStatus(id, appId, request.status(), principal);
    }
}
