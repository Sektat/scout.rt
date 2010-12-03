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
package org.eclipse.scout.rt.ui.swt.basic.tree;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.ui.swt.ISwtEnvironment;
import org.eclipse.scout.rt.ui.swt.extension.UiDecorationExtensionPoint;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class SwtScoutTreeModel extends LabelProvider implements ITreeContentProvider, IFontProvider, IColorProvider {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(SwtScoutTreeModel.class);

  private ITree m_tree;
  private final ISwtEnvironment m_environment;
  private final TreeViewer m_treeViewer;
  private Image m_imgCheckboxTrue;
  private Image m_imgCheckboxFalse;
  private Color m_disabledForegroundColor;

  public SwtScoutTreeModel(ITree tree, ISwtEnvironment environment, TreeViewer treeViewer) {
    m_tree = tree;
    m_environment = environment;
    m_treeViewer = treeViewer;
    m_imgCheckboxTrue = m_environment.getIcon(AbstractIcons.CheckboxYes);
    m_imgCheckboxFalse = m_environment.getIcon(AbstractIcons.CheckboxNo);
    m_disabledForegroundColor = m_environment.getColor(UiDecorationExtensionPoint.getLookAndFeel().getColorForegroundDiabled());
  }

  public Object[] getChildren(Object parentElement) {
    ITreeNode scoutNode = (ITreeNode) parentElement;
    return scoutNode.getFilteredChildNodes();
  }

  public Object getParent(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    return scoutNode.getParentNode();
  }

  public boolean hasChildren(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    return !scoutNode.isLeaf();
  }

  public Object[] getElements(Object inputElement) {
    if (m_tree != null) {
      if (m_tree.isRootNodeVisible()) {
        return new Object[]{m_tree.getRootNode()};
      }
      else {
        return m_tree.getRootNode().getFilteredChildNodes();
      }
    }
    else {
      return new Object[0];
    }
  }

  @Override
  public Image getImage(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    Image img = null;
    if (m_tree.isCheckable()) {
      if (scoutNode != null && scoutNode.isChecked()) {
        return m_imgCheckboxTrue;
      }
      else {
        return m_imgCheckboxFalse;
      }
    }
    else {
      img = m_environment.getIcon(scoutNode.getCell().getIconId());
    }
    return img;
  }

  @Override
  public String getText(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    return scoutNode.getCell().getText();
  }

  public Font getFont(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    if (scoutNode.getCell().getFont() != null) {
      return m_environment.getFont(scoutNode.getCell().getFont(), m_treeViewer.getTree().getFont());
    }
    return null;
  }

  public Color getForeground(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    ICell scoutCell = scoutNode.getCell();
    Color col = m_environment.getColor(scoutCell.getForegroundColor());
    if (col == null) {
      if (!scoutCell.isEnabled()) {
        col = m_disabledForegroundColor;
      }
    }
    return col;
  }

  public Color getBackground(Object element) {
    ITreeNode scoutNode = (ITreeNode) element;
    if (scoutNode.getCell().getBackgroundColor() != null) {
      return m_environment.getColor(scoutNode.getCell().getBackgroundColor());
    }
    return null;
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

}