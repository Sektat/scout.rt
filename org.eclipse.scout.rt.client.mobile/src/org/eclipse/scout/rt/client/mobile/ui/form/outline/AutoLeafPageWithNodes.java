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
package org.eclipse.scout.rt.client.mobile.ui.form.outline;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.mobile.ui.form.fields.table.autotable.AutoTableForm;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithNodes;

public class AutoLeafPageWithNodes extends AbstractPageWithNodes {
  private ITableRow m_tableRow;

  public AutoLeafPageWithNodes(ITableRow row) {
    if (row == null) {
      throw new IllegalArgumentException("Row must not be null");
    }

    m_tableRow = row;
  }

  @Override
  protected void execInitPage() throws ProcessingException {
    Cell cell = getCellForUpdate();
    if (cell.getText() == null) {
      cell.setText(findAppropriateTitle());
    }
    if (cell.getIconId() == null) {
      cell.setIconId(m_tableRow.getIconId());
    }
  }

  private String findAppropriateTitle() {
    for (IColumn column : m_tableRow.getTable().getColumns()) {
      if (column.isVisible()) {
        return m_tableRow.getTable().getCell(m_tableRow, column).getText();
      }
    }

    return null;
  }

  @Override
  protected boolean getConfiguredTableVisible() {
    return false;
  }

  @Override
  protected boolean getConfiguredLeaf() {
    return true;
  }

  @Override
  protected void execPageActivated() throws ProcessingException {
    if (getDetailForm() == null) {
      AutoTableForm form = new AutoTableForm(m_tableRow);
      setDetailForm(form);
      form.start();
    }
  }
}