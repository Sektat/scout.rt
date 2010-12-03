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
package org.eclipse.scout.rt.client.ui.form.fields.tablefield;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.HashMap;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.NumberUtility;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.xmlparser.SimpleXmlElement;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.action.keystroke.KeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IBigDecimalColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IDoubleColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IIntegerColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.ILongColumn;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.tablefield.AbstractTableFieldData;

public abstract class AbstractTableField<T extends ITable> extends AbstractFormField implements ITableField<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractTableField.class);

  private T m_table;
  private boolean m_tableExternallyManaged;
  private P_ManagedTableListener m_managedTableListener;
  private P_TableStatusListener m_tableStatusListener;

  public AbstractTableField() {
    super();
  }

  /*
   * Configuration
   */
  @ConfigOperation
  @Order(190)
  protected void execReloadTableData() throws ProcessingException {
  }

  /**
   * The default implementation calculates the selected and total row count and the sum of all numeric columns;
   * Then calls {@link #setTableStatus(String)}
   */
  @ConfigOperation
  @Order(195)
  protected void execUpdateTableStatus() {
    if (!isTableStatusVisible()) return;
    //
    StringBuilder statusText = new StringBuilder();
    ITable table = getTable();
    if (table != null) {
      int nTotal = table.getFilteredRowCount();
      if (nTotal == 1) {
        statusText.append(ScoutTexts.get("OneRow"));
      }
      else {
        statusText.append(ScoutTexts.get("XRows", NumberUtility.format(nTotal)));
      }
      int nSel = table.getSelectedRowCount();
      if (nSel == 1) {
        statusText.append(", " + ScoutTexts.get("OneSelected"));
      }
      else if (nSel > 1) {
        statusText.append(", " + ScoutTexts.get("XSelected", NumberUtility.format(nSel)));
        // show sums of numeric columns
        for (IColumn c : table.getColumnSet().getVisibleColumns()) {
          NumberFormat fmt = null;
          Object sum = null;
          if (c instanceof IBigDecimalColumn) {
            fmt = ((IBigDecimalColumn) c).getNumberFormat();
            double d = NumberUtility.sum(TypeCastUtility.castValue(c.getSelectedValues(), double[].class));
            if (d != 0.0) {
              sum = d;
            }
          }
          else if (c instanceof IDoubleColumn) {
            fmt = ((IDoubleColumn) c).getNumberFormat();
            double d = NumberUtility.sum(TypeCastUtility.castValue(c.getSelectedValues(), double[].class));
            if (d != 0.0) {
              sum = d;
            }
          }
          else if (c instanceof ILongColumn) {
            fmt = ((ILongColumn) c).getNumberFormat();
            long d = NumberUtility.sum(TypeCastUtility.castValue(c.getSelectedValues(), long[].class));
            if (d != 0) {
              sum = d;
            }
          }
          else if (c instanceof IIntegerColumn) {
            fmt = ((IIntegerColumn) c).getNumberFormat();
            long d = NumberUtility.sum(TypeCastUtility.castValue(c.getSelectedValues(), long[].class));
            if (d != 0) {
              sum = d;
            }
          }
          //
          if (fmt != null && sum != null) {
            statusText.append(", " + c.getHeaderCell().getText() + ": " + fmt.format(sum));
          }
        }
      }
    }
    if (statusText.length() > 0) {
      setTableStatus(statusText.toString());
    }
    else {
      setTableStatus(null);
    }
  }

  @ConfigOperation
  @Order(200)
  protected void execSave(ITableRow[] insertedRows, ITableRow[] updatedRows, ITableRow[] deletedRows) {
  }

  @ConfigOperation
  @Order(210)
  protected void execSaveDeletedRow(ITableRow row) throws ProcessingException {
  }

  @ConfigOperation
  @Order(220)
  protected void execSaveInsertedRow(ITableRow row) throws ProcessingException {
  }

  @ConfigOperation
  @Order(230)
  protected void execSaveUpdatedRow(ITableRow row) throws ProcessingException {
  }

  @Override
  protected void execChangedMasterValue(Object newMasterValue) throws ProcessingException {
    reloadTableData();
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(200)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredTableStatusVisible() {
    return false;
  }

  private Class<? extends ITable> getConfiguredTable() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class<? extends ITable>[] f = ConfigurationUtility.filterClasses(dca, ITable.class);
    if (f.length == 1) return f[0];
    else {
      for (Class<? extends ITable> c : f) {
        if (c.getDeclaringClass() != AbstractTableField.class) {
          return c;
        }
      }
      return null;
    }
  }

  @ConfigPropertyValue("1")
  @Override
  protected double getConfiguredGridWeightY() {
    return 1;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void initConfig() {
    super.initConfig();
    setTableStatusVisible(getConfiguredTableStatusVisible());
    if (getConfiguredTable() != null) {
      try {
        setTableInternal((T) ConfigurationUtility.newInnerInstance(this, getConfiguredTable()));
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    // local enabled listener
    addPropertyChangeListener(PROP_ENABLED, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (m_table != null) {
          m_table.setEnabled(isEnabled());
        }
      }
    });
  }

  /*
   * Runtime
   */

  @Override
  protected void initFieldInternal() throws ProcessingException {
    if (m_table != null && !m_tableExternallyManaged) {
      m_table.initTable();
    }
    super.initFieldInternal();
  }

  @Override
  protected void disposeFieldInternal() {
    super.disposeFieldInternal();
    if (m_table != null && !m_tableExternallyManaged) {
      m_table.disposeTable();
    }
  }

  public final T getTable() {
    return m_table;
  }

  public void setTable(T newTable, boolean externallyManaged) {
    m_tableExternallyManaged = externallyManaged;
    setTableInternal(newTable);
  }

  private void setTableInternal(T table) {
    if (m_table == table) {
      return;
    }
    if (m_table != null) {
      if (!m_tableExternallyManaged) {
        if (m_managedTableListener != null) {
          m_table.removeTableListener(m_managedTableListener);
          m_managedTableListener = null;
        }
      }
      if (m_tableStatusListener != null) {
        m_table.removeTableListener(m_tableStatusListener);
        m_tableStatusListener = null;
      }
    }
    m_table = table;
    if (m_table != null) {
      if (!m_tableExternallyManaged) {
        // ticket 84893
        // m_table.setAutoDiscardOnDelete(false);
        m_managedTableListener = new P_ManagedTableListener();
        m_table.addTableListener(m_managedTableListener);
      }
      m_tableStatusListener = new P_TableStatusListener();
      m_table.addTableListener(m_tableStatusListener);
      updateTableStatus();
      m_table.setEnabled(isEnabled());
    }
    boolean changed = propertySupport.setProperty(PROP_TABLE, m_table);
    if (changed) {
      if (getForm() != null) {
        getForm().structureChanged(this);
      }
      updateKeyStrokes();
    }
  }

  @Override
  public void exportFormFieldData(AbstractFormFieldData target) throws ProcessingException {
    AbstractTableFieldData tableFieldData = (AbstractTableFieldData) target;
    if (m_table != null) {
      m_table.extractTableData(tableFieldData);
    }
  }

  @Override
  public void importFormFieldData(AbstractFormFieldData source, boolean valueChangeTriggersEnabled) throws ProcessingException {
    AbstractTableFieldData tableFieldData = (AbstractTableFieldData) source;
    if (tableFieldData.isValueSet()) {
      if (m_table != null) {
        try {
          if (!valueChangeTriggersEnabled) setValueChangeTriggerEnabled(false);
          //
          m_table.updateTable(tableFieldData);
        }
        finally {
          if (!valueChangeTriggersEnabled) setValueChangeTriggerEnabled(true);
        }
      }
    }
  }

  @Override
  public void loadXML(SimpleXmlElement x) throws ProcessingException {
    super.loadXML(x);
    if (m_table != null) {
      int[] selectedRowIndices = null;
      try {
        selectedRowIndices = (int[]) x.getObjectAttribute("selectedRowIndices", null);
      }
      catch (Exception e) {
        LOG.warn("reading attribute 'selectedRowIndices'", e);
      }
      Object[][] dataMatrix = null;
      try {
        dataMatrix = (Object[][]) x.getObjectAttribute("rows", null);
      }
      catch (Exception e) {
        LOG.warn("reading attribute 'rows'", e);
      }
      m_table.discardAllRows();
      if (dataMatrix != null && dataMatrix.length > 0) {
        m_table.addRowsByMatrix(dataMatrix);
      }
      if (selectedRowIndices != null && selectedRowIndices.length > 0) {
        m_table.selectRows(m_table.getRows(selectedRowIndices));
      }
    }
  }

  @Override
  public void storeXML(SimpleXmlElement x) throws ProcessingException {
    super.storeXML(x);
    if (m_table != null) {
      ITableRow[] selectedRows = m_table.getSelectedRows();
      int[] selectedRowIndices = new int[selectedRows.length];
      for (int i = 0; i < selectedRowIndices.length; i++) {
        selectedRowIndices[i] = selectedRows[i].getRowIndex();
      }
      try {
        x.setObjectAttribute("selectedRowIndices", selectedRowIndices);
      }
      catch (Exception e) {
        LOG.warn("writing attribute 'selectedRowIndices'", e);
      }
      Object[][] dataMatrix = m_table.getTableData();
      for (int r = 0; r < dataMatrix.length; r++) {
        for (int c = 0; c < dataMatrix[r].length; c++) {
          Object o = dataMatrix[r][c];
          if (o != null && !(o instanceof Serializable)) {
            LOG.warn("ignoring not serializable value at row=" + r + ", col=" + c + ": " + o + "[" + o.getClass() + "]");
            dataMatrix[r][c] = null;
          }
        }
      }
      try {
        x.setObjectAttribute("rows", dataMatrix);
      }
      catch (Exception e) {
        LOG.warn("writing attribute 'rows'", e);
      }
    }
  }

  @Override
  protected boolean execIsSaveNeeded() throws ProcessingException {
    boolean b = false;
    if (m_table != null && !m_tableExternallyManaged) {
      if (b == false && m_table.getDeletedRowCount() > 0) {
        b = true;
      }
      if (b == false && m_table.getInsertedRowCount() > 0) {
        b = true;
      }
      if (b == false && m_table.getUpdatedRowCount() > 0) {
        b = true;
      }
    }
    return b;
  }

  @Override
  protected void execMarkSaved() throws ProcessingException {
    super.execMarkSaved();
    if (m_table != null && !m_tableExternallyManaged) {
      try {
        m_table.setTableChanging(true);
        //
        for (int i = 0; i < m_table.getRowCount(); i++) {
          ITableRow row = m_table.getRow(i);
          if (!row.isStatusNonchanged()) {
            row.setStatusNonchanged();
          }
        }
        m_table.clearDeletedRows();
      }
      finally {
        m_table.setTableChanging(false);
      }
    }
  }

  @Override
  protected boolean execIsEmpty() throws ProcessingException {
    if (m_table != null) {
      return m_table.getRowCount() == 0;
    }
    else {
      return true;
    }
  }

  @Override
  public boolean isContentValid() {
    boolean b = super.isContentValid();
    if (b) {
      if (isMandatory()) {
        if (getTable() == null || getTable().getRowCount() < 1) {
          return false;
        }
      }
    }
    return b;
  }

  public String getTableStatus() {
    return (String) propertySupport.getProperty(PROP_TABLE_STATUS);
  }

  public void setTableStatus(String status) {
    propertySupport.setProperty(PROP_TABLE_STATUS, status);
  }

  public boolean isTableStatusVisible() {
    return propertySupport.getPropertyBool(PROP_TABLE_STATUS_VISIBLE);
  }

  public void setTableStatusVisible(boolean b) {
    propertySupport.setPropertyBool(PROP_TABLE_STATUS_VISIBLE, b);
    if (b) {
      updateTableStatus();
    }
  }

  public void updateTableStatus() {
    try {
      execUpdateTableStatus();
    }
    catch (Throwable t) {
      LOG.warn("Updating status of " + AbstractTableField.this.getClass().getName(), t);
    }
  }

  public void doSave() throws ProcessingException {
    if (m_table != null && !m_tableExternallyManaged) {
      try {
        m_table.setTableChanging(true);
        //
        // 1. batch
        execSave(m_table.getInsertedRows(), m_table.getUpdatedRows(), m_table.getDeletedRows());
        // 2. per row
        ITableRow[] insertedRows = m_table.getInsertedRows();
        ITableRow[] updatedRows = m_table.getUpdatedRows();
        ITableRow[] deletedRows = m_table.getDeletedRows();
        // deleted rows
        for (int i = 0; i < deletedRows.length; i++) {
          execSaveDeletedRow(deletedRows[i]);
        }
        // inserted rows
        for (int i = 0; i < insertedRows.length; i++) {
          ITableRow mr = insertedRows[i];
          execSaveInsertedRow(mr);
          mr.setStatusNonchanged();
          m_table.updateRow(mr);
        }
        // updated rows
        for (int i = 0; i < updatedRows.length; i++) {
          ITableRow mr = updatedRows[i];
          execSaveUpdatedRow(mr);
          mr.setStatusNonchanged();
          m_table.updateRow(mr);
        }
      }
      finally {
        m_table.setTableChanging(false);
      }
    }
    markSaved();
  }

  public void reloadTableData() throws ProcessingException {
    execReloadTableData();
  }

  @Override
  public IKeyStroke[] getContributedKeyStrokes() {
    HashMap<String, IKeyStroke> ksMap = new HashMap<String, IKeyStroke>();
    if (getTable() != null) {
      for (IMenu m : getTable().getMenus()) {
        String s = m.getKeyStroke();
        if (s != null && s.trim().length() > 0) {
          KeyStroke ks = new KeyStroke(s, m);
          ksMap.put(ks.getKeyStroke().toUpperCase(), ks);
        }
      }
    }
    return ksMap.values().toArray(new IKeyStroke[ksMap.size()]);
  }

  private class P_ManagedTableListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      switch (e.getType()) {
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_INSERTED:
        case TableEvent.TYPE_ROWS_UPDATED:
        case TableEvent.TYPE_COLUMN_STRUCTURE_CHANGED: {
          checkSaveNeeded();
          checkEmpty();
          break;
        }
      }
    }
  }

  private class P_TableStatusListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      switch (e.getType()) {
        case TableEvent.TYPE_ROWS_INSERTED:
        case TableEvent.TYPE_ROWS_UPDATED:
        case TableEvent.TYPE_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_SELECTED:
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROW_FILTER_CHANGED: {
          updateTableStatus();
          break;
        }
      }
    }
  }
}