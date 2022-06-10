/*
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
 */
/**
 * Payload_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class Payload_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype tlvToAsciiResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForInitializedCableModemResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForInitializedCableModemResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredCableModemResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredCableModemResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringV4Response;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showPacketsAsAsciiResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showV4PacketsAsAsciiResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringV4Response;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForSipMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForSipMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForNcsMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForNcsMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForIsdnMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForIsdnMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUninitializedMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUninitializedMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredMtaResponse;

    private com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredMtaResponse;

    public Payload_ctype() {
    }

    public Payload_ctype(
           com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype tlvToAsciiResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForInitializedCableModemResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForInitializedCableModemResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredCableModemResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredCableModemResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringV4Response,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showPacketsAsAsciiResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showV4PacketsAsAsciiResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringV4Response,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForSipMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForSipMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForNcsMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForNcsMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForIsdnMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForIsdnMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUninitializedMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUninitializedMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredMtaResponse,
           com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredMtaResponse) {
           this.tlvToAsciiResponse = tlvToAsciiResponse;
           this.generateAsciiFromTemplateForInitializedCableModemResponse = generateAsciiFromTemplateForInitializedCableModemResponse;
           this.generateTlvFromTemplateForInitializedCableModemResponse = generateTlvFromTemplateForInitializedCableModemResponse;
           this.generateAsciiFromTemplateForUnregisteredCableModemResponse = generateAsciiFromTemplateForUnregisteredCableModemResponse;
           this.generateTlvFromTemplateForUnregisteredCableModemResponse = generateTlvFromTemplateForUnregisteredCableModemResponse;
           this.generateAsciiFromStringResponse = generateAsciiFromStringResponse;
           this.generateAsciiFromStringV4Response = generateAsciiFromStringV4Response;
           this.showPacketsAsAsciiResponse = showPacketsAsAsciiResponse;
           this.showV4PacketsAsAsciiResponse = showV4PacketsAsAsciiResponse;
           this.generateTlvFromStringResponse = generateTlvFromStringResponse;
           this.generateTlvFromStringV4Response = generateTlvFromStringV4Response;
           this.generateAsciiFromTemplateForSipMtaResponse = generateAsciiFromTemplateForSipMtaResponse;
           this.generateTlvFromTemplateForSipMtaResponse = generateTlvFromTemplateForSipMtaResponse;
           this.generateAsciiFromTemplateForNcsMtaResponse = generateAsciiFromTemplateForNcsMtaResponse;
           this.generateTlvFromTemplateForNcsMtaResponse = generateTlvFromTemplateForNcsMtaResponse;
           this.generateAsciiFromTemplateForIsdnMtaResponse = generateAsciiFromTemplateForIsdnMtaResponse;
           this.generateTlvFromTemplateForIsdnMtaResponse = generateTlvFromTemplateForIsdnMtaResponse;
           this.generateAsciiFromTemplateForUninitializedMtaResponse = generateAsciiFromTemplateForUninitializedMtaResponse;
           this.generateTlvFromTemplateForUninitializedMtaResponse = generateTlvFromTemplateForUninitializedMtaResponse;
           this.generateAsciiFromTemplateForUnregisteredMtaResponse = generateAsciiFromTemplateForUnregisteredMtaResponse;
           this.generateTlvFromTemplateForUnregisteredMtaResponse = generateTlvFromTemplateForUnregisteredMtaResponse;
    }


    /**
     * Gets the tlvToAsciiResponse value for this Payload_ctype.
     * 
     * @return tlvToAsciiResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype getTlvToAsciiResponse() {
        return tlvToAsciiResponse;
    }


    /**
     * Sets the tlvToAsciiResponse value for this Payload_ctype.
     * 
     * @param tlvToAsciiResponse
     */
    public void setTlvToAsciiResponse(com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype tlvToAsciiResponse) {
        this.tlvToAsciiResponse = tlvToAsciiResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForInitializedCableModemResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForInitializedCableModemResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForInitializedCableModemResponse() {
        return generateAsciiFromTemplateForInitializedCableModemResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForInitializedCableModemResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForInitializedCableModemResponse
     */
    public void setGenerateAsciiFromTemplateForInitializedCableModemResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForInitializedCableModemResponse) {
        this.generateAsciiFromTemplateForInitializedCableModemResponse = generateAsciiFromTemplateForInitializedCableModemResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForInitializedCableModemResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForInitializedCableModemResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForInitializedCableModemResponse() {
        return generateTlvFromTemplateForInitializedCableModemResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForInitializedCableModemResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForInitializedCableModemResponse
     */
    public void setGenerateTlvFromTemplateForInitializedCableModemResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForInitializedCableModemResponse) {
        this.generateTlvFromTemplateForInitializedCableModemResponse = generateTlvFromTemplateForInitializedCableModemResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForUnregisteredCableModemResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForUnregisteredCableModemResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForUnregisteredCableModemResponse() {
        return generateAsciiFromTemplateForUnregisteredCableModemResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForUnregisteredCableModemResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForUnregisteredCableModemResponse
     */
    public void setGenerateAsciiFromTemplateForUnregisteredCableModemResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredCableModemResponse) {
        this.generateAsciiFromTemplateForUnregisteredCableModemResponse = generateAsciiFromTemplateForUnregisteredCableModemResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForUnregisteredCableModemResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForUnregisteredCableModemResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForUnregisteredCableModemResponse() {
        return generateTlvFromTemplateForUnregisteredCableModemResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForUnregisteredCableModemResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForUnregisteredCableModemResponse
     */
    public void setGenerateTlvFromTemplateForUnregisteredCableModemResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredCableModemResponse) {
        this.generateTlvFromTemplateForUnregisteredCableModemResponse = generateTlvFromTemplateForUnregisteredCableModemResponse;
    }


    /**
     * Gets the generateAsciiFromStringResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromStringResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromStringResponse() {
        return generateAsciiFromStringResponse;
    }


    /**
     * Sets the generateAsciiFromStringResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromStringResponse
     */
    public void setGenerateAsciiFromStringResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringResponse) {
        this.generateAsciiFromStringResponse = generateAsciiFromStringResponse;
    }


    /**
     * Gets the generateAsciiFromStringV4Response value for this Payload_ctype.
     * 
     * @return generateAsciiFromStringV4Response
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromStringV4Response() {
        return generateAsciiFromStringV4Response;
    }


    /**
     * Sets the generateAsciiFromStringV4Response value for this Payload_ctype.
     * 
     * @param generateAsciiFromStringV4Response
     */
    public void setGenerateAsciiFromStringV4Response(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromStringV4Response) {
        this.generateAsciiFromStringV4Response = generateAsciiFromStringV4Response;
    }


    /**
     * Gets the showPacketsAsAsciiResponse value for this Payload_ctype.
     * 
     * @return showPacketsAsAsciiResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getShowPacketsAsAsciiResponse() {
        return showPacketsAsAsciiResponse;
    }


    /**
     * Sets the showPacketsAsAsciiResponse value for this Payload_ctype.
     * 
     * @param showPacketsAsAsciiResponse
     */
    public void setShowPacketsAsAsciiResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showPacketsAsAsciiResponse) {
        this.showPacketsAsAsciiResponse = showPacketsAsAsciiResponse;
    }


    /**
     * Gets the showV4PacketsAsAsciiResponse value for this Payload_ctype.
     * 
     * @return showV4PacketsAsAsciiResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getShowV4PacketsAsAsciiResponse() {
        return showV4PacketsAsAsciiResponse;
    }


    /**
     * Sets the showV4PacketsAsAsciiResponse value for this Payload_ctype.
     * 
     * @param showV4PacketsAsAsciiResponse
     */
    public void setShowV4PacketsAsAsciiResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype showV4PacketsAsAsciiResponse) {
        this.showV4PacketsAsAsciiResponse = showV4PacketsAsAsciiResponse;
    }


    /**
     * Gets the generateTlvFromStringResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromStringResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromStringResponse() {
        return generateTlvFromStringResponse;
    }


    /**
     * Sets the generateTlvFromStringResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromStringResponse
     */
    public void setGenerateTlvFromStringResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringResponse) {
        this.generateTlvFromStringResponse = generateTlvFromStringResponse;
    }


    /**
     * Gets the generateTlvFromStringV4Response value for this Payload_ctype.
     * 
     * @return generateTlvFromStringV4Response
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromStringV4Response() {
        return generateTlvFromStringV4Response;
    }


    /**
     * Sets the generateTlvFromStringV4Response value for this Payload_ctype.
     * 
     * @param generateTlvFromStringV4Response
     */
    public void setGenerateTlvFromStringV4Response(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromStringV4Response) {
        this.generateTlvFromStringV4Response = generateTlvFromStringV4Response;
    }


    /**
     * Gets the generateAsciiFromTemplateForSipMtaResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForSipMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForSipMtaResponse() {
        return generateAsciiFromTemplateForSipMtaResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForSipMtaResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForSipMtaResponse
     */
    public void setGenerateAsciiFromTemplateForSipMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForSipMtaResponse) {
        this.generateAsciiFromTemplateForSipMtaResponse = generateAsciiFromTemplateForSipMtaResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForSipMtaResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForSipMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForSipMtaResponse() {
        return generateTlvFromTemplateForSipMtaResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForSipMtaResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForSipMtaResponse
     */
    public void setGenerateTlvFromTemplateForSipMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForSipMtaResponse) {
        this.generateTlvFromTemplateForSipMtaResponse = generateTlvFromTemplateForSipMtaResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForNcsMtaResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForNcsMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForNcsMtaResponse() {
        return generateAsciiFromTemplateForNcsMtaResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForNcsMtaResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForNcsMtaResponse
     */
    public void setGenerateAsciiFromTemplateForNcsMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForNcsMtaResponse) {
        this.generateAsciiFromTemplateForNcsMtaResponse = generateAsciiFromTemplateForNcsMtaResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForNcsMtaResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForNcsMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForNcsMtaResponse() {
        return generateTlvFromTemplateForNcsMtaResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForNcsMtaResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForNcsMtaResponse
     */
    public void setGenerateTlvFromTemplateForNcsMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForNcsMtaResponse) {
        this.generateTlvFromTemplateForNcsMtaResponse = generateTlvFromTemplateForNcsMtaResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForIsdnMtaResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForIsdnMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForIsdnMtaResponse() {
        return generateAsciiFromTemplateForIsdnMtaResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForIsdnMtaResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForIsdnMtaResponse
     */
    public void setGenerateAsciiFromTemplateForIsdnMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForIsdnMtaResponse) {
        this.generateAsciiFromTemplateForIsdnMtaResponse = generateAsciiFromTemplateForIsdnMtaResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForIsdnMtaResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForIsdnMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForIsdnMtaResponse() {
        return generateTlvFromTemplateForIsdnMtaResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForIsdnMtaResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForIsdnMtaResponse
     */
    public void setGenerateTlvFromTemplateForIsdnMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForIsdnMtaResponse) {
        this.generateTlvFromTemplateForIsdnMtaResponse = generateTlvFromTemplateForIsdnMtaResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForUninitializedMtaResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForUninitializedMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForUninitializedMtaResponse() {
        return generateAsciiFromTemplateForUninitializedMtaResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForUninitializedMtaResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForUninitializedMtaResponse
     */
    public void setGenerateAsciiFromTemplateForUninitializedMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUninitializedMtaResponse) {
        this.generateAsciiFromTemplateForUninitializedMtaResponse = generateAsciiFromTemplateForUninitializedMtaResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForUninitializedMtaResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForUninitializedMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForUninitializedMtaResponse() {
        return generateTlvFromTemplateForUninitializedMtaResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForUninitializedMtaResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForUninitializedMtaResponse
     */
    public void setGenerateTlvFromTemplateForUninitializedMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUninitializedMtaResponse) {
        this.generateTlvFromTemplateForUninitializedMtaResponse = generateTlvFromTemplateForUninitializedMtaResponse;
    }


    /**
     * Gets the generateAsciiFromTemplateForUnregisteredMtaResponse value for this Payload_ctype.
     * 
     * @return generateAsciiFromTemplateForUnregisteredMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateAsciiFromTemplateForUnregisteredMtaResponse() {
        return generateAsciiFromTemplateForUnregisteredMtaResponse;
    }


    /**
     * Sets the generateAsciiFromTemplateForUnregisteredMtaResponse value for this Payload_ctype.
     * 
     * @param generateAsciiFromTemplateForUnregisteredMtaResponse
     */
    public void setGenerateAsciiFromTemplateForUnregisteredMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateAsciiFromTemplateForUnregisteredMtaResponse) {
        this.generateAsciiFromTemplateForUnregisteredMtaResponse = generateAsciiFromTemplateForUnregisteredMtaResponse;
    }


    /**
     * Gets the generateTlvFromTemplateForUnregisteredMtaResponse value for this Payload_ctype.
     * 
     * @return generateTlvFromTemplateForUnregisteredMtaResponse
     */
    public com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype getGenerateTlvFromTemplateForUnregisteredMtaResponse() {
        return generateTlvFromTemplateForUnregisteredMtaResponse;
    }


    /**
     * Sets the generateTlvFromTemplateForUnregisteredMtaResponse value for this Payload_ctype.
     * 
     * @param generateTlvFromTemplateForUnregisteredMtaResponse
     */
    public void setGenerateTlvFromTemplateForUnregisteredMtaResponse(com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype generateTlvFromTemplateForUnregisteredMtaResponse) {
        this.generateTlvFromTemplateForUnregisteredMtaResponse = generateTlvFromTemplateForUnregisteredMtaResponse;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Payload_ctype)) return false;
        Payload_ctype other = (Payload_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.tlvToAsciiResponse==null && other.getTlvToAsciiResponse()==null) || 
             (this.tlvToAsciiResponse!=null &&
              this.tlvToAsciiResponse.equals(other.getTlvToAsciiResponse()))) &&
            ((this.generateAsciiFromTemplateForInitializedCableModemResponse==null && other.getGenerateAsciiFromTemplateForInitializedCableModemResponse()==null) || 
             (this.generateAsciiFromTemplateForInitializedCableModemResponse!=null &&
              this.generateAsciiFromTemplateForInitializedCableModemResponse.equals(other.getGenerateAsciiFromTemplateForInitializedCableModemResponse()))) &&
            ((this.generateTlvFromTemplateForInitializedCableModemResponse==null && other.getGenerateTlvFromTemplateForInitializedCableModemResponse()==null) || 
             (this.generateTlvFromTemplateForInitializedCableModemResponse!=null &&
              this.generateTlvFromTemplateForInitializedCableModemResponse.equals(other.getGenerateTlvFromTemplateForInitializedCableModemResponse()))) &&
            ((this.generateAsciiFromTemplateForUnregisteredCableModemResponse==null && other.getGenerateAsciiFromTemplateForUnregisteredCableModemResponse()==null) || 
             (this.generateAsciiFromTemplateForUnregisteredCableModemResponse!=null &&
              this.generateAsciiFromTemplateForUnregisteredCableModemResponse.equals(other.getGenerateAsciiFromTemplateForUnregisteredCableModemResponse()))) &&
            ((this.generateTlvFromTemplateForUnregisteredCableModemResponse==null && other.getGenerateTlvFromTemplateForUnregisteredCableModemResponse()==null) || 
             (this.generateTlvFromTemplateForUnregisteredCableModemResponse!=null &&
              this.generateTlvFromTemplateForUnregisteredCableModemResponse.equals(other.getGenerateTlvFromTemplateForUnregisteredCableModemResponse()))) &&
            ((this.generateAsciiFromStringResponse==null && other.getGenerateAsciiFromStringResponse()==null) || 
             (this.generateAsciiFromStringResponse!=null &&
              this.generateAsciiFromStringResponse.equals(other.getGenerateAsciiFromStringResponse()))) &&
            ((this.generateAsciiFromStringV4Response==null && other.getGenerateAsciiFromStringV4Response()==null) || 
             (this.generateAsciiFromStringV4Response!=null &&
              this.generateAsciiFromStringV4Response.equals(other.getGenerateAsciiFromStringV4Response()))) &&
            ((this.showPacketsAsAsciiResponse==null && other.getShowPacketsAsAsciiResponse()==null) || 
             (this.showPacketsAsAsciiResponse!=null &&
              this.showPacketsAsAsciiResponse.equals(other.getShowPacketsAsAsciiResponse()))) &&
            ((this.showV4PacketsAsAsciiResponse==null && other.getShowV4PacketsAsAsciiResponse()==null) || 
             (this.showV4PacketsAsAsciiResponse!=null &&
              this.showV4PacketsAsAsciiResponse.equals(other.getShowV4PacketsAsAsciiResponse()))) &&
            ((this.generateTlvFromStringResponse==null && other.getGenerateTlvFromStringResponse()==null) || 
             (this.generateTlvFromStringResponse!=null &&
              this.generateTlvFromStringResponse.equals(other.getGenerateTlvFromStringResponse()))) &&
            ((this.generateTlvFromStringV4Response==null && other.getGenerateTlvFromStringV4Response()==null) || 
             (this.generateTlvFromStringV4Response!=null &&
              this.generateTlvFromStringV4Response.equals(other.getGenerateTlvFromStringV4Response()))) &&
            ((this.generateAsciiFromTemplateForSipMtaResponse==null && other.getGenerateAsciiFromTemplateForSipMtaResponse()==null) || 
             (this.generateAsciiFromTemplateForSipMtaResponse!=null &&
              this.generateAsciiFromTemplateForSipMtaResponse.equals(other.getGenerateAsciiFromTemplateForSipMtaResponse()))) &&
            ((this.generateTlvFromTemplateForSipMtaResponse==null && other.getGenerateTlvFromTemplateForSipMtaResponse()==null) || 
             (this.generateTlvFromTemplateForSipMtaResponse!=null &&
              this.generateTlvFromTemplateForSipMtaResponse.equals(other.getGenerateTlvFromTemplateForSipMtaResponse()))) &&
            ((this.generateAsciiFromTemplateForNcsMtaResponse==null && other.getGenerateAsciiFromTemplateForNcsMtaResponse()==null) || 
             (this.generateAsciiFromTemplateForNcsMtaResponse!=null &&
              this.generateAsciiFromTemplateForNcsMtaResponse.equals(other.getGenerateAsciiFromTemplateForNcsMtaResponse()))) &&
            ((this.generateTlvFromTemplateForNcsMtaResponse==null && other.getGenerateTlvFromTemplateForNcsMtaResponse()==null) || 
             (this.generateTlvFromTemplateForNcsMtaResponse!=null &&
              this.generateTlvFromTemplateForNcsMtaResponse.equals(other.getGenerateTlvFromTemplateForNcsMtaResponse()))) &&
            ((this.generateAsciiFromTemplateForIsdnMtaResponse==null && other.getGenerateAsciiFromTemplateForIsdnMtaResponse()==null) || 
             (this.generateAsciiFromTemplateForIsdnMtaResponse!=null &&
              this.generateAsciiFromTemplateForIsdnMtaResponse.equals(other.getGenerateAsciiFromTemplateForIsdnMtaResponse()))) &&
            ((this.generateTlvFromTemplateForIsdnMtaResponse==null && other.getGenerateTlvFromTemplateForIsdnMtaResponse()==null) || 
             (this.generateTlvFromTemplateForIsdnMtaResponse!=null &&
              this.generateTlvFromTemplateForIsdnMtaResponse.equals(other.getGenerateTlvFromTemplateForIsdnMtaResponse()))) &&
            ((this.generateAsciiFromTemplateForUninitializedMtaResponse==null && other.getGenerateAsciiFromTemplateForUninitializedMtaResponse()==null) || 
             (this.generateAsciiFromTemplateForUninitializedMtaResponse!=null &&
              this.generateAsciiFromTemplateForUninitializedMtaResponse.equals(other.getGenerateAsciiFromTemplateForUninitializedMtaResponse()))) &&
            ((this.generateTlvFromTemplateForUninitializedMtaResponse==null && other.getGenerateTlvFromTemplateForUninitializedMtaResponse()==null) || 
             (this.generateTlvFromTemplateForUninitializedMtaResponse!=null &&
              this.generateTlvFromTemplateForUninitializedMtaResponse.equals(other.getGenerateTlvFromTemplateForUninitializedMtaResponse()))) &&
            ((this.generateAsciiFromTemplateForUnregisteredMtaResponse==null && other.getGenerateAsciiFromTemplateForUnregisteredMtaResponse()==null) || 
             (this.generateAsciiFromTemplateForUnregisteredMtaResponse!=null &&
              this.generateAsciiFromTemplateForUnregisteredMtaResponse.equals(other.getGenerateAsciiFromTemplateForUnregisteredMtaResponse()))) &&
            ((this.generateTlvFromTemplateForUnregisteredMtaResponse==null && other.getGenerateTlvFromTemplateForUnregisteredMtaResponse()==null) || 
             (this.generateTlvFromTemplateForUnregisteredMtaResponse!=null &&
              this.generateTlvFromTemplateForUnregisteredMtaResponse.equals(other.getGenerateTlvFromTemplateForUnregisteredMtaResponse())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getTlvToAsciiResponse() != null) {
            _hashCode += getTlvToAsciiResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForInitializedCableModemResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForInitializedCableModemResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForInitializedCableModemResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForInitializedCableModemResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForUnregisteredCableModemResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForUnregisteredCableModemResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForUnregisteredCableModemResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForUnregisteredCableModemResponse().hashCode();
        }
        if (getGenerateAsciiFromStringResponse() != null) {
            _hashCode += getGenerateAsciiFromStringResponse().hashCode();
        }
        if (getGenerateAsciiFromStringV4Response() != null) {
            _hashCode += getGenerateAsciiFromStringV4Response().hashCode();
        }
        if (getShowPacketsAsAsciiResponse() != null) {
            _hashCode += getShowPacketsAsAsciiResponse().hashCode();
        }
        if (getShowV4PacketsAsAsciiResponse() != null) {
            _hashCode += getShowV4PacketsAsAsciiResponse().hashCode();
        }
        if (getGenerateTlvFromStringResponse() != null) {
            _hashCode += getGenerateTlvFromStringResponse().hashCode();
        }
        if (getGenerateTlvFromStringV4Response() != null) {
            _hashCode += getGenerateTlvFromStringV4Response().hashCode();
        }
        if (getGenerateAsciiFromTemplateForSipMtaResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForSipMtaResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForSipMtaResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForSipMtaResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForNcsMtaResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForNcsMtaResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForNcsMtaResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForNcsMtaResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForIsdnMtaResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForIsdnMtaResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForIsdnMtaResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForIsdnMtaResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForUninitializedMtaResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForUninitializedMtaResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForUninitializedMtaResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForUninitializedMtaResponse().hashCode();
        }
        if (getGenerateAsciiFromTemplateForUnregisteredMtaResponse() != null) {
            _hashCode += getGenerateAsciiFromTemplateForUnregisteredMtaResponse().hashCode();
        }
        if (getGenerateTlvFromTemplateForUnregisteredMtaResponse() != null) {
            _hashCode += getGenerateTlvFromTemplateForUnregisteredMtaResponse().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Payload_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Payload_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("tlvToAsciiResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForInitializedCableModemResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForInitializedCableModemResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForUnregisteredCableModemResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForUnregisteredCableModemResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromStringResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromStringV4Response");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Response"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("showPacketsAsAsciiResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("showV4PacketsAsAsciiResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromStringResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromStringV4Response");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Response"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForSipMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForSipMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForNcsMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForNcsMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForIsdnMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForIsdnMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForUninitializedMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForUninitializedMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateAsciiFromTemplateForUnregisteredMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("generateTlvFromTemplateForUnregisteredMtaResponse");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaResponse"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
