/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.services.common.bookmark;

import java.io.Serializable;

public class TableColumnState implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  private String m_className;
  private int m_width;
  private int m_sortOrder = -1;
  private boolean m_sortAscending;

  public TableColumnState() {
  }

  protected TableColumnState(TableColumnState state) {
    this.m_className = state.m_className;
    this.m_width = state.m_width;
    this.m_sortOrder = state.m_sortOrder;
    this.m_sortAscending = state.m_sortAscending;
  }

  public String getClassName() {
    return m_className;
  }

  public void setColumnClassName(String name) {
    m_className = name;
  }

  public int getWidth() {
    return m_width;
  }

  public void setWidth(int i) {
    m_width = i;
  }

  public int getSortOrder() {
    return m_sortOrder;
  }

  public void setSortOrder(int i) {
    m_sortOrder = i;
  }

  public boolean isSortAscending() {
    return m_sortAscending;
  }

  public void setSortAscending(boolean b) {
    m_sortAscending = b;
  }

  @Override
  public Object clone() {
    return new TableColumnState(this);
  }

}