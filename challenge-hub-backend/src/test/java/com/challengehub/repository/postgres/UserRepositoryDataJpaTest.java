package com.challengehub.repository.postgres;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.challengehub.config.JpaAuditConfig;
import com.challengehub.entity.postgres.Enums;
import com.challengehub.entity.postgres.UserEntity;

@DataJpaTest
@Testcontainers
@Import(JpaAuditConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.baseline-version=1"
})
class UserRepositoryDataJpaTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("challengehub_test")
            .withUsername("admin")
            .withPassword("test123");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindByEmail() {
        UserEntity user = new UserEntity();
        user.setUsername("jpa_tester");
        user.setEmail("jpa_tester@example.com");
        user.setPassword("$2a$10$dummyhashedpassword");
        user.setRole(Enums.UserRole.USER);
        user.setStatus(Enums.UserStatus.ACTIVE);

        userRepository.save(user);

        assertThat(userRepository.findByEmail("jpa_tester@example.com")).isPresent();
    }
}
