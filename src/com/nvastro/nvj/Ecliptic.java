/*
 * Ecliptic.java  -  Draws the ecliptic
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

import java.awt.geom.Line2D;


// Note:  The method used here appears accurate for 1000AD - 3000AD, but
// beyond that range it starts differing from where the precessed celestial
// grid is drawn at the vernal and autumnal equinoxes (i.e. the ecliptic
// should cross precisely at RA = 0, Dec = 0, and RA = 12, Dec = 0).
// Not sure which is in error, but it is probably the ecliptic, as the
// sun's travels match the grid and not the ecliptic.  Needs further study...


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * The Ecliptic class contains a single static function to draw the ecliptic.
 *
 * @author Brian Simpson
 */
public class Ecliptic {
  static double sine2k = 0.397777156;  // e2k = mean obliquity of the ecliptic
  static double cose2k = 0.917482062;  //    at epoch J2000.0 (23.4392911 deg)

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private Ecliptic() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the ecliptic.
   *
   * @param mp Mapping parameters
   */
  public static void draw(MapParms mp) {
    double[] ras = new double[1];
    double[] dec = new double[1];
    float[] x = new float[2];
    float[] y = new float[2];
    boolean newpt, oldpt = false;
    Line2D.Float line = new Line2D.Float();

    if ( !mp.prefer.drawEcliptic() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrEcliptic() :
                                mp.prefer.colorEcliptic());

    /* Set up more restrictive clipping */
    if ( mp.clip2 != null )
      mp.g.clip(mp.clip2);    // Intersects existing clip (clip1)

    for ( int i = 0; i <= 360 && mp.isDrawing(); i++ ) {
      convEclipToEquat(i * Math.PI / 180, ras, dec); // J2000 ecliptic
      newpt = mp.rd2xydist(ras[0], dec[0], x, y) < 1.6f; // 91.7 degrees
      if ( (newpt || oldpt) && (i > 0) ) {
        line.setLine(x[1], y[1], x[0], y[0]);
        mp.g.draw(line);
      }

      x[1] = x[0]; y[1] = y[0];
      oldpt = newpt;
    }

    /* Cancel restrictive clipping */
    if ( mp.clip2 != null ) mp.g.setClip(mp.clip1); // Reset if changed
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Coordinate conversion.
   */
  private static void convEclipToEquat(double lambda,
                                       double[] ra, double[] dec) {
    double sinlambda  = Math.sin(lambda);
    // -pi <= Math.atan2 <= pi, no problem if 2nd arg is 0
    ra[0] = Math.atan2(sinlambda * cose2k, Math.cos(lambda));
    dec[0] = sine2k * sinlambda;
    if ( dec[0] >  1 ) dec[0] =  1;
    if ( dec[0] < -1 ) dec[0] = -1;
    dec[0] = Math.asin(dec[0]);
  }
}

