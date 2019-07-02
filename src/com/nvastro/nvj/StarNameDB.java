/*
 * StarNameDB.java  -  Star name database and methods
 * Copyright (C) 2011-2019 Brian Simpson
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Vector;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Helper class for a star name.
 *
 * @author Brian Simpson
 */
class StarName implements Comparable<StarName> {
  String name;
  String allnames;
  int    starnum;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public StarName(String name, String allnames, int starnum) {
    this.name = name;
    this.allnames = allnames;
    this.starnum = starnum;
  }

  public int compareTo(StarName sn) {
    return name.compareTo(sn.name);
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Star name database and methods.
 *
 * @author Brian Simpson
 */
public class StarNameDB {
  /** Name of star name database file (nvstarnames.txt) */
  static final public String SOURCE = "nvstarnames.txt";
  static private Vector<StarName> starnames = new Vector<StarName>();
  static private boolean initialized = false;
  static private String nothing = "";
  static final private int LPAREN = '(';  // Marker for altname
  static private StarDB stardb;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public StarNameDB() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initializes the star name database.  Called by Nvj during
   * program startup.
   */
  public static void init() {
    BufferedReader in = null;

    if ( initialized == false ) {
      stardb = new StarDB();
      File source = new File(Nvj.workingDir, SOURCE);
      String line;

      try {                 // FileReader can throw FileNotFoundException
        if ( source.exists() )
          in = new BufferedReader(new FileReader(source));
        else
          in = new BufferedReader(new InputStreamReader(
                   Nvj.class.getResourceAsStream("/com/nvastro/nvj/" +SOURCE)));

        while ( (line = in.readLine()) != null ) {  // Can throw IOException
          /* line will not contain \r or \n or \0 */
          if ( line.length() == 0 || line.charAt(0) == '#' ) continue;
          line = line.trim();
          if ( line.length() == 0 ) continue;

          if ( ! addStarName(line) ) {
            ErrLogger.logError(ErrLogger.formatError(
                      TextBndl.getString("SNFile.LnErr"), SOURCE, "  " + line));
          }
        }

        Collections.sort(starnames);
      }
      catch ( Exception e ) {
        String msg = TextBndl.getString("SNFile.RdErr");
        ErrLogger.die(ErrLogger.formatError(msg, SOURCE, null));
      }
      finally {
        if ( in != null ) try { in.close(); } catch(IOException e) { ; }
      }

      starnames.trimToSize();
      initialized = true;
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add star name.
   */
  static private boolean addStarName(String line) {
    int i, sp, lparen;
    int con = 0, grk = 0, gnm = 0, flm = 0;
    String name, allnames;

    /* Get constellation */
    if ( line.indexOf(' ') != 3 ) return false;
    String cons = line.substring(0, 3);
    for ( i = 0; i < 89; i++ ) {
      if ( cons.equals(Constellation.key[i]) ) break;
    }
    if ( i == 89 ) {
      if ( cons.equals(Constellation.ser) )
        i = 75;  // Serpens Caput (Head)
      else return false;
    }
    // Constellations in StarDB have offset of 1, not 0
    con = i + 1;

    /* Get designation of star within constellation */
    String next = line.substring(3).trim();
    try {
      sp = next.indexOf(' ');
      if ( sp > 4 ) return false;  // Should be 1-4 characters
      String des = next.substring(0, sp);
      if ( des.charAt(0) == 'F' && sp > 1 && Character.isDigit(des.charAt(1))) {
        flm = Integer.parseInt(des.substring(1));
      }
      else if ( Character.isAlphabetic(des.charAt(0)) ) {
        grk = des.charAt(0);
        if ( sp > 1 ) {
          if ( Character.isDigit(des.charAt(1)) ) {
            gnm = Integer.parseInt(des.substring(1));
          } else {
            gnm = des.charAt(1);
          }
        }
      }
      else if ( sp == 2 || (sp == 4 && next.charAt(2) == '-') ) {
        grk = Integer.parseInt(des.substring(0, 2));
        if ( sp == 4 ) gnm = Integer.parseInt(des.substring(3, 4));
      }
      else return false;
    } catch ( Exception e ) { return false; }

    /* Get name */
    allnames = next.substring(sp).trim();
    if ( allnames.length() == 0 ) return false;
    if ( (lparen = allnames.indexOf(LPAREN)) >= 0 )
         name = allnames.substring(0, lparen).trim();
    else name = allnames;

    /* Find star in stardb */
    short lim100 = 600;    // Search limit of magnitude 6.00
    int index = stardb.getIndex((byte)con, (byte)grk, (byte)gnm,
                                           (byte)flm, lim100);
    if ( con == 76 && index < 0 ) // If Serpens Caput fails, try Cauda
        index = stardb.getIndex((byte)77,  (byte)grk, (byte)gnm,
                                           (byte)flm, lim100);
    if ( index < 0 ) return false;

    starnames.addElement(new StarName(name, allnames, index));
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns number of star names.
   */
  public int getNumberOfNames() {
    return starnames.size();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns name of star.
   *
   * @param i Index into star name database
   * @return "" if out of range
   */
  public String tellName(int i) {
    if ( i < 0 || i >= starnames.size() ) return nothing;
    return starnames.elementAt(i).name;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns all names of star.
   *
   * @param i Index into star name database
   * @return "" if out of range
   */
  public String tellName2(int i) {
    if ( i < 0 || i >= starnames.size() ) return nothing;
    return starnames.elementAt(i).allnames;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns name of star.
   *
   * @param i Index into star database (not star name database)
   * @return null if out of range or no name available
   */
  public String tellNameForStarIndex(int i) {
    int j;

    int num = starnames.size();
    for ( j = 0; j < num; j++ ) {
      if ( i == starnames.elementAt(j).starnum ) break;
    }
    if ( j < num ) return tellName(j);
    else           return null;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the J2000 location.
   * Called by SkyObject.
   *
   * @param i Index into star name database
   * @return Coordinates of star (containing RA/Dec in radians)
   */
  public SphereCoords getJ2000Location(int i) {
    if ( i < 0 || i >= starnames.size() ) return new SphereCoords(0.0, 0.0);
    int index = starnames.elementAt(i).starnum;
    return stardb.getJ2000Location(index);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the apparent location.
   * Called by SkyObject.
   *
   * @param i Index into star name database
   * @param mp Mapping parameters
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   * @return Coordinates of star (containing RA/Dec in radians)
   */
  public SphereCoords getAppLocation(int i, MapParms mp,
                                     SphereCoords J2000Coords) {
    if ( i < 0 || i >= starnames.size() ) return new SphereCoords(0.0, 0.0);
    int index = starnames.elementAt(i).starnum;
    return stardb.getAppLocation(index, mp, J2000Coords);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Determines if specified star is viewable.
   *
   * @param i Index into star name database
   * @param pref User preferences
   * @return True if viewable, false if not
   */
  public boolean isViewable(int i, Preferences pref) {
    if ( i < 0 || i >= starnames.size() ) return false;
    int index = starnames.elementAt(i).starnum;
    return stardb.isViewable(index, pref);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the star names.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    int index;
    SphereCoords scoord;

    if ( !mp.prefer.drawStarNames() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrStarName() :
                                mp.prefer.colorStarName());
    mp.g.setFont(mp.prefer.fontStarName());
    FontMetrics fm = mp.g.getFontMetrics();
    float yoffset = fm.getAscent() / 2.0f;

    if ( mp.prefer.antialiasing && !mp.printing )
      mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

    int num = starnames.size();
    for ( int i = 0; i < num && mp.isDrawing(); i++ ) {
      index = starnames.elementAt(i).starnum;
      scoord = stardb.getJ2000Location(index);

      if ( mp.rd2xyhit(scoord, x, y) > 0 ) {
        mp.g.drawString(tellName(i), x[0] + 4, y[0] + yoffset);
      }
    }

    if ( mp.prefer.antialiasing && !mp.printing )
      mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
  }

  /* For testing */
  //public static void main(String[] args) {
  //  StarNameDB db = new StarNameDB();
  //
  //  for ( int i = 0; i < starnames.size(); i++ ) {
  //    System.out.println(db.tellName(i) + ", " +
  //                       db.tellName2(i));
  //  }
  //  System.out.println("Should be Sirius:  " + db.tellNameForStarIndex(0));
  //}
}

/*------------------------------------------------------------------------------

Misc. notes...
 Someday:  Add to star name listing some of the names at a large listing
 of star names on the Internet at:
    http://www.ras.ucalgary.ca/~gibson/starnames/starnames.html

------------------------------------------------------------------------------*/

