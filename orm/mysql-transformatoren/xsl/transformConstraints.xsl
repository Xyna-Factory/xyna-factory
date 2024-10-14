<!--
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="iso-8859-1" />

<xsl:template match="DBObject"><xsl:text>
-- #############################################################################
-- Tabellen-Constraints </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################
</xsl:text>
<!-- PK, CHECK, UNIQUE-Constraints -->
<xsl:for-each select="Constraint">
<xsl:if test="string-length(@name)>30">
<xsl:message>Constraint-Name to long (>30): @name</xsl:message>
<xsl:message terminate="yes"></xsl:message>
</xsl:if>
<xsl:if test="not (@type='FK')">
<xsl:text>ALTER TABLE </xsl:text> <xsl:value-of select="../@name"/><xsl:text>
  ADD CONSTRAINT </xsl:text><xsl:value-of select="@name"/> <xsl:choose>
<xsl:when test="@type='U'"><xsl:text> UNIQUE (</xsl:text>
<xsl:if test="not(string-length(@columnList)=0)"><xsl:value-of select="@columnList"/></xsl:if>
<xsl:if test="string-length(@columnList)=0">
  <xsl:for-each select="Column">
    <xsl:value-of select="@name"/> <xsl:if test="not(position()=last())">, </xsl:if>
  </xsl:for-each>
</xsl:if>
<xsl:text>)</xsl:text> 
<xsl:if test="@deferrable='Y'"> 
<xsl:text> DEFERRABLE</xsl:text></xsl:if>
<xsl:if test="string-length(@initially)>0"> 
<xsl:text> INITIALLY </xsl:text><xsl:value-of select="@initially"/></xsl:if></xsl:when>
<xsl:when test="@type='PK'"> PRIMARY KEY (<xsl:value-of select="@columnList"/>)<xsl:if test="@deferrable='Y'"> 
<xsl:text> DEFERRABLE</xsl:text></xsl:if>
<xsl:if test="string-length(@initially)>0"> 
<xsl:text> INITIALLY </xsl:text><xsl:value-of select="@initially"/></xsl:if></xsl:when>
<xsl:when test="@type='C'"> CHECK (<xsl:value-of select="@searchCond"/>
<xsl:text>)</xsl:text> 
<xsl:if test="@deferrable='Y'"> 
<xsl:text> DEFERRABLE</xsl:text></xsl:if>
<xsl:if test="string-length(@initially)>0"> 
<xsl:text> INITIALLY </xsl:text><xsl:value-of select="@initially"/></xsl:if></xsl:when>
</xsl:choose>
<xsl:text>;
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- NOT NULL - Constraints -->
<xsl:for-each select="Column">
<xsl:if test="string-length(@nnConstrName)>30">
<xsl:message>nnConstrName to long (>30): @nnConstrName</xsl:message>
<xsl:message terminate="yes"></xsl:message>
</xsl:if>
<xsl:if test="not(string-length(@nnConstrName)=0)">
<xsl:text>ALTER TABLE </xsl:text> <xsl:value-of select="../@name"/> <xsl:text>
  MODIFY </xsl:text><xsl:value-of select="@name"/> 
<xsl:text> </xsl:text> 
<xsl:choose >
  <xsl:when test="@type='VARCHAR2'"><xsl:text>VARCHAR</xsl:text></xsl:when>
  <xsl:when test="@type='CLOB'"><xsl:text>LONGTEXT</xsl:text></xsl:when>
  <xsl:when test="@type='BLOB'"><xsl:text>LONGBLOB</xsl:text></xsl:when>
  <xsl:when test="@type='NUMBER'"><xsl:text>FLOAT</xsl:text></xsl:when>
  <xsl:when test="@type='DATE'"><xsl:text>TIMESTAMP</xsl:text></xsl:when>
  <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
</xsl:choose> 
<xsl:if test="not(@type='DATE')  and not(@type='TIMESTAMP WITH LOCAL TIME ZONE') and not(@type='LONG') and not(@type='INTEGER') and not(@type='CLOB') and not(@type='BLOB')">
<xsl:text>(</xsl:text>
<xsl:value-of select="@length"/>
<xsl:if test="@type='NUMBER' and not(@scale=0)"> <xsl:text>, </xsl:text><xsl:value-of select="@scale"/> </xsl:if>
<xsl:text>)</xsl:text>
</xsl:if>
<xsl:if test="not(string-length(@default)=0)"> 
  <xsl:text> DEFAULT </xsl:text>
  <xsl:choose >
    <xsl:when test="@default='sysdate'">CURRENT_TIMESTAMP</xsl:when>
    <xsl:otherwise><xsl:value-of select="@default"/></xsl:otherwise>
  </xsl:choose>
</xsl:if>
<xsl:text> NOT NULL</xsl:text> 
<xsl:if test="@deferrable='Y'"> 
<xsl:text> DEFERRABLE</xsl:text></xsl:if>
<xsl:if test="string-length(@initially)>0"> 
<xsl:text> INITIALLY </xsl:text><xsl:value-of select="@initially"/></xsl:if><xsl:text>; 
</xsl:text> 
</xsl:if>
</xsl:for-each>
<!-- Enum-Check - Constraints -->
<xsl:for-each select="Column">
<xsl:if test="string-length(@checkConstraintName)>30">
<xsl:message>checkConstraintName to long (>30): @nnConstrName</xsl:message>
<xsl:message terminate="yes"></xsl:message>
</xsl:if>
<xsl:if test="not(string-length(@checkConstraintName)=0)">
<xsl:text>ALTER TABLE </xsl:text> <xsl:value-of select="../@name"/> <xsl:text>
  ADD CONSTRAINT </xsl:text><xsl:value-of select="@checkConstraintName"/> 
<xsl:text> CHECK (</xsl:text> <xsl:value-of select="@name"/><xsl:text> IN (</xsl:text>
<xsl:for-each select="AllowedValue">
<xsl:if test="not(position()=1)">,</xsl:if>
<xsl:if test="not(../@type='INTEGER')">
  <xsl:text>'</xsl:text><xsl:value-of select="@value"/><xsl:text>'</xsl:text>
</xsl:if>
<xsl:if test="../@type='INTEGER'"><xsl:value-of select="@value"/></xsl:if>
</xsl:for-each>
<xsl:text>));
</xsl:text> 
</xsl:if>
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
