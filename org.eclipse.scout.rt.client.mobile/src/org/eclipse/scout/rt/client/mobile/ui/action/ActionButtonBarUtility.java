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
package org.eclipse.scout.rt.client.mobile.ui.action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.rt.client.mobile.ui.form.IMobileAction;
import org.eclipse.scout.rt.client.mobile.ui.form.outline.AutoLeafPageWithNodes;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;

/**
 * @since 3.9.0
 */
public class ActionButtonBarUtility {

  public static List<IMobileAction> convertButtonsToActions(IButton[] buttons) {
    List<IMobileAction> menuList = new LinkedList<IMobileAction>();
    for (IButton button : buttons) {
      IMobileAction action = convertButtonToAction(button);
      if (action != null) {
        menuList.add(action);
      }
    }

    return menuList;
  }

  public static IMobileAction convertButtonToAction(IButton button) {
    if (button == null) {
      return null;
    }

    return new ButtonWrappingAction(button);
  }

  /**
   * If there are empty space menus distribute the row menus so that the menus alternate and the most important are on
   * top, starting with a empty space menu
   */
  public static void distributeRowActions(List<IMenu> menuList, IMenu[] emptySpaceMenus, List<IMenu> rowMenuList) {
    if (emptySpaceMenus == null) {
      return;
    }

    for (IMenu emptySpaceMenu : emptySpaceMenus) {
      if (rowMenuList.size() == 0) {
        break;
      }

      int index = menuList.indexOf(emptySpaceMenu) + 1;
      IMenu rowMenu = rowMenuList.get(0);
      menuList.add(index, rowMenu);
      rowMenuList.remove(rowMenu);
    }
  }

  /**
   * Fetches the actions of the given page (tree node and table row menus).
   */
  public static List<IMenu> fetchPageActions(IPage page) {
    List<IMenu> pageActions = new LinkedList<IMenu>();
    if (page.getTree() != null) {
      //Fetch the menus of the given page (getUIFacade().fireNodePopupFromUI() is not possible since the selected node may not the same as the given page)
      pageActions.addAll(Arrays.asList(page.getTree().fetchMenusForNodesInternal(new ITreeNode[]{page})));
      if (page instanceof AutoLeafPageWithNodes) {
        //AutoLeafPage has no parent so the table row actions are not fetched by the regular way (see AbstractOutline#P_OutlineListener).
        //Instead we directly fetch the table row actions
        pageActions.addAll(Arrays.asList(((AutoLeafPageWithNodes) page).getTableRow().getTable().getUIFacade().fireRowPopupFromUI()));
      }
    }

    return pageActions;
  }

}
