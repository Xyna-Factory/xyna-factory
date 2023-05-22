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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="iso-8859-1" />

<xsl:template match="DBObject">
<xsl:for-each select="Sequence">
<xsl:if test="not(@createSQL='no')"><xsl:text>


-- #############################################################################
-- Sequence </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################
INSERT INTO SequenceTable(sequenceName, val, increment, maxVal) VALUES ('</xsl:text><xsl:value-of select="@name"/><xsl:text>',</xsl:text> 
<!-- minValue -->
<xsl:if test="@minValue='no'"><xsl:text>1000,</xsl:text></xsl:if> 
<xsl:if test="not(@minValue='no')"><xsl:value-of select="@minValue"/><xsl:text>,</xsl:text></xsl:if> 
<!-- increment -->
<xsl:if test="string-length(@increment)=0">1,</xsl:if>
<xsl:if test="not(string-length(@increment)=0)"><xsl:value-of select="@increment"/><xsl:text>,</xsl:text></xsl:if>
<!-- maxValue -->
<xsl:if test="@maxValue='no'"> <xsl:text> NULL</xsl:text></xsl:if> 
<xsl:if test="not(@maxValue='no')"><xsl:value-of select="@maxValue"/></xsl:if> 
<!-- cycle -->
<!-- cache -->
<!-- order -->
<xsl:text>);
</xsl:text>

</xsl:if>

</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
