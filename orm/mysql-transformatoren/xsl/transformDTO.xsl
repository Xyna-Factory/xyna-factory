<!--
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"                 
                version="1.0"
                xmlns:lxslt="http://xml.apache.org/xslt"
                xmlns:my-ext="ext1"
                extension-element-prefixes="my-ext">
<xsl:output method="text" encoding="iso-8859-1" />
<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
<xsl:variable name="primaryKey"></xsl:variable>

<lxslt:component prefix="my-ext" elements="rules" 
                 functions="ret upper_first">
    <lxslt:script lang="javascript">
      function upper_first(s1)
      {
//        var retVal = new String(s1);
//        retVal = retVal.charAt(0).toUpperCase() + retVal.substring(1);
//        return retVal;
          return s1;
      }
  </lxslt:script>
</lxslt:component>
    
<xsl:template match="DBObject">
<!--<xsl:value-of select="my-ext:upperFirst(string(@name))"/>-->
<xsl:text>/*
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
package gip.common.dto;

import gip.base.common.*;
import java.io.Serializable;

</xsl:text>

<xsl:if test="@type='dto'">
<xsl:for-each select="Statement">
  <xsl:if test="not(@javadoc='no')">
  <xsl:text>/** </xsl:text><xsl:value-of select="@descr"/>
 *
 * Zugrundeliegende SQL-Definition des Views: <script> &lt;pre&gt; </script><xsl:text>
</xsl:text>
    <xsl:value-of select="../Statement"/>
    <script> &lt;/pre&gt; </script>
    <xsl:text> */ </xsl:text>
  </xsl:if>
</xsl:for-each>
</xsl:if>
<xsl:text>
@SuppressWarnings("serial")
public class </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO extends OBDTO {

</xsl:text> 

<!-- ********************************** Attribute definieren ************************** -->

<xsl:for-each select="Column"><xsl:text>  /** </xsl:text> <xsl:value-of select="@descr"/>
<xsl:text> (</xsl:text> <xsl:value-of select="@javaType"/> <xsl:text>) */
  public OBAttribute </xsl:text> <xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:for-each>

<xsl:text>

<!-- ********************************** Spaltenlisten fuer Insert und Select  ******** -->

  /** Spaltenliste fuer Insert */
  private final static String tableDetails = "</xsl:text>
<xsl:value-of select="@sqlName"/><xsl:text>(</xsl:text>
<xsl:for-each select="Column"><xsl:value-of select="@name"/>
<xsl:if test="not(position()=last())">, </xsl:if> 
<xsl:if test="position()=last()">
  <xsl:text>) "; //$NON-NLS-1$</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>;
  /** Spaltenliste fuer Select */
  private final static String tableSelect = " </xsl:text>
<xsl:for-each select="Column">
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE')"><xsl:value-of select="@name"/> </xsl:if>
<xsl:if test="@type='DATE' or @type='TIMESTAMP WITH LOCAL TIME ZONE'">
  <xsl:text>TO_CHAR(</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>,'</xsl:text>
  <xsl:if test="string-length(@format)=0">
    <xsl:text>DD.MM.YYYY</xsl:text>
  </xsl:if>
  <xsl:if test="string-length(@format)>0">
    <xsl:value-of select="@format"/>
  </xsl:if>
  <xsl:text>') </xsl:text> 
</xsl:if>
<xsl:if test="not(position()=last())">, </xsl:if> <xsl:if test="position()=last()">
  <xsl:text> "; //$NON-NLS-1$</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>;
</xsl:text> 
<!-- ********************************** Konstruktor ohne Attributwerte *************** -->

<xsl:text>
  /** Konstruktor ohne Parameter. Es wird kein Objekt in der DB erzeugt 
      oder aus der DB selectiert. */
  public </xsl:text> <xsl:value-of select="@tkName"/><xsl:text>DTO () {
    super();
    comment    = "</xsl:text><xsl:value-of select="@descr"/><xsl:text>"; //$NON-NLS-1$
    tableName  = "</xsl:text><xsl:value-of select="@viewName"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
<xsl:if test="@type='dto'">
<xsl:for-each select="Column">
<xsl:if test="substring(@descr,1,11)='PRIMARY KEY'"><xsl:text>
    primaryKey = "</xsl:text><xsl:value-of select="@name"/><xsl:text>"; //$NON-NLS-1$</xsl:text></xsl:if>
</xsl:for-each>
</xsl:if>

<xsl:text>
    attArr = new OBAttribute[</xsl:text>
      <xsl:for-each select="Column">
      <xsl:if test="position()=last()"><xsl:value-of select="last()"/></xsl:if>
      </xsl:for-each>
<xsl:text>];

</xsl:text>

  <!-- ********************************** Einzelne Attribute belegen ***************** -->

<xsl:for-each select="Column"><xsl:text>    </xsl:text> 
<xsl:value-of select="@name"/><xsl:text> = new OBAttribute("</xsl:text>
<xsl:value-of select="@name"/><xsl:text>", OBConstants.</xsl:text>
<xsl:if test="@type='VARCHAR2'">STRING,</xsl:if>
<xsl:if test="@type='CHAR'">STRING,</xsl:if>
<xsl:if test="@type='LONG'">LONGVARCHAR,</xsl:if>
<xsl:if test="@type='CLOB'">CLOB,</xsl:if>
<xsl:if test="@type='BLOB'">BLOB,</xsl:if>
<xsl:if test="@javaType='int'">INTEGER,</xsl:if>
<xsl:if test="@javaType='double'">DOUBLE,</xsl:if>
<xsl:if test="@javaType='boolean'">BOOLEAN,</xsl:if>
<xsl:if test="@javaType='long'">LONG,</xsl:if>
<xsl:if test="@javaType='intEnum'">INTEGER,</xsl:if>
<xsl:if test="@javaType='booleanEnum'">BOOLEAN,</xsl:if>
<xsl:if test="@javaType='longEnum'">LONG,</xsl:if>
<xsl:if test="@type='DATE'">DATE,</xsl:if>
<xsl:if test="@type='TIMESTAMP WITH LOCAL TIME ZONE'">TIMESTAMP_WITH_LOCAL_TIME_ZONE,</xsl:if>
<xsl:if test="@type='DATE'">30,</xsl:if>
<xsl:if test="@type='TIMESTAMP WITH LOCAL TIME ZONE'">30,</xsl:if>
<xsl:if test="@type='CLOB'">OBConstants.MAX_CLOB_LENGTH,</xsl:if>
<xsl:if test="@type='BLOB'">OBConstants.MAX_CLOB_LENGTH,</xsl:if>
<xsl:if test="@javaType='boolean'">1,</xsl:if>
<xsl:if test="@javaType='booleanEnum'">1,</xsl:if>
<xsl:if test="@type='INTEGER' and not(@javaType='boolean') and not(@javaType='booleanEnum')">38,</xsl:if>
<xsl:if test="@type='FLOAT'">38,</xsl:if>
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE') and not(@type='FLOAT') and not(@type='INTEGER') and not(@type='BLOB') and not(@type='CLOB')"><xsl:value-of select="@length"/><xsl:text>,</xsl:text></xsl:if>
 
<xsl:if test="@nullable='Y'">true,"</xsl:if>
<xsl:if test="not(@nullable='Y')">false,"</xsl:if>
<xsl:value-of select="@descr"/><xsl:text>"); //$NON-NLS-1$//$NON-NLS-2$
</xsl:text>
<xsl:if test="string-length(@format)>0">
<xsl:text>    </xsl:text><xsl:value-of select="@name"/><xsl:text>.setFormatMask("</xsl:text><xsl:value-of select="@format"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>    attArr[</xsl:text><xsl:value-of select="position()-1"/><xsl:text>] = </xsl:text>
<xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:for-each>

  <!-- ********************************** Primary Key belegen ********************** -->

<xsl:if test="@type='dto'">
<xsl:for-each select="Column">
<xsl:if test="substring(@descr,1,11)='PRIMARY KEY'">
<xsl:text>
    primaryKeyAtt = </xsl:text><xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
<xsl:text>
    </xsl:text><xsl:value-of select="@name"/><xsl:text>.setIgnored(true);
</xsl:text> 
</xsl:if>
</xsl:for-each>
</xsl:if>
<xsl:text>  }

</xsl:text>
<xsl:if test="@type='dto'">
<xsl:text>
  /**
   * Liefert ein Object des Typs
   * @return OBDTO
   */
  public static OBDTO getInstance() {
    return new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO();
  }

</xsl:text>
</xsl:if>
<!-- ********************************** Zugriffsfunktionen fuer Attribute *************** -->

<xsl:text>  /* Zugriffsfunktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public </xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>() </xsl:text><xsl:if test="not (@javaType='String')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ return </xsl:text>
<xsl:if test="@javaType='enum'"></xsl:if>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.valueOf(</xsl:if><xsl:if test="not(@javaType='enum')">.staticFromDbVal(</xsl:if></xsl:if>
<xsl:value-of select="@name"/>
<xsl:text>.get</xsl:text>
<xsl:if test="@javaType='int' or @javaType='intEnum'">Int</xsl:if>
<xsl:if test="@javaType='long' or @javaType='longEnum'">Long</xsl:if>
<xsl:if test="@javaType='boolean' or @javaType='booleanEnum'">Boolean</xsl:if>
<xsl:if test="@javaType='double'">Double</xsl:if>
<xsl:if test="@javaType='byte[]'">ByteArray</xsl:if>
<xsl:text>Value()</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)">)</xsl:if>
<xsl:text>; }
</xsl:text>
<xsl:text>  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> val) </xsl:text><xsl:if test="not (@javaType='String' or @javaType='byte[]')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.staticName(val)</xsl:if><xsl:if test="not(@javaType='enum')">.staticGetDbVal(val)</xsl:if></xsl:if>
<xsl:if test="string-length(@enumType)=0">val</xsl:if>
<xsl:text>);}
</xsl:text>
<xsl:text>  public </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO fill</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> val) </xsl:text><xsl:if test="not (@javaType='String' or @javaType='byte[]')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.staticName(val)</xsl:if><xsl:if test="not(@javaType='enum')">.staticGetDbVal(val)</xsl:if></xsl:if>
<xsl:if test="string-length(@enumType)=0">val</xsl:if>
<xsl:text>); return this; }
</xsl:text>
<xsl:if test="@javaType='intEnum'">
<xsl:text>  public int get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getIntValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(int value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='longEnum'">
<xsl:text>  public long get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getLongValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(long value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='booleanEnum'">
<xsl:text>  public boolean get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getBooleanValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(boolean value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='stringEnum' or @javaType='enum'">
<xsl:text>  public String get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(String value) { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- ********************************** Nullfunktionen fuer Attribute *************** -->
 
<xsl:text> 
  /* Null-Behandlungs-Funktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public boolean is</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>Null() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.isNull(); }
</xsl:text>
<xsl:text>  public boolean is</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>NotNull() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.isNotNull(); }
</xsl:text>
<xsl:text>  public </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>Null() { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setNull(); return this; }
</xsl:text>
</xsl:for-each>

<!-- ********************************** Ignorefunktionen fuer Attribute *************** -->
 
<xsl:text> 
  /* IGNORED-Behandlungs-Funktionen fuer Attribute */
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>  
  public boolean is</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>Ignored() {
    return </xsl:text><xsl:value-of select="@name"/><xsl:text>.isIgnored(); 
  }
</xsl:text>
</xsl:for-each>
<!-- *********************** CompOperator-Funktionen fuer Attribute *************** -->
 
<xsl:text> 
  /* Vergleichs-Operator-Funktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>CompOp(String compOp) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setCompOp(compOp); return this; }
</xsl:text>
<xsl:text>  public String get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>CompOp() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getCompOp(); }
</xsl:text>
</xsl:for-each>

<!-- ***************** statische name-Konstanten fuer Attribute *************** -->
 
<xsl:text> 
  /* name-Konstanten fuer Attribute */
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>  public static final String name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="@name"/><xsl:text>";//$NON-NLS-1$
</xsl:text>
</xsl:for-each>

<!-- ***************** statische label-Konstanten fuer Attribute *************** -->
 
<xsl:text> 
  /* label-Konstanten fuer Attribute */
</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="string-length(@label)>0">
<xsl:text>  public static final String label</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="@label"/>
<xsl:text>";
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- ********************************** Zugriffsfunktionen fuer Parameter *************** -->

<xsl:text>
  /* Parameter und deren Zugriffsfunktionen */

</xsl:text>

<xsl:for-each select="Param">
<xsl:text>
  public static final String name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="@name"/><xsl:text>";
</xsl:text>

<xsl:text>  /** ACHTUNG: Parameter, kein Attribut */</xsl:text>
<xsl:if test="not(string-length(@templateType))=0">
<xsl:text>
  @SuppressWarnings("unchecked")</xsl:text>
</xsl:if>
<xsl:text>
  public </xsl:text> 
<xsl:value-of select="@javaType"/><xsl:if test="not(string-length(@templateType))=0">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if>
<xsl:text> get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>() { return ((</xsl:text>
<xsl:choose>
  <xsl:when test="@javaType='int'">
    <xsl:text>Integer</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='long'">
    <xsl:text>Long</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='boolean'">
    <xsl:text>Boolean</xsl:text> 
  </xsl:when>
  <xsl:otherwise>
    <xsl:value-of select="@javaType"/><xsl:if test="not(string-length(@templateType))=0">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if>
  </xsl:otherwise>
</xsl:choose>
<xsl:text>) parameters.get(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>))</xsl:text>
<xsl:choose>
  <xsl:when test="@javaType='int'">
    <xsl:text>.intValue()</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='long'">
    <xsl:text>.longValue()</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='boolean'">
    <xsl:text>.booleanValue()</xsl:text> 
  </xsl:when>
</xsl:choose>
<xsl:text>; }
</xsl:text>
<xsl:text>  /** ACHTUNG: Parameter, kein Attribut */
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text><xsl:value-of select="@javaType"/><xsl:if test="not(string-length(@templateType))=0">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if>
<xsl:text> val)  { parameters.put(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>, val); }
</xsl:text>
<xsl:text>  /** ACHTUNG: Parameter, kein Attribut */
  public </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO fill</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text><xsl:value-of select="@javaType"/><xsl:if test="not(string-length(@templateType))=0">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if>
<xsl:text> val)  { parameters.put(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>, val); return this; }
</xsl:text>
<xsl:text>  public boolean is</xsl:text><xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/><xsl:text>Null() { return !parameters.containsKey(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>); }
</xsl:text>
<xsl:text>  public </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/><xsl:text>Null () { parameters.remove(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>); return this; }
</xsl:text>
<xsl:text>  public boolean is</xsl:text><xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/><xsl:text>Ignored() { return false; }
  
</xsl:text> 
</xsl:for-each>


<xsl:text>
  public Object getParameterAndFillIfNull(String key) {
    if (parameters.get(key)!=null) {
      return parameters.get(key);
    }
</xsl:text>  
<xsl:for-each select="Param">
  <xsl:if test="string-length(@ignoreNotNull)=0 or not(@ignoreNotNull='true')">
    <xsl:text>    if (key.equalsIgnoreCase(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>)) {
      parameters.put(name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>, new </xsl:text>
<xsl:choose>
  <xsl:when test="@javaType='int' or @javaType='Integer'">
    <xsl:text>Integer(OBAttribute.NULL)</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='long' or @javaType='Long'">
    <xsl:text>Long(OBAttribute.NULL)</xsl:text> 
  </xsl:when>
  <xsl:when test="@javaType='boolean' or @javaType='Boolean'">
    <xsl:text>Boolean(false)</xsl:text> 
  </xsl:when>
  <xsl:when test="string-length(substring-before(@javaType,'[]'))>0">
    <xsl:value-of select="substring-before(@javaType,'[]')"/><xsl:text>[0]</xsl:text> 
  </xsl:when>
  <xsl:otherwise>
    <xsl:value-of select="@javaType"/>
    <xsl:if test="not(string-length(@templateType))=0"><xsl:text>&lt;</xsl:text>
    <xsl:if test="string-length(substring-after(@templateType,'? extends '))>0">
      <xsl:value-of select="substring-after(@templateType,'? extends ')"/>
    </xsl:if>
    <xsl:if test="string-length(substring-after(@templateType,'? extends '))=0">
      <xsl:value-of select="@templateType"/>
    </xsl:if>
    <xsl:text>&gt;</xsl:text></xsl:if><xsl:text>()</xsl:text>
  </xsl:otherwise>
</xsl:choose>
<xsl:text>);
    }
</xsl:text>
  </xsl:if>
</xsl:for-each>
<xsl:text>  
    if (parameters.get(key)!=null) {
      return parameters.get(key);
    }
    return "";
  }
</xsl:text> 
<!-- ********************************** Diverse HilfsFunktionen *************** -->

<xsl:text>
  /** 
   * Liefert die AttributListe fuers Insert 
   * @return AttributListe fuers Insert
   */
  protected String tableDetails() { 
    return tableDetails; 
  }


  /** 
   * Liefert die AttributListe fuers Select 
   * @return AttributListe fuers Select
   */
  public String tableSelect() { 
    return tableSelect; 
  }
  

  /** 
   * Liefert die Anzahl der Attribute 
   * @return Anzahl der Attribute
   */
  public int numAttr() { 
    return </xsl:text>
<xsl:for-each select="Column">
<xsl:if test="position()=last()"><xsl:value-of select="last()"/></xsl:if>
</xsl:for-each><xsl:text>;
  }
</xsl:text>

<!-- ************************** Funktionen fuer Klassenname und Objekt  *************** -->

<xsl:text>
  /** 
   * Liefert den Klassennamen als String zurueck 
   * @return Klassenname
   */
  public String getClassName() {
    return "</xsl:text><xsl:value-of select="@viewName"/><xsl:text>"; //$NON-NLS-1$
  }
  
</xsl:text>

<xsl:text>
  /** 
   * Liefert ein Objekt des Typs </xsl:text> <xsl:value-of select="@tkName"/> <xsl:text>DTO zurueck 
   * @return Objekt des Typs </xsl:text> <xsl:value-of select="@tkName"/> <xsl:text>DTO
   */
  public Object getObject() {
    return new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO();
  }
  
  
  /** Methode, die das abstrakte Schema zurueckliefert (z.B. ipnet)
   * @return das abstrakte Schema
   */
  public String getProjectSchema() {
    return "</xsl:text>
<xsl:if test="string-length(@projectSchema)=0"> <xsl:text>ipnet</xsl:text></xsl:if>
<xsl:if test="string-length(@projectSchema)>0"> <xsl:value-of select="@projectSchema"/></xsl:if>
<xsl:text>"; //$NON-NLS-1$
  }
  
  
  /** Methode, die den SQL-Namen liefert
   * @return SQL-Name
   */
  public String getSQLName() {
    return "</xsl:text> <xsl:value-of select="@sqlName"/><xsl:text>";  //$NON-NLS-1$
  }

  /** Methode, die den SQL-Namen mit project liefert
   * @return SQL-Name
   */
  public static String getSQLRepresentation() {
    return OBObject.START_PROJECT_SCHEMA +  "</xsl:text>
           <xsl:if test="string-length(@projectSchema)=0"> <xsl:text>ipnet</xsl:text></xsl:if>
           <xsl:if test="string-length(@projectSchema)>0"> <xsl:value-of select="@projectSchema"/></xsl:if>
           <xsl:text>" + OBObject.END_PROJECT_SCHEMA +   //$NON-NLS-1$
           ".</xsl:text><xsl:value-of select="@sqlName"/><xsl:text>"; //$NON-NLS-1$ 
  }


  /** Liefert die Demultiplexingklasse 
   * @return Demultiplexingclasse fuer Reflection
   */
  public String getDMClassName() {
    return "gip.core.demultiplexing.</xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM"; //$NON-NLS-1$
  }
</xsl:text>

<!-- ************************** DTO-Spezialitaten  ************************************ -->
<xsl:text>

  /* -------------------------- DTO-Spezialitaeten ------------------------------------ */

</xsl:text>


<!-- ************************** Moegliche Capabilities  ******************************* -->

<xsl:text>  /** Interface mit den moeglichen Capabilities */
  public interface PosCap extends Serializable {
    /** @deprecated Bitte nicht benutzen, wird nur von der Benutzerverwaltung verwendet */
    public final static long find                 = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+01;
    public final static long findPK               = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+02;
    public final static long findFilter           = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+03;
    public final static long findWC               = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+04;
    /** @deprecated Bitte nicht benutzen, wird nur von der Benutzerverwaltung verwendet */
    public final static long findAll              = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+10;
    public final static long findAllPKs           = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+11;
    public final static long findAllFilter        = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+12;
    public final static long findAllWC            = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+13;
    /** @deprecated Bitte nicht benutzen, wird nur von der Benutzerverwaltung verwendet */
    public final static long count                = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+20;
    public final static long countPKs             = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+21;
    public final static long countFilter          = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+22;
    public final static long countWC              = </xsl:text><xsl:value-of select="@id"/><xsl:text>00+23;
</xsl:text>
<xsl:for-each select="Capability">

<xsl:if test="@type='DELETE'"><xsl:text>
    public final static long delete               = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+30;
    public final static long findAndDeletePKs     = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+31;
    public final static long findAndDeleteFilter  = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+32;
    public final static long findAndDeleteWC      = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+33;
    public final static long findAndDeleteList    = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+34;
    public final static long setLock              = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+36;
    public final static long getLock              = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+37;
    public final static long resetLock            = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+38;    
</xsl:text></xsl:if>
<xsl:if test="@type='NOTIFY'"><xsl:text>
    public final static long notifyTk             = </xsl:text><xsl:value-of select="../@id"/><xsl:text>00+35;

</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:for-each select="Capability">
<xsl:if test="@id&gt;99 or @id&lt;40">
<xsl:message terminate="yes">Capability-Id must be between 40 and 99: <xsl:value-of select="@javaName"/> (<xsl:value-of select="@id"/>)</xsl:message>
</xsl:if><xsl:if test="not(@type='GUI') and not(@type='DELETE') and not(@type='NOTIFY')">
<xsl:text>
    /** &lt;B&gt;Server-Methode: &lt;/B&gt;</xsl:text> <xsl:value-of select="../@tkName"/>.<xsl:value-of select="@javaName"/><xsl:text>()  &lt;BR&gt;
        &lt;B&gt;Return-Value: &lt;/B&gt; </xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DTO  &lt;BR&gt;
        &lt;B&gt;Benoetigte Parameter: &lt;/B&gt; &lt;BR&gt;
</xsl:text>
<xsl:for-each select="Param">
<xsl:text>        </xsl:text><xsl:value-of select="@dtoName"/> (<xsl:value-of select="@paramType"/>) &lt;BR&gt; 
</xsl:for-each>
<xsl:text>        &lt;BR&gt;
        </xsl:text>
<xsl:value-of select="../@tkName"/><xsl:text>DTO help = new </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO(); &lt;BR&gt; 
</xsl:text>
<xsl:for-each select="Param">
<xsl:text>        help.set</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@dtoName,2)"/><xsl:text>(val); &lt;BR&gt;
        help.</xsl:text><xsl:value-of select="@dtoName"/><xsl:text>.setIgnored(true); &lt;BR&gt;
</xsl:text>
</xsl:for-each>    
<xsl:text>    */
    public final static long </xsl:text>
<xsl:value-of select="@javaName"/><xsl:value-of select="@id"/>
<xsl:text> = </xsl:text>
<xsl:value-of select="../@id"/>00+<xsl:value-of select="@id"/>
<xsl:text> ;
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>  }

  /** Objekt, damit man auch dtoObject.possibleCapability.xyz aufrufen kann (Compilesicherheit!) 
      Bsp: OBRemoteRef.doOperation(dtoObject, dtoObject.possibleCapability.xyz); */
  public static final PosCap possibleCapability = new PosCap() {};

</xsl:text>

<!-- ************************** Checkbarkeit der Capabilities  ************************ -->

<xsl:text>
  /** 
   * Muss die Capability gecheckt werden
   * @param _capabilityId Id der zu pruefenden Capability
   * @return false, wenn kein Test notwendig ist, true wenn noetig oder Capability unbekannt
   */
  public boolean isCapabilityToCheck(long _capabilityId) { 
    if (_capabilityId==PosCap.find)                 return false;
    if (_capabilityId==PosCap.findPK)               return false;
    if (_capabilityId==PosCap.findFilter)           return false;
    if (_capabilityId==PosCap.findWC)               return false;
    if (_capabilityId==PosCap.findAll)              return false;
    if (_capabilityId==PosCap.findAllPKs)           return false;
    if (_capabilityId==PosCap.findAllFilter)        return false;
    if (_capabilityId==PosCap.findAllWC)            return false;
    if (_capabilityId==PosCap.count)                return false;
    if (_capabilityId==PosCap.countPKs)             return false;
    if (_capabilityId==PosCap.countFilter)          return false;
    if (_capabilityId==PosCap.countWC)              return false;
</xsl:text>
<xsl:for-each select="Capability">

<xsl:if test="@type='DELETE'"><xsl:text>
    if (_capabilityId==PosCap.delete)               return true;
    if (_capabilityId==PosCap.findAndDeletePKs)     return true;
    if (_capabilityId==PosCap.findAndDeleteFilter)  return true;
    if (_capabilityId==PosCap.findAndDeleteWC)      return true;
    if (_capabilityId==PosCap.findAndDeleteList)    return true;
    if (_capabilityId==PosCap.setLock)              return true;
    if (_capabilityId==PosCap.getLock)              return true;
    if (_capabilityId==PosCap.resetLock)            return true;
</xsl:text></xsl:if>
<xsl:if test="@type='NOTIFY'"><xsl:text>
    if (_capabilityId==PosCap.notifyTk)             return true;
</xsl:text>  
</xsl:if>
</xsl:for-each>
<xsl:for-each select="Capability">
<xsl:if test="not(@type='GUI') and not(@type='DELETE') and not(@type='NOTIFY')">
<xsl:text>    if (_capabilityId==PosCap.</xsl:text>
<xsl:value-of select="@javaName"/><xsl:value-of select="@id"/>
<xsl:text>) return </xsl:text>
<xsl:choose>
  <xsl:when test="@isChecked='yes'"><xsl:text>true</xsl:text></xsl:when>
  <xsl:when test="@isChecked='true'"><xsl:text>true</xsl:text></xsl:when>
  <xsl:when test="@isChecked='no'"><xsl:text>false</xsl:text></xsl:when>
  <xsl:when test="@isChecked='false'"><xsl:text>false</xsl:text></xsl:when>
  <xsl:otherwise><xsl:text>true</xsl:text></xsl:otherwise>
</xsl:choose>
<xsl:text>;
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>
    return true;
  }
</xsl:text>
  
<xsl:text>

  /** 
   * Liefert die eigentlich zu pruefende Capability
   * @param _capabilityId angeforderte Capability
   * @return zu pruefende Capability bzw. OBAttribute.NULL, wenn nichts zu pruefen ist
   */
  public long getCheckCapability(long _capabilityId) {
    if (!isCapabilityToCheck(_capabilityId)) {
      return OBAttribute.NULL;
    }
</xsl:text>
<xsl:for-each select="Capability">
<xsl:if test="@type='DELETE'"><xsl:text>
    if (_capabilityId==PosCap.delete)               return PosCap.delete;
    if (_capabilityId==PosCap.findAndDeletePKs)     return PosCap.delete;
    if (_capabilityId==PosCap.findAndDeleteFilter)  return PosCap.delete;
    if (_capabilityId==PosCap.findAndDeleteWC)      return PosCap.delete;
    if (_capabilityId==PosCap.findAndDeleteList)    return PosCap.delete;
    if (_capabilityId==PosCap.setLock)              return PosCap.setLock;
    if (_capabilityId==PosCap.getLock)              return PosCap.getLock;
    if (_capabilityId==PosCap.resetLock)            return PosCap.resetLock;    
</xsl:text></xsl:if></xsl:for-each>
<xsl:text>
    return _capabilityId;
  }

</xsl:text>
<xsl:for-each select="Capability">
<xsl:if test="@type='NOTIFY'"><xsl:text>
  public boolean hasNotifyCapabilityId() {
    return true;
  }
</xsl:text></xsl:if></xsl:for-each>  
<xsl:text>
  
  public long getDTOId() {
    return </xsl:text><xsl:value-of select="@id"/>00<xsl:text>;
  }

  
</xsl:text>

<xsl:text>}
</xsl:text>

</xsl:template>
</xsl:stylesheet>
