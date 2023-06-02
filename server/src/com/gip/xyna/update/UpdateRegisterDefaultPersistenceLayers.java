/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;


/**
 * Creates an XML file containing a set of persistence layers that will then be registered as persistence layers during
 * factory initialization
 */
public class UpdateRegisterDefaultPersistenceLayers extends Update {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateRegisterDefaultPersistenceLayers.class);

  private final Version allowedVersion;
  private final Version versionAfterUpdate;


  public UpdateRegisterDefaultPersistenceLayers(Version allowedVersion, Version versionAfterUpdate) {
    this.allowedVersion = allowedVersion;
    this.versionAfterUpdate = versionAfterUpdate;
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedVersion;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return versionAfterUpdate;
  }


  @Override
  protected void update() throws XynaException {

//    Set<Long> usedPersistenceLayerIds = new HashSet<Long>();
//
//    XMLPersistenceLayer xmlpers = new XMLPersistenceLayer();
//    Connection readingConnection = xmlpers.getConnection(Constants.PERSISTENCE_CONFIGURATION_DIR_WITHIN_STORAGE);
//    try {
//      Collection<PersistenceLayerInstanceBean> instances = readingConnection
//                      .loadCollection(PersistenceLayerInstanceBean.class);
//      if (instances != null && instances.size() > 0) {
//        logger.debug("Found " + instances.size() + " persistence layer instances found");
//        for (PersistenceLayerInstanceBean bean : instances) {
//          if (!usedPersistenceLayerIds.contains(bean.getPersistenceLayerID())) {
//            usedPersistenceLayerIds.add(bean.getPersistenceLayerID());
//          }
//        }
//      } else {
//        logger.debug("No persistence layer instances found, nothing to do");
//        return;
//      }
//    } finally {
//      readingConnection.closeConnection();
//    }
//
//    ArrayList<PersistenceLayerBean> defaultPersistenceLayers = new ArrayList<PersistenceLayerBean>();
//
//    for (long idAsLong : usedPersistenceLayerIds) {
//      if (idAsLong > Integer.MAX_VALUE) {
//        throw new RuntimeException("This update can only be applied for persistence layer instances < "
//                        + Integer.MAX_VALUE);
//      }
//      int id = (int) idAsLong;
//      switch (id) {
//        case 0 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(0L, ODSImpl.MEMORY_PERSISTENCE_LAYER_FQ_CLASSNAME));
//          break;
//        case 1 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(1L, ODSImpl.JAVA_PERSISTENCE_LAYER_FQ_CLASSNAME));
//          break;
//        case 2 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(2L, ODSImpl.MYSQL_PERSISTENCE_LAYER_FQ_CLASSNAME));
//          break;
//        case 3 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(3L, XMLPersistenceLayer.class.getName()));
//          break;
//        case 4 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(4L, DevNullPersistenceLayer.class.getName()));
//          break;
//        case 5 :
//          defaultPersistenceLayers
//                          .add(new PersistenceLayerBean(5L, XMLPersistenceLayerWithShellQueries.class.getName()));
//          break;
//        case 6 :
//          defaultPersistenceLayers.add(new PersistenceLayerBean(6L, ODSImpl.ORACLE_PERSISTENCE_LAYER_FQ_CLASSNAME));
//          break;
//
//        default :
//          throw new IllegalStateException("Found persistence layer with unknown ID");
//      }
//    }
//
//    Connection writingConnection = xmlpers.getConnection(Constants.PERSISTENCE_CONFIGURATION_DIR_WITHIN_STORAGE);
//    try {
//      writingConnection.persistCollection(defaultPersistenceLayers);
//    } finally {
//      writingConnection.commit();
//      writingConnection.closeConnection();
//    }
//
//    throw new RuntimeException("testing");

  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return false;
  }

}
