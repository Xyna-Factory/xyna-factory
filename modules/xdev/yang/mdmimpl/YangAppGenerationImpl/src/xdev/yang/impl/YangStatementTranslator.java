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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Anydata;
import org.yangcentral.yangkit.model.api.stmt.Anyxml;
import org.yangcentral.yangkit.model.api.stmt.Case;
import org.yangcentral.yangkit.model.api.stmt.Choice;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.LeafList;
import org.yangcentral.yangkit.model.api.stmt.Rpc;
import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.YangList;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.stmt.MainModuleImpl;



public class YangStatementTranslator {
  
  public static final Map<Class<?>, YangStatementTranslation> translations = setupStatementTranslations();


  private static Map<Class<?>, YangStatementTranslation> setupStatementTranslations() {
    Map<Class<?>, YangStatementTranslation> result = new HashMap<Class<?>, YangStatementTranslation>();
    result.put(Container.class, new YangStatementTranslation(Constants.TYPE_CONTAINER));
    result.put(Leaf.class, new YangStatementTranslation(Constants.TYPE_LEAF));
    result.put(Uses.class, new YangStatementTranslation(Constants.TYPE_USES));
    result.put(Rpc.class, new YangStatementTranslation(Constants.TYPE_RPC));
    result.put(Choice.class, new YangStatementTranslation(Constants.TYPE_CHOICE));
    result.put(Case.class, new YangStatementTranslation(Constants.TYPE_CASE));
    result.put(Anyxml.class, new YangStatementTranslation(Constants.TYPE_ANYXML));
    result.put(LeafList.class, new YangStatementTranslation(Constants.TYPE_LEAFLIST));
    result.put(YangList.class, new YangStatementTranslation(Constants.TYPE_LIST));
    result.put(Anydata.class, new YangStatementTranslation(Constants.TYPE_ANYDATA));
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


    public static String getNamespace(YangStatement statement) {
      if (statement.getContext().getNamespace() != null) {
        return statement.getContext().getNamespace().getUri().toString();
      } else if (statement.getContext().getCurModule() instanceof MainModuleImpl) {
        return ((MainModuleImpl) statement.getContext().getCurModule()).getNamespace().getUri().toString();
      }
      throw new RuntimeException("Namespace not found for " + statement);
    }


    public static List<YangElement> getSubStatements(YangStatement statement) {
      if (statement instanceof Uses) {
        Uses uses = (Uses) statement; 
        if (uses.getRefGrouping() == null) {
          return new ArrayList<YangElement>();
        }
        return uses.getRefGrouping().getSubElements();
      } else {
        return statement.getSubElements();
      }
    }

  }
}
