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

package com.gip.xyna.xfmg.xfctrl.dependencies;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;



public final class DependencyNode implements Serializable {

  private static final long serialVersionUID = 4935220855741491415L;

  private static volatile Logger logger = CentralFactoryLogging.getLogger(DependencyNode.class);

  private final ConcurrentMap<DependencyNode, Boolean> dependentNodes;
  private final ConcurrentMap<DependencyNode, Boolean> usedNodes;
  private final DependencySourceType type;

  private final String uniqueName;
  private final Long revision;


  protected DependencyNode(String name, DependencySourceType type, Long revision) {
    this(name, type, revision, null, null);
  }


  protected DependencyNode(String name, DependencySourceType type, Long revision, ConcurrentHashMap<DependencyNode, Boolean> dependentNodes,
                           ConcurrentHashMap<DependencyNode, Boolean> usedNodes) {

    if (name == null) {
      throw new IllegalArgumentException("name may not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    if (revision == null) {
      this.revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    } else {
      this.revision = revision;
    }

    if (dependentNodes == null) {
      this.dependentNodes = new ConcurrentHashMap<DependencyNode, Boolean>(0, 0.75f, 4);
    } else {
      this.dependentNodes = dependentNodes;
    }
    if (usedNodes == null) {
      this.usedNodes = new ConcurrentHashMap<DependencyNode, Boolean>(0, 0.75f, 4);
    } else {
      this.usedNodes = usedNodes;
    }

    this.type = type;
    this.uniqueName = name;

    if (logger.isTraceEnabled()) {
      String traceMessage = "Creating dependency node '" + name + "'";
      if (this.dependentNodes.size() > 0) {
        traceMessage += " with the following dependent items:";
      } else {
        traceMessage += " without any dependent items.";
      }
      logger.trace(traceMessage);
      Iterator<DependencyNode> iter = this.dependentNodes.keySet().iterator();
      while (iter.hasNext()) {
        DependencyNode node = iter.next();
        logger.trace("\t* " + node.getType() + ": '" + node.getUniqueName() + "'");
      }
    }

  }


  public String getUniqueName() {
    return uniqueName;
  }


  /**
   * @return true if the node did not exist before
   */
  protected boolean addNodeThatUsesThis(DependencyNode node) {
    if (node == null) {
      logger.warn("node is null!");
      return false;
    }
    return dependentNodes.putIfAbsent(node, Boolean.TRUE) == null;
  }

  /**
   * @return true if the node did not exist before
   */
  protected boolean addUsedNode(DependencyNode node) {
    if (node == null) {
      logger.warn("node is null!");
      return false;
    }
    return usedNodes.putIfAbsent(node, Boolean.TRUE) == null;
  }


  /**
   * @return true if the dependent node existed
   */
  protected boolean removeDependentNode(DependencyNode node) {
    return dependentNodes.remove(node) != null;
  }
  
  
  /**
   * @return true if the dependent node existed
   */
  protected boolean removeUsedNode(DependencyNode node) {
    return usedNodes.remove(node) != null;
  }


  public Set<DependencyNode> getDependentNodes() {
    return Collections.unmodifiableSet(dependentNodes.keySet());
  }

  public Set<DependencyNode> getUsedNodes() {
    return Collections.unmodifiableSet(usedNodes.keySet());
  }

  public DependencySourceType getType() {
    return type;
  }


  @Override
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (o == this)
      return true;
    if (!(o instanceof DependencyNode))
      return false;
    DependencyNode anotherNode = (DependencyNode) o;
    if (anotherNode.type != type) {
      return false;
    }
    if (anotherNode.revision != revision) {
      if (anotherNode.revision != null && revision != null) {
        if (!anotherNode.revision.equals(revision)) {
          return false;
        }
        //ok
      } else {
        return false;
      }
    }
    if (!anotherNode.uniqueName.equals(uniqueName)) {
      return false;
    }
    return true;
  }


  @Override
  public int hashCode() {
    int h = uniqueName.hashCode();
    h = h * 31 + type.hashCode();
    h *= 31;
    if (revision != null) {
      h += revision.hashCode();
    }
    return h;
  }


  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }


  public Long getRevision() {
    return revision;
  }
  
  @Override
  public String toString() {
    return uniqueName + "-" + revision;
  }


}
