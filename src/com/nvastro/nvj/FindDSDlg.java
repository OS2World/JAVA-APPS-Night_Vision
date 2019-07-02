/*
 * FindDSDlg.java  -  "Find deep sky object" dialog
 * Copyright (C) 2011-2017 Brian Simpson
 * This file is part of Night Vision.
 *
 * Night Vision is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Night Vision is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Night Vision.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.nvastro.nvj;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Find deep sky object" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class FindDSDlg extends EscapeDlg {
  private JCheckBox galxy;
  private JCheckBox globc;
  private JCheckBox openc;
  private JCheckBox pltnb;
  private JCheckBox dffnb;
  private JCheckBox drknb;
  private JCheckBox other;
  private JTextField text;
  private JList<String> list;
  private int[] map;
  private JButton OK;
  private JButton Apply;
  private ActionListener listeners = null;
  private Preferences prefer;
  private SkyObject skyobj = null;

  static private FindDSDlg dlg = null;
  static private boolean ignorefeedback = false;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new FindDSDlg(owner, prefer);

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private FindDSDlg(final Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("FindDSDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          if ( setSkyObject() == false )
            return; // Don't close dlg

          ((Nvj)owner).gotoObject(skyobj);
        }
        /* Add capability to change DS DB via LoadDSDlg */
        if ( cmd.equals("DSFile") ) {  // Keep half-baked?!?
          File file = LoadDSDlg.showDlg(FindDSDlg.this);

          if ( file != null ) {
            try {
              DeepSkyDB.reInit(file);

              resetFilter();
              setListData();

              if ( listeners != null )
              listeners.actionPerformed(new ActionEvent(FindDSDlg.this,
                                        e.getID(), "update"));
            } finally { }
          }
        }
        else if ( !cmd.equals("Apply") )
          FindDSDlg.this.close(); // Pop down dialog
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Add a ItemListener for checkboxes */
    ItemListener itemListener = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        //if (e.getStateChange() == ItemEvent.SELECTED) {
        //} else { }
        setListData();
      }
    };

    /* Add a ListSelectionListener to control state of OK/Apply buttons */
    ListSelectionListener selectListener  = new ListSelectionListener () {
      public void valueChanged(ListSelectionEvent e) {
        boolean enable = (list.getSelectedIndex() >= 0) ? true : false;
        OK.setEnabled(enable);
        Apply.setEnabled(enable);
      }
    };

    /* Add a MouseListener for double clicks on list */
    MouseListener mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if ( e.getClickCount() == 2 ) {
          if ( setSkyObject() == false )
            return;

          ((Nvj)owner).gotoObject(skyobj);
        }
      }
    };

    /* Add a DocumentListener for changes in filter text */
    DocumentListener docListener = new DocumentListener() {
      public void changedUpdate(DocumentEvent e) { }
      public void insertUpdate(DocumentEvent e) {
        setListData();
      }
      public void removeUpdate(DocumentEvent e) {
        setListData();
      }
    };

    /* Create filter controls */
    galxy = new JCheckBox(TextBndl.getString("FindDSDlg.Galaxies"), true);
    globc = new JCheckBox(TextBndl.getString("FindDSDlg.GlobClstr"), true);
    openc = new JCheckBox(TextBndl.getString("FindDSDlg.OpenClstr"), true);
    pltnb = new JCheckBox(TextBndl.getString("FindDSDlg.PlanetNeb"), true);
    dffnb = new JCheckBox(TextBndl.getString("FindDSDlg.DiffuseNeb"), true);
    drknb = new JCheckBox(TextBndl.getString("FindDSDlg.DarkNeb"), true);
    other = new JCheckBox(TextBndl.getString("FindDSDlg.Other"), true);
    text  = new JTextField(20);
    galxy.addItemListener(itemListener);
    globc.addItemListener(itemListener);
    openc.addItemListener(itemListener);
    pltnb.addItemListener(itemListener);
    dffnb.addItemListener(itemListener);
    drknb.addItemListener(itemListener);
    other.addItemListener(itemListener);
    text.getDocument().addDocumentListener(docListener);
    JPanel filterpanel = new JPanel();
    filterpanel.setBorder(new TitledBorder(
                          TextBndl.getString("FindDSDlg.Filter")));
    filterpanel.setLayout(new GridLayout(4,2));
    filterpanel.add(galxy);
    filterpanel.add(dffnb);
    filterpanel.add(globc);
    filterpanel.add(pltnb);
    filterpanel.add(openc);
    filterpanel.add(drknb);
    filterpanel.add(other);
    filterpanel.add(text);

    /* Create listbox */
    list = new JList<String>();
    list.setVisibleRowCount(7);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(selectListener);
    list.addMouseListener(mouseListener);
    NFScrollPane slist = new NFScrollPane(list);
    addKeyBindings(list);
    setListData();

    /* Create some buttons */
    OK = new JButton(TextBndl.getString("Dlg.OK"));
    OK.setActionCommand("OK");
    OK.addActionListener(listener);
    OK.setEnabled(false);            // OK initially disabled
    Apply = new JButton(TextBndl.getString("Dlg.Apply"));
    Apply.setActionCommand("Apply");
    Apply.addActionListener(listener);
    Apply.setEnabled(false);         // Apply initially disabled
    JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
    Cancel.setActionCommand("Cancel");
    Cancel.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "findds");
    setHelpPage("findds");

    /* Create a Box and add buttons for OK, Apply, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(OK);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Apply);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    { // For Giampiero & Clive; Will likely move someday
      String SlewOut = Preferences.SlewOut();
      if ( SlewOut != null && SlewOut.length() != 0 ) {
        JButton DSFile = new JButton("DS Catalog");
        DSFile.setActionCommand("DSFile");
        DSFile.addActionListener(listener);
        b.add(DSFile);
        b.add(Box.createHorizontalStrut(7));
        b.add(Box.createHorizontalGlue());
      }
    }
    b.add(Cancel);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(filterpanel, BorderLayout.NORTH);
    getContentPane().add(slist);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    Dimension dim = getPreferredSize();
    dim.width = 500;
    setPreferredSize(dim);
    pack();
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(list);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Resets the filter to include all objects.
   */
  private void resetFilter() {
    ignorefeedback = true;

    galxy.setSelected(true);
    globc.setSelected(true);
    openc.setSelected(true);
    pltnb.setSelected(true);
    dffnb.setSelected(true);
    drknb.setSelected(true);
    other.setSelected(true);
    text.setText("");

    ignorefeedback = false;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the data in the listbox.
   */
  private void setListData() {
    int i, j, type, num;
    boolean ga, gc, oc, pn, dn, dk, ot;
    String txt;

    if ( ignorefeedback ) return;

    ga = galxy.isSelected();
    gc = globc.isSelected();
    oc = openc.isSelected();
    pn = pltnb.isSelected();
    dn = dffnb.isSelected();
    dk = drknb.isSelected();
    ot = other.isSelected();
    txt = text.getText().trim().toLowerCase();
    //stem.out.println("Filter text = \"" + txt + "\"");
    if ( txt.length() == 0 ) txt = null;

    DeepSkyDB db = new DeepSkyDB();
    num = db.getNumberOfObjects();
    Vector<String> data = new Vector<String>(num);
    map = new int[num];
    for ( i = 0, j = 0; i < num; i++ ) {
      type = db.getGenType(i);

      /* Note:  The following must be kept in sync */
      /* with what getGenType in DeepSkyDB returns */
      if ( ((type == 0 && ga) ||
            (type == 1 && gc) ||
            (type == 2 && oc) ||
            (type == 3 && pn) ||
            (type == 4 && dn) ||
            (type == 5 && dk) ||
            (type == 6 && ot)) &&
           (txt == null ||    // Blank -> don't filter text
            db.tellName2(i).toLowerCase().contains(txt)) ) {
        data.addElement(db.tellName2(i));
        map[j++] = i;
      }
    }
    data.trimToSize();

    list.setListData(data);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets skyobj to the object selected (or null for none).
   */
  private boolean setSkyObject() {
    int select = list.getSelectedIndex();
    if ( select < 0 ) return false;

    skyobj = new SkyObject(SkyObject.DS, map[select]);

    if ( (skyobj.isViewable(prefer) == false) &&
         (OptionDlg.showConfirmDialog(this,
                                      TextBndl.getString("FindDlgW.NoSee"),
                                      TextBndl.getString("FindDlgW.Title"),
                                      JOptionPane.YES_NO_OPTION,
                                      JOptionPane.QUESTION_MESSAGE)
           != JOptionPane.YES_OPTION) ) {
           // != YES_OPTION correcty handles Esc pressed,
           // == NO_OPTION does not
      skyobj = null;
      return false;
    }
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add key bindings.
   */
  private void addKeyBindings(JList<String> list) {
    InputMap source =
             list.getInputMap(JComponent.WHEN_FOCUSED);
    InputMap target = list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

    /* Allow standard bindings to work */
    copyKeyBinding(KeyEvent.VK_UP,        0,               source, target);
    copyKeyBinding(KeyEvent.VK_DOWN,      0,               source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_UP,   0,               source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_DOWN, 0,               source, target);
    copyKeyBinding(KeyEvent.VK_LEFT,      0,               source, target);
    copyKeyBinding(KeyEvent.VK_RIGHT,     0,               source, target);
    // The following two don't appear to work when focus on buttons
    // copyKeyBinding(KeyEvent.VK_HOME,   Event.CTRL_MASK, source, target);
    // copyKeyBinding(KeyEvent.VK_END,    Event.CTRL_MASK, source, target);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Copy key binding.
   */
  private void copyKeyBinding(int code, int mod,
                                InputMap source, InputMap target) {
    KeyStroke stroke = KeyStroke.getKeyStroke(code, mod);
    target.put(stroke, source.get(stroke));
  }
}

