/**
 * 
 */
package org.brekka.pegasus.core.dao;

import java.util.List;
import java.util.UUID;

import org.brekka.commons.persistence.dao.EntityDAO;
import org.brekka.pegasus.core.model.Deposit;
import org.brekka.pegasus.core.model.Inbox;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
public interface DepositDAO extends EntityDAO<UUID, Deposit> {

    /**
     * @param inbox
     * @return
     */
    List<Deposit> retrieveByInbox(Inbox inbox);

}