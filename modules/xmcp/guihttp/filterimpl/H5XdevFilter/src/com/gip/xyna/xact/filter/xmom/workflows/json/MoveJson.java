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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;

public class MoveJson extends PositionJson {

  public enum ConflictHandling { USE_DESTINATION, USE_SOURCE, APPEND }


  private static final String LABEL_REVISION = "revision";
  private static final String LABEL_FORCE = "force";
  private static final String LABEL_CONFLICT_HANDLING = "conflictHandling";

  private int revision;
  private boolean force;
  private ConflictHandling conflictHandling = null;


  private MoveJson() {
  }

  public MoveJson(int revision, String relativeTo, RelativePosition relativePosition) {
    super(relativeTo, relativePosition);
    this.revision = revision;
  }
  
  public MoveJson(int revision, String relativeTo, int insideIndex) {
    super(relativeTo, insideIndex);
    this.revision = revision;
  }


  public static JsonVisitor<MoveJson> getJsonVisitor() {
   return new MoveJsonVisitor();
  }

  public int getRevision() {
    return revision;
  }

  public boolean isForce() {
    return force;
  }
  
  public ConflictHandling getConflictHandling() {
    return conflictHandling;
  }


  private static class MoveJsonVisitor extends PositionJsonVisitor<MoveJson> {
    @Override
    protected MoveJson create() {
      return new MoveJson();
    }

    @Override
    public void attribute(String label, String value, Type type) {
      super.attribute(label, value, type);
      if (LABEL_REVISION.equals(label)) {
        get().revision = Integer.valueOf(value);
        return;
      }

      if (LABEL_FORCE.equals(label)) {
        get().force = Boolean.valueOf(value);
        return;
      }
      
      if (LABEL_CONFLICT_HANDLING.equals(label)) {
        get().conflictHandling = ConflictHandling.valueOf(value);
        return;
      }
    }

  }

}
