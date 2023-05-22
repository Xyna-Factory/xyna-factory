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
package com.gip.xyna.xfmg.xfctrl.deploystate;



public interface DeploymentItemInterface {

  public boolean resolve();
  
  public String getDescription();
  
  
  public static interface MatchableInterface extends DeploymentItemInterface {
    
    /**
     * Im Gegensatz zu equals wird hier auf Kompatibilit�t �berpr�ft, nicht auf Identit�t.
     * Beispiel: Operation X matches Y, auch wenn die OutputTypen oder InputTypen von einander abgeleitet sind
     * 
     * Achtung: Bei A.matches(B) wird �berpr�ft, ob B mit der Definition von A kompatibel ist, nicht andersherum.
     */
    public boolean matches(DeploymentItemInterface other);
    
    public String getName();
    
  }
  
}
