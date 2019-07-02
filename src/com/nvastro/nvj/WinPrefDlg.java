/*
 * WinPrefDlg.java  -  "Window preferences" dialog
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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Window preferences" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class WinPrefDlg extends EscapeDlg {
  final private JCheckBox tbar, info, azalt, zoom, field; //, antialias;
  private ActionListener listeners = null;
  final private Preferences prefer;
  static private WinPrefDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new WinPrefDlg(owner, prefer);
    else {
      dlg.loadPrefer();
    }

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private WinPrefDlg(final Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("WinPrefDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          WinPrefDlg.this.close();   // Pop down dialog
        if ( !cmd.equals("Cancel") ) {
          updatePrefer();
          if ( listeners != null ) {
            ((Nvj)owner).updateLayout();
            listeners.actionPerformed(new ActionEvent(WinPrefDlg.this,
                                      e.getID(), "update"));
          }
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Create controls for this window */
    tbar  = new JCheckBox(TextBndl.getString("WinPrefDlg.ToolBar"));
    info  = new JCheckBox(TextBndl.getString("WinPrefDlg.WinInfo"));
    azalt = new JCheckBox(TextBndl.getString("WinPrefDlg.ScrlAzAlt"));
    zoom  = new JCheckBox(TextBndl.getString("WinPrefDlg.ScrlZoom"));
    field = new JCheckBox(TextBndl.getString("WinPrefDlg.ScrlField"));
    //antialias = new JCheckBox(TextBndl.getString("WinPrefDlg.AntiAlias"));
    loadPrefer();

    /* Create a box to hold controls */
    Box a = Box.createVerticalBox();
    tbar.setAlignmentX(0.0f);
    a.add(tbar);
    a.add(Box.createVerticalStrut(9));
    info.setAlignmentX(0.0f);
    a.add(info);
    a.add(Box.createVerticalStrut(9));
    azalt.setAlignmentX(0.0f);
    a.add(azalt);
    a.add(Box.createVerticalStrut(9));
    zoom.setAlignmentX(0.0f);
    a.add(zoom);
    a.add(Box.createVerticalStrut(9));
    field.setAlignmentX(0.0f);
    a.add(field);
    //a.add(Box.createVerticalStrut(9));
    //antialias.setAlignmentX(0.0f);
    //a.add(antialias);

    /* Create some buttons */
    JButton OK = new JButton(TextBndl.getString("Dlg.OK"));
    OK.setActionCommand("OK");
    OK.addActionListener(listener);
    JButton Apply = new JButton(TextBndl.getString("Dlg.Apply"));
    Apply.setActionCommand("Apply");
    Apply.addActionListener(listener);
    JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
    Cancel.setActionCommand("Cancel");
    Cancel.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "setwin");
    setHelpPage("setwin");

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

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(a);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(tbar);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    tbar.setSelected(prefer.showToolBar());
    info.setSelected(prefer.showWinInfo());
    azalt.setSelected(prefer.showScrlAzAlt());
    zoom.setSelected(prefer.showScrlZoom());
    field.setSelected(prefer.showScrlField());
    //antialias.setSelected(prefer.antialiasing);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    prefer.showToolBar(tbar.isSelected());
    prefer.showWinInfo(info.isSelected());
    prefer.showScrlAzAlt(azalt.isSelected());
    prefer.showScrlZoom(zoom.isSelected());
    prefer.showScrlField(field.isSelected());
    //prefer.antialiasing = antialias.isSelected();
  }
}

