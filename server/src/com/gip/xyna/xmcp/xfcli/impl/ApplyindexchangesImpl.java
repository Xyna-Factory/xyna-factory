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
package com.gip.xyna.xmcp.xfcli.impl;

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.xmcp.xfcli.generated.Applyindexchanges;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision.IndexModification;



public class ApplyindexchangesImpl extends XynaCommandImplementation<Applyindexchanges> {

  public void execute(OutputStream statusOutputStream, Applyindexchanges payload) throws XynaException {
    ODSImpl ods = ODSImpl.getInstance();
    
    Map<Long, Set<DatabaseIndexCollision>> allCollisions = ods.getIndexCollisions();
    Map<Long, Set<DatabaseIndexCollision>> filteresCollisions = new HashMap<Long, Set<DatabaseIndexCollision>>();
    
    // filter by plName
    String[] plNames = payload.getPlname();
    if (plNames == null || plNames.length <= 0) {
      filteresCollisions.putAll(allCollisions);
    } else {
      for (String plName : plNames) {
        Long plId = ods.getPersistenceLayerInstanceId(plName);
        Set<DatabaseIndexCollision> subCollisions = allCollisions.get(plId);
        if (subCollisions == null) {
          writeLineToCommandLine(statusOutputStream, "Could not apply index collisions for persistence layer instance ", plName, ", no collisions found.");
        } else {
          filteresCollisions.put(plId, subCollisions);
        }
      }
    }
    if (filteresCollisions.size() <= 0) {
      writeLineToCommandLine(statusOutputStream, "No collisions left after filtering.");
    } else {
      // filter by modification
      String[] modifcations = payload.getIndexmodification();
      if (modifcations == null || modifcations.length <= 0) {
        // ntbd
      } else {
        Set<IndexModification> allowedModifications = new HashSet<IndexModification>();
        for (String modifcation : modifcations) {
          allowedModifications.add(IndexModification.valueOf(modifcation.toUpperCase()));
        }
        Collection<Long> keysToRemove = new ArrayList<Long>();
        for (Entry<Long, Set<DatabaseIndexCollision>> collisionsEntry : filteresCollisions.entrySet()) {
          Set<DatabaseIndexCollision> collisions = collisionsEntry.getValue();
          Iterator<DatabaseIndexCollision> collIter = collisions.iterator();
          while (collIter.hasNext()) {
            DatabaseIndexCollision collision = collIter.next();
            if (!allowedModifications.contains(collision.getIndexModification())) {
              collIter.remove();
            }
          }
          if (collisions.size() <= 0) {
            keysToRemove.add(collisionsEntry.getKey());
          }
        }
        for (Long plId : keysToRemove) {
          filteresCollisions.remove(plId);
        }
      }
      if (filteresCollisions.size() <= 0) {
        writeLineToCommandLine(statusOutputStream, "No collisions left after filtering.");
      } else {
        writeLineToCommandLine(statusOutputStream, "Resolving issues in ", filteresCollisions.size(), " persistence layer instances.");
        for (Entry<Long, Set<DatabaseIndexCollision>> collisionsEntry : filteresCollisions.entrySet()) {
          writeLineToCommandLine(statusOutputStream, "Resolving ", collisionsEntry.getValue().size(),
                                 " issues in ", ods.getPersistenceLayerInstanceName(collisionsEntry.getKey()));
          ods.resolveIndexCollisions(collisionsEntry.getKey(), collisionsEntry.getValue());
        }
      }
    }
  }

}
