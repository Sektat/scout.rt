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
package org.eclipse.scout.rt.client.ui.form.fields.datefield;

import java.util.Date;

import org.eclipse.scout.commons.UTCDate;
import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.commons.annotations.FormData.SdkCommand;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractUTCFieldData;

/**
 * Date field with override on {@link #getHolderType()} to {@link UTCDate}
 */
@FormData(value = AbstractUTCFieldData.class, sdkCommand = SdkCommand.USE)
public abstract class AbstractUTCDateField extends AbstractDateField implements IUTCDateField {

  public AbstractUTCDateField() {
    this(true);
  }

  public AbstractUTCDateField(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  public Class<Date> getHolderType() {
    return super.getHolderType();
  }

}
