/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brekka.pegasus.core.dao.hibernate;

import java.util.List;

import org.brekka.pegasus.core.dao.ConnectionDAO;
import org.brekka.pegasus.core.model.Actor;
import org.brekka.pegasus.core.model.Connection;
import org.brekka.pegasus.core.model.KeySafe;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

/**
 * Connection Hibernate DAO
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
@Repository
public class ConnectionHibernateDAO extends AbstractPegasusHibernateDAO<Connection<?, ?, ?>> implements ConnectionDAO  {

    /* (non-Javadoc)
     * @see org.brekka.commons.persistence.dao.hibernate.AbstractIdentifiableEntityHibernateDAO#type()
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Class type() {
        return Connection.class;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Connection<?, ?, ?>> identifyConnectionsBetween(final KeySafe<?> keySafe, final Actor contextMember) {
        return getCurrentSession().createQuery(
            "select conn " +
            "  from Connection as conn " +
            " join conn.owner as owner " +
            " join conn.source as source " +
            " where conn.target = :target" +
            "   and (owner = :member" +
            "    or owner in (from Associate as assoc where assoc.member = :member)" +
            "    or owner in (select assoc.organization from Associate as assoc where assoc.member = :member))")
            .setEntity("target", keySafe)
            .setEntity("member", contextMember)
            .list();
    }

    @Override
    public Connection<?, ?, ?> retrieveBySurrogate(final Actor owner, final KeySafe<?> source, final KeySafe<?> target) {
        return (Connection<?, ?, ?>) getCurrentSession().createCriteria(type())
                .add(Restrictions.eq("owner", owner))
                .add(Restrictions.eq("source", source))
                .add(Restrictions.eq("target", target))
                .uniqueResult();
    }

    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.dao.ConnectionDAO#retrieveConnectionsByTarget(org.brekka.pegasus.core.model.KeySafe, java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <Target extends KeySafe<?>, T extends Connection<Actor, KeySafe<? extends Actor>, Target>> List<T> retrieveConnectionsByTarget(
            final Target target, final Class<T> expected) {
        return getCurrentSession().createCriteria(expected)
            .add(Restrictions.eq("target", target))
            .list();
    }

    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.dao.ConnectionDAO#deleteForKeySafe(org.brekka.pegasus.core.model.KeySafe)
     */
    @Override
    public void deleteWithSourceKeySafe(final KeySafe<?> keySafe) {
        getCurrentSession().createQuery("delete from Connection where source=:source")
            .setEntity("source", keySafe)
            .executeUpdate();
    }

    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.dao.ConnectionDAO#deleteWithOwner(org.brekka.pegasus.core.model.Actor)
     */
    @Override
    public void deleteWithOwner(final Actor owner) {
        getCurrentSession().createQuery("delete from Connection where owner=:owner")
            .setEntity("owner", owner)
            .executeUpdate();
    }

}
