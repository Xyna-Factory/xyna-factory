/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.factory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 * A facade implementation using the default XynaFactory, only works in deployed state.
 * @see XynaFactoryFacade
 */
public class DefaultXynaFactory implements XynaFactoryFacade {

    private static Logger logger = Logger.getLogger("XyPilot");

    @Override
    public Map<Long, List<String>> getDeployedDatatypes() {
        return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().getDeployedDatatypes();
    }

    @Override
    public Map<Long, List<String>> getDeployedExceptions() {
        return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().getDeployedExceptions();
    }

    @Override
    public void getDependenciesRecursivly(Long revision, Set<Long> dependencies) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, dependencies);
    }

    @Override
    public String getProperty(String key) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().getProperty(key);
    }

    @Override
    public Long getRevision(String applicationName, String versionName, String workspaceName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(applicationName, versionName, workspaceName);
    }

    @Override
    public Long getRevisionDefiningXMOMObject(String originalXmlName, long parentRevision) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObject(originalXmlName, parentRevision);
    }

    @Override
    public long getRevisionDefiningXMOMObjectOrParent(String originalXmlName, long parentRevision) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(originalXmlName, parentRevision);
    }

    @Override
    public Set<Long> getAllRevisionsDefiningXMOMObject(String originalXmlName, long parentRevision) {
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getAllRevisionsDefiningXMOMObject(originalXmlName, parentRevision);
    }

    @Override
    public <T extends GenerationBase> T parseGeneration(T gb) throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        gb.parseGeneration(true, false, false);
        return gb;
    }

    @Override
    public GenerationBase getGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException {
        try {
            GenerationBase.FactoryManagedRevisionXMLSource input = new GenerationBase.FactoryManagedRevisionXMLSource();
            File xmlFile = input.getFileLocation(fqn, revision, deployedState);
            logger.debug(xmlFile.getAbsolutePath());
            String xml = Files.readString(xmlFile.toPath());
            return getGenerationBase(fqn, revision, xml, deployedState);
        } catch (IOException e) {
            throw new XynaException("Couldn't get xml", e);
        }
    }

    @Override
    public GenerationBase getGenerationBase(String fqn, long revision, String xml, boolean deployedState) throws XynaException {

        GenerationBase.XMLSourceAbstraction inputSource = new GenerationBase.FactoryManagedRevisionXMLSource() {

            @Override
            public Document getOrParseXML(GenerationBase obj, boolean fileFromDeploymentLocation)
                    throws Ex_FileAccessException, XPRC_XmlParsingException {
                if (obj.getOriginalFqName().equals(fqn)) {
                    return XMLUtils.parseString(xml, true);
                }
                return super.getOrParseXML(obj, fileFromDeploymentLocation);
            }

            @Override
            public XMOMType determineXMOMTypeOf(String fqNameIn, Long originalRevision)
                    throws Ex_FileAccessException, XPRC_XmlParsingException {
                if (fqNameIn.equals(fqn)) {
                    try {
                        return XMOMType.getXMOMTypeByRootTag(
                                XMLUtils.getRootElementName(new ByteArrayInputStream(xml.getBytes())));
                    } catch (XMLStreamException e) {
                        throw new XPRC_XmlParsingException("Could not determine XMOM Type", e);
                    }
                }
                return super.determineXMOMTypeOf(fqNameIn, originalRevision);
            }

        };
        GenerationBase gb = GenerationBase.getOrCreateInstance(fqn, new GenerationBaseCache(),
                revision, inputSource);

        gb.setXMLInputSource(inputSource);
        gb.resetState();
        gb.parseGeneration(deployedState, false);
        return gb;
    }

    @Override
    public DomOrExceptionGenerationBase getDomOrExceptionGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException {
        logger.debug("Try get DomOrExceptionGenerationBase for " + fqn + "; deployed: " + deployedState);

        GenerationBase gb = getGenerationBase(fqn, revision, deployedState);
        if (!(gb instanceof DomOrExceptionGenerationBase)) {
            throw new XynaException("Type is not an instance of DomOrExceptionGenerationBase. Got " + gb.getClass().getName());
        }

        logger.debug("Got type " + gb.getTypeAsString());
        return (DomOrExceptionGenerationBase) gb;
    }

    @Override
    public DOM getDom(String fqn, long revision, boolean deployedState) throws XynaException {
        logger.debug("Try get DOM for " + fqn + "; deployed: " + deployedState);

        GenerationBase gb = getGenerationBase(fqn, revision, deployedState);
        if (!(gb instanceof DOM)) {
            throw new XynaException("Type is not an instance of DOM. Got " + gb.getClass().getName());
        }

        logger.debug("Got type " + gb.getTypeAsString());
        return (DOM) gb;
    }

    @Override
    public ExceptionGeneration getException(String fqn, long revision, boolean deployedState) throws XynaException {
        logger.debug("Try get Exception for " + fqn + "; deployed: " + deployedState);

        GenerationBase gb = getGenerationBase(fqn, revision, deployedState);
        if (!(gb instanceof ExceptionGeneration)) {
            throw new XynaException("Type is not an instance of ExceptionGeneration. Got " + gb.getClass().getName());
        }

        logger.debug("Got type " + gb.getTypeAsString());
        return (ExceptionGeneration) gb;
    }

    @Override
    public WF getWorkflow(String fqn, long revision, boolean deployedState) throws XynaException {
        logger.debug("Try get WF for " + fqn + "; deployed: " + deployedState);

        GenerationBase gb = getGenerationBase(fqn, revision, deployedState);
        if (!(gb instanceof WF)) {
            throw new XynaException("Type is not an instance of WF. Got " + gb.getClass().getName());
        }

        logger.debug("Got type " + gb.getTypeAsString());
        return (WF) gb;
    }

    @Override
    public void Publish(MessageInputParameter message) throws XynaException {
      XynaFactory.getInstance().getXynaMultiChannelPortal().publish(message);
    }


    @Override
    public String resolveSessionToUser(String sessionId) {
      return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement()
          .resolveSessionToUser(sessionId);
    }

}
