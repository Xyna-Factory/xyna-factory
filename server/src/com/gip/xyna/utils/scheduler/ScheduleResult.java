/*
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
package com.gip.xyna.utils.scheduler;


public interface ScheduleResult {
  
  public enum ScheduleResultType {
    Scheduled(true),
    Remove(true), 
    BreakLoop(false),
    Continue(false),
    Reorder(true),
    Tag(false);
    
    private boolean remove;

    private ScheduleResultType( boolean remove ) {
      this.remove = remove;
    }

    public boolean hasToRemove() {
      return remove;
    }

  }

  public static final ScheduleResult Scheduled = new FixedScheduleResult(ScheduleResultType.Scheduled);
  public static final ScheduleResult Remove = new FixedScheduleResult(ScheduleResultType.Remove);
  public static final ScheduleResult BreakLoop = new FixedScheduleResult(ScheduleResultType.BreakLoop);
  public static final ScheduleResult Continue = new FixedScheduleResult(ScheduleResultType.Continue);
  public static final ScheduleResult Reorder = new FixedScheduleResult(ScheduleResultType.Reorder);
  
  ScheduleResult.ScheduleResultType getType();

  boolean isHide();

  String getTag();
  
  public static class FixedScheduleResult implements ScheduleResult {
    private ScheduleResult.ScheduleResultType type;
    public FixedScheduleResult(ScheduleResult.ScheduleResultType type) {
      this.type = type;
    }
    public ScheduleResult.ScheduleResultType getType() {
      return type;
    }
    public boolean isHide() {
      return false;
    }
    public String getTag() {
      return null;
    }
  }
  
  public static class TagScheduleResult implements ScheduleResult {
    private String tag;
    private boolean hide;
    public TagScheduleResult(String tag, boolean hide) {
      this.tag = tag;
      this.hide = hide;
    }
    public ScheduleResult.ScheduleResultType getType() {
      return ScheduleResultType.Tag;
    }
    public boolean isHide() {
      return hide;
    }
    public String getTag() {
      return tag;
    }
    
  }
}