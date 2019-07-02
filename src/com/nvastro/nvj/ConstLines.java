/*
 * ConstLines.java  -  Draws the constellation lines
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
import java.io.DataInputStream;
import java.io.IOException;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Draws the constellation lines.
 *
 * @author Brian Simpson
 */
public class ConstLines {
  static final String SOURCE = "conlines.db";
  static private int num = 0;   // Number of points (array size)
  static private boolean initialized = false;
  // Can't use array of SphereCoords, as some ra's are "illegal"
  static private double ra[], dec[];

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public ConstLines() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initializes the constellation lines.  Called by Nvj during
   * program startup.
   */
  public static void init() {
    if ( initialized == false ) {
      try {
        DataInputStream in = new DataInputStream(
                                 Nvj.class.getResourceAsStream(SOURCE));
        num = in.available() / 8;

        if ( num > 0 ) {
          ra  = new double[num];
          dec = new double[num];

          for ( int i = 0; i < num; i++ ) {
            ra[i]  = in.readFloat();
            dec[i] = in.readFloat();
          }
          if ( ra[num-1] != -2.0 ) throw new IOException();
        }
      }
      catch ( Exception e ) {
        num = 0;
      }

      if ( num == 0 ) // Should not happen
        System.err.println("Cannot open or read " + SOURCE);

      initialized = true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the constellation lines.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[2];
    float[] y = new float[2];
    boolean newsection = true, newpt, oldpt = false;
    Line2D.Float line = new Line2D.Float();

    if ( !mp.prefer.drawConstLines() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrConst() :
                                mp.prefer.colorConst());

    /* Set up more restrictive clipping */
    if ( mp.clip2 != null )
      mp.g.clip(mp.clip2);    // Intersects existing clip (clip1)

    for ( int i = 0; i < num && mp.isDrawing(); i++ ) {
      if ( ra[i] >= 0 ) {
        newpt = mp.rd2xydist(ra[i], dec[i], x, y) < 1.58f; // ~90.5 deg
        if ( (newpt || oldpt) && newsection == false ) {
          line.setLine(x[1], y[1], x[0], y[0]);
          mp.g.draw(line);
        }

        x[1] = x[0]; y[1] = y[0];
        oldpt = newpt;
        newsection = false;
      }
      else {
        oldpt = false;
        newsection = true;
      }
    }

    /* Cancel restrictive clipping */
    if ( mp.clip2 != null ) mp.g.setClip(mp.clip1); // Reset if changed
  }
}

/*------------------------------------------------------------------------------

SOURCE (above) has similar format to SOURCE in MilkyWay.java.
See that file for description.

------------------------------------------------------------------------------*/

