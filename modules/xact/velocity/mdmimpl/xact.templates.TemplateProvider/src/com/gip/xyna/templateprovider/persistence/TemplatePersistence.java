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

package com.gip.xyna.templateprovider.persistence;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import xact.templates.VelocityTemplatePart;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdnc.xnwh.VelocityTemplateStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;



public class TemplatePersistence {

  private static ODS ods = null;
  private final static String VELOCITY_TEMPLATE_CONTYPE_PROPERTY_NAME = "acs.velocitytemplate.connectiontype";

  private static Logger logger = Logger.getLogger(TemplatePersistence.class.getName());

  public static PreparedQueryCache queryCache = new PreparedQueryCache();


  public static List<VelocityTemplatePart> readFromHistory() throws XynaException {

    ODSConnection conHistory = getConnectionForVelocityTemplate();
    try {
      // nimm gecachte Anfrage wenn moeglich
      PreparedQuery<? extends VelocityTemplateStorable> pq = queryCache
                      .getQueryFromCache("SELECT * FROM " + VelocityTemplateStorable.TABLENAME, conHistory,
                                         new VelocityTemplateStorable().getReader());

      Parameter sqlparameter = new Parameter();
      Collection<? extends VelocityTemplateStorable> queryResult = conHistory.query(pq, sqlparameter, -1);


      List<VelocityTemplatePart> ret = new ArrayList<VelocityTemplatePart>();

      for (VelocityTemplateStorable tp : queryResult) {
        VelocityTemplatePart velocityTemplatePart = new VelocityTemplatePart();
        velocityTemplatePart.setId((Long) tp.getPrimaryKey());
        velocityTemplatePart.setApplication(tp.getApplication());
        velocityTemplatePart.setScope(tp.getScope());
        velocityTemplatePart.setPart(tp.getPart());
        velocityTemplatePart.setConstraintSet(tp.getConstraintSet());
        velocityTemplatePart.setScore(tp.getScore().intValue());
        velocityTemplatePart.setContent(tp.getContent());
        velocityTemplatePart.setType(tp.getType());

        ret.add(velocityTemplatePart);
      }
      return ret;
    }
    finally {
      try {
        conHistory.closeConnection();
      }
      catch (Exception e) {/* nop */
      }
    }


  }


  private static ODSConnection getConnectionForVelocityTemplate() throws PersistenceLayerException {
    if (ods == null) {
      synchronized (TemplatePersistence.class) {
        if (ods == null) {
          ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
        }
      }
    }
    ods.registerStorable(VelocityTemplateStorable.class);
    String value = XynaFactory.getInstance().getFactoryManagement()
                    .getProperty(VELOCITY_TEMPLATE_CONTYPE_PROPERTY_NAME);
    ODSConnectionType type = ODSConnectionType.HISTORY;
    if (value != null) {
      try {
        type = ODSConnectionType.getByString(value);
      }
      catch (IllegalArgumentException e) {
        logger.warn("Illegal xynaProperty value '" + value + "' in property '" + VELOCITY_TEMPLATE_CONTYPE_PROPERTY_NAME + "' using HISTORY");
      }
    }
    return ods.openConnection(type);
  }


}
