/*
 * EscapeDlg.java  -  Override of JDialog
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

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
// KeyAdapter


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Override of JDialog that will hide when Escape is pressed.
 * (And will hide when closed via clicking on "X".)
 * Provides facility for setting the component which is to receive
 * first focus.  Implements help page functions.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class EscapeDlg extends JDialog {
  private Component firstfocus = null;
  private String helppage = null;
  /** Dialog owner - main star window */
  protected Frame owner = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param owner Main window
   * @param title Title of dialog
   * @param modal True for a modal dialog, false for one that allows
   *              other windows to be active at the same time
   */
  EscapeDlg(Frame owner, String title, boolean modal) {
    super(owner, title, modal);
    this.owner = owner;

    /* Handle closing (keep dft HIDE_ON_CLOSE) */

    /* Set up close (hide) via Escape keystroke */
    Action close = new AbstractAction("close") {
      public void actionPerformed(ActionEvent e) {
        EscapeDlg.this.close();
      }
    };
    JRootPane rp = getRootPane();
    rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                     .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
    rp.getActionMap().put("close", close);
    /* Set up help via F1 keystroke */
    Action f1 = new AbstractAction("f1") {
      public void actionPerformed(ActionEvent e) {
        HelpWin.showHelpPage(helppage);
      }
    };
    rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                     .put(KeyStroke.getKeyStroke("F1"), "f1");
    rp.getActionMap().put("f1", f1);

    addWindowListener(new WindowAdapter() {
      /* Sets focus when window is first opened */
      public void windowOpened(WindowEvent e) {
        if ( firstfocus != null ) firstfocus.requestFocus();
      }

      /* Cleans up menu residue */                // Called when closed
      public void windowClosing(WindowEvent e) {  //   with system menu
        if ( EscapeDlg.this.owner != null ) EscapeDlg.this.owner.repaint();
      }
    });
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Hides the dialog (and repaints the owner).
   */
  public void close() {
    if ( owner != null ) owner.repaint();
    setVisible(false);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the help page for this dialog.
   */
  public void setHelpPage(String page) {
    helppage = page;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the help page for this dialog.
   */
  public void showHelpPage() {
    HelpWin.showHelpPage(helppage);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets which component receives focus first.
   *
   * @param c The component to receive first focus
   */
  public void setFirstFocus(Component c) {
    firstfocus = c;
  }
}

