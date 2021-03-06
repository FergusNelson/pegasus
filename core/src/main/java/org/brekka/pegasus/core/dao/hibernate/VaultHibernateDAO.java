/**
 * 
 */
package org.brekka.pegasus.core.dao.hibernate;

import java.util.List;

import org.brekka.pegasus.core.dao.VaultDAO;
import org.brekka.pegasus.core.model.Member;
import org.brekka.pegasus.core.model.Vault;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 */
@Repository
public class VaultHibernateDAO extends AbstractPegasusHibernateDAO<Vault> implements VaultDAO {

    /* (non-Javadoc)
     * @see org.brekka.commons.persistence.dao.impl.AbstractIdentifiableEntityHibernateDAO#type()
     */
    @Override
    protected Class<Vault> type() {
        return Vault.class;
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.dao.VaultDAO#retrieveForUser(org.brekka.pegasus.core.model.Member)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Vault> retrieveForMember(Member member) {
        return getCurrentSession().createCriteria(Vault.class)
                .add(Restrictions.eq("owner", member))
                .list();
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.dao.VaultDAO#retrieveBySlug(java.lang.String, org.brekka.pegasus.core.model.Member)
     */
    @Override
    public Vault retrieveBySlug(String vaultSlug, Member member) {
        return (Vault) getCurrentSession().createCriteria(Vault.class)
                .add(Restrictions.eq("slug", vaultSlug))
                .add(Restrictions.eq("owner", member))
                .uniqueResult();
    }

}
