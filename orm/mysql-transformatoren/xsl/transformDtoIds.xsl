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

<xsl:template match="DBObject"><xsl:value-of select="@tkName"/><xsl:text>;</xsl:text>
<xsl:if test="string-length(@tkName)&lt;3"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;4"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;5"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;6"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;7"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;8"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;9"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;10"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;11"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;12"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;13"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;14"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;15"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;16"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;17"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;18"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;19"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;20"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;21"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;22"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;23"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;24"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;25"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;26"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;27"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;28"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;29"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;30"><xsl:text> </xsl:text></xsl:if>
<xsl:if test="string-length(@tkName)&lt;31"><xsl:text> </xsl:text></xsl:if>
<xsl:value-of select="@id"/></xsl:template>
</xsl:stylesheet>
