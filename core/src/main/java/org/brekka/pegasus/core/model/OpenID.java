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

package org.brekka.pegasus.core.model;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Open ID
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
@Entity
@DiscriminatorValue("OpenID")
public class OpenID extends AuthenticationToken {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -1838450960454825692L;

    /**
     * The open ID URI of this member
     * Can't be made non-nullable due to the inheritance of {@link AuthenticationToken}.
     */
    @Column(name="`URI`", unique=true, length=512)
    private String uri;

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

    @Override
    public String getUsername() {
        return getUri();
    }
}
