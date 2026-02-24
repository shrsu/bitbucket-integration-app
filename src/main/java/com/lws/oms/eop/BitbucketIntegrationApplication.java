package com.lws.oms.eop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.groovy.template.GroovyTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(exclude = {
    UserDetailsServiceAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    GroovyTemplateAutoConfiguration.class})
public class BitbucketIntegrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(BitbucketIntegrationApplication.class, args);
  }

}
