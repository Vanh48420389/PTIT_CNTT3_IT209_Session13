package vn.edu.ptit.campushub;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicUserCanReadSeededPosts() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void anonymousUserCannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bài mới","content":"Nội dung hợp lệ","categoryId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tìm nhóm học DevOps","content":"Cần thêm hai thành viên cùng thực hành.","categoryId":6}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tìm nhóm học DevOps"))
                .andExpect(jsonPath("$.author.username").value("student"));
    }

    @Test
    void invalidPostReturnsReadableMessage() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","content":"Nội dung","categoryId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void teamRecruitmentFullFlowTest() throws Exception {
        // 1. Student creates a recruitment post
        MvcResult postResult = mockMvc.perform(post("/api/posts")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Tuyển thành viên Hackathon 2026",
                                  "content": "Cần tìm 1 bạn chuyên Backend Spring Boot",
                                  "categoryId": 6,
                                  "isRecruiting": true,
                                  "targetMembers": 1,
                                  "skillsRequired": "Spring Boot, Docker",
                                  "contactInfo": "Zalo: 0999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isRecruiting").value(true))
                .andExpect(jsonPath("$.targetMembers").value(1))
                .andExpect(jsonPath("$.recruitmentStatus").value("OPEN"))
                .andReturn();

        Map<?, ?> postMap = objectMapper.readValue(postResult.getResponse().getContentAsString(), Map.class);
        Long postId = ((Number) postMap.get("id")).longValue();

        // 2. Author cannot apply to own post
        mockMvc.perform(post("/api/posts/" + postId + "/apply")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Tôi tự ứng tuyển",
                                  "contactInfo": "0999999999"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // 3. haianh applies to student's post
        MvcResult applyResult = mockMvc.perform(post("/api/posts/" + postId + "/apply")
                        .with(httpBasic("haianh", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Em ứng tuyển vị trí Spring Boot",
                                  "contactInfo": "haianh@gmail.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.applicant.username").value("haianh"))
                .andReturn();

        Map<?, ?> applyMap = objectMapper.readValue(applyResult.getResponse().getContentAsString(), Map.class);
        Long appId = ((Number) applyMap.get("id")).longValue();

        // 4. Duplicate application by haianh returns 409 CONFLICT
        mockMvc.perform(post("/api/posts/" + postId + "/apply")
                        .with(httpBasic("haianh", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Em gửi lại",
                                  "contactInfo": "haianh@gmail.com"
                                }
                                """))
                .andExpect(status().isConflict());

        // 5. Author views applications
        mockMvc.perform(get("/api/posts/" + postId + "/applications")
                        .with(httpBasic("student", "student123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 6. Author accepts haianh -> Post reaches targetMembers (1) -> status becomes FULL
        mockMvc.perform(patch("/api/posts/" + postId + "/applications/" + appId + "/status")
                        .with(httpBasic("student", "student123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ACCEPTED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // 7. Verify post is now FULL
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedMembersCount").value(1))
                .andExpect(jsonPath("$.recruitmentStatus").value("FULL"));
    }
}
