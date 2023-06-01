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
package com.gip.xyna.xact.trigger.actions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.FilterAction;
import com.gip.xyna.xact.trigger.GUIHTTPFilter;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;


/**
 *
 */
public class BuildServiceImplAction implements FilterAction {
  
  public boolean match(String uri, String method) {
    return uri.startsWith(GUIHTTPFilter.BUILD_SERVICE_IMPL_TEMPLATE_REQUEST);
  }
  
  public String getTitle() {
    return "Build ServiceImpl";
  }

  public void appendForm(StringBuilder sb, String indentation) {
    sb.append(indentation).append("<form action=\"buildServiceImplTemplate\" method=\"get\">\n");
    sb.append(indentation).append("  FqClassNameDOM </br>\n");
    sb.append(indentation).append("  <input type=\"text\" size=\"30\" maxlength=\"100\" name=\"p\"> </br>\n");
    sb.append(indentation).append("  </br>\n");
    sb.append(indentation).append("  <input type=\"submit\" value=\"get Filter\" >\n");
    sb.append(indentation).append("</form>\n");
  }  

  public FilterResponse act(Logger logger, HTTPTriggerConnection tc) throws XynaException {
    logger.info("got service implementation template request");
    String fqClassNameDOM = tc.getParas().getProperty("datatype");
    String workspaceName = tc.getParas().getProperty("workspace");

    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, workspaceName);
    
    GenerationBase gb = GenerationBase.getOrCreateInstance(fqClassNameDOM, new GenerationBaseCache(), revision);
    gb.parseGeneration(false/*saved*/, false, false);
    
    if(gb instanceof DOM) {
      boolean containsServiceCall = false;
      boolean hasJavaInstanceMethod = false;
      DOM dom = (DOM)gb;
      List<Operation> operations = dom.getOperations();
      for (Operation op : operations) {
        if(op instanceof JavaOperation && !op.isStatic()) {
          hasJavaInstanceMethod = true;
          JavaOperation jop = (JavaOperation)op;
          if(jop.getImpl() != null && jop.getImpl().contains("getImplementationOfInstanceMethods()")) {
            containsServiceCall = true;
            break;
          }
        }
      }
      if(hasJavaInstanceMethod && !containsServiceCall) {
        String log = "Datatype " + fqClassNameDOM + " has no member service with service call implementation.";
        tc.sendError(log);
        logger.error(log);
        return FilterResponse.responsibleWithoutXynaorder();
      }
    }
    try (InputStream is = XynaFactory.getInstance().getXynaMultiChannelPortal().getServiceImplTemplate(fqClassNameDOM, revision, true)){
      logger.debug("sending built service implementation template");
      tc.sendResponse(HTTPTriggerConnection.HTTP_OK, HTTPTriggerConnection.MIME_DEFAULT_BINARY, new Properties(), is);
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      throw new Ex_FileAccessException("unknown", e);
    }

    return FilterResponse.responsibleWithoutXynaorder();
  }

}
