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
-- View </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text>
-- #############################################################################
</xsl:text><xsl:value-of select="Comment"/><xsl:text>
</xsl:text>
<xsl:if test="@materialized='Y'">
<xsl:text>DROP MATERIALIZED VIEW </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text>;
-- Kann Fehler ausloesen, wenn nicht vorhanden 
-- Evtl. muss folgender Befehl ausgefuehrt werden: 
-- DROP VIEW </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text>;

CREATE MATERIALIZED VIEW </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text>
REFRESH </xsl:text><xsl:value-of select="@refresh"/>
<xsl:if test="string-length(@startWith)>0"><xsl:text>
START WITH </xsl:text><xsl:value-of select="@startWith"/></xsl:if>
<xsl:if test="string-length(@next)>0"><xsl:text>
NEXT </xsl:text><xsl:value-of select="@next"/></xsl:if>
<xsl:text>
WITH </xsl:text><xsl:value-of select="@with"/><xsl:text>
AS </xsl:text>
</xsl:if>
<xsl:if test="string-length(@materialized)=0 or not(@materialized='Y')">
<xsl:text>CREATE OR REPLACE VIEW </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if> AS 
</xsl:if>
<xsl:value-of select="Statement"/>
<xsl:text>;

-- test column consistency
SELECT </xsl:text>
<xsl:for-each select="Column">
<xsl:if test="position()>1"><xsl:text>, </xsl:text></xsl:if>
<xsl:value-of select="@name"/>
</xsl:for-each>
<xsl:text> FROM </xsl:text>
<xsl:if test="@type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text> LIMIT 1;
</xsl:text>

</xsl:template>
</xsl:stylesheet>
