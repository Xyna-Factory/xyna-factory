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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.StringXMLSource;

import xmcp.xypilot.impl.Config;
import xmcp.xypilot.impl.util.DOMUtils;

public class TestXynaFactory implements XynaFactoryFacade {

    List<String> deployedDatatypes = Arrays.asList("base.Text", "base.Date", "test.xypilot.IPv4Address");
    List<String> deployedExceptions = Arrays.asList("test.xypilot.InvalidIPv4AddressException");

    @Override
    public Map<Long, List<String>> getDeployedDatatypes() {
        HashMap<Long, List<String>> dts = new HashMap<>();
        dts.put(-1l, deployedDatatypes);
        return dts;
    }

    @Override
    public Map<Long, List<String>> getDeployedExceptions() {
        HashMap<Long, List<String>> exs = new HashMap<>();
        exs.put(-1l, deployedExceptions);
        return exs;
    }

    @Override
    public void getDependenciesRecursivly(Long revision, Set<Long> dependencies) {
    }

    @Override
    public String getProperty(String key) {
        switch (key) {
            case Config.PROPERTY_XYPILOT_URI:
                return "http://localhost:5000";
            case Config.PROPERTY_XYPILOT_MAX_SUGGESTIONS:
                return "1";
            case Config.PROPERTY_XYPILOT_MODEL:
                return "fastertransformer";
        }
        return null;
    }

    @Override
    public Long getRevision(String applicationName, String versionName, String workspaceName)
            throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
        return -1l;
    }

    @Override
    public Long getRevisionDefiningXMOMObject(String originalXmlName, long parentRevision) {
        return -1l;
    }

    @Override
    public long getRevisionDefiningXMOMObjectOrParent(String originalXmlName, long parentRevision) {
        return -1l;
    }

    @Override
    public Set<Long> getAllRevisionsDefiningXMOMObject(String originalXmlName, long parentRevision) {
        return new HashSet<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GenerationBase> T parseGeneration(T gb) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        // test factory needs to explicitly load the xml from the resource directory
        StringXMLSource xmlSource = DOMUtils.createXMLSourceFromResourceDirectory(gb.getFqClassName());
        gb = (T) GenerationBase.getOrCreateInstance(gb.getFqClassName(), new GenerationBaseCache(), -1l, xmlSource);
        gb.parseGeneration(false, false, false);
        return gb;
    }

    @Override
    public GenerationBase getGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getGenerationBase'");
    }

    @Override
    public GenerationBase getGenerationBase(String fqn, long revision, String xml, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getGenerationBase'");
    }

    @Override
    public DomOrExceptionGenerationBase getDomOrExceptionGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getDomOrExceptionGenerationBase'");
    }

    @Override
    public DOM getDom(String fqn, long revision, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getDom'");
    }

    @Override
    public ExceptionGeneration getException(String fqn, long revision, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getException'");
    }

    @Override
    public WF getWorkflow(String fqn, long revision, boolean deployedState) throws XynaException {
        throw new UnsupportedOperationException("Unsupported method 'getWorkflow'");
    }

}
