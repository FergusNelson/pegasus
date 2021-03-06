/*
 * Copyright 2013 the original author or authors.
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

package org.brekka.pegasus.core.dao;

import java.util.UUID;

import org.brekka.commons.persistence.dao.EntityDAO;
import org.brekka.pegasus.core.model.Collective;
import org.brekka.pegasus.core.model.Member;
import org.brekka.pegasus.core.model.Participant;

/**
 * Manages persistence of {@link Participant} instances
 *
 * @author Andrew Taylor (andrew@brekka.org)
 */
public interface ParticipantDAO extends EntityDAO<UUID, Participant> {

    /**
     * @param collective
     * @param member
     * @return
     */
    Participant retrieveByMember(Collective collective, Member member);

}
