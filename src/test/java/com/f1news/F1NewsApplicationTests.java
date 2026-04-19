package com.f1news;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "vapid.public.key=",
    "vapid.private.key="
})
class F1NewsApplicationTests {

    @Test
    void contextLoads() {
    }
}
