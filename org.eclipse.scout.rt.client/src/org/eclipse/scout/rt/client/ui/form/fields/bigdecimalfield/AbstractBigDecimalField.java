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
package org.eclipse.scout.rt.client.ui.form.fields.bigdecimalfield;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.decimalfield.AbstractDecimalField;
import org.eclipse.scout.rt.shared.ScoutTexts;

public abstract class AbstractBigDecimalField extends AbstractDecimalField<BigDecimal> implements IBigDecimalField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractBigDecimalField.class);

  public AbstractBigDecimalField() {
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(300)
  @ConfigPropertyValue("null")
  protected Double getConfiguredMinValue() {
    return null;
  }

  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(310)
  @ConfigPropertyValue("null")
  protected Double getConfiguredMaxValue() {
    return null;
  }

  @Override
  protected void initConfig() {
    super.initConfig();
    setMinValue(getConfiguredMinValue() != null ? BigDecimal.valueOf(getConfiguredMinValue()) : null);
    setMaxValue(getConfiguredMaxValue() != null ? BigDecimal.valueOf(getConfiguredMaxValue()) : null);
  }

  // convert string to a real long
  @Override
  protected BigDecimal parseValueInternal(String text) throws ProcessingException {
    BigDecimal retVal = null;
    if (text == null) text = "";
    else text = text.trim();
    if (text.length() > 0) {
      NumberFormat numberFormat = createNumberFormat();
      if (isPercent()) {
        if (text.endsWith("%")) {
          text = StringUtility.trim(text.substring(0, text.length() - 1));
        }
        String suffix = "%";
        if (numberFormat instanceof DecimalFormat) {
          suffix = ((DecimalFormat) numberFormat).getPositiveSuffix();
        }
        text = StringUtility.concatenateTokens(text, suffix);
      }
      ParsePosition p = new ParsePosition(0);
      Number n = numberFormat.parse(text, p);
      if (p.getErrorIndex() >= 0 || p.getIndex() != text.length()) {
        throw new ProcessingException(ScoutTexts.get("InvalidNumberMessageX", text));
      }
      NumberFormat fmt = NumberFormat.getNumberInstance();
      /* add/preserve fraction digits for multiplier */
      int npc = ("" + Math.abs(getMultiplier())).length() - 1;
      fmt.setMaximumFractionDigits(getFractionDigits() + npc);
      p = new ParsePosition(0);
      String fmtText = fmt.format(n.doubleValue());
      retVal = BigDecimal.valueOf(fmt.parse(fmtText, p).doubleValue());
      if (p.getErrorIndex() >= 0 || p.getIndex() != fmtText.length()) {
        throw new ProcessingException(ScoutTexts.get("InvalidNumberMessageX", fmtText));
      }
    }
    return retVal;
  }
}