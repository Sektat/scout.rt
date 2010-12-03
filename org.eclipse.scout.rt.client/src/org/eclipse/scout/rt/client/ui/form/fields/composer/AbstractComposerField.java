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
package org.eclipse.scout.rt.client.ui.form.fields.composer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.xmlparser.SimpleXmlElement;
import org.eclipse.scout.rt.client.ui.action.keystroke.AbstractKeyStroke;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.tree.AbstractTree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeVisitor;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeAdapter;
import org.eclipse.scout.rt.client.ui.basic.tree.TreeEvent;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.client.ui.form.fields.composer.attribute.IComposerAttribute;
import org.eclipse.scout.rt.client.ui.form.fields.composer.entity.IComposerEntity;
import org.eclipse.scout.rt.client.ui.form.fields.composer.internal.ComposerDisplayTextBuilder;
import org.eclipse.scout.rt.client.ui.form.fields.composer.internal.LegacyComposerStatementBuilder;
import org.eclipse.scout.rt.client.ui.form.fields.composer.node.AttributeNode;
import org.eclipse.scout.rt.client.ui.form.fields.composer.node.EitherOrNode;
import org.eclipse.scout.rt.client.ui.form.fields.composer.node.EntityNode;
import org.eclipse.scout.rt.client.ui.form.fields.composer.node.RootNode;
import org.eclipse.scout.rt.client.ui.form.fields.composer.operator.ComposerOp;
import org.eclipse.scout.rt.client.ui.form.fields.composer.operator.IComposerOp;
import org.eclipse.scout.rt.shared.AbstractIcons;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractFormFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.AbstractComposerAttributeData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.AbstractComposerData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.AbstractComposerEntityData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerAttributeNodeData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerConstants;
import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerEitherOrNodeData;
import org.eclipse.scout.rt.shared.data.form.fields.composer.ComposerEntityNodeData;
import org.eclipse.scout.rt.shared.data.form.fields.treefield.AbstractTreeFieldData;
import org.eclipse.scout.rt.shared.data.form.fields.treefield.TreeNodeData;
import org.eclipse.scout.rt.shared.services.common.jdbc.LegacySearchFilter;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

@SuppressWarnings("deprecation")
public abstract class AbstractComposerField extends AbstractFormField implements IComposerField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractComposerField.class);

  private IComposerFieldUIFacade m_uiFacade;
  private ITree m_tree;
  private SimpleXmlElement m_initValue;
  private ArrayList<IComposerAttribute> m_attributes;
  private ArrayList<IComposerEntity> m_entities;

  public AbstractComposerField() {
    super();
  }

  /*
   * Configuration
   */
  private Class<? extends ITree> getConfiguredTree() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class<? extends ITree>[] f = ConfigurationUtility.filterClasses(dca, ITree.class);
    if (f.length == 1) return f[0];
    else {
      for (Class<? extends ITree> c : f) {
        if (c.getDeclaringClass() != AbstractComposerField.class) {
          return c;
        }
      }
      return null;
    }
  }

  private Class<? extends IComposerAttribute>[] getConfiguredComposerAttributes() {
    Class[] c = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.sortFilteredClassesByOrderAnnotation(c, IComposerAttribute.class);
  }

  private Class<? extends IComposerEntity>[] getConfiguredComposerEntities() {
    Class[] c = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.sortFilteredClassesByOrderAnnotation(c, IComposerEntity.class);
  }

  /**
   * Override this method to decorate or enhance new nodes whenever they are created
   * 
   * @return the new node, must not be null
   *         <p>
   *         Normally overrides call super. {@link #execCreateRootNode()}
   */
  @ConfigOperation
  @Order(100)
  protected RootNode execCreateRootNode() {
    return new RootNode(this);
  }

  /**
   * Override this method to decorate or enhance new nodes whenever they are created
   * 
   * @return the new node or null to ignore the add of a new node of this type
   *         <p>
   *         Normally overrides call super.
   *         {@link #execCreateEntityNode(ITreeNode, IComposerEntity, boolean, Object[], String[])}
   */
  @ConfigOperation
  @Order(110)
  protected EntityNode execCreateEntityNode(ITreeNode parentNode, IComposerEntity e, boolean negated, Object[] values, String[] texts) {
    EntityNode node = new EntityNode(this);
    node.setEntity(e);
    node.setValues(values);
    node.setTexts(texts);
    node.setNegative(negated);
    node.setStatus(ITreeNode.STATUS_INSERTED);
    return node;
  }

  /**
   * Override this method to decorate or enhance new nodes whenever they are created
   * 
   * @return the new node or null to ignore the add of a new node of this type
   *         <p>
   *         Normally overrides call super.
   *         {@link #execCreateAttributeNode(ITreeNode, IComposerAttribute, Integer, IComposerOp, Object[], String[])}
   */
  @ConfigOperation
  @Order(120)
  protected AttributeNode execCreateAttributeNode(ITreeNode parentNode, IComposerAttribute a, Integer aggregationType, IComposerOp op, Object[] values, String[] texts) {
    if (aggregationType != null && aggregationType == ComposerConstants.AGGREGATION_NONE) {
      aggregationType = null;
    }
    AttributeNode node = new AttributeNode(this);
    node.setAttribute(a);
    node.setAggregationType(aggregationType);
    node.setOp(op);
    node.setValues(values);
    node.setTexts(texts);
    node.setStatus(ITreeNode.STATUS_INSERTED);
    return node;
  }

  /**
   * Override this method to decorate or enhance new nodes whenever they are created
   * 
   * @return the new node or null to ignore the add of a new node of this type
   *         <p>
   *         Normally overrides call super.{@link #execCreateEitherNode(ITreeNode, boolean)}
   */
  @ConfigOperation
  @Order(130)
  protected EitherOrNode execCreateEitherNode(ITreeNode parentNode, boolean negated) {
    EitherOrNode node = new EitherOrNode(this, true);
    node.setNegative(negated);
    node.setStatus(ITreeNode.STATUS_INSERTED);
    return node;
  }

  /**
   * Override this method to decorate or enhance new nodes whenever they are created
   * 
   * @return the new node or null to ignore the add of a new node of this type
   *         <p>
   *         Normally overrides call super.{@link #execCreateAdditionalOrNode(ITreeNode, boolean)}
   */
  @ConfigOperation
  @Order(140)
  protected EitherOrNode execCreateAdditionalOrNode(ITreeNode parentNode, boolean negated) {
    EitherOrNode node = new EitherOrNode(this, false);
    node.setNegative(negated);
    node.setStatus(ITreeNode.STATUS_INSERTED);
    return node;
  }

  public Map<String, String> getMetaDataOfAttribute(IComposerAttribute a) {
    return null;
  }

  public Map<String, String> getMetaDataOfAttributeData(AbstractComposerAttributeData a, Object[] values) {
    return null;
  }

  @Override
  protected void initConfig() {
    m_uiFacade = new P_UIFacade();
    super.initConfig();
    // attributes
    m_attributes = new ArrayList<IComposerAttribute>();
    for (Class<? extends IComposerAttribute> c : getConfiguredComposerAttributes()) {
      try {
        IComposerAttribute a = ConfigurationUtility.newInnerInstance(this, c);
        m_attributes.add(a);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    for (IComposerAttribute a : m_attributes) {
      a.setParentEntity(null);
    }
    // entities
    m_entities = new ArrayList<IComposerEntity>();
    for (Class<? extends IComposerEntity> c : getConfiguredComposerEntities()) {
      try {
        IComposerEntity e = ConfigurationUtility.newInnerInstance(this, c);
        m_entities.add(e);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    HashMap<Class<? extends IComposerEntity>, IComposerEntity> instanceMap = new HashMap<Class<? extends IComposerEntity>, IComposerEntity>();
    for (IComposerEntity e : m_entities) {
      e.setParentEntity(null);
      instanceMap.put(e.getClass(), e);
    }
    for (IComposerEntity e : m_entities) {
      e.initializeChildEntities(instanceMap);
    }
    // tree
    if (getConfiguredTree() != null) {
      try {
        m_tree = ConfigurationUtility.newInnerInstance(this, getConfiguredTree());
        RootNode rootNode = execCreateRootNode();
        rootNode.getCellForUpdate().setText(getLabel());
        m_tree.setRootNode(rootNode);
        m_tree.setNodeExpanded(rootNode, true);
        m_tree.setEnabled(isEnabled());
        m_tree.addTreeListener(
            new TreeAdapter() {
              @Override
              public void treeChanged(TreeEvent e) {
                switch (e.getType()) {
                  case TreeEvent.TYPE_NODES_DELETED:
                          case TreeEvent.TYPE_NODES_INSERTED:
                          case TreeEvent.TYPE_NODES_UPDATED: {
                          checkSaveNeeded();
                          checkEmpty();
                          break;
                        }
                      }
                    }
            }
            );
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    else {
      LOG.warn("there is no inner class of type ITree in " + getClass());
    }
    // local enabled listener
    addPropertyChangeListener(PROP_ENABLED, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (m_tree != null) {
          m_tree.setEnabled(isEnabled());
        }
      }
    });
  }

  /*
   * Runtime
   */
  @Override
  protected void initFieldInternal() throws ProcessingException {
    getTree().initTree();
    for (IComposerEntity e : getComposerEntities()) {
      e.initEntity();
    }
    for (IComposerAttribute a : getComposerAttributes()) {
      a.initAttribute();
    }
    super.initFieldInternal();
  }

  @Override
  protected void disposeFieldInternal() {
    super.disposeFieldInternal();
    getTree().disposeTree();
  }

  @Override
  protected void applySearchInternal(SearchFilter search) {
    ITreeNode rootNode = getTree().getRootNode();
    if (rootNode != null) {
      StringBuffer buf = new StringBuffer();
      new ComposerDisplayTextBuilder().build(rootNode, buf, "");
      String s = buf.toString();
      if (s.trim().length() > 0) {
        search.addDisplayText(s);
      }
    }
    if (search instanceof LegacySearchFilter) {
      LegacySearchFilter l = (LegacySearchFilter) search;
      if (rootNode != null) {
        Object specialConstraint = new LegacyComposerStatementBuilder(l.getBindMap()).build(rootNode);
        if (specialConstraint != null) {
          try {
            l.addSpecialWhereToken(specialConstraint);
          }
          catch (ProcessingException e) {
            LOG.error("adding legacy search filter", e);
          }
        }
      }
    }
  }

  public final ITree getTree() {
    return m_tree;
  }

  @Override
  public void exportFormFieldData(AbstractFormFieldData target) throws ProcessingException {
    AbstractTreeFieldData treeFieldData = (AbstractTreeFieldData) target;
    if (m_tree != null) {
      m_tree.exportTreeData(treeFieldData);
    }
  }

  @Override
  public void importFormFieldData(AbstractFormFieldData source, boolean valueChangeTriggersEnabled) throws ProcessingException {
    AbstractTreeFieldData treeFieldData = (AbstractTreeFieldData) source;
    if (treeFieldData.isValueSet()) {
      if (m_tree != null) {
        try {
          if (!valueChangeTriggersEnabled) setValueChangeTriggerEnabled(false);
          //
          m_tree.importTreeData(treeFieldData);
        }
        finally {
          if (!valueChangeTriggersEnabled) setValueChangeTriggerEnabled(true);
        }
      }
    }
  }

  public IComposerAttribute[] getComposerAttributes() {
    return m_attributes.toArray(new IComposerAttribute[0]);
  }

  public IComposerEntity[] getComposerEntities() {
    return m_entities.toArray(new IComposerEntity[0]);
  }

  @Override
  public void loadXML(SimpleXmlElement x) throws ProcessingException {
    super.loadXML(x);
    ITree tree = getTree();
    try {
      tree.setTreeChanging(true);
      //
      getTree().removeAllChildNodes(getTree().getRootNode());
      loadXMLRec(x, getTree().getRootNode());
    }
    finally {
      tree.setTreeChanging(false);
    }
  }

  private void loadXMLRec(SimpleXmlElement x, ITreeNode parent) {
    // build tree
    for (SimpleXmlElement xmlElem : x.getChildren()) {
      if ("attribute".equals(xmlElem.getName())) {
        String id = xmlElem.getStringAttribute("id");
        IComposerOp op;
        Integer aggregationType;
        try {
          op = ComposerOp.create(xmlElem.getIntAttribute("op", ComposerConstants.OPERATOR_EQ));
          aggregationType = xmlElem.getIntAttribute("aggregationType", 0);
          if (aggregationType == 0) {
            aggregationType = null;
          }
        }
        catch (Exception e) {
          LOG.warn("read op", e);
          continue;
        }
        ArrayList<Object> valueList = new ArrayList<Object>();
        try {
          for (int i = 1; i <= 5; i++) {
            String valueName = (i == 1 ? "value" : "value" + i);
            if (xmlElem.hasAttribute(valueName)) {
              valueList.add(xmlElem.getObjectAttribute(valueName, null));
            }
          }
        }
        catch (Exception e) {
          LOG.warn("read value for attribute " + id, e);
          continue;
        }
        ArrayList<String> displayValueList = new ArrayList<String>();
        for (int i = 1; i <= 5; i++) {
          String displayValueName = (i == 1 ? "displayValue" : "displayValue" + i);
          if (xmlElem.hasAttribute(displayValueName)) {
            displayValueList.add(xmlElem.getStringAttribute(displayValueName, null));
          }
        }
        // find definition
        IComposerAttribute foundAtt = ComposerFieldUtility.externalIdToAttribute(this, id, null);
        if (foundAtt == null) {
          LOG.warn("cannot find attribute with id=" + id);
          continue;
        }
        ITreeNode node = addAttributeNode(parent, foundAtt, aggregationType, op, valueList.toArray(), displayValueList.toArray(new String[1]));
        if (node != null) {
          // add children recursive
          loadXMLRec(xmlElem, node);
        }
      }
      else if ("entity".equals(xmlElem.getName())) {
        String id = xmlElem.getStringAttribute("id");
        boolean negated = xmlElem.getStringAttribute("negated", "false").equalsIgnoreCase("true");
        String text = xmlElem.getStringAttribute("displayValues", null);
        // find definition
        IComposerEntity foundEntity = ComposerFieldUtility.externalIdToEntity(this, id, null);
        if (foundEntity == null) {
          LOG.warn("cannot find entity with id=" + id);
          continue;
        }
        ITreeNode node = addEntityNode(parent, foundEntity, negated, null, text != null ? new String[]{text} : null);
        if (node != null) {
          // add children recursive
          loadXMLRec(xmlElem, node);
        }
      }
      else if ("or".equals(xmlElem.getName())) {
        boolean beginning = xmlElem.getStringAttribute("begin", "false").equalsIgnoreCase("true");
        boolean negated = xmlElem.getStringAttribute("negated", "false").equalsIgnoreCase("true");
        ITreeNode node = null;
        if (beginning) {
          node = addEitherNode(parent, negated);
        }
        else {
          // find last EitherOrNode
          EitherOrNode orNode = null;
          for (ITreeNode n : parent.getChildNodes()) {
            if (n instanceof EitherOrNode && ((EitherOrNode) n).isBeginOfEitherOr()) {
              orNode = (EitherOrNode) n;
            }
          }
          if (orNode != null) {
            node = addAdditionalOrNode(orNode, negated);
          }
        }
        if (node != null) {
          // add children recursive
          loadXMLRec(xmlElem, node);
        }
      }

    }
  }

  @Override
  public void storeXML(SimpleXmlElement x) throws ProcessingException {
    super.storeXML(x);
    storeXMLRec(x, getTree().getRootNode());
  }

  private void storeXMLRec(SimpleXmlElement x, ITreeNode parent) {
    for (ITreeNode node : parent.getChildNodes()) {
      if (node instanceof EntityNode) {
        EntityNode entityNode = (EntityNode) node;
        SimpleXmlElement xEntity = new SimpleXmlElement("entity");
        xEntity.setAttribute("id", ComposerFieldUtility.entityToExternalId(this, entityNode.getEntity()));
        xEntity.setAttribute("negated", (entityNode.isNegative() ? "true" : "false"));
        String[] texts = entityNode.getTexts();
        xEntity.setAttribute("displayValues", texts != null && texts.length > 0 ? StringUtility.emptyIfNull(texts[0]) : null);
        x.addChild(xEntity);
        // recursion
        storeXMLRec(xEntity, node);
      }
      else if (node instanceof AttributeNode) {
        AttributeNode attNode = (AttributeNode) node;
        SimpleXmlElement xAtt = new SimpleXmlElement("attribute");
        xAtt.setAttribute("id", ComposerFieldUtility.attributeToExternalId(this, attNode.getAttribute()));
        IComposerOp op = attNode.getOp();
        try {
          xAtt.setAttribute("op", op.getOperator());
          if (attNode.getAggregationType() != null) {
            xAtt.setIntAttribute("aggregationType", attNode.getAggregationType());
          }
        }
        catch (Exception e) {
          LOG.warn("write op " + op, e);
        }
        String[] texts = attNode.getTexts();
        if (texts != null) {
          for (int i = 0; i < texts.length; i++) {
            String displayValueName = (i == 0 ? "displayValue" : "displayValue" + (i + 1));
            xAtt.setAttribute(displayValueName, StringUtility.emptyIfNull(texts[i]));
          }
        }
        Object[] values = attNode.getValues();
        if (values != null) {
          for (int i = 0; i < values.length; i++) {
            String valueName = (i == 0 ? "value" : "value" + (i + 1));
            try {
              xAtt.setObjectAttribute(valueName, values[i]);
            }
            catch (Exception e) {
              LOG.warn("write value[" + i + "] for attribute " + attNode.getAttribute() + ": " + values[i], e);
            }
          }
        }
        x.addChild(xAtt);
      }
      else if (node instanceof EitherOrNode) {
        EitherOrNode orNode = (EitherOrNode) node;
        SimpleXmlElement xOr = new SimpleXmlElement("or");
        xOr.setAttribute("begin", "" + orNode.isBeginOfEitherOr());
        xOr.setAttribute("negated", (orNode.isNegative() ? "true" : "false"));
        x.addChild(xOr);
        // recursion
        storeXMLRec(xOr, node);
      }
    }
  }

  public void resetValue() {
    if (m_initValue == null) {
      getTree().removeAllChildNodes(getTree().getRootNode());
    }
    else {
      try {
        loadXML(m_initValue);
      }
      catch (ProcessingException e) {
        LOG.error("unexpected error occured while restoring initial value", e);
        getTree().removeAllChildNodes(getTree().getRootNode());
      }
    }

    checkSaveNeeded();
    checkEmpty();
  }

  public EntityNode addEntityNode(ITreeNode parentNode, IComposerEntity e, boolean negated, Object[] values, String[] texts) {
    EntityNode node = execCreateEntityNode(parentNode, e, negated, values, texts);
    if (node != null) {
      getTree().addChildNode(parentNode, node);
      getTree().setNodeExpanded(node, true);
    }
    return node;
  }

  public EitherOrNode addEitherNode(ITreeNode parentNode, boolean negated) {
    EitherOrNode node = execCreateEitherNode(parentNode, negated);
    if (node != null) {
      getTree().addChildNode(parentNode, node);
      getTree().setNodeExpanded(node, true);
    }
    return node;
  }

  public EitherOrNode addAdditionalOrNode(ITreeNode parentNode, boolean negated) {
    EitherOrNode node = execCreateAdditionalOrNode(parentNode, negated);
    if (node != null) {
      getTree().addChildNode(parentNode.getChildNodeIndex() + 1, parentNode.getParentNode(), node);
      getTree().setNodeExpanded(node, true);
    }
    return node;
  }

  public AttributeNode addAttributeNode(ITreeNode parentNode, IComposerAttribute a, Integer aggregationType, IComposerOp op, Object[] values, String[] texts) {
    AttributeNode node = execCreateAttributeNode(parentNode, a, aggregationType, op, values, texts);
    if (node != null) {
      getTree().addChildNode(parentNode, node);
    }
    return node;
  }

  public void updateInitialValue() {
    try {
      // clone composer field by storing as XML
      SimpleXmlElement element = new SimpleXmlElement();
      storeXML(element);
      m_initValue = element;
    }
    catch (ProcessingException e) {
      LOG.warn("unexpected error occured while storing initial value", e);
    }
  }

  @Override
  protected boolean execIsSaveNeeded() throws ProcessingException {
    boolean b = false;
    if (b == false && m_tree.getDeletedNodeCount() > 0) {
      b = true;
    }
    if (b == false && m_tree.getInsertedNodeCount() > 0) {
      b = true;
    }
    if (b == false && m_tree.getUpdatedNodeCount() > 0) {
      b = true;
    }
    return b;
  }

  @Override
  protected void execMarkSaved() throws ProcessingException {
    try {
      m_tree.setTreeChanging(true);
      //
      ITreeVisitor v = new ITreeVisitor() {
        public boolean visit(ITreeNode node) {
          if (!node.isStatusNonchanged()) {
            node.setStatusInternal(ITreeNode.STATUS_NON_CHANGED);
            m_tree.updateNode(node);
          }
          return true;
        }
      };
      m_tree.visitNode(m_tree.getRootNode(), v);
      m_tree.clearDeletedNodes();

      updateInitialValue();
    }
    finally {
      m_tree.setTreeChanging(false);
    }
  }

  @Override
  protected boolean execIsEmpty() throws ProcessingException {
    if (m_tree.getRootNode() != null && m_tree.getRootNode().getChildNodeCount() > 0) {
      return false;
    }
    return true;
  }

  public IComposerFieldUIFacade getUIFacade() {
    return m_uiFacade;
  }

  /**
   * ui facade
   */
  private class P_UIFacade implements IComposerFieldUIFacade {

  }

  /**
   * inner tree type
   */
  public class Tree extends AbstractTree {

    @Override
    protected boolean getConfiguredRootNodeVisible() {
      return true;
    }

    @Override
    protected TreeNodeData exportTreeNodeData(ITreeNode node, AbstractTreeFieldData treeData) throws ProcessingException {
      IComposerField composerField = AbstractComposerField.this;
      AbstractComposerData composerData = (AbstractComposerData) treeData;
      if (node instanceof EntityNode) {
        EntityNode enode = (EntityNode) node;
        String externalId = ComposerFieldUtility.entityToExternalId(composerField, enode.getEntity());
        AbstractComposerEntityData eData = ComposerFieldUtility.externalIdToEntityData(composerData, externalId, null);
        if (eData == null) {
          LOG.warn("could not find entity data for: " + enode.getEntity());
          return null;
        }
        ComposerEntityNodeData data = new ComposerEntityNodeData();
        data.setEntity(eData);
        data.setNegative(enode.isNegative());
        return data;
      }
      else if (node instanceof AttributeNode) {
        AttributeNode anode = (AttributeNode) node;
        String externalId = ComposerFieldUtility.attributeToExternalId(composerField, anode.getAttribute());
        AbstractComposerAttributeData aData = ComposerFieldUtility.externalIdToAttributeData(composerData, externalId, null);
        if (aData == null) {
          LOG.warn("could not find attribute data for: " + anode.getAttribute());
          return null;
        }
        ComposerAttributeNodeData data = new ComposerAttributeNodeData();
        data.setAttribute(aData);
        data.setNegative(false);
        data.setAggregationType(anode.getAggregationType());
        data.setOperator(anode.getOp().getOperator());
        data.setValues(anode.getValues());
        return data;
      }
      else if (node instanceof EitherOrNode) {
        EitherOrNode eonode = (EitherOrNode) node;
        ComposerEitherOrNodeData data = new ComposerEitherOrNodeData();
        data.setNegative(eonode.isNegative());
        data.setBeginOfEitherOr(eonode.isBeginOfEitherOr());
        return data;
      }
      else {
        return null;
      }
    }

    @Override
    protected ITreeNode importTreeNodeData(ITreeNode parentNode, AbstractTreeFieldData treeData, TreeNodeData nodeData) throws ProcessingException {
      IComposerField composerField = AbstractComposerField.this;
      if (nodeData instanceof ComposerEntityNodeData) {
        ComposerEntityNodeData enodeData = (ComposerEntityNodeData) nodeData;
        String externalId = ComposerFieldUtility.entityDataToExternalId(enodeData.getEntity());
        IComposerEntity e = ComposerFieldUtility.externalIdToEntity(composerField, externalId, null);
        if (e == null) {
          LOG.warn("could not find entity for: " + enodeData.getEntity());
          return null;
        }
        return addEntityNode(parentNode, e, enodeData.isNegative(), null, enodeData.getTexts());
      }
      else if (nodeData instanceof ComposerAttributeNodeData) {
        ComposerAttributeNodeData anodeData = (ComposerAttributeNodeData) nodeData;
        String externalId = ComposerFieldUtility.attributeDataToExternalId(anodeData.getAttribute(), getMetaDataOfAttributeData(anodeData.getAttribute(), anodeData.getValues()));
        IComposerAttribute a = ComposerFieldUtility.externalIdToAttribute(composerField, externalId, null);
        if (a == null) {
          LOG.warn("could not find attribute for: " + anodeData.getAttribute());
          return null;
        }
        IComposerOp op;
        try {
          op = ComposerOp.create(anodeData.getOperator());
        }
        catch (Exception e) {
          LOG.warn("read op " + anodeData.getOperator(), e);
          return null;
        }
        return addAttributeNode(parentNode, a, anodeData.getAggregationType(), op, anodeData.getValues(), anodeData.getTexts());
      }
      else if (nodeData instanceof ComposerEitherOrNodeData) {
        ComposerEitherOrNodeData eonodeData = (ComposerEitherOrNodeData) nodeData;
        if (eonodeData.isBeginOfEitherOr()) {
          return addEitherNode(parentNode, eonodeData.isNegative());
        }
        else {
          return addAdditionalOrNode(parentNode, eonodeData.isNegative());
        }
      }
      else {
        return null;
      }
    }

    @Override
    protected void execDecorateCell(ITreeNode node, Cell cell) throws ProcessingException {
      node.decorateCell();
      if (getIconId() != null) {
        cell.setIconId(getIconId());
      }
      else {
        if (node instanceof RootNode) {
          cell.setIconId(AbstractIcons.ComposerFieldRoot);
        }
        else if (node instanceof EntityNode) {
          cell.setIconId(AbstractIcons.ComposerFieldEntity);
        }
        else if (node instanceof AttributeNode) {
          if (((AttributeNode) node).getAggregationType() != null) {
            cell.setIconId(AbstractIcons.ComposerFieldAggregation);
          }
          else {
            cell.setIconId(AbstractIcons.ComposerFieldAttribute);
          }
        }
        else if (node instanceof EitherOrNode) {
          cell.setIconId(AbstractIcons.ComposerFieldEitherOrNode);
        }
      }
    }

    @Order(10)
    public class DeleteKeyStroke extends AbstractKeyStroke {

      @Override
      protected String getConfiguredKeyStroke() {
        return "delete";
      }

      @Override
      protected void execAction() throws ProcessingException {
        ITree tree = getTree();
        if (tree != null) {
          ITreeNode node = tree.getSelectedNode();
          //check if already deleted
          if (node != null && node.getTree() == tree) {
            IMenu menu = null;
            if (node instanceof AttributeNode) {
              menu = node.getMenu(AttributeNode.DeleteAttributeMenu.class);
            }
            else if (node instanceof EntityNode) {
              menu = node.getMenu(EntityNode.DeleteEntityMenu.class);
            }
            else if (node instanceof EitherOrNode) {
              menu = node.getMenu(EitherOrNode.DeleteEitherOrMenu.class);
            }
            if (menu != null) {
              menu.prepareAction();
              if (menu.isVisible() && menu.isEnabled()) {
                //correct selection
                tree.selectPreviousNode();
                menu.doAction();
              }
            }
          }
        }
      }
    }
  }

}