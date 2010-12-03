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
package org.eclipse.scout.rt.client.ui.form.fields.internal;

import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;

public final class GridDataBuilder {
  private GridDataBuilder() {
  }

  public static GridData createFromHints(IFormField f, int gridColumnCount) {
    GridData data = f.getGridDataHints();
    if (data.w == IFormField.FULL_WIDTH) {
      data.w = gridColumnCount;
    }
    return data;
  }
}