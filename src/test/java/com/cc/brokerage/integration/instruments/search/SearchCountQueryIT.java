package com.cc.brokerage.integration.instruments.search;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The total of a page comes from a second query with the same LIKE. It only runs when the page alone cannot tell
 * the total: a full page, or a page past the end.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.cc.brokerage.integration.instruments.search.SearchCountQueryIT$CountRecorder")
class SearchCountQueryIT extends IntegrationTestBase {

    @BeforeEach
    void forgetEarlierQueries() {
        CountRecorder.COUNTS.clear();
    }

    @Test
    void aFirstPageThatIsNotFullSkipsTheCount() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        assertThat(CountRecorder.COUNTS).isEmpty();
    }

    @Test
    void aLastPageThatIsNotFullSkipsTheCount() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("page", "1").param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        assertThat(CountRecorder.COUNTS).isEmpty();
    }

    @Test
    void aFullPageCountsTheMatches() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        assertThat(CountRecorder.COUNTS).hasSize(1);
    }

    public static class CountRecorder implements StatementInspector {

        static final List<String> COUNTS = new CopyOnWriteArrayList<>();

        @Override
        public String inspect(String sql) {
            String lower = sql.toLowerCase(Locale.ROOT);
            if (lower.contains("count(") && lower.contains("instruments")) {
                COUNTS.add(sql);
            }
            return sql;
        }
    }
}
