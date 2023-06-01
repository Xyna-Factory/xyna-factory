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

package com.gip.xyna.xnwh.persistence.dbmodifytable;

import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.XynaFactoryWarehouse;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.dbmodifytable.DatabaseIndexCollision.IndexModification;



public class DatabasePersistenceLayerWithAlterTableSupportHelper {


  private static final Logger logger = CentralFactoryLogging
      .getLogger(DatabasePersistenceLayerWithAlterTableSupportHelper.class);
  
  private final static String FURTHER_STARTUP_INFO_TYPE = DatabasePersistenceLayerWithAlterTableSupportHelper.class.getSimpleName();


  /**
   * falls im storable bei einer spalte (Column) das attribut size nicht angegeben ist, wird dieser wert verwendet
   */
  public static final int DEFAULT_SIZE_COLUMN_TYPE = 128; // TODO configurable?


  private DatabasePersistenceLayerWithAlterTableSupportHelper() {
  }


  public static <T extends Storable> void addTable(DatabasePersistenceLayerConnectionWithAlterTableSupport connection,
                                                   Class<T> klass) throws PersistenceLayerException {

    Persistable persi = Storable.getPersistable(klass);
    final String tableName = persi.tableName();
    Column[] cols = Storable.getColumns(klass);

    boolean tableExists = connection.doesTableExist(persi);

    //ja => update notwendig?
    if (tableExists) {
      if (logger.isDebugEnabled()) {
        logger.debug("Validating table " + tableName);
      }
      //typen von spalten ermitteln und auf kompatibilität prüfen
      //falls nicht kompatibel => fehler werfen.
      //falls nicht existent => spalte hinzufügen
      Set<DatabaseIndexCollision> collisions = connection.checkColumns(persi, klass, cols);

      if (collisions != null && collisions.size() > 0) {
        ODSImpl.getInstance().addIndexCollisions(connection.getPersistenceLayerInstanceId(), collisions);
        informIndexCollisionsAtStartup( collisions );
      }
    } else {
      //nein => create table
      logger.info("Creating table " + tableName);
      connection.createTable(persi, klass, cols);
    }

  }


  private static void informIndexCollisionsAtStartup(Set<DatabaseIndexCollision> collisions) {
    boolean inform = true;
    switch( XynaFactoryWarehouse.SHOW_STARTUP_INFORMATION_INDEX_COLLISIONS.get() ) {
    case always:
      inform = true;
      break;
    case never: 
      inform = false;
      return;
    case onlyMissing:
      inform = false;
      for( DatabaseIndexCollision coll : collisions ) {
        if( coll.getIndexModification() == IndexModification.CREATE ) {
          inform = true;
          break;
        }
      }
      break;
    case onlyMissingOrChanged:
      inform = false;
      for( DatabaseIndexCollision coll : collisions ) {
        if( coll.getIndexModification() == IndexModification.CREATE || coll.getIndexModification() == IndexModification.MODIFY ) {
          inform = true;
          break;
        }
      }
      break;
    }
    
    if( inform ) {
      if ( ! XynaExtendedStatusManagement.containsKey(FURTHER_STARTUP_INFO_TYPE) ) {
        XynaExtendedStatusManagement.addFurtherInformationAtStartup(FURTHER_STARTUP_INFO_TYPE,
            "There have been index collisions, call listindexcollisions for further information.");
      }
    }
  }


  /**
   * validierung von spalte
   */
  public static <T extends Storable> void checkColumn(DatabasePersistenceLayerConnectionWithAlterTableSupport connection,
                                                      DatabaseColumnInfo colInfo, Column col, Class<T> klass,
                                                      String tableName, boolean automaticColumnTypeWidening)
      throws PersistenceLayerException {

    // Check if the old column type can store the data that is requested by the new column type
    // if this is the case we don't need to alter the column type.
    boolean columnsAreCompatible = connection.areColumnsCompatible(col, klass, colInfo);
    if (columnsAreCompatible) {
      //zb varchar / varchar
      //muss noch überprüfen, ob varchar größe auch ok ist.
      if (colInfo.isTypeDependentOnSizeSpecification()) {
        if (col.size() > 0) {
          if (col.size() > colInfo.getCharLength()) {
            //TODO das ist eigtl etwas gemogelt, die größe könnte für verschiedene typen auch verschiedene auswirkungen haben.
            if (automaticColumnTypeWidening) {
              try {
                connection.modifyColumnsCompatible(col, klass, tableName);
              } catch (RuntimeException e) {
                throw new XNWH_GeneralPersistenceLayerException("Could not modify column " + col + " of table " + tableName, e);
              }
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("column <" + col.name() + "> of table <" + tableName + "> has type <"
                    + colInfo.getTypeAsString() + "(" + colInfo.getCharLength()
                    + ">). the size provided by storable class (" + col.size()
                    + ") suggests that this is not enough space.");
              }
            }
          }
        } //else keine größe angegeben im storable, dann ist sie wohl nicht so wichtig
      } //else dann passen die typen unabhängig von einer größenangabe im colInfo.type

      return;
    }

    // Check if the old type can be converted into the new type without losses.
    // Mostly it is expected to 
    //könnte zb sein, dass vorhanden=TINYINT, recommended=MEDIUMINT
    boolean areBaseTypesCompatible = connection.areBaseTypesCompatible(col, klass, colInfo);
    if (areBaseTypesCompatible) {

      if (automaticColumnTypeWidening) {
        try {
          connection.widenColumnsCompatible(col, klass, tableName);
        } catch (RuntimeException e) {
          throw new XNWH_GeneralPersistenceLayerException("Could not widen column " + col + " of table " + tableName, e);
        }
      } else {
        if (logger.isDebugEnabled()) {
          String oldTypeString = colInfo.getTypeAsString();
          if (colInfo.isTypeDependentOnSizeSpecification()) {
            oldTypeString += "(" + colInfo.getCharLength() + ")";
          }

          String recommendedTypeString = connection.getTypeAsString(col, klass);
          boolean recommendedTypeIsDependentOnSizeSpecification =
              connection.isTypeDependentOnSizeSpecification(col, klass);
          if (recommendedTypeIsDependentOnSizeSpecification) {
            if (col.size() > 0) {
              recommendedTypeString += "(" + col.size() + ")";
            } else {
              recommendedTypeString += "(" + XynaProperty.DEFAULT_SIZE_COLUMN_TYPE.get() + ")";
            }
          }
          logger.debug("column <" + col.name() + "> of table <" + tableName + "> has type <" + oldTypeString
              + ">. the type provided by storable class (" + recommendedTypeString
              + ") suggests that this is not enough space.");
        }
        
        // TODO: The type is compatible, great! But does it really fit? Here needs to be a size check
        // for example an INT that will be converted into an VARCHAR might require a specific length for the VARCHAR! 
      }

      return;
    }

    //oder: vorhanden=DATE, recommended=BOOLEAN => nicht kompatibel => fehler
    String compatibleTypesString = connection.getCompatibleColumnTypesAsString(col, klass);
    throw new XNWH_GeneralPersistenceLayerException("incompatible type detected in table " + tableName + " in column "
        + col.name() + ". expected one of " + compatibleTypesString + ", got " + colInfo.getTypeAsString() + ".");
  }

}
