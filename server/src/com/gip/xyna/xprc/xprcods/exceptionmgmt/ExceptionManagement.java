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

package com.gip.xyna.xprc.xprcods.exceptionmgmt;



import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.DuplicateExceptionCodeException;
import com.gip.xyna.utils.exceptions.ExceptionHandler;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.exceptioncode.CodeGroupUnknownException;
import com.gip.xyna.utils.exceptions.exceptioncode.DuplicateCodeGroupException;
import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.CodeGroup;
import com.gip.xyna.utils.exceptions.exceptioncode.InvalidPatternException;
import com.gip.xyna.utils.exceptions.exceptioncode.NoCodeAvailableException;
import com.gip.xyna.utils.exceptions.exceptioncode.OverlappingCodePatternException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



/**
 * schnittstelle um exceptions abfragen zu können, exception codes zu verwalten etc
 */
public final class ExceptionManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "ExceptionManagement";

  private static final Logger logger = CentralFactoryLogging.getLogger(ExceptionManagement.class);

  private final DeploymentHandler exceptionDatabaseDeploymentHandler = new ExceptionDatabaseDeploymentHandler();
  private final UndeploymentHandler exceptionDatabaseUndeploymentHandler = new ExceptionDatabaseUndeploymentHandler();

  private ExceptionCodeManagement codeManagement;


  static {
    try {
      addDependencies(ExceptionManagement.class, new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                      new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                          DeploymentHandling.class),
                      new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                          DependencyRegister.class),
                      new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                          Configuration.class)})));

    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("", t);
    }
  }


  public ExceptionManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    codeManagement = new BlackExceptionCodeManagement(XynaFactory.getInstance().getProcessing().getXynaProcessingODS()
                    .getODS());
    codeManagement.init();
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .addDeploymentHandler(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE,
                                          exceptionDatabaseDeploymentHandler);
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .addUndeploymentHandler(DeploymentHandling.PRIORITY_EXCEPTION_DATABASE,
                                            exceptionDatabaseUndeploymentHandler);
  }


  @Override
  protected void shutdown() throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .removeDeploymentHandler(exceptionDatabaseDeploymentHandler);
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
                    .removeUndeploymentHandler(exceptionDatabaseUndeploymentHandler);
  }


  public Set<String> getAllSavedExceptionNames() {
    return null;
  }


  public Set<String> getAllDeployedExceptionNames() {
    return null;
  }


  private static class ExceptionDatabaseDeploymentHandler implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
      if (object instanceof ExceptionGeneration) {
        ExceptionGeneration eg = (ExceptionGeneration) object;
        Map<String, String> messages = eg.getExceptionEntry().getMessages();
        if (messages.size() == 0) {
          if (!object.isReservedServerObject()) {
            logger.warn(object.getOriginalFqName() + " contains no exception messages.");
          }
        }
        try {
          Long rev = object.getRevision();
          if (object.isReservedServerObject()) {
            //mit -1 registrieren, weil XynaExceptionBase.getRevision() für diese Exception auch -1 zurückgibt
            rev = -1L;
          }
          for (Map.Entry<String, String> entry : messages.entrySet()) {
              ExceptionHandler.cacheErrorMessage(true, eg.getExceptionEntry().getCode(), entry.getValue(), entry.getKey(), rev);
          }
        } catch (DuplicateExceptionCodeException e) {
          throw new XPRC_DeploymentHandlerException(object.getFqClassName(), "exceptiondatabase", e);
        }
      }
    }

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    }

    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }

  }

  private static class ExceptionDatabaseUndeploymentHandler implements UndeploymentHandler {

    public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
      if (object instanceof ExceptionGeneration) {
        File file = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(object.getOriginalFqName(), object.getRevision()) + ".xml");
        Document doc;
        try {
          doc = XMLUtils.parse(file.getAbsolutePath());
        } catch (Ex_FileAccessException e1) {
          logger.warn("XML not found. Can not deregister exception codes.", e1);
          return;
        } catch (XPRC_XmlParsingException e1) {
          throw new XPRC_UnDeploymentHandlerException(object.getFqClassName(), "exceptiondatabase", e1);
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        String xpathType = "/" + GenerationBase.EL.EXCEPTIONSTORAGE + "/" + GenerationBase.EL.EXCEPTIONTYPE;
        String code;
        try {
          code = xpath.evaluate(xpathType + "/@" + GenerationBase.ATT.EXCEPTION_CODE, doc);
        } catch (XPathExpressionException e) {
          throw new RuntimeException("xpath could not be evaluated for xml " + file.getAbsolutePath(), e);
        }
        ExceptionHandler.deregisterErrorCode(code, object.getRevision());
      }
    }

    public void exec(FilterInstanceStorable object) {
    }

    public void exec(TriggerInstanceStorable object) {
    }

    public void exec(Capacity object) {
    }

    public void exec(DestinationKey object) {
    }

    public void finish() throws XPRC_UnDeploymentHandlerException {
    }

    public boolean executeForReservedServerObjects(){
      return false;
    }

    public void exec(FilterStorable object) {
    }

    public void exec(TriggerStorable object) {
    }
  }


  /**
   * prüft, ob in im xml definierte exception bereits einen code besitzt. falls nicht, wird ein neuer generiert
   * @param doc
   * @throws NoCodeAvailableException
   * @throws CodeGroupUnknownException
   * @throws PersistenceLayerException
   */
  public void checkExceptionCode(Document doc) throws CodeGroupUnknownException, NoCodeAvailableException,
                  PersistenceLayerException {
    codeManagement.checkExceptionCode(doc);
  }


  public CodeGroup[] listCodeGroups() throws PersistenceLayerException {
    return codeManagement.listCodeGroups();
  }


  public void addCodeGroup(String codeGroupName) throws DuplicateCodeGroupException, PersistenceLayerException {
    codeManagement.addCodeGroup(codeGroupName);
  }


  public void removeCodeGroup(String codeGroupName) throws CodeGroupUnknownException, PersistenceLayerException {
    codeManagement.removeCodeGroup2(codeGroupName);
  }


  public void addCodePattern(String codeGroupName, String pattern, int startIndex, int endIndex, int padding)
                  throws CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException,
                  PersistenceLayerException {
    codeManagement.addCodePattern(codeGroupName, pattern, startIndex, endIndex, padding);
  }


  public void removeCodePattern(String codeGroupName, int patternIndex) throws CodeGroupUnknownException,
                  PersistenceLayerException {
    codeManagement.removeCodePattern2(codeGroupName, patternIndex);
  }


  public void exportToXml() throws PersistenceLayerException {
    codeManagement.exportToXml();
  }


  public void importFromXml() throws XynaException {
    codeManagement.importFromXml();
  }

}
