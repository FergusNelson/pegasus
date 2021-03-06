/**
 * 
 */
package org.brekka.pegasus.core.services;

import org.brekka.pegasus.core.model.DomainName;
import org.brekka.pegasus.core.model.EMailAddress;
import org.brekka.pegasus.core.model.Member;

/**
 * Operations for action on e-mail addresses
 * 
 * @author Andrew Taylor (andrew@brekka.org)
 */
public interface EMailAddressService {

    /**
     * @param email
     * @return
     */
    EMailAddress createEMail(String email, Member owner, boolean requiresVerification);
    
    

    /**
     * Retrieve or create a domain name entry
     * @param domain
     * @return
     */
    DomainName toDomainName(String domain);

    /**
     * @param recipientEMail
     * @return
     */
    EMailAddress retrieveByAddress(String recipientEMail);
}
