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
package org.eclipse.scout.rt.client.ui.desktop.outline.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.IVirtualTreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.navigation.INavigationHistoryService;
import org.eclipse.scout.rt.client.ui.form.FormEvent;
import org.eclipse.scout.rt.client.ui.form.FormListener;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.ContextMap;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;
import org.eclipse.scout.service.SERVICES;

/**
 * A page containing a list of "menu" entries<br>
 * child pages are explicitly added
 */
public abstract class AbstractPageWithTable<T extends ITable> extends AbstractPage implements IPageWithTable<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractPageWithTable.class);

  private T m_table;
  private ISearchForm m_searchForm;
  private boolean m_searchRequired;
  private boolean m_searchActive;
  private boolean m_showEmptySpaceMenus;
  private boolean m_showTableRowMenus;
  private final HashMap<ITableRow, IPage> m_tableRowToPageMap = new HashMap<ITableRow, IPage>();
  private final HashMap<IPage, ITableRow> m_pageToTableRowMap = new HashMap<IPage, ITableRow>();
  private P_MyNodeListener m_myNodeListener;
  private P_ChildNodeListener m_childNodeListener;

  public AbstractPageWithTable() {
  }

  /**
   * calling the constructor with callInitializer == false means, the table won't be constructed upon init
   * but upon activation. this is a performance-optimization and especially recommended for tablepages
   * where the parent is directly another table page (and no folder- or plain page) in this case the parent page can
   * have a huge amount of child pages with a lot of tables to be constructed but never used.
   * 
   * @param callInitializer
   */
  public AbstractPageWithTable(boolean callInitializer) {
    super(callInitializer);
    if (!callInitializer) {
      callMinimalInitializer();
    }
  }

  public AbstractPageWithTable(ContextMap contextMap) {
    super(contextMap);
  }

  public AbstractPageWithTable(boolean callInitializer, ContextMap contextMap) {
    super(callInitializer, contextMap);
    if (!callInitializer) {
      callMinimalInitializer();
    }
  }

  protected void callMinimalInitializer() {
    setChildrenDirty(true);
    setLeafInternal(getConfiguredLeaf());
    setEnabledInternal(getConfiguredEnabled());
    setExpandedInternal(getConfiguredExpanded());
  }

  /*
   * Configuration
   */
  /**
   * this configuration may be overridden by another implementation (see
   * configurator)<br>
   * default is to use inner class search form
   */
  @ConfigProperty(ConfigProperty.SEARCH_FORM)
  @Order(90)
  @ConfigPropertyValue("null")
  protected Class<? extends ISearchForm> getConfiguredSearchForm() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredSearchRequired() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(110)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredShowEmptySpaceMenus() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(120)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredShowTableRowMenus() {
    return true;
  }

  /**
   * get table data (from service)
   * <p>
   * this method is only called when either {@link #getSearchFilter()}!=null or {@link #isSearchRequired()}==false
   * 
   * @param filter
   *          is guaranteed not to be null
   */
  @ConfigOperation
  @Order(90)
  protected Object[][] execLoadTableData(SearchFilter filter) throws ProcessingException {
    return null;
  }

  /**
   * Populate the table<br>
   * for most cases the override of just {@link #execLoadTableData(SearchFilter)} is sufficient
   * <p>
   * It is good practice to populate table using ITable.replaceRows() instead of ITable.removeAllRows();
   * ITable.addRows() because in the former case the tree structure below the changing rows is not discarded but only
   * marked as dirty.<br>
   * The subtree is lazily reloaded when the user clicks next time on a child node
   */
  @ConfigOperation
  @Order(100)
  protected void execPopulateTable() throws ProcessingException {
    ensureSearchFormStarted();
    if (isSearchActive()) {
      SearchFilter filter = getSearchFilter();
      if (filter.isCompleted() || !isSearchRequired()) {
        // create a copy of the filter, just in case the subprocess is modifying
        // or extending the filter
        filter = (SearchFilter) filter.clone();
        //do NOT reference the result data object and warp it into a ref, so the processor is allowed to delete the contents to free up memory sooner
        getTable().replaceRowsByMatrix(new AtomicReference<Object>(execLoadTableData(filter)));
      }
    }
    else {
      // searchFilter should never be null
      //do NOT reference the result data object and warp it into a ref, so the processor is allowed to delete the contents to free up memory sooner
      getTable().replaceRowsByMatrix(new AtomicReference<Object>(execLoadTableData(new SearchFilter())));
    }
  }

  /**
   * create a child page for every table row that was added to the table
   */
  @ConfigOperation
  @Order(110)
  protected IPage execCreateChildPage(ITableRow row) throws ProcessingException {
    return null;
  }

  /**
   * create a virtual child page for every table row that was added to the table
   * The virtual page is tranformed (resolved) into the correct page when it is first time activated or selected.�
   * <p>
   * This saves resources, memory and improves performance
   */
  @ConfigOperation
  @Order(111)
  protected IPage execCreateVirtualChildPage(ITableRow row) throws ProcessingException {
    if (ConfigurationUtility.isMethodOverwrite(AbstractPageWithTable.class, "execCreateChildPage", new Class[]{ITableRow.class}, AbstractPageWithTable.this.getClass())) {
      return new VirtualPage();
    }
    return null;
  }

  @Override
  protected ITreeNode execResolveVirtualChildNode(IVirtualTreeNode node) throws ProcessingException {
    ITableRow row = getTableRowFor(node);
    if (row == null) {
      return null;
    }
    //remove old association
    unlinkTableRowWithPage(row);
    //add new association
    IPage childPage = execCreateChildPage(row);
    if (childPage != null) {
      ICell tableCell = m_table.getSummaryCell(row);
      childPage.getCellForUpdate().updateFrom(tableCell);
      linkTableRowWithPage(row, childPage);
    }
    return childPage;
  }

  private Class<? extends ITable> getConfiguredTable() {
    Class<?>[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.filterClass(dca, ITable.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void initConfig() {
    super.initConfig();
    m_searchActive = true;
    setSearchRequired(getConfiguredSearchRequired());
    setShowEmptySpaceMenus(getConfiguredShowEmptySpaceMenus());
    setShowTableRowMenus(getConfiguredShowTableRowMenus());
    Class<? extends ITable> tableClass = getConfiguredTable();
    if (tableClass != null) {
      try {
        m_table = (T) ConfigurationUtility.newInnerInstance(this, tableClass);
        m_table.addTableListener(new P_TableListener());
        m_table.setEnabled(isEnabled());
        m_table.setAutoDiscardOnDelete(true);
        m_table.setUserPreferenceContext(getBookmarkIdentifier());
        m_table.initTable();
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    // legacy-support for searchform-inner classes
    if (getConfiguredSearchForm() == null) {
      Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
      Class<? extends ISearchForm> searchFormClass = ConfigurationUtility.filterClass(dca, ISearchForm.class);
      if (searchFormClass != null) {
        LOG.warn("inner searchforms are deprecated...");
        try {
          m_searchForm = ConfigurationUtility.newInnerInstance(this, searchFormClass);
        }
        catch (Exception e) {
          LOG.warn(null, e);
        }
        initSearchFormInternal();
      }
    }
  }

  private void initSearchFormInternal() {
    m_searchForm.setDisplayHint(ISearchForm.DISPLAY_HINT_VIEW);
    if (m_searchForm.getDisplayViewId() == null) {
      m_searchForm.setDisplayViewId(IForm.VIEW_ID_PAGE_SEARCH);
    }
    m_searchForm.setAutoAddRemoveOnDesktop(false);
    // listen for search action
    m_searchForm.addFormListener(new FormListener() {
      public void formChanged(FormEvent e) throws ProcessingException {
        switch (e.getType()) {
          case FormEvent.TYPE_STORE_BEFORE: {
            IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
            IPage page = AbstractPageWithTable.this;
            if (desktop != null && desktop.getOutline() != null && desktop.getOutline().getActivePage() == page) {
              SERVICES.getService(INavigationHistoryService.class).addStep(0, page.getCell().getText(), page.getCell().getIconId());
            }
            break;
          }
          case FormEvent.TYPE_STORE_AFTER: {
            try {
              reloadPage();
            }
            catch (ProcessingException ex) {
              if (ex.isInterruption()) {
                // nop
              }
              else {
                SERVICES.getService(IExceptionHandlerService.class).handleException(ex);
              }
            }
            break;
          }
        }
      }
    });
    try {
      execInitSearchForm();
    }
    catch (Exception e) {
      LOG.warn(null, e);
    }
  }

  /**
   * Initialize the search form.
   * <p>
   * This method is invoked when the search form is used for the first time. Hence implement all initialization code for
   * the search form herein.
   * <p>
   * If the search form is defined as inner class then this method is called when the page is initialized. Otherwise it
   * is invoked when the search form is used for the first time.
   * 
   * @see #ensureSearchFormStarted()
   */
  @ConfigOperation
  @Order(120)
  protected void execInitSearchForm() throws ProcessingException {
  }

  /**
   * Ensures that the search form is initialized and started, if one is defined for this table at all. This allows lazy
   * initialization of search forms.
   */
  protected void ensureSearchFormStarted() {
    if (m_searchForm == null && getConfiguredSearchForm() != null) {
      // there is no search form, but should be
      try {
        setSearchForm(getConfiguredSearchForm().newInstance());
        initSearchFormInternal();
        m_searchForm.startSearch();
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
  }

  public final T getTable() {
    if (m_table == null) {
      ensureInitialized();
    }
    return m_table;
  }

  public boolean isShowEmptySpaceMenus() {
    return m_showEmptySpaceMenus;
  }

  public void setShowEmptySpaceMenus(boolean showEmptySpaceMenus) {
    m_showEmptySpaceMenus = showEmptySpaceMenus;
  }

  public boolean isShowTableRowMenus() {
    return m_showTableRowMenus;
  }

  public void setShowTableRowMenus(boolean showTableRowMenus) {
    m_showTableRowMenus = showTableRowMenus;
  }

  public ISearchForm getSearchFormInternal() {
    ensureSearchFormStarted();
    return m_searchForm;
  }

  public void setSearchForm(ISearchForm searchForm) {
    m_searchForm = searchForm;
    ensureSearchFormStarted();
  }

  public SearchFilter getSearchFilter() {
    ensureSearchFormStarted();
    if (getSearchFormInternal() != null) {
      return getSearchFormInternal().getSearchFilter();
    }
    else {
      return new SearchFilter();
    }
  }

  public boolean isSearchRequired() {
    return m_searchRequired;
  }

  public void setSearchRequired(boolean b) {
    m_searchRequired = b;
  }

  @Override
  public void setEnabled(boolean b) {
    super.setEnabled(b);
    if (m_table != null) {
      m_table.setEnabled(isEnabled());
    }
  }

  public boolean isSearchActive() {
    return m_searchActive;
  }

  public void setSearchActive(boolean b) {
    m_searchActive = b;
    if (isSelectedNode()) {
      getOutline().setSearchForm(m_searchActive ? getSearchFormInternal() : null);
    }
  }

  @Override
  public void pageActivatedNotify() {
    ensureInitialized();
    ensureSearchFormStarted();
    super.pageActivatedNotify();
  }

  /**
   * override to add/remove local tree listener
   */
  @Override
  public void setTreeInternal(ITree tree, boolean includeSubtree) {
    if (getTree() != null && m_myNodeListener != null) {
      getTree().removeTreeListener(m_myNodeListener);
      m_myNodeListener = null;
    }
    if (getTree() != null && m_childNodeListener != null) {
      getTree().removeTreeListener(m_childNodeListener);
      m_childNodeListener = null;
    }
    super.setTreeInternal(tree, includeSubtree);
    if (getTree() != null) {
      m_myNodeListener = new P_MyNodeListener();
      getTree().addTreeListener(m_myNodeListener);
    }
    if (getTree() != null) {
      m_childNodeListener = new P_ChildNodeListener();
      getTree().addTreeListener(m_childNodeListener);
    }
  }

  /**
   * load table data
   */
  private void loadTableDataImpl() throws ProcessingException {
    ISearchForm searchForm = getSearchFormInternal();
    if (m_table != null) {
      try {
        m_table.setTableChanging(true);
        //
        execPopulateTable();
        // set minimized status of search form
        if (searchForm != null) {
          searchForm.setMinimized(getTable().getRowCount() > 0);
        }
      }
      finally {
        m_table.setTableChanging(false);
      }
    }
  }

  /**
   * load tree children<br>
   * this method delegates to the table reload<br>
   * when the table is loaded and this node is not a leaf node then the table
   * rows are mirrored in child nodes
   */
  @Override
  public final void loadChildren() throws ProcessingException {
    ITree tree = getTree();
    try {
      if (tree != null) tree.setTreeChanging(true);
      //
      // backup currently selected tree node and its path to root
      boolean oldSelectionOwned = false;
      int oldSelectionDirectChildIndex = -1;
      ITreeNode oldSelectedNode = null;
      if (tree != null) oldSelectedNode = tree.getSelectedNode();
      Object[] oldSelectedRowKeys = null;
      if (oldSelectedNode != null) {
        ITreeNode t = oldSelectedNode;
        while (t != null && t.getParentNode() != null) {
          if (t.getParentNode() == this) {
            oldSelectionOwned = true;
            oldSelectedRowKeys = getTableRowFor(t).getKeyValues();
            oldSelectionDirectChildIndex = t.getChildNodeIndex();
            break;
          }
          t = t.getParentNode();
        }
      }
      //
      setChildrenLoaded(false);
      ClientSyncJob.getCurrentSession().getMemoryPolicy().beforeTablePageLoadData(this);
      try {
        loadTableDataImpl();
      }
      catch (ProcessingException pe) {
        if (!pe.isInterruption()) {
          throw pe;
        }
      }
      finally {
        ClientSyncJob.getCurrentSession().getMemoryPolicy().afterTablePageLoadData(this);
      }
      setChildrenLoaded(true);
      setChildrenDirty(false);
      // table events will handle automatic tree changes in case table is
      // mirrored in tree.
      // restore currently selected tree node when it was owned by our table
      // rows.
      // in case selection was lost, try to select similar index as before
      if (tree != null && oldSelectionOwned && tree.getSelectedNode() == null) {
        ITableRow row = getTable().getSelectedRow();
        if (row != null) {
          tree.selectNode(getTreeNodeFor(row));
        }
        else {
          row = getTable().findRowByKey(oldSelectedRowKeys);
          if (row != null) {
            tree.selectNode(getTreeNodeFor(row));
          }
          else if (oldSelectedNode != null && oldSelectedNode.getTree() == tree) {
            tree.selectNode(oldSelectedNode);
          }
          else {
            int index = Math.max(-1, Math.min(oldSelectionDirectChildIndex, getChildNodeCount() - 1));
            if (index >= 0 && index < getChildNodeCount()) {
              tree.selectNode(getChildNode(index));
            }
            else {
              tree.selectNode(this);
            }
          }
        }
      }
    }
    finally {
      if (tree != null) tree.setTreeChanging(false);
    }
  }

  private void linkTableRowWithPage(ITableRow tableRow, IPage page) {
    m_tableRowToPageMap.put(tableRow, page);
    m_pageToTableRowMap.put(page, tableRow);
  }

  private void unlinkTableRowWithPage(ITableRow tableRow) {
    IPage page = m_tableRowToPageMap.remove(tableRow);
    if (page != null) {
      m_pageToTableRowMap.remove(page);
    }
  }

  /**
   * Computes the list of linked child pages for the given table rows and updates their summary cell.
   */
  private IPage[] getUpdatedChildPagesFor(ITableRow[] tableRows) {
    return getChildPagesFor(tableRows, true);
  }

  /**
   * Computes the list of linked child pages for the given table rows. Revalidates the the pages cell
   * if <code>updateChildPageCells</code> is true. Otherwise, the cells are not updated.
   */
  private IPage[] getChildPagesFor(ITableRow[] tableRows, boolean updateChildPageCells) {
    IPage[] pages = new IPage[tableRows.length];
    int missingCount = 0;
    try {
      for (int i = 0; i < tableRows.length; i++) {
        pages[i] = m_tableRowToPageMap.get(tableRows[i]);
        if (pages[i] != null) {
          if (updateChildPageCells) {
            // update tree nodes from table rows
            ICell tableCell = getTable().getSummaryCell(tableRows[i]);
            pages[i].setEnabledInternal(tableRows[i].isEnabled());
            pages[i].getCellForUpdate().updateFrom(tableCell);
          }
        }
        else {
          missingCount++;
        }
      }
    }
    catch (ProcessingException e) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
    if (missingCount > 0) {
      IPage[] tmp = new IPage[pages.length - missingCount];
      int index = 0;
      for (IPage element : pages) {
        if (element != null) {
          tmp[index] = element;
          index++;
        }
      }
      pages = tmp;
    }
    return pages;
  }

  public ITreeNode getTreeNodeFor(ITableRow tableRow) {
    if (tableRow == null) {
      return null;
    }
    else {
      return m_tableRowToPageMap.get(tableRow);
    }
  }

  public ITableRow getTableRowFor(ITreeNode childPageNode) {
    return m_pageToTableRowMap.get(childPageNode);
  }

  public ITableRow[] getTableRowsFor(ITreeNode[] childPageNodes) {
    ITableRow[] rows = new ITableRow[childPageNodes.length];
    int missingCount = 0;
    for (int i = 0; i < childPageNodes.length; i++) {
      rows[i] = m_pageToTableRowMap.get(childPageNodes[i]);
      if (rows[i] == null) {
        missingCount++;
      }
    }
    if (missingCount > 0) {
      ITableRow[] tmp = new ITableRow[rows.length - missingCount];
      int index = 0;
      for (ITableRow element : rows) {
        if (element != null) {
          tmp[index] = element;
          index++;
        }
      }
      rows = tmp;
    }
    return rows;
  }

  /**
   * Table listener and tree controller<br>
   * the table is reflected in tree children only if the tree/page node is not
   * marked as being a leaf
   */
  private class P_TableListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent e) {
      switch (e.getType()) {
        case TableEvent.TYPE_ROW_ACTION: {
          if (!e.isConsumed()) {
            ITreeNode node = getTreeNodeFor(e.getFirstRow());
            if (node != null) {
              e.consume();
              if (getTree() != null) {
                getTree().getUIFacade().setNodeSelectedAndExpandedFromUI(node);
              }
            }
          }
          break;
        }
        case TableEvent.TYPE_ALL_ROWS_DELETED:
        case TableEvent.TYPE_ROWS_DELETED: {
          if (!isLeaf()) {
            ITableRow[] tableRows = e.getRows();
            IPage[] childNodes = getChildPagesFor(tableRows, false);
            for (int i = 0; i < childNodes.length; i++) {
              unlinkTableRowWithPage(tableRows[i]);
            }
            if (getTree() != null) {
              getTree().removeChildNodes(AbstractPageWithTable.this, childNodes);
            }
          }
          break;
        }
        case TableEvent.TYPE_ROWS_INSERTED: {
          if (!isLeaf()) {
            ArrayList<IPage> childPageList = new ArrayList<IPage>();
            ITableRow[] tableRows = e.getRows();
            for (ITableRow element : tableRows) {
              try {
                IPage childPage = execCreateVirtualChildPage(element);
                if (childPage != null) {
                  ICell tableCell = m_table.getSummaryCell(element);
                  childPage.getCellForUpdate().updateFrom(tableCell);
                  linkTableRowWithPage(element, childPage);
                  childPageList.add(childPage);
                }
              }
              catch (ProcessingException ex) {
                SERVICES.getService(IExceptionHandlerService.class).handleException(ex);
              }
              catch (Throwable t) {
                SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Page " + element, t));
              }
            }
            IPage[] childPages = childPageList.toArray(new IPage[childPageList.size()]);
            if (getTree() != null) {
              getTree().addChildNodes(AbstractPageWithTable.this, childPages);
              // check if a page was revoked
              for (ITableRow element : tableRows) {
                IPage page = m_tableRowToPageMap.get(element);
                if (page != null && page.getParentNode() == null) {
                  unlinkTableRowWithPage(element);
                }
              }
            }
          }
          break;
        }
        case TableEvent.TYPE_ROWS_UPDATED: {
          if (!isLeaf()) {
            IPage[] childNodes = getUpdatedChildPagesFor(e.getRows());
            if (getTree() != null) {
              getTree().updateChildNodes(AbstractPageWithTable.this, childNodes);
            }
          }
          break;
        }
        case TableEvent.TYPE_ROW_ORDER_CHANGED: {
          if (!isLeaf()) {
            IPage[] childNodes = getUpdatedChildPagesFor(e.getRows());
            if (getTree() != null) {
              getTree().updateChildNodeOrder(AbstractPageWithTable.this, childNodes);
            }
          }
          break;
        }
        case TableEvent.TYPE_ROWS_SELECTED: {
          break;
        }
        case TableEvent.TYPE_ROW_FILTER_CHANGED: {
          if (!isLeaf()) {
            if (getTree() != null) {
              getTree().applyNodeFilters();
            }
          }
          break;
        }
      }// end switch
    }
  }

  /**
   * Tree listener on myself in order to load children when selected and add
   * "new" menus
   */
  private class P_MyNodeListener extends TreeAdapter {
    @Override
    public void treeChanged(TreeEvent e) {
      switch (e.getType()) {
        case TreeEvent.TYPE_NODE_POPUP: {
          if (e.getNode() == AbstractPageWithTable.this) {
            if (isShowEmptySpaceMenus()) {
              IMenu[] menus = m_table.getUIFacade().fireEmptySpacePopupFromUI();
              if (menus != null) {
                e.addPopupMenus(menus);
              }
            }
          }
          break;
        }
      }// end switch
    }
  }

  /**
   * Tree listener on children in order to delegate events to table rows
   */
  private class P_ChildNodeListener extends TreeAdapter {
    @Override
    public void treeChanged(TreeEvent e) {
      switch (e.getType()) {
        case TreeEvent.TYPE_NODE_POPUP: {
          if (e.getCommonParentNode() == AbstractPageWithTable.this) {
            if (isShowTableRowMenus()) {
              ITableRow row = getTableRowFor(e.getNode());
              if (row != null) {
                m_table.getUIFacade().setSelectedRowsFromUI(new ITableRow[]{row});
                IMenu[] menus = m_table.getUIFacade().fireRowPopupFromUI();
                if (menus != null) {
                  e.addPopupMenus(menus);
                }
              }
            }
          }
          break;
        }
        case TreeEvent.TYPE_NODE_ACTION: {
          if (!e.isConsumed()) {
            if (e.getCommonParentNode() == AbstractPageWithTable.this) {
              ITableRow row = getTableRowFor(e.getNode());
              if (row != null) {
                e.consume();
                /*
                 * ticket 78684: this line added
                 */
                m_table.getUIFacade().setSelectedRowsFromUI(new ITableRow[]{row});
                m_table.getUIFacade().fireRowActionFromUI(row);
              }
            }
          }
          break;
        }
        case TreeEvent.TYPE_NODES_DRAG_REQUEST: {
          if (e.getCommonParentNode() == AbstractPageWithTable.this) {
            ITableRow[] rows = getTableRowsFor(e.getNodes());
            m_table.getUIFacade().setSelectedRowsFromUI(rows);
            TransferObject t = m_table.getUIFacade().fireRowsDragRequestFromUI();
            if (t != null) {
              e.setDragObject(t);
            }
          }
          break;
        }
        case TreeEvent.TYPE_NODE_DROP_ACTION: {
          if (e.getCommonParentNode() == AbstractPageWithTable.this) {
            ITableRow row = getTableRowFor(e.getNode());
            if (row != null) {
              m_table.getUIFacade().fireRowDropActionFromUI(row, e.getDropObject());
            }
          }
          break;
        }
      }// end switch
    }
  }
}