package com.digitalWallet.user_service.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

//@Configuration
public class WebClientConfig {
    //@Bean(name = "webClient")
    public WebClient.Builder webClient(ObservationRegistry observationRegistry){
        return WebClient.builder().observationRegistry(observationRegistry);
    }
}
