package com.project4.TaskManager.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloud.name}")
    private String Cloud_Name;
    @Value("${cloud.key}")
    private String Api_Key;
    @Value("${cloud.secret}")
    private String Api_Secret;

    @Bean
    public Cloudinary cloudinary(){
        Map<String,String> config=new HashMap<>();
        config.put("cloud_name",Cloud_Name);
        config.put("api_key",Api_Key);
        config.put("api_secret",Api_Secret);
        return new Cloudinary(config);
    }

}
