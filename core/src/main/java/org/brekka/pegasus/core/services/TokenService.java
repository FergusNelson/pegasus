/**
 * 
 */
package org.brekka.pegasus.core.services;

import org.brekka.pegasus.core.model.Token;

/**
 * @author Andrew Taylor
 *
 */
public interface TokenService {

    Token allocateAnonymous();
    
    Token createForInbox(String slug);

    /**
     * @param inboxToken
     * @return
     */
    Token retrieveByPath(String path);
}