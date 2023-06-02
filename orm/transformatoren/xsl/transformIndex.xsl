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
-- Tabellen-Index </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################</xsl:text>
<xsl:for-each select="Index">
CREATE <xsl:value-of select="@unique"/> INDEX <xsl:value-of select="@name"/><xsl:text> ON </xsl:text> 
  <xsl:value-of select="../@name"/>(<xsl:value-of select="@columnList"/><xsl:text>) TABLESPACE </xsl:text><xsl:value-of select="@tablespace"/>;</xsl:for-each>

<xsl:for-each select="Constraint">
<xsl:if test="@type='FK'">
<xsl:if test="not (@omitIndex='Y')">
CREATE INDEX <xsl:value-of select="@name"/>_x 
ON <xsl:value-of select="../@name"/> (<xsl:value-of select="@columnList"/>) TABLESPACE IDX;
</xsl:if>
</xsl:if>
</xsl:for-each>
</xsl:template></xsl:stylesheet>