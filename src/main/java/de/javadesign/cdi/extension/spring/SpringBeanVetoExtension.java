package de.javadesign.cdi.extension.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.context.ContextLoader;

import de.javadesign.cdi.extension.spring.context.ApplicationContextProvider;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.text.MessageFormat;

/**
 * This CDI Extension observes ProcessAnnotatedType events fired for every found bean during bean
 * discovery phase to prevent Spring Beans from being deployed again if found by the CDI XML or
 * class path scanner.
 *
 */
public class SpringBeanVetoExtension implements Extension {

    private static final Logger LOG = LoggerFactory.getLogger("de.javadesign.cdi.extension");

    private boolean applicationContextFound = false;
    private boolean initialized = false;

    private ConfigurableListableBeanFactory beanFactory;

    public SpringBeanVetoExtension() {
        LOG.info(MessageFormat.format("{0} created.", this.getClass().getSimpleName()));        
    }

    public void vetoSpringBeans(@Observes ProcessAnnotatedType event) {
        if (!initialized) {
            init();
        }
        if (!applicationContextFound) {
            return;
        }
        if (0 != beanFactory.getBeanNamesForType(event.getAnnotatedType().getJavaClass()).length) {
            event.veto();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Vetoing " + event.getAnnotatedType().getJavaClass().getCanonicalName());
            }
        }
    }

    public void init() {
        initialized = true;
        AbstractApplicationContext applicationContext = (AbstractApplicationContext) ContextLoader
                .getCurrentWebApplicationContext();

        if (applicationContext==null) {
            LOG.warn("No Web Spring-ApplicationContext found, try to resolve via application context provider.");
            applicationContext = (AbstractApplicationContext) ApplicationContextProvider.getApplicationContext();
        }

        if (null != applicationContext) {
            LOG.info("ApplicationContext found.");
            applicationContextFound = true;
            beanFactory = applicationContext.getBeanFactory();
        } else {
            LOG.warn("No Spring-ApplicationContext found.");
        }
    }

}
