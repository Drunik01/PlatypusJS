/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.eas.client.forms.api.containers;

import com.eas.client.forms.api.FormEventsIProxy;
import com.eas.client.forms.api.HasChildren;
import com.eas.client.forms.api.HasContainerEvents;
import com.eas.client.forms.api.events.ActionEvent;
import com.eas.client.forms.api.events.ChangeEvent;
import com.eas.client.forms.api.events.ComponentEvent;
import com.eas.client.forms.api.events.MouseEvent;
import com.eas.controls.events.ControlEventsIProxy;
import com.eas.script.AlreadyPublishedException;
import com.eas.script.EventMethod;
import com.eas.script.HasPublished;
import com.eas.script.HasPublishedInvalidatableCollection;
import com.eas.script.NoPublisherException;
import com.eas.script.ScriptFunction;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import jdk.nashorn.api.scripting.JSObject;

/**
 *
 * @author mg
 */
public class TabbedPane extends JTabbedPane implements HasPublished, HasContainerEvents, HasChildren, HasPublishedInvalidatableCollection {

    private static final String CONSTRUCTOR_JSDOC = ""
            + "/**\n"
            + " * A component that lets the user switch between a group of components by\n"
            + " * clicking on a tab with a given title and/or icon.\n"
            + " */";

    protected JSObject onItemSelected;

    protected ChangeListener tabsChangeListener = (javax.swing.event.ChangeEvent e) -> {
        try {
            onItemSelected.call(getPublished(), new Object[]{new ChangeEvent(e).getPublished()});
        } catch (Exception ex) {
            Logger.getLogger(TabbedPane.class.getName()).log(Level.SEVERE, null, ex);
        }
    };

    @ScriptFunction(jsDoc = CONSTRUCTOR_JSDOC, params = {})
    public TabbedPane() {
        super();
        super.addChangeListener(tabsChangeListener);
    }

    @ScriptFunction(jsDoc = ""
            + "/**\n"
            + " * Event that is fired when one of the components is selected in this tabbed pane.\n"
            + " */")
    @EventMethod(eventClass = ChangeEvent.class)
    public JSObject getOnItemSelected() {
        return onItemSelected;
    }

    @ScriptFunction
    public void setOnItemSelected(JSObject aValue) {
        if (onItemSelected != aValue) {
            onItemSelected = aValue;
        }
    }

    public void add(JComponent aComp, String aText) {
        super.addTab(aText, aComp);
        super.revalidate();
        super.repaint();
    }

    private static final String ADD_JSDOC = ""
            + "/**\n"
            + " * Appends the component whith specified text to the end of this container.\n"
            + " * @param component the component to add.\n"
            + " * @param text the text for the tab.\n"
            + " * @param icon the icon for the tab (optional).\n"
            + " */";

    @ScriptFunction(jsDoc = ADD_JSDOC, params = {"component", "text", "icon"})
    public void add(JComponent aComp, String aText, Icon aIcon) {
        if (aComp != null) {
            super.addTab(aText, aIcon, aComp);
            super.revalidate();
            super.repaint();
        }
    }

    protected ContainerListener invalidatorListener = new ContainerAdapter() {

        @Override
        public void componentAdded(ContainerEvent e) {
            invalidatePublishedCollection();
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            invalidatePublishedCollection();
        }

    };

    protected JSObject publishedCollectionInvalidator;

    @Override
    public JSObject getPublishedCollectionInvalidator() {
        return publishedCollectionInvalidator;
    }

    @Override
    public void setPublishedCollectionInvalidator(JSObject aValue) {
        publishedCollectionInvalidator = aValue;
    }

    @Override
    public void invalidatePublishedCollection() {
        if (publishedCollectionInvalidator != null && publishedCollectionInvalidator.isFunction()) {
            publishedCollectionInvalidator.call(getPublished(), new Object[]{});
        }
    }
    private static final String SELECTED_COMPONENT_JSDOC = ""
            + "/**\n"
            + " * The selected component.\n"
            + " */";

    @ScriptFunction(jsDoc = SELECTED_COMPONENT_JSDOC)
    @Override
    public JComponent getSelectedComponent() {
        return (JComponent) super.getSelectedComponent();
    }

    @ScriptFunction
    public void setSelectedComponent(JComponent aComp) {
        if (aComp == null) {
            super.setSelectedIndex(-1);
        } else {
            super.setSelectedComponent(aComp);
        }
    }

    private static final String SELECTED_INDEX_JSDOC = ""
            + "/**\n"
            + " * The selected component's index.\n"
            + " */";

    @ScriptFunction(jsDoc = SELECTED_INDEX_JSDOC)
    @Override
    public int getSelectedIndex() {
        return super.getSelectedIndex();
    }

    @ScriptFunction
    @Override
    public void setSelectedIndex(int aIndex) {
        super.setSelectedIndex(aIndex);
    }

    @ScriptFunction(jsDoc = CHILD_JSDOC, params = {"index"})
    @Override
    public JComponent child(int aIndex) {
        return (JComponent) super.getComponent(aIndex);
    }

    @ScriptFunction(jsDoc = CHILDREN_JSDOC)
    @Override
    public JComponent[] getChildren() {
        List<JComponent> ch = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) {
            ch.add(child(i));
        }
        return ch.toArray(new JComponent[]{});
    }

    @ScriptFunction(jsDoc = COUNT_JSDOC)
    @Override
    public int getCount() {
        return super.getComponentCount();
    }

    @ScriptFunction(jsDoc = REMOVE_JSDOC, params = {"component"})
    @Override
    public void remove(JComponent aComp) {
        super.remove(aComp);
        super.revalidate();
        super.repaint();
    }

    @ScriptFunction(jsDoc = CLEAR_JSDOC)
    @Override
    public void clear() {
        super.removeAll();
        super.revalidate();
        super.repaint();
    }

    protected JSObject published;

    @Override
    public JSObject getPublished() {
        if (published == null) {
            if (publisher == null || !publisher.isFunction()) {
                throw new NoPublisherException();
            }
            published = (JSObject) publisher.call(null, new Object[]{this});
        }
        return published;
    }

    @Override
    public void setPublished(JSObject aValue) {
        if (published != null) {
            throw new AlreadyPublishedException();
        }
        published = aValue;
    }

    private static JSObject publisher;

    public static void setPublisher(JSObject aPublisher) {
        publisher = aPublisher;
    }

    protected ControlEventsIProxy eventsProxy = new FormEventsIProxy(this);

    @ScriptFunction(jsDoc = ON_MOUSE_CLICKED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseClicked() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseClicked);
    }

    @ScriptFunction
    @Override
    public void setOnMouseClicked(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseClicked, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_DRAGGED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseDragged() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseDragged);
    }

    @ScriptFunction
    @Override
    public void setOnMouseDragged(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseDragged, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_ENTERED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseEntered() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseEntered);
    }

    @ScriptFunction
    @Override
    public void setOnMouseEntered(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseEntered, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_EXITED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseExited() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseExited);
    }

    @ScriptFunction
    @Override
    public void setOnMouseExited(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseExited, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_MOVED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseMoved() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseMoved);
    }

    @ScriptFunction
    @Override
    public void setOnMouseMoved(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseMoved, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_PRESSED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMousePressed() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mousePressed);
    }

    @ScriptFunction
    @Override
    public void setOnMousePressed(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mousePressed, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_RELEASED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseReleased() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseReleased);
    }

    @ScriptFunction
    @Override
    public void setOnMouseReleased(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseReleased, aValue);
    }

    @ScriptFunction(jsDoc = ON_MOUSE_WHEEL_MOVED_JSDOC)
    @EventMethod(eventClass = MouseEvent.class)
    @Override
    public JSObject getOnMouseWheelMoved() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.mouseWheelMoved);
    }

    @ScriptFunction
    @Override
    public void setOnMouseWheelMoved(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.mouseWheelMoved, aValue);
    }

    @ScriptFunction(jsDoc = ON_ACTION_PERFORMED_JSDOC)
    @EventMethod(eventClass = ActionEvent.class)
    @Override
    public JSObject getOnActionPerformed() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.actionPerformed);
    }

    @ScriptFunction
    @Override
    public void setOnActionPerformed(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.actionPerformed, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_HIDDEN_JSDOC)
    @EventMethod(eventClass = ComponentEvent.class)
    @Override
    public JSObject getOnComponentHidden() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentHidden);
    }

    @ScriptFunction
    @Override
    public void setOnComponentHidden(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentHidden, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_MOVED_JSDOC)
    @EventMethod(eventClass = ComponentEvent.class)
    @Override
    public JSObject getOnComponentMoved() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentMoved);
    }

    @ScriptFunction
    @Override
    public void setOnComponentMoved(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentMoved, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_RESIZED_JSDOC)
    @EventMethod(eventClass = ComponentEvent.class)
    @Override
    public JSObject getOnComponentResized() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentResized);
    }

    @ScriptFunction
    @Override
    public void setOnComponentResized(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentResized, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_SHOWN_JSDOC)
    @EventMethod(eventClass = ComponentEvent.class)
    @Override
    public JSObject getOnComponentShown() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentShown);
    }

    @ScriptFunction
    @Override
    public void setOnComponentShown(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentShown, aValue);
    }

    @ScriptFunction(jsDoc = ON_FOCUS_GAINED_JSDOC)
    @EventMethod(eventClass = FocusEvent.class)
    @Override
    public JSObject getOnFocusGained() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.focusGained);
    }

    @ScriptFunction
    @Override
    public void setOnFocusGained(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.focusGained, aValue);
    }

    @ScriptFunction(jsDoc = ON_FOCUS_LOST_JSDOC)
    @EventMethod(eventClass = FocusEvent.class)
    @Override
    public JSObject getOnFocusLost() {
        return eventsProxy != null ? eventsProxy.getHandlers().get(ControlEventsIProxy.focusLost) : null;
    }

    @ScriptFunction
    @Override
    public void setOnFocusLost(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.focusLost, aValue);
    }

    @ScriptFunction(jsDoc = ON_KEY_PRESSED_JSDOC)
    @EventMethod(eventClass = KeyEvent.class)
    @Override
    public JSObject getOnKeyPressed() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.keyPressed);
    }

    @ScriptFunction
    @Override
    public void setOnKeyPressed(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.keyPressed, aValue);
    }

    @ScriptFunction(jsDoc = ON_KEY_RELEASED_JSDOC)
    @EventMethod(eventClass = KeyEvent.class)
    @Override
    public JSObject getOnKeyReleased() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.keyReleased);
    }

    @ScriptFunction
    @Override
    public void setOnKeyReleased(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.keyReleased, aValue);
    }

    @ScriptFunction(jsDoc = ON_KEY_TYPED_JSDOC)
    @EventMethod(eventClass = KeyEvent.class)
    @Override
    public JSObject getOnKeyTyped() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.keyTyped);
    }

    @ScriptFunction
    @Override
    public void setOnKeyTyped(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.keyTyped, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_ADDED_JSDOC)
    @EventMethod(eventClass = ContainerEvent.class)
    @Override
    public JSObject getOnComponentAdded() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentAdded);
    }

    @ScriptFunction
    @Override
    public void setOnComponentAdded(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentAdded, aValue);
    }

    @ScriptFunction(jsDoc = ON_COMPONENT_REMOVED_JSDOC)
    @EventMethod(eventClass = ContainerEvent.class)
    @Override
    public JSObject getOnComponentRemoved() {
        return eventsProxy.getHandlers().get(ControlEventsIProxy.componentRemoved);
    }

    @ScriptFunction
    @Override
    public void setOnComponentRemoved(JSObject aValue) {
        eventsProxy.getHandlers().put(ControlEventsIProxy.componentRemoved, aValue);
    }

    @Override
    public String toString() {
        return String.format("%s [%s] count:%d", super.getName() != null ? super.getName() : "", getClass().getSimpleName(), getCount());
    }
}
