/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xdev.yang.impl;



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;



public class YangStatementTranslator {

  public static final Map<Class<?>, YangStatementTranslation> translations = setupStatementTranslations();


  private static Map<Class<?>, YangStatementTranslation> setupStatementTranslations() {
    Map<Class<?>, YangStatementTranslation> result = new HashMap<Class<?>, YangStatementTranslation>();
    result.put(Container.class, new YangStatementTranslation(Constants.TYPE_CONTAINER));
    result.put(Leaf.class, new YangStatementTranslation(Constants.TYPE_LEAF));
    result.put(Uses.class, new YangStatementTranslation(Constants.TYPE_USES));
    return result;
  }


  public static YangStatementTranslation getTranslation(Class<?> yangStatementClass) {
    return translations.get(yangStatementClass);
  }


  public static class YangStatementTranslation {

    private String yangStatementName;


    public YangStatementTranslation(String yangStatementName) {
      this.yangStatementName = yangStatementName;

    }


    public String getYangStatementName() {
      return yangStatementName;
    }


    public static String getLocalName(YangStatement statement) {
      return statement.getArgStr();
    }


    public static String getUriString(YangStatement statement) {
      return statement.getContext().getNamespace().getUri().toString();
    }


    public static List<YangElement> getSubStatements(YangStatement statement) {
      if (statement instanceof Uses) {
        return ((Uses) statement).getRefGrouping().getSubElements();
      } else {
        return statement.getSubElements();
      }
    }


  }
}
