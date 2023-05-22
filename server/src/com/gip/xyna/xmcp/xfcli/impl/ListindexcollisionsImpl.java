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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.gip.xyna.xmcp.xfcli.generated.Listindexcollisions;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision;



public class ListindexcollisionsImpl extends XynaCommandImplementation<Listindexcollisions> {

  public void execute(OutputStream statusOutputStream, Listindexcollisions payload) throws XynaException {
    ODSImpl ods = ODSImpl.getInstance();
    
    Map<Long, Set<DatabaseIndexCollision>> collisions = ods.getIndexCollisions();
    
    
    writeLineToCommandLine(statusOutputStream, "Listing information for ", collisions.size(), " ", (collisions.size() == 1 ? "persistence layer" : "persistence layers"));
    String format = "  %-20s %-15s %-25s  %-35s";
    writeLineToCommandLine(statusOutputStream, String.format(format, "persistenceLayer", "modification", "table", "column"));
    
    for (Entry<Long, Set<DatabaseIndexCollision>> collisionEntry : collisions.entrySet()) {
        String persistenceLayer = ods.getPersistenceLayerInstanceName(collisionEntry.getKey());
        SortedSet<DatabaseIndexCollision> sortedCollisions = new TreeSet<DatabaseIndexCollision>(new DatabaseIndexCollisionPrettyPrintComparator());
        sortedCollisions.addAll(collisionEntry.getValue());
        for (DatabaseIndexCollision collision : sortedCollisions) {
          writeLineToCommandLine(statusOutputStream, String.format(format, persistenceLayer,
                                                                           collision.getIndexModification(),
                                                                           collision.getPersi().tableName(),
                                                                           collision.getColumn().name()));
        }
    }
    
  }
  
  
  private static class DatabaseIndexCollisionPrettyPrintComparator implements Comparator<DatabaseIndexCollision> {

    public int compare(DatabaseIndexCollision o1, DatabaseIndexCollision o2) {
      int comp = o1.getIndexModification().compareTo(o2.getIndexModification());
      if (comp == 0) {
        comp = o1.getPersi().tableName().compareTo(o2.getPersi().tableName());
        if (comp == 0) {
          comp = o1.getColumn().name().compareTo(o2.getColumn().name());
        }
      }
      return comp;
    }
    
  }

}
