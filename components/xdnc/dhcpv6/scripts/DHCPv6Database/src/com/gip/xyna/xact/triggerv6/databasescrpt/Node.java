package com.gip.xyna.xact.triggerv6.databasescrpt;
/*
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
 */
import java.util.ArrayList;
import java.util.List;


public class Node {
  
  private DHCPv6EncodingAdm identity;
  private List<Node> children = new ArrayList<Node>();

  public Node(DHCPv6EncodingAdm e)
  {
    this.identity=e;
  }
  
  public void addChild(Node c)
  {
    this.children.add(c);
  }
  
  public DHCPv6EncodingAdm getIdentity()
  {
    return this.identity;
  }

  public void setIdentity(DHCPv6EncodingAdm i)
  {
    this.identity=i;
  }
  
  public List<Node> getChildren()
  {
    return this.children;
  }

  public void setChildren(List<Node> cl)
  {
   this.children=cl;
  }

}
