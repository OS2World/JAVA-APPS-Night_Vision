/*
 * SphereCoords.java  -  Spherical coordinates
 * Copyright (C) 2011-2015 Brian Simpson
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
 * Spherical coordinates.  Can be used for Right Ascension / Declination,
 * Lambda / Beta (ecliptical), and Azimuth / Altitude.
 * <p> (RA, Dec) = (0hrs, 0deg) -&gt; [ 1, 0, 0 ]
 * <p> (RA, Dec) = (6hrs, 0deg) -&gt; [ 0, 1, 0 ]
 * <p> -&gt; As RA increases, we have rotation about the z axis in the direction
 * x-&gt;y, i.e. a right handed rotation system suited for 3x3 matrix
 * multiplication.
 * <p> However Azimuth progresses in the opposite direction (i.e. left handed),
 * therefore this class, when used for Az/Alt, is principally used for
 * encapsulating the coordinates and provides convenience text functions.
 * Know what you are doing if you do matrix multiplication.
 *
 * @author Brian Simpson
 */
public class SphereCoords {
  /** 2 * Pi */ public static final double TwoPI = Math.PI * 2;
  protected double ra, dec;       // Radians

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Default Constructor.  Both ra and dec set at 0 radians.
   */
  public SphereCoords() {
    ra = dec = 0.0;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   * (ra can also be lambda or az, and dec can also be beta or alt.)
   *
   * @param ra Right Ascension in radians
   * @param dec Declination in radians, between between +/- pi/2 (inclusive)
   */
  public SphereCoords(double ra, double dec) {
    // For speed reasons don't check arguments.  Hopefully this won't bite me.
    this.ra = ra; this.dec = dec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Copy Constructor.
   *
   * @param sc Spherical coordinates to copy from
   */
  public SphereCoords(SphereCoords sc) {
    this.ra = sc.ra; this.dec = sc.dec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets ra and dec (or lambda and beta, or az and alt).
   *
   * @param ra Right Ascension in radians
   * @param dec Declination in radians, between between +/- pi/2 (inclusive)
   */
  public void set(double ra, double dec) {
    // For speed reasons don't check arguments.  Hopefully this won't bite me.
    this.ra = ra; this.dec = dec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets ra and decl (or lambda and beta, or az and alt) based on the given
   * spherical coordinates.
   *
   * @param sc Spherical coordinates
   */
  public void set(SphereCoords sc) {
    this.ra = sc.ra; this.dec = sc.dec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Right Ascension in radians.
   */
  public double getRA() { return ra; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Ecliptical Longitude in radians.
   */
  public double getLambda() { return ra; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Azimuth in radians.
   */
  public double getAz() { return ra; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Declination in radians.
   */
  public double getDec() { return dec; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Ecliptical Latitude in radians.
   */
  public double getBeta() { return dec; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Altitude in radians.
   */
  public double getAlt() { return dec; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a 3x1 matrix resulting from multiplication by the
   * specified 3x3 rotation matrix.
   *
   * @param m 3x3 matrix
   */
  public Matrix3x1 rotate(Matrix3x3 m) {
    double sinra = Math.sin(ra);
    double cosra = Math.cos(ra);
    double sinde = Math.sin(dec);
    double cosde = Math.cos(dec);
    return m.mult(new Matrix3x1(cosra * cosde, sinra * cosde, sinde));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns new SphereCoords resulting from multiplication by the
   * specified 3x3 rotation matrix.
   *
   * @param m 3x3 matrix
   */
  public SphereCoords rotateRADec(Matrix3x3 m) {
    double ra, dec;

    Matrix3x1 mat = rotate(m);
    mat.num[2] = Math.max(-1.0, Math.min(mat.num[2], 1.0));
    dec = Math.asin(mat.num[2]);
    if ( mat.num[0] == 0 && mat.num[1] == 0 ) ra = 0.0;
    else ra = Math.atan2(mat.num[1], mat.num[0]); /* ra between -pi and pi */
    if ( ra < 0 ) ra += TwoPI;                    /* ra between 0 and 2 pi */
    return new SphereCoords(ra, dec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns right ascension in string form as "#h #.#m".
   * Used in Object identification window.
   */
  public String tellRAHrMnT() {
    double d = ra * 12 / Math.PI;
    while ( d < 0 ) d += 24;
    while ( d >= 24 ) d -= 24;

    int hr = (int)Math.floor(d);
    d = (d - hr) * 60;
    int mn = (int)Math.floor(d);
    d = (d - mn) * 10;
    int tn = (int)Math.round(d);
    if ( tn == 10 ) {
      tn = 0; mn++;
      if ( mn == 60 ) {
        mn = 0; hr++;
        if ( hr == 24 ) hr = 0;
      }
    }
    return new String(hr + IdentifyDlg.RAHour + " " +
                      mn + TextBndl.getDPChar() + tn + IdentifyDlg.RAMin);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns right ascension in string form as "#h #m #.#s".
   * Used in Object identification window.
   */
  public String tellRAHrMnScT() {
    double d = ra * 12 / Math.PI;
    while ( d < 0 ) d += 24;
    while ( d >= 24 ) d -= 24;

    int hr = (int)Math.floor(d);
    d = (d - hr) * 60;
    int mn = (int)Math.floor(d);
    d = (d - mn) * 60;
    int sc = (int)Math.floor(d);
    d = (d - sc) * 10;
    int tn = (int)Math.round(d);
    if ( tn == 10 ) {
      tn = 0; sc++;
      if ( sc == 60 ) {
        sc = 0; mn++;
        if ( mn == 60 ) {
          mn = 0; hr++;
          if ( hr == 24 ) hr = 0;
        }
      }
    }
    return new String(hr + IdentifyDlg.RAHour + " " + mn + IdentifyDlg.RAMin +
                      " " + sc + TextBndl.getDPChar() + tn + IdentifyDlg.RASec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns azimuth in string form as "[-]#d #m".
   * Used in Object identification window.
   */
  public String tellAzDgMn() {
    return tellDgMn(ra, true);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns declination in string form as "[-]#d #m".
   * Used in Object identification window.
   */
  public String tellDecDgMn() {
    return tellDgMn(dec, false);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns altitude in string form as "[-]#d #m".
   * Used in Object identification window.
   */
  public String tellAltDgMn() {
    return tellDgMn(dec, false);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns angle in string form as "[-]#d #m".
   *
   * @param rad Angle in radians
   * @param offset If false, -180 &lt;= result &lt;= 180;
   *               if true, 0 &lt;= result &lt;= 360
   * @return Angle as string
   */
  static public String tellDgMn(double rad, boolean offset) {
    String sign;

    double deg = rad * 180 / Math.PI;
    if ( deg > 400 || deg < -400 ) return "";  // Something screwy about arg.
    while ( deg  >  180 ) deg -= 360;             // deg <= 180
    while ( deg <= -180 ) deg += 360;             // deg > -180
    if ( offset == true && deg < 0 ) deg += 360;  // 0 <= deg < 360
    // With rounding, -180 <= deg <= 180, or 0 <= deg <= 360

    if ( deg < 0 ) { deg *= -1; sign = String.valueOf(TextBndl.getMinusChar());}
    else                      { sign = "";  }

    int dg = (int)Math.floor(deg);
    deg = (deg - dg) * 60;
    int mn = (int)Math.round(deg);
    if ( mn == 60 ) {
      mn = 0; dg++;
    }
    if ( dg == 0 && mn == 0 ) sign = "";  // Prevent "-0d 0m"
    return new String(sign + dg + IdentifyDlg.Deg + " " +
                             mn + IdentifyDlg.Min);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns declination in string form as "[-]#d #m #s".
   * Used in Object identification window.
   */
  public String tellDecDgMnSc() {
    return tellDgMnSc(dec, false);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns angle in string form as "[-]#d #m #s".
   *
   * @param rad Angle in radians
   * @param offset If false, -180 &lt;= result &lt;= 180;
   *               if true, 0 &lt;= result &lt;= 360
   * @return Angle as string
   */
  static public String tellDgMnSc(double rad, boolean offset) {
    String sign;

    double deg = rad * 180 / Math.PI;
    if ( deg > 400 || deg < -400 ) return "";  // Something screwy about arg.
    while ( deg  >  180 ) deg -= 360;             // deg <= 180
    while ( deg <= -180 ) deg += 360;             // deg > -180
    if ( offset == true && deg < 0 ) deg += 360;  // 0 <= deg < 360
    // With rounding, -180 <= deg <= 180, or 0 <= deg <= 360

    if ( deg < 0 ) { deg *= -1; sign = String.valueOf(TextBndl.getMinusChar());}
    else                      { sign = "";  }

    int dg = (int)Math.floor(deg);
    deg = (deg - dg) * 60;
    int mn = (int)Math.floor(deg);
    deg = (deg - mn) * 60;
    int sc = (int)Math.round(deg);
    if ( sc == 60 ) {
      sc = 0; mn++;
      if ( mn == 60 ) {
        mn = 0; dg++;
      }
    }
    if ( dg == 0 && mn == 0 && sc == 0 ) sign = "";  // Prevent "-0d 0m 0s"
    return new String(sign + dg + IdentifyDlg.Deg + " " + mn + IdentifyDlg.Min +
                       " " + sc + IdentifyDlg.Sec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the separation between two points, in the range of 0.0 through pi.
   *
   * @param sp Coordinates of other point
   * @return Angular distance in radians
   */
  public double getDistanceFrom(SphereCoords sp) {
    double angle = Math.sin(dec) * Math.sin(sp.getDec()) +
                   Math.cos(dec) * Math.cos(sp.getDec()) *
                   Math.cos(ra - sp.getRA());

    if ( angle >  1 ) angle =  1;
    if ( angle < -1 ) angle = -1;
    return Math.acos(angle);        // Separation in radians, >= 0
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the angular distance from the specified coordinate
   * in string form as "degrees:minutes".
   *
   * @param sp Coordinates of other point
   * @return Angular distance
   */
  public String tellDistanceFrom(SphereCoords sp) {
    return tellDgMnSc(getDistanceFrom(sp), true);
  }
}

