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

<xsl:template match="DBObject">
-- #############################################################################
-- Tabellen-Kommentare <xsl:value-of select="@name"/>
-- #############################################################################
ALTER TABLE <xsl:value-of select="@name"/> COMMENT='<xsl:value-of select="@aliasName"/>: <xsl:value-of select="@descr"/><xsl:text>';</xsl:text>
<xsl:for-each select="Column">
ALTER TABLE <xsl:value-of select="../@name"/> CHANGE <xsl:value-of select="@name"/><xsl:text> </xsl:text><xsl:value-of select="@name"/><xsl:text> </xsl:text>
<xsl:choose >
  <xsl:when test="@type='VARCHAR2'"><xsl:text>VARCHAR</xsl:text></xsl:when>
  <xsl:when test="@type='CLOB'"><xsl:text>LONGTEXT</xsl:text></xsl:when>
  <xsl:when test="@type='BLOB'"><xsl:text>LONGBLOB</xsl:text></xsl:when>
  <xsl:when test="@type='NUMBER'"><xsl:text>FLOAT</xsl:text></xsl:when>
  <xsl:when test="@type='DATE'"><xsl:text>TIMESTAMP</xsl:text></xsl:when>
  <xsl:otherwise><xsl:value-of select="@type"/></xsl:otherwise>
</xsl:choose> 
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE') and not(@type='LONG') and not(@type='INTEGER') and not(@type='FLOAT') and not(@type='CLOB') and not(@type='BLOB')">
<xsl:text>(</xsl:text>
<xsl:value-of select="@length"/>
<xsl:if test="@type='NUMBER' and not(@scale=0)"> <xsl:text>, </xsl:text><xsl:value-of select="@scale"/> </xsl:if>
<xsl:text>)</xsl:text>
</xsl:if>
<xsl:if test="not(string-length(@nnConstrName)=0)"> NOT NULL </xsl:if>
<xsl:if test="not(string-length(@default)=0)"> 
  <xsl:text> DEFAULT </xsl:text> 
  <xsl:choose >
    <xsl:when test="@default='sysdate'">CURRENT_TIMESTAMP</xsl:when>
    <xsl:otherwise><xsl:value-of select="@default"/></xsl:otherwise>
  </xsl:choose>
</xsl:if>
<xsl:text> COMMENT '</xsl:text><xsl:value-of select="@name"/>: <xsl:value-of select="@descr"/><xsl:text>';</xsl:text>
</xsl:for-each>
<xsl:text>
</xsl:text>
</xsl:template>
</xsl:stylesheet>

