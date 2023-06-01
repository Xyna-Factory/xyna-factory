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
package com.gip.xyna.xfmg.xfctrl.rtctxmgmt;

import java.util.Collection;

import com.gip.xyna.update.Version;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;

/**
 * Fehler bei der Ausführung (RuntimeExceptions/Error) werden nur geloggt 
 */
public interface RuntimeContextChangeHandler {
  
  // Mgmt
  
  String getName();
  
  Version getVersion();
  
  void displacedByNewVersion();
  
  // handling
  
  void creation(RuntimeDependencyContext rc);
  
  void removal(RuntimeDependencyContext rc);
  
  /**
   * wird nach dem umsetzen der runtimecontext-dependencies aufgerufen
   */
  void migration(RuntimeDependencyContext from, RuntimeDependencyContext to);
  
  /**
   * wird nicht aufgerufen, falls ein rtc-change durch ein migrate ausgelöst wird. stattdessen wird die methode migration(from, to) aufgerufen.
   */
  void dependencyChanges(RuntimeDependencyContext of, Collection<RuntimeDependencyContext> oldDependencies , Collection<RuntimeDependencyContext> newDependencies);
  
}