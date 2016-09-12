/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tomitribe.tribestream.registryng.entities;


import org.tomitribe.tribestream.registryng.security.LoginContext;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.NamingException;
import java.util.Set;

/**
 * Invoked by Hibernate Envers every time a new revision is created.
 */
public class RevisionListener implements org.hibernate.envers.RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {

        try {
            Revision revision = Revision.class.cast(revisionEntity);

            final String username = getManagedBean(LoginContext.class).getUsername();
            revision.setUsername(username);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Programmatically looks up the CDI managed bean of the given type, as unfortunately the RevisionListener is not
     * created by CDI.
     * @param clazz The requested bean type
     * @param <T>
     * @return The requested bean
     * @throws NamingException if the BeanManager is not available
     */
    private <T> T getManagedBean(Class<T> clazz) throws NamingException {
        // RevisionListener is not instantiated as a CDI managed bean, so we
        // have to do a programmatic lookup of the LoginContext.
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        Bean<?> bean = beanManager.resolve(beans);
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, clazz, ctx);
    }

}
