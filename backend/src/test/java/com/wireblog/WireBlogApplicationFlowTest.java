package com.wireblog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:wireblog-flow-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "app.jwt.secret=flow-test-jwt-secret-must-be-at-least-32-bytes",
        "app.jwt.expiration-ms=3600000",
        "app.cors.allowed-origin-patterns=http://localhost:4200",
        "app.news.api-key=",
        "app.upload.dir=${java.io.tmpdir}/wireblog-flow-test-uploads"
})
class WireBlogApplicationFlowTest {
    private static final AtomicInteger USER_NUMBER = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void removeTestUploads() throws Exception {
        Path uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "wireblog-flow-test-uploads");
        if (!Files.exists(uploadDir)) return;
        try (var files = Files.walk(uploadDir)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
        }
    }

    @Test
    void readerAndAuthorCanPublishDiscoverAndDiscussStories() throws Exception {
        JsonNode author = register("author");
        JsonNode reader = register("reader");
        String authorToken = token(author);
        String readerToken = token(reader);

        mvc.perform(get("/api/users/me").header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handle").value(author.get("handle").asText()));
        mvc.perform(patch("/api/users/me").header("Authorization", bearer(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("displayName", "Updated Writer", "bio", "Stories worth following"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Stories worth following"));

        JsonNode published = apiPost("/api/posts", authorToken, Map.of(
                "title", "A Story Worth Following",
                "excerpt", "A short summary",
                "content", "<p>Story body</p><script>alert(1)</script>",
                "tags", new String[]{"technology"},
                "trailTitle", "Technology Watch",
                "publish", true));
        long postId = published.get("id").asLong();
        String slug = published.get("slug").asText();
        assertThat(published.get("content").asText()).doesNotContain("<script>");
        assertThat(published.path("trail").path("slug").asText()).isNotBlank();

        mvc.perform(get("/api/posts").param("q", "Story Worth").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(slug));
        mvc.perform(get("/api/posts").param("tag", "technology").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId));
        mvc.perform(get("/api/posts/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").doesNotExist());
        mvc.perform(get("/api/trails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(published.path("trail").path("slug").asText()));
        mvc.perform(get("/api/trails/{slug}", published.path("trail").path("slug").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stories[0].id").value(postId));

        JsonNode comment = apiPost("/api/comments/post/" + postId, readerToken,
                Map.of("body", "A useful comment"));
        apiPost("/api/comments/post/" + postId, authorToken,
                Map.of("body", "A reply", "parentId", comment.get("id").asLong()));
        mvc.perform(get("/api/comments/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].replies[0].body").value("A reply"));
        mvc.perform(get("/api/notifications").header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("REPLY"));
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
        mvc.perform(patch("/api/notifications/read-all").header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(readerToken)))
                .andExpect(content().string("0"));

        mvc.perform(post("/api/comments/{commentId}/flag", comment.get("id").asLong())
                        .header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/pulses/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(0));
        mvc.perform(post("/api/pulses/posts/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("choice", "NEW_ANGLE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotes").value(1));

        mvc.perform(post("/api/posts/{postId}/share", postId)
                        .header("Authorization", bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("channel", "WHATSAPP"))))
                .andExpect(status().isOk());

        JsonNode link = apiPost("/api/posts/share-link", authorToken, Map.of(
                "url", "https://example.org/story",
                "title", "Shared External Story",
                "note", "Why it matters"));
        mvc.perform(get("/api/posts/{slug}", link.get("slug").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://example.org/story"));
        mvc.perform(put("/api/posts/{id}/share-link", link.get("id").asLong())
                        .header("Authorization", bearer(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("url", "https://example.org/updated", "title", "Updated Link"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value("https://example.org/updated"));
        mvc.perform(put("/api/posts/{id}", postId)
                        .header("Authorization", bearer(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "title", "Updated Story",
                                "content", "Revised story content",
                                "tags", new String[]{"technology", "updates"},
                                "trailTitle", "Technology Watch",
                                "publish", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Story"));
        mvc.perform(put("/api/posts/{id}", postId)
                        .header("Authorization", bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "title", "Unauthorized Edit",
                                "content", "Should not save",
                                "tags", new String[0],
                                "publish", true))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/posts/mine").header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        MockMultipartFile image = new MockMultipartFile("file", "tiny.png", "image/png",
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10, 1});
        MvcResult upload = mvc.perform(multipart("/api/upload").file(image)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andReturn();
        String uploadUrl = json.readTree(upload.getResponse().getContentAsString()).path("url").asText();
        String imageName = uploadUrl.substring(uploadUrl.lastIndexOf('/') + 1);
        assertThat(Path.of(System.getProperty("java.io.tmpdir"), "wireblog-flow-test-uploads", imageName))
                .exists();
        MockMultipartFile invalidImage = new MockMultipartFile("file", "not-an-image.png", "image/png",
                "not an image".getBytes());
        mvc.perform(multipart("/api/upload").file(invalidImage)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/posts/{id}", link.get("id").asLong())
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/posts/{id}", postId)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{slug}", slug))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/news/headlines"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/sponsorships/active").param("placement", "NEWS_TICKER"))
                .andExpect(status().isOk());
    }

    @Test
    void normalUsersCanFollowPublicGroupsAndRequestPrivateMembership() throws Exception {
        JsonNode creator = register("groupcreator");
        JsonNode member = register("groupmember");
        String creatorToken = token(creator);
        String memberToken = token(member);

        JsonNode publicGroup = apiPost("/api/groups", creatorToken, Map.of(
                "name", "Public Technology Group",
                "description", "Discuss technology stories",
                "visibility", "PUBLIC",
                "joinPolicy", "OPEN",
                "topics", new String[]{"technology", "science"},
                "rules", "Be constructive"));
        long publicGroupId = publicGroup.get("id").asLong();
        mvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visibility").value("PUBLIC"));
        mvc.perform(get("/api/groups/{id}", publicGroupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules").value("Be constructive"));
        JsonNode followed = apiPost("/api/groups/" + publicGroupId + "/follow", memberToken, Map.of());
        assertThat(followed.get("isMember").asBoolean()).isTrue();

        JsonNode groupPost = apiPost("/api/groups/" + publicGroupId + "/posts", memberToken, Map.of(
                "title", "What this means",
                "body", "Let's discuss the source.",
                "trailTitle", "Technology Watch"));
        apiPost("/api/groups/" + publicGroupId + "/posts/" + groupPost.get("id").asLong() + "/comments",
                creatorToken, Map.of("body", "Good point"));
        mvc.perform(get("/api/trails/{slug}", "technology-watch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityPosts[0].title").value("What this means"));

        JsonNode privateGroup = apiPost("/api/groups", creatorToken, Map.of(
                "name", "Private Review Group",
                "visibility", "PRIVATE",
                "joinPolicy", "REQUEST",
                "topics", new String[0],
                "rules", "Members only"));
        long privateGroupId = privateGroup.get("id").asLong();
        JsonNode privatePost = apiPost("/api/groups/" + privateGroupId + "/posts", creatorToken, Map.of(
                "title", "Private discussion",
                "body", "Visible only after joining",
                "trailTitle", "Private Watch"));
        assertThat(privatePost.path("trail").path("slug").asText()).isEqualTo("private-watch");
        mvc.perform(get("/api/trails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.slug == 'private-watch')]").doesNotExist());
        mvc.perform(get("/api/trails/{slug}", "private-watch"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/groups/{id}", privateGroupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts").isEmpty())
                .andExpect(jsonPath("$.members").isEmpty());
        mvc.perform(post("/api/groups/{id}/join-requests", privateGroupId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinRequestPending").value(true));
        JsonNode requestView = getJson("/api/groups/" + privateGroupId, creatorToken);
        long requestId = requestView.path("joinRequests").get(0).path("id").asLong();
        mvc.perform(patch("/api/groups/{id}/join-requests/{requestId}", privateGroupId, requestId)
                        .header("Authorization", bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(2));
        mvc.perform(get("/api/groups/{id}", privateGroupId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
        long approvedRequestId = jdbc.queryForObject(
                "SELECT id FROM group_join_requests WHERE group_id = ?", Long.class, privateGroupId);
        mvc.perform(delete("/api/groups/{id}", privateGroupId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_join_requests WHERE id = ?",
                Integer.class, approvedRequestId)).isZero();

        JsonNode pendingDeletionGroup = apiPost("/api/groups", creatorToken, Map.of(
                "name", "Delete With Pending Request",
                "visibility", "PRIVATE",
                "joinPolicy", "REQUEST"));
        long pendingGroupId = pendingDeletionGroup.get("id").asLong();
        mvc.perform(post("/api/groups/{id}/join-requests", pendingGroupId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        long pendingRequestId = jdbc.queryForObject(
                "SELECT id FROM group_join_requests WHERE group_id = ?", Long.class, pendingGroupId);
        mvc.perform(delete("/api/groups/{id}", pendingGroupId)
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_join_requests WHERE id = ?",
                Integer.class, pendingRequestId)).isZero();

        JsonNode inviteOnly = apiPost("/api/groups", creatorToken, Map.of(
                "name", "Invite Only Readers",
                "visibility", "PRIVATE",
                "joinPolicy", "INVITE_ONLY"));
        mvc.perform(post("/api/groups/{id}/join-requests", inviteOnly.get("id").asLong())
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/groups/{id}", publicGroupId)
                        .header("Authorization", bearer(creatorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "visibility", "PRIVATE",
                                "joinPolicy", "REQUEST",
                                "description", "Updated",
                                "topics", new String[]{"technology"},
                                "rules", "No abuse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));

        mvc.perform(post("/api/friends/{userId}", member.get("userId").asLong())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationship").value("OUTGOING_REQUEST"));
        mvc.perform(get("/api/friends/search").param("q", "groupmember")
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handle").value(member.get("handle").asText()));
        mvc.perform(post("/api/friends/{userId}/accept", creator.get("userId").asLong())
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relationship").value("FRIEND"));
        mvc.perform(get("/api/friends").header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationship").value("FRIEND"));
        mvc.perform(delete("/api/friends/{userId}", member.get("userId").asLong())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/groups/{id}", publicGroupId)
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/groups/{id}/posts/{postId}", publicGroupId, groupPost.get("id").asLong())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/groups/{id}/members/{userId}", publicGroupId, member.get("userId").asLong())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/groups/{id}", inviteOnly.get("id").asLong())
                        .header("Authorization", bearer(creatorToken)))
                .andExpect(status().isOk());
    }

    @Test
    void administratorsCanModerateUsersPostsCommentsAndSponsorships() throws Exception {
        JsonNode admin = register("platformadmin");
        JsonNode author = register("moderatedauthor");
        JsonNode reader = register("moderatedreader");
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.get("userId").asLong());
        entityManager.clear();
        String adminToken = token(login("platformadmin"));
        String authorToken = token(author);
        String readerToken = token(reader);

        JsonNode draft = apiPost("/api/posts", authorToken, Map.of(
                "title", "Moderation Test Draft",
                "content", "Draft content",
                "tags", new String[]{"moderation"},
                "publish", false));
        mvc.perform(get("/api/admin/posts").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(draft.get("id").asLong()));
        mvc.perform(get("/api/admin/posts").header("Authorization", bearer(authorToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/posts/{id}/publish", draft.get("id").asLong())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(post("/api/admin/posts/{id}/archive", draft.get("id").asLong())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        JsonNode comment = apiPost("/api/comments/post/" + draft.get("id").asLong(), readerToken,
                Map.of("body", "Please review this"));
        mvc.perform(post("/api/comments/{id}/flag", comment.get("id").asLong())
                        .header("Authorization", bearer(readerToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/comments/flagged").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(comment.get("id").asLong()));
        mvc.perform(patch("/api/admin/comments/{id}/flag", comment.get("id").asLong())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("flagged", false))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/users").param("q", "moderatedreader")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].handle").value(reader.get("handle").asText()));
        mvc.perform(patch("/api/admin/users/{id}/role", reader.get("userId").asLong())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("role", "READER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("READER"));

        LocalDate today = LocalDate.now();
        Map<String, Object> sponsorship = Map.of(
                "brandName", "Test Publisher",
                "headline", "A test placement",
                "targetUrl", "https://example.org",
                "placement", "NEWS_TICKER",
                "status", "ACTIVE",
                "startDate", today.toString(),
                "endDate", today.plusDays(1).toString());
        JsonNode deal = apiPost("/api/admin/sponsorships", adminToken, sponsorship);
        mvc.perform(get("/api/sponsorships/active").param("placement", "NEWS_TICKER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brandName").value("Test Publisher"));
        mvc.perform(post("/api/sponsorships/{id}/click", deal.get("id").asLong()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/sponsorships").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clicks").value(1));
        mvc.perform(put("/api/admin/sponsorships/{id}", deal.get("id").asLong())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "brandName", "Updated Publisher",
                                "headline", "Updated placement",
                                "targetUrl", "https://example.org/updated",
                                "placement", "NEWS_TICKER",
                                "status", "PAUSED",
                                "startDate", today.toString(),
                                "endDate", today.plusDays(1).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
        mvc.perform(delete("/api/admin/sponsorships/{id}", deal.get("id").asLong())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/posts/{id}", draft.get("id").asLong())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/posts/{slug}", draft.get("slug").asText()))
                .andExpect(status().isNotFound());

        JsonNode groupWithRequests = apiPost("/api/groups", authorToken, Map.of(
                "name", "Admin Delete Group Requests",
                "visibility", "PRIVATE",
                "joinPolicy", "REQUEST"));
        long groupId = groupWithRequests.get("id").asLong();
        mvc.perform(post("/api/groups/{id}/join-requests", groupId)
                        .header("Authorization", bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        long groupRequestId = jdbc.queryForObject(
                "SELECT id FROM group_join_requests WHERE group_id = ?", Long.class, groupId);
        mvc.perform(delete("/api/groups/{id}", groupId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_join_requests WHERE id = ?",
                Integer.class, groupRequestId)).isZero();

        JsonNode userDeletionGroup = apiPost("/api/groups", authorToken, Map.of(
                "name", "Delete User Request Cleanup",
                "visibility", "PRIVATE",
                "joinPolicy", "REQUEST"));
        long userRequestGroupId = userDeletionGroup.get("id").asLong();
        mvc.perform(post("/api/groups/{id}/join-requests", userRequestGroupId)
                        .header("Authorization", bearer(readerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        long userRequestId = jdbc.queryForObject(
                "SELECT id FROM group_join_requests WHERE group_id = ?", Long.class, userRequestGroupId);
        mvc.perform(delete("/api/admin/users/{id}", reader.get("userId").asLong())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
        entityManager.flush();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_join_requests WHERE id = ?",
                Integer.class, userRequestId)).isZero();
    }

    private JsonNode register(String prefix) throws Exception {
        int number = USER_NUMBER.incrementAndGet();
        return apiPost("/api/auth/register", null, Map.of(
                "displayName", prefix + " display",
                "handle", prefix + number,
                "email", prefix + number + "@example.test",
                "password", "password123"));
    }

    private JsonNode login(String handlePrefix) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM users WHERE handle LIKE ?",
                String.class, handlePrefix + "%");
        return apiPost("/api/auth/login", null, Map.of("email", email, "password", "password123"));
    }

    private JsonNode apiPost(String path, String token, Object payload) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .contentType(MediaType.APPLICATION_JSON).content(body(payload));
        if (token != null) request.header("Authorization", bearer(token));
        return json.readTree(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String token(JsonNode response) {
        return response.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String body(Object payload) throws Exception {
        return json.writeValueAsString(payload);
    }
}
