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
package com.gip.xyna.xmcp.xfcli.scriptentry.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.XMLSourceAbstraction;

public class SingleRevisionFileSystemXMLSource implements XMLSourceAbstraction {

  private String basePath;
  private Long primaryRev;
  private Long otherRev;
  
  private final Set<Long> dependencies;
  
  private HashSet<String> objectFqns;
  
  public SingleRevisionFileSystemXMLSource(String basePath, Long primaryRev, Long otherRev, Collection<String> objectFqns) {
    this.basePath = basePath;
    this.primaryRev = primaryRev;
    this.otherRev = otherRev;
    this.dependencies = Set.of(otherRev);
    this.objectFqns = new HashSet<String>(objectFqns);
  }
  
  
  @Override
  public Set<Long> getDependenciesRecursivly(Long revision) {
    return dependencies;
  }

  @Override
  public XMOMType determineXMOMTypeOf(String fqName, Long originalRevision) throws Ex_FileAccessException, XPRC_XmlParsingException {
    if(Objects.equals(otherRev, originalRevision)) {
      return XMOMType.DATATYPE;
    }
    

    File file = getFileLocation(fqName, originalRevision, true);
    try (FileInputStream fis = new FileInputStream(file)) {
      return XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(fis));
    } catch (FileNotFoundException e) {
      throw new Ex_FileAccessException(file.getAbsolutePath(), e);
    } catch (IOException e) {
      throw new XPRC_XmlParsingException(file.getAbsolutePath(), e);
    } catch (XMLStreamException e) {
      throw new XPRC_XmlParsingException(file.getAbsolutePath(), e);
    }
  }

  @Override
  public Long getRevisionDefiningXMOMObjectOrParent(String fqName, Long revision) {
    if(Objects.equals(revision, otherRev)) {
      return otherRev;
    }
    
    return objectFqns.contains(fqName) ? primaryRev : otherRev;
  }

  @Override
  public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return new Workspace((Objects.equals(revision, primaryRev) ? "primary" : "secondary"));
  }

  @Override
  public boolean isOfRuntimeContextType(Long revision, RuntimeContextType type) {
    return true;
  }

  @Override
  public Long getRevision(RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return rtc.getName().equals("primary") ? primaryRev : otherRev;
  }

  @Override
  public File getFileLocation(String fqName, Long revision, boolean fileFromDeploymentLocatio) {
    
    if(!Objects.equals(revision, primaryRev)) {
      return null;
    }
    
    String fqPath = "saved/XMOM/" + fqName.replaceAll("\\.", Constants.fileSeparator) + ".xml"; //TODO: saved/XMOM
    return new File(basePath, fqPath);
  }

  @Override
  public Document getOrParseXML(GenerationBase generator, boolean fileFromDeploymentLocation)
      throws Ex_FileAccessException, XPRC_XmlParsingException {
    File file = getFileLocation(generator.getFqClassName(), generator.getRevision(), fileFromDeploymentLocation);
    if(file == null) {
      return XMLUtils.parseString("");
    }
    return XMLUtils.parse(file, true);
  }
  

  @Override
  public File getClassOutputFolder() {
    return new File(basePath);
  }

}
