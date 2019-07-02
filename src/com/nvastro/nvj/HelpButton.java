/*
 * HelpButton.java  -  Extends JButton to add activation of help page
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Extends JButton to add activation of help page.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class HelpButton extends JButton {
  private String helppage = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for button with standard help page.
   *
   * @param text Text for help button
   */
  public HelpButton(String text) { this(text, null); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for button with specific help page.
   *
   * @param text Text for help button
   * @param page Associated help page
   */
  public HelpButton(String text, String page) {
    super(text);

    helppage = page;
    addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        HelpWin.showHelpPage(helppage);
      }
    });

    /* Prevent button from requesting focus */
    setRequestFocusEnabled(false);
  }
}

