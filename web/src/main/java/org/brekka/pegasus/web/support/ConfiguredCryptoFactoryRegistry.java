package org.brekka.pegasus.web.support;

import org.brekka.phoenix.CryptoFactory;
import org.brekka.phoenix.CryptoFactoryRegistry;
import org.brekka.phoenix.impl.CryptoFactoryRegistryImpl;
import org.brekka.stillingar.annotations.ConfigurationListener;
import org.brekka.stillingar.annotations.Configured;
import org.brekka.xml.phoenix.v1.model.CryptoProfileRegistryDocument;
import org.brekka.xml.phoenix.v1.model.CryptoProfileRegistryDocument.CryptoProfileRegistry;
import org.springframework.stereotype.Component;

@Configured
@Component
public class ConfiguredCryptoFactoryRegistry implements CryptoFactoryRegistry {
    
    private CryptoFactoryRegistry delegate;
    
    @Override
    public CryptoFactory getDefault() {
        return delegate.getDefault();
    }
    
    @Override
    public CryptoFactory getFactory(int profileId) {
        return delegate.getFactory(profileId);
    }
    
    
    @ConfigurationListener
    public void configure(@Configured CryptoProfileRegistry cryptoProfileRegistry) {
        delegate = CryptoFactoryRegistryImpl.createRegistry(cryptoProfileRegistry);
    }
}