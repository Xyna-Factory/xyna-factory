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
package com.gip.xyna.xdev.map.typegen;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.map.typegen.XmomDataCreator.LabelCustomization;
import com.gip.xyna.xdev.map.typegen.exceptions.WSDLParsingException;
import com.gip.xyna.xdev.map.typegen.exceptions.XSDParsingException;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.map.types.TypeInfoMember;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper.IDGenerator;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;


public class TypeInfoGeneratorTest extends TestCase {

  public static List<String> importParameterList = Arrays.asList( "name=junit",
                                                                  "workspaces=workspace_junit",
                                                                  "basePath=basePath",
                                                                  "pathCustomization=#basepath/#nshost/#nspath",
                                                                  "labelCustomization=suffixForAttribute= (attribute)");
  
  public static GenerationParameter generationParameter;
  static {
    generationParameter = fillImportParameter(importParameterList);
  }
    
  public static GenerationParameter fillImportParameter(List<String> list, String ... additional ) {
    ArrayList<String> params = new ArrayList<String>(list);
    for( String a : additional ) {
      params.add(a);
    }
    Map<String, String> map = StringParameter.paramListToMap(params);
    return ImportParameter.parameterForImport(map);
  }
  
  public static class ImportParameter extends TypeGenerationOptions {
   
    public ImportParameter() {
      this.pathCustomization = "/#basepath/#nshost/#nspath";
      this.basePath = "xdnc.model.xsd";
      this.dataModelName = "junit";
    }
    
    public static ImportParameter parameterForImport(Map<String, String> map) {
      ImportParameter ip = new ImportParameter();
      
      for( Map.Entry<String,String> entry : map.entrySet() ) {
        String key = entry.getKey();
        String value = entry.getValue();
        if( "pathCustomization".equals(key) ) {
          ip.pathCustomization = value; 
        }
        if( "basePath".equals(key) ) {
          ip.basePath = value; 
        }
        if( "labelCustomization".equals(key) ) {
          //FIXME
          if( value.contains("suffixForAttribute") ) {
            ip.labelCustomization.put(LabelCustomization.suffixForAttribute, " (attribute)");
          }
          if( value.contains("suffixForOptional") ) {
            ip.labelCustomization.put(LabelCustomization.suffixForOptional, " (optional)");
          }
        }
        if( "generationOptions_expandChoice".equals(key) ) {
          ip.generationOptions_expandChoice = true;
        }
      }
      return ip;
    }

  }
  
  
  static {
    
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    rootLogger.removeAllAppenders();
    rootLogger.addAppender(new ConsoleAppender(
               new PatternLayout("XYNA %-5p [%t] (%C:%M:%L) - [%x] %m%n")));
    
    //XmomGeneratorBuilder.useDefaultRevisionForTests = true;
    
    TypeMappingEntryHelper.setIdGenerator( new IDGeneratorImpl() );
    
  }
  
  public static class IDGeneratorImpl implements IDGenerator {
    AtomicLong idgen = new AtomicLong();
    
    public long getId() {
      return idgen.getAndIncrement();
    }
    
  }

  
  
  public void testFailure1() throws FileNotFoundException, WSDLParsingException {
    List<String> xsds = buildXSD( 
        "<xsd:element name=\"sTest\" type=\"Undefined\" />"       
        );
    try {
      TypeGeneration tg = new TypeGeneration(generationParameter);
      tg.parseXSDs(xsds);
      fail("failure expected");
    } catch( XSDParsingException e ) {
      assertEquals("[[Error] TypeInfoGeneratorTest.xsd:3:46: src-resolve: Cannot resolve the name 'Undefined' to a(n) 'type definition' component.]",
                   e.getErrors().toString());
    }

  }
  
  public void testFailure2() throws FileNotFoundException, WSDLParsingException {
    List<String> xsds = buildXSD(
       "<xsd:complexType name=\"CTest_Type\">",
       "  <xsd:sequence>",
       "    <xsd:element name=\"entry\" type=\"xsd:string\" minoccurs=\"0\" maxoccurs=\"Unbounded\"/>",
       "  </xsd:sequence>",          
       "  </xsd:complexType>",                          
       "<xsd:element name=\"cTest\" type=\"CTest_Type\" />"       
        );
    try {
      TypeGeneration tg = new TypeGeneration(generationParameter);
      tg.parseXSDs(xsds);
      fail("failure expected");
    } catch (XSDParsingException e) {
      assertEquals("[[Error] TypeInfoGeneratorTest.xsd:5:86: s4s-att-not-allowed: Attribute 'minoccurs' cannot appear in element 'element'., "
          +"[Error] TypeInfoGeneratorTest.xsd:5:86: s4s-att-not-allowed: Attribute 'maxoccurs' cannot appear in element 'element'.]"
    , e.getErrors().toString());

    }

  }
  
  public void testXmomPath_OmitHost_UseNsPath() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#nspath");
    
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.path.to.ns.STest\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_ReverseHost() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#nsrevhost/#nspath");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.com.gip.www.path.to.ns.STest\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );
   
  }
  

  public void testXmomPath_UseHost() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#nshost/#nspath");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.path.to.ns.STest\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_OmitHost() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.STest\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_Prefix() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"prefixTestSuffix\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#type_prefix");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,prefixTestSuffix,root)\n"+
                 "         TypeInfoMember(prefixTestSuffix,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.prefix.PrefixTestSuffix\n"+
                 "         prefixTestSuffix String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_Suffix() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"prefixTestSuffix\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#type_suffix");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,prefixTestSuffix,root)\n"+
                 "         TypeInfoMember(prefixTestSuffix,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.suffix.PrefixTestSuffix\n"+
                 "         prefixTestSuffix String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_Reserved() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/path/to/ns",                            
     "<xsd:element name=\"interfaceTestSuffix\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/do/this/#type_prefix");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/path/to/ns,interfaceTestSuffix,root)\n"+
                 "         TypeInfoMember(interfaceTestSuffix,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.do0.this0.interface0.InterfaceTestSuffix\n"+
                 "         interfaceTestSuffix String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testXmomPath_InvalidChar() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSDWithNamespace( 
      "http://www.gip.com/pa_th/$to-#_ns_v01.01",                            
     "<xsd:element name=\"ele\" type=\"xsd:string\" />"       
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "pathCustomization=#basepath/#nsrevhost/#nspath");
    TypeGeneration tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com/pa_th/$to-#_ns_v01.01,ele,root)\n"+
                 "         TypeInfoMember(ele,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.com.gip.www.pa_th._to_ns_v01.01.Ele\n"+
                 "         ele String\n",
        xmomTypesToString(tg)
        );
   
  }
  

  
  
  public void testSimple() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD( 
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Simple,http://www.gip.com,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.STest\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );
   
  }
  
  public void testComplex() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"CTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"cTest\" type=\"CTest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,CTest_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CTest_Type\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testAnonymous() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:element name=\"aTest\">",
     "  <xsd:complexType>",
     "    <xsd:sequence>",
     "      <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "    </xsd:sequence>",          
     "  </xsd:complexType>",
     "</xsd:element>"
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Anonymous,http://www.gip.com,aTest,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ATest\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testAnonymousRef() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:element name=\"aTest\">",
     "  <xsd:complexType>",
     "    <xsd:sequence>",
     "      <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "    </xsd:sequence>",          
     "  </xsd:complexType>",
     "</xsd:element>",
     "<xsd:element name=\"rTest\">",
     "  <xsd:complexType>",
     "    <xsd:sequence>",
     "      <xsd:element ref=\"aTest\"/>",
     "    </xsd:sequence>",          
     "  </xsd:complexType>",
     "</xsd:element>"
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Anonymous,http://www.gip.com,aTest,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,rTest,root)\n"+
                 "         TypeInfoMember(aTest,Element,type=TypeInfo(Anonymous,http://www.gip.com,aTest,root))\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ATest\n"+
                 "         entry String\n"+
                 "basePath.www.gip.com.RTest\n"+
                 "         aTest basePath.www.gip.com.ATest\n",
        xmomTypesToString(tg)
        );
  }
  
  public void testRecursion() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:element name=\"recTest\">",
     "  <xsd:complexType>",
     "    <xsd:sequence>",
     "      <xsd:element ref=\"recTest\" minOccurs=\"0\"/>",
     "    </xsd:sequence>",          
     "  </xsd:complexType>",
     "</xsd:element>"
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Anonymous,http://www.gip.com,recTest,root)\n"+
                 "         TypeInfoMember(recTest,optional Element,type=TypeInfo(Anonymous,http://www.gip.com,recTest,root))\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.RecTest\n"+
                 "         recTest basePath.www.gip.com.RecTest\n",
        xmomTypesToString(tg)
        );
  }

  public void testDuplicateAnonymous() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:element name=\"aTest\">",
     "  <xsd:complexType>",
     "    <xsd:sequence>",
     "      <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "    </xsd:sequence>",          
     "  </xsd:complexType>",
     "</xsd:element>",
     "<xsd:complexType name=\"ATest\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"aTest2\" type=\"ATest\" />"       
     
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Anonymous,http://www.gip.com,aTest,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,ATest,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ATest1\n"+
                 "         entry String\n"+
                 "basePath.www.gip.com.ATest\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }

  public void testDuplicateSimple() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:element name=\"sTest\" type=\"xsd:string\" />",       
     "<xsd:complexType name=\"STest\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"sTest2\" type=\"STest\" />"       
     
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,STest,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Simple,http://www.gip.com,sTest,root)\n"+
                 "         TypeInfoMember(sTest,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.STest\n"+
                 "         entry String\n"+
                 "basePath.www.gip.com.STest1\n"+
                 "         sTest String\n",
        xmomTypesToString(tg)
        );

  }

  public void testDuplicateVar() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
      "<xsd:complexType name=\"DTest_Type\">",
      "  <xsd:sequence>",
      "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
      "    <xsd:element name=\"entry\" type=\"xsd:integer\"/>",
      "  </xsd:sequence>",          
      "  </xsd:complexType>",                          
      "<xsd:element name=\"dTest\" type=\"DTest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,DTest_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "         TypeInfoMember(entry,Element,type=java_Integer)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.DTest_Type\n"+
                 "         entry String\n"+
                 "         entry1 'entry' Integer\n",
        xmomTypesToString(tg)
        );

  }

  public void testMoreDuplicates() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
       "<xsd:element name=\"Basic\" type=\"Basic_Typ\" />",
       "<xsd:complexType name=\"Basic_Typ\">",
       "  <xsd:all>",
       "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
       "     <xsd:element name=\"entry\">",
       "       <xsd:complexType>",
       "         <xsd:all>",
       "           <xsd:element name=\"innen\" type=\"xsd:string\"/>",
       "        </xsd:all>",
       "       </xsd:complexType>",
       "     </xsd:element>",
       "     <xsd:element name=\"entry\" type=\"Entry0\" />",
       "     <xsd:element name=\"entry\">",
       "       <xsd:complexType>",
       "         <xsd:all>",
       "           <xsd:element name=\"innen\" type=\"xsd:string\"/>",
       "        </xsd:all>",
       "       </xsd:complexType>",
       "     </xsd:element>",
       "     <xsd:element name=\"entry\">",
       "       <xsd:complexType>",
       "         <xsd:all>",
       "           <xsd:element name=\"innen\" type=\"xsd:integer\"/>",
       "        </xsd:all>",
       "       </xsd:complexType>",
       "     </xsd:element>",
       "   </xsd:all>",
       "</xsd:complexType>",
       "<xsd:complexType name=\"Entry0\">",
       "  <xsd:all>",
       "    <xsd:element name=\"innen\" type=\"xsd:string\"/>",
       "  </xsd:all>",
       "</xsd:complexType>",
       "<xsd:element name=\"Entry\" type=\"Entry0\" />"
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,Basic_Typ,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "         TypeInfoMember(entry,Element,type=TypeInfo(Anonymous,http://www.gip.com,entry))\n"+
                 "         TypeInfoMember(entry,Element,type=TypeInfo(Complex,http://www.gip.com,Entry0,root))\n"+
                 "         TypeInfoMember(entry,Element,type=TypeInfo(Anonymous,http://www.gip.com,entry))\n"+
                 "         TypeInfoMember(entry,Element,type=TypeInfo(Anonymous,http://www.gip.com,entry))\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,entry)\n"+
                 "         TypeInfoMember(innen,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Entry0,root)\n"+
                 "         TypeInfoMember(innen,Element,type=java_String)\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,entry)\n"+
                 "         TypeInfoMember(innen,Element,type=java_Integer)\n",

                     
        xsdTypesToString(tg)
        );
    
    assertEquals("basePath.www.gip.com.Basic_Typ\n"+
                 "         entry String\n"+
                 "         entry1 'entry' basePath.www.gip.com.Entry\n"+
                 "         entry2 'entry' basePath.www.gip.com.Entry0\n"+
                 "         entry3 'entry' basePath.www.gip.com.Entry\n"+
                 "         entry4 'entry' basePath.www.gip.com.Entry1\n"+
                 "basePath.www.gip.com.Entry\n"+
                 "         innen String\n"+
                 "basePath.www.gip.com.Entry0\n"+
                 "         innen String\n"+
                 "basePath.www.gip.com.Entry1\n"+
                 "         innen Integer\n",
       xmomTypesToString(tg)
        );

  }

  public void testMinMaxOccurs() throws FileNotFoundException, XSDParsingException, WSDLParsingException { //FIXME
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"MMOTest_Type\">",
     "  <xsd:sequence maxOccurs=\"5\">",
     "    <xsd:element name=\"em\" type=\"xsd:string\"/>",
     "    <xsd:element name=\"eo\" type=\"xsd:string\" minOccurs=\"0\"/>",
     "    <xsd:element name=\"lm\" type=\"xsd:string\" maxOccurs=\"unbounded\"/>",
     "    <xsd:element name=\"lm4\" type=\"xsd:string\" maxOccurs=\"4\"/>",
     "    <xsd:element name=\"lo\" type=\"xsd:string\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",                          
     "<xsd:element name=\"mmoTest\" type=\"MMOTest_Type\" />"       
        );
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,MMOTest_Type,root)\n"+
                 "         TypeInfoMember(em,Element,type=java_String)\n"+
                 "         TypeInfoMember(eo,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(lm,List,type=java_String)\n"+
                 "         TypeInfoMember(lm4,List,type=java_String)\n"+
                 "         TypeInfoMember(lo,optional List,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.MMOTest_Type\n"+
                 "         em String\n"+
                 "         eo String\n"+
                 "         lm List<String>\n"+
                 "         lm4 List<String>\n"+
                 "         lo List<String>\n",
        xmomTypesToString(tg)
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "labelCustomization=suffixForOptional= (optional)");
    tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
    
    assertEquals("TypeInfo(Complex,http://www.gip.com,MMOTest_Type,root)\n"+
                 "         TypeInfoMember(em,Element,type=java_String)\n"+
                 "         TypeInfoMember(eo,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(lm,List,type=java_String)\n"+
                 "         TypeInfoMember(lm4,List,type=java_String)\n"+
                 "         TypeInfoMember(lo,optional List,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.MMOTest_Type\n"+
                 "         em String\n"+
                 "         eo 'eo (optional)' String\n"+
                 "         lm List<String>\n"+
                 "         lm4 List<String>\n"+
                 "         lo 'lo (optional)' List<String>\n",
        xmomTypesToString(tg)
        );
  
  }
  
  
  public void testSequenceParticle() throws FileNotFoundException, XSDParsingException, WSDLParsingException { //FIXME
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"SPTest_Type\">",
     "  <xsd:sequence maxOccurs=\"5\">",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"spTest\" type=\"SPTest_Type\" />"       
        );
    //FIXME WARN (TypeInfoGenerator.java:239) - ("http://www.gip.com":entry) has maxOccurs 5
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,SPTest_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.SPTest_Type\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }

  public void testAttribute() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"CATest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  <xsd:attribute name=\"attrO\" type=\"xsd:string\" />",
     "  <xsd:attribute name=\"attrM\" type=\"xsd:string\" use=\"required\" />",
     "</xsd:complexType>",                          
     "<xsd:element name=\"caTest\" type=\"CATest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,CATest_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "         TypeInfoMember(attrO,Attribute,type=java_String)\n"+
                 "         TypeInfoMember(attrM,required Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CATest_Type\n"+
                 "         entry String\n"+
                 "         attrO 'attrO (attribute)' String\n"+
                 "         attrM 'attrM (attribute)' String\n",
        xmomTypesToString(tg)
        );

  }

  public void testAttributeDuplicate() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"CATest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\" maxOccurs=\"1\" minOccurs=\"0\"/>",
     "  </xsd:sequence>",          
     "  <xsd:attribute name=\"entry\" type=\"xsd:string\" />",
     "</xsd:complexType>",                          
     "<xsd:element name=\"caTest\" type=\"CATest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,CATest_Type,root)\n"+
                 "         TypeInfoMember(entry,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(entry,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CATest_Type\n"+
                 "         entry String\n"+
                 "         entry1 'entry (attribute)' String\n",
        xmomTypesToString(tg)
        );

  }


  public void testExtension() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"ETest_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"extEntry\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     
     "<xsd:element name=\"eTest\" type=\"ETest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,ETest_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(extEntry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ETest_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         extEntry String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }
  public void testTwoExtensions() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"E1Test_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"ext1Entry\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:complexType name=\"E2Test_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"ext2Entry\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:element name=\"e1Test\" type=\"E1Test_Type\" />",    
     "<xsd:element name=\"e2Test\" type=\"E2Test_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,E2Test_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(ext2Entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,E1Test_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(ext1Entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.E2Test_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         ext2Entry String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry String\n"+
                 "basePath.www.gip.com.E1Test_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         ext1Entry String\n",
        xmomTypesToString(tg)
        );

  }

  public void testExtensionDuplicate() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"ETest_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     
     "<xsd:element name=\"eTest\" type=\"ETest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,ETest_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ETest_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         entry1 'entry' String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testExtensionDuplicateAttribute() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"ETest_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:attribute name=\"entry\" type=\"xsd:string\"/>",
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     
     "<xsd:element name=\"eTest\" type=\"ETest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,ETest_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(entry,Attribute,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ETest_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         entry1 'entry (attribute)' String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testExtensionDuplicateAttributeReverse() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry2\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "  <xsd:attribute name=\"entry\" type=\"xsd:string\" />",
     "</xsd:complexType>",
     "<xsd:complexType name=\"ETest_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     
     "<xsd:element name=\"eTest\" type=\"ETest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,ETest_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry2,Element,type=java_String)\n" +
                 "         TypeInfoMember(entry,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ETest_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         entry1 'entry' String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry2 String\n" +
                 "         entry 'entry (attribute)' String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testExtensionWithAttribute() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"BTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",
     "  <xsd:attribute name=\"attrBase\" type=\"xsd:string\"/>",
     "</xsd:complexType>",
     "<xsd:complexType name=\"ETest_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"BTest_Type\">",
     "      <xsd:attribute name=\"attrExt\" type=\"xsd:string\"/>",
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     
     "<xsd:element name=\"eTest\" type=\"ETest_Type\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,ETest_Type,base=TypeInfo(Complex,http://www.gip.com,BTest_Type),root)\n"+
                 "         TypeInfoMember(attrExt,Attribute,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,BTest_Type)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "         TypeInfoMember(attrBase,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.ETest_Type extends basePath.www.gip.com.BTest_Type\n"+
                 "         attrExt 'attrExt (attribute)' String\n"+
                 "basePath.www.gip.com.BTest_Type\n"+
                 "         entry String\n"+
                 "         attrBase 'attrBase (attribute)' String\n",
        xmomTypesToString(tg)
        );

  }

  public void testReservedWords() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"RTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"interface\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"Class\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"RTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"instance\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:element name=\"rTest\" type=\"RTest_Type\" />",       
     "<xsd:element name=\"cTest\" type=\"Class\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,RTest_Type,root)\n"+
                 "         TypeInfoMember(interface,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Class,base=TypeInfo(Complex,http://www.gip.com,RTest_Type,root),root)\n"+
                 "         TypeInfoMember(instance,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.RTest_Type\n"+
                 "         interface0 'interface' String\n"+
                 "basePath.www.gip.com.Class0 extends basePath.www.gip.com.RTest_Type\n"+
                 "         instance0 'instance' String\n",
        xmomTypesToString(tg)
        );

  }

  public void testSpecialNames() throws FileNotFoundException, WSDLParsingException  {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"SNTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"punkt._strich-var3\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"Strich-Punkt.Class3\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"SNTest_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"instance\" type=\"xsd:string\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:element name=\" snTest\" type=\"SNTest_Type\" />",       
     "<xsd:element name=\"cTest\" type=\"Strich-Punkt.Class3\" />"       
        );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    try {
    tg.parseXSDs(xsds);
    } catch( XSDParsingException e ) {
      System.err.println( e.getErrors() );
    }
    
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,Strich-Punkt.Class3,base=TypeInfo(Complex,http://www.gip.com,SNTest_Type,root),root)\n"+
                 "         TypeInfoMember(instance,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,SNTest_Type,root)\n"+
                 "         TypeInfoMember(punkt._strich-var3,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.Strich_Punkt_Class3 extends basePath.www.gip.com.SNTest_Type\n"+
                 "         instance0 'instance' String\n"+
                 "basePath.www.gip.com.SNTest_Type\n"+
                 "         punkt_strich_var3 'punkt._strich-var3' String\n",
        xmomTypesToString(tg)
        );

  }

  public void testAnyType() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"AnyTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"value\" type=\"xsd:string\"/>",
     "    <xsd:element ref=\"any\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:element name=\"any\" type=\"xsd:anyType\" />",       
     "<xsd:element name=\"anyTest\" type=\"AnyTest_Type\" />"       
        );
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,AnyTest_Type,root)\n"+
                 "         TypeInfoMember(value,Element,type=java_String)\n"+
                 "         TypeInfoMember(any,Element,type=TypeInfo(Simple,http://www.gip.com,any,root))\n"+
                 "TypeInfo(Simple,http://www.gip.com,any,root)\n"+
                 "         TypeInfoMember(any,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.AnyTest_Type\n"+
                 "         value String\n"+
                 "         any basePath.www.gip.com.Any\n"+
                 "basePath.www.gip.com.Any\n"+
                 "         any String\n",

        xmomTypesToString(tg)
        );
  }

  public void testSimpleContentExtension() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSD(
      "<xsd:complexType name=\"Text\">",
      "  <xsd:simpleContent>",
      "    <xsd:extension base=\"xsd:string\">",
      "      <xsd:attribute name=\"attr\" type=\"xsd:string\"/>",
      "    </xsd:extension>",
      "  </xsd:simpleContent>",
      "</xsd:complexType>",
      "<xsd:element name=\"text\" type=\"Text\" />"
        );
    TypeGeneration tg = new TypeGeneration(generationParameter);
    try {
    tg.parseXSDs(xsds);
    tg.generateTypes();
    } catch (Exception e) {
      e.printStackTrace();
    }
    assertEquals("TypeInfo(Complex,http://www.gip.com,Text,root)\n"+
                 "         TypeInfoMember(Text,Text,type=java_String)\n"+
                 "         TypeInfoMember(attr,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.Text\n"+
                 "         text 'Text' String\n"+
                 "         attr 'attr (attribute)' String\n",

        xmomTypesToString(tg)
        );
  }
  
  public void testImport() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSDs( new XSDGenerator("TypeInfoGeneratorTest0.xsd").
                                   namespace( null, "http://www.gip.com/1").
                                   rows("<xsd:complexType name=\"CTest_Type\">",
                                        "  <xsd:sequence>",
                                        "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                        "  </xsd:sequence>",          
                                        "</xsd:complexType>"),
                                   new XSDGenerator("TypeInfoGeneratorTest1.xsd").
                                   namespace(null, "http://www.gip.com/2").
                                   namespace("ns1", "http://www.gip.com/1").
                                   rows("<xsd:import namespace=\"http://www.gip.com/1\" schemaLocation=\"./TypeInfoGeneratorTest0.xsd\"/>",
                                        "<xsd:element name=\"cTest\" type=\"ns1:CTest_Type\" />")
                                  );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
                            
    assertEquals("TypeInfo(Complex,http://www.gip.com/1,CTest_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
                 xsdTypesToString(tg)
    );
    assertEquals("basePath.www.gip.com.1.CTest_Type\n"+
                 "         entry String\n",
                 xmomTypesToString(tg)
    );
  }

  
  public void testGlobalLocal_Qualified() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_Q.xsd").
                                  elementFormDefault("qualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
     "<xsd:element name=\"global\" type=\"xsd:string\"/>",                            
     "<xsd:complexType name=\"GLTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"local\" type=\"xsd:string\" minOccurs=\"0\"/>",
     "    <xsd:element ref=\"global\" minOccurs=\"0\"/>",
     "    <xsd:element name=\"anonymous\" minOccurs=\"0\">",
     "      <xsd:complexType>",
     "        <xsd:sequence>",
     "          <xsd:element name=\"aEntry\" type=\"xsd:string\"/>",
     "        </xsd:sequence>",          
     "      </xsd:complexType>",
     "    </xsd:element>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"glTest\" type=\"GLTest_Type\" />"       
        ) );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,GLTest_Type,root)\n"+
                 "         TypeInfoMember(local,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(global,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(anonymous,optional Element,type=TypeInfo(Anonymous,http://www.gip.com,anonymous))\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,anonymous)\n"+
                 "         TypeInfoMember(aEntry,Element,type=java_String)\n"+
                 "TypeInfo(Simple,http://www.gip.com,global,root)\n"+
                 "         TypeInfoMember(global,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.GLTest_Type\n"+
                 "         local String\n"+
                 "         global String\n"+
                 "         anonymous basePath.www.gip.com.Anonymous\n"+
                 "basePath.www.gip.com.Anonymous\n"+
                 "         aEntry String\n"+
                 "basePath.www.gip.com.Global\n"+
                 "         global String\n",
        xmomTypesToString(tg)
        );

  }
  
  public void testGlobalLocal_Unqualified() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_U.xsd").
                                  elementFormDefault("unqualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
     "<xsd:element name=\"global\" type=\"xsd:string\"/>",                            
     "<xsd:complexType name=\"GLTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"local\" type=\"xsd:string\" minOccurs=\"0\"/>",
     "    <xsd:element ref=\"global\" minOccurs=\"0\"/>",
     "    <xsd:element name=\"anonymous\" minOccurs=\"0\">",
     "      <xsd:complexType>",
     "        <xsd:sequence>",
     "          <xsd:element name=\"aEntry\" type=\"xsd:string\"/>",
     "        </xsd:sequence>",          
     "      </xsd:complexType>",
     "    </xsd:element>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"glTest\" type=\"GLTest_Type\" />"       
        ) );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,GLTest_Type,root)\n"+
                 "         TypeInfoMember(local,optional unqualified Element,type=java_String)\n"+
                 "         TypeInfoMember(global,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(anonymous,optional unqualified Element,type=TypeInfo(Anonymous,http://www.gip.com,anonymous))\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,anonymous)\n"+
                 "         TypeInfoMember(aEntry,unqualified Element,type=java_String)\n"+
                 "TypeInfo(Simple,http://www.gip.com,global,root)\n"+
                 "         TypeInfoMember(global,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.GLTest_Type\n"+
                 "         local String\n"+
                 "         global String\n"+
                 "         anonymous basePath.www.gip.com.Anonymous\n"+
                 "basePath.www.gip.com.Anonymous\n"+
                 "         aEntry String\n"+
                 "basePath.www.gip.com.Global\n"+
                 "         global String\n",
        xmomTypesToString(tg)
        );

  }

  public void testGlobalLocal_Form() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_F.xsd").
                                  elementFormDefault("qualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
     "<xsd:element name=\"global\" type=\"xsd:string\"/>",                            
     "<xsd:complexType name=\"GLTest_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"local\" type=\"xsd:string\" minOccurs=\"0\" form=\"unqualified\"/>",
     "    <xsd:element ref=\"global\" minOccurs=\"0\"/>",
     "    <xsd:element name=\"anonymous\" minOccurs=\"0\" form=\"unqualified\">",
     "      <xsd:complexType>",
     "        <xsd:sequence>",
     "          <xsd:element name=\"aEntry\" type=\"xsd:string\"/>",
     "        </xsd:sequence>",          
     "      </xsd:complexType>",
     "    </xsd:element>",
     "  </xsd:sequence>",          
     "  </xsd:complexType>",                          
     "<xsd:element name=\"glTest\" type=\"GLTest_Type\" />"       
        ) );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
   
    assertEquals("TypeInfo(Complex,http://www.gip.com,GLTest_Type,root)\n"+
                 "         TypeInfoMember(local,optional unqualified Element,type=java_String)\n"+
                 "         TypeInfoMember(global,optional Element,type=java_String)\n"+
                 "         TypeInfoMember(anonymous,optional unqualified Element,type=TypeInfo(Anonymous,http://www.gip.com,anonymous))\n"+
                 "TypeInfo(Anonymous,http://www.gip.com,anonymous)\n"+
                 "         TypeInfoMember(aEntry,Element,type=java_String)\n"+
                 "TypeInfo(Simple,http://www.gip.com,global,root)\n"+
                 "         TypeInfoMember(global,Element,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.GLTest_Type\n"+
                 "         local String\n"+
                 "         global String\n"+
                 "         anonymous basePath.www.gip.com.Anonymous\n"+
                 "basePath.www.gip.com.Anonymous\n"+
                 "         aEntry String\n"+
                 "basePath.www.gip.com.Global\n"+
                 "         global String\n",
        xmomTypesToString(tg)
        );

  }

  
  
  
  public void testAttribute_Qualified() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_AQ.xsd").
                                  attributeFormDefault("qualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
                                       "<xsd:complexType name=\"CATest_Type\">",
                                       "  <xsd:sequence>",
                                       "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                       "  </xsd:sequence>",          
                                       "  <xsd:attribute name=\"attr1\" type=\"xsd:string\" />",
                                       "  <xsd:attribute name=\"attr2\" type=\"xsd:string\" />",
                                       "</xsd:complexType>",                          
                                       "<xsd:element name=\"caTest\" type=\"CATest_Type\" />"       
                                      ) );

    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();

    assertEquals("TypeInfo(Complex,http://www.gip.com,CATest_Type,root)\n"+
        "         TypeInfoMember(entry,Element,type=java_String)\n"+
        "         TypeInfoMember(attr1,qualified Attribute,type=java_String)\n"+
        "         TypeInfoMember(attr2,qualified Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CATest_Type\n"+
        "         entry String\n"+
        "         attr1 'attr1 (attribute)' String\n"+
        "         attr2 'attr2 (attribute)' String\n",
        xmomTypesToString(tg)
        );
  }

  
  public void testAttribute_Unqualified() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_AU.xsd").
                                  attributeFormDefault("unqualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
                                       "<xsd:complexType name=\"CATest_Type\">",
                                       "  <xsd:sequence>",
                                       "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                       "  </xsd:sequence>",          
                                       "  <xsd:attribute name=\"attr1\" type=\"xsd:string\" />",
                                       "  <xsd:attribute name=\"attr2\" type=\"xsd:string\" />",
                                       "</xsd:complexType>",                          
                                       "<xsd:element name=\"caTest\" type=\"CATest_Type\" />"       
                                      ) );

    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();

    assertEquals("TypeInfo(Complex,http://www.gip.com,CATest_Type,root)\n"+
        "         TypeInfoMember(entry,Element,type=java_String)\n"+
        "         TypeInfoMember(attr1,Attribute,type=java_String)\n"+
        "         TypeInfoMember(attr2,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CATest_Type\n"+
        "         entry String\n"+
        "         attr1 'attr1 (attribute)' String\n"+
        "         attr2 'attr2 (attribute)' String\n",
        xmomTypesToString(tg)
        );
  }

  public void testAttribute_Form() throws FileNotFoundException, XSDParsingException, WSDLParsingException, XFMG_NoSuchRevision {
    List<String> xsds = buildXSDs(new XSDGenerator("TypeInfoGeneratorTest_AF.xsd").
                                  attributeFormDefault("unqualified").
                                  namespace( null, "http://www.gip.com").
                                  rows(
                                       "<xsd:complexType name=\"CATest_Type\">",
                                       "  <xsd:sequence>",
                                       "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                       "  </xsd:sequence>",          
                                       "  <xsd:attribute name=\"attr1\" type=\"xsd:string\" form=\"qualified\"/>",
                                       "  <xsd:attribute name=\"attr2\" type=\"xsd:string\" form=\"unqualified\"/>",
                                       "</xsd:complexType>",                          
                                       "<xsd:element name=\"caTest\" type=\"CATest_Type\" />"       
                                      ) );

    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();

    assertEquals("TypeInfo(Complex,http://www.gip.com,CATest_Type,root)\n"+
        "         TypeInfoMember(entry,Element,type=java_String)\n"+
        "         TypeInfoMember(attr1,qualified Attribute,type=java_String)\n"+
        "         TypeInfoMember(attr2,Attribute,type=java_String)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.CATest_Type\n"+
        "         entry String\n"+
        "         attr1 'attr1 (attribute)' String\n"+
        "         attr2 'attr2 (attribute)' String\n",
        xmomTypesToString(tg)
        );
  }


  public void testChoiceWithCommonBase() throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException {
    List<String> xsds = buildXSD(
     "<xsd:complexType name=\"Base_Type\">",
     "  <xsd:sequence>",
     "    <xsd:element name=\"value\" type=\"xsd:string\"/>",
     "  </xsd:sequence>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"Choice_Type\">",
     "  <xsd:choice>",
     "    <xsd:element name=\"derived1\" type=\"Derived1_Type\"/>",
     "    <xsd:element name=\"derived2\" type=\"Derived2_Type\"/>",
     "  </xsd:choice>",          
     "</xsd:complexType>",
     "<xsd:complexType name=\"Derived1_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"Base_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"d1val\" type=\"xsd:integer\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:complexType name=\"Derived2_Type\">",
     "  <xsd:complexContent>",
     "    <xsd:extension base=\"Base_Type\">",
     "      <xsd:sequence>",
     "        <xsd:element name=\"d2val\" type=\"xsd:boolean\"/>",
     "      </xsd:sequence>",          
     "    </xsd:extension>",
     "  </xsd:complexContent>",
     "</xsd:complexType>",
     "<xsd:element name=\"choice\" type=\"Choice_Type\" />"      
        );
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
    assertEquals("TypeInfo(Complex,http://www.gip.com,Choice_Type,root)\n"+
                 "         TypeInfoMember(Base_Type,Choice,type=TypeInfo(Complex,http://www.gip.com,Base_Type),"+
                 "choice(derived1->Derived1_Type,derived2->Derived2_Type))\n"+
                 "TypeInfo(Complex,http://www.gip.com,Derived1_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type))\n"+
                 "         TypeInfoMember(d1val,Element,type=java_Integer)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Base_Type)\n"+
                 "         TypeInfoMember(value,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Derived2_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type))\n"+
                 "         TypeInfoMember(d2val,Element,type=java_Boolean)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.Choice_Type\n"+
                 "         base_Type 'Base_Type' basePath.www.gip.com.Base_Type\n"+
                 "basePath.www.gip.com.Derived1_Type extends basePath.www.gip.com.Base_Type\n"+
                 "         d1val Integer\n"+
                 "basePath.www.gip.com.Base_Type\n"+
                 "         value String\n"+
                 "basePath.www.gip.com.Derived2_Type extends basePath.www.gip.com.Base_Type\n"+
                  "         d2val Boolean\n",
        xmomTypesToString(tg)
        );
    
    GenerationParameter impParams = fillImportParameter(importParameterList, "generationOptions_expandChoice=true");
    tg = new TypeGeneration(impParams);
    tg.parseXSDs(xsds);
    tg.generateTypes();
    assertEquals("TypeInfo(Complex,http://www.gip.com,Choice_Type,root)\n"+
                 "         TypeInfoMember(derived1,Element,type=TypeInfo(Complex,http://www.gip.com,Derived1_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type)))\n"+
                 "         TypeInfoMember(derived2,Element,type=TypeInfo(Complex,http://www.gip.com,Derived2_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type)))\n"+
                 "TypeInfo(Complex,http://www.gip.com,Derived1_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type))\n"+
                 "         TypeInfoMember(d1val,Element,type=java_Integer)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Base_Type)\n"+
                 "         TypeInfoMember(value,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,Derived2_Type,base=TypeInfo(Complex,http://www.gip.com,Base_Type))\n"+
                 "         TypeInfoMember(d2val,Element,type=java_Boolean)\n",
        xsdTypesToString(tg)
        );
    assertEquals("basePath.www.gip.com.Choice_Type\n"+
                 "         derived1 basePath.www.gip.com.Derived1_Type\n"+
                 "         derived2 basePath.www.gip.com.Derived2_Type\n"+
                 "basePath.www.gip.com.Derived1_Type extends basePath.www.gip.com.Base_Type\n"+
                 "         d1val Integer\n"+
                 "basePath.www.gip.com.Base_Type\n"+
                 "         value String\n"+
                 "basePath.www.gip.com.Derived2_Type extends basePath.www.gip.com.Base_Type\n"+
                  "         d2val Boolean\n",
        xmomTypesToString(tg)
        );
  }

  public void testCommonTargetNamespaceTwoXsds() throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    List<String> xsds = buildXSDs( new XSDGenerator("TypeInfoGeneratorTest0.xsd").
                                   namespace( null, "http://www.gip.com").
                                   rows("<xsd:complexType name=\"CTest1_Type\">",
                                        "  <xsd:sequence>",
                                        "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                        "  </xsd:sequence>",          
                                        "</xsd:complexType>"),
                                   new XSDGenerator("TypeInfoGeneratorTest1.xsd").
                                   namespace( null, "http://www.gip.com").
                                   rows("<xsd:complexType name=\"CTest2_Type\">",
                                        "  <xsd:sequence>",
                                        "    <xsd:element name=\"entry\" type=\"xsd:string\"/>",
                                        "  </xsd:sequence>",          
                                        "</xsd:complexType>"),
                                   new XSDGenerator("TypeInfoGeneratorTest2.xsd").
                                   namespace(null, "http://www.gip.com/inc").
                                   namespace("ns", "http://www.gip.com").
                                   //namespace("ns2", "http://www.gip.com").
                                   rows("<xsd:import namespace=\"http://www.gip.com\" schemaLocation=\"./TypeInfoGeneratorTest0.xsd\"/>",
                                        "<xsd:import namespace=\"http://www.gip.com\" schemaLocation=\"./TypeInfoGeneratorTest1.xsd\"/>",
                                        "<xsd:element name=\"c1Test\" type=\"ns:CTest1_Type\" />",
                                        "<xsd:element name=\"c2Test\" type=\"ns:CTest2_Type\" />"
                                        )
                                  );
    
    TypeGeneration tg = new TypeGeneration(generationParameter);
    tg.parseXSDs(xsds);
    tg.generateTypes();
                            
    assertEquals("TypeInfo(Complex,http://www.gip.com,CTest2_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n"+
                 "TypeInfo(Complex,http://www.gip.com,CTest1_Type,root)\n"+
                 "         TypeInfoMember(entry,Element,type=java_String)\n",
                     xsdTypesToString(tg)
    );
    assertEquals("basePath.www.gip.com.CTest2_Type\n"+
                 "         entry String\n"+
                 "basePath.www.gip.com.CTest1_Type\n"+
                 "         entry String\n",
                 xmomTypesToString(tg)
    );
  }















  /**
   * TODO kann aufgrund der Beschränkungen von TypeMappingEntryHelper keine vergleichbaren Daten liefern
   * @param typeInfos
   */
  @SuppressWarnings("unused")
  private void assertExportImport(List<TypeInfo> typeInfos) {
    TypeMappingEntryHelper tmeh = new TypeMappingEntryHelper("junit");
    
    List<TypeMappingEntry> typeMappings = new ArrayList<TypeMappingEntry>();
    for( TypeInfo typeInfo : typeInfos ) {
      typeMappings.addAll( tmeh.toTypeMappingEntries(typeInfo) );
    }
    
    for( TypeMappingEntry tme : typeMappings ) {
      System.out.println(  tme.getKeyv() + "     "+tme.getValue() );
    }
    
    
    List<TypeInfo> imported = tmeh.importTypeMappingEntries(typeMappings);
    
    Collections.sort(typeInfos, new ToStringComparator<TypeInfo>() );
    Collections.sort(imported, new ToStringComparator<TypeInfo>() );

    System.out.println( typeInfos );
    System.out.println( imported );
    
  }
  
  
  private static class ToStringComparator<T> implements Comparator<T> {
    public int compare(T o1, T o2) {
      return String.valueOf(o1).compareTo(String.valueOf(o2));
    }
  }

  public static String xsdTypesToString(TypeGeneration tg) {
    StringBuilder sb = new StringBuilder();
    List<TypeInfo> tis = tg.getTypeInfos();
    for( TypeInfo ti : tis ) {
      sb.append( ti ).append("\n");
      for( TypeInfoMember tim : ti.getMembers() ) {
        sb.append("         ").append(tim).append("\n");
      }
    }
    return sb.toString();
  }

  public static String xmomTypesToString(TypeGeneration tg) {
    StringBuilder sb = new StringBuilder();
    List<TypeInfo> tis = tg.getTypeInfos();
    for( TypeInfo ti : tis ) {
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
    }
    return sb.toString();
  }

  
  protected List<String> buildXSD(String ... rows) {
    XSDGenerator xsd = new XSDGenerator("TypeInfoGeneratorTest.xsd").namespace(null, "http://www.gip.com").rows(rows);
    List<String> xsdFiles = new ArrayList<String>();
    try {
      xsdFiles.add( xsd.save() );
    } catch( IOException e ) {
      fail(e.getMessage());
    }
    return xsdFiles;
  }
  
  protected List<String> buildXSDs(XSDGenerator ... xsds) {
    List<String> xsdFiles = new ArrayList<String>();
    for( XSDGenerator xsd : xsds ) {
      try {
        xsdFiles.add( xsd.save() );
      } catch( IOException e ) {
        fail(e.getMessage());
      }
    }
    return xsdFiles;
  }
  
  protected List<String> buildXSDWithNamespace(String namespace, String ... rows) {
    XSDGenerator xsd = new XSDGenerator("TypeInfoGeneratorTest.xsd").namespace(null, namespace).rows(rows);
    List<String> xsdFiles = new ArrayList<String>();
    try {
      xsdFiles.add( xsd.save() );
    } catch( IOException e ) {
      fail(e.getMessage());
    }
    return xsdFiles;
  }

}
