/*
 * TimeRateDlg.java  -  "Set time rates" dialog
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Set time rates" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class TimeRateDlg extends EscapeDlg {
  private EComboBox update, rate;
  private ActionListener listeners = null;
  private Preferences prefer;
  static private TimeRateDlg dlg = null;

  /* The following two must be kept in sync with string values ! */
  private static final int UPDTVALUES[] = { 1, 2, 4, 8, 15, 30, 60, 120 };
  private static final int TMSPVALUES[] = { 1, 10, 60, 1440, 10080,
    -10080, -1440, -60, -10, -1 };
  // Note:  There is a static final in Preferences:  MAXTIMEFACTOR = 10080
  //        that must >= to the above.  (Yes, it's redundant...)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new TimeRateDlg(owner, prefer);
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
  private TimeRateDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("TimeRateDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          TimeRateDlg.this.close(); // Pop down dialog
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(TimeRateDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Create controls for this window */
    update = new EComboBox();
      /* Keep the following is sync with UPDTVALUES above! */
      update.addItem(TextBndl.getString("TimeRateDlg.1S"));
      update.addItem(TextBndl.getString("TimeRateDlg.2S"));
      update.addItem(TextBndl.getString("TimeRateDlg.4S"));
      update.addItem(TextBndl.getString("TimeRateDlg.8S"));
      update.addItem(TextBndl.getString("TimeRateDlg.15S"));
      update.addItem(TextBndl.getString("TimeRateDlg.30S"));
      update.addItem(TextBndl.getString("TimeRateDlg.1M"));
      update.addItem(TextBndl.getString("TimeRateDlg.2M"));
    rate = new EComboBox();
      /* Keep the following is sync with TMSPVALUES above! */
      rate.addItem(TextBndl.getString("TimeRateDlg.1X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.10X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.60X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.1440X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.10080X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.M10080X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.M1440X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.M60X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.M10X"));
      rate.addItem(TextBndl.getString("TimeRateDlg.M1X"));
    loadPrefer();
    JLabel L1 = new JLabel(TextBndl.getString("TimeRateDlg.Update"));
    JLabel L2 = new JLabel(TextBndl.getString("TimeRateDlg.Advance"));

    /* Create a box to hold labels and comboboxes */
    Box a = Box.createVerticalBox();
    a.add(L1);
    a.add(Box.createVerticalStrut(6));
    update.setAlignmentX(0.0f); // Allows left justified labels
    a.add(update);
    a.add(Box.createVerticalStrut(12));
    a.add(L2);
    a.add(Box.createVerticalStrut(6));
    rate.setAlignmentX(0.0f);   // Allows left justified labels
    a.add(rate);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"timerate");
    setHelpPage("timerate");

    /* Create a box and add buttons for OK, Apply, Cancel, & Help */
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
    setFirstFocus(update);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    int i;
    int up = prefer.getUpdatePeriod();
    int ts = prefer.getTimeSpeed();

    for ( i = 0; i < UPDTVALUES.length; i++ ) {
      if ( UPDTVALUES[i] == up ) {
        update.setSelectedIndex(i); break;
      }
    }
    for ( i = 0; i < TMSPVALUES.length; i++ ) {
      if ( TMSPVALUES[i] == ts ) {
        rate.setSelectedIndex(i); break;
      }
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    prefer.setUpdatePeriod(UPDTVALUES[update.getSelectedIndex()]);
    prefer.setTimeSpeed(TMSPVALUES[rate.getSelectedIndex()]);
  }
}

