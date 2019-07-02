/*
 * DecNumFormat.java  -  Improvement over NumberFormat to prevent "-0.0"
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

import java.text.*; // NumberFormat


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Improvement over NumberFormat to prevent "-0.0".
 *
 * @author Brian Simpson
 */
public class DecNumFormat {
  NumberFormat fmt = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param pattern Pattern for formatted String (e.g. "0.0"); See NumberFormat
   *                for examples
   */
  public DecNumFormat(String pattern) {
    fmt = NumberFormat.getInstance();

    if ( fmt instanceof DecimalFormat ) {
      ((DecimalFormat)fmt).applyPattern(pattern); // (rounds)
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns value as formatted String.
   *
   * @param val Value to be formatted
   */
  public String format(double val) {
    // (Make "-0.01" appear as "0.0", not "-0.0")
    String str = fmt.format(val);
    if ( str.charAt(0) == '-' && Double.parseDouble(str) == 0.0 )
      return str.substring(1);
    else return str;
  }
}

