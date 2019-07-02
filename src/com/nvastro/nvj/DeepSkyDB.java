/*
 * DeepSkyDB.java  -  Deep sky object database and methods
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
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Helper class for DS object.
 *
 * @author Brian Simpson
 */
class DSObject {
  String name;
  String altname;
  String commonname;
  byte   type;        // 0 = no type specified
  byte   flag;        // If "S", mag is for showing only
  double ra;
  double dec;
  short  mag100;      // 0 = no mag specified
  String size;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public DSObject(String name, String altname, String commonname, byte type,
                  byte flag, double ra, double dec, short mag100, String size) {
    this.name = name;
    this.altname = altname;
    this.commonname = commonname;
    this.type = type;
    this.flag = flag;
    this.ra = ra;
    this.dec = dec;
    this.mag100 = mag100;         // Magnitude * 100
    this.size = size;
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Deep sky object database and methods.
 *
 * @author Brian Simpson
 */
public class DeepSkyDB {
  static Vector<DSObject> dsobjects = new Vector<DSObject>();  // Perhaps I
         // should just use arrays to store all of the DS data, but maybe, just
         // maybe, I might want to dynamically change the number of objects...
  /** Name of deep sky database file (nvdeepsky.txt) */
  static final public  String SOURCE = "nvdeepsky.txt";
  static final private String NOTHING = "";
  static final private String COMMA = ", ";
  //atic final private String DASH = " - ";
  static final private int    MAXERR= 10;
  static private boolean initialized = false;
  static private boolean empty = true; // DB initially empty
  static private int minmag100, maxmag100; // Limits of objects in DB
  static final private int MINMAG100 = -100; // -1st mag abs. minimum
  static final private int MAXMAG100 = 2000; // 20th mag abs. maximum
  static private File ExtFile = null;
  static private DecNumFormat mag_format;
  /* Note:  The following order must match that of DeepSkyImages.java */
  /*        It must also match the print images drawn below           */
  static final private String[] types = { "OB", "PN", "DN", "DK", "GC", "OC",
                                          "S2", "SG", "EG", "IG", "GA" };
  /* Note:  The following categorizes the above types as galaxies (0), */
  /* globular clusters (1), open clusters (2), planetary nebulas (3),  */
  /* diffuse nebulas (4), dark nebulas (5), and other (6)              */
  /* and must be kept in sync with what FindDSDlg expects              */
  static final private int[]   types2 = {   6,    3,    4,    5,    1,    2,
                                            6,    0,    0,    0,    0  };
  private DeepSkyImages dsimages;

  final static private double h2r = Math.PI / 12;
  final static private double d2r = Math.PI / 180;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public DeepSkyDB() {
    if ( initialized == false ) {
      init();
    }

    dsimages = new DeepSkyImages();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Reads deep sky database file and creates DB.  Called by Nvj during
   * program startup.
   */
  public static void init() {
    int i, j, k, errors = 0;
    byte type;
    byte[] flag    = new byte[1];
    short[] mag100 = new short[1];
    double[] ra    = new double[1];
    double[] dec   = new double[1];
    String line, name, altname, commonname, size;
    String[] field = new String[8];
    BufferedReader in = null;
    String fname = null;

    if ( initialized == false ) {
      if ( ExtFile == null )
        ExtFile = new File(Nvj.workingDir, SOURCE);

      try {                 // FileReader can throw FileNotFoundException
        if ( ExtFile.exists() ) {
          fname = ExtFile.getName();
          in = new BufferedReader(new FileReader(ExtFile));
        } else {
          fname = SOURCE;
          in = new BufferedReader(new InputStreamReader(
                   Nvj.class.getResourceAsStream("/com/nvastro/nvj/" + fname)));
        }

        while ( (line = in.readLine()) != null ) {  // Can throw IOException
          if ( (i = line.indexOf('#')) >= 0 ) line = line.substring(0, i);
          line = line.trim();
          if ( line.length() == 0 ) continue;

          /* Convert line to tokens */
          StringTokenizer t = new StringTokenizer(line, "|", true);
          k = t.countTokens();

          // Look for: field | field | field ..., but some fields may be missing
          for ( j = 0; j < 8; j++ ) field[j] = NOTHING;
          for ( i = 0, j = 0; i < k && j < 8; j++ ) { // j fields, i tokens
            field[j] = t.nextToken(); i++;
            // Was field blank and we picked up next '|'?
            if ( field[j].equals("|") ) { field[j] = NOTHING; }
            else if ( i++ < k ) t.nextToken();  // '|'
          }

          name = field[0].trim();
          if ( name.length() == 0 ) {
            if ( errors++ < MAXERR )
              ErrLogger.logError(ErrLogger.formatError(
                 TextBndl.getString("DSFile.NmErr"), fname, "  " + line));
            else if ( errors == MAXERR+1 ) ErrLogger.logError("...");
            continue;
          }
          if ( ! uniqueName(name) ) {
            if ( errors++ < MAXERR )
              ErrLogger.logError(ErrLogger.formatError(
                 TextBndl.getString("DSFile.Nm2Err"), fname, "  " + line));
            else if ( errors == MAXERR+1 ) ErrLogger.logError("...");
            continue;
          }

          altname = field[1].trim();

          type = convertType(field[3]);

          if ( ! convertCoordinates(field[4], ra, dec) ) {
            if ( errors++ < MAXERR )
              ErrLogger.logError(ErrLogger.formatError(
                 TextBndl.getString("DSFile.CrdErr"), fname, "  " + line));
            else if ( errors == MAXERR+1 ) ErrLogger.logError("...");
            continue;
          }

          if ( ! convertMagnitude(field[5], mag100, flag) ) {
            if ( errors++ < MAXERR )
              ErrLogger.logError(ErrLogger.formatError(
                 TextBndl.getString("DSFile.MagErr"), fname, "  " + line));
            else if ( errors == MAXERR+1 ) ErrLogger.logError("...");
            continue;
          }

          size = convertSize(field[6]);

          commonname = field[7].trim();

          dsobjects.addElement(new DSObject(name, altname, commonname, type,
                                   flag[0], ra[0], dec[0], mag100[0], size));
        }

        in.close();
      }
      catch ( Exception e ) {
        String msg = TextBndl.getString("DSFile.RdErr");
        ErrLogger.die(ErrLogger.formatError(msg, fname, null));
      }

      mag_format = new DecNumFormat("0.0");

      dsobjects.trimToSize();
      initialized = true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Reinitializes the DB with the selected file.  Called by LoadDSDlg.
   */
  public static void reInit(File file) {
    ExtFile = file;

    synchronized ( dsobjects ) { // Synchronize here and in draw to prevent
      // two threads from interfering with each other
      dsobjects.removeAllElements();
      initialized = false;
      empty = true;
      init();
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns true if name is unique.
   */
  static private boolean uniqueName(String name) {
    int i, j;

    j = dsobjects.size();
    for ( i = 0; i < j; i++ )
      if ( name.equals(((DSObject)dsobjects.elementAt(i)).name) ) return false;
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Convert type.
   */
  static private byte convertType(String type) {
    int i;

    if ( type.length() < 2 ) return 0;

    for ( i = 0; i < types.length; i++ ) {
      if ( type.substring(0, 2).toUpperCase().equals(types[i]) ) break;
    }
    if ( i < types.length ) return (byte)i;
    else return 0;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Convert coordinates.
   */
  static private boolean convertCoordinates(String coords, double[] ra,
                                            double[] dec) {
    /* Separate Right Ascension & Declination */
    StringTokenizer t = new StringTokenizer(coords, ",");
    if ( t.countTokens() != 2 ) return false;
    String ras = t.nextToken();
    String des = t.nextToken();

    try {
      /* Convert Right Ascension */
      double h, m, s;
      t = new StringTokenizer(ras, ":");
      if ( t.countTokens() != 2 && t.countTokens() != 3 ) return false;
      h = Double.valueOf(t.nextToken()).doubleValue();
      m = Double.valueOf(t.nextToken()).doubleValue();
      s = t.hasMoreTokens() ?
          Double.valueOf(t.nextToken()).doubleValue() : 0;
      if ( h < 0 || h > 23 || m < 0 || m >= 60 || s < 0 || s >= 60 )
        return false;
      ra[0] = (((s / 60) + m) / 60 + h) * h2r;  // radians

      /* Convert Declination */
      t = new StringTokenizer(des, ":");
      if ( t.countTokens() != 2 && t.countTokens() != 3 ) return false;
      h = Double.valueOf(t.nextToken()).doubleValue();
      m = Double.valueOf(t.nextToken()).doubleValue();
      s = t.hasMoreTokens() ?
          Double.valueOf(t.nextToken()).doubleValue() : 0;
      if ( h < -90 || h > 90 || m < 0 || m >= 60 || s < 0 || s >= 60 )
        return false;
      // Can't just test if h < 0 (e.g. des = -00:49)
      if ( des.indexOf('-') >= 0 ) { m *= -1; s*= -1; }
      dec[0] = ((s / 60) + m) / 60 + h;  // degrees
      if ( dec[0] > 90 || dec[0] < -90 ) return false;
      dec[0] *= d2r;                     // radians
    } catch(Exception e) { return false; }

    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Convert magnitude.  Returns true if successful.
   */
  static private boolean convertMagnitude(String mags, short[] mag100,
                                          byte[] flag) {
    mags = mags.trim();
    if ( mags.startsWith("U") ) {
      flag[0] = 'U';
      mags = mags.substring(1).trim();
    }
    else flag[0] = 0;

    if ( mags.length() == 0 ) { mag100[0] = 0; }   // OK
    else {
      try { mag100[0] = (short)(100 * Float.valueOf(mags).floatValue());
      } catch(Exception e) { mag100[0] = 0; return false; }
    }
    if ( empty ) {                       // If DB empty
      minmag100 = maxmag100 = mag100[0]; // Limits set at first object
      empty = false;
    }
    // If outside of reasonable limits, clamp it
    if ( mag100[0] > MAXMAG100 ) mag100[0] = MAXMAG100;
    if ( mag100[0] < MINMAG100 ) mag100[0] = MINMAG100;
    // If new extreme, record it
    if ( maxmag100 < mag100[0] ) maxmag100 = mag100[0];
    if ( minmag100 > mag100[0] ) minmag100 = mag100[0];
    return true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Convert size.
   */
  static private String convertSize(String size) {
    size = size.trim();
    int length = size.length();
    if ( length > 0 ) {
      if      ( size.endsWith("d") ) {
        size = size.substring(0, --length) + IdentifyDlg.Deg;
      }
      else if ( size.endsWith("m") ) {
        size = size.substring(0, --length) + IdentifyDlg.Min;
      }
      else if ( size.endsWith("s") ) {
        size = size.substring(0, --length) + IdentifyDlg.Sec;
      }
      else {
        size = size + IdentifyDlg.Min;
      }
    }
    return ( length > 0 ) ? size : NOTHING;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the number of deep sky objects.
   */
  public int getNumberOfObjects() {
    return dsobjects.size();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns (100X) the minimum magnitude of deep sky objects.
   */
  public int getMinMag100() {
    return empty ? 0 : minmag100;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns (100X) the maximum magnitude of deep sky objects.
   */
  public int getMaxMag100() {
    return empty ? 0 : maxmag100;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the deep sky object name.  E.g. "M42".
   *
   * @param i Index into deep sky database
   * @return "" if out of range
   */
  public String tellName(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return NOTHING;
    return ((DSObject)dsobjects.elementAt(i)).name;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the deep sky object name (and alt & common names if available).
   * E.g. "M42 - Orion Nebula".
   *
   * @param i Index into deep sky database
   * @return "" if out of range
   */
  public String tellName2(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return NOTHING;

    String name = ((DSObject)dsobjects.elementAt(i)).name;
    if ( ((DSObject)dsobjects.elementAt(i)).altname.length() != 0 ) {
      name += COMMA + ((DSObject)dsobjects.elementAt(i)).altname;
    }
    if ( ((DSObject)dsobjects.elementAt(i)).commonname.length() != 0 ) {
      //me += DASH + ((DSObject)dsobjects.elementAt(i)).commonname;
      name += COMMA + ((DSObject)dsobjects.elementAt(i)).commonname;
    }
    return name;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the deep sky object type.  E.g. "Diffuse nebula".
   *
   * @param i Index into deep sky database
   * @return "" if out of range
   */
  public String tellType(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return NOTHING;

    int j = ((DSObject)dsobjects.elementAt(i)).type;
    return TextBndl.getString("DS." + types[j]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the deep sky object general type.  E.g. 1 (for nebulas)
   *
   * @param i Index into deep sky database
   * @return 0 if out of range (for unspecified)
   */
  public int getGenType(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return 0;

    return types2[((DSObject)dsobjects.elementAt(i)).type];
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns deep sky object's right ascension.
   *
   * @param i Index into deep sky database
   * @return Right ascension in radians
   */
  public double getRARad(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return 0;
    return ((DSObject)dsobjects.elementAt(i)).ra;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns deep sky object's declination.
   *
   * @param i Index into deep sky database
   * @return Declination in radians
   */
  public double getDecRad(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return 0;
    return ((DSObject)dsobjects.elementAt(i)).dec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the J2000 location.
   * Called by SkyObject.
   *
   * @param i Index into deep sky database
   * @return Coordinates of object (containing RA/Dec in radians)
   */
  public SphereCoords getJ2000Location(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return new SphereCoords(0.0, 0.0);
    return new SphereCoords(getRARad(i), getDecRad(i));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the apparent location.
   * Called by SkyObject.
   *
   * @param i Index into deep sky database
   * @param mp Mapping parameters
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   * @return Coordinates of object (containing RA/Dec in radians)
   */
  public SphereCoords getAppLocation(int i, MapParms mp,
                                     SphereCoords J2000Coords) {
    if ( i < 0 || i >= dsobjects.size() ) return new SphereCoords(0.0, 0.0);
    SphereCoords sc = new SphereCoords(getRARad(i), getDecRad(i));
    if ( J2000Coords != null ) J2000Coords.set(sc);
    mp.precessNutate(sc);
    mp.adjustEquatForAberration(sc);
    return sc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the magnitude.
   *
   * @param i Index into deep sky database
   * @return Magnitude multiplied by 100 (so (short) integer can be used)
   */
  public short getMag100(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return 0;
    return ((DSObject)dsobjects.elementAt(i)).mag100;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the magnitude.
   *
   * @param i Index into deep sky database
   * @return Magnitude as a String
   */
  public String tellMag(int i) {
    if ( i < 0 || i >= dsobjects.size() ) return NOTHING;
    short m = getMag100(i);
    if ( m == 0 || ((DSObject)dsobjects.elementAt(i)).flag != 0 )
      return NOTHING;
    return mag_format.format(m / 100.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates if object is viewable based on magnitude.  (No bounds checking.)
   *
   * @param i Index into database
   * @param pref User preferences
   * @return True if viewable, false if not
   */
  public boolean isViewable(int i, Preferences pref) {
    return pref.drawDeepSky() &&
           ((DSObject)dsobjects.elementAt(i)).mag100 <= getClipLimit100(pref);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Objects shown have magnitudes up to and including this limit
   */
  private int getClipLimit100(Preferences pref) {
    // If mp.prefer.getZiDSLimMag10 returns 81, allow objects up to 8.19 (819)
    int ziMag100 = pref.getZiDSLimMag10() * 10 + 9;
    int zoMag100 = pref.getZoDSLimMag10() * 10;

    // getZoomFactor goes from 0.0 to 1.0 as user zooms in
    return (int)(pref.getZoomFactor() * (ziMag100 - zoMag100) + zoMag100);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Finds nearest deep sky object to specified coordinates.
   *
   * @param mp Mapping parameters
   * @param sc Specified coordinates
   * @param sep On return, set at angular distance in radians from sc
   * @param str On return, contains text on object
   * @return Index of object, or -1
   */
  public int findNearestDS(MapParms mp, SphereCoords sc, double[] sep,
                           StringBuffer str) {
    float[] x = new float[1];    // dummy variable
    float[] y = new float[1];    // dummy variable
    double dtmp;
    double ra = sc.getRA();
    double dec = sc.getDec();
    DSObject object;

    int close = -1;                       // No close object found (yet)
    if ( ! mp.prefer.drawDeepSky() ) return close;
    short limMag100 = (short)getClipLimit100(mp.prefer);

    /* Deepsky objects are represented by a 9x9 bitmap */
    /* Reject objects farther than 4 pels + 1/16 inch (= .0625) */
    sep[0] = (4 + .0625 * MapParms.pelsPerInch) / mp.pelsPerRadian; // Radians

    int k = dsobjects.size();
    for ( int i = 0; i < k; i++ ) {
      object = (DSObject)dsobjects.elementAt(i);
      if ( object.mag100 > limMag100 ) continue;
      dtmp = MapParms.separation(object.ra, object.dec, ra, dec);
      if ( sep[0] > dtmp &&
           mp.rd2xyhit(object.ra, object.dec, x, y) == 1 ) {
        sep[0] = dtmp;
        close = i;
      }
    }

    if ( close >= 0 ) {
      str.append(IdentifyDlg.TYPE + tellType(close) + "\n");

      object = (DSObject)dsobjects.elementAt(close);
      str.append(IdentifyDlg.NAME + /* object.name */ tellName2(close) + "\n");

      String mag = tellMag(close);
      if ( ! mag.equals("") ) {
        str.append(IdentifyDlg.MAG + mag + "\n");
      }

      String size = ((DSObject)dsobjects.elementAt(close)).size;
      if ( ! size.equals("") ) {
        str.append(IdentifyDlg.ANGSZ + size + "\n");
      }
    }
    return close;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the deep sky objects.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    synchronized ( dsobjects ) { // This method is likely called by the 2nd
    // thread; others are called by the GUI thread; use synchronized
    // on this method and the method for changing/loading dsobjects
    int k = dsobjects.size();
    float[] x = new float[1];
    float[] y = new float[1];
    DSObject object;

    if ( !mp.prefer.drawDeepSky() || !mp.isDrawing() ) return;
    boolean names = mp.prefer.drawDeepSkyNames();

    mp.g.setColor(mp.printing ? mp.prefer.prclrDeepSky() :
                                mp.prefer.colorDeepSky());
    mp.g.setFont(mp.prefer.fontDeepSky());
    FontMetrics fm = mp.g.getFontMetrics();
    int yoffset = fm.getAscent() / 2;

    Image[] images = dsimages.getImages(mp.prefer.colorDeepSky(),
                                        mp.prefer.colorBackGnd());
    float offset = DeepSkyImages.OFFSET - 0.5f;  // 0.5 will make subsequent
                   // integer truncation a rounding operation
    int xoffset = DeepSkyImages.OFFSET * 3 / 2;

    /* Set up magnitude clipping limit */
    short limMag100 = (short)getClipLimit100(mp.prefer);

    if ( !mp.printing ) {               // If painting screen
      if ( mp.prefer.antialiasing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
      for ( int i = 0; i < k && mp.isDrawing(); i++ ) {
        object = (DSObject)dsobjects.elementAt(i);
        if ( object.mag100 > limMag100 ) continue;
        if ( mp.rd2xyhit(object.ra, object.dec, x, y) > 0 ) {
          mp.g.drawImage(images[object.type],
                         (int)(x[0] - offset), (int)(y[0] - offset), null);

          if ( names )
            mp.g.drawString(object.name, x[0] + xoffset, y[0] + yoffset);
        }
      }
      if ( mp.prefer.antialiasing )
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    else {                              // Else printing
      Ellipse2D.Float ellipse = new Ellipse2D.Float();
      Rectangle2D.Float rect = new Rectangle2D.Float();
      Line2D.Float line = new Line2D.Float();

      for ( int i = 0; i < k && mp.isDrawing(); i++ ) {
        object = (DSObject)dsobjects.elementAt(i);
        if ( object.mag100 > limMag100 ) continue;
        if ( mp.rd2xyhit(object.ra, object.dec, x, y) > 0 ) {
          switch ( object.type ) {

           /* This section must match "types" (static variable above) */

           case 1:      // "PN" Planetary nebula
            ellipse.setFrame(x[0] - 1.6f, y[0] - 1.6f, 3.2f, 3.2f);
            mp.g.draw(ellipse);
            line.setLine(x[0] - 3.2f, y[0], x[0] - 1.6f, y[0]);
            mp.g.draw(line);
            line.setLine(x[0] + 1.6f, y[0], x[0] + 3.2f, y[0]);
            mp.g.draw(line);
            line.setLine(x[0], y[0] - 3.2f, x[0], y[0] - 1.6f);
            mp.g.draw(line);
            line.setLine(x[0], y[0] + 1.6f, x[0], y[0] + 3.2f);
            mp.g.draw(line);
            xoffset = 6;
            break;

           case 2:      // "DN" Diffuse nebula
            rect.setRect(x[0] - 2.8f, y[0] - 2.8f, 5.6f, 5.6f);
            mp.g.draw(rect);
            xoffset = 6;
            break;

           case 3:      // "DK" Dark nebula
            line.setLine(x[0] - 2.8f, y[0] - 2.8f, x[0] + 2.8f, y[0] + 2.8f);
            mp.g.draw(line);
            line.setLine(x[0] - 2.8f, y[0] + 2.8f, x[0] + 2.8f, y[0] - 2.8f);
            mp.g.draw(line);
            xoffset = 6;
            break;

           case 4:      // "GC" Globular cluster
            ellipse.setFrame(x[0] - 3.04f, y[0] - 3.04f, 6.08f, 6.08f);
            mp.g.draw(ellipse);
            line.setLine(x[0] - 3.04f, y[0], x[0] + 3.04f, y[0]);
            mp.g.draw(line);
            line.setLine(x[0], y[0] - 3.04f, x[0], y[0] + 3.04f);
            mp.g.draw(line);
            xoffset = 6;
            break;

           case 5:      // "OC" Open cluster
            line.setLine(x[0] - 0.4f, y[0] - 3.12f, x[0] + 0.4f, y[0] - 3.12f);
            mp.g.draw(line);
            line.setLine(x[0] - 0.4f, y[0] + 3.12f, x[0] + 0.4f, y[0] + 3.12f);
            mp.g.draw(line);
            line.setLine(x[0] - 3.12f, y[0] - 0.4f, x[0] - 3.12f, y[0] + 0.4f);
            mp.g.draw(line);
            line.setLine(x[0] + 3.12f, y[0] - 0.4f, x[0] + 3.12f, y[0] + 0.4f);
            mp.g.draw(line);
            line.setLine(x[0] - 2.56f, y[0] - 2.0f, x[0] - 2.0f, y[0] - 2.56f);
            mp.g.draw(line);
            line.setLine(x[0] + 2.56f, y[0] + 2.0f, x[0] + 2.0f, y[0] + 2.56f);
            mp.g.draw(line);
            line.setLine(x[0] - 2.56f, y[0] + 2.0f, x[0] - 2.0f, y[0] + 2.56f);
            mp.g.draw(line);
            line.setLine(x[0] + 2.56f, y[0] - 2.0f, x[0] + 2.0f, y[0] - 2.56f);
            mp.g.draw(line);
            xoffset = 6;
            break;

           case 6:      // "S2" Double star
            line.setLine(x[0] - 2.0f, y[0] - 2.0f, x[0] + 2.0f, y[0] + 2.0f);
            mp.g.draw(line);
            line.setLine(x[0] - 2.0f, y[0] + 2.0f, x[0] + 2.0f, y[0] - 2.0f);
            mp.g.draw(line);
            xoffset = 5;
            break;

           case 7:      // "SG" Spiral galaxy
           case 8:      // "EG" Elliptical galaxy
           case 9:      // "IG" Irregular galaxy
           case 10:     // "GA" Generic galaxy
            ellipse.setFrame(x[0] - 4.8f, y[0] - 2.4f, 9.6f, 4.8f);
            mp.g.draw(ellipse);
            xoffset = 7;
            break;

           default:     // 0 or anything else, generic object
            ellipse.setFrame(x[0] - 3.2f, y[0] - 3.2f, 6.4f, 6.4f);
            mp.g.draw(ellipse);
            xoffset = 6;
            break;
          }

          if ( names ) {
            mp.g.drawString(object.name, (int)(x[0] + xoffset),
                                         (int)(y[0] + yoffset));
          }
        }
      }
    }
    } // End synchronized
  }

  /* For testing */
  //static public void main(String[] args) {
  //  DeepSkyDB db = new DeepSkyDB();
  //  for ( int i = 0; i < dsobjects.size(); i++ ) {
  //    System.out.println(db.tellName(i) + " " +
  //                       db.tellType(i) + " " + db.tellMag(i) + " " +
  //                       db.getRARad(i)*12/Math.PI + " " +
  //                       db.getDecRad(i)*180/Math.PI + " " +
  //                       ((DSObject)dsobjects.elementAt(i)).commonname);
  //  }
  //}
}

