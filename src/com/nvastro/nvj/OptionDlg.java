/*
 * OptionDlg.java  -  JOptionPane with non-resizable border
 * Copyright (C) 2011-2012 Brian Simpson
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
import javax.swing.*;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * JOptionPane with non-resizable border.
 *
 * @author Brian Simpson
 */
public class OptionDlg extends Object {

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private OptionDlg() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Brings up a dialog displaying a message using a default icon
   * determined by the messageType parameter.
   *
   * @param parentComponent Parent of this dialog
   * @param message The message to display
   * @param title The title string for the dialog
   * @param messageType The type of message to be displayed:
   *        JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
   *        JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE,
   *        or JOptionPane.PLAIN_MESSAGE
   */
  static public void showMessageDialog(Component parentComponent,
                                Object message, String title, int messageType) {
    showMessageDialog(parentComponent, message, title, messageType, null);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Brings up a dialog displaying a message.
   *
   * @param parentComponent Parent of this dialog
   * @param message The message to display
   * @param title The title string for the dialog
   * @param messageType The type of message to be displayed:
   *        JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
   *        JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE,
   *        or JOptionPane.PLAIN_MESSAGE
   * @param icon An icon to display in the dialog that helps the user identify
   *        the kind of message that is being displayed
   */
  static public void showMessageDialog(Component parentComponent,
                                Object message, String title, int messageType,
                                Icon icon) {
    JOptionPane pane = new JOptionPane(message, messageType);
    if ( icon != null ) pane.setIcon(icon);
    JDialog dialog = pane.createDialog(parentComponent, title);
    // Note:  Dft operation when user initiates a "close" is hide dlg
    dialog.setResizable(false);
    dialog.setVisible(true);
    dialog.dispose();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Brings up a dialog where the number of choices is determined by
   * the optionType parameter, where the messageType parameter determines
   * the icon to display.
   *
   * @param parentComponent Parent of this dialog
   * @param message The message to display
   * @param title The title string for the dialog
   * @param optionType The options available on the dialog:
   *        JOptionPane.YES_NO_OPTION, or JOptionPane.YES_NO_CANCEL_OPTION
   * @param messageType The type of message to be displayed:
   *        JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
   *        JOptionPane.WARNING_MESSAGE, JOptionPane.QUESTION_MESSAGE,
   *        or JOptionPane.PLAIN_MESSAGE
   * @return An integer indicating the option selected by the user
   */
  static public int showConfirmDialog(Component parentComponent, Object message,
                               String title, int optionType, int messageType) {
    JOptionPane pane = new JOptionPane(message, messageType, optionType);
    JDialog dialog = pane.createDialog(parentComponent, title);
    // Note:  Dft operation when user initiates a "close" is hide dlg
    dialog.setResizable(false);
    dialog.setVisible(true);
    dialog.dispose();
    // Code derived from JOptionPane page
    Object selectedValue = pane.getValue();
    if ( selectedValue != null && selectedValue instanceof Integer )
      return ((Integer)selectedValue).intValue();
    return JOptionPane.CLOSED_OPTION;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  int rc = showConfirmDialog(null, "Yes or No", "Pick one",
  //               JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
  //  System.out.print("rc = " + rc);
  //  if      ( rc == JOptionPane.YES_OPTION )
  //    System.out.println(", (YES)");
  //  else if ( rc == JOptionPane.NO_OPTION )
  //    System.out.println(", (NO)");
  //  else
  //    System.out.println(", (whatever)");
  //  System.exit(0);
  //}
}

