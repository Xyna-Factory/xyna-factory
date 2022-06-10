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
package com.gip.xyna.xfmg.xfctrl.classloading;


import java.net.URL;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


/**
 * oberklasse aller classloader, für die getOriginalXmlName/Path sinnvoll ist (zb MDM/Exception)
 */
public abstract class MDMClassLoaderXMLBase extends ClassLoaderBase {

  private static final Logger logger = CentralFactoryLogging.getLogger(MDMClassLoaderXMLBase.class);

  private final String fqClassName;
  private final String originalXmlName;
  private final String originalXmlPath;
  
  protected MDMClassLoaderXMLBase(ClassLoaderType type, String fqClassName, URL[] urls, ClassLoaderBase[] parents,
                                  String originalXmlPath, String originalXmlName, Long revision) {

    super(type, fqClassName, urls, parents, revision);

    this.originalXmlName = originalXmlName;
    this.originalXmlPath = originalXmlPath;
    this.fqClassName = fqClassName;

  }


  protected String getFqClassName() {
    return fqClassName;
  }


  public String getOriginalXmlName() {
    return originalXmlName;
  }


  public String getOriginalXmlPath() {
    return originalXmlPath;
  }


  public String toString() {
    return new StringBuilder(super.toString()).append("-").append(getRevision()).append("-").append(fqClassName).toString();
  }
  
  protected boolean isResponsible(String name) {
    return name.equals(fqClassName) || name.startsWith(fqClassName + "$"); 
  }


}
