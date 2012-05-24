/**
 * 
 */
package org.brekka.pegasus.core.model;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.brekka.xml.pegasus.v1.model.InvitationDocument;
import org.hibernate.annotations.Type;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
@Entity
@Table(name="`Invitation`")
public class Invitation extends SnapshotEntity {
    
    /**
     * Serial UID
     */
    private static final long serialVersionUID = 6635175785485701951L;

    @ManyToOne
    @JoinColumn(name="`SenderID`", nullable=false)
    private Actor sender;
    
    @ManyToOne
    @JoinColumn(name="`RecipientID`", nullable=false)
    private Member recipient;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="`Actioned`")
    private Date actioned;
    
    @Column(name="`Status`", nullable=false)
    @Enumerated(EnumType.STRING)
    private InvitationStatus invitationStatus = InvitationStatus.NEW;

    @OneToOne
    @JoinColumn(name="`XmlEntityID`", nullable=false)
    private XmlEntity<InvitationDocument> xml;
    
    /**
     * The key to the resource that the invitation if for. It will have been encrypted
     * using the public key of the keySafe found in the XML entity. If the invitation
     * is accepted, this should be transferred, otherwise it should be deleted in phalanx.
     */
    @Type(type="pg-uuid")
    @Column(name="`CryptedDataID`", updatable=false)
    private UUID cryptedDataId;

    
    
    public Actor getSender() {
        return sender;
    }

    public void setSender(Actor sender) {
        this.sender = sender;
    }

    public Member getRecipient() {
        return recipient;
    }

    public void setRecipient(Member recipient) {
        this.recipient = recipient;
    }

    public Date getActioned() {
        return actioned;
    }

    public void setActioned(Date actioned) {
        this.actioned = actioned;
    }

    public InvitationStatus getInvitationStatus() {
        return invitationStatus;
    }

    public void setInvitationStatus(InvitationStatus invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    public XmlEntity<InvitationDocument> getXml() {
        return xml;
    }

    public void setXml(XmlEntity<InvitationDocument> xml) {
        this.xml = xml;
    }

    public UUID getCryptedDataId() {
        return cryptedDataId;
    }

    public void setCryptedDataId(UUID cryptedDataId) {
        this.cryptedDataId = cryptedDataId;
    }
}