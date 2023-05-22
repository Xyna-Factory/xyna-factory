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
 
package gip.core.demultiplexing.demultiplexinggen;

import org.apache.log4j.Logger;

import gip.base.common.*;
import gip.base.db.OBContext;
import gip.base.db.OBDBObject;
import gip.base.db.demultiplexing.*;

import gip.common.dto.*;
import gip.core.demultiplexing.*;
</xsl:text>
<xsl:if test="(//Capability) and not (contains(@realTkName, 'IM_'))">
  <xsl:text>import </xsl:text><xsl:value-of select="@package"/><xsl:text>.*;
</xsl:text>
</xsl:if>
<xsl:for-each select="Import">
import <xsl:value-of select="@class"/><xsl:text>;
</xsl:text>
</xsl:for-each>
<xsl:text>
public class </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DMGen extends OBDM {

  private static Logger testLogger = Logger.getLogger("testdebug");

</xsl:text> 

<!-- ************************** Allgemeine Capabilities  ******************************* -->

<xsl:text>
  /* -------------------------- find-Methoden ------------------------------------------ */

  /** Sucht den Datensatz anhand des Primaerschluessels
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht der PK
   * @return Das gefundene Objekt
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO findPK(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO retVal = (</xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO) OBDBObject.find(context, new </xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO(), filter.getPrimaryKey(), filter.getHint());            

    retVal.setUsedFor(filter.getUsedFor());
    retVal = </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAdditional(context,retVal);
    return retVal;
  }

  /** Sucht den Datensatz anhand eines Filterobjektes
   * @param context Durchzureichendes Context-Object
   * @param filter das Filterobjekt
   * @return Das gefundene Objekt
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO findFilter(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO retVal = (</xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO) OBDBObject.find(context, new </xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO(), filter);            

    retVal.setUsedFor(filter.getUsedFor());
    retVal = </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAdditional(context,retVal);
    return retVal;
  }

  /** Sucht den Datensatz anhand einer SQL-Bedingung
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die SQL-Bedingung
   * @return Das gefundene Objekt
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO findWC(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO retVal = (</xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO) OBDBObject.find(context, new </xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO(), filter.getWhereClause(), filter.getHint());            

    retVal.setUsedFor(filter.getUsedFor());
    retVal = </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAdditional(context,retVal);
    return retVal;
  }

  /* -------------------------- findAll-Methoden --------------------------------------- */

  /** Sucht alle passenden Datensaetze anhand der Primaerschluesselliste
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die PK-Liste
   * @return Die gefundenen Objekte
   * @throws OBException Wird durchgereicht
   */
  public static OBListDTO&lt;</xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO&gt; findAllFilter(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.findAllFilter(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }

  /** Sucht alle passenden Datensaetze anhand eines Beispielobjektes
   * @param context Durchzureichendes Context-Object
   * @param filter das Beispielobjekt
   * @return Die gefundenen Objekte
   * @throws OBException Wird durchgereicht
   */
  public static OBListDTO&lt;</xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO&gt; findAllPKs(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.findAllPKs(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }

  /** Sucht alle passenden Datensaetze anhand einer SQL-Bedingung
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die SQL-Bedingung
   * @return Die gefundenen Objekte
   * @throws OBException Wird durchgereicht
   */
  public static OBListDTO&lt;</xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO&gt; findAllWC(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.findAllWC(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }

  /* -------------------------- count-Methoden ----------------------------------------- */

  /** Sucht die Anzahl der passenden Datensaetze anhand eines Beispielobjektes
   * @param context Durchzureichendes Context-Object
   * @param filter das Beispielobjekt
   * @return Die gefundene Anzahl
   * @throws OBException Wird durchgereicht
   */
  public static LongDTO countFilter(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.countFilter(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }

  /** Sucht die Anzahl der passenden Datensaetze anhand einer PK-Liste
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die PK-Liste
   * @return Die gefundene Anzahl
   * @throws OBException Wird durchgereicht
   */
  public static LongDTO countPKs(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.countPKs(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }

  /** Sucht die Anzahl der passenden Datensaetze anhand einer SQL-Bedingung
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die SQL-Bedingung
   * @return Die gefundene Anzahl
   * @throws OBException Wird durchgereicht
   */
  public static LongDTO countWC(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO filter) throws OBException {
    return OBDM.countWC(context,new </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO(),filter);
  }
</xsl:text>
<xsl:for-each select="Capability">

<xsl:if test="@type='DELETE'">
  /* -------------------------- delete-Methoden ---------------------------------------- */
  <xsl:variable name="_tk">
    <xsl:if test="not(string-length(../@realTkName)=0)"><xsl:value-of select="../@realTkName"/></xsl:if>
    <xsl:if test="string-length(../@realTkName)=0"><xsl:value-of select="../@tkName"/></xsl:if>
  </xsl:variable>
   <xsl:text> 
  /** Loescht das Objekt, dessen PK im filter steckt
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht der PK
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO delete(OBContext context, </xsl:text>
    <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="$_tk"/><xsl:text> toDelete = </xsl:text>
               <xsl:value-of select="$_tk"/><xsl:text>.find(context, filter.getPrimaryKey(), filter.getHint());
    toDelete.delete(context);
    return new VoidDTO();
  }

  /** Loescht alle Objekte, die dem Beispielobjekt ensprechen
   * @param context Durchzureichendes Context-Object
   * @param filter Das BeispielObjekt
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO findAndDeleteFilter(OBContext context, </xsl:text>
                <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    OBListDTO&lt;</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO&gt; toDelete = </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DM.findAllFilter(context, new </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DTO(), filter);

    for (int i=0; i&lt;toDelete.size(); i++) { 
      </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.delete(context,(</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO) toDelete.elementAt(i));
    }
    
    return new VoidDTO();
  }

  /** Loescht alle Objekte, die einen der uebergebenen PKs haben
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die PK-Liste
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO findAndDeletePKs(OBContext context, </xsl:text>
                <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    OBListDTO&lt;</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO&gt; toDelete = </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DM.findAllPKs(context, new </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DTO(), filter);

    for (int i=0; i&lt;toDelete.size(); i++) { 
      </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.delete(context,(</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO) toDelete.elementAt(i));
    }
    
    return new VoidDTO();
  }

  /** Loescht alle Objekte, die der SQL-Bedingung ensprechen
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht die SQL-Bedingung
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO findAndDeleteWC(OBContext context, </xsl:text>
                <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    OBListDTO&lt;</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO&gt; toDelete = </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DM.findAllWC(context, new </xsl:text>
               <xsl:value-of select="../@tkName"/><xsl:text>DTO(), filter);

    for (int i=0; i&lt;toDelete.size(); i++) { 
      </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.delete(context,(</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO) toDelete.elementAt(i));
    }
    
    return new VoidDTO();
  }
  
  /** Sperrt das Objekt durch setzen der lockrow mit der entsprechenden staffid
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht der PK
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO setLock(OBContext context, </xsl:text>
    <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="$_tk"/><xsl:text> toLock = </xsl:text>
               <xsl:value-of select="$_tk"/><xsl:text>.find(context, filter.getPrimaryKey(), filter.getHint());
    toLock.setLock(context, toLock.getPrimaryKey(), "", context.getStaffId());
    return new VoidDTO();
  }
  
  /** Liefert die staffId des gesperrten Objektes oder -1 wenn das Objekt nicht gesperrt ist
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht der PK
   * @return LongDTO mit der staffId
   * @throws OBException Wird durchgereicht
   */
  public static LongDTO getLock(OBContext context, </xsl:text>
    <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="$_tk"/><xsl:text> toFind = </xsl:text>
               <xsl:value-of select="$_tk"/><xsl:text>.find(context, filter.getPrimaryKey(), filter.getHint());
    return LongDM.convertTkToDTO(context,toFind.getLock(context, toFind.getPrimaryKey()),filter.getUsedFor());
  }

  /** setzt die Sperre des Objects zurueck durch setzen der lockrow mit -1 
   * @param context Durchzureichendes Context-Object
   * @param filter Darin steht der PK
   * @return Ein leeres VoidDTO
   * @throws OBException Wird durchgereicht
   */
  public static VoidDTO resetLock(OBContext context, </xsl:text>
    <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="$_tk"/><xsl:text> toLock = </xsl:text>
               <xsl:value-of select="$_tk"/><xsl:text>.find(context, filter.getPrimaryKey(), filter.getHint());
    toLock.setLock(context, toLock.getPrimaryKey(), "", -1);
    return new VoidDTO();
  }
  
 
 </xsl:text></xsl:if>
 <xsl:if test="@type='NOTIFY'">
  /* -------------------------- Notify-Methode ----------------------------------------- */
  <xsl:variable name="_tk">
    <xsl:if test="not(string-length(../@realTkName)=0)"><xsl:value-of select="../@realTkName"/></xsl:if>
    <xsl:if test="string-length(../@realTkName)=0"><xsl:value-of select="../@tkName"/></xsl:if>
  </xsl:variable>
  <xsl:text>
  public static VoidDTO notifyTk(OBContext context, </xsl:text>
                <xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text><xsl:value-of select="$_tk"/><xsl:text> toNotify = new </xsl:text>
                 <xsl:value-of select="$_tk"/><xsl:text>();
    // Wenn PK gesetzt ist, suche konkretes Objekt. Ansonsten wird die notify-Methode 
    // am leeren TK Objekt gerufen
    if (filter.isPrimaryKeyNotNull()) { 
      toNotify = </xsl:text>
                 <xsl:value-of select="$_tk"/><xsl:text>.find(context, filter.getPrimaryKey(), filter.getHint());
    }
    toNotify.notifyTk(context, filter.getNotification(), filter.getNotifyParam());
    return new VoidDTO();
  }    
</xsl:text></xsl:if>
</xsl:for-each>
<xsl:text>
  /* -------------------------- Konvertierungs-Methode --------------------------------- */

  /** Wandelt ein TK-Objekt in ein DTO-Objekt um
   * @param context Durchzureichendes Context-Object
   * @param dto Das TK-Quellobjekt
   * @param usedFor Wozu soll das Objekt benutzt werden. Danach richtet sich die Fuellmenge
   * @return deas gefuellte DTO
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO convertTkToDTO(OBContext context, </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO dto, long usedFor) throws OBException {
    dto.setUsedFor(usedFor);
    dto = </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAdditional(context,dto);
    return dto;
  }

</xsl:text>
<xsl:text>
  /* -------------------------- Konvertierungs-Methode --------------------------------- */

  /** Wandelt ein TK-Objekt in ein DTO-Objekt um
   * @param context Durchzureichendes Context-Object
   * @param src Das TK-Quellobjekt
   * @param usedFor Wozu soll das Objekt benutzt werden. Danach richtet sich die Fuellmenge
   * @return deas gefuellte DTO
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO convertTkToDTO(OBContext context, OBObject src, long usedFor) throws OBException {
    </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO retVal = (</xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO) OBDBObject.find(context, new </xsl:text>
               <xsl:value-of select="@tkName"/><xsl:text>DTO(), src.getPrimaryKey(), src.getHint());

    return convertTkToDTO(context,retVal,usedFor);
  }

</xsl:text>

  /* -------------------------- TK-Methoden -------------------------------------------- */
<xsl:for-each select="Capability">
<xsl:variable name="_tk">
    <xsl:if test="not(string-length(../@realTkName)=0)"><xsl:value-of select="../@realTkName"/></xsl:if>
    <xsl:if test="string-length(../@realTkName)=0"><xsl:value-of select="../@tkName"/></xsl:if>
</xsl:variable>
<xsl:if test="not(@type='GUI') and not(@type='DELETE') and not(@type='NOTIFY')">
<xsl:text>  /** </xsl:text><xsl:value-of select="@descr"/>
<xsl:if test="@manual='Y'"><xsl:text>
   * ACHTUNG: Diese Methode wird in DM-Klasse ueberschrieben. Hier nur die Checks! </xsl:text>
</xsl:if>  
<xsl:text>
   * @param context Durchzureichendes Context-Objekt
   * @param filter DTO mit Daten und Aktion
   * @return Rueckgabe-DTO (Typ:</xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DTO)
   * @throws OBException Wird durchgereicht
   */
  @SuppressWarnings("static-access")
  public static </xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DTO</xsl:text>
<xsl:if test="not(string-length(@templateType))=0">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if><xsl:text> </xsl:text>
<xsl:value-of select="@javaName"/><xsl:value-of select="@id"/><xsl:text>(OBContext context, </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO filter) throws OBException {
    </xsl:text>

<xsl:text>///* // Check der Parameter </xsl:text>
<xsl:for-each select="Param">
<xsl:text>
    if (OBConstants.checkParams) {
      if (!filter.is</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/><xsl:text>Ignored()) {
        if (filter.is</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/><xsl:text>Null()) {
          throw new OBException(OBException.OBErrorNumber.parameterNotSet, filter.name</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/><xsl:text>);
        }  
      }
    } 
</xsl:text>
</xsl:for-each>
<xsl:text>    //*/ // End of Check  
</xsl:text>
<xsl:text>    // Debug-Output
    String testlog = "\n    </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO testDTO = new </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DTO();\n";
    testlog+="    testDTO.setCapabilityId(testDTO.possibleCapability.</xsl:text><xsl:value-of select="@javaName"/><xsl:value-of select="@id"/><xsl:text>);\n";
</xsl:text>
<xsl:for-each select="Param">
<xsl:text>
    testlog += "    testDTO.set</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/>
<xsl:text>(\"" + filter.get</xsl:text><xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/>
<xsl:text>() + "\");\n";</xsl:text>
</xsl:for-each>


<xsl:text>
    testlog+="    </xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DTO result = (</xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DTO) OBRemoteRef.doOperation(testDTO);\n";
    testLogger.debug(testlog);
    </xsl:text>
  <xsl:if test="not(@manual='Y')">
    <xsl:if test="not(@returnValueType='void')"><xsl:value-of select="@returnValueType"/>
<xsl:if test="not(string-length(@templateType))=0 and not(contains(@returnValueType,'[]'))">&lt;<xsl:value-of select="@templateType"/>&gt;</xsl:if><xsl:text>  retVal = </xsl:text></xsl:if>
    <xsl:value-of select="$_tk"/><xsl:text>.</xsl:text><xsl:value-of select="@javaName"/>
<xsl:text>(context</xsl:text>  
<xsl:for-each select="Param">
<xsl:text>,
      filter.get</xsl:text>
      <xsl:value-of select="translate(substring(@dtoName,1,1),$lcletters,$ucletters)"/><xsl:value-of select="substring(@dtoName,2)"/>
      <xsl:text>()</xsl:text>
</xsl:for-each>
<xsl:text>);</xsl:text>
</xsl:if>
<xsl:choose>
  <xsl:when test="@returnValueType='void'"><xsl:text>
    return new VoidDTO();</xsl:text>
  </xsl:when>  
  <xsl:when test="@manual='Y'"><xsl:text>
    return null;</xsl:text>
  </xsl:when>  
  <xsl:when test="@convert='N'"><xsl:text>
    return retVal;</xsl:text>
  </xsl:when>  
  <xsl:otherwise>
    <xsl:text>
    return </xsl:text><xsl:value-of select="@returnDto"/><xsl:text>DM.convertTkToDTO(context,retVal,filter.getUsedFor());</xsl:text>
  </xsl:otherwise>
</xsl:choose>
<xsl:text>    
  }   

</xsl:text>
</xsl:if>
</xsl:for-each>

<xsl:text>

  /* -------------------------- Event-Handling-Methode --------------------------------- */

  /** Diese Methode dient als Verteiler der Aktionen
   * @param context Durchzureichendes Context-Object
   * @param baseDto Das DTO, das Daten und Verwendungszweck enthaelt
   * @return Das Rueckgabe-Objekt
   * @throws OBException Wird durchgereicht
   */
  public OBDTOInterface doOperation(OBContext context, OBDTO baseDto) throws OBException {
    </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO dto = (</xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO) baseDto;
    if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findPK) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findPK(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findFilter) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findFilter(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findWC) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findWC(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findAllFilter) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAllFilter(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findAllPKs) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAllPKs(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.findAllWC) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.findAllWC(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.countFilter) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.countFilter(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.countPKs) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.countPKs(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="@tkName"/>
      <xsl:text>DTO.PosCap.countWC) {
      return </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DM.countWC(context,dto);
    }
    
</xsl:text>

<xsl:for-each select="Capability">

<xsl:if test="@type='DELETE'"><xsl:text>

    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.delete) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.delete(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.findAndDeleteFilter) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.findAndDeleteFilter(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.findAndDeletePKs) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.findAndDeletePKs(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.findAndDeleteWC) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.findAndDeleteWC(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.setLock) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.setLock(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.getLock) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.getLock(context,dto);
    }
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.resetLock) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.resetLock(context,dto);
    }
</xsl:text></xsl:if>
<xsl:if test="@type='NOTIFY'"><xsl:text>    
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.notifyTk) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.notifyTk(context, dto);
    }
    
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text> 
    // ------------------------------------- Spezielle Capabilities -----------------------
    
</xsl:text>
<xsl:for-each select="Capability">
<xsl:if test="not(@type='GUI') and not(@type='DELETE') and not(@type='NOTIFY')"><xsl:text>
    else if (dto.getCapabilityId()==</xsl:text><xsl:value-of select="../@tkName"/>
      <xsl:text>DTO.PosCap.</xsl:text><xsl:value-of select="@javaName"/><xsl:value-of select="@id"/>
      <xsl:text>) {
      return </xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DM.</xsl:text><xsl:value-of select="@javaName"/><xsl:value-of select="@id"/>
      <xsl:text>(context,dto);
    }
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>
    // ------------------------------------- Freie Capabilities ---------------------------

    else {
      throw new OBException(OBException.OBErrorNumber.WRONG_CAPABILTY, ""+dto.getCapabilityId());
    }
  } 
</xsl:text>

<xsl:text>
}
</xsl:text>
</xsl:template>
</xsl:stylesheet>
