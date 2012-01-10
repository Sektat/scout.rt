/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.core.window.desktop.navigation;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rwt.IBrowserHistory;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.events.BrowserHistoryEvent;
import org.eclipse.rwt.events.BrowserHistoryListener;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.desktop.navigation.INavigationHistoryService;
import org.eclipse.scout.rt.client.ui.desktop.navigation.NavigationHistoryEvent;
import org.eclipse.scout.rt.client.ui.desktop.navigation.NavigationHistoryListener;
import org.eclipse.scout.rt.shared.services.common.bookmark.AbstractPageState;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.ui.rap.core.IRwtCoreEnvironment;
import org.eclipse.scout.service.SERVICES;

/**
 * <h3>RwtScoutNavigation</h3> ...
 * 
 * @author Andreas Hoegger
 * @since 3.7.0 June 2011
 */
public class RwtScoutNavigationSupport {

  private final IRwtCoreEnvironment m_uiEnvironment;
  private IBrowserHistory m_uiHistory;
  private INavigationHistoryService m_historyService;
  private P_NavigationHistoryListener m_scoutListener;
  private BrowserHistoryListener m_uiListener = new BrowserHistoryListener() {
    private static final long serialVersionUID = 1L;

    @Override
    public void navigated(BrowserHistoryEvent event) {
      handleNavigationFromUi(event.entryId);
    }
  };

  public RwtScoutNavigationSupport(IRwtCoreEnvironment uiEnvironment) {
    m_uiEnvironment = uiEnvironment;
  }

  public void install() {
    if (m_uiHistory == null) {
      m_uiHistory = RWT.getBrowserHistory();
      m_uiHistory.addBrowserHistoryListener(m_uiListener);
    }
    new ClientSyncJob("", getUiEnvironment().getClientSession()) {
      @Override
      protected void runVoid(IProgressMonitor monitor) throws Throwable {
        m_historyService = SERVICES.getService(INavigationHistoryService.class);
        if (m_scoutListener == null) {
          m_scoutListener = new P_NavigationHistoryListener();
          m_historyService.addNavigationHistoryListener(m_scoutListener);
        }
      }
    }.schedule();
  }

  public void uninstall() {
    if (m_historyService != null && m_scoutListener != null) {
      m_historyService.removeNavigationHistoryListener(m_scoutListener);
    }
    if (m_uiHistory != null) {
      m_uiHistory.removeBrowserHistoryListener(m_uiListener);
    }
  }

  protected void handleNavigationFromUi(final String entryId) {
    Runnable t = new Runnable() {
      @Override
      public void run() {
        try {
          for (Bookmark b : m_historyService.getBookmarks()) {
            if (getId(b).equals(entryId)) {
              m_historyService.stepTo(b);
              break;
            }
          }
        }
        catch (ProcessingException e) {
          //nop
        }
      }
    };
    getUiEnvironment().invokeScoutLater(t, 0);

  }

  private IRwtCoreEnvironment getUiEnvironment() {
    return m_uiEnvironment;
  }

  protected void handleBookmarkAddedFromScout(Bookmark bookmark) {
    String id = getId(bookmark);
    StringBuilder textBuilder = new StringBuilder(getUiEnvironment().getClientSession().getDesktop().getTitle() + " - ");
    textBuilder.append(cleanNl(bookmark.getText()));
    m_uiHistory.createEntry(id, textBuilder.toString());
  }

  private String cleanNl(String s) {
    s = s.replaceAll("(\r\n)|(\n\r)|(\n)|(\r)", "-");
    s = s.replaceAll("\\s*\\-\\s*", "-");
    s = s.replaceAll("\\s+", "-");
    return s;
  }

  private String getId(Bookmark b) {
    StringBuilder key = new StringBuilder();
    if (!StringUtility.isNullOrEmpty(b.getOutlineClassName())) {
      key.append(b.getOutlineClassName());
    }
    List<AbstractPageState> path = b.getPath();
    if (!path.isEmpty()) {
      for (int i = 0; i < path.size(); i++) {
        if (!StringUtility.isNullOrEmpty(path.get(i).getLabel())) {
          key.append("-" + path.get(i).getLabel());
        }
      }
    }
    return cleanNl(key.toString());
  }

  private class P_NavigationHistoryListener implements NavigationHistoryListener {
    @Override
    public void navigationChanged(NavigationHistoryEvent e) {
      if (e.getType() == NavigationHistoryEvent.TYPE_BOOKMARK_ADDED) {
        final Bookmark bookmark = e.getBookmark();
        Runnable r = new Runnable() {
          @Override
          public void run() {
            handleBookmarkAddedFromScout(bookmark);
          }
        };
        getUiEnvironment().invokeUiLater(r);
      }
    }
  }

}