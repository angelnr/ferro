package com.ferro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.ferro.support.AbstractPostgresIntegrationTest;

@SpringBootTest
class FerroApplicationTests extends AbstractPostgresIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context.getBean(FerroApplication.class)).isNotNull();
    }
}
