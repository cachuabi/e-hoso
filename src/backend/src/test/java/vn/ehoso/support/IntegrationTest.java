package vn.ehoso.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import vn.ehoso.account.domain.Role;
import vn.ehoso.account.domain.User;
import vn.ehoso.account.domain.UserStatus;
import vn.ehoso.account.repository.UserRepository;
import vn.ehoso.notification.service.NotificationDispatcher;

/** Boots the application against a real PostgreSQL 17 (Testcontainers) with helpers. */
@SpringBootTest(properties = "ehoso.notification.delay-ms=86400000")
@AutoConfigureMockMvc
@Import(TestBeans.class)
public abstract class IntegrationTest {
  protected static final String PASSWORD = "Matkhau123";
  protected static final byte[] PDF = "%PDF-1.7 fake content".getBytes();

  private static final String EXTERNAL_DB = System.getenv("TEST_DB_URL");
  private static final PostgreSQLContainer<?> POSTGRES =
      EXTERNAL_DB != null ? null : new PostgreSQLContainer<>("postgres:17-alpine");

  static {
    if (POSTGRES != null) {
      POSTGRES.start();
    }
  }

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;
  @Autowired protected UserRepository users;
  @Autowired protected PasswordEncoder encoder;
  @Autowired protected TestBeans.MutableClock clock;
  @Autowired protected TestBeans.MemoryStorage storage;
  @Autowired protected TestBeans.FakeMail mail;
  @Autowired protected NotificationDispatcher dispatcher;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    if (POSTGRES != null) {
      registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
      registry.add("spring.datasource.username", POSTGRES::getUsername);
      registry.add("spring.datasource.password", POSTGRES::getPassword);
    } else {
      registry.add("spring.datasource.url", () -> EXTERNAL_DB);
    }
  }

  /** Creates an active user directly in the database. */
  protected User user(Role role) {
    User user = new User();
    String tag = UUID.randomUUID().toString().replace("-", "");
    user.setFullName("Người " + role.name());
    user.setEmail(tag + "@test.local");
    user.setRole(role);
    user.setStatus(UserStatus.ACTIVE);
    user.setPasswordHash(encoder.encode(PASSWORD));
    if (role == Role.CITIZEN) {
      long digits = Math.abs(UUID.randomUUID().getMostSignificantBits()) % 1_000_000_000_000L;
      user.setNationalId(String.format("%012d", digits));
      user.setPhone("0900000000");
    }
    return users.save(user);
  }

  /** Signs in and returns the bearer header value. */
  protected String login(User user) throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("email", user.getEmail(), "password", PASSWORD))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + json.readTree(body).get("accessToken").asText();
  }

  /** Serializes to JSON. */
  protected String toJson(Object value) throws Exception {
    return json.writeValueAsString(value);
  }

  /** Parses the response body. */
  protected JsonNode read(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  /** Sends a GET. */
  protected ResultActions doGet(String token, String url) throws Exception {
    return mvc.perform(auth(get(url), token));
  }

  /** Sends a JSON POST. */
  protected ResultActions doPost(String token, String url, Object body) throws Exception {
    return mvc.perform(
        auth(post(url), token).contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
  }

  /** Sends a multipart upload. */
  protected ResultActions upload(
      String token, String dossierId, String kind, String componentId, String name, byte[] data)
      throws Exception {
    var request =
        multipart("/api/v1/dossiers/" + dossierId + "/attachments")
            .file(new MockMultipartFile("file", name, null, data))
            .param("kind", kind);
    if (componentId != null) {
      request.param("componentId", componentId);
    }
    return mvc.perform(token == null ? request : request.header("Authorization", token));
  }

  /** Creates a complete draft of a procedure with its required files, not yet submitted. */
  protected String draft(String token, String code) throws Exception {
    JsonNode procedure = read(doGet(null, "/api/v1/procedures/" + code));
    JsonNode created =
        read(
            doPost(
                token,
                "/api/v1/dossiers",
                Map.of("procedureCode", code, "formData", validForm(code))));
    String id = created.at("/summary/id").asText();
    for (JsonNode component : procedure.get("components")) {
      if ("REQUIRED".equals(component.get("requirement").asText())) {
        upload(token, id, "DOSSIER", component.get("id").asText(), "tep.pdf", PDF);
      }
    }
    return id;
  }

  /** Returns valid form data of a procedure. */
  protected Map<String, Object> validForm(String code) {
    return switch (code) {
      case "CHUNG_THUC" -> Map.of("hoTen", "Nguyễn Văn A", "tenGiayTo", "Bằng", "soBan", 2);
      case "KHAI_SINH" ->
          Map.of(
              "hoTenTre", "Bé A",
              "ngaySinh", "2026-10-01",
              "gioiTinh", "Nam",
              "noiSinh", "Hà Nội",
              "hoTenMe", "Trần Thị B");
      default ->
          Map.of(
              "hoTen", "Lê Văn C",
              "ngaySinh", "1990-01-01",
              "tinhTrang", "Chưa kết hôn",
              "mucDich", "Kết hôn");
    };
  }

  /** Reads the current version of a dossier. */
  protected long version(String token, String url) throws Exception {
    return read(doGet(token, url)).get("version").asLong();
  }

  private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
    return token == null ? builder : builder.header("Authorization", token);
  }
}
