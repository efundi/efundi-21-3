package edu.nwu.sakaistudentlink.services;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * author: Jaco Gillman
 * 
 */
@Configuration
@PropertySource("classpath:slink.properties")
public class SettingsProperties {

	private ConfigurableEnvironment env;
	
	public void init() {	
    	AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(SettingsProperties.class);
    	env = context.getEnvironment();
	}

    public String getProperty(String key) {
        return env.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }    
}