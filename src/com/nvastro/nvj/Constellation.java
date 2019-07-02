/*
 * Constellation.java  -  Handles constellation names (& draws them)
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

import java.awt.FontMetrics;
import java.awt.RenderingHints;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Handles constellation names (& draws them); all static methods.
 *
 * @author Brian Simpson
 */
public class Constellation {
  /* 3 letter constellation designations (Ser replaced by Se1 &amp; Se2) */
  static final String[] key = {
    "And",      "Ant",      "Aps",      "Aqr",      "Aql",
    "Ara",      "Ari",      "Aur",      "Boo",      "Cae",
    "Cam",      "Cnc",      "CVn",      "CMa",      "CMi",
    "Cap",      "Car",      "Cas",      "Cen",      "Cep",

    "Cet",      "Cha",      "Cir",      "Col",      "Com",
    "CrA",      "CrB",      "Crv",      "Crt",      "Cru",
    "Cyg",      "Del",      "Dor",      "Dra",      "Equ",
    "Eri",      "For",      "Gem",      "Gru",      "Her",

    "Hor",      "Hya",      "Hyi",      "Ind",      "Lac",
    "Leo",      "LMi",      "Lep",      "Lib",      "Lup",
    "Lyn",      "Lyr",      "Men",      "Mic",      "Mon",
    "Mus",      "Nor",      "Oct",      "Oph",      "Ori",

    "Pav",      "Peg",      "Per",      "Phe",      "Pic",
    "Psc",      "PsA",      "Pup",      "Pyx",      "Ret",
    "Sge",      "Sgr",      "Sco",      "Scl",      "Sct",
    "Se1",      "Se2",      "Sex",      "Tau",      "Tel",

    "Tri",      "TrA",      "Tuc",      "UMa",      "UMi",
    "Vel",      "Vir",      "Vol",      "Vul"       };

  /* 3 letter designation for Serpens */
  static final String ser = "Ser";

  /* Note:  The following are in units of .1Hrs and Deg */
  static final short[] loc = {
    4, 38,      102, -35,   160, -75,   227, -10,   196, 0,
    173, -52,   25, 23,     57, 40,     146, 30,    46, -40,
    55, 66,     85, 18,     130, 42,    68, -24,    76, 7,
    210, -21,   80, -59,    15, 65,     130, -45,   222, 68,

    16, -6,     104, -79,   145, -66,   58, -38,    127, 21,
    186, -41,   158, 31,    124, -19,   115, -14,   124, -60,
    207, 42,    207, 12,    52, -62,    179, 62,    212, 7,
    40, -19,    28, -30,    71, 23,     224, -45,   173, 31,

    30, -53,    100, -18,   25, -73,    212, -54,   224, 45,
    105, 16,    104, 33,    57, -20,    152, -15,   152, -45,
    82, 42,     188, 36,    55, -78,    210, -38,   70, -5,
    123, -70,   160, -51,   210, -86,   171, -1,    56, 5,

    194, -66,   228, 19,    36, 44,     6, -50,     55, -50,
    11, 11,     222, -30,   78, -36,    88, -30,    39, -60,
    195, 18,    191, -25,   170, -35,   05, -33,    187, -11,
    156, 9,     182, -2,    102, -2,    41, 17,     192, -50,

    21, 32,     160, -67,   238, -63,   108, 52,    150, 77,
    96, -48,    134, -5,    77, -69,    202, 25     };

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private Constellation() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns number of objects (89).
   */
  static final public int getNumberOfObjects() { return 89; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the full (Latin) name of the constellation, with Serpens
   * as two constellations.
   *
   * @param i Bounds: 0 &lt;= i &lt;= 88,
   *          Serpens Caput (Head) = 75, Serpens Cauda (Tail) =  76
   * @return "" if out of bounds
   */
  static final public String tellName(int i) {
    if ( i < 0 || i > 88 ) return "";

    return TextBndl.getString("Const." + key[i]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the full (Latin) name of the constellation, with Serpens
   * as one constellation.  Used for finding by (whole) constellation.
   *
   * @param i Bounds: 0 &lt;= i &lt;= 87,
   *          Serpens = 75
   * @return "" if out of bounds
   */
  static final public String tellName88(int i) {
    if ( i < 0 || i > 87 ) return "";

    if ( i == 75 )
      return TextBndl.getString("Const.Ser");
    else if ( i > 75 ) i++;

    return TextBndl.getString("Const." + key[i]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the 3 letter abbreviation for the constellation.
   *
   * @param i Bounds: 0 &lt;= i &lt;= 88, Ser = 75 &amp; 76
   * @return "" if out of bounds
   */
  static final public String tellAbbr(int i) {
    if ( i < 0 || i > 88 ) return "";

    if ( i == 75 || i == 76 ) return ser; // Return Ser rather than Se1 or Se2
    return key[i];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the constellation number for the given 3 character abbreviation.
   * This is an inverse of tellAbbr().
   * Used by StarDB while reading external star DBs.
   * ("And" -&gt; 0; "Ser" -&gt; 75 &amp; 76; "Vul" -&gt; 88)
   *
   * @param con 3 character abbreviation (case insensitive)
   * @param s Used only for Serpens (false = Caput, true = Cauda)
   * @return Constellation number 0 - 87, -1 if no match
   */
  static final public int getCNum(String con, boolean s) {
    int i;
    if ( con.equalsIgnoreCase(ser) ) return (s ? 76 : 75);
    for ( i = 0; i < 89; i++ )
      if ( con.equalsIgnoreCase(key[i]) ) break;
    if ( i == 89 ) return -1;
    return i;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the J2000 location (RA/Dec in radians).
   * Called by SkyObject.
   *
   * @param i Constellation selection index
   */
  static final public SphereCoords getJ2000Location(int i) {
    if ( i < 0 || i > 88 ) return null;

    return new SphereCoords(loc[2 * i] * Math.PI / 120,
                            loc[2*i+1] * Math.PI / 180);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the apparent location (RA/Dec in radians).
   * Called by SkyObject.
   *
   * @param i Constellation selection index
   * @param mp Mapping parameters
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   */
  static final public SphereCoords getAppLocation(int i, MapParms mp,
                                                  SphereCoords J2000Coords) {
    if ( i < 0 || i > 88 ) return null;

    SphereCoords sc = new SphereCoords(loc[2 * i] * Math.PI / 120,
                                       loc[2*i+1] * Math.PI / 180);
    if ( J2000Coords != null ) J2000Coords.set(sc);
    mp.precessNutate(sc);
    return sc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the constellation names.
   *
   * @param mp Mapping parameters
   */
  static final public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    SphereCoords scoord = new SphereCoords();
    String s;

    if ( !mp.prefer.drawConstNames() || !mp.isDrawing() ) return;
    boolean full = mp.prefer.drawConstNFull();

    mp.g.setColor(mp.printing ? mp.prefer.prclrConst() :
                                mp.prefer.colorConst());
    mp.g.setFont(mp.prefer.fontConst());

    FontMetrics fm = mp.g.getFontMetrics();
    int yoffset = fm.getAscent() / 2;

    // Couldn't get KEY_TEXT_ANTIALIASING working, at least with
    // the version of Java I was using at the time this code was
    // written, thus use of KEY_ANTIALIASING...
    if ( mp.prefer.antialiasing && !mp.printing )
      mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
    for ( int i = 0; i < 89 && mp.isDrawing(); i++ ) {
      scoord.set(loc[2*i] * Math.PI / 120, loc[2*i+1] * Math.PI / 180);

      if ( mp.rd2xyhit(scoord, x, y) > 0 ) {
        s = full ? tellName(i) : tellAbbr(i);
        x[0] -= fm.stringWidth(s) / 2;
        y[0] += yoffset;
        mp.g.drawString(s, x[0], y[0]);
      }
    }
    if ( mp.prefer.antialiasing && !mp.printing )
      mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
  }

  /* For testing */
  //public static void main(String[] args) {
  //  System.out.println(Constellation.tellName(0));
  //  System.out.println(Constellation.tellAbbr(0));
  //  System.out.println(Constellation.getJ2000Location(0).tellRAHrMnT());
  //  System.out.println(Constellation.getJ2000Location(0).tellDecDgMn());
  //  System.out.println(Constellation.tellName(75));
  //  System.out.println(Constellation.tellAbbr(75));
  //  System.out.println(Constellation.getJ2000Location(75).tellRAHrMnT());
  //  System.out.println(Constellation.getJ2000Location(75).tellDecDgMn());
  //  System.out.println(Constellation.tellName(87));
  //  System.out.println(Constellation.tellAbbr(87));
  //  System.out.println(Constellation.getJ2000Location(87).tellRAHrMnT());
  //  System.out.println(Constellation.getJ2000Location(87).tellDecDgMn());
  //}
}

/*------------------------------------------------------------------------------

From "Working with Text" at Sun's Java site:
(http://java.sun.com/docs/books/tutorial/uiswing/painting/drawingText.html)

  g.drawString("Hello World!", x, y);

For the text painting methods, x and y are integers that specify the position
of the lower left corner of the text. To be precise, the y coordinate
specifies the baseline of the text -- the line that most letters rest on --
which doesn't include room for the tails (descenders) on letters such as "y".
...
Note:  The text-painting methods' interpretation of x and y is different from
that of the shape-painting methods. When painting a shape (such as a rectangle),
x and y specify the upper left corner of the shape's bounding rectangle,
instead of the lower left corner.

"getHeight" = "getAscent" + "getDescent" + "getLeading"
Ascent = typical height of capital letters
Descent = number of pixels between the baseline and the descender line
Leading = distance between one line of text and the next, specifically the
          distance between the descender line of one line of text and the
          ascender line of the next line of text
Height = distance baseline to baseline

------------------------------------------------------------------------------*/

