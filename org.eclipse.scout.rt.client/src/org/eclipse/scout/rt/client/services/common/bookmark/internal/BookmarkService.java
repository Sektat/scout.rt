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
package org.eclipse.scout.rt.client.services.common.bookmark.internal;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.services.common.bookmark.BookmarkServiceEvent;
import org.eclipse.scout.rt.client.services.common.bookmark.BookmarkServiceListener;
import org.eclipse.scout.rt.client.services.common.bookmark.IBookmarkService;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.bookmark.menu.ActivateBookmarkKeyStroke;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkData;
import org.eclipse.scout.rt.shared.services.common.bookmark.BookmarkFolder;
import org.eclipse.scout.rt.shared.services.common.bookmark.IBookmarkStorageService;
import org.eclipse.scout.rt.shared.services.common.bookmark.IBookmarkVisitor;
import org.eclipse.scout.service.AbstractService;
import org.eclipse.scout.service.SERVICES;

/**
 * Client side service for bookmark support Uses the server side
 * {@link org.eclipse.scout.rt.client.ui.desktop.bookmark.IBookmarkStorageService} for data persistence
 */
@Priority(-3)
public class BookmarkService extends AbstractService implements IBookmarkService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BookmarkService.class);

  private final EventListenerList m_listenerList = new EventListenerList();
  private final BookmarkData m_model;

  public BookmarkService() {
    m_model = new BookmarkData();
  }

  @Override
  public void initializeService() {
    super.initializeService();
    addBookmarkServiceListener(new BookmarkServiceListener() {
      public void bookmarksChanged(BookmarkServiceEvent e) {
        handleBookmarksChangedInternal(e);
      }
    });
  }

  protected void handleBookmarksChangedInternal(BookmarkServiceEvent e) {
    switch (e.getType()) {
      case BookmarkServiceEvent.TYPE_CHANGED: {
        //refresh global keystrokes
        final ArrayList<Bookmark> list = new ArrayList<Bookmark>();
        getBookmarkData().getUserBookmarks().visit(new IBookmarkVisitor() {
          public boolean visitFolder(List<BookmarkFolder> path) {
            return true;
          }

          public boolean visitBookmark(List<BookmarkFolder> path, Bookmark b) {
            if (b.getKeyStroke() != null) {
              list.add(b);
            }
            return true;
          }
        });
        if (list.size() > 0) {
          IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
          if (desktop != null) {
            ArrayList<IKeyStroke> newKeyStrokes = new ArrayList<IKeyStroke>();
            for (IKeyStroke k : desktop.getKeyStrokes()) {
              if (k instanceof ActivateBookmarkKeyStroke) {
                //remove
              }
              else {
                newKeyStrokes.add(k);
              }
            }
            for (Bookmark b : list) {
              ActivateBookmarkKeyStroke k = new ActivateBookmarkKeyStroke(b);
              k.prepareAction();
              newKeyStrokes.add(k);
            }
            desktop.setKeyStrokes(newKeyStrokes.toArray(new IKeyStroke[newKeyStrokes.size()]));
          }
        }
        break;
      }
    }
  }

  public void loadBookmarks() throws ProcessingException {
    IBookmarkStorageService storageService = SERVICES.getService(IBookmarkStorageService.class);
    importBookmarks(storageService.getBookmarkData());
  }

  public void storeBookmarks() throws ProcessingException {
    IBookmarkStorageService storageService = SERVICES.getService(IBookmarkStorageService.class);
    importBookmarks(storageService.storeBookmarkData(m_model));
  }

  public void setStartBookmark() throws ProcessingException {
    Bookmark b = ClientSyncJob.getCurrentSession().getDesktop().createBookmark();
    b.setKind(Bookmark.USER_BOOKMARK);
    m_model.getUserBookmarks().setStartupBookmark(b);
  }

  public void deleteStartBookmark() throws ProcessingException {
    m_model.getUserBookmarks().setStartupBookmark(null);
  }

  public Bookmark getStartBookmark() {
    Bookmark b = m_model.getUserBookmarks().getStartupBookmark();
    if (b == null) {
      b = m_model.getGlobalBookmarks().getStartupBookmark();
    }
    return b;
  }

  public final BookmarkData getBookmarkData() {
    return m_model;
  }

  public void activate(Bookmark b) throws ProcessingException {
    if (b != null) {
      try {
        ClientSyncJob.getCurrentSession().getDesktop().activateBookmark(b, false);
      }
      catch (Throwable t) {
        LOG.error(null, t);
      }
    }
  }

  public void addBookmarkServiceListener(BookmarkServiceListener listener) {
    m_listenerList.add(BookmarkServiceListener.class, listener);
  }

  public void removeBookmarkServiceListener(BookmarkServiceListener listener) {
    m_listenerList.remove(BookmarkServiceListener.class, listener);
  }

  private void fireBookmarksChanged() {
    BookmarkServiceEvent e = new BookmarkServiceEvent(this, BookmarkServiceEvent.TYPE_CHANGED);
    fireBookmarkSeviceEvent(e);
  }

  private void fireBookmarkSeviceEvent(BookmarkServiceEvent e) {
    EventListener[] a = m_listenerList.getListeners(BookmarkServiceListener.class);
    if (a != null) {
      for (int i = 0; i < a.length; i++) {
        ((BookmarkServiceListener) a[i]).bookmarksChanged(e);
      }
    }
  }

  private void importBookmarks(BookmarkData model) throws ProcessingException {
    m_model.setUserBookmarks(model.getUserBookmarks());
    m_model.setGlobalBookmarks(model.getGlobalBookmarks());
    fireBookmarksChanged();
  }

}