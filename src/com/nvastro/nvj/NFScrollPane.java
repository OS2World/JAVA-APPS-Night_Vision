/*
 * NFScrollPane.java  -  Non-focusable JScrollPane (Override of JScrollPane)
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

import javax.swing.JScrollPane;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Non-focusable JScrollPane (Override of JScrollPane).
 * The scrollbars of a JScrollPane are focus-traversable starting with Java
 * 1.4.  This code prevents this, but also works with 1.3.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class NFScrollPane extends JScrollPane {

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public NFScrollPane() {
    super();
    init();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param view The component to display in the scrollpane's viewport
   */
  public NFScrollPane(Component view) {
    super(view);
    init();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialization.
   */
  private void init() {
    // Keep scrolls out of focus loop
    try {
      getHorizontalScrollBar().setFocusable(false);
      getVerticalScrollBar().setFocusable(false);
    }
    catch ( Exception e ) { }
  }
}

