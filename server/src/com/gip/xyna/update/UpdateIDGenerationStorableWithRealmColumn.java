/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.update;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.idgeneration.GeneratedIDsStorable;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;



public class UpdateIDGenerationStorableWithRealmColumn extends UpdateJustVersion {

  public UpdateIDGenerationStorableWithRealmColumn(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }


  @Override
  protected void update() throws XynaException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(GeneratedIDsStorable.class);
    try {
      ODSConnection c = ods.openConnection();
      try {
        Collection<GeneratedIDsStorable> coll = c.loadCollection(GeneratedIDsStorable.class);
        List<GeneratedIDsStorable> l = new ArrayList<GeneratedIDsStorable>();
        boolean foundXMOMPersistence = false;
        long defaultId = -1;
        Set<Integer> bindings = new HashSet<Integer>();
        for (GeneratedIDsStorable s : coll) {
          if (s.getRealm() == null) {
            s.setRealm(IDGenerator.REALM_DEFAULT);
            defaultId = Math.max(s.getLastStoredId(), defaultId);
            bindings.add(s.getBinding());
            l.add(s);
          } else if (s.getRealm().equals(XMOMPersistenceManagement.IDGEN_REALM)) {
            foundXMOMPersistence = true;
          }
        }
        if (!foundXMOMPersistence && defaultId >= 0) {
          for (Integer binding : bindings) {
            //da bisher xmompersistenceids speziellen realm abgefragt haben, aber den standard realm verwendet haben,
            //muss der realm entsprechend hoch initialisiert werden, um id-kollisionen mit bereits vergebenen ids  zu verhindern
            GeneratedIDsStorable s =
                new GeneratedIDsStorable(XMOMPersistenceManagement.IDGEN_REALM, XMOMPersistenceManagement.IDGEN_REALM, binding);
            s.setLastStoredId(defaultId);
            s.setResultingFromShutdown(true);
            l.add(s);
          }
        }
        if (l.size() > 0) {
          c.persistCollection(l);
          c.commit();
        }
      } finally {
        c.closeConnection();
      }
    } finally {
      ods.unregisterStorable(GeneratedIDsStorable.class);
    }
  }


}
