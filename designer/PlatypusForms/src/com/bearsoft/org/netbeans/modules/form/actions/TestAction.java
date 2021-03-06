/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package com.bearsoft.org.netbeans.modules.form.actions;

import com.bearsoft.org.netbeans.modules.form.*;
import com.bearsoft.org.netbeans.modules.form.palette.PaletteItem;
import com.bearsoft.org.netbeans.modules.form.palette.PaletteUtils;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.plaf.synth.SynthLookAndFeel;
import org.openide.ErrorManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/**
 * Preview design action.
 *
 * @author Tomas Pavek, Jan Stola
 */
@ActionID(id = "com.bearsoft.org.netbeans.modules.form.actions.TestAction", category = "Form")
@ActionRegistration(displayName = "#ACT_TestMode", lazy = true)
public class TestAction extends CallableSystemAction implements Runnable {

    private static String name;
    /**
     * Maps path of form file to laf-classname -> preview map. It is used to
     * keep at most one preview per laf per file.
     */
    private Map<String, Map<String, Frame>> previews = new HashMap<>();

    public TestAction() {
        setEnabled(false);
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    /**
     * Human presentable name of the action. This should be presented as an item
     * in a menu.
     *
     * @return the name of the action
     */
    @Override
    public String getName() {
        if (name == null) {
            name = NbBundle.getMessage(TestAction.class, "ACT_TestMode"); // NOI18N
        }
        return name;
    }

    /**
     * Help context where to find more about the action.
     *
     * @return the help context for this action
     */
    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("gui.testing"); // NOI18N
    }

    /**
     * @return resource for the action icon
     */
    @Override
    protected String iconResource() {
        return "com/bearsoft/org/netbeans/modules/form/resources/test_form.png"; // NOI18N
    }

    @Override
    public void performAction() {
        if (formDesigner != null) {
            selectedLaf = null;
            if (java.awt.EventQueue.isDispatchThread()) {
                run();
            } else {
                java.awt.EventQueue.invokeLater(this);
            }
        }
    }

    @Override
    public void run() {
        RADVisualComponent<?> topComp = formDesigner.getTopDesignComponent();
        if (topComp == null) {
            return;
        }

        RADVisualComponent<?> parent = topComp.getParentComponent();
        while (parent != null) {
            topComp = parent;
            parent = topComp.getParentComponent();
        }

        FormModel formModel = formDesigner.getFormModel();
        RADVisualFormContainer formContainer = (RADVisualFormContainer) topComp;

        try {
            if (selectedLaf == null) {
                selectedLaf = UIManager.getLookAndFeel().getClass();
            }

            // Dispose the previous preview (if it exists)
            FileObject formJsFile = formModel.getDataObject().getPrimaryFile();
            Map<String, Frame> map = previews.get(formJsFile.getPath());
            if (map == null) {
                map = new HashMap<>();
                previews.put(formJsFile.getPath(), map);
            }
            Frame previousFrame = map.get(selectedLaf.getName());
            if (previousFrame != null) {
                previousFrame.dispose();
            }

            // create a copy of form
            final ClassLoader classLoader = Lookup.getDefault().lookup(ClassLoader.class);
            final FormLAF.PreviewInfo previewInfo = FormLAF.initPreviewLaf(selectedLaf, classLoader);
            final Frame frame = (Frame) PlatypusFormLayoutView.createFormView(topComp, formDesigner.getFormEditor(), previewInfo);
            frame.setEnabled(true); // Issue 178457
            frame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    frame.dispose();
                }
            });
            map.put(selectedLaf.getName(), frame);

            // set title
            String title = frame.getTitle();
            if (title == null || "".equals(title)) { // NOI18N
                title = topComp == formModel.getTopRADComponent()
                        ? formModel.getName() : topComp.getName();
                frame.setTitle(NbBundle.getMessage(TestAction.class, "FMT_TestingForm", title));
            }

            // prepare close operation
            if (frame instanceof JFrame) {
                ((JFrame) frame).setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                HelpCtx.setHelpIDString(((JFrame) frame).getRootPane(),
                        "gui.modes"); // NOI18N
            } else {
                frame.addWindowListener(new java.awt.event.WindowAdapter() {

                    @Override
                    public void windowClosing(java.awt.event.WindowEvent evt) {
                        frame.dispose();
                    }
                });
            }

            // set size
            Dimension size = formContainer.getBeanInstance().getSize();
            Dimension diffSize = RADVisualFormContainer.getDecoratedWindowContentDimensionDiff();
            size = new Dimension(size.width + diffSize.width, size.height + diffSize.height);
            frame.setSize(size);
            frame.setUndecorated(false);
            frame.setFocusableWindowState(true);
            // Issue 66594 and 12084
            EventQueue.invokeLater(() -> {
                frame.setBounds(org.openide.util.Utilities.findCenterBounds(frame.getSize()));
                frame.setVisible(true);
            });
        } catch (Exception ex) {
            org.openide.ErrorManager.getDefault().notify(org.openide.ErrorManager.INFORMATIONAL, ex);
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return getPopupPresenter();
    }

    @Override
    public JMenuItem getPopupPresenter() {
        JMenu layoutMenu = new LAFMenu(getName());
        layoutMenu.setEnabled(isEnabled());
        HelpCtx.setHelpIDString(layoutMenu, SelectLayoutAction.class.getName());
        return layoutMenu;
    }
    // -------
    private PlatypusFormLayoutView formDesigner;

    public void setFormDesigner(PlatypusFormLayoutView designer) {
        formDesigner = designer;
        setEnabled(formDesigner != null && formDesigner.getTopDesignComponent() != null);
    }
    // LAFMenu
    private Class<?> selectedLaf;

    private class LAFMenu extends JMenu implements ActionListener {

        private boolean initialized = false;

        private LAFMenu(String name) {
            super(name);
        }

        @Override
        public JPopupMenu getPopupMenu() {
            JPopupMenu popup = super.getPopupMenu();
            JMenuItem mi;
            if (!initialized) {
                popup.removeAll();

                boolean isSynthLAF = UIManager.getLookAndFeel() instanceof SynthLookAndFeel;
                String lafName = UIManager.getLookAndFeel().getClass().getName();

                // Swing L&Fs
                UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
                for (int i = 0; i < lafs.length; i++) {
                    String className = lafs[i].getClassName();
                    if (isSynthLAF) {
                        try {
                            Class<?> lafClass = Class.forName(className);
                            if (!lafName.equals(className) && SynthLookAndFeel.class.isAssignableFrom(lafClass)) {
                                continue; // 134848, 145807: Cannot use two different SynthLookAndFeels
                            }
                        } catch (ClassNotFoundException cnfex) {
                            // should not happen
                            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, cnfex);
                        }
                    }
                    mi = new JMenuItem(lafs[i].getName());
                    mi.putClientProperty("lafInfo", new LookAndFeelItem(lafs[i].getClassName())); // NOI18N
                    mi.addActionListener(this);
                    popup.add(mi);
                }

                // L&Fs from the Palette
                Node[] cats = PaletteUtils.getCategoryNodes(PaletteUtils.getPaletteNode(), false);
                for (int i = 0; i < cats.length; i++) {
                    if ("LookAndFeels".equals(cats[i].getName())) { // NOI18N
                        final Node lafNode = cats[i];
                        Node[] items = PaletteUtils.getItemNodes(lafNode, true);
                        if (items.length != 0) {
                            popup.add(new JSeparator());
                        }
                        for (int j = 0; j < items.length; j++) {
                            PaletteItem pitem = items[j].getLookup().lookup(PaletteItem.class);
                            boolean supported = false;
                            try {
                                Class<?> clazz = pitem.getComponentClass();
                                if ((clazz != null) && (LookAndFeel.class.isAssignableFrom(clazz))) {
                                    LookAndFeel laf = (LookAndFeel) clazz.newInstance();
                                    supported = laf.isSupportedLookAndFeel();
                                    if (supported && isSynthLAF && !lafName.equals(pitem.getComponentClassName())
                                            && SynthLookAndFeel.class.isAssignableFrom(clazz)) {
                                        supported = false; // 134848, 145807: Cannot use two different SynthLookAndFeels
                                    }
                                }
                            } catch (Exception | LinkageError ex) {
                                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                            }
                            if (supported) {
                                mi = new JMenuItem(items[j].getDisplayName());
                                mi.putClientProperty("lafInfo", new LookAndFeelItem(pitem)); // NOI18N
                                mi.addActionListener(this);
                                popup.add(mi);
                            }
                        }
                    }
                }

                initialized = true;
            }
            return popup;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object o = e.getSource();
            if (o instanceof JComponent) {
                JComponent source = (JComponent) o;
                LookAndFeelItem item = (LookAndFeelItem) source.getClientProperty("lafInfo"); // NOI18N
                try {
                    selectedLaf = item.getLAFClass();
                    run();
                } catch (Exception ex) {
                    ErrorManager.getDefault().notify(ex);
                }
            }
        }
    }

    /**
     * Information about one look and feel.
     */
    static class LookAndFeelItem {

        /**
         * Name of the look and feel's class.
         */
        private String className;
        /**
         * The corresponding PaletteItem, if exists.
         */
        private PaletteItem pitem;

        public LookAndFeelItem(String className) {
            this.className = className;
        }

        public LookAndFeelItem(PaletteItem pitem) {
            this.pitem = pitem;
            this.className = pitem.getComponentClassName();
        }

        public String getClassName() {
            return className;
        }

        public Class<?> getLAFClass() throws ClassNotFoundException {
            Class<?> clazz;
            if (pitem == null) {
                if (className == null) {
                    clazz = UIManager.getLookAndFeel().getClass();
                } else {
                    clazz = Class.forName(className);
                }
            } else {
                clazz = pitem.getComponentClass();
            }
            return clazz;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LookAndFeelItem)) {
                return false;
            }
            LookAndFeelItem item = (LookAndFeelItem) obj;
            return (pitem == item.pitem) && ((pitem != null)
                    || ((className == null) ? (item.className == null) : className.equals(item.className)));
        }

        @Override
        public int hashCode() {
            return (className == null) ? pitem.hashCode() : className.hashCode();
        }
    }
}
