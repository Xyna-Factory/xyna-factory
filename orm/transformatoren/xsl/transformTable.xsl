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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="iso-8859-1" />
<xsl:template match="DBObject"><xsl:text>
-- #############################################################################
-- Tabelle </xsl:text><xsl:value-of select="@aliasName"/>
<xsl:if test="string-length(@name)>30">
<xsl:message>Table-Name to long (>30): @name</xsl:message>
<xsl:message terminate="yes"></xsl:message>
</xsl:if>
<xsl:text>
-- #############################################################################
-- 
-- 
CREATE TABLE </xsl:text><xsl:value-of select="@name"/> (
<xsl:for-each select="Column">
<xsl:text>  </xsl:text>
<xsl:value-of select="@name"/>
<xsl:text> </xsl:text> <xsl:value-of select="@type"/>
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE') and not(@type='LONG') and not(@type='INTEGER') and not(@type='FLOAT') and not(@type='CLOB') and not(@type='BLOB')">
<xsl:text>(</xsl:text>
<xsl:value-of select="@length"/>
<xsl:if test="@type='NUMBER' and not(@scale=0)"> <xsl:text>, </xsl:text><xsl:value-of select="@scale"/> </xsl:if>
<xsl:text>)</xsl:text>
</xsl:if>
<xsl:if test="not(string-length(@default)=0)"> 
  <xsl:text> DEFAULT </xsl:text><xsl:value-of select="@default"/> 
</xsl:if>
<xsl:if test="not(position()=last())"><xsl:text>,
</xsl:text></xsl:if>
</xsl:for-each>
<xsl:text>)
</xsl:text>
<xsl:value-of select="Storage"/>
<xsl:text>;

</xsl:text>

<xsl:for-each select="Synonym">CREATE OR REPLACE PUBLIC SYNONYM <xsl:value-of select="@name"/> FOR <xsl:value-of select="../@name"/>;
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
