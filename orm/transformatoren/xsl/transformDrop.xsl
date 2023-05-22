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
DROP TABLE </xsl:text><xsl:value-of select="@name"/>
<xsl:text>;
</xsl:text>

<xsl:for-each select="Synonym">
<xsl:text>DROP PUBLIC SYNONYM </xsl:text><xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:for-each>

<xsl:for-each select="Sequence">
<xsl:if test="not(@createSQL='no')"><xsl:text>
-- #############################################################################
-- Sequence </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################
DROP SEQUENCE </xsl:text><xsl:value-of select="@name"/>; 

<xsl:for-each select="Synonym">
<xsl:text>DROP PUBLIC SYNONYM </xsl:text><xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:for-each>

</xsl:if>
</xsl:for-each>

</xsl:template>
</xsl:stylesheet>
