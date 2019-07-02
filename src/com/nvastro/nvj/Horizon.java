/*
 * Horizon.java  -  Draws the horizon
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

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Draws the horizon.
 *
 * @author Brian Simpson
 */
public class Horizon {
  static private final double HalfPI = Math.PI / 2; // 90 degrees
  static private boolean initialized = false;
  static private String[] Dir; /* "N", "NE", "E", "SE", "S", "SW", "W", "NW" */
  static private int[] lDir = { 0, 45, 90, 135, 180, 225, 270, 315 };
  private TexturePaint tp;
  private Color color = null; // Color used for tp

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public Horizon() {
    if ( initialized == false )
      init();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets text for 8 compass directions ("N", "NE", "E", ...).
   */
  private void init() {
    Dir = new String[8];
    Dir[0] = TextBndl.getString("Compass.N");
    Dir[1] = TextBndl.getString("Compass.NE");
    Dir[2] = TextBndl.getString("Compass.E");
    Dir[3] = TextBndl.getString("Compass.SE");
    Dir[4] = TextBndl.getString("Compass.S");
    Dir[5] = TextBndl.getString("Compass.SW");
    Dir[6] = TextBndl.getString("Compass.W");
    Dir[7] = TextBndl.getString("Compass.NW");

    initialized = true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set texture.
   */
  private void setTexture(Color c) {
    if ( color == null || !color.equals(c) ) {
      color = c;
      BufferedImage texture = LocalGraphics.getBufferedImage(4,4);
      Graphics2D g = texture.createGraphics();
      g.setColor(color);
      g.drawLine(0, 0, 3, 3); g.drawLine(0, 2, 2, 0);
      Rectangle2D tr = new Rectangle2D.Double(0, 0, 4, 4);
      tp = new TexturePaint(texture, tr);
      g.dispose();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the horizon.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    int i, j, k;
    int width, height;          // Width & height of text
    int circle;  // Non-zero if horizon represented by circle 90 deg from center
    double rad;
    GeneralPath gp;

    if ( !mp.prefer.drawHorizon() || !mp.isDrawing() ) return;

    gp = new GeneralPath(GeneralPath.WIND_NON_ZERO, 180);

    Color clr = mp.printing ? mp.prefer.prclrHorizon() :
                              mp.prefer.colorHorizon();
    mp.g.setColor(clr);
    setTexture(clr);
    mp.g.setFont(mp.prefer.fontHorizon());
    FontMetrics fm = mp.g.getFontMetrics();
    height = fm.getAscent();

    double cosfld, sinfld;
    double midx = mp.getMidX();
    double midy = mp.getMidY();
    double radius = Math.PI * mp.pelsPerRadian / 2; // View radius in pixels

    if ( ! mp.prefer.modeRADec ) {   // If Alt-Az mode
      int alt, az, fld;
      double dalt = mp.prefer.getAlt() / MapParms.Deg2Rad;
      if ( dalt >= 0 ) alt = (int)(dalt + 0.5);   // Round to
      else             alt = (int)(dalt - 0.5);   //   nearest degree
      az  = (int)(mp.prefer.getAz() / MapParms.Deg2Rad + 0.5);
      fld = mp.prefer.getFld();
      cosfld = Math.cos(fld * MapParms.Deg2Rad);
      sinfld = Math.sin(fld * MapParms.Deg2Rad);

      // Determine if the horizon will be represented as a circle (i.e. we are
      // looking at the zenith or nadir), or as an arc and a half circle
      // (Note: Currently no circle is shown on monitor if zenith)
      if      ( dalt >  89.9 ) circle =  1; // Zenith
      else if ( dalt < -89.9 ) circle = -1; // Nadir
      else                     circle =  0; // Elsewhere

      /* If printing and not a circle (i.e. not zenith or nadir)
         then draw a gray half circle (opposite to arc and half circle
         below) to show edge of view (Not sure if this is desired...) */
      if ( mp.printing && circle == 0 ) {
        mp.g.setColor(Color.lightGray);

        for ( i = -90; i <= 90; i += 2 ) {
          rad = (i - fld) * MapParms.Deg2Rad;
          x[0] = (float)(midx - Math.sin(rad) * radius);
          y[0] = (float)(midy - Math.cos(rad) * radius);
          if ( i == -90 ) gp.moveTo(x[0], y[0]);
          else            gp.lineTo(x[0], y[0]);
        }
        mp.g.draw(gp);
        gp.reset();

        mp.g.setColor(clr);
      }

      /* Draw the horizon (circle, or arc and half circle) */
      if ( circle != 1 || mp.printing ) {
        if ( circle != 0 ) {    // If zenith (printing only) or Nadir
          for ( i = 0; i < 360; i += 2 ) {
            rad = i * MapParms.Deg2Rad;
            x[0] = (float)(midx + Math.cos(rad) * radius);
            y[0] = (float)(midy + Math.sin(rad) * radius);
            if ( i == 0 ) gp.moveTo(x[0], y[0]);
            else          gp.lineTo(x[0], y[0]);
          }
        }
        else {                  // Else do arc and half circle
          // Construct arc (represents alt = 0)
          j = az - 90;
          k = j + 180;
          for ( i = j; i <= k; i += 2 ) {
            mp.aa2xydist(i * MapParms.Deg2Rad, 0.0, x, y);
            if ( i == j ) gp.moveTo(x[0], y[0]);
            else          gp.lineTo(x[0], y[0]);
          }

          // Add half circle
          for ( i = 88; i > -90; i -= 2 ) {
            rad = (i - fld) * MapParms.Deg2Rad;
            x[0] = (float)(midx + Math.sin(rad) * radius);
            y[0] = (float)(midy + Math.cos(rad) * radius);
            gp.lineTo(x[0], y[0]);
          }
        }

        gp.closePath();

        drawHorzn(mp, gp, clr, circle <= 0 ? true : false);
      }

      /* Draw compass directions */
      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      // Sine of altitude and sine of absolute value of altitude
      double sAlt = Math.sin(alt * MapParms.Deg2Rad);
      double saAlt = Math.sin(Math.abs(alt) * MapParms.Deg2Rad);
      for ( i = 0; i < 8 && mp.isDrawing(); i++ ) {
        j = lDir[i];

        /* If not looking straight up or down then
           don't bother with > 90 or < -90 from middle */
        k = j - az;     // Difference from scrollbar
        while ( k >= 180 ) k -= 360;
        while ( k < -180 ) k += 360;
        k = Math.abs(k); // Degrees difference from middle
        if ( circle == 0 && k > 90 )
          continue;

        /* Set x & y to be approximate location of text */
        rad = j * MapParms.Deg2Rad;
        mp.aa2xydist(rad, 0.0, x, y);

        /* Correct for north or south pole */
        // E.g. when at north pole all compass directions are "S"
        if      ( mp.prefer.getLatDeg() ==  90.0 ) k = 4;  // "S"
        else if ( mp.prefer.getLatDeg() == -90.0 ) k = 0;  // "N"
        else                                       k = i;

        /* Calculate text placement adjustments */
        width = fm.stringWidth(Dir[k]); // Width of text
        // Difference from middle of window will determine adjustment
        rad = (j - az) * MapParms.Deg2Rad;

        // Assuming no field rotation (yet), and xoff & yoff represent
        // (2X) right and down offsets of the center of the text.
        double xoff = - Math.sin(rad) * sAlt;
        double yoff = saAlt * (1 - Math.cos(rad)) - 1;
        // If field rotation stays at zero, then final offsets are:
        // xoff = (xoff - 1) * width / 2
        // yoff = (yoff + 1) * height / 2
        // (-1 and +1 adjust offset for lower left corner of text)

        // Now allow for field rotation
        double xoffr = xoff * cosfld - yoff * sinfld;
        double yoffr = yoff * cosfld + xoff * sinfld;

        // Adjust x & y with final offsets
        x[0] += (xoffr - 1) * width / 2.0;
        y[0] += (yoffr + 0.8) * height / 2.0;  // 0.8 = 1 - 0.2 (fudge)

        mp.g.drawString(Dir[k], x[0], y[0]);
      }
      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    else {                           // Else RA-Dec mode
      double azz;
      SphereCoords viewrd = new SphereCoords(mp.prefer.getRARad(),
                                             mp.prefer.getDecRad());
      SphereCoords viewaa = new SphereCoords();
      mp.rd2aa(viewrd, viewaa); // Calc. Alt-Az coordinates of center of view

      // Determine if the horizon will be represented as a circle (i.e. we are
      // looking at the zenith or nadir), or as an arc and a half circle
      //   89.9 degrees is approx. 1.569 radians
      if      ( viewaa.getAlt() >  1.569 ) circle =  1; // Zenith
      else if ( viewaa.getAlt() < -1.569 ) circle = -1; // Nadir
      else                                 circle =  0; // Elsewhere

      /* If printing and not a circle (i.e. not zenith or nadir)
         then draw a gray half circle (opposite to arc and half circle
         below) to show edge of view (Not sure if this is desired...) */
      if ( mp.printing && circle == 0 ) {
        mp.g.setColor(Color.lightGray);

        azz = viewaa.getAz() / MapParms.Deg2Rad - 90;
        mp.aa2xydist(azz * MapParms.Deg2Rad, 0.0, x, y);
        azz = Math.atan2(y[0]-midy, x[0]-midx) / MapParms.Deg2Rad;
        for ( i = 0; i <= 180; i += 2 ) {
          rad = (azz + i) * MapParms.Deg2Rad;
          x[0] = (float)(midx + Math.cos(rad) * radius);
          y[0] = (float)(midy + Math.sin(rad) * radius);
          if ( i == 0 ) gp.moveTo(x[0], y[0]);
          else          gp.lineTo(x[0], y[0]);
        }

        mp.g.draw(gp);
        gp.reset();

        mp.g.setColor(clr);
      }

      /* Draw the horizon (circle, or arc and half circle) */
      if ( circle != 1 || mp.printing ) {
        if ( circle != 0 ) {    // If Zenith (printing only) or Nadir
          for ( i = 0; i < 360; i += 2 ) {   // then do circle
            rad = i * MapParms.Deg2Rad;
            x[0] = (float)(midx + Math.cos(rad) * radius);
            y[0] = (float)(midy + Math.sin(rad) * radius);
            if ( i == 0 ) gp.moveTo(x[0], y[0]);
            else          gp.lineTo(x[0], y[0]);
          }
        }
        else {                  // Else do arc and half circle
          azz = viewaa.getAz() / MapParms.Deg2Rad - 90;

          // Construct arc
          for ( i = 0; i <= 180; i += 2 ) {
            mp.aa2xydist((azz + i) * MapParms.Deg2Rad, 0.0, x, y);
            if ( i == 0 ) gp.moveTo(x[0], y[0]);
            else          gp.lineTo(x[0], y[0]);
          }

          // Add half circle
          azz = Math.atan2(y[0]-midy, x[0]-midx) / MapParms.Deg2Rad;
          for ( i = 2; i < 180; i += 2 ) {
            rad = (azz + i) * MapParms.Deg2Rad;
            x[0] = (float)(midx + Math.cos(rad) * radius);
            y[0] = (float)(midy + Math.sin(rad) * radius);
            gp.lineTo(x[0], y[0]);
          }
        }

        gp.closePath();

        drawHorzn(mp, gp, clr, circle <= 0 ? true : false);
      }

      /* Draw compass directions */
      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      // Sine of altitude and sine of absolute value of altitude
      double sAlt = Math.sin(viewaa.getAlt());
      double saAlt = Math.sin(Math.abs(viewaa.getAlt()));
      float [] x2 = new float[1];
      float [] y2 = new float[1];
      for ( i = 0; i < 8 && mp.isDrawing(); i++ ) {
        j = lDir[i];

        /* If not looking straight up or down then
           don't bother with > 90 or < -90 from middle */
        // (Add 360.5 so that int truncation does rounding)
        k = (int)(j + 360.5 - viewaa.getAz() / MapParms.Deg2Rad);
        while ( k >= 180 ) k -= 360;
        k = Math.abs(k); // Degrees difference from middle
        if ( circle == 0 && k > 90 )
          continue;

        /* Set x & y to be approximate location of text */
        rad = j * MapParms.Deg2Rad;
        mp.aa2xydist(rad, 0.0, x, y);

        /* Correct for north or south pole */
        // E.g. when at north pole all compass directions are "S"
        if      ( mp.prefer.getLatDeg() ==  90.0 ) k = 4;  // "S"
        else if ( mp.prefer.getLatDeg() == -90.0 ) k = 0;  // "N"
        else                                       k = i;

        /* Calculate text placement adjustments */
        width = fm.stringWidth(Dir[k]); // Width of text
        // Difference from middle of window will determine adjustment
        rad = j * MapParms.Deg2Rad - viewaa.getAz();

        // Look at Alt-Az section for method...
        double xoff = - Math.sin(rad) * sAlt;
        double yoff = saAlt * (1 - Math.cos(rad)) - 1;
        // Allow for rotations
        mp.aa2xydist(viewaa.getAz() - HalfPI, 0.0, x2, y2);
        azz = Math.atan2(y2[0]-midy, x2[0]-midx) + Math.PI;
        cosfld = Math.cos(azz);
        sinfld = Math.sin(azz);
        double xoffr = xoff * cosfld - yoff * sinfld;
        double yoffr = yoff * cosfld + xoff * sinfld;
        // Adjust x & y with final offsets
        x[0] += (xoffr - 1) * width / 2.0;
        y[0] += (yoffr + 0.8) * height / 2.0;  // 0.8 = 1 - 0.2 (fudge)

        mp.g.drawString(Dir[k], x[0], y[0]);
      }
      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw horizon.
   */
  private void drawHorzn(MapParms mp, GeneralPath gp, Color clr, boolean fill) {
    if ( mp.isDrawing() ) {
      if ( fill ) {
        if ( mp.printing ) {
          mp.g.fill(gp);
          if ( mp.milk != null ) {
            mp.milk.intersect(new Area(gp));
            if ( ! mp.milk.isEmpty() ) {
              Color mw = mp.prefer.prclrMilkyWay();
              mp.g.setColor(new Color((2*clr.getRed() + mw.getRed())/3,
                                      (2*clr.getGreen() + mw.getGreen())/3,
                                      (2*clr.getBlue() + mw.getBlue())/3));
              mp.g.fill(mp.milk);
              mp.g.setColor(clr);
            }
          }
          mp.g.draw(gp);
        } // End if printing
        else if ( mp.prefer.shadeHorizon ) {
          Composite comp = mp.g.getComposite();
          mp.g.setComposite(AlphaComposite.getInstance(
               AlphaComposite.SRC_OVER, (/*mp.printing ? 0.4f :*/ 0.75f)));
          mp.g.fill(gp);
          mp.g.draw(gp); // drawing before setComposite is smoother
          mp.g.setComposite(comp);
        }
        else {
          mp.g.setPaint(tp);
          mp.g.fill(gp);
          mp.g.setPaint(clr);
          mp.g.draw(gp);
        }
      } // End if fill
      else mp.g.draw(gp);
    }
  }

  /* For testing */
  //public static void main(String[] args) {
  //  Horizon h = new Horizon();
  //  for ( int i = 0; i < 8; i++ ) {
  //    System.out.println(Dir[i] + " = " + lDir[i]);
  //  }
  //}
}

/*------------------------------------------------------------------------------

An experiment was run on 10-23-01 to compare the size of output print files
between prints using shading vs prints using the crosshatch pattern:

 37335832  out1.ps     Crosshatch pattern
 37335832  out2.ps     Shading
  2943656  out.ps      Horizon turned off

Objects:  Planets, DS (no names), Milky Way, Const lines and boundaries.

When printed, the shaded print looked considerably better than the crosshatch
print.  (Both color & b/w)

On screen, 4 of 4 people preferred the crosshatch (marginally for some).
Both paint at about the same rate (crosshatch may be slightly faster).
I tried setting the clip to the GeneralPath shape and then filling a
rectangle (rather than filling the GeneralPath shape), but the speed
seemed about the same.  Unfortunately drawing the horizon is sslowww...

- - - -

Early April 02 Experiments:  Print issues caused by Graphics2D.setComposite(
AlphaComposite.SRC_OVER, alpha) where alpha was not 1 and then drawing
anything (even a small anything).  (setComposite with an alpha of less than 1
allowed the MilkyWay to partially show through.)  Problems were:
 - Vastly increased print size (e.g. from 4MB w/o horizon to 46MB w horizon)
 - Changes in font (didn't matter if text was drawn previously/subsequently)
 - Much longer print processing.  StarWin.print called ~ 50 times (instead of 2)
Changing from Sun Java 1.3 to 1.4 (Linux) helped a little (font difference
less pronounced, StarWin.print called ~ 7 times), but print size about the same.
(No difference if MilkyWay on or off.)
-> Led to decision to not use setComposite, but rather use solid colors
and handle the interaction between the MilkyWay and Horizon myself.

------------------------------------------------------------------------------*/

