/*
 * Matrix3x3.java  -  3x3 matrix
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
 * 3x3 matrix class.
 *
 * @author Brian Simpson
 */
public class Matrix3x3 {
  double num[][];               // Dft visibility (public to only this pkg)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for identity matrix.
   */
  public Matrix3x3() {
    num = new double[3][3];                   // double primitives init to 0
    num[0][0] = num[1][1] = num[2][2] = 1.0;  // Identity matrix
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for specific matrix.
   *
   * Input parameters are values for each row.
   */
  public Matrix3x3(double a, double b, double c, double d, double e, double f,
                   double g, double h, double i) {
    num = new double[3][3];                   // double primitives init to 0
    set(a, b, c, d, e, f, g, h, i);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Clone method.
   */
  public Object clone() {
    // Need to override dft clone, otherwise only "shallow" copy is made
    return new Matrix3x3(num[0][0], num[0][1], num[0][2],
                         num[1][0], num[1][1], num[1][2],
                         num[2][0], num[2][1], num[2][2]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set method.
   *
   * Input parameters are values for each row.
   */
  public void set(double a, double b, double c, double d, double e, double f,
                  double g, double h, double i) {
    num[0][0] = a; num[0][1] = b; num[0][2] = c;
    num[1][0] = d; num[1][1] = e; num[1][2] = f;
    num[2][0] = g; num[2][1] = h; num[2][2] = i;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set method.
   *
   * @param m 3x3 matrix holding desired values
   */
  public void set(Matrix3x3 m) {
    num[0][0] = m.num[0][0]; num[0][1] = m.num[0][1]; num[0][2] = m.num[0][2];
    num[1][0] = m.num[1][0]; num[1][1] = m.num[1][1]; num[1][2] = m.num[1][2];
    num[2][0] = m.num[2][0]; num[2][1] = m.num[2][1]; num[2][2] = m.num[2][2];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies 2 3x3 matrices as m x n.
   *
   * @param m Left matrix
   * @param n Right matrix
   * @return Resulting matrix
   */
  public static Matrix3x3 mult(Matrix3x3 m, Matrix3x3 n) {
    return new Matrix3x3(
      m.num[0][0]*n.num[0][0]+m.num[0][1]*n.num[1][0]+m.num[0][2]*n.num[2][0],
      m.num[0][0]*n.num[0][1]+m.num[0][1]*n.num[1][1]+m.num[0][2]*n.num[2][1],
      m.num[0][0]*n.num[0][2]+m.num[0][1]*n.num[1][2]+m.num[0][2]*n.num[2][2],
      m.num[1][0]*n.num[0][0]+m.num[1][1]*n.num[1][0]+m.num[1][2]*n.num[2][0],
      m.num[1][0]*n.num[0][1]+m.num[1][1]*n.num[1][1]+m.num[1][2]*n.num[2][1],
      m.num[1][0]*n.num[0][2]+m.num[1][1]*n.num[1][2]+m.num[1][2]*n.num[2][2],
      m.num[2][0]*n.num[0][0]+m.num[2][1]*n.num[1][0]+m.num[2][2]*n.num[2][0],
      m.num[2][0]*n.num[0][1]+m.num[2][1]*n.num[1][1]+m.num[2][2]*n.num[2][1],
      m.num[2][0]*n.num[0][2]+m.num[2][1]*n.num[1][2]+m.num[2][2]*n.num[2][2]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies 2 3x3 matrices as m x this.
   *
   * @param m Left matrix
   * @return Resulting matrix
   */
  public Matrix3x3 premult(Matrix3x3 m) {
    return mult(m, this);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies 2 3x3 matrices as this x n.
   *
   * @param n Right matrix
   * @return Resulting matrix
   */
  public Matrix3x3 postmult(Matrix3x3 n) {
    return mult(this, n);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies a 3x3 matrix with a 3x1 matrix.
   *
   * @param m Left (3x3) matrix
   * @param n Right (3x1) matrix
   * @return Resulting (3x1) matrix
   */
  public static Matrix3x1 mult(Matrix3x3 m, Matrix3x1 n) {
    return new Matrix3x1(
      m.num[0][0]*n.num[0] + m.num[0][1]*n.num[1] + m.num[0][2]*n.num[2],
      m.num[1][0]*n.num[0] + m.num[1][1]*n.num[1] + m.num[1][2]*n.num[2],
      m.num[2][0]*n.num[0] + m.num[2][1]*n.num[1] + m.num[2][2]*n.num[2]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies a 3x3 matrix (this) with a 3x1 matrix.
   *
   * @param n Right (3x1) matrix
   * @return Resulting (3x1) matrix
   */
  public Matrix3x1 mult(Matrix3x1 n) {
    return mult(this, n);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Inverts a 3x3 matrix.
   *
   * @return Resulting 3x3 matrix
   */
  public Matrix3x3 invert() {
    double d[][] = new double[3][3];
    int i, j, m, n, p, q;

    /* Calculate determinant */
    double det = num[0][0]*num[1][1]*num[2][2] + num[0][1]*num[1][2]*num[2][0] +
                 num[0][2]*num[1][0]*num[2][1] - num[0][2]*num[1][1]*num[2][0] -
                 num[0][0]*num[1][2]*num[2][1] - num[0][1]*num[1][0]*num[2][2];
                 // Doubt that a 0 determinant will result from rot. matrices
    for ( i = 0; i < 3; i++ ) {                  // Row
      m = (i + 1) % 3; n = (i + 2) % 3;          // Other rows
      for ( j = 0; j < 3; j++ ) {                // Column
        p = (j + 1) % 3; q = (j + 2) % 3;        // Other columns
        d[j][i] = (num[m][p]*num[n][q] - num[m][q]*num[n][p]) / det;
      }
    }
    return new Matrix3x3(d[0][0],d[0][1],d[0][2], d[1][0],d[1][1],d[1][2],
                         d[2][0],d[2][1],d[2][2]);
  }
}

