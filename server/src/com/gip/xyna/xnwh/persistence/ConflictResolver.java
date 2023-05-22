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
package com.gip.xyna.xnwh.persistence;

import java.util.List;

/**
 * Interface z.b. f�r Storables zur Beschreibung von Konfliktaufl�sungen.
 * 
 * Konflikten treten auf, wenn mehrere Kopien eines Objektes existieren, die das gleiche Objekt
 * beschreiben sollen, und diese in ein stellvertretendes Objekt zusammengef�hrt werden sollen.
 * 
 * Ein anderer m�glicher Konflikt tritt auf, wenn eine Programminstanz behauptet, dass Objekt
 * gel�scht zu haben, w�hrend die andere meint, dass es noch existiert.
 *
 */
public interface ConflictResolver<T> {
  
  /**
   * Methode zur Bereinigung von Konflikten auf einem Objekt.
   * 
   * @param conflictingObjects Liste der konfliktbehafteten Objekte.
   * @param hasBeenDeleted true falls der Konflikt deswegen besteht, weil das Objekt irgendwo als gel�scht gilt
   * @return null falls das Objekt gel�scht werden soll oder das konfliktbereinigte Objekt
   */
  public T resolveConflicts(List<T> conflictingObjects, boolean hasBeenDeleted);
}
