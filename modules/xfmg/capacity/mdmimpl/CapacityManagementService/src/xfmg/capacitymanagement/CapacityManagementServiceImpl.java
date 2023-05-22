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
package xfmg.capacitymanagement;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.SchedulerBean;

import xprc.xpce.Application;
import xprc.xpce.OwnContext;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;



public class CapacityManagementServiceImpl {

  protected CapacityManagementServiceImpl() {
  }


  public static CapacityChangeResult changeCapacityState(CapacityName capacityName, CapacityState capacityState)
      throws XynaException {

    if (capacityName == null)
      throw new IllegalArgumentException("Capacity Name may not be null");
    if (capacityState == null)
      throw new IllegalArgumentException("Capacity State may not be null");
    String capName = capacityName.getName();
    State state;
    if (capacityState.getDisabled()) {
      state = State.DISABLED;
    } else {
      state = State.ACTIVE;
    }

    boolean success = XynaFactory.getPortalInstance().getProcessingPortal().changeCapacityState(capName, state);
    return new CapacityChangeResult(success);

  }


  public static CapacityChangeResult changeCapacityCardinality(CapacityName capacityName,
                                                               CapacityCardinality capacityCardinality)
      throws XynaException {

    if (capacityName == null) {
      throw new IllegalArgumentException("Capacity Name may not be null");
    }
    if (capacityCardinality == null) {
      throw new IllegalArgumentException("Capacity Cardinality may not be null");
    }
    String capName = capacityName.getName();
    Integer capCard = capacityCardinality.getCardinality();

    boolean success = XynaFactory.getPortalInstance().getProcessingPortal().changeCapacityCardinality(capName, capCard);
    return new CapacityChangeResult(success);


  }


  public static CapacityChangeResult addCapacity(CapacityDefinition capacityDef) throws XynaException {

    if (capacityDef == null) {
      throw new IllegalArgumentException("Capacity Definition may not be null");
    }

    if (capacityDef.getName() == null) {
      throw new IllegalArgumentException("The name of the capacity to be added may not be null");
    }
    if (capacityDef.getCardinality() == null) {
      throw new IllegalArgumentException("The cardinality of the capacity to be added may not be null");
    }

    String name = capacityDef.getName().getName();
    Integer cardinality = capacityDef.getCardinality().getCardinality();
    boolean disabled = false;
    if (capacityDef.getState() != null && capacityDef.getState().getDisabled()) {
      disabled = true;
    }

    State state;
    if (disabled) {
      state = State.DISABLED;
    } else {
      state = State.ACTIVE;
    }

    boolean success = XynaFactory.getPortalInstance().getProcessingPortal().addCapacity(name, cardinality, state);
    return new CapacityChangeResult(success);

  }


  public static CapacityChangeResult removeCapacity(CapacityName capacityName) throws XynaException {
    if (capacityName == null) {
      throw new IllegalArgumentException("Capacity name may not be null");
    }
    boolean success = XynaFactory.getPortalInstance().getProcessingPortal().removeCapacity(capacityName.getName());
    return new CapacityChangeResult(success);
  }


  public static XynaObjectList<CapacityDefinition> getAllCapacityDefinitions() throws XynaException {

    ArrayList<CapacityDefinition> result = new ArrayList<CapacityDefinition>();
    Collection<CapacityInformation> caps = XynaFactory.getPortalInstance().getProcessingPortal().listCapacityInformation();

    for (CapacityInformation info : caps) {
      boolean disabled = false;
      if (info.getState().equals(State.DISABLED)) {
        disabled = true;
      }
      result.add(new CapacityDefinition(new CapacityName(info.getName()),
                                        new CapacityCardinality(info.getCardinality()), new CapacityState(disabled)));
    }

    return new XynaObjectList<CapacityDefinition>(result, CapacityDefinition.class);

  }


  public static XynaObjectList<CapacityDefinition> getAllCapacitiesRequiredForOrderType(OrderType orderType)
      throws XynaException {
    return getAllCapacitiesRequiredForOrderTypeInRuntimeContext(orderType, null);
  }
  
  
  public static XynaObjectList<CapacityDefinition> getAllCapacitiesRequiredForOrderTypeInRuntimeContext(OrderType orderType, RuntimeContext rc)
                  throws XynaException {
    XynaObjectList<CapacityDefinition> result =
        new XynaObjectList<CapacityDefinition>(new ArrayList<CapacityDefinition>(), CapacityDefinition.class);

    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext internalRc = convertRuntimeContextOrDefault(rc);
    List<Capacity> caps =
        XynaFactory.getPortalInstance().getProcessingPortal().listCapacitiesForOrderType(new DestinationKey(orderType.getOrderType(), internalRc));
    if (caps != null) {
      for (Capacity c : caps) {
        CapacityDefinition newDef =
            new CapacityDefinition(new CapacityName(c.getCapName()), new CapacityCardinality(c.getCardinality()), new CapacityState(false));
        result.add(newDef);
      }
    }

    return result;
  }
  


  @Deprecated
  public static CapacityChangeResult requireCapacityForWorkflow(WorkflowName workflowName, CapacityName capName,
                                                                CapacityCardinality capCard) throws XynaException {
    boolean success =
        XynaFactory.getPortalInstance().getProcessingPortal()
            .requireCapacityForWorkflow(workflowName.getWorkflowName(), capName.getName(), capCard.getCardinality());
    return new CapacityChangeResult(success);
  }


  public static CapacityChangeResult requireCapacityForOrderType(OrderType orderType, CapacityName capName,
                                                                 CapacityCardinality capCard) throws XynaException {
    return requireCapacityForOrderTypeInRuntimeContext(orderType, null, capName, capCard);
  }
  
  public static CapacityChangeResult requireCapacityForOrderTypeInRuntimeContext(OrderType orderType, RuntimeContext rc, CapacityName capName,
                                                                                 CapacityCardinality capCard) throws XynaException {
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext internalRc = convertRuntimeContextOrDefault(rc);
    boolean success =
        XynaFactory.getPortalInstance().getProcessingPortal()
            .requireCapacityForOrderType(orderType.getOrderType(), capName.getName(), capCard.getCardinality(), internalRc);
    return new CapacityChangeResult(success);
  }


  @Deprecated
  public static CapacityChangeResult removeCapacityForWorkflow(WorkflowName workflowName, CapacityName capName)
      throws XynaException {
    boolean success =
        XynaFactory.getPortalInstance().getProcessingPortal()
            .removeCapacityForWorkflow(workflowName.getWorkflowName(), capName.getName());
    return new CapacityChangeResult(success);
  }


  public static CapacityChangeResult removeCapacityForOrderType(OrderType orderType, CapacityName capName)
      throws XynaException {
    return removeCapacityForOrderTypeInRuntimeContext(orderType, null, capName);
  }
  
  public static CapacityChangeResult removeCapacityForOrderTypeInRuntimeContext(OrderType orderType, RuntimeContext rc, CapacityName capName)
                  throws XynaException {
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext internalRc = convertRuntimeContextOrDefault(rc);
    boolean success =
        XynaFactory.getPortalInstance().getProcessingPortal().removeCapacityForOrderType(orderType.getOrderType(), capName.getName(), internalRc);
    return new CapacityChangeResult(success);
  }


  public static SchedulerBean getSchedulerBeanForCurrentOrder(XynaOrderServerExtension correlatedXynaOrder)
      throws XynaException {
    List<Capacity> caps =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
            .getCapacities(correlatedXynaOrder.getDestinationKey());

    if (caps != null) {
      return new SchedulerBean(caps);
    } else {
      return new SchedulerBean();
    }
  }
  
  
  private static com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext convertRuntimeContextOrDefault(RuntimeContext rc) {
    if (rc == null) {
      return RevisionManagement.DEFAULT_WORKSPACE;
    }
    if (rc instanceof Application) {
      Application app = (Application) rc;
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application(app.getName(), app.getVersion());
    } else if (rc instanceof Workspace) {
      Workspace ws = (Workspace) rc;
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(ws.getName());
    } else if (rc instanceof OwnContext) {
      // TODO inside Mgmt-App? RootOrder?
      return RevisionManagement.DEFAULT_WORKSPACE;
    } else {
      return RevisionManagement.DEFAULT_WORKSPACE;
    }
  }

}
