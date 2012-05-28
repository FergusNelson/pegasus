/**
 * 
 */
package org.brekka.pegasus.web.pages.direct;

import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionAttribute;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.brekka.pegasus.core.model.AnonymousTransfer;
import org.brekka.pegasus.core.services.AnonymousService;
import org.brekka.pegasus.web.support.Transfers;
import org.brekka.xml.pegasus.v1.model.BundleType;
import org.brekka.xml.pegasus.v1.model.FileType;

/**
 * @author Andrew Taylor
 *
 */
public class FetchDirect {
    
    @InjectPage
    private UnlockDirect unlockPage;
    
    @InjectPage
    private AgreementDirect agreementDirectPage;
    
    @Inject
    private AnonymousService anonymousService;

    @SessionAttribute("transfers")
    private Transfers transfers;
    
    @Property
    private String token;
    
    @Property
    private AnonymousTransfer transfer;
    
    @Property
    private FileType file;
    
    
    Object onActivate(String token) {
        this.token = token;
        
        if (transfers == null) {
            unlockPage.onActivate(token);
            return unlockPage;
        }
        
        transfer = (AnonymousTransfer) transfers.get(token);
        if (transfer == null) {
            unlockPage.onActivate(token);
            return unlockPage;
        }
        BundleType bundleXml = transfer.getBundle().getXml();
        if (bundleXml.isSetAgreement() 
                && !anonymousService.isAccepted(transfer)) {
            agreementDirectPage.init(token);
            return agreementDirectPage;
        }
        return Boolean.TRUE;
    }
    
    void init(String token) {
        this.token = token;
    }
    
    String onPassivate() {
        return token;
    }
    
    public String[] getFileContext() {
        return new String[]{ file.getUUID(), file.getName() };
    }
}
