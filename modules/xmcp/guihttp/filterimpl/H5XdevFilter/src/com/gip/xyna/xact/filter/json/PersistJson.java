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
package com.gip.xyna.xact.filter.json;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;

public class PersistJson {

  private static final String LABEL_REVISION = "revision";
  private static final String LABEL_FORCE = "force";
  private static final String LABEL_LABEL = "label";
  private static final String LABEL_PATH = "path";
  
  private int revision;
  private boolean force;
  private String path;
  private String label;

  
  private PersistJson() {}
  
  public PersistJson(int revision, boolean force) {
    this.revision = revision;
    this.force = force;
  }
 
  public int getRevision() {
    return revision;
  }
  
  public String getLabel() {
    return label;
  }
  
  public boolean isForce() {
    return force;
  }
  
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public static JsonVisitor<PersistJson> getJsonVisitor() {
    return new SaveJsonVisitor();
  }

  private static class SaveJsonVisitor extends EmptyJsonVisitor<PersistJson> {
    PersistJson sj = new PersistJson();

    @Override
    public PersistJson get() {
      return sj;
    }
    @Override
    public PersistJson getAndReset() {
      PersistJson ret = sj;
      sj = new PersistJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(LABEL_REVISION) ) {
        sj.revision = Integer.valueOf(value);
        return;
      }
      if (label.equals(LABEL_FORCE)) {
        sj.force = Boolean.valueOf(value);
        return;
      }
      if( label.equals(LABEL_LABEL) ) {
        sj.label = value;
        return;
      }
      if( label.equals(LABEL_PATH) ) {
        sj.path = removeInvalidPathChars(value);
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }

  private static String removeInvalidPathChars(String requestedPath) {
    String path = requestedPath.replaceAll("[^a-zA-Z0-9.]_", "");

    while (path.startsWith(".") || path.endsWith(".")) {
      // remove leading period if existing
      if (path.startsWith(".")) {
        path = path.replaceFirst(".", "");
      }
  
      // remove trailing period if existing
      if (path.endsWith(".")) {
        if (path.length() > 1) {
          path = path.substring(0, path.length()-1);
        } else {
          path = "";
        }
      }
    }

    return path;
  }

}
