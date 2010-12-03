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
package org.eclipse.scout.rt.client.ui.basic.cell;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.shared.data.basic.FontSpec;

/**
 * A Cell represents model properties of a tree node or table cell.
 * <p>
 * This implementation shares graphical aspects with other cell instances and uses a {@link CellExtension} to store
 * rarely used properties.
 */
public class Cell implements ICell {

  private static final Map<CellStyle, CellStyle> SHARED_STYLE_STORE;
  private static final Object SHARED_STYLE_STORE_LOCK = new Object();
  private static final CellStyle DEFAULT_CELL_STYLE;

  static {
    SHARED_STYLE_STORE = new HashMap<CellStyle, CellStyle>();
    DEFAULT_CELL_STYLE = new CellStyle();
    SHARED_STYLE_STORE.put(DEFAULT_CELL_STYLE, DEFAULT_CELL_STYLE);
  }

  private ICellObserver m_observer;
  private Object m_value;
  private String m_text;
  private ICellSpecialization m_cellSpecialization = DEFAULT_CELL_STYLE;

  public Cell() {
    super();
  }

  public Cell(ICell c) {
    super();
    try {
      updateFrom(c);
    }
    catch (ProcessingException e) {
      //should never happen
      e.printStackTrace();
    }
  }

  public Cell(ICellObserver observer) {
    super();
    setObserver(observer);
  }

  public Cell(ICellObserver observer, ICell c) throws ProcessingException {
    super();
    updateFrom(c);
    setObserver(observer);
  }

  public void updateFrom(ICell c) throws ProcessingException {
    if (c != null) {
      setFont(c.getFont());
      setForegroundColor(c.getForegroundColor());
      setBackgroundColor(c.getBackgroundColor());
      setHorizontalAlignment(c.getHorizontalAlignment());
      setTooltipText(c.getTooltipText());
      setIconId(c.getIconId());
      setText(c.getText());
      setValue(c.getValue());
      setEnabled(c.isEnabled());
      //do not reset observer
    }
  }

  public Object getValue() {
    return m_value;
  }

  /**
   * @return true if the value has in fact changed
   */
  public boolean setValue(Object value) throws ProcessingException {
    if (getObserver() != null) {
      value = getObserver().validateValue(this, value);
    }
    if (CompareUtility.equals(m_value, value)) {
      return false;
    }
    else {
      m_value = value;
      if (getObserver() != null) {
        getObserver().cellChanged(this, VALUE_BIT);
      }
      return true;
    }
  }

  public String getText() {
    return m_text;
  }

  public void setText(String s) {
    if (CompareUtility.notEquals(m_text, s)) {
      m_text = s;
      if (getObserver() != null) {
        getObserver().cellChanged(this, TEXT_BIT);
      }
    }
  }

  public String getIconId() {
    return m_cellSpecialization.getIconId();
  }

  public void setIconId(String s) {
    if (CompareUtility.notEquals(m_cellSpecialization.getIconId(), s)) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setIconId(s);
      setValueInternal(ICON_ID_BIT, newStyle);
    }
  }

  public String getTooltipText() {
    return m_cellSpecialization.getTooltipText();
  }

  public void setTooltipText(String s) {
    if (m_cellSpecialization instanceof CellStyle) {
      if (!StringUtility.isNullOrEmpty(s)) {
        ICellSpecialization newStyle = new CellExtension(m_cellSpecialization);
        newStyle.setTooltipText(s);
        setValueInternal(TOOLTIP_BIT, newStyle);
      }
    }
    else if (CompareUtility.notEquals(m_cellSpecialization.getIconId(), s)) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setTooltipText(s);
      setValueInternal(TOOLTIP_BIT, newStyle);
    }
  }

  public int getHorizontalAlignment() {
    return m_cellSpecialization.getHorizontalAlignment();
  }

  public void setHorizontalAlignment(int a) {
    if (m_cellSpecialization.getHorizontalAlignment() != a) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setHorizontalAlignment(a);
      setValueInternal(H_ALIGN_BIT, newStyle);
    }
  }

  public String getBackgroundColor() {
    return m_cellSpecialization.getBackgroundColor();
  }

  public void setBackgroundColor(String c) {
    if (CompareUtility.notEquals(m_cellSpecialization.getBackgroundColor(), c)) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setBackgroundColor(c);
      setValueInternal(BG_COLOR_BIT, newStyle);
    }
  }

  public String getForegroundColor() {
    return m_cellSpecialization.getForegroundColor();
  }

  public void setForegroundColor(String c) {
    if (CompareUtility.notEquals(m_cellSpecialization.getForegroundColor(), c)) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setForegroundColor(c);
      setValueInternal(FG_COLOR_BIT, newStyle);
    }
  }

  public FontSpec getFont() {
    return m_cellSpecialization.getFont();
  }

  public void setFont(FontSpec f) {
    if (m_cellSpecialization.getFont() == null && f != null
        || m_cellSpecialization.getFont() != null && f == null
        || CompareUtility.notEquals(m_cellSpecialization.getFont(), f)) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setFont(f);
      setValueInternal(FONT_BIT, newStyle);
    }
  }

  public boolean isEnabled() {
    return m_cellSpecialization.isEnabled();
  }

  public void setEnabled(boolean b) {
    if (m_cellSpecialization instanceof CellStyle) {
      if (!b) {
        ICellSpecialization newStyle = new CellExtension(m_cellSpecialization);
        newStyle.setEnabled(b);
        setValueInternal(ENABLED_BIT, newStyle);
      }
    }
    else if (m_cellSpecialization.isEnabled() != b) {
      ICellSpecialization newStyle = m_cellSpecialization.copy();
      newStyle.setEnabled(b);
      setValueInternal(ENABLED_BIT, newStyle);
    }
  }

  public ICellObserver getObserver() {
    return m_observer;
  }

  public void setObserver(ICellObserver observer) {
    m_observer = observer;
  }

  private void setValueInternal(int bitPos, ICellSpecialization cellSpecialization) {
    synchronized (SHARED_STYLE_STORE_LOCK) {
      CellStyle newStyle = cellSpecialization.getCellStyle();
      CellStyle sharedStyle = SHARED_STYLE_STORE.get(newStyle);
      if (sharedStyle == null) {
        SHARED_STYLE_STORE.put(newStyle, newStyle);
        m_cellSpecialization = cellSpecialization.reconcile(newStyle);
      }
      else {
        m_cellSpecialization = cellSpecialization.reconcile(sharedStyle);
      }
    }
    if (getObserver() != null) {
      getObserver().cellChanged(this, bitPos);
    }
  }

  @Override
  public String toString() {
    String s = getText();
    if (s == null) {
      s = "" + getValue();
    }
    return s;
  }
}