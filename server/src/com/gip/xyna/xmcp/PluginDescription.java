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
package com.gip.xyna.xmcp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import com.gip.xyna.utils.misc.StringParameter;


/**
 * PluginDescription kapselt verschiedene Informationen zu einem Plugin.
 * Die enthaltenen Informationen dienen zur Anzeige des Plugins in GUI und CLI und zur 
 * Konfiguration der einzelnen Plugin-Methoden.
 * <br>
 * PluginDescription ist immutable und threadsafe.
 * <br>
 * Eine Erstellung einer PluginDescription l�uft �ber einen Builder:
 * <pre>
 * pluginDescription = PluginDescription.create(PluginType.dataModelType).
 *   name(this.dataModelTypeName).
 *   label("Data Model Type "+this.dataModelTypeName).
 *   description("Imports MIBs.").
 *   parameters(PluginDescription.ParameterUsage.Create, ImportParameter.allParameters).
 *   parameters(PluginDescription.ParameterUsage.Configure, ImportParameter.initialParameters).
 *   build();
 * </pre>
 */
public class PluginDescription implements Comparable<PluginDescription>, Serializable{
  
  private static final long serialVersionUID = 1L;
  
  private PluginType type;
  private String name;
  private String label;
  private String description;
  private EnumMap<ParameterUsage, List<StringParameter<?>>> parameters;
  
  //formspezifische daten
  private String[] datatypes;
  private String[] forms;
  
  public static enum PluginType {
    orderInputSource, dataModelType, repositoryAccess, connectionPool, remoteDestinationType, keystoretype;
  }
  
  public static enum ParameterUsage {
    Create, Modify, Configure, Delete, Write, Read,
  }
  
  private PluginDescription() {
    parameters = new EnumMap<ParameterUsage, List<StringParameter<?>>>(ParameterUsage.class);
  }
  
  public PluginDescription(PluginDescription pd) {
    this.type = pd.type;
    this.name = pd.name;
    this.label = pd.label;
    this.description = pd.description;
    this.parameters = new EnumMap<ParameterUsage, List<StringParameter<?>>>(ParameterUsage.class);
    this.datatypes = cloneArray(pd.datatypes);
    this.forms = cloneArray(pd.forms);
    for( ParameterUsage pu : ParameterUsage.values() ) {
      List<StringParameter<?>> sps = pd.parameters.get(pu);
      if( sps != null ) {
        List<StringParameter<?>> copy = new ArrayList<StringParameter<?>>(sps);
        this.parameters.put( pu, Collections.unmodifiableList(copy) );
      }
    }
  } 

  private static String[] cloneArray(String[] arr) {
    if (arr == null) {
      return null;
    }
    String[] result = new String[arr.length];
    for (int i = 0; i<arr.length; i++) {
      result[i] = arr[i];
    }
    return result;
  }

  public int compareTo(PluginDescription description) {
    return name.compareTo(description.name);
  }


  public String getName() {
    return name;
  }


  public String getLabel() {
    return label;
  }


  public String getDescription() {
    return description;
  }


  public String[] getForms() {
    return forms;
  }


  public String[] getDatatypes() {
    return datatypes;
  }


  @Deprecated
  public List<StringParameter<?>> getParameters() {
    return getParameters(ParameterUsage.Create); //TODO raus, wenn XynaBlackEditionWebServices aktualisiert ist
  }


  public List<StringParameter<?>> getParameters(ParameterUsage usage) {
    return parameters.get(usage);
  }


  public boolean hasParameters(ParameterUsage usage) {
    return parameters.get(usage) != null && ! parameters.get(usage).isEmpty();
  }


  public static Builder create(PluginType type) {
    return new Builder(type);
  }


  public static class Builder {
    PluginDescription pd;
    
    private List<String> datatypes = new ArrayList<String>();
    private List<String> forms = new ArrayList<String>();
    
    public Builder(PluginType type) {
      pd = new PluginDescription();
      pd.type = type;
    }

    public Builder name(String name) {
      pd.name = name;
      return this;
    }
    public Builder label(String label) {
      pd.label = label;
      return this;
    }

    public Builder description(String description) {
      pd.description = description;
      return this;
    }

    public Builder parameters(ParameterUsage usage, List<StringParameter<?>> parameter) {
      pd.parameters.put(usage,parameter);
      return this;
    }
    
    public Builder addDatatype(String datatype) {
      datatypes.add(datatype);
      return this;
    }
    
    public Builder addForm(String form) {
      forms.add(form);
      return this;
    }

    public PluginDescription build() {
      if (!datatypes.isEmpty()) {
        pd.datatypes = datatypes.toArray(new String[0]);
      }
      if (!forms.isEmpty()) {
        pd.forms = forms.toArray(new String[0]);
      }
      return new PluginDescription(pd);
    }

  }


}
