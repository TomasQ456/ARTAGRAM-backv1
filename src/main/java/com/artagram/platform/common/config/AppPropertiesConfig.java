package com.artagram.platform.common.config;

import com.artagram.platform.auth.EmailProperties;
import com.artagram.platform.auth.JwtProperties;
import com.artagram.platform.canvas.CanvasProperties;
import com.artagram.platform.party.PartyProperties;
import com.artagram.platform.billing.QuotaProperties;
import com.artagram.platform.profile.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        EmailProperties.class,
        StorageProperties.class,
        CanvasProperties.class,
        PartyProperties.class,
        QuotaProperties.class,
        CorsProperties.class,
        com.artagram.platform.ia.IaProperties.class
})
public class AppPropertiesConfig {
}
