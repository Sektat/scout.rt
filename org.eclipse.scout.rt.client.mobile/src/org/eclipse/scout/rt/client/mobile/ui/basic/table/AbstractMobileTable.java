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
package org.eclipse.scout.rt.client.mobile.ui.basic.table;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ClientJob;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.columns.IRowSummaryColumn;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.form.TableRowForm;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.ITableUIFacade;
import org.eclipse.scout.rt.client.ui.form.IForm;

/**
 * @since 3.9.0
 */
public abstract class AbstractMobileTable extends AbstractTable implements IMobileTable {
  private DrillDownStyleMap m_drillDownStyleMap;
  private int m_tableRowFormDisplayHint;
  private String m_tableRowFormDisplayViewId;

  public AbstractMobileTable() {
    this(true);
  }

  public AbstractMobileTable(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected void initConfig() {
    super.initConfig();

    m_drillDownStyleMap = new DrillDownStyleMap();
    setPagingEnabled(getConfiguredPagingEnabled());
    setPageSize(getConfiguredPageSize());
    setAutoCreateTableRowForm(execIsAutoCreateTableRowForm());
    setDefaultDrillDownStyle(execComputeDefaultDrillDownStyle());
  }

  @Override
  public boolean isPagingEnabled() {
    return propertySupport.getPropertyBool(PROP_PAGING_ENABLED);
  }

  @Override
  public void setPagingEnabled(boolean enabled) {
    propertySupport.setPropertyBool(PROP_PAGING_ENABLED, enabled);
  }

  @Override
  public int getPageSize() {
    return propertySupport.getPropertyInt(PROP_PAGE_SIZE);
  }

  @Override
  public void setPageSize(int pageSize) {
    propertySupport.setPropertyInt(PROP_PAGE_SIZE, pageSize);
  }

  @Override
  public int getPageIndex() {
    return propertySupport.getPropertyInt(PROP_PAGE_INDEX);
  }

  @Override
  public void setPageIndex(int index) {
    propertySupport.setPropertyInt(PROP_PAGE_INDEX, index);
  }

  @Override
  public int getPageCount() {
    if (getRowCount() == 0) {
      return 1;
    }
    return new Double(Math.ceil((double) getRowCount() / (double) getPageSize())).intValue();
  }

  @Override
  public boolean isAutoCreateTableRowForm() {
    return propertySupport.getPropertyBool(PROP_AUTO_CREATE_TABLE_ROW_FORM);
  }

  @Override
  public void setAutoCreateTableRowForm(boolean autoCreateTableRowForm) {
    propertySupport.setPropertyBool(PROP_AUTO_CREATE_TABLE_ROW_FORM, autoCreateTableRowForm);
  }

  @Override
  public String getDefaultDrillDownStyle() {
    return propertySupport.getPropertyString(PROP_DEFAULT_DRILL_DOWN_STYLE);
  }

  @Override
  public void setDefaultDrillDownStyle(String defaultDrillDownStyle) {
    propertySupport.setPropertyString(PROP_DEFAULT_DRILL_DOWN_STYLE, defaultDrillDownStyle);
  }

  public void putDrillDownStyle(ITableRow row, String drillDownStyle) {
    m_drillDownStyleMap.put(row, drillDownStyle);
  }

  public String getDrillDownStyle(ITableRow row) {
    return m_drillDownStyleMap.get(row);
  }

  public int getTableRowFormDisplayHint() {
    return m_tableRowFormDisplayHint;
  }

  public void setTableRowFormDisplayHint(int tableRowFormDisplayHint) {
    m_tableRowFormDisplayHint = tableRowFormDisplayHint;
  }

  public String getTableRowFormDisplayViewId() {
    return m_tableRowFormDisplayViewId;
  }

  public void setTableRowFormDisplayViewId(String tableRowFormDisplayViewId) {
    m_tableRowFormDisplayViewId = tableRowFormDisplayViewId;
  }

  @Override
  public void setDrillDownStyleMap(DrillDownStyleMap drillDownStyleMap) {
    m_drillDownStyleMap = drillDownStyleMap;
    if (m_drillDownStyleMap == null) {
      m_drillDownStyleMap = new DrillDownStyleMap();
    }
  }

  @Override
  public DrillDownStyleMap getDrillDownStyleMap() {
    return m_drillDownStyleMap;
  }

  public static void setAutoCreateRowForm(ITable table, Boolean autoCreateRowForm) {
    table.setProperty(IMobileTable.PROP_AUTO_CREATE_TABLE_ROW_FORM, autoCreateRowForm);
  }

  public static Boolean isAutoCreateRowForm(ITable table) {
    return (Boolean) table.getProperty(IMobileTable.PROP_AUTO_CREATE_TABLE_ROW_FORM);
  }

  public static void setDrillDownStyleMap(ITable table, DrillDownStyleMap drillDownStyles) {
    table.setProperty(IMobileTable.PROP_DRILL_DOWN_STYLE_MAP, drillDownStyles);
  }

  public static DrillDownStyleMap getDrillDownStyleMap(ITable table) {
    return (DrillDownStyleMap) table.getProperty(IMobileTable.PROP_DRILL_DOWN_STYLE_MAP);
  }

  public static void setDefaultDrillDownStyle(ITable table, String defaultDrillDownStyle) {
    table.setProperty(IMobileTable.PROP_DEFAULT_DRILL_DOWN_STYLE, defaultDrillDownStyle);
  }

  public static String getDefaultDrillDownStyle(ITable table) {
    return (String) table.getProperty(IMobileTable.PROP_DEFAULT_DRILL_DOWN_STYLE);
  }

  public static Boolean isPagingEnabled(ITable table) {
    return (Boolean) table.getProperty(IMobileTable.PROP_PAGING_ENABLED);
  }

  public static void setPagingEnabled(ITable table, Boolean enabled) {
    table.setProperty(IMobileTable.PROP_PAGING_ENABLED, enabled);
  }

  public static Integer getPageSize(ITable table) {
    return (Integer) table.getProperty(IMobileTable.PROP_PAGE_SIZE);
  }

  public static void setPageSize(ITable table, int pageSize) {
    table.setProperty(IMobileTable.PROP_PAGE_SIZE, pageSize);
  }

  public static Integer getPageIndex(ITable table) {
    return (Integer) table.getProperty(IMobileTable.PROP_PAGE_INDEX);
  }

  public static void setPageIndex(ITable table, int index) {
    table.setProperty(IMobileTable.PROP_PAGE_INDEX, index);
  }

  protected boolean getConfiguredPagingEnabled() {
    return true;
  }

  protected int getConfiguredPageSize() {
    return 50;
  }

  protected boolean execIsAutoCreateTableRowForm() {
    if (isCheckable()) {
      return false;
    }

    return true;
  }

  protected String execComputeDefaultDrillDownStyle() {
    if (isCheckable()) {
      return IRowSummaryColumn.DRILL_DOWN_STYLE_NONE;
    }

    return IRowSummaryColumn.DRILL_DOWN_STYLE_ICON;
  }

  protected void startTableRowForm(ITableRow row) throws ProcessingException {
    TableRowForm form = new TableRowForm(row);
    form.setDisplayHint(getTableRowFormDisplayHint());
    form.setDisplayViewId(getTableRowFormDisplayViewId());
    form.setModal(IForm.DISPLAY_HINT_DIALOG == form.getDisplayHint());
    form.start();
    if (IRowSummaryColumn.DRILL_DOWN_STYLE_ICON.equals(getDrillDownStyle(row))) {
      form.addFormListener(new ClearTableSelectionFormCloseListener(this));
    }
  }

  protected void clearSelectionDelayed() {
    ClientSyncJob job = new ClientSyncJob("Clearing selection", ClientJob.getCurrentSession()) {

      @Override
      protected void runVoid(IProgressMonitor monitor) throws Throwable {
        clearSelection();
      }

    };
    job.schedule();
  }

  protected void clearSelection() {
    selectRow(null);
  }

  @Override
  protected ITableUIFacade createUIFacade() {
    return new P_MobileTableUIFacade();
  }

  @Override
  public IMobileTableUiFacade getUIFacade() {
    return (IMobileTableUiFacade) super.getUIFacade();
  }

  protected class P_MobileTableUIFacade extends P_TableUIFacade implements IMobileTableUiFacade {

    @Override
    public void setPageIndexFromUi(int pageIndex) {
      setPageIndex(pageIndex);
    }

  }

}
