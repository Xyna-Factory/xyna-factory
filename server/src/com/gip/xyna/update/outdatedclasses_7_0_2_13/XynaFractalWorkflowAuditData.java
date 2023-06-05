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

package com.gip.xyna.update.outdatedclasses_7_0_2_13;

import java.util.List;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;


public class XynaFractalWorkflowAuditData extends XynaEngineSpecificAuditData {

  private static final long serialVersionUID = 7506570722780027423L;


  public XynaFractalWorkflowAuditData(AuditData parent) {
    super(parent);
  }


  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards)
      throws Ex_FileAccessException, XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    if (getProcess() == null) {
      return "";
    }
    return createXmlForFractalWorkflow(id, parentId, startTime, endTime, exceptions, revision, removeAuditDataAfterwards);
  }


  private String createXmlForFractalWorkflow(long id, long parentId, long startTime, long endTime,
                                             List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards) throws Ex_FileAccessException,
      XPRC_CREATE_MONITOR_STEP_XML_ERROR {

    XynaExceptionInformation exception = null;
    if (exceptions != null && exceptions.size() > 0) {
      // FIXME andere exceptions
      exception = exceptions.get(0);
    }
    
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(Integer.MAX_VALUE);
    
    if (container == null) {
      // TODO this gets the order input data as long as no execution input parameters are available. both should be available 
      container = new StepAuditDataContainer();
      container.addPreStepInformation(Integer.MAX_VALUE, null, getOrderInputData(), 0, getOrderInputVersion());
      //wieso hier nicht auch addPostStepInfo mit den outputdaten des wfs?
    }
    
    XMLExtension xmlExtension = new XMLExtension(this, revision, removeAuditDataAfterwards);
    
    boolean hasError = exception != null;
    if (startTime > 0 || endTime > 0 || hasError || containsCompensation()) {
      xmlExtension.createXmlForFractalWorkflow(id, parentId, startTime, endTime, exception, container);
    }
    
    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    return xmlHelper.auditToXml(xmlExtension.getString(), revision, xmlExtension.getAuditImports());
  }

}
