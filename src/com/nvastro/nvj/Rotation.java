/*
 * Rotation.java  -  Rotations for Prec, Nut, LST, Lat, Az, Alt, and Fld Rot.
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
 * This class represents cumulative rotations for Precession, Nutation, LST,
 * Latitude, Azimuth, Altitude, and Field Rotation.
 *
 * @author Brian Simpson
 */
public class Rotation extends Nutate implements Cloneable {
  /*+*********************************************************************
  * A right handed coordinate system is used, with the z axis going away *
  * from the observer, the y axis going to the right of the observer,    *
  * and the x axis going above the observer.  The following was written  *
  * prior to adding precession and nutation, but it illustrates how the  *
  * rotations are dealt with...                                          *
  *                                                                      *
  * Since it is assumed that the observer has just stepped on the        *
  * north pole as he was traveling along a longitude line associated     *
  * with 0 hours R.A., he will be facing 12 hours R.A.  Thus as he looks *
  * straight up at the north celestial pole (center of his view,         *
  * represented by the z azis), 0 hours R.A. goes to the top of his view *
  * (along the x axis), and 6 hours R.A. goes to the right of his view   *
  * (along the y axis).                                                  *
  *                                                                      *
  * The following conditions will produce a unitary view matrix (i.e.    *
  * an identity matrix for the view matrix):                             *
  * LST = 0 hours, Latitude = 90 degrees, Azimuth = 0 degrees (north),   *
  * Altitude = 90 degrees (looking straight up), FldRotate = 0 degrees.  *
  *                                                                      *
  * For non unitary conditions, the following corrections apply:         *
  *                                                                      *
  * Adjust for      Rotation axis      Rotation direction                *
  * LST                  z               x X y  as  LST increases        *
  * Latitude             y               z X x  as  Lat decreases        *
  * Azimuth              z               y X x  as  Azm increases        *
  * Altitude             y               x X z  as  Alt decreases        *
  * FldRotate            z               y X x  as  Fld increases        *
  *                                                                      *
  * The various rotations are done in the above order, but since they    *
  * implemented via matrices, the resulting view is described as:        *
  *                                                                      *
  * View = FldRotate * Altitude * Azimuth * Latitude * LST               *
  *                                                                      *
  ***********************************************************************/

  private Matrix3x3 prec, lst, lat, az, alt, fld, falt, vwop, ntpr, view,
                    unpn, ll, unll;
  // (Nutation matrix is in superclass)
  static final private Matrix3x3 z180 =  // Rotate 180 degrees about z axis
    new Matrix3x3(-1.0, 0.0, 0.0,   0.0, -1.0, 0.0,   0.0, 0.0, 1.0);
  static final private double S2R = Math.PI / 648000;  // Seconds to radians
  /** 2 * Pi */ public static final double TwoPI = Math.PI * 2;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This is the sole constructor.
   */
  public Rotation() {
    prec = new Matrix3x3();     // Identity matrix
    lst  = new Matrix3x3();     // Identity matrix
    lat  = new Matrix3x3();     // Identity matrix
    az   = new Matrix3x3();     // Identity matrix
    alt  = new Matrix3x3();     // Identity matrix
    fld  = new Matrix3x3();     // Identity matrix
    falt = new Matrix3x3();     // Identity matrix
    vwop = new Matrix3x3();     // Identity matrix
    ntpr = new Matrix3x3();     // Identity matrix
    view = new Matrix3x3();     // Identity matrix
    unpn = new Matrix3x3();     // Identity matrix
    ll   = new Matrix3x3();     // Identity matrix
    unll = new Matrix3x3();     // Identity matrix
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Produces a clone of this object.
   */
  public Object clone() {
    Rotation r = (Rotation) super.clone();
    // Do more than "shallow" copy
    r.prec = (Matrix3x3) prec.clone();
    r.lst  = (Matrix3x3)  lst.clone();
    r.lat  = (Matrix3x3)  lat.clone();
    r.az   = (Matrix3x3)   az.clone();
    r.alt  = (Matrix3x3)  alt.clone();
    r.fld  = (Matrix3x3)  fld.clone();
    r.falt = (Matrix3x3) falt.clone();
    r.vwop = (Matrix3x3) vwop.clone();
    r.ntpr = (Matrix3x3) ntpr.clone();
    r.view = (Matrix3x3) view.clone();
    r.unpn = (Matrix3x3) unpn.clone();
    r.ll   = (Matrix3x3)   ll.clone();
    r.unll = (Matrix3x3) unll.clone();
    return r;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the precession and nutation matrices.
   *
   * @param jday Julian day
   */
  public void setJDay(double jday) {
    super.setJDay(jday);  // Sets nutation
    double t = (jday - J2000_0) / 36525;
    // Using constants from "Astronomical Algorithms" P.134,
    // and matrix method from "Practical Astronomy with your Calculator"
    double zeta  = ((0.017998 * t + 0.30188) * t + 2306.2181) * t;
    double zzzz  = ((0.018203 * t + 1.09468) * t + 2306.2181) * t;
    double theta = ((0.041833 * t + 0.42665) * t + 2004.3109) * t;
    zeta  *= S2R;    // Seconds to radians
    zzzz  *= S2R;    // Seconds to radians
    theta *= S2R;    // Seconds to radians
    double cx = Math.cos(zeta);
    double sx = Math.sin(zeta);
    double cz = Math.cos(zzzz);
    double sz = Math.sin(zzzz);
    double ct = Math.cos(theta);
    double st = Math.sin(theta);

    prec.set(cx*ct*cz - sx*sz, -(sx*ct*cz + cx*sz), -st*cz,
             cx*ct*sz + sx*cz,   cx*cz - sx*ct*sz,  -st*sz,
             cx*st,              -sx*st,             ct);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the lst matrix.
   *
   * @param lstHrs Local sidereal time in hours, from 0 to 24; identity = 0
   */
  public void setLSTHrs(double lstHrs) {
    /* Set up rotation for LST (rotation about z axis with x axis moving  */
    /* in the direction of the y axis as LST increases).                  */
    lstHrs *= Math.PI / 12;
    double cos = Math.cos(lstHrs);
    double sin = Math.sin(lstHrs);
    lst.set(cos, sin, 0.0,   -sin, cos, 0.0,   0.0, 0.0, 1.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the lat matrix.
   *
   * @param latDeg Latitude in degrees, from 90 to -90; identity = 90
   */
  public void setLatDeg(double latDeg) {
    /* Set up rotation for latitude (rotation about y axis with z axis    */
    /* moving in the direction of the x axis as latitude decreases from   */
    /* 90 degrees;  z axis is initially aligned with the north pole).     */
    latDeg = (90.0 - latDeg) * Math.PI / 180.0;
    double cos = Math.cos(latDeg);
    double sin = Math.sin(latDeg);
    lat.set(cos, 0.0, -sin,   0.0, 1.0, 0.0,   sin, 0.0, cos);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the az matrix.
   *
   * @param azRad Azimuth in radians, from 0 to 2pi; identity = 0
   */
  public void setAzRad(double azRad) {
    /* Set up rotation for viewing azimuth (rotation about z axis with    */
    /* y axis moving in the direction of the x axis as Azimuth increases).*/
    double cos = Math.cos(azRad);
    double sin = Math.sin(azRad);
    az.set(cos, -sin, 0.0,   sin, cos, 0.0,   0.0, 0.0, 1.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the alt matrix.
   *
   * @param altRad Altitude in radians, from pi/2 to -pi/2; identity = pi/2
   */
  public void setAltRad(double altRad) {
    /* Set up rotation for viewing altitude (rotation about y axis with   */
    /* x axis moving in the direction of the z axis as Altitude decreases */
    /* from 90 degrees (pi/2)).                                           */
    double cos = Math.cos(altRad);
    double sin = Math.sin(altRad);
    alt.set(sin, 0.0, cos,   0.0, 1.0, 0.0,   -cos, 0.0, sin);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the fld matrix.
   *
   * @param fldDeg Field rotation in degrees, from 0 to 360; identity = 0
   */
  public void setFldDeg(int fldDeg) {
    /* Set up rotation for fld (rotation about z axis with y axis moving  */
    /* in the direction of the x axis as fld increases).                  */
    double f = fldDeg * Math.PI / 180;
    double cos = Math.cos(f);
    double sin = Math.sin(f);
    fld.set(cos, -sin, 0.0,   sin, cos, 0.0,   0.0, 0.0, 1.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the matrix describing precession and nutation.
   */
  public Matrix3x3 getPrecessNutate() { return ntpr; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the alt matrix (with fld rotation applied).
   */
  public Matrix3x3 getFldAlt() { return falt; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the view matrix.
   */
  public Matrix3x3 getView() { return view; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the view (without precession and nutation) matrix.
   */
  public Matrix3x3 getVWOPN() { return vwop; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up the view and other matrices.
   */
  public void reCalc(boolean modeRADec) {
    falt = alt.premult(fld);
    ll   = lst.premult(lat);
    unll = ll.invert();

    if ( modeRADec) {
      /* fld x alt x az x z180 (alt~dec, az~(-ra)) */
      vwop = z180.premult(az).premult(falt);
    } else {
      /* fld x alt x az x lat x lst */
      vwop = ll.premult(az).premult(falt);
    }

    ntpr = prec.premult(getNutMatrix());
    view = vwop.postmult(ntpr);
    unpn = ntpr.invert();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Precesses and Nutates the coordinates.
   *
   * @param ra Right ascension in radians; On return has new value
   * @param dec Declination in radians; On return has new value
   */
  public void precessNutate(double[] ra, double [] dec) {
    SphereCoords sc = new SphereCoords(ra[0], dec[0]);
    precessNutate(sc);
    ra[0] = sc.getRA();
    dec[0] = sc.getDec();
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Precesses and Nutates the coordinates.
   *
   * @param sc Spherical coordinates; On return has new value
   */
  public void precessNutate(SphereCoords sc) {
    sc.set(sc.rotateRADec(ntpr));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Unprecesses and Unnutates the coordinates.
   *
   * @param ra Right ascension in radians; On return has new value
   * @param dec Declination in radians; On return has new value
   */
  public void unPrecessNutate(double[] ra, double [] dec) {
    SphereCoords sc = new SphereCoords(ra[0], dec[0]);
    unPrecessNutate(sc);
    ra[0] = sc.getRA();
    dec[0] = sc.getDec();
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Unprecesses and Unnutates the coordinates.
   *
   * @param sc Spherical coordinates; On return has new value
   */
  public void unPrecessNutate(SphereCoords sc) {
    sc.set(sc.rotateRADec(unpn));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts apparent RA/Dec to Az/Alt.
   *
   * @param rd Coordinate object containing RA/Dec (both in radians)
   * @param aa Coordinate object containing Az/Alt (both in radians)
   */
  public void rd2aa(SphereCoords rd, SphereCoords aa) {
    double az, alt;

    Matrix3x1 m = rd.rotate(ll);
    if      ( m.num[2] >  1 ) alt =  HalfPI;
    else if ( m.num[2] < -1 ) alt = -HalfPI;
    else                      alt = Math.asin(m.num[2]);
    if ( m.num[0] == 0 && m.num[1] == 0 ) az = 0.0;
    else az = Math.atan2(m.num[1], -m.num[0]); /* az between -pi and pi */
    if ( az < 0 ) az += TwoPI;                 /* az between 0 and 2 pi */
    aa.set(az, alt);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts Az/Alt to apparent RA/Dec.
   *
   * @param aa Coordinate object containing Az/Alt (both in radians)
   * @param rd Coordinate object containing RA/Dec (both in radians)
   */
  public void aa2rd(SphereCoords aa, SphereCoords rd) {
    double ra, dec;

    SphereCoords aa2 = new SphereCoords(Math.PI - aa.getAz(), aa.getAlt());

    Matrix3x1 m = aa2.rotate(unll);
    if      ( m.num[2] >  1 ) dec =  HalfPI;
    else if ( m.num[2] < -1 ) dec = -HalfPI;
    else                      dec = Math.asin(m.num[2]);
    if ( m.num[0] == 0 && m.num[1] == 0 ) ra = 0.0;
    else ra = Math.atan2(m.num[1], m.num[0]);  /* ra between -pi and pi */
    if ( ra < 0 ) ra += TwoPI;                 /* ra between 0 and 2 pi */
    rd.set(ra, dec);
  }
}

