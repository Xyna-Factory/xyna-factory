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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

/**
 * A facade interface to the XynaFactory.
 * It includes only the methods that are needed by the XyPilot.
 * The main purpose of this interface is to make the XyPilot independent of the XynaFactory
 * and switch to a different implementation for testing, where no 'real' XynaFactory is available.
 */
public interface XynaFactoryFacade {

    public Map<Long, List<String>> getDeployedDatatypes();

    public Map<Long, List<String>> getDeployedExceptions();

    public void getDependenciesRecursivly(Long revision, Set<Long> dependencies);

    public String getProperty(String key);

    public Long getRevision(String applicationName, String versionName, String workspaceName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

    public Long getRevisionDefiningXMOMObject(String originalXmlName, long parentRevision);

    public long getRevisionDefiningXMOMObjectOrParent(String originalXmlName, long parentRevision);

    public Set<Long> getAllRevisionsDefiningXMOMObject(String originalXmlName, long parentRevision);

    public <T extends GenerationBase> T parseGeneration(T gb) throws XynaException;

    public GenerationBase getGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException;

    public GenerationBase getGenerationBase(String fqn, long revision, String xml, boolean deployedState) throws XynaException;

    public DomOrExceptionGenerationBase getDomOrExceptionGenerationBase(String fqn, long revision, boolean deployedState) throws XynaException;

    public DOM getDom(String fqn, long revision, boolean deployedState) throws XynaException;

    public ExceptionGeneration getException(String fqn, long revision, boolean deployedState) throws XynaException;

    public WF getWorkflow(String fqn, long revision, boolean deployedState) throws XynaException;

    public void Publish(MessageInputParameter message) throws XynaException;
}
