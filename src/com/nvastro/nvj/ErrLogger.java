/*
 * ErrLogger.java  -  Logs errors, and displays them in a dialog
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
import java.text.MessageFormat;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Logs errors, and displays them in a dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class ErrLogger extends EscapeDlg {
  static private Vector<String> errors = new Vector<String>();
  static private int num = 0;
  static private JMenuItem menuItem1 = null, menuItem2 = null;
  static private ErrLogger el = null;
  static private ROTextArea ta = null;  // A scrollable read-only JTextArea

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Logs error.  Assumes no \n at end of line.
   */
  public static void logError(String err) {
    errors.addElement(err);
    num++;
    if ( el != null && ta != null ) {
      ta.append(err + "\n");
      // repaint is automatic
    }
    if ( menuItem1 != null ) menuItem1.setEnabled(true);
    if ( menuItem2 != null ) menuItem2.setEnabled(true);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns number of errors.
   */
  public static int getNumberOfErrors() {
    return num;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up control of menu items for main window, and sets initial values.
   */
  public static void setupErrorMenuItems(JMenuItem item1, JMenuItem item2) {
    menuItem1 = item1;
    menuItem2 = item2;

    if ( menuItem1 != null ) menuItem1.setEnabled(num != 0);
    if ( menuItem2 != null ) menuItem2.setEnabled(num != 0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Displays errors in a dialog box.
   *
   * @param owner Main window
   */
  public static void displayErrors(Frame owner) {
    if ( el == null ) {
      el = new ErrLogger(owner);
    }
    el.setVisible(true);
    el.toFront();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * (Private) Constructor (builds dialog box).
   * @param owner Main window
   */
  private ErrLogger(final Frame owner) {
    /* Set window name */
    super(owner, TextBndl.getString("Messages.Title"), false);
    /* Keep default HIDE_ON_CLOSE */

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ErrLogger.this.close(); // Pop down dialog
      }
    };

    /* Create controls for this window */
    ta = new ROTextArea("", 15, 50);

    /* Load up the ROTextArea */
    for ( int i = 0; i < num; i++ ) {
      ta.append((String)errors.elementAt(i) + "\n");
    }

    /* Create some buttons */
    JButton Close = new JButton(TextBndl.getString("Dlg.Close"));
    Close.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"messages");
    setHelpPage("messages");

    /* Create a Box and add buttons for OK, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(Close);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(ta);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(Close);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(ta);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * A convenience function that combines the components
   * into a resulting string.
   *
   * @param a A string containing {0}
   * @param b A string to be substituted for {0} in string a
   * @param c If not null, a string to be appended to string a
   * @return The resulting string
   */
  public static String formatError(String a, String b, String c) {
    String msg;
    Object[] args = { b };
    try {
      msg = MessageFormat.format(a, args);
    } catch ( Exception e2 ) { msg = a; }
    if ( c == null ) return msg;
    else             return msg + c;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * A convenience function to display an error message
   * (which is appended by a program termination message)
   * and exit(1).
   *
   * @param msg Error message
   */
  public static void die(String msg) {
    System.err.println(msg + "\n" + TextBndl.getString("Pgm.Term"));
    OptionDlg.showMessageDialog(Nvj.parentFrame,
                         msg + "\n" + TextBndl.getString("Pgm.Term"),
                         Nvj.PgmName, JOptionPane.ERROR_MESSAGE);
    System.exit(1);
  }

  /* For testing */
  //public static void main(String args[]) {
  //  ErrLogger.logError("Error 1");
  //  ErrLogger.logError("Error 2");
  //  for ( int i = 0; i < num; i++ ) {
  //    System.out.println((String)errors.elementAt(i));
  //  }
  //  ErrLogger.displayErrors(null);
  //  System.out.println("Press \"Ctrl-C\" after dismissing dialog");
  //  //stem.exit(0);
  //}
}

