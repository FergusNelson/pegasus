/**
 * 
 */
package org.brekka.pegasus.core.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.xmlbeans.XmlException;
import org.brekka.paveway.core.model.Bundle;
import org.brekka.paveway.core.model.ByteSequence;
import org.brekka.paveway.core.model.Compression;
import org.brekka.paveway.core.model.CryptedFile;
import org.brekka.paveway.core.services.BundleService;
import org.brekka.paveway.core.services.ResourceCryptoService;
import org.brekka.paveway.core.services.ResourceEncryptor;
import org.brekka.paveway.core.services.ResourceStorageService;
import org.brekka.pegasus.core.PegasusErrorCode;
import org.brekka.pegasus.core.PegasusException;
import org.brekka.pegasus.core.model.Allocation;
import org.brekka.pegasus.core.model.AllocationFile;
import org.brekka.pegasus.core.model.Transfer;
import org.brekka.pegasus.core.services.EventService;
import org.brekka.phoenix.CryptoFactory;
import org.brekka.phoenix.CryptoFactoryRegistry;
import org.brekka.xml.pegasus.v1.model.AllocationDocument;
import org.brekka.xml.pegasus.v1.model.AllocationType;
import org.brekka.xml.pegasus.v1.model.BundleType;
import org.brekka.xml.pegasus.v1.model.FileType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Andrew Taylor (andrew@brekka.org)
 *
 */
class AllocationServiceSupport {
    
    @Autowired
    protected BundleService bundleService;
    
    @Autowired
    protected ResourceStorageService resourceStorageService;
    
    @Autowired
    protected ResourceCryptoService resourceCryptoService;
    
    @Autowired
    protected CryptoFactoryRegistry cryptoFactoryRegistry;
    
    @Autowired
    protected EventService eventService;
    

    /**
     * Prepare the XML based structure that will contain the details for this bundle,
     * while will be subsequently encrypted.
     * @param comment
     * @param fileBuilders
     * @return
     */
    protected AllocationDocument prepareDocument(int maxDownloads, Bundle bundle) {
        AllocationDocument doc = AllocationDocument.Factory.newInstance();
        AllocationType allocationType = doc.addNewAllocation();
        BundleType bundleType = allocationType.addNewBundle();
        Collection<CryptedFile> files = bundle.getFiles().values();
        for (CryptedFile file : files) {
            FileType fileXml = bundleType.addNewFile();
            fileXml.setName(file.getFileName());
            fileXml.setMimeType(file.getMimeType());
            fileXml.setUUID(file.getId().toString());
            fileXml.setKey(file.getSecretKey().getEncoded());
            fileXml.setLength(file.getOriginalLength());
            if (maxDownloads > 0) {
                fileXml.setMaxDownloads(maxDownloads);
            }
        }
        return doc;
    }
    
    protected void decryptDocument(Allocation allocation, byte[] secretKeyBytes) {
        try {
            AllocationType allocationType = decrypt(allocation, secretKeyBytes);
            allocation.setXml(allocationType);
            assignFileXml(allocation);
        } catch (XmlException | IOException e) {
            throw new PegasusException(PegasusErrorCode.PG200, e, 
                    "Failed to retrieve XML for allocation '%s'", allocation.getId());
        }
        if (allocation instanceof Transfer) {
            eventService.transferUnlocked((Transfer) allocation);
        }
    }
    
    protected void createAllocationFiles(Allocation allocation, Bundle bundle) {
        // TODO
    }
    
    
    protected void assignFileXml(Allocation allocation) {
        AllocationType allocationType = allocation.getXml();
        Map<UUID, AllocationFile> files = allocation.getFiles();
        List<FileType> fileList = allocationType.getBundle().getFileList();
        for (FileType fileType : fileList) {
            AllocationFile allocationFile = files.get(UUID.fromString(fileType.getUUID()));
            allocationFile.setXml(fileType);
        }
    }
    
    
    private AllocationType decrypt(Allocation allocation, byte[] secretKeyBytes) throws XmlException, IOException {
        UUID allocationId = allocation.getId();
        ByteSequence byteSequence = resourceStorageService.retrieve(allocationId);
        IvParameterSpec iv = new IvParameterSpec(allocation.getIv());
        CryptoFactory cryptoFactory = cryptoFactoryRegistry.getFactory(allocation.getProfile());
        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, cryptoFactory.getSymmetric().getKeyGenerator().getAlgorithm());
        
        try ( InputStream is = byteSequence.getInputStream(); ) {
            InputStream dis = resourceCryptoService.decryptor(allocation.getProfile(), Compression.GZIP, iv, secretKey, is);
            AllocationDocument allocationDocument = AllocationDocument.Factory.parse(dis);
            allocation.setXml(allocation.getXml());
            return allocationDocument.getAllocation();
        }
    }
    
    
    protected void encryptDocument(Allocation allocation, AllocationDocument allocationDoc) {
        allocation.setId(UUID.randomUUID());
        // Fetch the default crypto factory, generate a new secret key
        CryptoFactory defaultCryptoFactory = cryptoFactoryRegistry.getDefault();
        SecretKey secretKey = defaultCryptoFactory.getSymmetric().getKeyGenerator().generateKey();
        
        ResourceEncryptor encryptor = resourceCryptoService.encryptor(secretKey, Compression.GZIP);
        ByteSequence byteSequence = resourceStorageService.allocate(allocation.getId());
        OutputStream os = byteSequence.getOutputStream();
        try ( OutputStream eos = encryptor.encrypt(os) ) {
            allocationDoc.save(eos);
        } catch (IOException e) {
            throw new PegasusException(PegasusErrorCode.PG200, e, 
                    "Failed to store bundle XML");
        }
        allocation.setProfile(defaultCryptoFactory.getProfileId());
        allocation.setSecretKey(secretKey);
        allocation.setIv(encryptor.getIV().getIV());
        allocation.setXml(allocationDoc.getAllocation());
    }

}