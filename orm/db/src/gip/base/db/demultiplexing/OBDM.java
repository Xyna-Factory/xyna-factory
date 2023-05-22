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
package gip.base.db.demultiplexing;

import gip.base.common.LongDTO;
import gip.base.common.OBDTO;
import gip.base.common.OBDTOInterface;
import gip.base.common.OBException;
import gip.base.common.OBListDTO;
import gip.base.db.OBContext;
import gip.base.db.OBDBObject;
import gip.base.db.XynaContextFactory;

import org.apache.log4j.Logger;


/**
 * OBDM
 */
public class OBDM {
  
//  public static OBDTO convertTkToDTO(OBContext context, OBDTO retVal, long usedFor) {
//    return retVal;
//  }

  private transient static Logger logger = Logger.getLogger(OBDM.class);
  
  // ------------------- findAll-Methoden -------------------------------------------------

  
    public static <E extends OBDTO> OBListDTO<E> findAllPKs(OBContext context, E example, OBDTO filter) throws OBException {
    OBListDTO<E> retVal = new OBListDTO<E>(OBDBObject.findAll(context,example,filter.getPks(),filter.getOrderClause(),filter.getHint()));
    // hier keine maxRows-Behandlung
    return retVal;
  }
  
  
  public static <E extends OBDTO> OBListDTO<E> findAllFilter(OBContext context, E example, OBDTO filter) throws OBException {
    OBListDTO<E> retVal = new OBListDTO<E>(OBDBObject.findAll(context,example,filter));
    
    return retVal;
  }
  
  
  public static <E extends OBDTO> OBListDTO<E> findAllWC(OBContext context, E example, OBDTO filter) throws OBException {    
    OBListDTO<E> retVal = new OBListDTO<E>(OBDBObject.findAll(context,example,filter.getWhereClause(),filter.getOrderClause(), filter.getHint(), filter.getMaxRowsSelect(), null));
    
    return retVal;
  }
  
  
  // ------------------- count-Methoden ---------------------------------------------------
  
  
  public static LongDTO countPKs(OBContext context, OBDTO example, OBDTO filter) throws OBException {
    long retVal = OBDBObject.count(context,example,filter.getPks());
    
    return LongDM.convertTkToDTO(context, retVal, filter.getUsedFor());
  }
  
  
  public static LongDTO countFilter(OBContext context, OBDTO example, OBDTO filter) throws OBException {
    long retVal = OBDBObject.count(context,example,filter);
    
    return LongDM.convertTkToDTO(context, retVal, filter.getUsedFor());
  }
  
  
  public static LongDTO countWC(OBContext context, OBDTO example, OBDTO filter) throws OBException {
    long retVal = OBDBObject.count(context,example,filter.getWhereClause());
    
    return LongDM.convertTkToDTO(context, retVal, filter.getUsedFor());
  }

  
  /**
   *  Fuehrt allgemein eine Aktion aus
   * @param context Durchgereichtes Context-Objekt
   * @param dto
   * @return Ergebis-DTO
   * @throws OBException Durchgereicht oder beim Erzeugen der DM-Klasse
   */
  public OBDTOInterface doOperation(OBContext context, OBDTO dto) throws OBException {
    if (dto.getCapabilityId() < 1) {
      throw new OBException(OBException.OBErrorNumber.capabilityNotSet);
    }
    String dmClassName = dto.getDMClassName();
    try {
      Class<?> dmClass=Class.forName(dmClassName);
      OBDM dm = (OBDM) dmClass.newInstance();
      return dm.doOperation(context,dto);
    }
    catch (OBException e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error doing operation",e);//$NON-NLS-1$
      throw e;
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + "error doing operation",e);//$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.unknownError1, new String[] {e.getMessage()});
    }
    
  }  

  
}


