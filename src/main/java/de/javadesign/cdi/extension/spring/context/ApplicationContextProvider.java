package de.javadesign.cdi.extension.spring.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;


/**
 * Provides Spring's application context in the case of using a non-servlet initialization.
 * This class should be considered in Spring's component scan configuration when the application
 * context is initialized by a setup which does not depend on a servlet container.
 *
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext = null;
    
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

}
