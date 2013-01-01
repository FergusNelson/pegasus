/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brekka.pegasus.core.services.impl;

import java.util.List;
import java.util.UUID;

import org.brekka.commons.persistence.support.EntityUtils;
import org.brekka.pegasus.core.PegasusErrorCode;
import org.brekka.pegasus.core.PegasusException;
import org.brekka.pegasus.core.dao.ConnectionDAO;
import org.brekka.pegasus.core.model.Actor;
import org.brekka.pegasus.core.model.Connection;
import org.brekka.pegasus.core.model.Division;
import org.brekka.pegasus.core.model.KeySafe;
import org.brekka.pegasus.core.model.Member;
import org.brekka.pegasus.core.model.Partnership;
import org.brekka.pegasus.core.model.Vault;
import org.brekka.pegasus.core.services.MemberService;
import org.brekka.phalanx.api.beans.IdentityKeyPair;
import org.brekka.phalanx.api.model.AuthenticatedPrincipal;
import org.brekka.phalanx.api.model.KeyPair;
import org.brekka.phalanx.api.model.PrivateKeyToken;
import org.brekka.phalanx.api.services.PhalanxService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Common {@link KeySafe} operations shared among various service implementations.
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
abstract class AbstractKeySafeServiceSupport {
  
    @Autowired
    protected ConnectionDAO connectionDAO;
    
    @Autowired
    protected PhalanxService phalanxService;
    
    @Autowired
    protected MemberService memberService;
    
        
    protected PrivateKeyToken resolvePrivateKeyFor(KeySafe<?> keySafe, AuthenticatedMemberBase<?> currentMember) {
        PrivateKeyToken privateKeyToken;
        
        Actor contextMember = currentMember.getMember();
        Actor keySafeOwner = keySafe.getOwner();
        if (EntityUtils.identityEquals(keySafeOwner, contextMember)) {
            /*
             * The user owns this keySafe that means it is their personal chain.
             * Simply walk up the chain to get to the vault.
             */
            privateKeyToken = traverseChain(keySafe, currentMember);
        } else {
            /*
             * Not a personal chain. Check to see if there are any connections from this keysafe to the current user
             * or to organizations they are members of
             */
            List<Connection<?,?,?>> connectionList = connectionDAO.identifyConnectionsBetween(keySafe, contextMember);
            PrivateKeyToken found = null;
            for (Connection<?, ?, ?> connection : connectionList) {
                KeySafe<?> source = connection.getSource();
                found = resolveAndUnlock(source, connection.getKeyPairId(), currentMember);
                if (found != null) {
                    // We have found a private key that we can decrypt the connection with
                    break;
                }
            }
            
            if (found == null) {
                // Still not found
                if (keySafe instanceof Division) {
                    Division<?> division = (Division<?>) keySafe;
                    /*
                     * Try the division parent
                     */
                    KeySafe<?> parent = division.getParent();
                    if (parent != null) {
                        found = resolveAndUnlock(parent, division.getKeyPairId(), currentMember);
                    }
                } else {
                    throw new PegasusException(PegasusErrorCode.PG701, 
                            "Unable to handle keySafe type '%s' at this location", keySafe.getClass().getName());
                }
            }
            
            if (found == null) {
                throw new PegasusException(PegasusErrorCode.PG700, 
                        "Unable to locate a chain of keys that will unlock the keySafe '%s'", keySafe.getId());
            }
            privateKeyToken = found;
        }
        return privateKeyToken;
    }
    
    
    protected <Owner extends Actor, Target extends Actor> Partnership<Owner, Target> createPartnership(Owner owner, Division<Owner> source,
            Division<Target> target, KeyPair connectionKeyPair) {
        if (connectionKeyPair == null) {
            connectionKeyPair = createKeyPair(source);
        }
        Partnership<Owner, Target> partnership = new Partnership<>();
        partnership.setOwner(owner);
        partnership.setSource(source);
        partnership.setTarget(target);
        partnership.setKeyPairId(connectionKeyPair.getId());
        connectionDAO.create(partnership);
        return partnership;
    }
    
    protected KeyPair createKeyPair(KeySafe<?> keySafe) {
        KeyPair keyPair;
        if (keySafe instanceof Vault) {
            Vault vault = (Vault) keySafe;
            AuthenticatedPrincipal vaultKey = getVaultKey(vault);
            KeyPair vaultKeyPair = vaultKey.getDefaultPrivateKey().getKeyPair();
            keyPair = phalanxService.generateKeyPair(vaultKeyPair);
        } else if (keySafe instanceof Division) {
            Division<?> division = (Division<?>) keySafe;
            IdentityKeyPair identityKeyPair = new IdentityKeyPair(division.getKeyPairId());
            keyPair = phalanxService.generateKeyPair(identityKeyPair);
        } else {
            throw new IllegalStateException("Unknown keySafe type: " + keySafe.getClass().getName());
        }
        return keyPair;
    }
    /**
     * @param parent
     * @param currentMember
     * @return
     */
    protected PrivateKeyToken resolveAndUnlock(KeySafe<?> parent, UUID keyPairId, AuthenticatedMemberBase<?> currentMember) {
        KeyPair keyPair = new IdentityKeyPair(keyPairId);
        return resolveAndUnlock(parent, keyPair, currentMember);
    }
    
    /**
     * @param parent
     * @param currentMember
     * @return
     */
    protected PrivateKeyToken resolveAndUnlock(KeySafe<?> parent, KeyPair keyPair, AuthenticatedMemberBase<?> currentMember) {
        PrivateKeyToken privateKeyToken = currentMember.getPrivateKey(keyPair);
        if (privateKeyToken == null) {
            /*
             * Need to continue the hunt for the key
             */
            PrivateKeyToken foundPrivateKeyToken = resolvePrivateKeyFor(parent, currentMember);
            if (foundPrivateKeyToken != null) {
                privateKeyToken = phalanxService.decryptKeyPair(keyPair, foundPrivateKeyToken);
                currentMember.retainPrivateKey(keyPair, privateKeyToken);
            }
        }
        return privateKeyToken;
    }

    /**
     * Shortcut for personal chains that avoids the connections lookup.
     * @param keySafe
     * @return
     */
    private PrivateKeyToken traverseChain(KeySafe<?> keySafe, AuthenticatedMemberBase<?> currentMember) {
        PrivateKeyToken privateKeyToken;
        if (keySafe instanceof Vault) {
            Vault vault = (Vault) keySafe;
            AuthenticatedPrincipal vaultKey = currentMember.getVaultKey(vault);
            if (vaultKey == null) {
                throw new PegasusException(PegasusErrorCode.PG704, 
                        "Vault '%s' is not currently available. Most likely it needs to be unlocked.", vault.getId());
            }
            privateKeyToken = vaultKey.getDefaultPrivateKey();
        } else if (keySafe instanceof Division) {
            Division<?> division = (Division<?>) keySafe;
            
            // Check the parent
            KeySafe<?> parent = division.getParent();
            if (parent != null) {
                KeyPair keyPair = new IdentityKeyPair(division.getKeyPairId());
                PrivateKeyToken parentPrivateKey = currentMember.getPrivateKey(keyPair);
                if (parentPrivateKey == null) {
                    parentPrivateKey = traverseChain(parent, currentMember);
                    privateKeyToken = phalanxService.decryptKeyPair(keyPair, parentPrivateKey);
                    currentMember.retainPrivateKey(keyPair, privateKeyToken);
                } else {
                    privateKeyToken = parentPrivateKey;
                }
            } else {
                throw new PegasusException(PegasusErrorCode.PG702, 
                        "Reached end of personal chain for actor '%s' without finding a key", currentMember.getMember().getId());
            }
        } else {
            throw new PegasusException(PegasusErrorCode.PG703, 
                    "Unable to handle keySafe type '%s' at this location", keySafe.getClass().getName());
        }
        return privateKeyToken;
    }

    protected PrivateKeyToken unlockPrivateKey(KeyPair keyPair, Vault vault, AuthenticatedMemberBase<?> currentMember) {
        AuthenticatedPrincipal vaultKey = currentMember.getVaultKey(vault);
        PrivateKeyToken userPrivateKey = vaultKey.getDefaultPrivateKey();
        PrivateKeyToken privateKeyToken = phalanxService.decryptKeyPair(new IdentityKeyPair(keyPair.getId()), userPrivateKey);
        return privateKeyToken;
    }
    
    protected AuthenticatedPrincipal getVaultKey(Vault vault) {
        AuthenticatedMemberBase<Member> currentMember = AuthenticatedMemberBase.getCurrent(memberService, Member.class);
        AuthenticatedPrincipal authenticatedPrincipal = currentMember.getVaultKey(vault);
        if (authenticatedPrincipal == null) {
            // not unlocked
            throw new PegasusException(PegasusErrorCode.PG600, "Vault '%s' is locked", vault.getId());
        }
        return authenticatedPrincipal;
    }
}