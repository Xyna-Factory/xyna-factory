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

package com.gip.xyna.xfmg.xfctrl.dependencies;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;


public class DependencyRegisterTest extends TestCase {

  public void testAddDependencies() throws XynaException {

    DependencyRegister register = new DependencyRegister("unused");
    register.addDependency(DependencySourceType.DATATYPE, "usedDataType1", DependencySourceType.FILTER, "usingFilter1");


    Set<DependencyNode> allDatatypeDependencies = register.getDependencyNodesByType(DependencySourceType.DATATYPE);
    assertEquals("Unexpected resultset size after adding single dependency", 1, allDatatypeDependencies.size());

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType1", DependencySourceType.FILTER, "usingFilter1");
    allDatatypeDependencies = register.getDependencyNodesByType(DependencySourceType.DATATYPE);
    assertEquals("Unexpected resultset size after adding the same dependency two times", 1, allDatatypeDependencies.size());

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType2", DependencySourceType.FILTER, "usingFilter1");
    allDatatypeDependencies = register.getDependencyNodesByType(DependencySourceType.DATATYPE);
    assertEquals("Unexpected resultset size after adding a second used object", 2, allDatatypeDependencies.size());

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType2", DependencySourceType.TRIGGER, "usingTrigger2");
    allDatatypeDependencies = register.getDependencyNodesByType(DependencySourceType.DATATYPE);
    assertEquals("Unexpected resultset size after adding a second used object", 2, allDatatypeDependencies.size());

  }

  public void testGetDependencies() throws XynaException {

    DependencyRegister register = new DependencyRegister("unused");

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType1", DependencySourceType.FILTER, "intermediateFilter1");
    register.addDependency(DependencySourceType.FILTER, "intermediateFilter1", DependencySourceType.TRIGGER, "usingTrigger1");

    Set<DependencyNode> dependencies = register.getDependencies("usedDataType1", DependencySourceType.DATATYPE);

    assertEquals("Unexpected number of dependencies for added datatype", 1, dependencies.size());
    Iterator<DependencyNode> iter = dependencies.iterator();
    DependencyNode next = iter.next();
    assertEquals("Unexpected name", "intermediateFilter1", next.getUniqueName());
    assertEquals("Unexpected type", DependencySourceType.FILTER, next.getType());

  }


  public void testGetDependenciesRecurse() throws XynaException {

    DependencyRegister register = new DependencyRegister("unused");

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType1", DependencySourceType.FILTER,
                           "intermediateFilter1");
    register.addDependency(DependencySourceType.FILTER, "intermediateFilter1", DependencySourceType.TRIGGER,
                           "usingTrigger1");
    
    DependencyNode root = register.getDependencyNode("usedDataType1", DependencySourceType.DATATYPE, VersionManagement.REVISION_WORKINGSET);
    assertEquals("Unexpected name", "usedDataType1", root.getUniqueName());
    assertEquals("Unexpected type", DependencySourceType.DATATYPE, root.getType());
    assertEquals("Unexpected number of dependencies for added datatype", 1, root.getDependentNodes().size());

    Iterator<DependencyNode> iter1 = root.getDependentNodes().iterator();
    DependencyNode next1 = iter1.next();
    assertEquals("Unexpected name", "intermediateFilter1", next1.getUniqueName());
    assertEquals("Unexpected type", DependencySourceType.FILTER, next1.getType());
    assertEquals("Unexpected number of dependencies for added filter", 1, next1.getDependentNodes().size());

    Iterator<DependencyNode> iter2 = next1.getDependentNodes().iterator();
    DependencyNode next2 = iter2.next();
    assertEquals("Unexpected name", "usingTrigger1", next2.getUniqueName());
    assertEquals("Unexpected type", DependencySourceType.TRIGGER, next2.getType());
    assertEquals("Unexpected number of dependencies for added filter", 0, next2.getDependentNodes().size());

  }


  public void testGetUsedNodes() throws XynaException {

    DependencyRegister register = new DependencyRegister("unused");

    register.addDependency(DependencySourceType.DATATYPE, "usedDataType1", DependencySourceType.FILTER,
                           "intermediateFilter1");
    register.addDependency(DependencySourceType.FILTER, "intermediateFilter1", DependencySourceType.TRIGGER,
                           "usingTrigger1");

    Set<DependencyNode> usedNonRecurse = register.getAllUsedNodes("usingTrigger1", DependencySourceType.TRIGGER, false);
    Iterator<DependencyNode> iter1 = usedNonRecurse.iterator();
    DependencyNode next1 = iter1.next();
    assertEquals("Unexpected number of used objects for trigger (non-recurse)", 1, usedNonRecurse.size());
    assertEquals("Unexpected name", "intermediateFilter1", next1.getUniqueName());
    assertEquals("Unexpected type", DependencySourceType.FILTER, next1.getType());

    Set<DependencyNode> usedRecurse = register.getAllUsedNodes("usingTrigger1", DependencySourceType.TRIGGER, true);
    assertEquals("Unexpected number of used objects for trigger (recurse)", 2, usedRecurse.size());
    Iterator<DependencyNode> iter2 = usedRecurse.iterator();
    while (iter2.hasNext()) {
    DependencyNode next2 = iter2.next();
    if (next2.getUniqueName().equals("intermediateFilter1"))
      assertEquals("Unexpected type", DependencySourceType.FILTER, next2.getType());
    else if  (next2.getUniqueName().equals("usedDataType1"))
      assertEquals("Unexpected type", DependencySourceType.DATATYPE, next2.getType());
    else
      fail ("Used objects contain unexpected object: " + next2.getUniqueName());
    }

  }

}
