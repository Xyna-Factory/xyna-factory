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
package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyUnreadableString;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;

class XynaPropertySupport {

  private List<XynaPropertySupport.XynaPropertyData> xynaPropertyData;
  private String user;
  private HashSet<String> usedInstanceNames;
  //If this is the XynaPropertySupport of a SubScope, the stepName will be the ClassName of the ScopeStep
  //else this is null
  private String stepName = null;


  private enum XPType {
    xp_boolean("XynaPropertyBoolean", 
               PrimitiveType.BOOLEAN_OBJ,
               com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyBoolean.class.getSimpleName()), 
    //
    xp_int("XynaPropertyInt",
           PrimitiveType.INTEGER, 
           com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyInteger.class.getSimpleName()), 
    //
    xp_long("XynaPropertyLong",
            PrimitiveType.LONG_OBJ,
            com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyLong.class.getSimpleName()),
    //
    xp_duration("XynaPropertyDuration",
                PrimitiveType.STRING,
                com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyRelativeDate.class.getSimpleName()) {
                public String toLiteral(String defaultValue) {
                  if (defaultValue == null) {
                    return "(" + com.gip.xyna.utils.timing.Duration.class.getName() +") null";
                  }
                  return com.gip.xyna.utils.timing.Duration.class.getName()+".valueOfSum(\"" + defaultValue+"\")";
                }
    },
    //
    xp_string("XynaPropertyString",
              PrimitiveType.STRING,
              com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyString.class.getSimpleName()),
    //
    xp_customizable("XynaPropertyString",
                    PrimitiveType.STRING, 
                    com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaPropertyCustomizable.class.getSimpleName()),
    //
    xp_unreadable("XynaPropertyUnreadableString", PrimitiveType.STRING, XynaPropertyUnreadableString.class.getSimpleName()),
    //
    xp_other("XynaPropertyString", 
             PrimitiveType.STRING, 
             "other");

    private String propertyClass;
    private PrimitiveType primitiveType;
    private String xmomClass;
    

    private XPType(String propertyClass, PrimitiveType primitiveType, String xmomClass) {
      this.propertyClass = propertyClass;
      this.primitiveType = primitiveType;
      this.xmomClass = xmomClass;
    }


    public static XynaPropertySupport.XPType valueOfClassName(String className) {
      for (XynaPropertySupport.XPType xpt : values()) {
        if (xpt.xmomClass.equals(className)) {
          return xpt;
        }
      }
      return xp_other;
    }


    public String getPropertyClass() {
      return propertyClass;
    }


    public String toLiteral(String defaultValue) {
      return primitiveType.toLiteral(defaultValue);
    }
    
    public com.gip.xyna.xprc.xfractwfe.generation.XynaPropertySupport.XynaPropertyData.BehaviourIfPropertyNotSet getDefaultBehaviour() {
      if (this == XPType.xp_unreadable) {
        return com.gip.xyna.xprc.xfractwfe.generation.XynaPropertySupport.XynaPropertyData.BehaviourIfPropertyNotSet.throwException;
      }
      return com.gip.xyna.xprc.xfractwfe.generation.XynaPropertySupport.XynaPropertyData.BehaviourIfPropertyNotSet.nothing;
    }

  }

  private static class XynaPropertyData {

    private XynaPropertySupport.XPType type;
    private String varName;
    private String name;
    private String instanceName;
    private XynaPropertyData.BehaviourIfPropertyNotSet behaviourIfPropertyNotSet;
    private String defaultValue;
    private EnumMap<DocumentationLanguage, String> documentation;
    private String stepName = null;

    private static final Pattern XYNA_PROP_VARNAME_REPLACE_PATTERN = Pattern.compile("[^a-zA-Z0-9]");


    private enum BehaviourIfPropertyNotSet {
      createProperty, useValue, throwException, nothing;

      public static XynaPropertyData.BehaviourIfPropertyNotSet valueOfClassName(String className) {
        if (com.gip.xyna.xfmg.xods.configuration.xynaobjects.CreateProperty.class.getSimpleName().equals(className)) {
          return createProperty;
        } else if (com.gip.xyna.xfmg.xods.configuration.xynaobjects.UseValue.class.getSimpleName().equals(className)) {
          return useValue;
        } else if (com.gip.xyna.xfmg.xods.configuration.xynaobjects.ThrowException.class.getSimpleName().equals(className)) {
          return throwException;
        } else {
          return nothing;
        }
      }
    }


    public XynaPropertyData(AVariable var, HashSet<String> usedInstanceNames, String stepName) {
      type = XPType.valueOfClassName(var.getClassNameDirectly());
      varName = var.getVarName();
      behaviourIfPropertyNotSet = type.getDefaultBehaviour();
      this.stepName = stepName;
      for (AVariable c : var.getChildren()) {
        if (c.getVarName().equals("propertyName")) {
          name = c.value;
          if (name != null) {
            createInstanceName(usedInstanceNames);
          } else {
            return; //unvollständig, daher nicht weiter probieren
          }
        }
        if (c.getVarName().equals("value")) {
          if( c.isJavaBaseType() ) {
            defaultValue = c.value;
          } else {
            for (AVariable cc : c.getChildren()) { 
              defaultValue = cc.value;
            }
          }
        }
        if (c.getVarName().equals("defaultValueAsString")) {
          defaultValue = c.value;
        }
        if (c.getVarName().equals("behaviourIfPropertyNotSet")) {
          fillBehaviourIfPropertyNotSet(c);
        }
      }
    }
    
    public XynaPropertyData(AVariable var, HashSet<String> usedInstanceNames) {
      this(var, usedInstanceNames, null);
      
    }


    private void createInstanceName(HashSet<String> usedInstanceNames) {
      String varName = XYNA_PROP_VARNAME_REPLACE_PATTERN.matcher(name).replaceAll("_");
      if(stepName == null) {
        instanceName = "XYNAPROPERTY_" + varName;
      } else {
        instanceName = "XYNAPROPERTY_" + stepName + "_" + varName;
      }
      if (usedInstanceNames != null) {
        int i = 0;
        while (usedInstanceNames.contains(instanceName)) {
          if(stepName == null) {
            instanceName = "XYNAPROPERTY" + (i++) + "_" + varName;
          } else {
            instanceName = "XYNAPROPERTY" + (i++) + "_" + stepName + "_" + varName;
          }
        }
      }
    }


    private void fillBehaviourIfPropertyNotSet(AVariable behaviour) {
      behaviourIfPropertyNotSet = BehaviourIfPropertyNotSet.valueOfClassName(behaviour.getClassNameDirectly());
      switch (behaviourIfPropertyNotSet) {
        case createProperty :
          for (AVariable c : behaviour.getChildren()) {
            if (c.getVarName().equals("documentation")) {
              fillDocumentation(c.getChildren());
            }
          }
          break;
        case useValue :
          break;
        case throwException :
          break;
      }
    }


    private void fillDocumentation(List<AVariable> docs) {
      if (docs != null && !docs.isEmpty()) {
        documentation = new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class);
        for (AVariable d : docs) {
          String lang = "EN";
          String docString = null;
          for (AVariable dd : d.getChildren()) {
            if (dd.getVarName().equals("documentationLanguage")) {
              lang = dd.getClassNameDirectly();
            }
            if (dd.getVarName().equals("documentation")) {
              docString = dd.value;
            }
          }
          documentation.put(DocumentationLanguage.valueOf(lang), docString);
        }
      }
    }


    public String createInstantiationString() {
      String defaultValueInput = defaultValue;
      if (behaviourIfPropertyNotSet == BehaviourIfPropertyNotSet.throwException) {
        defaultValueInput = null;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("private static final XynaPropertyUtils.").append(type.getPropertyClass()).append(" ");
      sb.append(instanceName).append(" = new XynaPropertyUtils.").append(type.getPropertyClass());
      sb.append("(\"").append(name).append("\",").append(type.toLiteral(defaultValueInput)).append(")");
      if (documentation != null) {
        for (Map.Entry<DocumentationLanguage, String> entry : documentation.entrySet()) {
          sb.append(".\n           setDefaultDocumentation(");
          sb.append("com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage.").append(entry.getKey());
          sb.append(",\"").append(StringUtils.toLiteral(entry.getValue())).append("\")");
        }
      }
      sb.append(";");
      return sb.toString();
    }


    public String createXynaPropertyAssignmentString() {
      StringBuilder sb = new StringBuilder();
      sb.append(varName).append(".setXynaPropertyImpl(").append(instanceName).append(")");
      if (behaviourIfPropertyNotSet == BehaviourIfPropertyNotSet.throwException) {
        sb.append(".setThrowExceptionIfNoValueSet(true)");
      }
      sb.append(";");
      return sb.toString();
    }
  }


  public XynaPropertySupport(GenerationBase creator) {
    this(creator, null);
  }
  
  public XynaPropertySupport(GenerationBase creator, String stepName) {
    this.user = creator.getFqClassName();
    this.stepName = stepName;
  }

  public void checkForXynaProperties(List<? extends AVariable> vars) {
    if (vars != null) {
      for (AVariable var : vars) {
        checkForXynaProperty(var);
      }
    }
  }


  private void checkForXynaProperty(AVariable var) {
    if (var != null && var.getFQClassName() != null
        && var.getFQClassName().startsWith("com.gip.xyna.xfmg.xods.configuration.xynaobjects.")) {
      if (var.getChildren() != null && !var.getChildren().isEmpty()) {
        XynaPropertySupport.XynaPropertyData xpd;
        if(stepName == null) {
         xpd = new XynaPropertyData(var, usedInstanceNames);
        } else {
          xpd = new XynaPropertyData(var, usedInstanceNames, stepName);
        }
        if (xpd.name != null) {
          if (xynaPropertyData == null) {
            xynaPropertyData = new ArrayList<XynaPropertySupport.XynaPropertyData>();
          }
          if (usedInstanceNames == null) {
            usedInstanceNames = new HashSet<String>();
          }
          xynaPropertyData.add(xpd);
          usedInstanceNames.add(xpd.instanceName);
        }
      }
    }
  }


  public void generateXynaProperties(CodeBuffer cb) {
    if (xynaPropertyData == null) {
      return; //nichts zu tun
    }
    for (XynaPropertySupport.XynaPropertyData xpd : xynaPropertyData) {
      cb.addLine(xpd.createInstantiationString());
    }
    cb.addLB();
  }


  public void generateXynaPropertyAssignment(CodeBuffer cb) {
    if (xynaPropertyData == null) {
      return; //nichts zu tun
    }
    for (XynaPropertySupport.XynaPropertyData xpd : xynaPropertyData) {
      cb.addLine(xpd.createXynaPropertyAssignmentString());
    }
    cb.addLB();
  }

  public void generateJavaReadWriteObject(CodeBuffer cb, boolean read) {
    if( ! read ) {
      return; //nichts zu tun für write
    }
    if (xynaPropertyData == null) {
      return; //nichts zu tun
    }
    for (XynaPropertySupport.XynaPropertyData xpd : xynaPropertyData) {
      cb.addLine(xpd.createXynaPropertyAssignmentString());
    }
  }


  /**
   * verwendet {@link XynaPropertyUtils.UserType.Workflow}
   */
  public void generateJavaOnDeployment(CodeBuffer cb) {
    if (xynaPropertyData == null) {
      return; //nichts zu tun
    }
    for (XynaPropertySupport.XynaPropertyData xpd : xynaPropertyData) {
      cb.addLine(xpd.instanceName, ".registerDependency(XynaPropertyUtils.UserType.Workflow,\"", user, "\")");
    }
  }


  public void generateJavaOnUndeployment(CodeBuffer cb) {
    if (xynaPropertyData == null) {
      return; //nichts zu tun
    }
    for (XynaPropertySupport.XynaPropertyData xpd : xynaPropertyData) {
      cb.addLine(xpd.instanceName, ".unregister()");
    }
  }

}