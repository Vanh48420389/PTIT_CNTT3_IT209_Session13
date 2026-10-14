package vn.edu.ptit.campushub.post.team;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.campushub.post.PostService;
import vn.edu.ptit.campushub.post.team.TeamDtos.ApplicationView;

@RestController
@RequestMapping("/api/me/applications")
public class MyApplicationsController {
    private final PostService service;

    public MyApplicationsController(PostService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationView> getMyApplications(Principal principal) {
        return service.getMyApplications(principal);
    }
}
