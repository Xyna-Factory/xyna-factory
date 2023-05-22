<!--
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
-->
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   version="1.0" xmlns:xyna="http://www.gip.com/xyna/xdev/xfractmod">

   <xsl:output method="text" encoding="iso-8859-1" />
   
   <xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
   <xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
   <xsl:param name="suffix" />

   <xsl:template match="xyna:DataType">
      <xsl:text>/*
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
</xsl:text>
      <xsl:text>package </xsl:text>
      <xsl:value-of select="@TypePath" />
      <xsl:text>;</xsl:text>
      <xsl:apply-templates select="xyna:Service" />
   </xsl:template>

   <xsl:template match="xyna:Service">
      <xsl:text>

/**
 * DO NOT EDIT!!!
 * Auto-generated class for checking the interface of the service implementation against the mdm datatype.
 * Please DON'T add this file to svn version control.
 */
public class </xsl:text>
      <xsl:value-of select="@TypeName" />
      <xsl:value-of select="$suffix" />
      <xsl:text> {

</xsl:text>
      <xsl:apply-templates select="xyna:Operation" />
      <xsl:text>}</xsl:text>
   </xsl:template>

   <xsl:template match="xyna:Operation">
      <xsl:text>  public void check</xsl:text>
      <xsl:value-of select="translate(substring(@Name,1,1),$lcletters,$ucletters)"/>
      <xsl:value-of select="substring(@Name,2)"/>
      <xsl:text>() {
    </xsl:text>
      <xsl:if test="count(xyna:Throws) != 0">
         <xsl:text>try {
      </xsl:text>
      </xsl:if>
      <!-- Output -->
      <xsl:choose>
         <xsl:when test="count(xyna:Output/xyna:Data) + count(xyna:Output/xyna:Exception) = 0">
            <xsl:text></xsl:text>
         </xsl:when>
         <xsl:when test="count(xyna:Output/xyna:Data) + count(xyna:Output/xyna:Exception) = 1">
            <xsl:choose>
               <xsl:when test="xyna:Output/*/@IsList">
                  <xsl:text>java.util.List&lt;? extends </xsl:text>
                  <xsl:call-template name="getFQClassname">
                    <xsl:with-param name="referencePath" select="xyna:Output/*/@ReferencePath"/>
                    <xsl:with-param name="referenceName" select="xyna:Output/*/@ReferenceName" />
                  </xsl:call-template>
                  <xsl:text>&gt; </xsl:text>
                  <xsl:value-of select="xyna:Output/*/@VariableName" />
                  <xsl:text> = </xsl:text>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:call-template name="getFQClassname">
                    <xsl:with-param name="referencePath" select="xyna:Output/*/@ReferencePath"/>
                    <xsl:with-param name="referenceName" select="xyna:Output/*/@ReferenceName" />
                  </xsl:call-template>
                  <xsl:text> </xsl:text>
                  <xsl:value-of select="xyna:Output/*/@VariableName" />
                  <xsl:text> = </xsl:text>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:otherwise>
            <!-- TODO -->
         </xsl:otherwise>
      </xsl:choose>
      <xsl:value-of select="../@TypeName" />
      <xsl:text>Impl.</xsl:text>
      <xsl:value-of select="@Name" />
      <xsl:text>(</xsl:text>
      <!-- Input -->
      <xsl:for-each select="xyna:Input/xyna:Data | xyna:Input/xyna:Exception">
         <xsl:choose>
            <xsl:when test="@IsList">
               <xsl:text>new java.util.ArrayList&lt;</xsl:text>
               <xsl:call-template name="getFQClassname">
                    <xsl:with-param name="referencePath" select="@ReferencePath"/>
                    <xsl:with-param name="referenceName" select="@ReferenceName" />
                 </xsl:call-template>
                 <xsl:text>&gt;()</xsl:text>
            </xsl:when>
            <xsl:otherwise>
               <xsl:text>new </xsl:text>
               <xsl:call-template name="getFQClassname">
                    <xsl:with-param name="referencePath" select="@ReferencePath"/>
                    <xsl:with-param name="referenceName" select="@ReferenceName" />
                 </xsl:call-template>
              <xsl:text>(</xsl:text>
              <xsl:if test="local-name() = 'Exception'">
                 <xsl:choose>
                    <xsl:when test="@ReferencePath = 'core.exception'">
                          <xsl:text>&quot;&quot;</xsl:text>
                       </xsl:when>
                       <xsl:otherwise>
                          <xsl:text>null</xsl:text>
                       </xsl:otherwise>
                    </xsl:choose>
                 </xsl:if>
              <xsl:text>)</xsl:text>
              </xsl:otherwise>
         </xsl:choose>
         <xsl:if test="position() != last()">
           <xsl:text>, </xsl:text>
         </xsl:if>
      </xsl:for-each>
      <xsl:text>);</xsl:text>
      <!-- Exceptions -->
      <xsl:if test="count(xyna:Throws) != 0">
         <xsl:text>
    }
</xsl:text>
         <xsl:for-each select="xyna:Throws/xyna:Exception">
            <xsl:text>    catch(</xsl:text>
                <xsl:call-template name="getFQClassname">
                    <xsl:with-param name="referencePath" select="@ReferencePath"/>
                    <xsl:with-param name="referenceName" select="@ReferenceName" />
                 </xsl:call-template>
            <xsl:text> </xsl:text>
            <xsl:value-of select="@VariableName" />
            <xsl:text>) {
    }</xsl:text>
        </xsl:for-each>
     </xsl:if>
     <xsl:text>
  }

</xsl:text>
   </xsl:template>
   
   <xsl:template name="getFQClassname">
      <xsl:param name="referencePath" />
      <xsl:param name="referenceName" />
      <xsl:choose>
         <xsl:when test="$referencePath = 'core.exception'">
            <xsl:choose>
               <xsl:when test="$referenceName = 'Exception'">
                  <xsl:text>java.lang</xsl:text>
               </xsl:when>
               <xsl:when test="$referenceName = 'XynaExceptionBase'">
                  <xsl:text>com.gip.xyna.xdev.xfractmod.xmdm</xsl:text>
               </xsl:when>
               <xsl:when test="$referenceName = 'XynaException'">
                  <xsl:text>com.gip.xyna.utils.exceptions</xsl:text>
               </xsl:when>
            </xsl:choose>
         </xsl:when>
         <xsl:when test="$referenceName = 'SchedulerBean'">
            <xsl:text>com.gip.xyna.xprc.xsched</xsl:text>
         </xsl:when>
         <xsl:otherwise>
            <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$referencePath"/>
                <xsl:with-param name="replace" select="'.3.0.'"/>
                <xsl:with-param name="by" select="'._3._0.'"/>
             </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:text>.</xsl:text>
      <xsl:value-of select="$referenceName" />
      <!-- Workaround for abstract datatype -->
      <xsl:if test="$referenceName = 'SNMPConnectionData'">
         <xsl:text>V2c</xsl:text>
      </xsl:if>
   </xsl:template>
   
   <xsl:template name="string-replace-all">
      <xsl:param name="text"/>
      <xsl:param name="replace"/>
      <xsl:param name="by"/>
      <xsl:choose>
         <xsl:when test="contains($text,$replace)">
            <xsl:value-of select="substring-before($text,$replace)"/>
            <xsl:value-of select="$by"/>
            <xsl:call-template name="string-replace-all">
               <xsl:with-param name="text" select="substring-after($text,$replace)"/>
               <xsl:with-param name="replace" select="$replace"/>
               <xsl:with-param name="by" select="$by"/>
            </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
            <xsl:value-of select="$text"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

</xsl:stylesheet>