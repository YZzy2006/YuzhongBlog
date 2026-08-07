# YuzhonBlog - Project Guidelines

## Project Overview
Spring Boot 4.1.0 + Vue 3 blog backend with JPA/MySQL, JWT auth, and SPA frontend.

## Architecture
- **Backend**: `com.ticketingsystem.yuzhonblog` - Controller -> Service -> Repository -> Entity
- **Frontend**: `frontend/` - Vue 3 + Vite, builds into `src/main/resources/static/`
- **Database**: MySQL 8, JPA/Hibernate with ddl-auto=update in dev

## Build & Run
```bash
# Backend only
mvn spring-boot:run

# Frontend only (dev server)
cd frontend && npm run dev

# Full build (frontend -> static -> JAR)
cd frontend && npm run build && cd .. && mvn package
```

## Testing Conventions

### Naming
- **Unit tests**: `MethodName_Scenario_ExpectedResult` (e.g., `getBySlug_NonExistentSlug_ThrowsBusinessException`)
- **Integration tests**: Class name ends with `IntegrationTest` suffix (e.g., `ArticleControllerIntegrationTest`)

### Test Profiles
- `test` - H2 in-memory database, used for unit and integration tests locally
- `mysql-ci` - MySQL service container, used in GitHub Actions CI pipeline

### Running Tests
```bash
# All tests (H2 in-memory)
mvn test -Dspring.profiles.active=test

# Unit tests only (no Spring context)
mvn test -Dtest="*Test" -Dspring.profiles.active=test

# Integration tests only (with MySQL)
mvn test -Dtest="*IntegrationTest" -Dspring.profiles.active=test

# With coverage report
mvn test jacoco:report -Dspring.profiles.active=test
# Report at: target/site/jacoco/index.html
```

### Coverage
- **Target**: 70% line coverage minimum
- **Tool**: JaCoCo (report generated at `target/site/jacoco/`)
- **CI**: Coverage report uploaded as artifact on every successful run

### Test Structure
- `src/test/java/.../service/` - Unit tests with `@ExtendWith(MockitoExtension.class)`, mock repositories
- `src/test/java/.../controller/` - Integration tests with `@SpringBootTest` + `@AutoConfigureMockMvc`
- `src/test/resources/application-test.properties` - H2 test config
- `src/test/resources/application-mysql-ci.properties` - CI MySQL test config

### Writing Tests
- Use `@BeforeEach` to clean state (e.g., `repository.deleteAll()`)
- Use `@Mock` + `@InjectMocks` for service unit tests
- Use `MockMvc` for controller integration tests
- Use AssertJ assertions (`assertThat`) over JUnit assertions
- Verify exception types with `assertThatThrownBy().isInstanceOf()`
- Verify mock interactions with `verify()` and `argThat()`

## Key Packages
| Package | Purpose |
|---------|---------|
| `controller/` | Public REST endpoints |
| `controller/admin/` | Admin endpoints (JWT protected) |
| `service/` | Business logic |
| `repository/` | Spring Data JPA repositories |
| `entity/` | JPA entities (extend BaseEntity) |
| `dto/` | Request/Response DTOs |
| `common/` | ApiResponse, ErrorCode, BusinessException, BaseEntity |
| `config/` | SecurityConfig, WebConfig, SpaWebConfig |
| `security/` | JwtAuthenticationFilter, LoginRateLimiter |
| `util/` | JwtUtil |

## Security Model
- 3 filter chains (ordered): login (rate-limited) -> admin (JWT required) -> default (permit all)
- Public API: `/api/articles/**`, `/api/categories/**`, `/api/tags/**`, `/api/projects/**`, `/api/site/**`
- Admin API: `/admin/**` requires JWT Bearer token
- Stateless sessions (no cookies)
