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
package org.eclipse.scout.rt.server.services.common.jdbc.internal.exec;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.parsers.token.IToken;
import org.eclipse.scout.rt.server.services.common.jdbc.style.ISqlStyle;

interface IBindOutput {

  IToken getToken();

  /**
   * @return true if this bind corresponds to a jdbc "?" bind
   */
  boolean isJdbcBind();

  int getJdbcBindIndex();

  void setJdbcBindIndex(int index);

  boolean isBatch();

  boolean isSelectInto();

  Class getBindType();

  void setNextBatchIndex(int i);

  void finishBatch() throws ProcessingException;

  void setReplaceToken(ISqlStyle style);

  void consumeValue(Object value) throws ProcessingException;

}
