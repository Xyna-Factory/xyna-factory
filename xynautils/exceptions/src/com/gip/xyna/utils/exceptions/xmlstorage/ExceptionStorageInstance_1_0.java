/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.utils.exceptions.xmlstorage;



import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.ExceptionStorage;
import com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidClassNameException;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaClass;
import com.gip.xyna.utils.exceptions.utils.codegen.JavaGenUtils;



public class ExceptionStorageInstance_1_0 extends ExceptionStorageInstance {

  private static final Logger logger = Logger.getLogger(ExceptionStorageInstance_1_0.class.getName());

  private String fqClassName;


  public void setFQClassName(String fqClassName) {
    this.fqClassName = fqClassName;
  }


  @Override
  public JavaClass[] generateJavaClasses(boolean loadFromResource, ExceptionEntryProvider provider, String xmlFile)
                  throws InvalidValuesInXMLException {
    try {
      JavaClass jc = new JavaClass(JavaGenUtils.getPackageNameFromFQName(fqClassName), JavaGenUtils
                      .getSimpleNameFromFQName(fqClassName));
      if (loadFromResource) {
        String exceptionStorage = jc.addImport(ExceptionStorage.class.getName());
        String loggerClass = jc.addImport(Logger.class.getName());
        String enumerationClass = jc.addImport(Enumeration.class.getName());
        String urlcon = jc.addImport(URLConnection.class.getName());
        String url = jc.addImport(URL.class.getName());
        String is = jc.addImport(InputStream.class.getName());

        File f = new File(xmlFile);        
        jc.addMemberVar("public static " + loggerClass + " logger = " + loggerClass + ".getLogger("
                        + jc.getSimpleClassName() + ".class.getName())");
        
                
        CodeBuffer staticInit = new CodeBuffer("Utils");
        staticInit.addLine("try {");
        
        staticInit.addLine(enumerationClass + "<" + url + "> urls = " + jc.getSimpleClassName() + ".class.getClassLoader().getResources(\"" + f.getName() + "\")");
        staticInit.addLine("while (urls.hasMoreElements()) {");
        staticInit.addLine(url + " url = urls.nextElement()");
        staticInit.addLine("if (url != null) {");
        staticInit.addLine("if (logger.isTraceEnabled()) {");
        staticInit.addLine("logger.trace(\"trying to parse url \" + url + \", path= \" + url.getPath())");
        staticInit.addLine("}");
        staticInit.addLine(urlcon + " urlcon = url.openConnection()");
        staticInit.addLine("//deactivate cache to not get an old version");
        staticInit.addLine("boolean b = urlcon.getUseCaches()");
        staticInit.addLine("urlcon.setUseCaches(false)");
        staticInit.addLine("try {");
        staticInit.addLine(is + " is = urlcon.getInputStream()");
        staticInit.addLine("try {");
        staticInit.addLine("if (is == null) {");
        staticInit.addLine("throw new Exception(\"Resource " + f.getName() + " not found.\")");
        staticInit.addLine("}");
        staticInit.addLine(exceptionStorage + ".loadFromStream(is, \"" + f.getName() + "\")");
        staticInit.addLine("} finally {");
        staticInit.addLine("is.close()");
        staticInit.addLine("}");
        staticInit.addLine("} finally {");
        staticInit.addLine("//reset caching!");
        staticInit.addLine("try {");
        staticInit.addLine("urlcon.setUseCaches(b)");
        staticInit.addLine("} catch (Exception e) {");
        staticInit.addLine("if (logger.isTraceEnabled()) {");
        staticInit.addLine("logger.trace(\"could not reset urlConnection.useCaches after parsing resource. this is usually okay.\", e)");
        staticInit.addLine("}");//if
        staticInit.addLine("}");//catch
        staticInit.addLine("}"); //finally
        staticInit.addLine("} else {");
        staticInit.addLine("throw new Exception(\" Resource " + f.getName() + " not found.\")");
        staticInit.addLine("}"); //end if then else
        staticInit.addLine("}"); //end while
        staticInit.addLine("} catch (Exception e) {");
        staticInit.addLine("logger.error(\"Error loading Errormessages.\", e)");
        staticInit.addLine("e.printStackTrace()");
        staticInit.addLine("}");
        jc.addStaticInitBlock(staticInit);
      }

      for (ExceptionEntry entry : getEntries()) {
        if (entry instanceof ExceptionEntry_1_0) {
          ExceptionEntry_1_0 entry10 = (ExceptionEntry_1_0) entry;
          String varName = entry10.getVariableName();
          if (varName == null || varName.length() == 0) {
            varName = buildDefaultVarName(entry10.getCode(), entry10.getMessages().values().iterator().next());
          }
          if (entry.getParameter().size() > 0) {
            CodeBuffer method = new CodeBuffer("Utils");
            method.add("public static final String[] CODE_" + varName + "(");
            for (ExceptionParameter para : entry.getParameter()) {
              String type = null;
              if (para.isReference()) {
                type = jc.addImport(para.getTypePath(), para.getTypeName());
              } else {
                type = para.getJavaType();
              }
              method.addListElement(type + " " + para.getVarName());
            }
            method.add(") {").addLB();
            method.add("return new String[]{\"" + entry.getCode() + "\", ");
            for (ExceptionParameter para : entry.getParameter()) {
              method.addListElement("\"\" + " + para.getVarName());
            }
            method.add("};");
            method.addLB();
            method.addLine("}");
            jc.addMethod(method);
          } else {
            jc.addMemberVar("public static final String CODE_" + varName + " = \"" + entry10.getCode() + "\"");
          }
        } else {
          logger.warn("cannot generate code for exception entry of version other than 1.0");
          //TODO neue nicht supported
        }
      }
      return new JavaClass[] {jc};

    } catch (InvalidClassNameException e) {
      throw new InvalidValuesInXMLException(xmlFile, e.getMessage()).initCause(e);
    }
  }


  private static String buildDefaultVarName(String code, String text) {
    String varName = code + "_" + text.substring(0, Math.min(20, text.length()));
    return JavaGenUtils.transformVarNameForJava(varName);
  }

}
