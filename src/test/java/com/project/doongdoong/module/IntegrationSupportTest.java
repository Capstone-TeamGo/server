package com.project.doongdoong.module;

import com.project.doongdoong.domain.voice.service.VoiceService;
import com.project.doongdoong.global.util.WebClientUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public abstract class IntegrationSupportTest {

    @MockBean
    protected WebClientUtil webClientUtil;
}
