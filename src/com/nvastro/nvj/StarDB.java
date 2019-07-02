/*
 * StarDB.java  -  Star database and methods
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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Star (Used only for external star DBs)
 */
class Star implements Comparable<Star> {
  public double ra;
  public double dec;
  public double mag;
  public String sp;
  public byte con;
  public byte greek;
  public byte misc;
  public byte flam;

  public int compareTo(Star s) {
    if ( mag > s.mag )       return  1;
    else if ( mag == s.mag ) return  0;
    else                     return -1;
  }
}


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Star database and methods.
 *
 * @author Brian Simpson
 */
public class StarDB {
  static final String SOURCE = "star.db";
  static final String EXTSOURCE = "nvstars.txt";
  static private final int NUM_LAB = 2000; // I'll assume that I'll never need
                                           // to show more than 2000 labels...
                           // Once counted 1691 labels on a 180 degree view
  /** Number of bytes in DB per star */
  static public final int STAR_BYTES = 24; // Number of bytes in DB per star
  static private int num = 0;              // Number of stars in DB
  static private boolean initialized = false;
  static private DecNumFormat mag_format;
  static private double[] ra;           // Right ascension
  static private double[] dec;          // Declination
  static private double[] rx;           // Rectangular x coordinate
  static private double[] ry;           // Rectangular y coordinate
  static private double[] rz;           // Rectangular z coordinate
  static private short[]  mag100;       // Magnitude * 100
  static private byte[]   flam;
  static private byte[]   greek;
  static private byte[]   misc;
  static private byte[]   con;
  static private String[] spect;
  private StarImages starimages;

  // Used for reading external star DB
  static private int numComplaints = 0;
  static private int linenum = 0;
  static private ArrayList<Star> extstars;

  final static private double h2r = Math.PI / 12;
  final static private double d2r = Math.PI / 180;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public StarDB() {
    if ( initialized == false ) {
      init();
    }

    starimages = new StarImages();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialize the star DB.  Called by Nvj during program startup.
   */
  public static void init() {
    if ( initialized == false ) {
      if ( ! tryExtFile() ) {   // Try external star DB 1st
        try {                   // then try internal
          DataInputStream in = new DataInputStream(
                                   Nvj.class.getResourceAsStream(SOURCE));
          int len = in.available();

          internalDB(in, len / STAR_BYTES);
        }
        catch ( Exception e ) { // Should not happen
          System.err.println("Cannot open or read " + SOURCE);
          num = 0;
        }
      }

      if ( num == 0 ) // Should not happen
        ErrLogger.die("No star DB available.");

      mag_format = new DecNumFormat("0.00");

      initialized = true;
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialize via the internal star DB.
   *
   * @param in DataInputStream from which to read in coordinates
   * @param n Number of coordinates
   * @throws IOException If db corrupted
   */
  private static void internalDB(DataInputStream in, int n) throws IOException {
    double cosde;

    num = n;

    ra     = new double[num];
    dec    = new double[num];
    rx     = new double[num];
    ry     = new double[num];
    rz     = new double[num];
    mag100 = new short[num];
    flam   = new byte[num];
    greek  = new byte[num];
    misc   = new byte[num];
    con    = new byte[num];
    spect  = new String[num];
    byte[] spb = new byte[2];

    for ( int i = 0; i < num; i++ ) {
      // The order is important (must match DB)
      ra[i]     = in.readDouble();
      dec[i]    = in.readDouble();
      mag100[i] = in.readShort();
      spb[0] = in.readByte();
      spb[1] = in.readByte();
      spect[i] = new String(spb).trim();
      con[i]    = in.readByte();
      greek[i]  = in.readByte();
      flam[i]   = in.readByte();
      misc[i]   = in.readByte();

      // Derived numbers
      cosde = Math.cos(dec[i]);
      rx[i] = cosde * Math.cos(ra[i]);
      ry[i] = cosde * Math.sin(ra[i]);
      rz[i] = Math.sin(dec[i]);
      // Note: Using rectangular coordinates is a bit speedier than
      // using spherical coordinates by a factor of about 6/5.  Not
      // sure if this is worth the extra memory space.
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Experimental and undocumented function for reading an external star db.
   * Format read is likely to change.
   */
  private static boolean tryExtFile() {
    int i, j, k;
    String line;
    String[] field = new String[8];
    File source = new File(Nvj.workingDir, EXTSOURCE);

    /*--------------------------------------------------------------------------
     * Rules
     * -----
     *
     * External star db must be named "nvstars.txt" (EXTSOURCE)
     * and must be in same location as nvstarnames.txt, nvdeepsky.txt, ...
     *
     * 1 star per line
     * '#' and anything after is a comment
     * Blank lines OK
     *
     * Format:
     * HH:MM:SS.ss|sDD:MM:SS.s|sMagn|Spec|Con|Byr#|Flam
     *
     * 06:45:09.25|-16:42:47.3|-1.44|A0  |CMa|alp |9    # Comment
     * 12:26:35.94|-63:05:56.6|+0.77|B0  |Cru|alp2|
     * 22:27:46.22|+31:50:23.9|+6.00|K2
     *
     * -9.5 < magnitude < 20
     * Up to 4 chars for Spectral data
     * Use 3 chars for constellation (case insensitive) or leave blank
     * Use 2-3 chars for Bayer (case insensitive), optionally followed by number
     -------------------------------------------------------------------------*/

    extstars = new ArrayList<Star>(1000);
    Star star;
    num = 0;
    if ( ! source.exists() ) return false;
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(source));
      while ( (line = in.readLine()) != null ) {  // Can throw IOException
        linenum++;
        if ( (i = line.indexOf('#')) >= 0 ) line = line.substring(0, i);
        line = line.trim();
        if ( line.length() == 0 ) continue;

        star = new Star();

        /* Convert line to tokens */
        StringTokenizer t = new StringTokenizer(line, "|", true);
        k = t.countTokens();

        // Look for: field | field | field ..., but some fields may be missing
        for ( j = 0; j < 7; j++ ) field[j] = "";
        for ( i = 0, j = 0; i < k && j < 7; j++ ) { // j fields, i tokens
          field[j] = t.nextToken(); i++;
          // Was field blank and we picked up next '|'?
          if ( field[j].equals("|") ) { field[j] = ""; }
          else if ( i++ < k ) t.nextToken();  // '|'
        }

        if ( ! convertRA(field[0], star) ) {
          complain("Bad RA in", line, true); continue;
        }
        if ( ! convertDec(field[1], star) ) {
          complain("Bad Dec in", line, true); continue;
        }
        if ( ! convertMag(field[2], star) ) {
          complain("Bad magnitude in", line, true); continue;
        }
        star.sp = new String(field[3].trim() + "    ").substring(0, 4);

        i = convertCon(field[4], star);
        if ( i < 0 ) {
          complain("Bad constellation in", line, false);
        }
        else if ( i > 0 ) {
          if ( convertByr(field[5], star) < 0 )
            complain("Bad Bayer in", line, false);
          if ( ! convertFlm(field[6], star) )
            complain("Bad Flamsteed in", line, false);
        }

        extstars.add(star);
        num++;
      }
    }
    catch ( Exception e ) {
      num = 0;
    }
    finally {
      if ( in != null ) {
        try { in.close(); } catch (IOException c) {}
        in = null;
      }
    }
    Collections.sort(extstars);
    if ( num == 0 ) return false; // External stars not available

    ra     = new double[num];
    dec    = new double[num];
    rx     = new double[num];
    ry     = new double[num];
    rz     = new double[num];
    mag100 = new short[num];
    flam   = new byte[num];
    greek  = new byte[num];
    misc   = new byte[num];
    con    = new byte[num];
    spect  = new String[num];

    double cosde;
    Star s;
    for ( i = 0; i < num; i++ ) {
      s = extstars.get(i);
      ra[i]     = s.ra;
      dec[i]    = s.dec;
      mag100[i] = (short)(s.mag * 100);
      spect[i] = s.sp;
      con[i]    = s.con;
      greek[i]  = s.greek;
      flam[i]   = s.flam;
      misc[i]   = s.misc;

      // Derived numbers
      cosde = Math.cos(dec[i]);
      rx[i] = cosde * Math.cos(ra[i]);
      ry[i] = cosde * Math.sin(ra[i]);
      rz[i] = Math.sin(dec[i]);
    }

    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private void complain(String msg, String line, boolean reject) {
    if ( ++numComplaints <= 10 ) {
      ErrLogger.logError(EXTSOURCE + (reject ? ": Rejecting line, " : ": ") +
                         msg + " line " + linenum + ": " + line);
      if ( numComplaints == 10 )
        ErrLogger.logError(EXTSOURCE + ": 10 errors found; " +
                           "no more will be reported");
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private boolean convertRA(String rastr, Star star) {
    StringTokenizer t = new StringTokenizer(rastr, ":");
    if ( t.countTokens() != 3 ) return false;

    try {
      double h = Double.parseDouble(t.nextToken());
      double m = Double.parseDouble(t.nextToken());
      double s = Double.parseDouble(t.nextToken());
      if ( h < 0 || h > 23 || m < 0 || m >= 60 || s < 0 || s >= 60 )
        return false;
      star.ra = (((s / 60) + m) / 60 + h) * h2r;  // radians
    } catch(Exception e) { return false; }
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private boolean convertDec(String decstr, Star star) {
    StringTokenizer t = new StringTokenizer(decstr, ":");
    if ( t.countTokens() != 3 ) return false;

    try {
      double d = Double.parseDouble(t.nextToken());
      double m = Double.parseDouble(t.nextToken());
      double s = Double.parseDouble(t.nextToken());
      if ( d < -90 || d > 90 || m < 0 || m >= 60 || s < 0 || s >= 60 )
        return false;
      // Can't just test if d < 0 (e.g. decstr = -00:49:00.0)
      if ( decstr.indexOf('-') >= 0 ) { m *= -1; s*= -1; }
      star.dec = ((s / 60) + m) / 60 + d;    // degrees
      if ( star.dec > 90 || star.dec < -90 ) return false;
      star.dec *= d2r;                       // radians
    } catch(Exception e) { return false; }
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private boolean convertMag(String magstr, Star star) {
    try {
      star.mag = Double.parseDouble(magstr);
      if ( star.mag < -9.5 || star.mag > 20 ) return false;
    } catch(Exception e) { return false; }
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private int convertCon(String constr, Star star) {
    constr = constr.trim();
    int len = constr.length();
    if ( len == 0 ) {
      star.con = 0;
      return 0;                      // Constellation unspecified
    }
    else if ( len != 3 ) {
      star.con = 0;
      return -1;                     // Not 3 chars
    }

    // 4.45 (radians) is about 17 hours - used for splitting Serpens
    star.con = (byte)(Constellation.getCNum(constr, star.ra > 4.45) + 1);
    if ( star.con == 0 ) return -1;  // No match
    else                 return 1;   // Match found
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private int convertByr(String byrstr, Star star) {
    star.misc = 0;
    byrstr = byrstr.trim();

    int len = byrstr.length();
    if ( len == 0 ) {
      star.greek = 0;
      return 0;                      // Bayer unspecified
    }

    // Convert something like "pi 3" to "pi3"
    if ( len > 3 && byrstr.charAt(2) == ' ' ) {
      byrstr = new String(byrstr.substring(0, 2) + byrstr.substring(3));
      len--;
    }

    // Split off trailing number, if there
    try {
      if ( byrstr.matches("[A-Za-z]{2,3}\\d") ) {
        len--;
        star.misc = Byte.parseByte(byrstr.substring(len));
        byrstr = byrstr.substring(0, len);
      }
    } catch ( Exception e ) { }

    star.greek = (byte)Greek.getGNum(byrstr);
    if ( star.greek == 0 ) return -1;// No match
    else                   return  1;// Match found
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by tryExtFile().
   */
  static private boolean convertFlm(String flmstr, Star star) {
    star.flam = 0;
    flmstr = flmstr.trim();
    if ( flmstr.length() == 0 ) return true;

    try {
      star.flam = Byte.parseByte(flmstr);
    } catch ( Exception e ) { return false; }
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by StarNameDB.
   *
   * @return -1 if not found
   */
  public int getIndex(byte cns, byte grk, byte gnm, byte flm, short lim100) {
    int i;

    if ( flm > 0 ) {
      for ( i = 0; i < num && mag100[i] <= lim100; i++ )
        if ( cns == con[i] && flm == flam[i] ) return i;
    }
    else {
      for ( i = 0; i < num && mag100[i] <= lim100; i++ )
        if ( cns == con[i] && grk == greek[i] &&
             (gnm == 0 || gnm == misc[i]) ) return i;
    }

    return -1;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * January 2017 Added additional star designations coded in greek and misc.
   * This function extracts all of the designations.  (No bounds checking.)
   * Called by StarDB (for IndentifyDlg) and by StarDsgnDB (for FindStarDlg).
   *
   * @param i The star number within the database
   * @param cns An array of 1 int to return the constellation number
   * @param grk An array of 1 int to return a number to sort Greek designations
   * @param flm An array of 1 int to return the flamsteed number
   *
   * cns[0] Returns the constellation number (or 0 if none)
   *        (And = 1, Ser = 76,77, Vul = 89)
   *        (If 0, then ignore return value and flm[0])
   * grk[0] Returns a number used to sort Greek designations
   *        (Alpha = 0, Beta = 10, Gamma = 20, ..., Beta1 = 11, ...)
   *        (Used only by FindStarDlg)
   * flm[0] Returns the flamsteed number (or 0 if none)
   * @return The alphanumeric designation (Bayer, ...) or null for none.
   */
  public String getDesignations(int i, int[] cns, int[] grk, int[] flm) {
    char[] chrs = new char[2];

    cns[0] = con[i];
    if ( cns[0] == 0 ) {
      return null;
    }

    if ( flam[i] > 0 ) {
      flm[0] = flam[i];
    } else {
      flm[0] = 0;
    }

    grk[0] = 0;
    if ( greek[i] > 0 && greek[i] <= 24 ) {  // Greek letter
      grk[0] = greek[i] * 10;
      if ( misc[i] > 0 && misc[i] <= 9 ) {
        grk[0] += misc[i];
        return new String(Greek.tellGreek(greek[i]) + misc[i]);
      }
      else
        return Greek.tellGreek(greek[i]);
    }
    else if ( greek[i] > 24 ) {              // Alphanumeric
      chrs[0] = (char) greek[i];
      if ( misc[i] == 0 ) {
        return new String(chrs, 0, 1);
      } else {
        chrs[1] = (char) misc[i];
        return new String(chrs, 0, 2);
      }
    }
    else if ( greek[i] < 0 ) {               // V###
      // Remove msb (sign) to recover the number
      int V = ((int)(greek[i] & 0x7F)) * 100 + ((int)(misc[i]));
      return new String("V" + V);
    }

    if ( flm[0] == 0 ) { // If no flamsteed (& no alphanumeric)
      cns[0] = 0;        // then better zero out constellation
    }
    return null; // No alphanumeric string
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * January 2017 Added additional star designations coded in greek and misc.
   * This function extracts all of the designations and places them into a
   * single string to display on the chart.  (No bounds checking.)
   * Called only with StarDB.
   *
   * @param i The star number within the database
   * @param byr Flag specifying whether to display Bayer, alphanumeric, ...
   * @param flm Flag specifying whether to display Flamsteed
   *
   * @return All designations or null for none.
   */
  private String getDesignation(int i, boolean byr, boolean flm) {
    char[] chrs = new char[2];
    String flamStr = null, bayrStr = null;

    if ( con[i] == 0 ) {
      return null;
    }

    if ( flm && flam[i] > 0 )
      flamStr = String.format("%d", flam[i]);


    if ( byr ) {
      if ( greek[i] > 0 && greek[i] <= 24 ) {  // Greek letter
        if ( misc[i] > 0 && misc[i] <= 9 ) {
          bayrStr = new Character(Greek.getgreek(greek[i])).toString() +
                    Byte.toString(misc[i]);
        }
        else
          bayrStr = new Character(Greek.getgreek(greek[i])).toString();
      }
      else if ( greek[i] > 24 ) {              // Alphanumeric
        chrs[0] = (char) greek[i];
        if ( misc[i] == 0 ) {
          bayrStr = new String(chrs, 0, 1);
        } else {
          chrs[1] = (char) misc[i];
          bayrStr = new String(chrs, 0, 2);
        }
      }
      else if ( greek[i] < 0 ) {               // V###
        // Remove msb (sign) to recover the number
        int V = ((int)(greek[i] & 0x7F)) * 100 + ((int)(misc[i]));
        bayrStr = new String("V" + V);
      }
    }

    if ( flamStr != null ) {
      if ( bayrStr != null ) {
        //return flamStr + "-" + bayrStr;
        if ( greek[i] > 0 && greek[i] <= 24 ) return bayrStr;
        else                                  return flamStr;
      }
      else
        return flamStr;
    }
    else if ( bayrStr != null )
      return bayrStr;
    else
      return null;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the number of stars.
   */
  public int getNumberOfStars() {
    return num;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns 100 x magnitude of star.  (No bounds checking.)
   *
   * @param i Index into star database
   */
  public int getMag100(int i) {
    return mag100[i];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns 100 x max magnitude of DB (dimmest star).
   */
  public int getMaxMag100() {
    return mag100[num - 1];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns right ascension (in radians) of star.  (No bounds checking.)
   *
   * @param i Index into star database
   */
  public double getRARad(int i) {
    return ra[i];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns declination (in radians) of star.  (No bounds checking.)
   *
   * @param i Index into star database
   */
  public double getDecRad(int i) {
    return dec[i];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the J2000 location.
   * Called by SkyObject and StarNameDB.  (No bounds checking.)
   *
   * @param i Index into star database
   * @return Coordinates of star (containing RA/Dec in radians)
   */
  public SphereCoords getJ2000Location(int i) {
    return new SphereCoords(ra[i], dec[i]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the apparent location.
   * Called by SkyObject and StarNameDB.  (No bounds checking.)
   *
   * @param i Index into star database
   * @param mp Mapping parameters
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   * @return Coordinates of star (containing RA/Dec in radians)
   */
  public SphereCoords getAppLocation(int i, MapParms mp,
                                     SphereCoords J2000Coords) {
    SphereCoords sc = new SphereCoords(ra[i], dec[i]);
    if ( J2000Coords != null ) J2000Coords.set(sc);
    mp.precessNutate(sc);
    mp.adjustEquatForAberration(sc);
    return sc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns magnitude in String format.  (No bounds checking.)
   *
   * @param i Index into star database
   */
  public String tellMagnitude(int i) {
    return mag_format.format(mag100[i]/100.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns spectral data in String format.  (No bounds checking.)
   *
   * @param i Index into star database
   */
  public String tellSpectral(int i) {
    return spect[i];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates if star is viewable based on magnitude.  (No bounds checking.)
   *
   * @param i Index into star database
   * @param pref User preferences
   * @return True if viewable, false if not
   */
  public boolean isViewable(int i, Preferences pref) {
    return mag100[i] <= getClipLimit100(pref);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Stars shown have magnitudes up to and including this limit
   */
  private int getClipLimit100(Preferences pref) {
    // If mp.prefer.getZiLimMag10 returns 61, allow stars up to 6.19 (619)
    int ziMag100 = pref.getZiLimMag10() * 10 + 9;
    int zoMag100 = pref.getZoLimMag10() * 10;

    // getZoomFactor goes from 0.0 to 1.0 as user zooms in
    return (int)(pref.getZoomFactor() * (ziMag100 - zoMag100) + zoMag100);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Finds nearest star (within sep distance) to specified coordinates.
   *
   * @param mp Mapping parameters
   * @param sc Specified coordinates
   * @param sep On return, set at angular distance in radians from sc
   * @param str On return, contains text on object
   * @return Index of star, or -1
   */
  public int findNearestStar(MapParms mp, SphereCoords sc, double[] sep,
                             StringBuffer str) {
    float[] x = new float[1];    // dummy variable
    float[] y = new float[1];    // dummy variable
    double dtmp;
    double ra = sc.getRA();
    double dec = sc.getDec();
    int[] cns = new int[1],
          grk = new int[1], // Unused
          flm = new int[1];
    String byr;

    /* Reject stars farther than 1/8 inch */
    sep[0] = .125 * MapParms.pelsPerInch / mp.pelsPerRadian; // Radians

    short limMag100 = (short)getClipLimit100(mp.prefer);
    int close = -1;           // No close star found (yet)
    int n0 = -1;              // No close named star found (yet)
    String name0 = null, tmpname0;
    StarNameDB namedb = new StarNameDB();

    for ( int i = 0; i < num; i++ ) {
      if ( mag100[i] > limMag100 ) break;
      dtmp = MapParms.separation(StarDB.ra[i], StarDB.dec[i], ra, dec);
      if ( sep[0] > dtmp &&
           mp.rd2xyhit(StarDB.ra[i], StarDB.dec[i], x, y) == 1 ) {
        sep[0] = dtmp;
        close = i;

        tmpname0 = namedb.tellNameForStarIndex(i);
        if ( tmpname0 != null ) { n0 = i; name0 = tmpname0; }
      }
    }

    if ( close >= 0 ) {
      str.append(IdentifyDlg.TYPE + IdentifyDlg.STAR + "\n");

      String name = namedb.tellNameForStarIndex(close);
      if ( name != null ) {
        str.append(IdentifyDlg.NAME + name + "\n");
      }

      byr = getDesignations(close, cns, grk, flm);
      if ( cns[0] > 0 ) {
        str.append(IdentifyDlg.DESGN);

        String cnst = " " + Constellation.tellAbbr(cns[0] - 1);
        if ( byr != null ) {
          str.append(byr).append(cnst);
          if ( flm[0] > 0 )
            str.append(", ");
        }
        if ( flm[0] > 0 )
          str.append((int) flm[0]).append(cnst);
        str.append("\n");
      }

      /* If next to named star */
      if ( n0 >= 0 && n0 != close ) {
        dtmp = MapParms.separation(StarDB.ra[n0], StarDB.dec[n0],
                                   StarDB.ra[close], StarDB.dec[close]);
        if ( dtmp < .00175 &&                // If < .1 degree
             dtmp * mp.pelsPerRadian <= 3 )  // and <= 3 pels
          str.append(IdentifyDlg.NEARSTAR + name0 + "\n");
      }

      /* Append spectral info if available */
      String spect = tellSpectral(close);
      if ( ! spect.equals("") ) {
        str.append(IdentifyDlg.SPECT + spect + "\n");
      }

      /* Append magnitude info */
      str.append(IdentifyDlg.MAG + tellMagnitude(close) + "\n");
    }
    return close;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the star database.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    float a, b;
    boolean drawBayr, drawFlam;
    // To save time, I'll record labeled stars & their x,y positions
    float[] xlab = null, ylab = null;    // = null avoids error msg
    int[] ilab = null;                   // ditto
    int nlab = 0;                        // ditto
    String lbl;

    if ( !mp.isDrawing() ) return;

    /* Set up arrays, if needed, for Bayer and Flamsteed labels */
    drawBayr = mp.prefer.drawBayer();
    drawFlam = mp.prefer.drawFlamsteed();
    if ( drawBayr || drawFlam ) {
      xlab = new float[NUM_LAB];
      ylab = new float[NUM_LAB];
      ilab = new int[NUM_LAB];
      nlab = 0;
    }

    /* Set up magnitude clipping limit */
    short limMag100 = (short)getClipLimit100(mp.prefer);

    boolean bmpStars = mp.prefer.getBmpStars(); // Are we painting bmps?

    if ( !mp.printing && bmpStars ) { // If bmp painting to screen
      /* Get star images and the offset needed to position them */
      Image[] stars = starimages.getImages(mp.prefer.colorStar(),
                                           mp.prefer.colorBackGnd());
      float offset = StarImages.OFFSET - 0.5f; // 0.5 will make subsequent
                     // integer truncation a rounding operation

      // Star size = int(a + b * mag100)
      // See bottom of file for derivation of a and b
      b = (mp.prefer.getSzDim() - mp.prefer.getSzBright() - 1) /
          ((float)(limMag100 + 2 - mag100[0]));
      a = mp.prefer.getSzBright() + b * (1 - mag100[0]);

      /* Loop through stars */
      for ( int i = 0; i < num && mp.isDrawing(); i++ ) {
        if ( mag100[i] > limMag100 ) break;

        if ( mp.rd2xyhit(rx[i], ry[i], rz[i], x, y) > 0 ) {
          mp.g.drawImage(stars[(int)(a + b * mag100[i])],
                         (int)(x[0] - offset), (int)(y[0] - offset), null);

          if ( ((drawBayr && greek[i] != 0) || (drawFlam && flam[i] > 0)) &&
               nlab < NUM_LAB ) {
            xlab[nlab] = x[0];
            ylab[nlab] = y[0];
            ilab[nlab++] = i;
          }
        }
      }
    }
    else {                     // Else printing (or !bmpStars)
      Ellipse2D.Float circle = new Ellipse2D.Float();
      float s, offset;
      if ( !mp.printing ) {
        mp.g.setPaint(mp.prefer.colorStar());
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      } else
        mp.g.setPaint(mp.prefer.prclrStar());

      // Star size = a + b * mag100
      // The full magnitude range is maxmag100 - minmag100 (E.g. 700 - (-150))
      // The limited magnitude range is limmag100 - minmag100
      // size = szBright + (szDim - szBright) *
      //                   ((mag100 - minmag100) / (limmag100 - minmag100)
      //      = szBright for mag100 = minmag100 (= -150)
      //      = szDim    for mag100 = limmag100
      // (11 >= szBright >= szDim >= 1) ==> (11 >= size >= 1)
      // For szBright = 11 and szDim = 1, the following coefficients
      // for equation:  size = a + b * mag100
      // will yield a number between 1 and 11 (inclusive):
      b = (float)((mp.prefer.getSzDim() - mp.prefer.getSzBright()) /
          ((double)limMag100 - mag100[0]));      // minmag100 = mag100[0]
      a = mp.prefer.getSzBright() - b * mag100[0];
      // Let's trim this down a bit when printing
      if ( mp.printing ) {
        a /= 1.5;
        b /= 1.5;
      }

      /* Loop through stars */
      for ( int i = 0; i < num && mp.isDrawing(); i++ ) {
        if ( mag100[i] > limMag100 ) break;

        if ( mp.rd2xyhit(rx[i], ry[i], rz[i], x, y) > 0 ) {
          s = a + b * mag100[i];

          // To properly place the stars, need to right-shift & down-shift
          // by 0.5 when painting to screen (vs printing to paper),
          // not sure why...
          offset = s / 2 - (mp.printing ? 0.0f : 0.5f);

          circle.setFrame(x[0] - offset, y[0] - offset, s, s);
          mp.g.fill(circle);

          if ( ((drawBayr && greek[i] != 0) || (drawFlam && flam[i] > 0)) &&
               nlab < NUM_LAB ) {
            xlab[nlab] = x[0];
            ylab[nlab] = y[0];
            ilab[nlab++] = i;
          }
        }
      }

      // The following was used to compare placement of stars:
      // painting to screen vs printing to paper
      //if ( mp.printing ) circle.setFrame(29.0, 29.0, 2, 2);
      //else               circle.setFrame(29.5, 29.5, 2, 2);
      //mp.g.fill(circle);
      //mp.g.drawLine(22, 30, 27, 30);
      //mp.g.drawLine(33, 30, 38, 30);
      //mp.g.drawLine(30, 22, 30, 27);
      //mp.g.drawLine(30, 33, 30, 38);

      if ( !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /* Do star labels */
    if ( drawBayr || drawFlam ) {
      mp.g.setColor(mp.printing ? mp.prefer.prclrStarLabel() :
                                  mp.prefer.colorStarLabel());
      mp.g.setFont(mp.prefer.fontStarLabel());
      FontMetrics fm = mp.g.getFontMetrics();
      float yoffset = fm.getAscent() / 2.0f;
      float xoffset = 4;    // Same as star names for painting

      /* If printing, or if zoom sufficiently high to reduce number
         of labels, then have time to call ssllloowww function ...
         ... that trims out copies of star labels                   */
      if ( mp.printing || mp.prefer.getZoom() > 4 )
        trimStarLabels(mp, xlab, ylab, ilab, nlab, fm.getAscent() / 4);

      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      for ( int i = 0; i < nlab && mp.isDrawing(); i++ ) {
        if ( ilab[i] < 0 ) continue;  // Star was "trimmed"

        lbl = getDesignation(ilab[i], drawBayr, drawFlam);
        if ( lbl != null )
          mp.g.drawString(lbl, xlab[i] - fm.stringWidth(lbl) - xoffset,
                    ylab[i] + yoffset);
      }
      if ( mp.prefer.antialiasing && !mp.printing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Removes nearby copies of star labels that might overlay
   * and produce bolder print.  1st occurrence is kept since
   * succeeding copies represent dimmer stars.
   */
  private void trimStarLabels(MapParms mp, float[] xlab, float[] ylab,
                              int[] ilab, int n, int offset) {
    for ( int i = 0; i < n && mp.isDrawing(); i++ ) {
      if ( ilab[i] < 0 ) continue;

      for ( int j = i + 1; j < n && mp.isDrawing(); j++ ) {
        if ( ilab[j] < 0 ) continue;

        if ( con[ilab[i]] == con[ilab[j]] && greek[ilab[i]] == greek[ilab[j]] &&
             flam[ilab[i]] == flam[ilab[j]] && misc[ilab[i]] == misc[ilab[j]] &&
             (offset * offset) >
             ((xlab[i] - xlab[j]) * (xlab[i] - xlab[j]) +
              (ylab[i] - ylab[j]) * (ylab[i] - ylab[j])) )
          ilab[j] = -1;  // Disable this element
      }
    }
  }

  /* For testing */
  //public static void main(String args[]) {
  //  System.out.println("Reading DB...");
  //  StarDB stardb = new StarDB();
  //
  //  /* Report brightest stars */
  //  System.out.println("Brightest stars are:");
  //  for ( int i = 0; i < num; i++ ) {
  //    if ( mag100[i] > 100 ) break;
  //
  //    SphereCoords scoord = new SphereCoords(ra[i], dec[i]);
  //    String out = new String(scoord.tellRAHrMnT() + " " +
  //                            scoord.tellDecDgMn() + " " +
  //                            stardb.tellMagnitude(i) + " " +
  //                            Greek.tell3greek(greek[i]) + " " +
  //                            Constellation.tellAbbr(con[i]-1) + " " +
  //                            stardb.tellSpectral(i));
  //    System.out.println(out);
  //  }
  //  System.exit(0);
  //}
}

/*------------------------------------------------------------------------------

Deriving the size/magnitude formula for star images (on screen)
---------------------------------------------------------------

Lets say szBright = 7 and szDim = 4, corresponding to
minmag100 = -144 (Sirius) and limmag100 = 859 (corresponding
to maximum displayed magnitude of 8.5).  The equation
to hit both of these points is (using slope = slope):

  y - 7        4 - 7                     4 - 7
 -------  =  ---------   ==>   y = 7 + --------- (x + 144)
 x + 144     859 + 144                 859 + 144

Notice that when x = -144, y = 7, and when x = 859, y = 4.

The above equation hits both endpoints exactly, and rounding would
be used on y to get the size of the star.  However since x is
confined to -144 <= x <= 859, the range of x for a star size of
7, and the range of x for a star size of 4, are both half the range
of x for star sizes of 5 and 6.  This can be remedied by bumping
7 to 8, and then using truncation to derive y rather than x:

                    4 - 7 - 1
 y = trunc( 7 + 1 + --------- (x + 144) )
                    859 + 144

The equation needs to be tweaked so that the resulting y can never
be out of the range 4 <= y <= 7.  This can be done by adding 1 to
859 and subtracting 1 from -144.  The final equation, generalized, is:

                             szDim - szBright - 1
 y = trunc( szBright + 1 + ------------------------- (x - minmag100 + 1) )
                           limmag100 + 2 - minmag100

This yields:  x = -144, y = trunc(7.996), and x = 859, y = trunc(4.004).
The ranges of x for each star size are nearly identical.

- - -> Star images are based from 0, while size numbers are based
from 1.  Therefore:

                         szDim - szBright - 1
 y = trunc( szBright + ------------------------- (x - minmag100 + 1) )
                       limmag100 + 2 - minmag100

For y = trunc(a + b * x) we get:

       szDim - szBright - 1
 b = -------------------------
     limmag100 + 2 - minmag100

 a = szBright + b * (1 - minmag100)

------------------------------------------------------------------------------*/

