package com.alhilal.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();

            // normalize IPv6 localhost to IPv4
            // ::1 and 0:0:0:0:0:0:0:1 are both IPv6 for localhost
            if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
                ip = "127.0.0.1";
            }

            System.out.println("[RATE LIMITER] Request from IP: " + ip);
            return Mono.just(ip);
        };
    }
}