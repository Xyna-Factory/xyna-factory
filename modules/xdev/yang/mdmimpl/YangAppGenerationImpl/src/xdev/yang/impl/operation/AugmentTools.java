/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yangcentral.yangkit.base.YangContext;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Anydata;
import org.yangcentral.yangkit.model.api.stmt.Anyxml;
import org.yangcentral.yangkit.model.api.stmt.Augment;
import org.yangcentral.yangkit.model.api.stmt.Choice;
import org.yangcentral.yangkit.model.api.stmt.Container;
import org.yangcentral.yangkit.model.api.stmt.Description;
import org.yangcentral.yangkit.model.api.stmt.Grouping;
import org.yangcentral.yangkit.model.api.stmt.IfFeature;
import org.yangcentral.yangkit.model.api.stmt.Leaf;
import org.yangcentral.yangkit.model.api.stmt.LeafList;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.StatusStmt;
import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.When;
import org.yangcentral.yangkit.model.api.stmt.YangList;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.stmt.DescriptionImpl;

import com.gip.xyna.utils.collections.Pair;

import org.yangcentral.yangkit.common.api.QName;

import xdev.yang.impl.YangStatementTranslator.YangStatementTranslation;


public class AugmentTools {

  private static Pattern qNamePattern = Pattern.compile("\\/([^:]*:)?([^\\/]+)");
  
  
  private PathTools _pathTools = new PathTools();
  
  public void handleAugment(List<Module> modules) {
    Map<String, Module> moduleNameMap = new HashMap<>();
    for (Module mod : modules) {
      if (mod.getAugments() == null) { continue; }
      for (Augment aug : mod.getAugments()) {
        Pair<List<QName>, Module> path = createQNamePath(aug, mod, modules, moduleNameMap);
        if(path.getFirst().isEmpty()) { continue; }
        List<YangElement> sublist = YangStatementTranslation.getSubStatements(path.getSecond());
        
        for (YangElement elem : sublist) {
          if (elem instanceof YangStatement) {
            handleAugment((YangStatement)elem, aug, path.getFirst());
          }
        }
      }
    }
  }
  
  
  private Pair<List<QName>, Module> createQNamePath(Augment aug, Module mod, List<Module> modules, Map<String, Module> moduleNameMap) {
    List<QName> result = new ArrayList<QName>();
    Matcher matcher = qNamePattern.matcher(aug.getArgStr());
    Module firstMod = null;
    while (matcher.find()) {
      String importPrefix = matcher.group(1).substring(0, matcher.group(1).length() - 1);
      String moduleName = mod.getImportByPrefix(importPrefix).getArgStr();
      Module m = moduleNameMap.computeIfAbsent(moduleName, (s) -> findModule(modules, s));
      if(m == null) { return new Pair<>(Collections.emptyList(), mod); }
      if(firstMod == null) {
        firstMod = m;
      }
      String namespace = m.getContext().getNamespace().getUri().toString();
      QName name = new QName(namespace, matcher.group(2));
      result.add(name);
    }

    return new Pair<>(result, firstMod);
  }
  
  private Module findModule(List<Module> modules, String moduleName) {
    for(Module m : modules) {
      if(m.getArgStr().equals(moduleName)) {
        return m;
      }
    }
    return null;
  }


  public void handleAugment(YangStatement root, Augment aug, List<QName> path) {
    if (root == null) { return; }
    if (aug == null) { return; }
    if (path == null) { return; }
    Optional<YangStatement> opt = _pathTools.navigateToPath(root, path, 0);
    if (!opt.isPresent()) { return; }
    List<YangElement> filtered = filterElementsToAdd(aug);
    for (YangElement elem : filtered) {
      opt.get().addChild(elem);
    }
  }
  
  
  private List<YangElement> filterElementsToAdd(Augment aug) {
    List<YangElement> ret = new ArrayList<>();
    StringBuilder comment = new StringBuilder();
    StringBuilder conditions = new StringBuilder();
    for (YangElement elem : aug.getSubElements()) {
      if (elem instanceof Anyxml) { ret.add(elem); }
      else if (elem instanceof Anydata) { ret.add(elem); }
      else if (elem instanceof Choice) { ret.add(elem); }
      else if (elem instanceof Container) { ret.add(elem); }
      else if (elem instanceof Grouping) { ret.add(elem); }
      else if (elem instanceof Leaf) { ret.add(elem); }
      else if (elem instanceof LeafList) { ret.add(elem); }
      else if (elem instanceof YangList) { ret.add(elem); }
      else if (elem instanceof StatusStmt) { ret.add(elem); }
      else if (elem instanceof Uses) { ret.add(elem); }
      else if (elem instanceof Description) {
        if (comment.length() > 0) { comment.append("; "); }
        comment.append(((Description)elem).getArgStr());
      }
      else if (elem instanceof When) {
        if (conditions.length() > 0) { conditions.append(", "); }
        conditions.append("CONDITION: ").append(elem.toString());
      }
      else if (elem instanceof IfFeature) {
        if (conditions.length() > 0) { conditions.append(", "); }
        conditions.append("CONDITION: ").append(elem.toString());
      }
    }
    if (comment.length() > 0) {
      if (conditions.length() > 0) {
        conditions.append("; ");
      }
      conditions.append(comment);
    }
    if (conditions.length() > 0) {
      this.handleDescription(ret, conditions, aug.getContext());
    }
    return ret;
  }
  
  
  private void handleDescription(List<YangElement> ret, StringBuilder str, YangContext context) {
    YangSubelementContentHelper helper = new YangSubelementContentHelper();
    for (YangElement elem : ret) {
      if (!(elem instanceof YangStatement)) { continue; }
      YangStatement ys = (YangStatement) elem;
      Description existing = helper.getDescriptionSubelementOrNull(ys);
      if (existing == null) {
        DescriptionImpl desc = new DescriptionImpl(str.toString());
        desc.setContext(context);
        ys.addChild(desc);
      } else {
        String newArgStr = existing.getArgStr();
        if (newArgStr == null) {
          newArgStr = "";
        }
        if (!newArgStr.isBlank()) {
          newArgStr += "; ";
        }
        newArgStr += str.toString();
        existing.setArgStr(newArgStr);
      }
    }
  }
  
  
  
}
