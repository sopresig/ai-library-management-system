package com.dhitha.lms.clientbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "lms.ai")
public class AiModelProperties {

  private String baseUrl = "https://api.xiaomimimo.com/v1";
  private String apiKey;
  private String model = "mimo-v2.5-pro";
  private boolean enabled = true;
  private String lmStudioBaseUrl = "http://localhost:1234/v1";
  private String lmStudioModel = "qwen3.5-27b";
  private boolean lmStudioEnabled = true;
}
