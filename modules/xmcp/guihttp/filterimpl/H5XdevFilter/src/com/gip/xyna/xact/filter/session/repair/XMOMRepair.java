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

package com.gip.xyna.xact.filter.session.repair;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;

import xmcp.processmodeller.datatypes.ErrorKeyValuePair;
import xmcp.processmodeller.datatypes.RepairEntry;



public class XMOMRepair {

  private Set<RepairInterface> repairInterfaces;
  private static Logger logger = CentralFactoryLogging.getLogger(XMOMRepair.class);


  public XMOMRepair() {
    repairInterfaces = new HashSet<RepairInterface>();
    repairInterfaces.add(new DOMRepair());
    repairInterfaces.add(new ExceptionGenerationRepair());
    repairInterfaces.add(new WorkflowRepair());
  }


  public List<RepairEntry> repair(GenerationBaseObject gbo) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    for (RepairInterface ri : repairInterfaces) {
      if (ri.responsible(gbo)) {
        result.addAll(ri.repair(gbo));
      }
    }

    if (result.size() > 0) {
      gbo.markAsModified();
    }

    return result;
  }


  public List<RepairEntry> getRepairEntries(GenerationBaseObject gbo) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    for (RepairInterface ri : repairInterfaces) {
      if (ri.responsible(gbo)) {
        try {
          result.addAll(ri.getRepairEntries(gbo));
        } catch (Exception e) {
          logger.warn("Exception during repair entry analysis for " + gbo.getFQName(), e);
        }
      }
    }

    return result;
  }


  /***
   * 
   * @param loc
   *   location mentioned in returned RepairEntry objects
   * @param vars
   *   variables to check
   * @param creatId
   *   function to create objectID for converted variable, given the index of it in variable list
   * @param apply
   *   true to execute conversion (repair), false to getRepairEntries only
   * @return
   *   List of all repairs done/required for this list of variables
   */
  public static List<RepairEntry> convertAVariableList(String loc, List<AVariable> vars, Function<Integer, String> creatId, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    if (vars == null) {
      return result;
    }

    for (int i = 0; i < vars.size(); i++) {
      AVariable avar = vars.get(i);

      if (!variableHasToBeConverted(avar)) {
        continue;
      }

      String objId = creatId.apply(i);
      RepairEntry entry = createVarConvertedRepairEntry(loc, avar, objId);
      result.add(entry);
      if (apply) {
        convertVariableToPrototype(avar);
      }

    }

    return result;
  }


  public static void convertVariableToPrototype(AVariable avar) {
    String oldId = avar.getId();
    avar.createPrototype(avar.getLabel());
    avar.setId(oldId);
  }


  public static boolean variableHasToBeConverted(AVariable avar) {
    if (avar == null || avar.isPrototype() || avar.getDomOrExceptionObject() == null) {
      return false;
    }

    DomOrExceptionGenerationBase doe = avar.getDomOrExceptionObject();

    if (!doe.exists()) {
      return true;
    }

    if (!canRetrieveRootTag(doe)) {
      return true;
    }

    return false;
  }


  public static RepairEntry createVarConvertedRepairEntry(String location, AVariable avar, String objId) {
    RepairEntry result = new RepairEntry();

    result.setDescription("Variabletype not found.");
    result.setId(objId);
    result.setLocation(location);
    result.setType("Convert to Prototype Variable");

    return result;
  }


  public static boolean canRetrieveRootTag(DomOrExceptionGenerationBase doe) {
    try {
      DomOrExceptionGenerationBase.retrieveRootTag(doe.getOriginalFqName(), doe.getRevision());
      return true;
    } catch (Ex_FileAccessException e) {
      return false;
    } catch (XPRC_XmlParsingException e) {
      return false;
    }
  }


  public static List<ErrorKeyValuePair> convertForError(List<RepairEntry> entries) {
    List<ErrorKeyValuePair> result = new ArrayList<ErrorKeyValuePair>();

    for (RepairEntry entry : entries) {
      ErrorKeyValuePair ekvp = new ErrorKeyValuePair();
      ekvp.setKey(entry.getType());
      ekvp.setValue(entry.getDescription());
      result.add(ekvp);
    }

    return result;
  }
}
