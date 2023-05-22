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
package com.gip.xyna.xact.filter.xmom.workflows.json;



import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;



public class CopyJson extends PositionJson {

  private static final String LABEL_REVISION = "revision";
  private static final String LABEL_REMOVE = "removeFromClipboard";


  private int revision;
  private boolean removeFromClipboard;


  private CopyJson() {
  }

  public CopyJson(int revision, String relativeTo, RelativePosition relativePosition) {
    super(relativeTo, relativePosition);
    this.revision = revision;
  }

  public CopyJson(int revision, String relativeTo, int insideIndex) {
    super(relativeTo, insideIndex);
    this.revision = revision;
  }

  public static JsonVisitor<CopyJson> getJsonVisitor() {
    return new CopyJsonVisitor();
  }

  public int getRevision() {
    return revision;
  }

  public boolean getRemoveFromClipboard() {
    return removeFromClipboard;
  }


  private static class CopyJsonVisitor extends PositionJsonVisitor<CopyJson> {

    @Override
    protected CopyJson create() {
      return new CopyJson();
    }

    @Override
    public void attribute(String label, String value, Type type) {
      super.attribute(label, value, type);

      if (label.equals(LABEL_REVISION)) {
        get().revision = Integer.valueOf(value);
        return;
      }

      if (label.equals(LABEL_REMOVE)) {
        get().removeFromClipboard = Boolean.parseBoolean(value);
        return;
      }
    }

  }

}
