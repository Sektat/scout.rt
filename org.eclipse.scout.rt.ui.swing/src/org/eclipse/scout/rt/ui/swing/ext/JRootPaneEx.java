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
package org.eclipse.scout.rt.ui.swing.ext;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.rt.ui.swing.SwingLayoutUtility;

/**
 * 1. Root pane with lazy size validation (minSize, maxSize) When minSize or
 * maxSize is not respected correctRootPaneSize() is called Also when
 * preferredSize is not met, correctRootPaneSize is called
 * <p>
 * 2. Fixed bug in JRootPane.RootLayout.maximumSize uses min(widths) instead of max(widths)
 * <p>
 * 3. Support for transparency by providing {@link ScreenCapture}
 */
public class JRootPaneEx extends JRootPane {
  private static final long serialVersionUID = 1L;

  private OptimisticLock m_opLock = new OptimisticLock();

  @Override
  protected LayoutManager createRootLayout() {
    return new RootLayoutEx();
  }

  @Override
  public void validate() {
    SwingLayoutUtility.invalidateSubtree(this);
    super.validate();
    try {
      if (m_opLock.acquire()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            try {
              if (m_opLock.acquire()) {
                if (getParent() != null && isVisible()) {
                  // check minSize and maxSize requirements
                  Dimension d = getSize();
                  if (d.width > 0 && d.height > 0) {
                    Dimension[] sizes = SwingLayoutUtility.getValidatedSizes(JRootPaneEx.this);
                    Dimension minMaxSize = new Dimension(
                        Math.min(Math.max(d.width, sizes[0].width), sizes[2].width),
                        Math.min(Math.max(d.height, sizes[0].height), sizes[2].height)
                        );
                    if (minMaxSize.width != d.width || minMaxSize.height != d.height || sizes[1].width != d.width || sizes[1].height != d.height) {
                      correctRootPaneSize(minMaxSize.width - d.width, minMaxSize.height - d.height, sizes[1].width - d.width, sizes[1].height - d.height);
                    }
                  }
                }
              }
            }
            finally {
              m_opLock.release();
            }
          }
        });
      }
    }
    finally {
      m_opLock.release();
    }
  }

  /**
   * @param widthDelta
   *          is the width correction (existingWidth+widthDelta=correctWidth) to
   *          meet min/max sizes
   * @param heightDelta
   *          is the height correction
   *          (existingHeight+heightDelta=correctHeight) to meet min/max sizes
   * @param preferredWidthDelta
   *          is the width correction (existingWidth+widthDelta=prefWidth) to
   *          meet preferred size
   * @param preferredHeightDelta
   *          is the height correction (existingHeight+heightDelta=prefHeight)
   *          to meet preferred size
   */
  protected void correctRootPaneSize(int widthDelta, int heightDelta, int preferredWidthDelta, int preferredHeightDelta) {
  }

  protected class RootLayoutEx extends JRootPane.RootLayout {

    private static final long serialVersionUID = 1L;

    @Override
    public Dimension maximumLayoutSize(Container target) {
      Dimension rd, mbd;
      Insets i = getInsets();
      if (menuBar != null && menuBar.isVisible()) {
        mbd = menuBar.getMaximumSize();
      }
      else {
        mbd = new Dimension(0, 0);
      }
      if (contentPane != null) {
        rd = contentPane.getMaximumSize();
      }
      else {
        rd = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE - i.top - i.bottom - mbd.height - 1);
      }
      // fixed bug here, use max (and not min)
      return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right, rd.height + mbd.height + i.top + i.bottom);
    }
  }

}