/**
 * 
 */
package org.brekka.pegasus.core.services.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.brekka.paveway.core.model.UploadedFiles;
import org.brekka.pegasus.core.dao.DispatchDAO;
import org.brekka.pegasus.core.model.Actor;
import org.brekka.pegasus.core.model.Allocation;
import org.brekka.pegasus.core.model.Dispatch;
import org.brekka.pegasus.core.model.Division;
import org.brekka.pegasus.core.model.EMailAddress;
import org.brekka.pegasus.core.model.Inbox;
import org.brekka.pegasus.core.model.KeySafe;
import org.brekka.pegasus.core.model.Member;
import org.brekka.pegasus.core.model.PegasusTokenType;
import org.brekka.pegasus.core.model.Token;
import org.brekka.pegasus.core.services.AnonymousService;
import org.brekka.pegasus.core.services.DispatchService;
import org.brekka.pegasus.core.services.EMailAddressService;
import org.brekka.pegasus.core.services.InboxService;
import org.brekka.pegasus.core.services.KeySafeService;
import org.brekka.pegasus.core.services.MemberService;
import org.brekka.xml.pegasus.v2.model.AllocationType;
import org.brekka.xml.pegasus.v2.model.BundleType;
import org.brekka.xml.pegasus.v2.model.DetailsType;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
@Service
@Transactional
public class DispatchServiceImpl extends AllocationServiceSupport implements DispatchService {

    @Autowired
    private MemberService memberService;
    
    @Autowired
    private EMailAddressService eMailAddressService;
    
    @Autowired
    private InboxService inboxService;
    
    @Autowired
    private AnonymousService anonymousService;
    
    @Autowired
    private KeySafeService keySafeService;
    
    @Autowired
    private DispatchDAO dispatchDAO;
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.DispatchService#createDispatch(org.brekka.pegasus.core.model.KeySafe, org.brekka.xml.pegasus.v2.model.DetailsType, java.lang.Integer, java.util.List)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Dispatch createDispatch(KeySafe<?> keySafe, DetailsType details, DateTime expires, Integer maxDownloads,
            UploadedFiles files) {
        Dispatch dispatch = new Dispatch();
        AuthenticatedMemberBase<Member> authenticatedMember = AuthenticatedMemberBase.getCurrent(memberService, Member.class);
        Actor activeActor = authenticatedMember.getActiveActor();
        
        BundleType bundleType = completeFiles(0, files);
        
        // Copy the allocation to
        AllocationType allocationType = prepareAllocationType(bundleType, details);
        encryptDocument(dispatch, allocationType, keySafe);
        
        if (keySafe instanceof Division) {
            dispatch.setDivision((Division<?>) keySafe);
        }
        dispatch.setKeySafe(keySafe);
        dispatch.setActor(activeActor);
        dispatch.setExpires(expires.toDate());
        
        Token token = tokenService.generateToken(PegasusTokenType.DISPATCH);
        dispatch.setToken(token);
        
        dispatchDAO.create(dispatch);
        createAllocationFiles(dispatch);
        return dispatch;
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.DispatchService#createDispatchAndAllocate(java.lang.String, org.brekka.pegasus.core.model.Division, org.brekka.pegasus.core.model.KeySafe, org.brekka.xml.pegasus.v2.model.DetailsType, int, java.util.List)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Allocation createDispatchAndAllocate(String recipientEMail, Division<?> division, KeySafe<?> keySafe,
            DetailsType details, DateTime dispatchExpires, DateTime allocationExpires, int maxDownloads, UploadedFiles files) {
        Dispatch dispatch = createDispatch(keySafe, details, dispatchExpires, null, files);
        
        Inbox inbox = null;
        if (StringUtils.isNotBlank(recipientEMail)) {
            EMailAddress address = eMailAddressService.retrieveByAddress(recipientEMail);
            if (address != null) {
                // Known to the system.
                inbox = inboxService.retrieveForEMailAddress(address);
            }
        }
        Allocation allocation;
        if (inbox != null) {
            allocation = inboxService.createDeposit(inbox, null, details, allocationExpires, dispatch);
        } else {
            allocation = anonymousService.createTransfer(details, allocationExpires, maxDownloads, null, dispatch, null);
        }
        return allocation;
    }


    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.DispatchService#retrieveCurrentForInterval(org.joda.time.DateTime, org.joda.time.DateTime)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public List<Dispatch> retrieveCurrentForInterval(KeySafe<?> keySafe, DateTime from, DateTime until) {
        AuthenticatedMemberBase<Member> authenticatedMember = AuthenticatedMemberBase.getCurrent(memberService, Member.class);
        Actor activeActor = authenticatedMember.getActiveActor();
        return dispatchDAO.retrieveForInterval(keySafe, activeActor, from.toDate(), until.toDate());
    }
}
