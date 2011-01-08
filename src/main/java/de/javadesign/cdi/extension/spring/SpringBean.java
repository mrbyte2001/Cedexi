/*
 * Copyright (C) 2010 Dirk Dreyer.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License. * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to
 * in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package de.javadesign.cdi.extension.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * A SpringBean object which implement Bean<?> from injection spi.
 * 
 * @author Dirk Dreyer
 */
public class SpringBean implements Bean<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger("de.javadesign.cdi.extension");

    private String beanName;

    private Class<?> beanClass;

    private Set<Type> beanTypes;

    private Set<Annotation> qualifiers;

    private Set<Class<? extends Annotation>> stereotypes;

    private ConfigurableBeanFactory beanFactory;

    /**
     * Constructor.
     * 
     * @param beanName
     *            the bean name
     * @param beanClass
     *            the bean class
     * @param beanTypes
     *            the bean types
     * @param qualifiers
     *            the qualifiers
     * @param stereotypes
     *            the stereotypes
     * @param beanFactory
     *            the beanfactory
     */
    public SpringBean(final String beanName, final Class<?> beanClass, final Set<Type> beanTypes,
            final Set<Annotation> qualifiers, final Set<Class<? extends Annotation>> stereotypes,
            final ConfigurableBeanFactory beanFactory) {
        this.beanName = beanName;
        this.beanClass = beanClass;
        this.beanTypes = Collections.unmodifiableSet(beanTypes);
        this.qualifiers = Collections.unmodifiableSet(qualifiers);
        this.stereotypes = Collections.unmodifiableSet(stereotypes);
        this.beanFactory = beanFactory;
    }

    /**
     * Call the spring bean factory to get the bean.
     */
    @Override
    public Object create(final CreationalContext<Object> context) {
        LOGGER.info(MessageFormat.format("Create bean: {0}", this.beanName));

        return this.beanFactory.getBean(this.beanName);

    }

    /**
     * Call the spring bean factory to destroy the bean.
     */
    @Override
    public void destroy(final Object instance, final CreationalContext<Object> context) {
        LOGGER.info(MessageFormat.format("Destroy Bean: {0}", this.beanName));
        final boolean isPrototype = this.beanFactory.isPrototype(this.beanName);

        if (isPrototype) {
            this.beanFactory.destroyBean(this.beanName, instance);
            context.release();
        }
    }

    @Override
    public Set<Type> getTypes() {

        return this.beanTypes;
    }

    @Override
    public Set<Annotation> getQualifiers() {

        return this.qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {

        return Dependent.class;
    }

    @Override
    public String getName() {

        return this.beanName;
    }

    @Override
    public boolean isNullable() {

        return true;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {

        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass() {

        return this.beanClass;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {

        return this.stereotypes;
    }

    @Override
    public boolean isAlternative() {

        return false;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("SpringBean [");
        if (this.beanName != null) {
            builder.append("beanName=");
            builder.append(this.beanName);
            builder.append(", ");
        }
        if (this.beanClass != null) {
            builder.append("beanClass=");
            builder.append(this.beanClass);
            builder.append(", ");
        }
        if (this.beanTypes != null) {
            builder.append("beanTypes=");
            builder.append(this.beanTypes);
            builder.append(", ");
        }
        if (this.qualifiers != null) {
            builder.append("qualifiers=");
            builder.append(this.qualifiers);
            builder.append(", ");
        }
        if (this.stereotypes != null) {
            builder.append("stereotypes=");
            builder.append(this.stereotypes);
            builder.append(", ");
        }
        if (this.beanFactory != null) {
            builder.append("beanFactory=");
            builder.append(this.beanFactory);
        }
        builder.append("]");
        return builder.toString();
    }

}
