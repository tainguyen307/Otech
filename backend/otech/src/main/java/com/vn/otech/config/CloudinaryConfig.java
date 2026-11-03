package com.vn.otech.config;

import com.cloudinary.Cloudinary;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

@Configuration
public class CloudinaryConfig {
    @Bean
    @ConditionalOnExpression("\'${cloudinary.cloud-name:}\' != \'\' && \'${cloudinary.api-key:}\' != \'\' && \'${cloudinary.api-secret:}\' != \'\'")
    Cloudinary cloudinary(@Value("${cloudinary.cloud-name}") String cloudName,
                          @Value("${cloudinary.api-key}") String apiKey,
                          @Value("${cloudinary.api-secret}") String apiSecret) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName.trim());
        config.put("api_key", apiKey.trim());
        config.put("api_secret", apiSecret.trim());
        return new Cloudinary(config);
    }
}
