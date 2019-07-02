/*
 * FindStarDlg.java  -  "Find star" dialog
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Find star" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class FindStarDlg extends EscapeDlg {
  private JList<String> namelist, conslist, dsgnlist;
  private JButton OK;
  private JButton Apply;
  private Preferences prefer;
  private SkyObject skyobj = null;

  static private FindStarDlg dlg = null;
  static private StarDsgnDB sddb = null;
  static private boolean ignorefeedback = false;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new FindStarDlg(owner, prefer);

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private FindStarDlg(final Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("FindStarDlg.Title"), false);
    this.prefer = prefer;
    String[] data, cons, dsgn;
    int i, num;

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
          FindStarDlg.this.close(); // Pop down dialog
      }
    };

    /* Get data to display */
    StarNameDB db = new StarNameDB();
    num = db.getNumberOfNames();
    data = new String[num];
    for ( i = 0; i < num; i++ ) data[i] = db.tellName2(i);
    // - - -
    cons = new String[88];
    for ( i = 0; i <  88; i++ ) cons[i] = Constellation.tellName88(i);
    // - - -
    sddb = new StarDsgnDB();
    num = sddb.getNumberOfDsgns(3); // Arbitrarily pick constellation 3
    dsgn = new String[num];
    for ( i = 0; i < num; i++ )
      dsgn[i] = sddb.tellDesignation(3, i);

    /* Create controls for this window */
    namelist = new JList<String>(data);
    namelist.setVisibleRowCount(7);
    namelist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    NFScrollPane snamelist = new NFScrollPane(namelist);
    addKeyBindings(namelist);
    // - - -
    conslist = new JList<String>(cons);
    conslist.setVisibleRowCount(7);
    conslist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    NFScrollPane sconslist = new NFScrollPane(conslist);
    addKeyBindings(conslist);
    // - - -
    dsgnlist = new JList<String>(dsgn);
    dsgnlist.setVisibleRowCount(5);
    dsgnlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    NFScrollPane sdsgnlist = new NFScrollPane(dsgnlist);
    addKeyBindings(dsgnlist);
    dsgnlist.setEnabled(false);

    /* Create vertical box */
    Box v = Box.createVerticalBox();
    v.add(new JLabel(TextBndl.getString("FindStarDlg.SelectName")));
    v.add(snamelist);
    v.add(Box.createVerticalStrut(7));
    v.add(new JLabel(TextBndl.getString("FindStarDlg.SelectDsgn")));
    v.add(sconslist);
    v.add(sdsgnlist);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"findstar");
    setHelpPage("findstar");

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
       to manipulate them and the listboxes */
    ListSelectionListener selectListener  = new ListSelectionListener () {
      public void valueChanged(ListSelectionEvent e) {
        if ( ignorefeedback ) return;
        ignorefeedback = true;

        boolean btnenable = false;
        @SuppressWarnings("unchecked")
        JList<String> source = (JList<String>)e.getSource();

        if ( source == namelist ) {
          conslist.clearSelection();
          dsgnlist.clearSelection();
          dsgnlist.setEnabled(false);
          if ( namelist.getSelectedIndex() >= 0 ) btnenable = true;
        }
        else if ( source == conslist ) {
          namelist.clearSelection();
          int con = conslist.getSelectedIndex();
          if ( con >= 0 ) {
            int num = sddb.getNumberOfDsgns(con);
            String[] dsgn = new String[num];
            for ( int i = 0; i < num; i++ )
              dsgn[i] = sddb.tellDesignation(con, i);
            dsgnlist.setListData(dsgn);
            dsgnlist.setEnabled(true);
          }
          else {
            dsgnlist.clearSelection();
            dsgnlist.setEnabled(false);
          }
        }
        else /* source == dsgnlist */ {
          if ( dsgnlist.getSelectedIndex() >= 0 ) btnenable = true;
        }

        OK.setEnabled(btnenable);
        Apply.setEnabled(btnenable);

        ignorefeedback = false;
      }
    };
    namelist.addListSelectionListener(selectListener);
    conslist.addListSelectionListener(selectListener);
    dsgnlist.addListSelectionListener(selectListener);

    /* Look for double clicks */
    MouseListener mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if ( e.getClickCount() == 2 ) {
          @SuppressWarnings("unchecked")
          JList<String> source = (JList<String>)e.getSource();
          if ( source == dsgnlist && !dsgnlist.isEnabled() )
            return;
          if ( setSkyObject() == false )
            return;

          ((Nvj)owner).gotoObject(skyobj);
        }
      }
    };
    namelist.addMouseListener(mouseListener);
    dsgnlist.addMouseListener(mouseListener);

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(v);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(namelist);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets skyobj to the object selected (or null for none).
   */
  private boolean setSkyObject() {
    int select = namelist.getSelectedIndex();
    if ( select >= 0 ) {
      skyobj = new SkyObject(SkyObject.STARNM, select);
    }
    else {
      int con = conslist.getSelectedIndex();
      int idx = dsgnlist.getSelectedIndex();
      select = sddb.getStarIndex(con, idx);
      if ( select < 0 ) return false; // Should not happen

      skyobj = new SkyObject(SkyObject.STAR, select);
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

