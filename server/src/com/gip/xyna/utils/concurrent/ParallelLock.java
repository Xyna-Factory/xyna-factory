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
package com.gip.xyna.utils.concurrent;

/**
 * ParallelLock gibt die M�glichkeit, parallel nebeneinander �hnliche Locks zu halten. 
 * Bei Locken und Unlocken wird ein Lock-Object �bergeben, anhand dessen der Lock vorgenomment wird.
 * 
 * Dabei ist erforderlich, dass das Lock-Object {@link Object#hashCode()} und {@link Object#equals(Object)}
 * korrekt implementiert.
 * 
 * Damit kann erreicht werden, dass zwei Operationen zu verschiedenen Lock-Objecten parallel laufen k�nnen, 
 * zwei Operationen zum gleichen Lock-Object jedoch seriell laufen m�ssen.
 *
 */
public interface ParallelLock<T> {

  public void lock(T lockObject);
  
  public void unlock(T lockObject);
  
  public boolean tryLock(T lockObject);
  
  //public boolean tryLock(T lockObject, long time, TimeUnit unit); schwer zu implementieren daher erstmal nicht vorgeschrieben
  
  
}
