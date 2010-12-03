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
package org.eclipse.scout.rt.client.services.common.platform;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.prefs.UserScope;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.basic.filechooser.FileChooser;
import org.eclipse.scout.service.AbstractService;
import org.osgi.framework.Bundle;
import org.osgi.service.prefs.BackingStoreException;

@Priority(-1)
public class PlatformService extends AbstractService implements IPlatformService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(PlatformService.class);

  private IEclipsePreferences m_props = null;

  private Bundle getBundle() {
    return ClientSyncJob.getCurrentSession().getBundle();
  }

  public boolean setProperty(String key, String value) {
    if (m_props == null) {
      m_props = new UserScope().getNode(getBundle().getSymbolicName());
    }
    String oldValue = m_props.get(key, null);
    if (value == null) {
      m_props.remove(key);
    }
    else {
      m_props.put(key, value);
      try {
        m_props.flush();
      }
      catch (BackingStoreException e) {
        LOG.warn("storing property: " + key + "=" + value, e);
      }
    }
    return !CompareUtility.equals(oldValue, value);
  }

  public String getProperty(String key, String def) {
    if (m_props == null) {
      m_props = new UserScope().getNode(getBundle().getSymbolicName());
    }
    return m_props.get(key, def);
  }

  public String getFile() {
    return getFile(null, true);
  }

  public String getFile(String ext, boolean open) {
    return getFile(ext, open, null);
  }

  public String getFile(String ext, boolean open, String curPath) {
    return getFile(ext, open, curPath, false);
  }

  public String getFile(String ext, boolean open, String curPath, boolean folderMode) {
    if (curPath == null) {
      curPath = FileChooser.getCurrentDirectory();
      if (curPath == null) {
        try {
          curPath = Platform.getStateLocation(getBundle()).toFile().getCanonicalPath();
        }
        catch (IOException io) {
          // throw new
          // ProcessingException("PlatformService.getFile(): unable to get current-dir",
          // io);
          curPath = null;
        }
      }
    }
    File f = null;
    File[] a = new FileChooser(new File(curPath), new String[]{ext}, open).startChooser();
    if (a != null && a.length > 0) {
      f = a[0];
    }
    //
    if (f == null) {
      /* nop */
    }
    else if (f.isDirectory() != folderMode) {
      f = null;
    }
    else if (f.getAbsolutePath().indexOf("*") >= 0) {
      f = null;
    }
    //
    if (f != null) {
      FileChooser.setCurrentDirectory(f.getParent());
      return f.getAbsolutePath();
    }
    return null;
  }

}