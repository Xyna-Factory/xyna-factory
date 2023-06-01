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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.local;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.SynchronousSuccessfullRemoteOrderExecutionResponse;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

@Persistable(primaryKey=StoredResponse.COL_ID, tableName=StoredResponse.TABLE_NAME)
public class StoredResponse extends Storable<StoredResponse> {
  
  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME = "storedresponse";
  public static final String COL_ID = "orderId";
  public static final String COL_FACTORYID = "factoryId";
  public static final String COL_RESPONSE = "response";

  private static final ResultSetReader<? extends StoredResponse> reader = new ResultSetReader<StoredResponse>() {

    @Override
    public StoredResponse read(ResultSet rs) throws SQLException {
      StoredResponse sr = new StoredResponse();
      sr.factoryId = rs.getString(COL_FACTORYID);
      sr.orderId = rs.getLong(COL_ID);
      sr.response = rs.getString(COL_RESPONSE);
      return sr;
    }
    
  };
  
  @Column(name=COL_ID)
  private Long orderId;
  
  @Column(name=COL_RESPONSE, size=Integer.MAX_VALUE)
  private String response;
  
  @Column(name=COL_FACTORYID)
  private String factoryId;


  public StoredResponse(Long orderId, String factoryId, String response) {
    this.orderId = orderId;
    this.factoryId = factoryId;
    this.response = response;
  }

  public StoredResponse() {
  }

  @Override
  public ResultSetReader<? extends StoredResponse> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return orderId;
  }

  @Override
  public <U extends StoredResponse> void setAllFieldsFromData(U data) {
    StoredResponse s = (StoredResponse) data;
    factoryId = s.factoryId;
    response = s.response;
    orderId = s.orderId;
  }

  
  public Long getOrderId() {
    return orderId;
  }

  
  public String getResponse() {
    return response;
  }

  
  public String getFactoryId() {
    return factoryId;
  }
  
  private static final String XML_RESPONSE = "Resp";
  private static final String XML_ID = "Id";
  private static final String XML_TYPE = "Type";


  public OrderExecutionResponse deserializeResponse() {
    Document doc;
    try {
      doc = XMLUtils.parseString(response);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }

    Element root = doc.getDocumentElement();
    Long orderId = Long.valueOf(root.getAttribute(XML_ID));
    String type = root.getAttribute(XML_TYPE);

    if (type.equals("E")) {
      ErroneousOrderExecutionResponse ret = new ErroneousOrderExecutionResponse(SerializableExceptionInformation
          .fromXml(XMLUtils.getChildElementByName(root, SerializableExceptionInformation.XML_ROOT)));
      ret.setOrderId(orderId);
      return ret;
    } else if (type.equals("S")) {
      String xml = XMLUtils.getXMLString(XMLUtils.getChildElements(root).get(0), false);
      return new SynchronousSuccesfullOrderExecutionResponse(xml, orderId);
    } else {
      throw new RuntimeException(type);
    }
  }


  public static String serializeResponse(OrderExecutionResponse response) {
    XmlBuilder xb = new XmlBuilder();
    xb.startElementWithAttributes(XML_RESPONSE);
    xb.addAttribute(XML_ID, "" + response.getOrderId());
    if (response instanceof ErroneousOrderExecutionResponse) {
      xb.addAttribute(XML_TYPE, "E");
      xb.endAttributes();
      ErroneousOrderExecutionResponse eresp = (ErroneousOrderExecutionResponse) response;
      String xml = eresp.getExceptionInformation().toXml();
      xb.append(xml);
    } else if (response instanceof SynchronousSuccesfullOrderExecutionResponse) {
      xb.addAttribute(XML_TYPE, "S");
      xb.endAttributes();
      SynchronousSuccesfullOrderExecutionResponse sresp = (SynchronousSuccesfullOrderExecutionResponse) response;
      String xml = sresp.getPayloadXML(); //in RemoteOrderResponseListener wird xml immer erzeugt
      xb.append(xml);
    } else if (response instanceof SynchronousSuccessfullRemoteOrderExecutionResponse) {
      xb.addAttribute(XML_TYPE, "S");
      xb.endAttributes();
      SynchronousSuccessfullRemoteOrderExecutionResponse sresp = (SynchronousSuccessfullRemoteOrderExecutionResponse) response;
      GeneralXynaObject obj = XynaFactory.getInstance().getProcessing().getXmomSerialization()
          .deserialize(sresp.getCreatedInRevision(), sresp.getFqn(), sresp.getPayload());
      String xml = obj == null ? null : obj.toXml();
      xb.append(xml);
    } else {
      throw new RuntimeException("Unsupported response type: " + response.getClass().getName());
    }
    xb.endElement(XML_RESPONSE);
    return xb.toString();
  }

}
