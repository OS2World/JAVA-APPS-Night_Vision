/*
 * Matrix3x1.java  -  3x1 matrix
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


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * 3x1 matrix class.
 *
 * @author Brian Simpson
 */
public class Matrix3x1 {
  double num[];                 // Dft visibility (public to only this pkg)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for 0 matrix.
   */
  public Matrix3x1() {
    num = new double[3];               // double primitives init to 0
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for specific values.
   *
   * @param a 1st value
   * @param b 2nd value
   * @param c 3rd value
   */
  public Matrix3x1(double a, double b, double c) {
    num = new double[3];               // double primitives init to 0
    num[0] = a; num[1] = b; num[2] = c;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Clone method.
   */
  public Object clone() {
    // Need to override dft clone, otherwise only "shallow" copy is made
    return new Matrix3x1(num[0], num[1], num[2]);
  }
}

