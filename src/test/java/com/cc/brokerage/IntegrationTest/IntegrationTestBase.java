package com.cc.brokerage.IntegrationTest;

import com.cc.brokerage.adapter.in.web.ApiBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTestBase {

    protected static final String BASE_PATH = ApiBase.PATH;
    protected static final String API_V1 = BASE_PATH + "/v1";

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.6")
            .withDatabaseName("brokerage")
            .withUsername("brokerage")
            .withPassword("brokerage")
            .withInitScript("db/database.sql");

    public static final LocalStackContainer LOCALSTACK = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.0"));

    static {
        POSTGRES.start();
        LOCALSTACK.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void resetOrders() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("db/reset-orders.sql"));
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
