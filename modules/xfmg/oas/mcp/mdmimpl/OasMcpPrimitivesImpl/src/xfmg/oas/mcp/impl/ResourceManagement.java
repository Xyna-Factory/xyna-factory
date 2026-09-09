/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xfmg.oas.mcp.impl;



import java.nio.charset.StandardCharsets;

import xact.templates.Document;



public class ResourceManagement {


  public static Document loadResource(String path) {
    try (java.io.InputStream is = ResourceManagement.class.getClassLoader().getResourceAsStream("resources/" + path)) {
      if (is == null) {
        return new Document.Builder().text("{}").instance();
      }
      String data = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      return new Document.Builder().text(data).instance();
    } catch (Exception e) {
      return new Document.Builder().text("{}").instance();
    }
  }
}
