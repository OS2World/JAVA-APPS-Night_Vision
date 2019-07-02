/*
 * MilkyWay.java  -  Draws the Milky Way
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

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.DataInputStream;
import java.io.IOException;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Draws the Milky Way.
 *
 * @author Brian Simpson
 */
public class MilkyWay {
  static final String SOURCE = "milkyway.db";
  static private int num = 0;   // Number of points (array size)
  static private boolean initialized = false;
  // Can't use array of SphereCoords, as some ra's are "illegal"
  static private float ra[], dec[];

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public MilkyWay() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initializes the Milky Way data.  Called by Nvj during program startup.
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
   * Draws the Milky Way.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    int pts;

    mp.milk = null;
    if ( !mp.prefer.drawMilkyWay() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrMilkyWay() :
                                mp.prefer.colorMilkyWay());

    /* Set up more restrictive clipping */
    if ( mp.clip2 != null )
      mp.g.clip(mp.clip2);    // Intersects existing clip (clip1)

    GeneralPath gp = new GeneralPath();
    int i = 0;
    while ( i < num && mp.isDrawing() ) {
      if ( mp.rd2xydist(ra[i], dec[i], x, y) < 2.18 ) { // < 125 degrees
        gp.reset();
        pts = 0;            // No previous coordinate
        while ( ++i < num && ra[i] >= 0 && mp.isDrawing() ) {
          mp.rd2xydist(ra[i], dec[i], x, y);
          if ( pts++ == 0 ) gp.moveTo(x[0], y[0]);
          else              gp.lineTo(x[0], y[0]);
        }
        if ( mp.isDrawing() ) {
          gp.closePath();
          mp.g.fill(gp);
          if ( mp.printing ) {
            if ( mp.milk == null ) mp.milk = new Area(gp);
            else                   mp.milk.add(new Area(gp));
          }
        }
      }
      else { /* Skip this section */
        while ( ++i < num && ra[i] >= 0 && mp.isDrawing() ) ;
      }
      if ( ra[i] == -2 ) break;  // Leave if EndMarker
      else i++;  // Skip over SectionEndMarker (RA = -1)
    }

    /* Cancel restrictive clipping */
    if ( mp.clip2 != null ) mp.g.setClip(mp.clip1); // Reset if changed
  }
}

/*------------------------------------------------------------------------------

Format of Milky Way data
------------------------

Section
Section
...
Section
EndMarker

Everything is in pairs of floats representing RA and Dec where
0 <= RA < 24 (unless pair is a special marker) and -90 <= Dec <= 90.
For example the EndMarker is a pair of floats:  (RA, Dec) = (-2.0f, 0.0f).

Each section is a chunk of the Milky Way and has the following format:

CenterPoint
RADec
RADec
...
RADec
SectionEndMarker (except last section)

The CenterPoint, which is not to be rendered, is the (RA, Dec) that is
approximately in the center of the farthest reaches of the section's
(RA, Dec) pairs.  It allows Night Vision to decide if the section should
be rendered or skipped.  The farthest point from the CenterPoint is
guaranteed to be <= 29.95 degrees.

Each RADec is a (RA, Dec) pair to be rendered.

The SectionEndMarker is (RA, Dec) = (-1.0f, 0.0f).

------------------------------------------------------------------------------*/

