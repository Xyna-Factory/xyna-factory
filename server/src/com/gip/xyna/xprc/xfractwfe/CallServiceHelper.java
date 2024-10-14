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

package com.gip.xyna.xprc.xfractwfe;

import com.gip.xyna.update.Updater;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


// TODO where does this belong?
public class CallServiceHelper {

  public static final String TOKEN_WF_TYPE_NAME = "TOKEN_WF_TYPE_NAME";
  public static final String TOKEN_WF_TYPE_PATH = "TOKEN_WF_TYPE_PATH";
  public static final String TOKEN_SERVICE_TYPE_NAME = "TOKEN_SERVICE_TYPE_NAME";
  public static final String TOKEN_SERVICE_TYPE_PATH = "TOKEN_SERVICE_TYPE_PATH";
  public static final String TOKEN_OPERATION_NAME = "TOKEN_OPERATION_NAME";
  public static final String TOKEN_SOURCE_DEFS = "TOKEN_SOURCE_DEFS";
  public static final String TOKEN_TARGET_DEFS = "TOKEN_TARGET_DEFS";
  public static final String TOKEN_ASSIGN_OBJECTS = "TOKEN_TARGETS_AS_SOURCES_FOR_ASSIGN";

  public static final String TOKEN_DATA_OBJECTS = "TOKEN_DATA_OBJECTS";

  public static final String TOKEN_WF_INPUT = "TOKEN_WF_INPUT";
  public static final String TOKEN_WF_OUTPUT = "TOKEN_WF_OUTPUT";


  public static String getServiceCallWithinWorkflowTemplate() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {

    StringBuilder sb = new StringBuilder();

    // TODO use GenerationBase.AT and GenerationBase.EL
    sb.append("<Service ID=\"1\" Label=\"encapsulation workflow\" TypeName=\"").append(TOKEN_WF_TYPE_NAME)
                    .append("\" TypePath=\"").append(TOKEN_WF_TYPE_PATH).append("\" Version=\"")
                    .append(Updater.getInstance().getXMOMVersion().getString())
                    .append("\" xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\">\n");
    sb.append("<Operation Label=\"").append(TOKEN_WF_TYPE_NAME).append("\" Name=\"").append(TOKEN_WF_TYPE_NAME)
                    .append("\">\n");
    sb.append("<Input>\n");
    sb.append(TOKEN_WF_INPUT);
    //      <Data ID="12" Label="correlationId" ReferenceName="CorrelationId" ReferencePath="bg" VariableName="correlationId">
    //      <Target RefID="9"/>
    //    </Data>
    //    <Data ID="13" Label="timeout" ReferenceName="Timeout" ReferencePath="bg" VariableName="timeout">
    //      <Target RefID="9"/>
    //    </Data>
    //    <Data ID="14" Label="synchroAnswer" ReferenceName="SynchroAnswer" ReferencePath="bg" VariableName="synchroAnswer">
    //      <Target RefID="9"/>
    //    </Data>
    sb.append("</Input>\n");
    sb.append("<Output>\n");
    sb.append(TOKEN_WF_OUTPUT);
    //      <Data ID="15" Label="synchroAnswer" ReferenceName="SynchroAnswer" ReferencePath="bg" VariableName="synchroAnswer15">
    //      <Source RefID="16"/>
    //    </Data>
    sb.append("</Output>\n");
    sb.append("<").append(GenerationBase.EL.THROWS).append(">\n");
    sb.append("<").append(GenerationBase.EL.EXCEPTION).append(" ").append(GenerationBase.ATT.REFERENCENAME)
                    .append("=\"").append(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME).append("\" ")
                    .append(GenerationBase.ATT.REFERENCEPATH).append("=\"")
                    .append(GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH).append("\" />\n");
    sb.append("</").append(GenerationBase.EL.THROWS).append(">\n");
    sb.append("<ServiceReference ID=\"8\" Label=\"" + TOKEN_SERVICE_TYPE_NAME + "\" ReferenceName=\""
                    + TOKEN_SERVICE_TYPE_NAME + "\" ReferencePath=\"" + TOKEN_SERVICE_TYPE_PATH + "\">\n");
    sb.append("<Source RefID=\"9\"/>\n");
    sb.append("<Target RefID=\"9\"/>\n");
    sb.append("</ServiceReference>\n");
    sb.append("<Function ID=\"9\" Label=\"" + TOKEN_OPERATION_NAME + "\">\n");
    sb.append(TOKEN_SOURCE_DEFS);
    //    "<Source RefID=\"12\"/>\n" +
    //    "<Source RefID=\"13\"/>\n" +
    //    "<Source RefID=\"14\"/>\n" +
    sb.append("<Source RefID=\"8\"/>\n");
    sb.append("<Target RefID=\"8\"/>\n");
    sb.append(TOKEN_TARGET_DEFS);
    //    "<Target RefID=\"10\"/>\n" +
    sb.append("<Invoke ServiceID=\"8\" Operation=\"" + TOKEN_OPERATION_NAME + "\">\n");
    sb.append(TOKEN_SOURCE_DEFS);
    //      "<Source RefID=\"12\"/>\n" +
    //      "<Source RefID=\"13\"/>\n" +
    //      "<Source RefID=\"14\"/>\n" +
    sb.append("</Invoke>\n");
    sb.append("<Receive ServiceID=\"8\">\n");
    sb.append(TOKEN_TARGET_DEFS);
    //      "<Target RefID=\"10\"/>\n" +
    sb.append("</Receive>\n");
    sb.append("</Function>\n");
    sb.append(TOKEN_DATA_OBJECTS);
    //  "<Data ID=\"10\" Label=\"answer\" ReferenceName=\"SynchroAnswer\" ReferencePath=\"bg\" VariableName=\"answer\">\n" +
    //    "<Source RefID=\"9\"/>\n" +
    //    "<Target RefID=\"16\"/>\n" +
    //  "</Data>\n" +
    sb.append("<Assign ID=\"16\">\n");
    sb.append(TOKEN_ASSIGN_OBJECTS);
    //    "<Source RefID=\"10\"/>\n" +
    //    "<Target RefID=\"15\"/>\n" +
    //    "<Copy>\n" +
    //      "</Source RefID=\"10\">\n" +
    //      "<Target RefID=\"15\"/>\n" +
    //    "</Copy>\n" +
    sb.append("</Assign>\n");
    sb.append("</Operation>\n");
    sb.append("</Service>");

    return sb.toString();
  }

}
