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
package com.gip.xyna.xact.filter.serialization;



import java.util.Map;
import java.util.function.Function;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;

import xint.mcp.schema.Content;
import xint.mcp.schema.TextContent;



public class ContentSerializer implements JsonSerializable {

  private static final Map<Class<?>, Function<Content, JsonSerializable>> serializers =
      Map.of(TextContent.class, c -> new TextContentSerializer((TextContent) c));

  private final Content content;


  public ContentSerializer(Content content) {
    this.content = content;
  }


  @Override
  public void toJson(JsonBuilder jb) {
    JsonSerializable serializer = serializers.get(content.getClass()).apply(content);
    serializer.toJson(jb);
  }


  public static class TextContentSerializer implements JsonSerializable {

    private final TextContent content;


    public TextContentSerializer(TextContent content) {
      this.content = content;
    }


    @Override
    public void toJson(JsonBuilder jb) {
      jb.addStringAttribute("type", "text");
      jb.addStringAttribute("text", content.getText());
      if (content.getAnnotations() != null) {
        jb.addObjectAttribute("annotations", new AnnotationsSerializer(content.getAnnotations()));
      }
    }

  }

}
