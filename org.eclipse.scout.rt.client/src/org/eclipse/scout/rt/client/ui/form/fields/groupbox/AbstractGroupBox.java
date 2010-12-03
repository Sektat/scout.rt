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
package org.eclipse.scout.rt.client.ui.form.fields.groupbox;

import java.util.ArrayList;

import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.ui.action.keystroke.DefaultFormEnterKeyStroke;
import org.eclipse.scout.rt.client.ui.action.keystroke.DefaultFormEscapeKeyStroke;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractCompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.internal.GroupBoxBodyGrid;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.internal.GroupBoxProcessButtonGrid;

public abstract class AbstractGroupBox extends AbstractCompositeField implements IGroupBox {

  private IGroupBoxUIFacade m_uiFacade;
  private boolean m_mainBoxFlag = false;
  private int m_gridColumnCountHint;
  private boolean m_scrollable;
  private IFormField[] m_controlFields;
  private IGroupBox[] m_groupBoxes;
  private IButton[] m_customButtons;
  private IButton[] m_systemButtons;
  private GroupBoxBodyGrid m_bodyGrid;
  private GroupBoxProcessButtonGrid m_customProcessButtonGrid;
  private GroupBoxProcessButtonGrid m_systemProcessButtonGrid;

  public AbstractGroupBox() {
    this(true);
  }

  public AbstractGroupBox(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */
  @ConfigPropertyValue("true")
  @Override
  protected boolean getConfiguredGridUseUiHeight() {
    return true;
  }

  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(200)
  @ConfigPropertyValue("-1")
  protected int getConfiguredGridColumnCount() {
    return -1;
  }

  /**
   * Show border around box
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(230)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredBorderVisible() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(231)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredExpandable() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(232)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredExpanded() {
    return true;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(233)
  @ConfigPropertyValue("BORDER_DECORATION_AUTO")
  protected String getConfiguredBorderDecoration() {
    return BORDER_DECORATION_AUTO;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(240)
  @ConfigPropertyValue("null")
  protected String getConfiguredBackgroundImageName() {
    return null;
  }

  @ConfigProperty(ConfigProperty.HORIZONTAL_ALIGNMENT)
  @Order(250)
  @ConfigPropertyValue("0")
  protected int getConfiguredBackgroundImageHorizontalAlignment() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.VERTICAL_ALIGNMENT)
  @Order(260)
  @ConfigPropertyValue("0")
  protected int getConfiguredBackgroundImageVerticalAlignment() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(270)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredScrollable() {
    return false;
  }

  @Override
  @ConfigPropertyValue("FULL_WIDTH")
  protected int getConfiguredGridW() {
    return FULL_WIDTH;
  }

  @Override
  protected void initConfig() {
    m_uiFacade = new P_UIFacade();
    m_bodyGrid = new GroupBoxBodyGrid(this);
    m_customProcessButtonGrid = new GroupBoxProcessButtonGrid(this, true, false);
    m_systemProcessButtonGrid = new GroupBoxProcessButtonGrid(this, false, true);
    super.initConfig();
    IFormField[] a = getFields();
    // categorize items
    ArrayList<IFormField> controlList = new ArrayList<IFormField>();
    ArrayList<IGroupBox> groupList = new ArrayList<IGroupBox>();
    ArrayList<IButton> customButtonList = new ArrayList<IButton>();
    ArrayList<IButton> systemButtonList = new ArrayList<IButton>();
    for (int i = 0; i < a.length; i++) {
      IFormField f = a[i];
      if (f instanceof IGroupBox) {
        groupList.add((IGroupBox) f);
        controlList.add(f);
      }
      else if (f instanceof IButton) {
        IButton b = (IButton) f;
        if (b.isProcessButton()) {
          if (b.getSystemType() != IButton.SYSTEM_TYPE_NONE) {
            systemButtonList.add((IButton) f);
          }
          else {
            customButtonList.add((IButton) f);
          }
        }
        else {
          controlList.add(f);
        }
      }
      else {
        controlList.add(f);
      }
    }
    m_controlFields = controlList.toArray(new IFormField[controlList.size()]);
    m_groupBoxes = groupList.toArray(new IGroupBox[groupList.size()]);
    m_customButtons = customButtonList.toArray(new IButton[customButtonList.size()]);
    m_systemButtons = systemButtonList.toArray(new IButton[systemButtonList.size()]);
    //
    setExpandable(getConfiguredExpandable());
    setExpanded(getConfiguredExpanded());
    setBorderVisible(getConfiguredBorderVisible());
    setBorderDecoration(getConfiguredBorderDecoration());
    setGridColumnCountHint(getConfiguredGridColumnCount());
    setBackgroundImageName(getConfiguredBackgroundImageName());
    setBackgroundImageHorizontalAlignment(getConfiguredBackgroundImageHorizontalAlignment());
    setBackgroundImageVerticalAlignment(getConfiguredBackgroundImageVerticalAlignment());
    setScrollable(getConfiguredScrollable());
  }

  @Override
  public IKeyStroke[] getContributedKeyStrokes() {
    ArrayList<IKeyStroke> list = new ArrayList<IKeyStroke>(2);
    if (isMainBox() && (getForm() != null && getForm().getOuterForm() == null)) {
      // add default escape and enter key stroke only if no similar key stroke
      // is defined on the mainbox
      boolean hasEnter = false;
      boolean hasEscape = false;
      for (IKeyStroke ks : getLocalKeyStrokes()) {
        if ("enter".equalsIgnoreCase(ks.getKeyStroke())) {
          hasEnter = true;
        }
        if ("escape".equalsIgnoreCase(ks.getKeyStroke())) {
          hasEscape = true;
        }
      }
      if (!hasEnter) {
        list.add(new DefaultFormEnterKeyStroke(getForm()));
      }
      if (!hasEscape) {
        list.add(new DefaultFormEscapeKeyStroke(getForm()));
      }
    }
    return list.toArray(new IKeyStroke[list.size()]);
  }

  @Override
  public void rebuildFieldGrid() {
    m_bodyGrid.validate();
    m_customProcessButtonGrid.validate();
    m_systemProcessButtonGrid.validate();
    if (isInitialized()) {
      if (getForm() != null) {
        getForm().structureChanged(this);
      }
    }
  }

  public boolean isMainBox() {
    return m_mainBoxFlag;
  }

  public void setMainBox(boolean b) {
    m_mainBoxFlag = b;
  }

  public int getGroupBoxIndex(IGroupBox groupBox) {
    for (int i = 0; i < m_groupBoxes.length; i++) {
      if (m_groupBoxes[i] == groupBox) return i;
    }
    return -1;
  }

  public int getCustomProcessButtonCount() {
    return m_customButtons.length;
  }

  public int getGroupBoxCount() {
    return m_groupBoxes.length;
  }

  public int getSystemProcessButtonCount() {
    return m_systemButtons.length;
  }

  public IGroupBox[] getGroupBoxes() {
    IGroupBox[] a = new IGroupBox[m_groupBoxes.length];
    System.arraycopy(m_groupBoxes, 0, a, 0, a.length);
    return a;
  }

  public IFormField[] getControlFields() {
    IFormField[] a = new IFormField[m_controlFields.length];
    System.arraycopy(m_controlFields, 0, a, 0, a.length);
    return a;
  }

  public IButton[] getCustomProcessButtons() {
    IButton[] a = new IButton[m_customButtons.length];
    System.arraycopy(m_customButtons, 0, a, 0, a.length);
    return a;
  }

  public IButton[] getSystemProcessButtons() {
    IButton[] a = new IButton[m_systemButtons.length];
    System.arraycopy(m_systemButtons, 0, a, 0, a.length);
    return a;
  }

  public final int getGridColumnCount() {
    return m_bodyGrid.getGridColumnCount();
  }

  public final int getGridRowCount() {
    return m_bodyGrid.getGridRowCount();
  }

  public void setGridColumnCountHint(int c) {
    m_gridColumnCountHint = c;
    if (isInitialized()) {
      rebuildFieldGrid();
    }
  }

  public int getGridColumnCountHint() {
    return m_gridColumnCountHint;
  }

  public boolean isScrollable() {
    return m_scrollable;
  }

  public void setScrollable(boolean scrollable) {
    if (m_scrollable != scrollable) {
      m_scrollable = scrollable;
      if (m_scrollable) {
        // force weighty to be > 0
        GridData gd = getGridDataHints();
        if (gd.weightY <= 0) {
          gd.weightY = 1;
          setGridDataHints(gd);
        }
      }
    }
  }

  // box is only visible when it has at least one visible item
  @Override
  protected void handleFieldVisibilityChanged() {
    super.handleFieldVisibilityChanged();
    if (isInitialized()) {
      rebuildFieldGrid();
    }
  }

  public boolean isBorderVisible() {
    return propertySupport.getPropertyBool(PROP_BORDER_VISIBLE);
  }

  public void setBorderVisible(boolean b) {
    propertySupport.setPropertyBool(PROP_BORDER_VISIBLE, b);
  }

  public String getBorderDecoration() {
    return propertySupport.getPropertyString(PROP_BORDER_DECORATION);
  }

  public void setBorderDecoration(String s) {
    propertySupport.setPropertyString(PROP_BORDER_DECORATION, s);
  }

  public boolean isExpandable() {
    return propertySupport.getPropertyBool(PROP_EXPANDABLE);
  }

  public void setExpandable(boolean b) {
    propertySupport.setPropertyBool(PROP_EXPANDABLE, b);
  }

  public boolean isExpanded() {
    return propertySupport.getPropertyBool(PROP_EXPANDED);
  }

  public void setExpanded(boolean b) {
    propertySupport.setPropertyBool(PROP_EXPANDED, b);
  }

  public IGroupBoxUIFacade getUIFacade() {
    return m_uiFacade;
  }

  public void setBackgroundImageName(String imageName) {
    propertySupport.setPropertyString(PROP_BACKGROUND_IMAGE_NAME, imageName);
  }

  public String getBackgroundImageName() {
    return propertySupport.getPropertyString(PROP_BACKGROUND_IMAGE_NAME);
  }

  /**
   * @since Build 178
   */
  public void setBackgroundImageVerticalAlignment(int a) {
    propertySupport.setPropertyInt(PROP_BACKGROUND_IMAGE_VERTICAL_ALIGNMENT, a);
  }

  /**
   * @since Build 178
   */
  public int getBackgroundImageVerticalAlignment() {
    return propertySupport.getPropertyInt(PROP_BACKGROUND_IMAGE_VERTICAL_ALIGNMENT);
  }

  /**
   * @since Build 178
   */
  public void setBackgroundImageHorizontalAlignment(int a) {
    propertySupport.setPropertyInt(PROP_BACKGROUND_IMAGE_HORIZONTAL_ALIGNMENT, a);
  }

  /**
   * @since Build 178
   */
  public int getBackgroundImageHorizontalAlignment() {
    return propertySupport.getPropertyInt(PROP_BACKGROUND_IMAGE_HORIZONTAL_ALIGNMENT);
  }

  private class P_UIFacade implements IGroupBoxUIFacade {
    public void setExpandedFromUI(boolean expanded) {
      setExpanded(expanded);
    }
  }
}