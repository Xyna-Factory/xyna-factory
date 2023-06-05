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
package com.gip.xyna.xact.filter.util;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.vars.ConstPermission;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;

/*
 * abstrahiert inputs/outputs von workflow-schritten etc
 * insbesondere haben manche schritte keine leichtzuordbaren output-variablen im step/generationbase-kontext:
 * - stepfunction: output-variablen liegen als globale variablen vor, mit der id, die im receive element steckt
 * - stepchoice: die outputs sind berechnet (mindestens teilweise) und nur über die assigns sichtbar
 * - etc. 
 */
public abstract class AVariableIdentification {

  boolean isDeletable = true;
  boolean isReadonly = false;
  ConstPermission constPermission = ConstPermission.NEVER;
  boolean allowCast = false;
  
  private static final Logger logger = CentralFactoryLogging.getLogger(AVariableIdentification.class);


  public static enum VarUsageType {
    input, output, thrown;
  }
  
  public interface Connectedness {

    boolean isUserConnected();
    public String getConnectedVariableId();
    boolean isConstantConnected();
   
  }
  
  public interface InternalGUIIdGeneration {
    public String createId();
  }
  
  public interface StepVariableIdProvider {
    public String getId();
  }
  
  public static class UseAVariable implements StepVariableIdProvider {
    
    private final AVariableIdentification a;
    
    public UseAVariable(AVariableIdentification a) {
      this.a = a;  
    }

    @Override
    public String getId() {
      return a.getIdentifiedVariable().getId();
    }
    
  }
  
  public static class ThrowExceptionIdProvider implements StepVariableIdProvider {
    
    @Override
    public String getId() {
      throw new RuntimeException();
    }
  }
  
  //gibt die id der outputvariable im step-/generationbasecontext zurück
  public StepVariableIdProvider idprovider = new ThrowExceptionIdProvider();
  
  //daten zur input-connection
  public Connectedness connectedness = new Connectedness() {
    
    public String getConnectedVariableId() {
      return null;
    }
    
    @Override
    public boolean isUserConnected() {
      return false;
    }

    @Override
    public boolean isConstantConnected() {
      return false;
    }
  };
  //default, wird dann überschrieben. das ist die id, die von der gui/im json benutzt wird
  public InternalGUIIdGeneration internalGuiId = new InternalGUIIdGeneration() {
    
    @Override
    public String createId() {
      return "gbxmlid-" + getIdentifiedVariable().getId();
    }
  };

  public GeneralXynaObject getConstantValue(GenerationBaseObject gbo) {
    if (!connectedness.isConstantConnected()) {
      return null;
    }

    try {
      return Utils.getGlobalConstVar(connectedness.getConnectedVariableId(), gbo.getWFStep()).getXoRepresentation();
    } catch (Exception e) {
      Utils.logError("Could not determine constant value for variable " + internalGuiId, e);
      return null;
    }
  }

  public void setDeletable(boolean isDeletable) {
    this.isDeletable = isDeletable;
  }

  public boolean isDeletable() {
    return isDeletable;
  }

  public void setReadonly(boolean isReadonly) {
    this.isReadonly = isReadonly;
  }

  public boolean isReadonly() {
    return isReadonly;
  }

  public void setConstPermission(ConstPermission constPermission) { // TODO: in setConstPermission umbenennen
    this.constPermission = constPermission;
  }

  public ConstPermission getConstPermission() {
    return constPermission;
  }

  public void setAllowCast(boolean allowCast) {
    this.allowCast = allowCast;
  }

  public boolean getAllowCast() {
    return allowCast;
  }


  public abstract AVariable getIdentifiedVariable();

  /**
   * das setzen des labels kann nicht immer direkt auf der "AVariable" durchgeführt werden, weil AVariable manchmal nur eine referenz eines anderen objektes ist.
   * beispiel: stepfunction.inputvariablen referenzieren die inputvariablen des referenzierten services.
   *           wenn das label am aufruf geändert werden soll, ist das nicht gleichbedeutend mit einer änderung in der service(-definition)
   */
  public abstract void setLabel(String label);
  
  public abstract AVariableIdentification createClone();

}
