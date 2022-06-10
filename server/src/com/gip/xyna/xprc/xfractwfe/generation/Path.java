/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.io.Serializable;
import java.util.Objects;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Cache;
import com.gip.xyna.utils.collections.Cache.CacheEntryCreation;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.Visitor;



/**
 * pfad bzgl datenmodell.
 * 
 * path-variable ist der default-pfad für den audit etc
 */
public final class Path implements Serializable {

  private static final long serialVersionUID = 1L;
  private final String path;
  private final String fqDataModelName;


  public Path(String path, String fqDataModelName) {
    if (path == null) {
      throw new IllegalArgumentException();
    }
    this.path = path;
    this.fqDataModelName = fqDataModelName;
  }


  public String getPath() {
    return path;
  }


  public String getFqDataModelName() {
    return fqDataModelName;
  }


  @Override
  public int hashCode() {
    return Objects.hash(path);
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Path other = (Path) obj;
    return Objects.equals(path, other.path);
  }


  private static class XMOMVariableIdentification implements VariableContextIdentification {

    public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
      throw new RuntimeException();
    }

    public TypeInfo getTypeInfo(String originalXmlName) {
      throw new RuntimeException();
    }

    public Long getRevision() {
      throw new RuntimeException();
    }

    public VariableInfo createVariableInfo(TypeInfo resultType) {
      throw new RuntimeException();
    }

  }


  public static interface PathCreationVisitor extends Visitor {

    public String getPath();
    public AVariable getCurrentDom();
    
  }


  //TODO cache reduzieren auf die objekte, die man wirklich benötigt
  private static final Cache<String, DOM> dataModelCache = new Cache<String, DOM>(new CacheEntryCreation<String, DOM>() {

    public DOM create(String fqDataModelName) {
      try {
        return DOM.generateUncachedInstance(fqDataModelName, false, RevisionManagement.REVISION_DATAMODEL);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      } catch (XPRC_InheritedConcurrentDeploymentException e) {
        throw new RuntimeException(e);
      } catch (AssumedDeadlockException e) {
        throw new RuntimeException(e);
      } catch (XPRC_MDMDeploymentException e) {
        throw new RuntimeException(e);
      }
    }
  });
  
  public static DOM getDataModelFromCache(String fqDataModelName) {
    return dataModelCache.getOrCreate(fqDataModelName);
  }

  public static void removeDataModelFromCache(String fqDataModelName) {
    dataModelCache.removeFromCache(fqDataModelName);
  }

  public static void clearCache() {
    dataModelCache.clearCache();
  }


  /**
   * erstellt ein pfad-objekt, passend zu dem xfl-path, bezogen auf das datenmodell. das pfad-objekt enthält
   * ggfs die OID, die sich aus den OID-bestandteilen, die im datenmodell angegeben sind, zusammensetzt.
   * 
   * falls der pfad im datenmodell nicht aufgelöst werden kann oder das datenmodell null ist, wird der pfad 1:1 übernommen
   */
  public static Path createPath(String xflVariablePath, String fqDataModelName) throws XPRC_InvalidPackageNameException,
      XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException,
      XPRC_ParsingModelledExpressionException, PersistenceLayerException {
    if (fqDataModelName == null) {
      return new Path(xflVariablePath, null);
    }

    DOM dom = dataModelCache.getOrCreate(fqDataModelName);

    if (!xflVariablePath.startsWith("%")) {
      xflVariablePath = "%0%." + xflVariablePath;
    }
    ModelledExpression expression = ModelledExpression.parse(new XMOMVariableIdentification(), xflVariablePath);

    /*
     * am dom entlang hangeln und für jeden schritt aus dem datenmodell entsprechend OID auslesen
     * 
     * index-informationen werden im syntax ".<index>" angefügt (nicht [<index>] wie es bei XFL ist)
     * 
     */
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    PathCreationVisitor v = dmm.getPathCreationVisitor(dom);
    expression.visitTargetExpression(v);

    return new Path(v.getPath(), fqDataModelName);
  }


  /**
   * gibt die datamodel-infos zurück, die an im datenmodell an der stelle, die durch den pfad beschrieben ist, angegeben sind. 
   * gibt null zurück, falls der pfad im datenmodell nicht aufgelöst werden kann.
   */
  public static DataModelInformation getDataModelInfoForPath(String xflVariablePath, String fqDataModelName)
      throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException,
      XPRC_MDMDeploymentException, XPRC_ParsingModelledExpressionException, PersistenceLayerException {

    DOM dom = dataModelCache.getOrCreate(fqDataModelName);

    if (!xflVariablePath.startsWith("%")) {
      xflVariablePath = "%0%." + xflVariablePath;
    }
    ModelledExpression expression = ModelledExpression.parse(new XMOMVariableIdentification(), xflVariablePath);

    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    PathCreationVisitor v = dmm.getPathCreationVisitor(dom);
    expression.visitTargetExpression(v);
    return v.getCurrentDom().getDataModelInformation();
  }

}
