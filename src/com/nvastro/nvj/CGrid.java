/*
 * CGrid.java  -  The CGrid class - draws the RA/Dec (celestial) grid
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

import java.awt.*;
import java.awt.geom.*;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * The CGrid class - draws the RA/Dec (celestial) grid.
 *
 * @author Brian Simpson
 */
public class CGrid {
  static final private double D2R = Math.PI / 180;  // Degrees to radians
  static final private double H2R = Math.PI / 12;   // RA hours to radians
  static final private double HALFPI = Math.PI / 2; // Pi / 2

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private CGrid() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the RA/Dec grid
   *
   * @param mp Mapping parameters
   */
  public static void draw(MapParms mp) {
    int i, j, k, k2, l, segsPerHour; // segsPerHour = line segments / hr
    double ra, decl, rahr, inc;
    float[] x = new float[2];
    float[] y = new float[2];
    float[] xm = new float[24]; // x at midpt        (24 is
    float[] ym = new float[24]; // y at midpt        the maximum
    float[] am = new float[24]; // angle at midpt    number of midpts
    float[] sm = new float[24]; // score at midpt    along a full circle)
    int nm = 0;                 // number of midpts
    boolean labels;
    FontMetrics fm = mp.g.getFontMetrics();
    int yoffset = fm.getAscent() / 4;
    AffineTransform trans = mp.g.getTransform();

    if ( !mp.prefer.drawCGrid() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrCGrid() :
                                mp.prefer.colorCGrid());
    labels = mp.prefer.drawCGridLabels();
    if ( labels )
      mp.g.setFont(mp.prefer.fontCGrid());

    GeneralPath gp = new GeneralPath();

    /* If printing, set up narrower line */
    Stroke oldStroke = null;
    if ( mp.printing ) {
      oldStroke = mp.g.getStroke();
      mp.g.setStroke(new BasicStroke(0.5f));
    }

    /* Set up more restrictive clipping */
    if ( mp.clip2 != null )
      mp.g.clip(mp.clip2);    // Intersects existing clip (clip1)

    /* Draw lines of RA (Longitude) - 1 segment per degree */
    for ( i = 0; i < 24 && mp.isDrawing(); i += 1 ) { // i in hours
      k = i % 3; k2 = i % 6;
      ra = i * H2R;           // Convert to ra in radians
      l = 0;
      nm = 0;

      for ( j = 90; j >= -90; j-- ) {   // j = degrees declination
        if ( k  != 0 && (j > 80 || j < -80) ) continue;
        if ( k2 != 0 && (j > 86 || j < -86) ) continue;

        if ( mp.rd2xydist(ra, j * D2R, x, y, true) < 1.66f ) { // 95 deg
          if ( l++ == 0 )
            gp.moveTo(x[0], y[0]);
          else {
            gp.lineTo(x[0], y[0]);

            if ( labels && (j % 15 == 7 || j % 15 == -8) ) {
              xm[nm] = (x[0] + x[1]) / 2;
              ym[nm] = (y[0] + y[1]) / 2;
              sm[nm] = scoremidpt(mp, xm[nm], ym[nm]);
              am[nm] = (float)Math.atan2(y[0]-y[1], x[0]-x[1]);
              nm++;
            }
          }
          x[1] = x[0]; // Save
          y[1] = y[0]; //   point
        }
        else {
          if ( l > 1 ) { mp.g.draw(gp); gp.reset(); }
          l = 0;
        }
      }
      if ( l > 1 ) { mp.g.draw(gp); gp.reset(); }

      if ( labels && ((j = selectpt(sm, nm)) >= 0) ) {
        mp.g.translate(xm[j], ym[j]);
        if      ( am[j] <= -HALFPI ) am[j] += Math.PI;
        else if ( am[j]  >  HALFPI ) am[j] -= Math.PI;
        mp.g.rotate(am[j]);
        mp.g.drawString(Integer.toString(i),
                        -fm.stringWidth(Integer.toString(i)) / 2, -yoffset);
        mp.g.setTransform(trans); // Reset to original transform
      }
    }

    /* Draw lines of Declination (Latitude) - 1 segment per 4' at equator */
    for ( i = 75; i >= -75 && mp.isDrawing(); i -= 15 ) { // Every 15 (75,-75)
      decl = i * D2R;                   // Convert to decl in radians
      l = 0;
      nm = 0;

      // Determine how many line segments drawn per 1 hour RA
      segsPerHour = (int)(15 * Math.cos(decl) + 0.5);
      // +/- 75 -> 4, +/- 60 -> 8, +/- 45 -> 11, +/- 30 -> 13
      // +/- 15 -> 14, 0 -> 15 (1 segment per 4' at equator)
      inc = H2R / segsPerHour;          // pi/12/segsPerHour

      for ( j = 0; j <= 24; j++ ) {     // j = hours
        rahr = j * H2R;                 // Convert to rahr in radians

        for ( k = 0; k < segsPerHour; k += 1 ) {
          ra = rahr + k * inc;          // Convert to ra in radians

          if ( mp.rd2xydist(ra, decl, x, y, true) < 1.66f ) { // 95 deg
            if ( l++ == 0 )
              gp.moveTo(x[0], y[0]);
            else {
              gp.lineTo(x[0], y[0]);

              if ( labels ) {
                if ( 2 * k == segsPerHour ) {          // Even segsPerHour
                  xm[nm] = x[0];
                  ym[nm] = y[0];
                  sm[nm] = scoremidpt(mp, xm[nm], ym[nm]);
                  am[nm] = (float)Math.atan2(y[0]-y[1], x[0]-x[1]);
                  nm++;
                }
                else if ( 2 * k == segsPerHour + 1 ) { // Odd segsPerHour
                  xm[nm] = (x[0] + x[1]) / 2;
                  ym[nm] = (y[0] + y[1]) / 2;
                  sm[nm] = scoremidpt(mp, xm[nm], ym[nm]);
                  am[nm] = (float)Math.atan2(y[0]-y[1], x[0]-x[1]);
                  nm++;
                }
              }
            }
            x[1] = x[0]; // Save
            y[1] = y[0]; //   point
          }
          else {
            if ( l > 1 ) { mp.g.draw(gp); gp.reset(); }
            l = 0;
          }
          if ( j == 24 ) break;         // Circle completed
        }
      }
      if ( l > 1 ) { mp.g.draw(gp); gp.reset(); }

      //For debugging
      //for ( j = 0; j < nm; j++ ) {
      //  gp.moveTo(xm[j]-10, ym[j]-10);
      //  gp.lineTo(xm[j]+10, ym[j]+10);
      //  gp.moveTo(xm[j]-10, ym[j]+10);
      //  gp.lineTo(xm[j]+10, ym[j]-10);
      //}
      if ( labels && ((j = selectpt(sm, nm)) >= 0) ) {
        mp.g.translate(xm[j], ym[j]);
        if      ( am[j] <= -HALFPI ) am[j] += Math.PI;
        else if ( am[j]  >  HALFPI ) am[j] -= Math.PI;
        mp.g.rotate(am[j]);
        mp.g.drawString(Integer.toString(i),
                        -fm.stringWidth(Integer.toString(i)) / 2, -yoffset);
        mp.g.setTransform(trans); // Reset to original transform
      }
    }

    if ( mp.clip2 != null ) mp.g.setClip(mp.clip1); // Reset if changed
    if ( mp.printing ) mp.g.setStroke(oldStroke);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Scores a point by how close it is to the window's center
   */
  static private float scoremidpt(MapParms mp, float x, float y) {
    int r = mp.getWidth() - 1;  // Right-most column of window
    int b = mp.getHeight() - 1; // Bottom row of window
    if ( x < 0 || x > r || y < 0 || y > b ) return 0;
    return (x * (r - x)) * (y * (b - y));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Selects best score (above 0)
   */
  static private int selectpt(float[] s, int n) {
    int ret = -1;    // Setting to -1 unnecessary, but avoids warning
    float top = 0;

    for ( int i = 0; i < n; i++ )
      if ( top < s[i] ) { top = s[i]; ret = i; }

    return (top > 0) ? ret : -1;
  }
}

