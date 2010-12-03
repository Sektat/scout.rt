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
package org.eclipse.scout.rt.client.ui.action;

import java.beans.PropertyChangeListener;
import java.security.Permission;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractAction extends AbstractPropertyObserver implements IAction {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractAction.class);

  private boolean m_initialized;
  private final EventListenerList m_listenerList = new EventListenerList();
  private final IActionUIFacade m_uiFacade;
  private boolean m_inheritAccessibility;
  private boolean m_enabledProperty;
  private boolean m_enabledGranted;
  private boolean m_visibleProperty;
  private boolean m_visibleGranted;
  private boolean m_singleSelectionAction;
  private boolean m_multiSelectionAction;
  private boolean m_emptySpaceAction;
  private boolean m_toggleAction;

  public AbstractAction() {
    this(true);
  }

  public AbstractAction(boolean callInitializer) {
    m_uiFacade = createUIFacade();
    m_enabledGranted = true;
    m_visibleGranted = true;
    if (callInitializer) {
      callInitializer();
    }
  }

  protected void callInitializer() {
    if (!m_initialized) {
      initConfig();
      try {
        execInitAction();
      }
      catch (Throwable t) {
        LOG.warn("Action " + getClass().getName(), t);
      }
      m_initialized = true;
    }
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(30)
  @ConfigPropertyValue("null")
  protected String getConfiguredIconId() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(40)
  @ConfigPropertyValue("null")
  protected String getConfiguredText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(50)
  @ConfigPropertyValue("null")
  protected String getConfiguredTooltipText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(55)
  @ConfigPropertyValue("null")
  protected String getConfiguredKeyStroke() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(10)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredEnabled() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(20)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredVisible() {
    return true;
  }

  /**
   * @return true if {@link #prepareAction()} should in addition consider the
   *         context of the action to decide for visibility and enabled.<br>
   *         For example a menu of a table field with {@link #isInheritAccessibility()}==true is invisible when the
   *         table
   *         field is disabled or invisible
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(22)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredInheritAccessibility() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(25)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredToggleAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(60)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredSingleSelectionAction() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(70)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredMultiSelectionAction() {
    return false;
  }

  /**
   * @deprecated obsolete, not used anymore
   */
  @Deprecated
  protected boolean getConfiguredNonSelectionAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(90)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredEmptySpaceAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredSeparator() {
    return false;
  }

  /**
   * called by constructor<br>
   * this way a menu can for example add/remove custom child menus
   */
  @ConfigOperation
  @Order(10)
  protected void execInitAction() throws ProcessingException {
  }

  /**
   * called by prepareAction before action is added to list or used<br>
   * this way a menu can be made dynamically visible / enabled
   */
  @ConfigOperation
  @Order(20)
  protected void execPrepareAction() throws ProcessingException {
  }

  /**
   * called when action is performed
   */
  @ConfigOperation
  @Order(30)
  protected void execAction() throws ProcessingException {
  }

  /**
   * called whenever the selection (of a toggle-action) is changed
   */
  @ConfigOperation
  @Order(31)
  protected void execToggleAction(boolean selected) throws ProcessingException {
  }

  protected void initConfig() {
    setIconId(getConfiguredIconId());
    setText(getConfiguredText());
    setTooltipText(getConfiguredTooltipText());
    setKeyStroke(getConfiguredKeyStroke());
    setInheritAccessibility(getConfiguredInheritAccessibility());
    setEnabled(getConfiguredEnabled());
    setVisible(getConfiguredVisible());
    setToggleAction(getConfiguredToggleAction());
    setSingleSelectionAction(getConfiguredSingleSelectionAction());
    setMultiSelectionAction(getConfiguredMultiSelectionAction());
    setEmptySpaceAction(getConfiguredEmptySpaceAction());
    setSeparator(getConfiguredSeparator());
    if (isSingleSelectionAction() || isMultiSelectionAction() || isEmptySpaceAction()) {
      // ok
    }
    else {
      // legacy case of implicit new menu
      setEmptySpaceAction(true);
    }
  }

  protected IActionUIFacade createUIFacade() {
    return new P_UIFacade();
  }

  public int acceptVisitor(IActionVisitor visitor) {
    switch (visitor.visit(this)) {
      case IActionVisitor.CANCEL:
        return IActionVisitor.CANCEL;
      case IActionVisitor.CANCEL_SUBTREE:
        return IActionVisitor.CONTINUE;
      case IActionVisitor.CONTINUE_BRANCH:
        return IActionVisitor.CANCEL;
      default:
        return IActionVisitor.CONTINUE;
    }
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(listener);
  }

  @Override
  public void addPropertyChangeListener(String propName, PropertyChangeListener listener) {
    propertySupport.addPropertyChangeListener(propName, listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(String propName, PropertyChangeListener listener) {
    propertySupport.removePropertyChangeListener(propName, listener);
  }

  public boolean hasProperty(String name) {
    return propertySupport.hasProperty(name);
  }

  public String getActionId() {
    String s = getClass().getName();
    int i = Math.max(s.lastIndexOf('$'), s.lastIndexOf('.'));
    s = s.substring(i + 1);
    return s;
  }

  public void doAction() throws ProcessingException {
    execAction();
  }

  public String getIconId() {
    return propertySupport.getPropertyString(PROP_ICON_ID);
  }

  public void setIconId(String iconId) {
    propertySupport.setPropertyString(PROP_ICON_ID, iconId);
  }

  public String getText() {
    return propertySupport.getPropertyString(PROP_TEXT);
  }

  public void setText(String text) {
    if (text != null) {
      propertySupport.setPropertyString(PROP_TEXT, StringUtility.removeMnemonic(text));
      propertySupport.setPropertyInt(PROP_MNEMONIC, StringUtility.getMnemonic(text));
    }
    else {
      propertySupport.setPropertyString(PROP_TEXT, null);
      propertySupport.setPropertyInt(PROP_MNEMONIC, 0x0);
    }
  }

  public String getKeyStroke() {
    return propertySupport.getPropertyString(PROP_KEYSTROKE);
  }

  public void setKeyStroke(String k) {
    // normalize key stroke format
    if (k != null) {
      k = k.toLowerCase();
      boolean shift = false;
      boolean ctrl = false;
      boolean alt = false;
      String key = null;
      if (k.endsWith(" ")) {
        key = " ";
      }
      for (String s : k.trim().split("[ -]")) {
        if (s.equals("shift")) shift = true;
        else if (s.equals("control")) ctrl = true;
        else if (s.equals("ctrl")) ctrl = true;
        else if (s.equals("strg")) ctrl = true;
        else if (s.equals("alt")) alt = true;
        else if (s.equals("alternate")) alt = true;
        else key = s;
      }
      if (key != null) {
        k = (shift ? "shift-" : "") + (ctrl ? "control-" : "") + (alt ? "alternate-" : "") + key;
      }
      else {
        k = null;
      }
    }
    propertySupport.setPropertyString(PROP_KEYSTROKE, k);
  }

  public String getTooltipText() {
    return propertySupport.getPropertyString(PROP_TOOLTIP_TEXT);
  }

  public void setTooltipText(String text) {
    propertySupport.setPropertyString(PROP_TOOLTIP_TEXT, text);
  }

  public boolean isEnabled() {
    return propertySupport.getPropertyBool(PROP_ENABLED);
  }

  public void setEnabled(boolean b) {
    m_enabledProperty = b;
    setEnabledInternal();
  }

  public boolean isSelected() {
    return propertySupport.getPropertyBool(PROP_SELECTED);
  }

  public void setSelected(boolean b) {
    boolean changed = propertySupport.setPropertyBool(PROP_SELECTED, b);
    if (changed) {
      try {
        execToggleAction(b);
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }
  }

  public boolean isToggleAction() {
    return m_toggleAction;
  }

  public void setToggleAction(boolean b) {
    m_toggleAction = b;
  }

  public boolean isVisible() {
    return propertySupport.getPropertyBool(PROP_VISIBLE);
  }

  public void setVisible(boolean b) {
    m_visibleProperty = b;
    setVisibleInternal();
  }

  public boolean isInheritAccessibility() {
    return m_inheritAccessibility;
  }

  public void setInheritAccessibility(boolean b) {
    m_inheritAccessibility = b;
  }

  /**
   * Access control<br>
   * when false, overrides isEnabled with false
   */
  public void setEnabledPermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setEnabledGranted(b);
  }

  public boolean isEnabledGranted() {
    return m_enabledGranted;
  }

  /**
   * Access control<br>
   * when false, overrides isEnabled with false
   */
  public void setEnabledGranted(boolean b) {
    m_enabledGranted = b;
    setEnabledInternal();
  }

  private void setEnabledInternal() {
    propertySupport.setPropertyBool(PROP_ENABLED, m_enabledGranted && m_enabledProperty);
  }

  public void setVisiblePermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setVisibleGranted(b);
  }

  public boolean isVisibleGranted() {
    return m_visibleGranted;
  }

  public void setVisibleGranted(boolean b) {
    m_visibleGranted = b;
    setVisibleInternal();
  }

  private void setVisibleInternal() {
    propertySupport.setPropertyBool(PROP_VISIBLE, m_visibleGranted && m_visibleProperty);
  }

  public boolean isSeparator() {
    return propertySupport.getPropertyBool(PROP_SEPARATOR);
  }

  public void setSeparator(boolean b) {
    propertySupport.setPropertyBool(PROP_SEPARATOR, b);
  }

  public boolean isSingleSelectionAction() {
    return m_singleSelectionAction;
  }

  public void setSingleSelectionAction(boolean b) {
    m_singleSelectionAction = b;
  }

  public boolean isMultiSelectionAction() {
    return m_multiSelectionAction;
  }

  public void setMultiSelectionAction(boolean b) {
    m_multiSelectionAction = b;
  }

  public boolean isEmptySpaceAction() {
    return m_emptySpaceAction;
  }

  public void setEmptySpaceAction(boolean b) {
    m_emptySpaceAction = b;
  }

  public char getMnemonic() {
    return (char) propertySupport.getPropertyInt(PROP_MNEMONIC);
  }

  public final void prepareAction() {
    try {
      prepareActionInternal();
      execPrepareAction();
    }
    catch (Throwable t) {
      LOG.warn("Action " + getClass().getName(), t);
    }
  }

  public IActionUIFacade getUIFacade() {
    return m_uiFacade;
  }

  /**
   * do not use this method, it is used internally by subclasses
   */
  protected void prepareActionInternal() throws ProcessingException {
  }

  protected class P_UIFacade implements IActionUIFacade {
    public void fireActionFromUI() {
      try {
        if (isEnabled() && isVisible()) {
          doAction();
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      catch (Throwable e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected exception", e));
      }
    }

    public void setSelectedFromUI(boolean b) {
      setSelected(b);
    }
  }
}