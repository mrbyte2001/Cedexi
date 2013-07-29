/*
 * Copyright (C) 2010 Dirk Dreyer.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to
 * in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package de.javadesign.cdi.extension.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.ContextLoader;

import de.javadesign.cdi.extension.spring.context.ApplicationContextProvider;

/**
 * CDI Extension to integrate Spring-Beans from Spring-Application-Context. <br />
 * ATTENTION: Add Spring ApplicationContextListener-Config in the web.xml before any CDI-Implementation-Listener-Config.
 * 
 * @author Dirk Dreyer
 */
public class SpringBeanIntegrationExtension implements Extension {

    private static final Logger LOGGER = LoggerFactory.getLogger("de.javadesign.cdi.extension");

    public static final String METHOD_NAME_GET_OBJECT_TYPE = "getObjectType";

    private static final Bean<?> CLASS_NOT_FOUND = null;

    /**
     * Default Constructor.
     */
    public SpringBeanIntegrationExtension() {
        LOGGER.info(MessageFormat.format("{0} created.", this.getClass().getSimpleName()));
    }

    /**
     * Listener method observes the AfterBeanDiscovery-Event.
     * 
     * @param event
     *            the AfterBeanDiscovery Event
     * @param beanManager
     *            the BeanManager
     */
    public void connectCdiAndSpring(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {

        AbstractApplicationContext applicationContext = (AbstractApplicationContext) ContextLoader
                .getCurrentWebApplicationContext();
        
        if (applicationContext==null) {
            LOGGER.warn("No Web Spring-ApplicationContext found, try to resolve via application context provider.");
            applicationContext = (AbstractApplicationContext) ApplicationContextProvider.getApplicationContext();
        }

        if (applicationContext != null) {
            LOGGER.info("ApplicationContext found.");
            registerBeans(applicationContext, event, beanManager);
        } else {
            LOGGER.warn("No Spring-ApplicationContext found.");
        }
    }

    /**
     * Register found beans from the application context.
     * 
     * @param applicationContext
     *            the spring application context
     * @param event
     *            the AfterBeanDiscoveryEvent
     */
    private void registerBeans(final AbstractApplicationContext applicationContext, final AfterBeanDiscovery event,
            final BeanManager beanManager) {
        final String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        final ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        Bean<?> springBean = null;
        for (final String beanName : beanDefinitionNames) {
            final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            springBean = createBean(beanName, beanDefinition, beanFactory, beanManager);

            if (springBean != null) {
                LOGGER.debug("Register Bean: {}.", springBean);
                event.addBean(springBean);
            }
        }

    }

    /**
     * Try to create a bean if class is found.
     * 
     * @param beanName
     *            the bean name to find and create
     * @param beanDefinition
     *            the spring bean definition
     * @param beanManager
     *            the injection bean manager
     * @param beanFactory
     *            a ConfigurableListableBeanFactory
     * @return the created bean or null if class was not found
     */
    private Bean<?> createBean(final String beanName, final BeanDefinition beanDefinition,
            final ConfigurableListableBeanFactory beanFactory, final BeanManager beanManager) {
        Class<?> beanClass = null;

        try {
            if (null == beanDefinition.getBeanClassName()) {
                // TODO: Support beans which are created via factory bean.
                LOGGER.warn("Ignored bean with name {} - there is no definition via bean's class name available", beanName);
                return CLASS_NOT_FOUND;
            }
            beanClass = Class.forName(beanDefinition.getBeanClassName());

            // FactoryBean? Bean class is returned by getObjectType method
            for (Class<?> beanClassInterface : beanClass.getInterfaces()) {
                if (!beanClassInterface.equals(FactoryBean.class)) {
                    continue;
                }
                try {
                    Method getObjectTypeMethod = beanClass.getDeclaredMethod(METHOD_NAME_GET_OBJECT_TYPE);
                    String s = ((ParameterizedType) getObjectTypeMethod.getGenericReturnType())
                            .getActualTypeArguments()[0].toString();
                    beanClass = Class.forName(s.substring(s.lastIndexOf(" ")+1));

                } catch (NoSuchMethodException ignored) {
                    LOGGER.warn("Ignored bean {} with Class {} that is assumed to be a FactoryBean, " +
                            "but has no getObjectType method.", beanName, beanClass);
                    return CLASS_NOT_FOUND;
                }
                break;
            }

        } catch (final ClassNotFoundException e) {
            LOGGER.warn("Class {} not found.", beanName);
            return CLASS_NOT_FOUND;
        }

        final AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(beanClass);
        final Set<Type> beanTypes = annotatedType.getTypeClosure();
        final Set<Annotation> qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Any>() {

            private static final long serialVersionUID = 1L;
            // any implementation
        });
        qualifiers.add(new AnnotationLiteral<Default>() {

            private static final long serialVersionUID = 1L;
            // default implementation
        });

        final Set<Class<? extends Annotation>> stereotypes = new HashSet<Class<? extends Annotation>>();

        for (final Annotation annotation : annotatedType.getAnnotations()) {
            if (beanManager.isQualifier(annotation.annotationType())) {
                qualifiers.add(annotation);
            }
            if (beanManager.isStereotype(annotation.annotationType())) {
                stereotypes.add(annotation.annotationType());
            }
        }

        return new SpringBean(beanName, beanClass, beanTypes, qualifiers, stereotypes, beanFactory);
    }
}
