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
package com.gip.xyna.xact.filter.session.repair;



import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;

import xmcp.processmodeller.datatypes.RepairEntry;



public abstract class DOMandExceptionRepair<T extends DomOrExceptionGenerationBase> implements RepairInterface {

  private static final String memberRepairString = "Class of member object %s (%s) does not exist.";
  private static final String parentRepairString = "Parent Object %s does not exist.";


  protected abstract T getDomOrExceptionGenerationBaseObj(GenerationBaseObject obj);


  protected abstract void replaceParent(T obj);


  @Override
  public List<RepairEntry> getRepairEntries(GenerationBaseObject gbo) {
    return repair(gbo, false);
  }


  @Override
  public List<RepairEntry> repair(GenerationBaseObject gbo) {
    return repair(gbo, true);
  }


  private List<RepairEntry> repair(GenerationBaseObject gbo, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<RepairEntry> re = null;
    T dom = getDomOrExceptionGenerationBaseObj(gbo);

    re = repairMembers(dom, apply);
    result.addAll(re);
    re = repairParent(dom, apply);
    result.addAll(re);

    return result;
  }


  private List<RepairEntry> repairParent(T dom, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();

    if (!dom.hasSuperClassGenerationObject()) {
      return result;
    }


    DomOrExceptionGenerationBase parentDOM = dom.getSuperClassGenerationObject();
    if (parentDomNeedsRepairs(parentDOM)) {
      RepairEntry entry = createParentRepairEntry(dom);
      result.add(entry);
      
      if (apply) {
        replaceParent(dom);
      }
    }

    return result;
  }


  private boolean parentDomNeedsRepairs(DomOrExceptionGenerationBase parent) {
    //if parent does not exist, we need to repair
    return !parent.exists();
  }


  protected List<RepairEntry> repairMembers(DomOrExceptionGenerationBase doe, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<AVariable> members = doe.getMemberVars();
    List<MemberSubstitute> substitutes = new ArrayList<DOMandExceptionRepair<T>.MemberSubstitute>();

    for (AVariable member : members) {
      if (memberNeedsRepairs(member)) {
        MemberSubstitute sub = createSubstitute(member);
        RepairEntry entry = createMemberRepairEntry(sub, doe);

        substitutes.add(sub);
        result.add(entry);
      }
    }

    if (apply) {
      for (MemberSubstitute sub : substitutes) {
        doe.replaceMemberVar(sub.getCurrentMember(), sub.getReplacement());
      }
    }


    return result;
  }


  private boolean memberNeedsRepairs(AVariable member) {
    DomOrExceptionGenerationBase doe = null;

    if (member instanceof DatatypeVariable) {
      doe = ((DatatypeVariable) member).getDomOrExceptionObject();
    } else if (member instanceof ExceptionVariable) {
      doe = ((ExceptionVariable) member).getDomOrExceptionObject();
    } else {
      throw new RuntimeException("unexpected AVariable subtype: " + member.getClass());
    }

    //if there is no DomOrExceptionGenerationBase, member is already a simple type
    if (doe == null)
      return false;

    //if DOM or Exception Object does not exist, we need to repair
    return !doe.exists();
  }


  private RepairEntry createParentRepairEntry(DomOrExceptionGenerationBase dom) {
    RepairEntry result = new RepairEntry();

    result.setDescription(String.format(parentRepairString, dom.getSuperClassGenerationObject().getFqClassName()));
    result.setId(ObjectType.typeInfoArea.toString());
    result.setLocation("Parent");
    result.setResource(dom.getFqClassName());
    result.setType("Parent replacement");

    return result;
  }


  private RepairEntry createMemberRepairEntry(MemberSubstitute sub, DomOrExceptionGenerationBase doe) {
    RepairEntry result = new RepairEntry();

    result.setDescription(String.format(memberRepairString, sub.getCurrentMember().getLabel(), sub.getCurrentMember().getFQClassName()));
    result.setId(ObjectId.createMemberVariableId(doe.getMemberVars().indexOf(sub.getCurrentMember())));
    result.setLocation("Member");
    result.setResource(doe.getFqClassName());
    result.setType("Member type replacement");

    return result;
  }


  private MemberSubstitute createSubstitute(AVariable member) {
    MemberSubstitute result = new MemberSubstitute();
    DatatypeVariable substitute = null;

    substitute = new DatatypeVariable(member.getCreator(), -1L);
    substitute.create(PrimitiveType.STRING);
    substitute.setLabel(member.getLabel());
    substitute.setPersistenceTypes(member.getPersistenceTypes());
    substitute.setIsList(member.isList());
    substitute.setVarName(member.getVarName());


    result.setCurrentMember(member);
    result.setReplacement(substitute);

    return result;
  }


  private class MemberSubstitute {

    private AVariable currentMember;
    private AVariable replacement;


    public void setCurrentMember(AVariable av) {
      currentMember = av;
    }


    public void setReplacement(AVariable av) {
      replacement = av;
    }


    public AVariable getCurrentMember() {
      return currentMember;
    }


    public AVariable getReplacement() {
      return replacement;
    }
  }
}
