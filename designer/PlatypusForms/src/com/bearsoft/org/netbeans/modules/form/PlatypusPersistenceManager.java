/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bearsoft.org.netbeans.modules.form;

import com.bearsoft.org.netbeans.modules.form.bound.RADModelGrid;
import com.bearsoft.org.netbeans.modules.form.bound.RADModelGridColumn;
import com.bearsoft.org.netbeans.modules.form.bound.RADModelScalarComponent;
import com.bearsoft.org.netbeans.modules.form.editors.IconEditor;
import com.bearsoft.org.netbeans.modules.form.editors.IconEditor.NbImageIcon;
import com.bearsoft.org.netbeans.modules.form.layoutsupport.LayoutSupportDelegate;
import com.bearsoft.org.netbeans.modules.form.layoutsupport.LayoutSupportRegistry;
import com.eas.client.forms.Form;
import com.eas.client.forms.FormFactory;
import com.eas.client.forms.HasChildren;
import com.eas.client.forms.HorizontalPosition;
import com.eas.client.forms.Orientation;
import com.eas.client.forms.Widget;
import com.eas.client.forms.components.model.ModelWidget;
import com.eas.client.forms.components.model.grid.ModelGrid;
import com.eas.client.forms.containers.ButtonGroup;
import com.eas.client.forms.layouts.BoxLayout;
import com.eas.client.forms.layouts.CardLayout;
import com.eas.client.forms.layouts.MarginConstraints;
import com.eas.client.forms.layouts.MarginLayout;
import com.eas.designer.application.PlatypusUtils;
import com.eas.gui.ScriptColor;
import com.eas.xml.dom.Source2XmlDom;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.openide.util.Exceptions;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author mg
 */
public class PlatypusPersistenceManager extends PersistenceManager {

    // setup documents framework
    protected static DocumentBuilderFactory docsBuidlersFactory = DocumentBuilderFactory.newInstance();
    static final String XML_FORM = "layout"; // NOI18N

    @Override
    public boolean canLoadForm(PlatypusFormDataObject formObject) throws PersistenceException {
        return false;
    }

    @Override
    public FormModel loadForm(PlatypusFormDataObject formDataObject, List<Throwable> nonfatalErrors) throws PersistenceException {
        try {
            String formContent = formDataObject.getFormFile().asText(PlatypusUtils.COMMON_ENCODING_NAME);
            Document doc = Source2XmlDom.transform(formContent);
            FormFactory formFactory = new FormFactory(doc.getDocumentElement(), formDataObject.getModel().getPublished()) {

                @Override
                protected ImageIcon resolveIcon(String aIconName) {
                    try {
                        NbImageIcon nbIcon = IconEditor.iconFromResourceName(formDataObject, aIconName);
                        return nbIcon != null ? (ImageIcon) nbIcon.getIcon() : null;
                    } catch (Exception ex) {
                        nonfatalErrors.add(ex);
                        return null;
                    }
                }

                @Override
                protected JComponent createAnchorsPane() {
                    return new FormUtils.Panel(new MarginLayout());
                }

                @Override
                protected JComponent createBorderPane(int hgap, int vgap) {
                    return new FormUtils.Panel(new BorderLayout(hgap, vgap));
                }

                @Override
                protected JComponent createBoxPane(int axis, int hgap, int vgap) {
                    JComponent res = new FormUtils.Panel();
                    res.setLayout(new BoxLayout(res, axis, hgap, vgap));
                    return res;
                }

                @Override
                protected JComponent createCardPane(int hgap, int vgap) {
                    return new FormUtils.Panel(new CardLayout(hgap, vgap));
                }

                @Override
                protected JComponent createFlowPane(int hgap, int vgap) {
                    return new FormUtils.Panel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
                }

                @Override
                protected JComponent createGridPane(int rows, int columns, int hgap, int vgap) {
                    return new FormUtils.Panel(new GridLayout(rows, columns, hgap, vgap));
                }

                @Override
                protected void addToAnchorsPane(JComponent parent, JComponent aTarget, MarginConstraints constraints) {
                    parent.add(aTarget, constraints);
                }

                @Override
                protected void addToBorderPane(JComponent parent, JComponent aComp, Integer aPlace, Integer aSize) {
                    aPlace = aPlace != null ? aPlace : HorizontalPosition.CENTER;
                    if (aPlace != HorizontalPosition.CENTER && aSize != null) {
                        Dimension prefSize = aComp.getPreferredSize();
                        if (aPlace == HorizontalPosition.LEFT || aPlace == HorizontalPosition.RIGHT) {
                            aComp.setPreferredSize(new Dimension(aSize, prefSize.height));
                        } else {
                            aComp.setPreferredSize(new Dimension(prefSize.width, aSize));
                        }
                    }
                    parent.add(aComp, aPlace);
                }

                @Override
                protected void addToBoxPane(JComponent parent, JComponent aTarget, Dimension prefSize) {
                    if (((BoxLayout) parent.getLayout()).getAxis() == Orientation.HORIZONTAL) {
                        parent.add(aTarget, prefSize.width);
                    } else {
                        parent.add(aTarget, prefSize.height);
                    }
                }

                @Override
                protected void addToCardPane(JComponent parent, JComponent aTarget, String cardName) {
                    parent.add(aTarget, cardName);
                }

                @Override
                protected void addToFlowPane(JComponent parent, JComponent aTarget) {
                    parent.add(aTarget);
                }

                @Override
                protected void addToGridPane(JComponent parent, JComponent aTarget) {
                    parent.add(aTarget);
                }

            };
            formFactory.parse();
            Form form = formFactory.getForm();
            FormModel formModel = new FormModel(formDataObject, form);
            formModel.setName(formDataObject.getName());

            // Let's take care of top level container
            RADVisualFormContainer formComp = new RADVisualFormContainer();
            formComp.initialize(formModel);
            formModel.initFormComponent(formComp);
            formComp.setBeanInstance(form.getViewWidget());
            for (RADProperty<?> radProp : formComp.getBeanProperties()) {
                radProp.setChanged(!radProp.isDefaultValue());
            }
            formComp.checkLayoutSupport();
            LayoutSupportDelegate layoutSupportDelegate = LayoutSupportRegistry.createSupportForLayout(((Container) formComp.getBeanInstance()).getLayout().getClass());
            formComp.getLayoutSupport().setLayoutDelegate(layoutSupportDelegate);
            formComp.setInModel(true);

            Map<String, RADComponent<?>> radComps = new HashMap<>();
            formFactory.getWidgets().entrySet().stream().forEach((Map.Entry<String, JComponent> aEntry) -> {
                try {
                    RADComponent<?> radComp = radComponentByWidget(formModel, aEntry.getValue());
                    radComps.put(radComp.getName(), radComp);
                    /*
                     if(radComp instanceof RADModelGrid){
                     RADModelGrid radGrid = (RADModelGrid)radComp;
                     ModelGrid grid = radGrid.getBeanInstance();
                     for(GridColumnsNode colNode : grid.getHeader()){
                     RADModelGridColumn radCol = new RADModelGridColumn();
                     radCol.initialize(formModel);
                     radCol.setStoredName(colNode.getName());
                     radCol.setBeanInstance(colNode);
                     radCol.setInModel(true);
                     for(RADProperty<?> radProp : radCol.getBeanProperties()){
                     radProp.setChanged(!radProp.isDefaultValue());
                     }
                     }
                     }
                     */
                } catch (Exception ex) {
                    nonfatalErrors.add(ex);
                }
            });

            RADComponent<?>[] toResolve = radComps.values().toArray(new RADComponent<?>[]{});
            for (RADComponent<?> radComp : toResolve) {
                Object oWidget = radComp.getBeanInstance();
                if (oWidget instanceof Widget) {
                    Widget w = (Widget) oWidget;
                    Widget p = w.getParentWidget();
                    if (p != null) {
                        if (p == form.getViewWidget()) {
                            radComp.setParent(formComp);
                            formComp.add(radComp);
                        } else {
                            String parentName = p.getComponent().getName();
                            RADComponent<?> radParent = radComps.get(parentName);
                            if (radParent instanceof ComponentContainer) {
                                ComponentContainer cont = (ComponentContainer) radParent;
                                radComp.setParent(cont);
                                cont.add(radComp);
                            } else {
                                formModel.getModelContainer().add(radComp);
                            }
                        }
                    } else {
                        formModel.getModelContainer().add(radComp);
                    }
                }
            }
            return formModel;
        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }

    @Override
    public void saveForm(PlatypusFormDataObject formObject, FormModel formModel, List<Throwable> nonfatalErrors) throws PersistenceException {
        try {
            DocumentBuilder builder = docsBuidlersFactory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.setXmlStandalone(true);
            Element root = doc.createElement(XML_FORM);
            doc.appendChild(root);
            writeProperties(formModel.getTopRADComponent().getBeanProperties(), doc, root);
            formModel.getAllComponents().forEach((RADComponent<?> aComp) -> {
                Element widgetElement = doc.createElement("widget");
                root.appendChild(widgetElement);
                widgetElement.setAttribute("type", aComp.getBeanClass().getSimpleName());
                widgetElement.setAttribute("name", aComp.getName());
                writeProperties(aComp.getBeanProperties(), doc, widgetElement);
                if (aComp instanceof RADVisualContainer<?>) {
                    Element layoutElement = doc.createElement("layout");
                    widgetElement.appendChild(layoutElement);
                    RADVisualContainer<?> radCont = (RADVisualContainer<?>) aComp;
                    writeProperties(radCont.getLayoutSupport().getAllProperties(), doc, layoutElement);
                }
                if (aComp instanceof RADVisualComponent<?>) {
                    RADVisualComponent<?> visComp = (RADVisualComponent<?>) aComp;
                    Element constraintsElement = doc.createElement("constraints");
                    widgetElement.appendChild(constraintsElement);
                    writeProperties(visComp.getConstraintsProperties(), doc, constraintsElement);
                    if (aComp instanceof RADModelGrid) {
                        RADModelGrid grid = (RADModelGrid) aComp;
                        writeSubBeans(grid, doc, widgetElement);
                    }
                }
                if (aComp.getParent() instanceof RADVisualComponent<?>) {
                    RADVisualComponent<?> visParent = (RADVisualComponent<?>) aComp.getParent();
                    widgetElement.setAttribute("parent", visParent.getName());
                }
            });
        } catch (ParserConfigurationException ex) {
            throw new PersistenceException(ex);
        }
    }

    private void writeSubBeans(ComponentContainer aColumnsContainer, Document doc, Element targetElement) throws DOMException {
        for (RADComponent<?> subBean : aColumnsContainer.getSubBeans()) {
            Element columnElement = doc.createElement("column");
            targetElement.appendChild(columnElement);
            writeProperties(subBean.getBeanProperties(), doc, columnElement);
            if (subBean instanceof RADModelGridColumn) {
                RADModelGridColumn radColumn = (RADModelGridColumn) subBean;
                Element viewElement = doc.createElement("view");
                columnElement.appendChild(viewElement);
                writeProperties(radColumn.getViewControl().getBeanProperties(), doc, viewElement);
            }
            if (subBean instanceof ComponentContainer) {
                writeSubBeans((ComponentContainer) subBean, doc, columnElement);
            }
        }
    }

    private void writeProperties(FormProperty<?>[] aProperties, Document doc, Element targetElement) throws DOMException {
        for (FormProperty<?> radProp : aProperties) {
            if (!radProp.getName().equals("type")) {
                if (!radProp.isDefaultValue()) {
                    try {
                        Object propValue = radProp.getValue();
                        if (propValue != null) {
                            if (propValue instanceof ComponentReference<?>) {
                                ComponentReference<?> compRef = (ComponentReference<?>) propValue;
                                if (compRef.getComponent() != null) {
                                    targetElement.setAttribute(radProp.getName(), compRef.getComponent().getName());
                                }
                            } else if (propValue instanceof IconEditor.NbImageIcon) {
                                targetElement.setAttribute(radProp.getName(), ((IconEditor.NbImageIcon) propValue).getName());
                            } else if (propValue instanceof java.awt.Cursor) {
                                targetElement.setAttribute(radProp.getName(), String.valueOf(((java.awt.Cursor) propValue).getType()));
                            } else if (propValue instanceof Color) {
                                targetElement.setAttribute(radProp.getName(), ScriptColor.encode((Color) propValue));
                            } else if (propValue instanceof Font) {
                                Font fontValue = (Font) propValue;
                                Element fontElement = doc.createElement("font");
                                targetElement.appendChild(fontElement);
                                fontElement.setAttribute("family", fontValue.getFamily());
                                fontElement.setAttribute("size", String.valueOf(fontValue.getSize()));
                                fontElement.setAttribute("style", String.valueOf(fontValue.getStyle()));
                            } else {
                                targetElement.setAttribute(radProp.getName(), String.valueOf(propValue));
                            }
                        }
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            } else {
                Logger.getLogger(PlatypusPersistenceManager.class.getName()).log(Level.WARNING, "Prohibited property name \"type\" occured while saving a form.");
            }
        }
    }

    private RADComponent<?> radComponentByWidget(FormModel aFormModel, JComponent aWidget) throws Exception {
        RADComponent<?> radComp;
        if (aWidget instanceof ModelWidget) {
            if (aWidget instanceof ModelGrid) {
                radComp = new RADModelGrid();
            } else {
                radComp = new RADModelScalarComponent<>();
            }
        } else {
            if (aWidget instanceof ButtonGroup) {
                radComp = new RADButtonGroup();
            } else {
                if (aWidget instanceof HasChildren) {
                    radComp = new RADVisualContainer<>();
                } else {
                    radComp = new RADVisualComponent<>();
                }
            }
        }
        radComp.initialize(aFormModel);
        radComp.setStoredName(aWidget.getName());
        ((RADComponent<JComponent>) radComp).setBeanInstance(aWidget);
        for (RADProperty<?> radProp : radComp.getBeanProperties()) {
            radProp.setChanged(!radProp.isDefaultValue());
        }
        if (radComp instanceof RADVisualContainer<?>) {
            RADVisualContainer<?> radCont = (RADVisualContainer<?>) radComp;
            radCont.checkLayoutSupport();
            LayoutSupportDelegate layoutSupportDelegate = LayoutSupportRegistry.createSupportForLayout(radCont.getBeanInstance().getLayout().getClass());
            radCont.getLayoutSupport().setLayoutDelegate(layoutSupportDelegate);
        }
        radComp.setInModel(true);
        return radComp;
    }
}
