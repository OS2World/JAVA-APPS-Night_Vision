/*
 * NFScrollBar.java  -  Non-focusable JScrollBar (Override of JScrollBar)
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

import javax.swing.JScrollBar;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Non-focusable JScrollBar (Override of JScrollBar).
 * JScrollBar's are focus-traversable starting with Java 1.4.
 * This code prevents this, but also works with 1.3.
 * This code should also prevent response to keystrokes that occur with 1.4.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class NFScrollBar extends JScrollBar {

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.  Default orientation (JScrollBar.VERTICAL).
   */
  public NFScrollBar() {
    super();
    init();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param orientation Choose JScrollBar.HORIZONTAL or JScrollBar.VERTICAL
   */
  public NFScrollBar(int orientation) {
    super(orientation);
    init();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialization.
   */
  private void init() {
    // Keep scrolls out of focus loop
    try { setFocusable(false); }
    catch ( Exception e ) { }
  }
}

