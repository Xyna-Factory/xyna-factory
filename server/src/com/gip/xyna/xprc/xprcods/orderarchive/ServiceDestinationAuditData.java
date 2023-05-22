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

package com.gip.xyna.xprc.xprcods.orderarchive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditXmlHelper;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall;


public class ServiceDestinationAuditData extends XynaEngineSpecificAuditData {

  private static final long serialVersionUID = 120549527090914245L;


  public ServiceDestinationAuditData(AuditData parent) {
    super(parent);
  }


  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions, Long revision, boolean removeAuditDataAfterwards)
      throws XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    return createXmlForServiceDestination(id, parentId, revision, removeAuditDataAfterwards, exceptions);
  }


  private String createXmlForServiceDestination(long id, long parentId, Long revision, boolean removeAuditDataAfterwards, List<XynaExceptionInformation> exceptions) {

    XMLExtension xmlExtension = new XMLExtension(this, revision, removeAuditDataAfterwards);
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(Integer.MAX_VALUE);
    if (container != null) {
      xmlExtension.createXmlForServiceDestination(id, parentId, container, exceptions);
    }
    
    AuditXmlHelper xmlHelper = new AuditXmlHelper();
    return xmlHelper.auditToXml(xmlExtension.getString(), revision, xmlExtension.getAuditImports());
  }


  public void addParameterPreStepValues(ProcessStep step, GeneralXynaObject[] generalXynaObjects, long version) {
    if (getProcess().equals(RemoteCall.FQ_XML_NAME + ".RemoteCall")) {
      if (generalXynaObjects != null && generalXynaObjects.length == 1 && generalXynaObjects[0] instanceof Container
          && ((Container) generalXynaObjects[0]).size() == 4) {
        Container c = (Container) generalXynaObjects[0];
        GeneralXynaObject[] newArr = new GeneralXynaObject[4];
        newArr[0] = c.get(0);
        newArr[1] = c.get(1);
        newArr[2] = wrapInAnyInputPayload(c.get(2));
        newArr[3] = wrapInAnyInputPayload(c.get(3));
        generalXynaObjects = newArr;
        version = XOUtils.nextVersion();
      } else {
        throw new RuntimeException("unexpected input for RemoteCall invocation: " + Arrays.toString(generalXynaObjects));
      }
    }
    super.addParameterPreStepValues(step, generalXynaObjects, version);
  }


  //FIXME vergleiche FIXMEs in RemoteCall
  private GeneralXynaObject wrapInAnyInputPayload(GeneralXynaObject generalXynaObject) {
    GeneralXynaObject aip = XynaObject.instantiate(RemoteCall.ANY_INPUT_PAYLOAD_FQ_XML_NAME, true, getParentAuditData().getRevision());
    List<GeneralXynaObject> list = new ArrayList<GeneralXynaObject>();
    if (generalXynaObject instanceof Container) {
      Container c = (Container) generalXynaObject;
      for (int i = 0; i < c.size(); i++) {
        list.add(c.get(i));
      }
    } else {
      list.add(generalXynaObject);
    }
    try {
      aip.set("inputPayload", list);
    } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
      throw new RuntimeException(e);
    }
    return aip;
  }


  public void addParameterPostStepValues(ProcessStep step, GeneralXynaObject[] generalXynaObjects, long version) {
    if (getProcess().equals(RemoteCall.FQ_XML_NAME + ".RemoteCall")) {
      if (generalXynaObjects != null && generalXynaObjects.length == 1) {
        GeneralXynaObject[] newArr = new GeneralXynaObject[1];
        newArr[0] = wrapInAnyInputPayload(generalXynaObjects[0]);
        generalXynaObjects = newArr;
        version = XOUtils.nextVersion();
      } else {
        throw new RuntimeException("unexpected output of RemoteCall invocation: " + Arrays.toString(generalXynaObjects));
      }
    }
    super.addParameterPostStepValues(step, generalXynaObjects, version);
  }

}
