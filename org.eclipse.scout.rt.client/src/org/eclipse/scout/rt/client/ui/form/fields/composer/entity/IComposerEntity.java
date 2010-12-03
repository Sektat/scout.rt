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
package org.eclipse.scout.rt.client.ui.form.fields.composer.entity;

import java.security.Permission;
import java.util.Map;

import org.eclipse.scout.commons.beans.IPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.form.fields.composer.attribute.IComposerAttribute;

/**
 * Composer entity
 */
public interface IComposerEntity extends IPropertyObserver {

  /**
   * Initialize this entity.
   */
  void initEntity() throws ProcessingException;

  /**
   * @deprecated the id must always by the class simple name
   */
  @Deprecated
  String getId();

  /**
   * @deprecated the id must always by the class simple name
   */
  @Deprecated
  void setId(String s);

  String getIconId();

  void setIconId(String s);

  String getText();

  void setText(String s);

  IComposerAttribute[] getComposerAttributes();

  IComposerEntity[] getComposerEntities();

  IComposerEntity getParentEntity();

  void setParentEntity(IComposerEntity parent);

  /**
   * In order to avoid loop cycles, this initializer is called by the composer field to load the child entity graph,
   * that may have cycles
   * 
   * @param instanceMap
   *          map containing all previously created entities, there should be only one entity per type
   */
  void initializeChildEntities(Map<Class<? extends IComposerEntity>, IComposerEntity> instanceMap);

  Permission getVisiblePermission();

  void setVisiblePermission(Permission p);

  boolean isVisibleGranted();

  void setVisibleGranted(boolean b);

  boolean isVisible();

  void setVisible(boolean b);

}