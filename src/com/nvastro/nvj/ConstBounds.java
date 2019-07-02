/*
 * ConstBounds.java  -  Draws the constellation boundaries
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

import java.awt.geom.GeneralPath;
import java.io.DataInputStream;
import java.io.IOException;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Draws the constellation boundaries.
 *
 * @author Brian Simpson
 */
public class ConstBounds {
  static final String SOURCE = "conbounds.db";
  static private int num = 0;   // Number of points (array size)
  static private boolean initialized = false;
  // Can't use array of SphereCoords, as some ra's are "illegal"
  static private float ra[], dec[];

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public ConstBounds() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initializes the constellation boundaries.  Called by Nvj during
   * program startup.
   */
  public static void init() {
    if ( initialized == false ) {
      try {
        DataInputStream in = new DataInputStream(
                                 Nvj.class.getResourceAsStream(SOURCE));
        num = in.available() / 8;

        if ( num > 0 ) {
          ra  = new float[num];
          dec = new float[num];

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
   * Draws the constellation boundaries.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    GeneralPath gp = new GeneralPath();
    int n = 0;

    if ( !mp.prefer.drawConstBounds() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrConstBound() :
                                mp.prefer.colorConstBound());

    /* Set up more restrictive clipping */
    if ( mp.clip2 != null )
      mp.g.clip(mp.clip2);    // Intersects existing clip (clip1)

    for ( int i = 0; i < num && mp.isDrawing(); i++ ) {
      if ( ra[i] >= 0 && mp.rd2xydist(ra[i], dec[i], x, y) < 1.62f ) { // 1.62
        // is approx. 92.8 degrees.  Max increment of data is 2 degrees.
        if ( n++ == 0 ) gp.moveTo(x[0], y[0]);
        else            gp.lineTo(x[0], y[0]);
      }
      else {
        if ( n > 1 ) { mp.g.draw(gp); gp.reset(); }
        n = 0;
      }
    }
    if ( n > 1  && mp.isDrawing() ) mp.g.draw(gp);

    /* Cancel restrictive clipping */
    if ( mp.clip2 != null ) mp.g.setClip(mp.clip1); // Reset if changed
  }
}

/*------------------------------------------------------------------------------

SOURCE (above) has similar format to SOURCE in MilkyWay.java.
See that file for description.

------------------------------------------------------------------------------*/

