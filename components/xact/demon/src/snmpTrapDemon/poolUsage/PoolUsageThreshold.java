/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpTrapDemon.poolUsage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.ResultSetReaderFunction;
import com.gip.xyna.utils.db.SQLUtils;

/**
 * PoolUsageThreshold liest die in der DB konfigurierten Schwellenwerte ein.
 * Anschließend kann für jede Kombination aus SharedNetwork und PoolType der richtige Schwellenwert ermittelt werden
 */
public class PoolUsageThreshold implements ResultSetReaderFunction {
  static Logger logger = Logger.getLogger(PoolUsageThreshold.class.getName());
  
  private float defaultThreshold = 0.0f;
  private HashMap<String,Float> thresholdByPoolType;
  private HashMap<String,Float> thresholdBySharedNetwork;
  private HashMap<String,Float> thresholdByBoth;
  private float lowestThreshold;

  /**
   * Lesen der Schwellenwerte
   * @param sqlUtils
   */
  public void read(SQLUtils sqlUtils) {
    thresholdByPoolType = new HashMap<String,Float>();
    thresholdBySharedNetwork = new HashMap<String,Float>();
    thresholdByBoth = new HashMap<String,Float>();
      
    sqlUtils.query( 
        "SELECT sharedNetworkID,poolTypeID,threshold FROM poolusagethreshold",
        null,
        this
        );
    
    lowestThreshold = searchLowestThreshold();
    
    logger.debug( "defaultThreshold="+defaultThreshold );
    logger.debug( "thresholdByPoolType="+thresholdByPoolType );
    logger.debug( "thresholdBySharedNetwork="+thresholdBySharedNetwork );
    logger.debug( "thresholdByBoth="+thresholdByBoth );
    logger.debug( "lowestThreshold="+lowestThreshold );
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.db.ResultSetReaderFunction#read(java.sql.ResultSet)
   */
  public boolean read(ResultSet rs) throws SQLException {
    String sharedNetwork = rs.getString(1);
    String poolType = rs.getString(2);
    float threshold = rs.getFloat(3);
    
    int mode = (sharedNetwork==null? 0: 2) + (poolType==null? 0: 1); 
    switch( mode ) {
    case 0: //weder sharedNetwork noch poolType gesetzt
      defaultThreshold = threshold;
      break;
    case 1: //poolType gesetzt
      thresholdByPoolType.put( poolType, threshold);
      break;
    case 2: //sharedNetwork gesetzt
      thresholdBySharedNetwork.put( sharedNetwork, threshold);
      break;
    case 3: //sharedNetwork und poolType gesetzt
      thresholdByBoth.put( sharedNetwork+"#"+poolType, threshold);
      break;
    }
    return true; //weiterlesen
  }

  /**
   * Suche nach dem niedrigsten Schwellenwert 
   * @return
   */
  private float searchLowestThreshold() {
    float lt = defaultThreshold;
    for( Float f : thresholdByPoolType.values() ) {
      if( lt > f ) {
        lt = f;
      }
    }
    for( Float f : thresholdBySharedNetwork.values() ) {
      if( lt > f ) {
        lt = f;
      }
    }
    for( Float f : thresholdByBoth.values() ) {
      if( lt > f ) {
        lt = f;
      }
    }
    return lt;
  }

  /**
   * Niedrigste Schwelle
   * @return the lowestThreshold
   */
  public float getLowestThreshold() {
    return lowestThreshold;
  }

  /**
   * Rückgabe der Schwelle für das angebene SharedNetwork und den PoolType
   * @param sharedNetwork
   * @param poolType
   * @return
   */
  public float getThreshold(String sharedNetwork, String poolType) {
    Float f = thresholdByBoth.get(sharedNetwork+"#"+poolType); 
    if( f == null ) {
      f = thresholdByPoolType.get(poolType); 
    }
    if( f == null ) {
      f = thresholdBySharedNetwork.get(sharedNetwork); 
    }
    if( f != null ) {
      return f.floatValue();
    } else {
      return defaultThreshold;
    }
  }

}
