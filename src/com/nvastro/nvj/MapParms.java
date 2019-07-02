/*
 * MapParms.java  -  All mapping parameters needed for drawing
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.geom.Area;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * MapParms encloses everything that a class such as StarDB or CGrid
 * needs to be able to draw itself (paint or print).  It contains a
 * Preferences object and an LST object (separate from the one in the
 * Preferences object).
 * <p>
 * The MapParms used for painting contains the same Preferences object
 * that the main thread uses for the user interface dialogs.  Changes in
 * this object by the main user interface thread should not have any
 * adverse consequences for the painting thread (which writes to an
 * off-screen buffer).  This is because Preferences is mostly a bunch
 * of on/off boolean switches, except for color information, but since
 * a change in the Preferences causes an immediate repaint, this should
 * be no problem.
 * <p>
 * An exception to the above is done to handle changes in time.
 * A separate LST is contained in MapParms (i.e. separate from the LST
 * in Preferences).  When a new paint is started, the "update" function
 * should be called to reset the separate LST to the value of the
 * Preferences' LST, and then the separate LST's time is frozen so that
 * the time is stable throughout the painting.  This allows the
 * Preferences' LST to continue running (in accordance to the user's
 * preferences).
 * <p>
 * For printing a new MapParms object should be cloned from the paint
 * MapParms object.  This will prevent changes to the paint Preferences
 * (same as user interface Preferences) from affecting the print
 * Preferences while printing is occurring.  (Although currently the
 * user interface is effectively frozen while printing occurs...)
 *
 * @author Brian Simpson
 */
public class MapParms extends Rotation implements Cloneable {
  final static public double pelsPerInch =     // Screen res for close testing
                             Toolkit.getDefaultToolkit().getScreenResolution();
  /** User preferences */
  public Preferences prefer;
  /** This lst is used rather than the lst in prefer; it is a "frozen" copy
      of prefer's lst */
  public LST lst;
  /** Pels per radians factor */
  public double pelsPerRadian;        // Horiz. res. = vert. res.
  public Graphics2D g;
  public boolean printing;            // Are we printing or painting
  public Shape clip1 = null,          // Full clip outline
               clip2 = null;          // If non-null, used to intersect clip1
  public Area  milk  = null;          // Milky Way during printing
  private boolean drawing;            // If false, cancel print/paint
  private int width, height;
  private double midx, midy;
  private double maxoffx, maxoffy;
  // The following for speed purposes... (may present multithreading problems)
  private double m_0, m_1, m_2, s_0, s_1, s_2;

  // TwoPI = Math.PI * 2  -  defined in superclass
  /** Deg to rad factor */ public static final double Deg2Rad = Math.PI / 180;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param prefer User preferences
   */
  public MapParms(Preferences prefer) {
    this.prefer = prefer;
    this.lst = (LST)(prefer.lst.clone());
    drawing = true;
    printing = false;
    setSize(new Dimension(100, 100)); // No significance to these numbers
    pelsPerRadian = 250; // Arbitrary, set before using...
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the size of the drawing area.
   *
   * @param d Drawing area dimensions
   */
  public void setSize(Dimension d) {
    // If width = 5, then available pels are 0 1 2 3 4 (I think)
    width = d.width;
    height = d.height;
    /* Determine window midpoint (Done here for speed) */
    midx = (width - 1) / 2.0;
    midy = (height - 1)/ 2.0;
    /* E.g. if width = 4, x = 0 to 3; midx = 1.5, not 2.0 */
    maxoffx = midx + 0.5; // Edge pixels
    maxoffy = midy + 0.5; // are full size
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a copy of MapParms (to be used for ID functions and printing).
   */
  public Object clone() {
    MapParms mp = (MapParms) super.clone();
    // Do more than "shallow" copy
    mp.prefer = (Preferences) prefer.clone();
    mp.lst = (LST) lst.clone();
    mp.clip1 = null;
    mp.clip2 = null;
    mp.milk  = null;
    mp.g     = null;
    mp.lst.stop();  // (Should already be stopped, but be safe...)
    mp.setLSTHrs(mp.lst.getLSTHrs());
    mp.reCalc(prefer.modeRADec);
    return mp;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Updates drawing parameters, copies and pauses time,
   * and sets rotation matrix.  To be done after Preferences
   * has been changed and before drawing.
   *
   * @param size Drawing area dimensions
   * @param dppr Default pels per radian (0.95 * screen width)
   */
  public void update(Dimension size, double dppr) {
    lst.updateStop(prefer.lst);      // Update time and stop
    setSize(size);
    pelsPerRadian = dppr * prefer.getZoom();
    setJDay(lst.getJulianEphDay());
    setLSTHrs(lst.getLSTHrs());
    setAzRad(prefer.getAz());
    setAltRad(prefer.getAlt());
    setLatDeg(prefer.getLatDeg());
    setFldDeg(prefer.getFld());
    reCalc(prefer.modeRADec);
    milk = null;
    drawing = true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Return width of drawing area (in pels).
   */
  public int getWidth() { return width; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Return height of drawing area (in pels).
   */
  public int getHeight() { return height; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Horizontal middle pel of area.
   * If width is 5, pels are 0, 1, 2, 3, 4; -&gt; middle pel is 2.
   * If width is 4, pels are 0, 1, 2, 3; -&gt; middle pel is 1.5.
   */
  public double getMidX() { return midx; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Vertical middle pel of area.
   * If height is 5, pels are 0, 1, 2, 3, 4; -&gt; middle pel is 2.
   * If height is 4, pels are 0, 1, 2, 3; -&gt; middle pel is 1.5.
   */
  public double getMidY() { return midy; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns flag to indicate if drawing is progressing.
   */
  public boolean isDrawing() { return drawing; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets flag to signal cancellation of drawing.
   */
  public void cancelDrawing() { drawing = false; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) if within 90 degrees of center.
   * Coordinates are precessed and nutated.
   *
   * @param scoord Coordinate object containing RA/Dec (both in radians)
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  1 - in window
   * <br>     0 - out of window, but within 90 deg. of center
   * <br>    -1 - more that 90 degrees from center of window
   *              (coordinates not calculated)
   */
  public int rd2xyhit(SphereCoords scoord, float[] x, float[] y) {
    return rd2xyhit(scoord.getRA(), scoord.getDec(), x, y, false);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) if within 90 degrees of center.
   *
   * @param scoord Coordinate object containing RA/Dec (both in radians)
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @param nopn no precession/nutation if true
   * @return  1 - in window
   * <br>     0 - out of window, but within 90 deg. of center
   * <br>    -1 - more that 90 degrees from center of window
   *              (coordinates not calculated)
   */
  public int rd2xyhit(SphereCoords scoord, float[] x, float[] y, boolean nopn) {
    return rd2xyhit(scoord.getRA(), scoord.getDec(), x, y, nopn);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) if within 90 degrees of center.
   * Coordinates are precessed and nutated.
   *
   * @param ras RA value in radians
   * @param dec Dec value in radians
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  1 - in window
   * <br>     0 - out of window, but within 90 deg. of center
   * <br>    -1 - more that 90 degrees from center of window
   *              (coordinates not calculated)
   */
  public int rd2xyhit(double ras, double dec, float[] x, float[] y) {
    return rd2xyhit(ras, dec, x, y, false);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) if within 90 degrees of center.
   *
   * @param ras RA value in radians
   * @param dec Dec value in radians
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @param nopn no precession/nutation if true
   * @return  1 - in window
   * <br>     0 - out of window, but within 90 deg. of center
   * <br>    -1 - more that 90 degrees from center of window
   *              (coordinates not calculated)
   */
  public int rd2xyhit(double ras, double dec, float[] x, float[] y,
                      boolean nopn) {
    double offx, offy;
    int rc;

    /* Would like to perform the following statement:
         Matrix3x1 m = <ras & dec as SphereCoords>.rotate(getView());
       However, in the interest of pure raw speed, I would prefer to not
       create any temporary objects for the garbage collector to clean up.
       (The above statement creates 2 temporary Matrix3x1's, which could
       be multiplied by > 100,000 stars.)  Thus the following...: */
    double sinra = Math.sin(ras);
    double cosra = Math.cos(ras);
    double sinde = Math.sin(dec);
    double cosde = Math.cos(dec);
    s_0 = cosra * cosde;
    s_1 = sinra * cosde;
    s_2 = sinde;
    Matrix3x3 r = nopn ? getVWOPN() : getView();
    m_0 = r.num[0][0]*s_0 + r.num[0][1]*s_1 + r.num[0][2]*s_2;
    m_1 = r.num[1][0]*s_0 + r.num[1][1]*s_1 + r.num[1][2]*s_2;
    m_2 = r.num[2][0]*s_0 + r.num[2][1]*s_1 + r.num[2][2]*s_2;

    /* Convert back to spherical coordinates (modified so that 0 degrees
       declination = z axis, 180 degrees declination = -z axis;
       thus 90 degrees declination, not 0, represents the "equator").
       If Latitude = 90 degrees, LST = 0, viewing straight up and facing
       0 azimuth (north, which at north pole is facing south along 12 hr),
       (as if you just walked up to north pole while facing 0 azimuth),
       then 0 degrees right ascension is straight up on screen and
       increasing right ascension goes clockwise on screen.
       (I.e. x axis goes up on screen, y axis goes to the right.
       Therefore 0 hours R.A. goes up,  6 hours R.A. goes right.) */
    if ( m_2 < -0.0002 ) return(-1); // > 90.01 degree beyond center
    else if ( m_2 > 1 ) dec = 0.0;
    else                dec = Math.acos(m_2);
    //if ( true ) {   /* (i.e. pick this method for now...) */
      if ( m_0 == 0 && m_1 == 0 ) ras = 0.0;
      else ras = Math.atan2(m_1, m_0);  // ras between -pi and pi
      offx = dec * Math.sin(ras) * pelsPerRadian;
      offy = dec * Math.cos(ras) * pelsPerRadian;
    //} else {      /* Not sure why following is not much faster */
    //  m_2 = Math.sqrt((m_0*m_0) + (m_1*m_1));
    //  if ( m_2 != 0 )
    //  {
    //    offx = dec * m_1 * pelsPerRadian / m_2;
    //    offy = dec * m_0 * pelsPerRadian / m_2;
    //  }
    //  else offx = offy = 0;
    //}

    rc = 1;
    if ( maxoffx <= Math.abs(offx) ) rc = 0;
    if ( maxoffy <= Math.abs(offy) ) rc = 0;
    x[0] = (float)(midx + offx);
    y[0] = (float)(midy - offy);
    return(rc);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts rectangular coordinates to x,y (window coordinates) if within
   * 90 degrees of center.  This function speeds up conversion of rectangular
   * stellar coordinates.  (Includes precession/nutation.)
   *
   * @param rx x part of rectangular input coordinates
   * @param ry y part of rectangular input coordinates
   * @param rz z part of rectangular input coordinates
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  1 - in window
   * <br>     0 - out of window, but within 90 deg. of center
   * <br>    -1 - more that 90 degrees from center of window
   *              (coordinates not calculated)
   */
  public int rd2xyhit(double rx, double ry, double rz, float[] x, float[] y) {
    double offx, offy, pels;
    int rc;

    Matrix3x3 r = getView();
    m_0 = r.num[0][0]*rx + r.num[0][1]*ry + r.num[0][2]*rz;
    m_1 = r.num[1][0]*rx + r.num[1][1]*ry + r.num[1][2]*rz;
    m_2 = r.num[2][0]*rx + r.num[2][1]*ry + r.num[2][2]*rz;

    /* See comments in previous function */
    if ( m_2 < -0.0002 ) return(-1); // > 90.01 degree beyond center
    else if ( m_2 > 1 ) pels = 0.0;
    else                pels = Math.acos(m_2) * pelsPerRadian;
    m_2 = Math.sqrt((m_0*m_0) + (m_1*m_1));
    if ( m_2 != 0 )
    {
      pels /= m_2;
      offx = pels * m_1;
      offy = pels * m_0;
    }
    else offx = offy = 0;

    rc = 1;
    if ( maxoffx <= Math.abs(offx) ) rc = 0;
    if ( maxoffy <= Math.abs(offy) ) rc = 0;
    x[0] = (float)(midx + offx);
    y[0] = (float)(midy - offy);
    return(rc);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) and
   * returns distance from center of window.
   * Coordinates are precessed and nutated.
   * (x,y always calculated.  Assumes clipping is set up.)
   *
   * @param scoord Coordinate object containing RA/Dec (both in radians)
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  Distance from center in radians
   */
  public float rd2xydist(SphereCoords scoord, float[] x, float[] y) {
    return rd2xydist(scoord.getRA(), scoord.getDec(), x, y, false);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) and
   * returns distance from center of window.
   * (x,y always calculated.  Assumes clipping is set up.)
   *
   * @param scoord Coordinate object containing RA/Dec (both in radians)
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @param nopn no precession/nutation if true
   * @return  Distance from center in radians
   */
  public float rd2xydist(SphereCoords scoord, float[] x, float[] y,
                         boolean nopn) {
    return rd2xydist(scoord.getRA(), scoord.getDec(), x, y, nopn);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) and
   * returns distance from center of window.
   * Coordinates are precessed and nutated.
   * (x,y always calculated.  Assumes clipping is set up.)
   *
   * @param ras RA value in radians
   * @param dec Dec value in radians
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  Distance from center in radians
   */
  public float rd2xydist(double ras, double dec, float[] x, float[] y) {
    return rd2xydist(ras, dec, x, y, false);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts RA/Dec to x,y (window coordinates) and
   * returns distance from center of window.
   * (x,y always calculated.  Assumes clipping is set up.)
   *
   * @param ras RA value in radians
   * @param dec Dec value in radians
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @param nopn no precession/nutation if true
   * @return  Distance from center in radians
   */
  public float rd2xydist(double ras, double dec, float[] x, float[] y,
                         boolean nopn) {
    /* (See comments in rd2xyhit function for hints on methodology) */
    double sinra = Math.sin(ras);
    double cosra = Math.cos(ras);
    double sinde = Math.sin(dec);
    double cosde = Math.cos(dec);
    s_0 = cosra * cosde;
    s_1 = sinra * cosde;
    s_2 = sinde;
    Matrix3x3 r = nopn ? getVWOPN() : getView();
    m_0 = r.num[0][0]*s_0 + r.num[0][1]*s_1 + r.num[0][2]*s_2;
    m_1 = r.num[1][0]*s_0 + r.num[1][1]*s_1 + r.num[1][2]*s_2;
    m_2 = r.num[2][0]*s_0 + r.num[2][1]*s_1 + r.num[2][2]*s_2;

    if      ( m_2 >  1 ) dec = 0.0;      // Do some clipping
    else if ( m_2 < -1 ) dec = Math.PI;  //   just in case
    else                 dec = Math.acos(m_2);
    if ( m_0 == 0 && m_1 == 0 ) ras = 0.0;
    else ras = Math.atan2(m_1, m_0);  // ras between -pi and pi
    x[0] = (float)(midx + dec * Math.sin(ras) * pelsPerRadian);
    y[0] = (float)(midy - dec * Math.cos(ras) * pelsPerRadian);
    return((float)dec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts Az/Alt to x,y (window coordinates) and
   * returns distance from center of window.
   * (x,y always calculated.  Assumes clipping is set up.)
   *
   * @param az  Az value in radians
   * @param alt Alt value in radians
   * @param x 1 element array to return x value
   * @param y 1 element array to return y value
   * @return  Distance from center in radians
   */
  public float aa2xydist(double az, double alt, float[] x, float[] y) {
    if ( prefer.modeRADec ) {
      SphereCoords aa = new SphereCoords(az, alt);
      SphereCoords rd = new SphereCoords();
      aa2rd(aa, rd);
      return rd2xydist(rd, x, y);
    }

    az -= prefer.getAz();         // Rotate azimuth

    /* Rotate altitude */
    double sinaz = Math.sin(az);
    double cosaz = Math.cos(az);
    double sinal = Math.sin(alt);
    double cosal = Math.cos(alt);
    /* Set up rectangular coordinates for azimuth & altitude.
       Note that as we increase in azimuth, we are rotating in opposite
       direction than if we were increasing in right ascension.  If we
       are facing north (0 Az), we want 0 Az line to hit win bottom. */
    s_0 = -cosaz * cosal;         // x axis = North
    s_1 = sinaz * cosal;          // y axis = West
    s_2 = sinal;                  // z axis = Zenith
    Matrix3x3 r = getFldAlt();
    m_0 = r.num[0][0]*s_0 + r.num[0][1]*s_1 + r.num[0][2]*s_2;
    m_1 = r.num[1][0]*s_0 + r.num[1][1]*s_1 + r.num[1][2]*s_2;
    m_2 = r.num[2][0]*s_0 + r.num[2][1]*s_1 + r.num[2][2]*s_2;

    if      ( m_2 >  1 ) alt = 0.0;      // Do some clipping
    else if ( m_2 < -1 ) alt = Math.PI;  //   just in case
    else                 alt = Math.acos(m_2);
    if ( m_0 == 0 && m_1 == 0 ) az = 0.0;
    else az = Math.atan2(m_1, m_0);   // az between -pi and pi
    x[0] = (float)(midx + alt * Math.sin(az) * pelsPerRadian);
    y[0] = (float)(midy - alt * Math.cos(az) * pelsPerRadian);
    return((float)alt);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts x,y (window coordinates) to Ra/Dec (J2000).
   *
   * @param x x value
   * @param y y value
   * @param sc Coordinate object to return Ra/Dec (in radians)
   * @return   True  - Within 90 degrees of center of window
   * <br>      False - Beyond 90 degrees
   */
  public boolean xy2rd(int x, int y, SphereCoords sc) {
    double offx, offy, ra, dec;
    double[] w = new double[3];
    Matrix3x1 v;
    double dist, sindec;

    offx = x - midx;
    offy = midy - y;
    offx /= pelsPerRadian;       /* Radians from */
    offy /= pelsPerRadian;       /*    center    */
    dist = Math.sqrt(offx*offx + offy*offy);
    /* Doesn't seem to be a problem if dist > pi */
    w[2] = Math.cos(dist);
    sindec = Math.sin(dist);
    if ( dist > 0 ) {
      w[1] = offx * sindec / dist;
      w[0] = offy * sindec / dist;
    }
    else
      w[0] = w[1] = 0;

    /* Derive v by multiplying w by the inverse of the view matrix */
    v = getView().invert().mult(new Matrix3x1(w[0], w[1], w[2]));
    if      ( v.num[2] >  1 ) dec =  HalfPI;
    else if ( v.num[2] < -1 ) dec = -HalfPI;
    else                      dec = Math.asin(v.num[2]);
    if ( v.num[0] == 0 && v.num[1] == 0 ) ra = 0.0;
    else ra = Math.atan2(v.num[1], v.num[0]);  /* ra between -pi and pi */

    sc.set(ra, dec);
    if ( dist <= HalfPI ) return true;
    else                  return false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts x,y (window coordinates) to Az/Alt.
   *
   * @param x x value
   * @param y y value
   * @param aa Coordinate object to return Az/Alt (in radians)
   */
  public void xy2aa(int x, int y, SphereCoords aa) {
    double[] az = new double[1];
    double[] alt = new double[1];
    xy2aa(x, y, az, alt);
    aa.set(az[0], alt[0]);
  }
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts x,y (window coordinates) to Az/Alt.
   *
   * @param x x value
   * @param y y value
   * @param az  1 element array to return Az value (in radians)
   * @param alt 1 element array to return Alt value (in radians)
   */
  public void xy2aa(int x, int y, double[] az, double[] alt) {
    double offx, offy;
    double[] w = new double[3];
    Matrix3x1 v;
    double dist, sindec;

    offx = x - midx;
    offy = midy - y;
    offx /= pelsPerRadian;       /* Radians from */
    offy /= pelsPerRadian;       /*    center    */
    dist = Math.sqrt(offx*offx + offy*offy);
    /* Doesn't seem to be a problem if dist > pi */
    w[2] = Math.cos(dist);
    sindec = Math.sin(dist);
    if ( dist > 0 ) {
      w[1] = offx * sindec / dist;
      w[0] = offy * sindec / dist;
    }
    else
      w[0] = w[1] = 0;

    /* Derive v by multiplying w by the inverse of the falt matrix */
    v = getFldAlt().invert().mult(new Matrix3x1(w[0], w[1], w[2]));
    if      ( v.num[2] >  1 ) alt[0] =  HalfPI;
    else if ( v.num[2] < -1 ) alt[0] = -HalfPI;
    else                      alt[0] = Math.asin(v.num[2]);
    if ( v.num[0] == 0 && v.num[1] == 0 ) az[0] = 0.0;
    else az[0] = Math.atan2(v.num[1], -v.num[0]); /* az between -pi and pi */
    az[0] += prefer.getAz();                      /* Current az rot. */
    while ( az[0]  <    0  ) az[0] += TwoPI;
    while ( az[0] >= TwoPI ) az[0] -= TwoPI;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns separation angle between 2 sets of RA/Dec coordinates.
   *
   * @param ra1 RA of 1st coordinate in radians
   * @param dec1 Dec of 1st coordinate in radians
   * @param ra2 RA of 2nd coordinate in radians
   * @param dec2 Dec of 2nd coordinate in radians
   * @return Separation angle in radians
   */
  public static double separation(double ra1, double dec1,
                                  double ra2, double dec2) {
    double angle;

    angle = Math.sin(dec1) * Math.sin(dec2) +
            Math.cos(dec1) * Math.cos(dec2) * Math.cos(ra1 - ra2);
    if ( angle >  1 ) angle =  1;
    if ( angle < -1 ) angle = -1;
    return Math.acos(angle);             // Separation in radians, >= 0
  }
}

