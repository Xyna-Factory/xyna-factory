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
package com.gip.xyna.xprc.xfractwfe.base;



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;


/**
 * lokale variablen und steps befinden sich in einer scope. eine scope hat input und outputvariablen
 */
public interface Scope {

  Scope getParentScope();

  void setInputVars(GeneralXynaObject o) throws XynaException;

  /**
   * wieviel InputVars werden benötigt?
   */
  int getNeededInputVarsCount();
  
  GeneralXynaObject getOutput();

  /**
   * steps to be started in the beginning to execute this scope
   */
  FractalProcessStep<?>[] getStartSteps();

  /**
   * alle steps des aktuellen scopes inkl aller steps der verschachtelten scopes. 
   * bei paralleler ausführung von threads ist hier nicht sichergestellt, dass alle 
   * steps zurückgegeben werden, da diese evtl erst dynamisch erzeugt werden (for-each).
   */
  FractalProcessStep<?>[] getAllSteps();

  /**
   * alle steps die nicht zu einer verschachtelten scope gehören
   */
  FractalProcessStep<?>[] getAllLocalSteps();

}
