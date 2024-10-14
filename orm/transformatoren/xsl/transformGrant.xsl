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
-- Tabellen-Grants </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################</xsl:text>
<xsl:for-each select="Grant">
GRANT <xsl:value-of select="@privilege"/> ON <xsl:value-of select="../@name"/> TO <xsl:value-of select="@grantee"/><xsl:text> </xsl:text><xsl:value-of select="@grantoption"/>;</xsl:for-each>
<xsl:text>
</xsl:text>

<xsl:for-each select="Sequence">
<xsl:for-each select="Grant">
GRANT <xsl:value-of select="@privilege"/> ON <xsl:value-of select="../@name"/> TO <xsl:value-of select="@grantee"/><xsl:text> </xsl:text><xsl:value-of select="@grantoption"/>;</xsl:for-each></xsl:for-each>
<xsl:text>

</xsl:text></xsl:template>
</xsl:stylesheet>
