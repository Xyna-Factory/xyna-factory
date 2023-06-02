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
<xsl:for-each select="Sequence">
<xsl:if test="not(@createSQL='no')"><xsl:text>


-- #############################################################################
-- Sequence </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################
CREATE SEQUENCE </xsl:text><xsl:value-of select="@name"/> 
<!-- increment -->
<xsl:if test="not(string-length(@increment)=0)"> <xsl:text> INCREMENT BY </xsl:text>
<xsl:value-of select="@increment"/>
</xsl:if>
<!-- maxValue -->
<xsl:if test="@maxValue='no'"> <xsl:text> NOMAXVALUE</xsl:text></xsl:if> 
<xsl:if test="not(@maxValue='no')"> <xsl:text> MAXVALUE </xsl:text>
<xsl:value-of select="@maxValue"/></xsl:if> 
<!-- minValue -->
<xsl:if test="@minValue='no'"> <xsl:text> NOMINVALUE</xsl:text></xsl:if> 
<xsl:if test="not(@minValue='no')"> <xsl:text> MINVALUE </xsl:text>
<xsl:value-of select="@minValue"/></xsl:if> 
<!-- cycle -->
<xsl:if test="@cycle='no'"> <xsl:text> NOCYCLE</xsl:text></xsl:if> 
<xsl:if test="not(@cycle='no')"> <xsl:text> CYCLE</xsl:text></xsl:if> 
<!-- cache -->
<xsl:if test="@cache='no'"> <xsl:text> NOCACHE</xsl:text></xsl:if> 
<xsl:if test="not(@cache='no')"> <xsl:text> CACHE </xsl:text>
<xsl:value-of select="@cache"/></xsl:if> 
<!-- order -->
<xsl:if test="@order='no'"> <xsl:text> NOORDER</xsl:text></xsl:if> 
<xsl:text>;
</xsl:text>

<!-- synonym -->
<xsl:for-each select="Synonym">CREATE OR REPLACE PUBLIC SYNONYM <xsl:value-of select="@name"/> FOR <xsl:value-of select="../@name"/><xsl:text>;</xsl:text>
</xsl:for-each>

<!-- grants-->
<xsl:for-each select="Grant">
GRANT <xsl:value-of select="@privilege"/> ON <xsl:value-of select="../@name"/> TO <xsl:value-of select="@grantee"/><xsl:text>;</xsl:text>
</xsl:for-each>
<xsl:text>
</xsl:text>

</xsl:if>

</xsl:for-each>
</xsl:template>
</xsl:stylesheet>
