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

<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
<xsl:variable name="lettersnumbers">abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890</xsl:variable>
<xsl:variable name="spaces">                                                              </xsl:variable>
<xsl:variable name="primaryKey"></xsl:variable>

<xsl:template match="DBObject">

<xsl:text>/*
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
 */
package gip.tk;

import gip.base.common.*;
import gip.base.db.OBContext;


import gip.tk.gen.</xsl:text><xsl:value-of select="@aliasName"/><xsl:text>Gen;


</xsl:text>
<xsl:text>/** 
 * </xsl:text><xsl:value-of select="@descr"/> <xsl:text> 
 */ </xsl:text>
<xsl:text>
public class </xsl:text><xsl:value-of select="@aliasName"/><xsl:text> extends </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>Gen {


  /** 
   * Konstruktor
   */
  public </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>() {
    super();
  }


  /** 
   * Fuehrt static ein insert oder update anhand der aktuell gesetzten Klasse durch.
   * Holt vorher abhaengige Objekte anhand eindeutiger Objekt-Merkmale aus der DB
   * und setzt daraufhin die Foreign-Keys.
   * @param context Durchzureichendes Context-Objekt
</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="not(substring(@descr,1,11)='PRIMARY KEY') and not(@name='remark') and not(@name='lockRow') and not(@name='inDate') and not(@name='changeDate')">
<xsl:text>   * @param </xsl:text><xsl:value-of select="@name"/><xsl:text> </xsl:text><xsl:value-of select="@descr"/>
<xsl:text>
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>   * @param pk PrimaryKey fuer den Update-Fall
   * @param update true fuer update, false fuer insert
   * @return gespeichertes </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>-Objekt
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@aliasName"/><xsl:text> store(OBContext context,
</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="not(substring(@descr,1,11)='PRIMARY KEY') and not(@name='remark') and not(@name='lockRow') and not(@name='inDate') and not(@name='changeDate')">
<xsl:text>                       </xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> </xsl:text>
                                            <xsl:value-of select="@name"/><xsl:text>, 
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>                       long pk,
                       boolean update) throws OBException {

    </xsl:text><xsl:value-of select="@aliasName"/><xsl:text> back = new </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>();

    if (update) {
      back = </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>.find(context,pk);
    }

    // Eigene Werte belegen
</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="not(substring(@descr,1,11)='PRIMARY KEY') and not(@name='remark') and not(@name='lockRow') and not(@name='inDate') and not(@name='changeDate')">
<xsl:text>    back.set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text>
<xsl:value-of select="@name"/><xsl:text>);
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>
    // FKs belegen

    // Sonstiges Vorher zu Erledigendes

    back.set(context,update);

    // Nachher zu Erledigendes

    return back;
  }

  /** 
   * Ueberprueft alle Geschaeftsregeln und damit die Konsistenz der DB
   * 'vor' dem Aufruf der set()-Methode.
   * @param context Durchzureichendes Kontext-Objekt
   * @param update update or insert
   * @throws OBException Im Fehlerfall
   */
  public void validateBeforeSet(OBContext context, boolean update) throws OBException {

  }


  /** 
   * Ueberprueft alle Geschaeftsregeln des Objektes und damit die Konsistenz in der DB.
   * @param context Durchzureichendes Kontext-Objekt
   * @throws OBException Im Fehlerfall
   */
  public void validate(OBContext context) throws OBException {

  }


  /** 
   * Ueberprueft alle Geschaeftsregeln des Objektes und damit die Konsistenz in der DB
   * zum Commit-Zeitpunkt oder bei explizitem Aufruf von OBDatabase.validateDeferrable(OBDecision)
   * Nach Moeglichkeit sollen alle Validierungen in validate(OBDecision), da dies die Fehlersuche erleichtert.
   * @param context Durchzureichendes Kontext-Objekt
   * @throws OBException Im Fehlerfall
   */
  public void validateDeferrable(OBContext context) throws OBException {
    
  }


  /** 
   * Loescht den Datensatz in der Datenbank.
   * @param context Durchzureichendes Kontext-Objekt
   * @throws OBException Im Fehlerfall
   */
  public void delete(OBContext context) throws OBException {
    // Ausschlusskriterien fuers Loeschen -> OBException 

    // Loesche alle abhaengigen Objekte

    // Aktualisiere abhaengige Attribute

    super.delete(context);
  }
  
</xsl:text>
  
<!-- ********************************** Objekt ueber UNIQUE-Constraint *************** -->

<xsl:for-each select="Constraint">
<xsl:if test="@type='U'">
<xsl:if test="string-length(@columnList)=0">
<xsl:text>  /** </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> ueber UNIQUE-Constraint von </xsl:text>
<xsl:for-each select="Column">
<xsl:value-of select="@name"/> <xsl:if test="not(position()=last())">, </xsl:if>
</xsl:for-each>
<xsl:text> */
  public static </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> getUnique</xsl:text><xsl:value-of select="../@aliasName"/><xsl:if test="not(string-length(@methodSuffix)=0)"><xsl:value-of select="@methodSuffix"/></xsl:if>
<xsl:text>(Context context, </xsl:text>
<xsl:for-each select="Column">
<xsl:value-of select="@javaType"/><xsl:text> </xsl:text><xsl:value-of select="@name"/> <xsl:if test="not(position()=last())">, </xsl:if></xsl:for-each>
<xsl:text> ) throws Exception {
    </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> searchFilter = new </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text>();
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>    searchFilter.set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text><xsl:value-of select="@name"/><xsl:text>);
</xsl:text>
</xsl:for-each>
<xsl:text>
    return </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text>.find(context,searchFilter);    
  }

</xsl:text>
</xsl:if>
</xsl:if>
</xsl:for-each>

<xsl:text>
}



</xsl:text>
</xsl:template>
</xsl:stylesheet>
