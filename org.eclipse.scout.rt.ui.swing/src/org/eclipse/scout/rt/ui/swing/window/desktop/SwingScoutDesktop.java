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
package org.eclipse.scout.rt.ui.swing.window.desktop;

import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.DesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.ui.swing.SwingUtility;
import org.eclipse.scout.rt.ui.swing.basic.SwingScoutComposite;
import org.eclipse.scout.rt.ui.swing.ext.JInternalFrameEx;
import org.eclipse.scout.rt.ui.swing.focus.SwingScoutFocusTraversalPolicy;
import org.eclipse.scout.rt.ui.swing.window.desktop.layout.MultiSplitDesktopManager;
import org.eclipse.scout.rt.ui.swing.window.desktop.layout.MultiSplitLayout;
import org.eclipse.scout.rt.ui.swing.window.desktop.layout.MultiSplitLayoutConstraints;

/**
 * Desktop with multi-split layout similar to eclipse perspectives. The matrix
 * consists of a 3x3 grid with 9 cells. Views can be placed using a viewId as
 * coordiante: NW N NE W C E SW S SE By default editors (and the page-table) are
 * located in the center at C, the outline is located at NW and the
 * outline-selector is located at SW Assigning no viewId will place the
 * InternalFrame over the whole screen filling all 9 cells
 */
public class SwingScoutDesktop extends SwingScoutComposite<IDesktop> implements ISwingScoutDesktop {

  private ToolsViewPlaceholder m_toolsViewPlaceholder;

  public SwingScoutDesktop() {
    //keep empty
  }

  @Override
  protected void initializeSwing() {
    super.initializeSwing();
    JDesktopPane swingDesktop = new JDesktopPane();
    setSwingField(swingDesktop);
    //
    swingDesktop.setDesktopManager(new MultiSplitDesktopManager());
    ColumnSplitStrategyWithToolsView columnSplitStrategy = new ColumnSplitStrategyWithToolsView();
    MultiSplitLayout layout = new MultiSplitLayout(columnSplitStrategy);
    swingDesktop.setLayout(layout);
    swingDesktop.setOpaque(true);
    // cursor
    swingDesktop.setCursor(null);
    // focus root
    swingDesktop.setFocusCycleRoot(true);
    swingDesktop.setFocusTraversalPolicy(new SwingScoutFocusTraversalPolicy());
    // AWE: this could be integrated in new ui (was forgotton in metal laf)
    swingDesktop.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(SwingUtility.createKeystroke("shift ctrl TAB"), "selectPreviousFrame");
    // register ctrl-TAB and ctrl-shift-TAB actions according to ui
    swingDesktop.getActionMap().put("selectNextFrame", new P_SwingTabFrameAction(1));
    swingDesktop.getActionMap().put("selectPreviousFrame", new P_SwingTabFrameAction(-1));
    m_toolsViewPlaceholder = new ToolsViewPlaceholder(swingDesktop, columnSplitStrategy);
    // development shortcuts
    SwingUtility.installDevelopmentShortcuts(getSwingDesktopPane());
  }

  public JDesktopPane getSwingDesktopPane() {
    return (JDesktopPane) getSwingField();
  }

  private void handleSwingTabAction(int delta) {
    JInternalFrame[] frames = getSwingDesktopPane().getAllFrames();
    TreeMap<Integer, JInternalFrame> sortedMap = new TreeMap<Integer, JInternalFrame>();
    for (int i = 0; i < frames.length; i++) {
      if (frames[i].isVisible() && !frames[i].isIcon()) {
        int tabIndex = i;
        if (frames[i] instanceof JInternalFrameEx) {
          tabIndex = ((JInternalFrameEx) frames[i]).getTabIndex();
        }
        if (delta < 0) {
          tabIndex = -tabIndex;
        }
        sortedMap.put(tabIndex, frames[i]);
      }
    }
    JInternalFrame next = null;
    if (sortedMap.size() > 0) {
      ArrayList<JInternalFrame> list = new ArrayList<JInternalFrame>(sortedMap.values());
      int nextIndex = 0;
      JInternalFrame selectedFrame = getSwingDesktopPane().getSelectedFrame();
      if (selectedFrame != null) {
        nextIndex = (list.indexOf(selectedFrame) + 1) % list.size();
      }
      next = list.get(nextIndex);
    }
    if (next != null) {
      try {
        next.setSelected(true);
      }
      catch (PropertyVetoException ve) {
      }
    }
  }

  public void addView(JInternalFrameEx f, Object constraints) {
    MultiSplitLayoutConstraints mc = (MultiSplitLayoutConstraints) constraints;
    f.setTabIndex(mc.tabIndex);
    f.setVisible(true);
    if (mc.col == 2) {
      f.setName("Synth.ToolsView");
      m_toolsViewPlaceholder.addFrame(f);
    }
    getSwingDesktopPane().add(f, mc);
    //ticket 90942, detail pane initially only has 20px height
    DesktopManager dm = getSwingDesktopPane().getDesktopManager();
    if (dm instanceof MultiSplitDesktopManager) {
      ((MultiSplitDesktopManager) dm).fitFrames(new JInternalFrame[]{f});
    }
    //end ticket
    getSwingDesktopPane().revalidate();
    getSwingDesktopPane().repaint();
  }

  public void removeView(JInternalFrameEx f) {
    m_toolsViewPlaceholder.removeFrame(f);
    getSwingDesktopPane().getDesktopManager().closeFrame(f);
    f.setVisible(false);
    getSwingDesktopPane().remove(f);
  }

  public ToolsViewPlaceholder getToolsViewPlaceholder() {
    return m_toolsViewPlaceholder;
  }

  /*
   * inner classes
   */

  /**
   * Focus cycle actions using correct frame order AWE: could be integrated into
   * UI
   */
  private class P_SwingTabFrameAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private int m_delta;

    public P_SwingTabFrameAction(int delta) {
      m_delta = delta;
    }

    public void actionPerformed(ActionEvent e) {
      handleSwingTabAction(m_delta);
    }
  }

}