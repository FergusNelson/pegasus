/**
 * 
 */
package org.brekka.pegasus.core.dao;

import java.util.List;
import java.util.UUID;

import org.brekka.commons.persistence.dao.EntityDAO;
import org.brekka.pegasus.core.model.Associate;
import org.brekka.pegasus.core.model.Enlistment;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
public interface EnlistmentDAO extends EntityDAO<UUID, Enlistment> {

    /**
     * @param organization
     * @param associate
     * @return
     */
    List<Enlistment> retrieveForAssociate(Associate associate);

}
