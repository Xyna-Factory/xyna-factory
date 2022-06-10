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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.TypeGeneration;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;



/**
 *
 */
public class InformationUtils {
 
  public static List<String> xsdTypesToString(TypeGeneration tg) {
    List<String> list = new ArrayList<String>();
    for( TypeInfo ti : tg.getTypeInfos() ) {
      list.add( xsdTypeToString(ti) );
    }
    return list;
  }

  public static String xsdTypeToString(TypeInfo ti) {
    StringBuilder sb = new StringBuilder();
    sb.append( ti ).append("\n");
    for( TypeInfoMember tim : ti.getMembers() ) {
      sb.append("         ").append(tim).append("\n");
    }
    return sb.toString();
  }


  
  public static List<String> xmomTypesToString(TypeGeneration tg) {
    List<String> list = new ArrayList<String>();
    for( TypeInfo ti : tg.getTypeInfos() ) {
      list.add( xmomTypeToString(ti) );
    }
    return list;
  }

  public static String xmomTypeToString(TypeInfo ti ) {
    StringBuilder sb = new StringBuilder();
    sb.append(ti.getXmomType().getFQTypeName());
    if( ti.hasBaseType() ) {
      sb.append(" extends ").append(ti.getBaseType().getXmomType().getFQTypeName());
    }
    sb.append("\n");
    for( TypeInfoMember tim : ti.getMembers() ) {
      String varName = tim.getVarName();
      sb.append("         ").append(varName);
      if( ! varName.equals( tim.getLabel() ) ) {
        sb.append(" '").append( tim.getLabel() ).append("'");
      }
      sb.append(" ");
      if( tim.isList() ) {
        sb.append("List<").append(tim.getVarType()).append(">");
      } else {
        sb.append(tim.getVarType());
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  public static List<String> xmomTypesToTree(TypeGeneration tg) {
    Map<TypeInfo, TreeNode> tns = new HashMap<TypeInfo, TreeNode>();
    for( TypeInfo ti : tg.getTypeInfos() ) {
      tns.put( ti, new TreeNode(ti) );
    }
    TreeNode root = new TreeNode(null);
    for( TypeInfo ti : tg.getTypeInfos() ) {
      //TreeNode aus Map entfernen und in Parent stecken
      TreeNode tn = tns.get(ti);
      TypeInfo p = ti.getBaseType();
      if( p != null ) {
        tns.get(p).addChild(tn);
      } else {
        root.addChild(tn);
      }
    }
    //Nun sind in Map nur Parents übrig
    root.sortRecursively();
    
    List<String> list = new ArrayList<String>();
    root.addToList(list, "");
    
    return list;
  }
  
  private static class TreeNode implements Comparable<TreeNode> {
    private TypeInfo parent;
    private List<TreeNode> children;
    public TreeNode(TypeInfo parent) {
      this.parent = parent;
    }

    public void addToList(List<String> list, String indent) {
      if( parent != null ) {
        list.add( indent + parent.getXmomType().getFQTypeName() );
        indent = indent +"  ";
      }
      if( children != null ) {
        for( TreeNode child : children ) {
          child.addToList(list, indent);
        }
      }
    }

    public void sortRecursively() {
      if( children != null ) {
        Collections.sort(children);
        for( TreeNode child : children ) {
          child.sortRecursively();
        }
      }
    }
    
    public void addChild(TreeNode child) {
      if( children == null ) {
        children = new ArrayList<TreeNode>();
      }
      children.add(child);
    }

    public int compareTo(TreeNode o) {
      return parent.getXmomType().getFQTypeName().compareTo( o.parent.getXmomType().getFQTypeName() );
    }
  }
  


  
}
