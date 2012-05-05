/**
 * 
 */
package org.brekka.pegasus.core.services.impl;

import java.util.List;

import org.brekka.pegasus.core.dao.ProfileDAO;
import org.brekka.pegasus.core.model.AuthenticatedMember;
import org.brekka.pegasus.core.model.KeySafe;
import org.brekka.pegasus.core.model.Member;
import org.brekka.pegasus.core.model.Profile;
import org.brekka.pegasus.core.model.Vault;
import org.brekka.pegasus.core.model.XmlEntity;
import org.brekka.pegasus.core.services.MemberService;
import org.brekka.pegasus.core.services.ProfileService;
import org.brekka.pegasus.core.services.XmlEntityService;
import org.brekka.xml.pegasus.v1.model.ProfileDocument;
import org.brekka.xml.pegasus.v1.model.ProfileType;
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
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private ProfileDAO profileDAO;
    
    @Autowired
    private XmlEntityService xmlEntityService;
    
    @Autowired
    private MemberService memberService;
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.ProfileService#createPlainProfile(org.brekka.pegasus.core.model.Member)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Profile createPlainProfile(Member member) {
        Profile profile = new Profile();
        profile.setOwner(member);
        
        ProfileDocument profileDocument = ProfileDocument.Factory.newInstance();
        profileDocument.addNewProfile();
        
        XmlEntity<ProfileDocument> xmlEntity = xmlEntityService.persistPlainEntity(profileDocument);
        profile.setXml(xmlEntity);
        
        profileDAO.create(profile);
        return profile;
    }

    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.ProfileService#createEncryptedProfile(org.brekka.pegasus.core.model.Member, org.brekka.pegasus.core.model.Vault)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Profile createEncryptedProfile(Member member, Vault vault) {
        Profile profile = new Profile();
        profile.setOwner(member);
        
        ProfileDocument profileDocument = ProfileDocument.Factory.newInstance();
        profileDocument.addNewProfile();
        
        XmlEntity<ProfileDocument> xmlEntity = xmlEntityService.persistEncryptedEntity(profileDocument, vault);
        profile.setXml(xmlEntity);
        
        profileDAO.create(profile);
        return profile;
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.ProfileService#retrieveProfile(org.brekka.pegasus.core.model.Member)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Profile retrieveProfile(Member member) {
        List<Profile> profileList = profileDAO.retrieveByMember(member);
        Profile profile = profileList.get(0);
        XmlEntity<ProfileDocument> xmlEntity = profile.getXml();
        if (xmlEntity.getCryptedDataId() == null) {
            // We can extract the model
            XmlEntity<ProfileDocument> managedXmlEntity = xmlEntityService.retrieveEntity(xmlEntity.getId(), ProfileDocument.class);
            xmlEntity.setBean(managedXmlEntity.getBean());
        }
        return profile;
    }

    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.ProfileService#releaseProfile(org.brekka.pegasus.core.model.Profile, org.brekka.pegasus.core.model.Vault)
     */
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public boolean releaseProfile(Profile profile, Vault vault) {
        if (profile.getXml().getBean() != null) {
            // The bean is already unlocked
            return false;
        }
        XmlEntity<ProfileDocument> xmlEntity = profile.getXml();
        if (!xmlEntity.getKeySafe().getId().equals(vault.getId())) {
            // This is not the vault we are looking for.
            return false;
        }
        // Unlock it
        XmlEntity<ProfileDocument> managedXmlEntity = xmlEntityService.retrieveEntity(xmlEntity.getId(), ProfileDocument.class);
        xmlEntity.setBean(managedXmlEntity.getBean());
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.ProfileService#currentUserProfileUpdated()
     */
    @Override
    public void currentUserProfileUpdated() {
        /*
         * TODO bind the update as a single operation to the transaction so that
         * multiple changes can be made to the profile but only persisted once.
         */
        AuthenticatedMemberImpl current = (AuthenticatedMemberImpl) memberService.getCurrent();
        Profile activeProfile = current.getActiveProfile();
        ProfileDocument profileDocument = activeProfile.getXml().getBean();
        XmlEntity<ProfileDocument> currentXml = activeProfile.getXml();
        XmlEntity<ProfileDocument> replacementXml;
        if (currentXml.getCryptedDataId() == null) {
            // Plain
            replacementXml = xmlEntityService.persistPlainEntity(profileDocument);
        } else {
            KeySafe keySafe = currentXml.getKeySafe();
            replacementXml = xmlEntityService.persistEncryptedEntity(profileDocument, keySafe);
        }
        xmlEntityService.delete(currentXml.getId());
        activeProfile.setXml(replacementXml);
        profileDAO.update(activeProfile);
    }

}
