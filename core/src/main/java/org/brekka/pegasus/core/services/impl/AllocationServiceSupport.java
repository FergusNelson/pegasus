/**
 * 
 */
package org.brekka.pegasus.core.services.impl;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.brekka.paveway.core.dao.CryptedFileDAO;
import org.brekka.paveway.core.model.CompletableUploadedFile;
import org.brekka.paveway.core.model.CryptedFile;
import org.brekka.paveway.core.model.UploadedFiles;
import org.brekka.paveway.core.services.PavewayService;
import org.brekka.paveway.core.services.ResourceCryptoService;
import org.brekka.paveway.core.services.ResourceStorageService;
import org.brekka.pegasus.core.dao.AllocationDAO;
import org.brekka.pegasus.core.dao.AllocationFileDAO;
import org.brekka.pegasus.core.model.AccessorContext;
import org.brekka.pegasus.core.model.Allocation;
import org.brekka.pegasus.core.model.AllocationFile;
import org.brekka.pegasus.core.model.Dispatch;
import org.brekka.pegasus.core.model.KeySafe;
import org.brekka.pegasus.core.model.Transfer;
import org.brekka.pegasus.core.model.XmlEntity;
import org.brekka.pegasus.core.services.EventService;
import org.brekka.pegasus.core.services.KeySafeService;
import org.brekka.pegasus.core.services.XmlEntityService;
import org.brekka.phalanx.api.services.PhalanxService;
import org.brekka.phoenix.api.services.CryptoProfileService;
import org.brekka.phoenix.api.services.SymmetricCryptoService;
import org.brekka.xml.pegasus.v2.model.AllocationDocument;
import org.brekka.xml.pegasus.v2.model.AllocationType;
import org.brekka.xml.pegasus.v2.model.BundleType;
import org.brekka.xml.pegasus.v2.model.DetailsType;
import org.brekka.xml.pegasus.v2.model.FileType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
class AllocationServiceSupport {

    @Autowired
    protected PhalanxService phalanxService;
    
    @Autowired
    protected PavewayService pavewayService;
    
    @Autowired
    protected ResourceStorageService resourceStorageService;
    
    @Autowired
    protected SymmetricCryptoService symmetricCryptoService;
    
    @Autowired
    protected ResourceCryptoService resourceCryptoService;
    
    @Autowired
    protected CryptoProfileService cryptoProfileService;
    
    @Autowired
    protected EventService eventService;
    
    @Autowired
    protected KeySafeService keySafeService;

    @Autowired
    private AllocationDAO allocationDAO;
    
    @Autowired
    private AllocationFileDAO allocationFileDAO;
    
    @Autowired
    private CryptedFileDAO cryptedFileDAO;
    
    @Autowired
    protected XmlEntityService xmlEntityService;
    
    

    protected BundleType completeFiles(int maxDownloads, UploadedFiles files) {
        if (files == null) {
            return null;
        }
        BundleType bundleType = BundleType.Factory.newInstance();
        List<CompletableUploadedFile> ready = files.uploadComplete();
        for (CompletableUploadedFile file : ready) {
            CryptedFile cryptedFile = pavewayService.complete(file);
            
            FileType fileXml = bundleType.addNewFile();
            fileXml.setName(cryptedFile.getFileName());
            fileXml.setMimeType(cryptedFile.getMimeType());
            fileXml.setUUID(cryptedFile.getId().toString());
            fileXml.setKey(cryptedFile.getSecretKey().getEncoded());
            fileXml.setLength(cryptedFile.getOriginalLength());
            if (maxDownloads > 0) {
                fileXml.setMaxDownloads(maxDownloads);
            }
        }
        return bundleType;
    }
    
    
    /**
     * @param maxDownloads
     * @param bundleType
     * @return
     */
    protected BundleType copyBundle(Integer maxDownloads, BundleType bundleType) {
        BundleType b = BundleType.Factory.newInstance();
        List<FileType> fileList = bundleType.getFileList();
        for (FileType file : fileList) {
            FileType f = b.addNewFile();
            f.setKey(file.getKey());
            f.setLength(file.getLength());
            f.setMimeType(file.getMimeType());
            f.setName(file.getName());
            f.setUUID(file.getUUID());
            if (maxDownloads != null) {
                f.setMaxDownloads(maxDownloads);
            }
        }
        return b;
    }
    /**
     * @param dispatch
     * @return
     */
    protected BundleType copyDispatchBundle(Dispatch dispatch, Integer maxDownloads) {
        XmlEntity<AllocationDocument> xml = xmlEntityService.retrieveEntity(dispatch.getXml().getId(), AllocationDocument.class);
        AllocationType dispatchXml = xml.getBean().getAllocation();
        BundleType dispatchBundle = copyBundle(maxDownloads, dispatchXml.getBundle());
        return dispatchBundle;
    }
    
    /**
     * Prepare the XML based structure that will contain the details for this bundle,
     * while will be subsequently encrypted.
     * @param bundleType
     * @return
     */
    protected AllocationType prepareAllocationType(BundleType bundleType, DetailsType detailsType) {
        AllocationType allocationType = AllocationType.Factory.newInstance();
        if (bundleType != null) {
            allocationType.setBundle(bundleType);
        }
        if (detailsType != null) {
            allocationType.setDetails(detailsType);
        }
        return allocationType;
    }
    
    protected void decryptDocument(Allocation allocation) {
        XmlEntity<AllocationDocument> existing = allocation.getXml();
        if (existing.getBean() != null) {
            // Already decrypted
            return;
        }
        XmlEntity<AllocationDocument> xml = xmlEntityService.release(existing, AllocationDocument.class);
        allocation.setXml(xml);
        assignFileXml(allocation);
        if (allocation instanceof Transfer) {
            eventService.transferUnlocked((Transfer) allocation);
        }
    }
    
    protected void encryptDocument(Allocation allocation, AllocationType allocationType, KeySafe<?> keySafe) {
        AllocationDocument allocationDocument = AllocationDocument.Factory.newInstance();
        allocationDocument.setAllocation(allocationType);
        XmlEntity<AllocationDocument> xmlEntity = xmlEntityService.persistEncryptedEntity(allocationDocument, keySafe, true);
        allocation.setXml(xmlEntity);
    }
    
    protected void createAllocationFiles(Allocation allocation) {
        XmlEntity<AllocationDocument> xml = xmlEntityService.release(allocation.getXml(), AllocationDocument.class);
        BundleType bundle = xml.getBean().getAllocation().getBundle();
        List<FileType> fileList = bundle.getFileList();
        for (FileType fileType : fileList) {
            AllocationFile allocationFile = new AllocationFile();
            allocationFile.setAllocation(allocation);
            CryptedFile cryptedFile = cryptedFileDAO.retrieveById(UUID.fromString(fileType.getUUID()));
            allocationFile.setCryptedFile(cryptedFile);
            allocationFile.setXml(fileType);
            allocationFileDAO.create(allocationFile);
        }
    }
    
    
    protected void assignFileXml(Allocation allocation) {
        XmlEntity<AllocationDocument> xml = xmlEntityService.release(allocation.getXml(), AllocationDocument.class);
        AllocationType allocationType = xml.getBean().getAllocation();
        List<AllocationFile> files = allocation.getFiles();
        List<FileType> fileList = allocationType.getBundle().getFileList();
        // Use nested loops as there should never be that many files.
        for (FileType fileType : fileList) {
            UUID cryptedFileID = UUID.fromString(fileType.getUUID());
            for (AllocationFile allocationFile : files) {
                UUID id = allocationFile.getCryptedFile().getId();
                if (id.equals(cryptedFileID)) {
                    allocationFile.setXml(fileType);
                    break; // Break out of this loop
                }
            }
        }
    }
    
    protected void bindToContext(Allocation allocation) {
        bindToContext(allocation.getId(), allocation);
    }
    
    protected void bindToContext(Serializable key, Allocation allocation) {
        AccessorContext accessorContext = AccessorContextImpl.getCurrent();
        accessorContext.retain(key, allocation);
        List<AllocationFile> files = allocation.getFiles();
        for (AllocationFile allocationFile : files) {
            accessorContext.retain(allocationFile.getCryptedFile().getId(), allocationFile);
        }
    }
    
    /* (non-Javadoc)
     * @see org.brekka.pegasus.core.services.AllocationService#refreshAllocation(org.brekka.pegasus.core.model.AnonymousTransfer)
     */
    protected void refreshAllocation(Allocation allocation) {
        allocationDAO.refresh(allocation);
        assignFileXml(allocation);
    }
}