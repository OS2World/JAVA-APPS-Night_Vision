/*
 * FindDlg.java  -  "Find ..." dialog
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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Find ..." dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class FindDlg extends EscapeDlg {
  private JList<String> list;
  private JButton OK;
  private JButton Apply;
  private Preferences prefer;
  private SkyObject skyobj = null;

  /** Constellation   */ static final public int CON    = 0;
  /** Star name       */ static final public int STARNM = 1;
  /** Deep sky object */ static final public int DS     = 2;
  /** Near sky object */ static final public int NS     = 3;
  private int dlgtype;
  static private FindDlg dlg0 = null;
  static private FindDlg dlg1 = null;
  static private FindDlg dlg2 = null;
  static private FindDlg dlg3 = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   * @param type CON, STARNM, DS, or NS
   */
  public static void showDlg(Frame owner, Preferences prefer, int type) {
    if      ( type == CON ) {
      if ( dlg0 == null ) dlg0 = new FindDlg(owner, prefer, type,
                                     TextBndl.getString("FindConstDlg.Title"));
      dlg0.setVisible(true);
    }
    else if ( type == STARNM ) {
      if ( dlg1 == null ) dlg1 = new FindDlg(owner, prefer, type,
                                     TextBndl.getString("FindStarDlg.Title"));
      dlg1.setVisible(true);
    }
    else if ( type == DS ) {
      if ( dlg2 == null ) dlg2 = new FindDlg(owner, prefer, type,
                                     TextBndl.getString("FindDSDlg.Title"));
      dlg2.setVisible(true);
    }
    else if ( type == NS ) {
      if ( dlg3 == null ) dlg3 = new FindDlg(owner, prefer, type,
                                     TextBndl.getString("FindNSDlg.Title"));
      dlg3.setVisible(true);
    }
    /* else ignore */
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   * @param type CON, STARNM, DS, or NS
   * @param title Title of dialog
   */
  private FindDlg(final Frame owner, Preferences prefer,int type,String title) {
    /* Set window name */
    super(owner, title, false);
    this.prefer = prefer;
    int i;

    /* Set which dialog we are doing */
    dlgtype = type;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          if ( setSkyObject() == false )
            return; // Don't close dlg

          ((Nvj)owner).gotoObject(skyobj);
        }
        if ( !cmd.equals("Apply") )
          FindDlg.this.close(); // Pop down dialog
      }
    };

    /* Look for double clicks */
    MouseListener mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if ( e.getClickCount() == 2 ) {
          //int index = list.locationToIndex(e.getPoint());
          if ( setSkyObject() == false )
            return;

          ((Nvj)owner).gotoObject(skyobj);
        }
      }
    };

    /* Get data to display */
    String[] data;
    if ( dlgtype == CON ) {
      data = new String[89];
      for ( i = 0; i < 89; i++ ) data[i] = Constellation.tellName(i);
    }
    else if ( dlgtype == STARNM ) {
      StarNameDB db = new StarNameDB();
      int num = db.getNumberOfNames();
      data = new String[num];
      for ( i = 0; i < num; i++ ) data[i] = db.tellName2(i);
    }
    else if ( dlgtype == DS ) {
      DeepSkyDB db = new DeepSkyDB();
      int num = db.getNumberOfObjects();
      data = new String[num];
      for ( i = 0; i < num; i++ ) data[i] = db.tellName2(i);
    }
    else /* ( dlgtype == NS ) */ {
      NearSkyDB db = new NearSkyDB();
      int num = db.getNumberOfObjects();
      data = new String[num];
      for ( i = 0; i < num; i++ ) data[i] = db.tellName(i);
    }

    /* Create controls for this window */
    list = new JList<String>(data);
    list.setVisibleRowCount(7);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    NFScrollPane slist = new NFScrollPane(list);
    addKeyBindings();

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "find");
    setHelpPage("find");

    /* Create a Box and add buttons for OK, Apply, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(OK);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Apply);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Cancel);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Now that the OK/Apply buttons exist, create a ListSelectionListener
       whose only purpose in life is to enable them */
    ListSelectionListener selectListener  = new ListSelectionListener () {
      public void valueChanged(ListSelectionEvent e) {
        boolean enable = (list.getSelectedIndex() >= 0) ? true : false;
        OK.setEnabled(enable);
        Apply.setEnabled(enable);
      }
    };
    list.addMouseListener(mouseListener);
    list.addListSelectionListener(selectListener);

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(slist);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(list);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets skyobj to the object selected (or null for none).
   */
  private boolean setSkyObject() {
    int select = list.getSelectedIndex();

    if      ( dlgtype == CON ) {
      skyobj = new SkyObject(SkyObject.CON, select);
    }
    else if ( dlgtype == STARNM ) {
      skyobj = new SkyObject(SkyObject.STARNM, select);
    }
    else if ( dlgtype == DS ) {
      skyobj = new SkyObject(SkyObject.DS, select);
    }
    else { /* dlgtype == NS */
      skyobj = new SkyObject(SkyObject.NS, select);
    }

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
  private void addKeyBindings() {
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

