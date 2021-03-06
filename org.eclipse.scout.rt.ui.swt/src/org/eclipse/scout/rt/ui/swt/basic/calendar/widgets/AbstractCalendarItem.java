package org.eclipse.scout.rt.ui.swt.basic.calendar.widgets;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.scout.rt.ui.swt.basic.calendar.CalendarItemContainer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

/**
 * Base class of a week or month calendar item.
 * 
 * @author Michael Rudolf, Andreas Hoegger
 */
public abstract class AbstractCalendarItem extends Composite implements PaintListener, MouseTrackListener {

  /** model calendar item */
  private CalendarItemContainer m_item;

  /** reference to parent cell */
  private AbstractCell m_cell;

  /** manager for context menu regarding this cell */
  private MenuManager m_menuManager;

  public AbstractCalendarItem(AbstractCell parent, int style, CalendarItemContainer item) {
    super(parent, style);

    // stores the (model) calendar item container associated with this graphical item
    m_item = item;

    // stores a ref to the parent cell/composite
    m_cell = parent;

    createControls();
    setLayout();
    setupMenu();
    hookListeners();
  }

  protected CalendarItemContainer getItem() {
    return m_item;
  }

  protected void setItem(CalendarItemContainer item) {
    m_item = item;
  }

  protected AbstractCell getCell() {
    return m_cell;
  }

  protected void setCell(AbstractCell cell) {
    m_cell = cell;
  }

  protected void createControls() {

  }

  protected void setLayout() {

  }

  private void setupMenu() {
    // create context menu (dynamic, gets filled when used)
    m_menuManager = new MenuManager();
    m_menuManager.setRemoveAllWhenShown(true);
    Menu contextMenu = m_menuManager.createContextMenu(this);
    this.setMenu(contextMenu);
  }

  protected void hookListeners() {
    addPaintListener(this);

    // menu listener for context menu
    m_menuManager.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(IMenuManager manager) {
        m_cell.getCalendar().showItemContextMenu(manager, m_item.getItem());
      }
    });

    // intercept a mouse click
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        m_cell.getCalendar().setSelectedDateFromUI(m_cell.getDate());
        m_cell.getCalendar().setSelectedItem(m_item.getItem());
      }
    });

    // for tooltip
    addMouseTrackListener(this);
  }

  // -- PaintListener

  /** needs to be overriden */
  @Override
  public void paintControl(PaintEvent e) {
  }

  // -- MouseTrackListener

  @Override
  public void mouseHover(MouseEvent e) {
    setToolTipText(m_cell.getCalendar().getModel().getTooltip(m_item.getItem(), m_cell.getDate().getTime()));
  }

  @Override
  public void mouseEnter(MouseEvent e) {
  }

  @Override
  public void mouseExit(MouseEvent e) {
    setToolTipText("");
  }

  @Override
  public String toString() {
    // text
    String s = m_cell.getCalendar().getModel().getLabel(m_item.getItem(), m_cell.getDate().getTime());

    if (this instanceof WeekCalendarItem) {
      return "WeekCalendarItem {" + s + "}";
    }
    else {
      return "MonthCalendarItem {" + s + "}";
    }

  }
}
