/*
 * Location.java  -  This class represents the observer's location
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

import java.util.HashMap;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.TimeZone;


/* Note:  Some good info on timezones, dst, and calendars at
 * http://www.afu.com/javafaq.htm  ->  Computer Dating
 */


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * This class represents the observer's location.  It
 * contains name, longitude, latitude, and time zone
 * and DST information.
 *
 * Note:  Objects of this class are immutable.
 * (I.e. there are no set functions.)
 * Thus the LST clone function does not have to clone Location.
 *
 * @author Brian Simpson
 */
public class Location implements Cloneable {
  // Must declare "implements Cloneable" since clone() method calls
  // super.clone() which requires this.  Otherwise will get
  // CloneNotSupportedException thrown.
  static final private String ANGSEP = TextBndl.getAngSep(); // ":"
  static final private String TMSEP  = TextBndl.getTmSep();  // ":"
  static final private String LAT = TextBndl.getString("Pgm.St.Lat") + " ";
  static final private String LNG = ", "+ TextBndl.getString("Pgm.St.Long")+" ";
  static final private char DP = TextBndl.getDPChar();    // '.'
  static final private char MN = TextBndl.getMinusChar(); // '-'
  static private String ANGSEP2; // 1 or 2 choices for angular separator
  static private String TMSEP2;  // 1 or 2 choices for time separator
  static private String[] tzstrings;
  static private HashMap<String, Integer> hash;
  private String city;
  private double lat, lon;        // In Degrees
  private int tz_offset;          // In Minutes, negative for W of Greenwich
  private TimeZone tz = null;
  private String tzs;             // Timezone string from city file
  private boolean handledst;      // True means auto-handles DST, does not mean
                                  // it will do DST (i.e. Phoenix)

  /*----------------------------------------------------------------------------
   * Setup timezones...
   * and ANGSEP2 and TMSEP2...
   */
  static {
    tzstrings = TimeZone.getAvailableIDs();
    hash = new HashMap<String, Integer>(tzstrings.length + 1, 1.0f);// Hopefully
                                                                // avoids rehash
    for ( int i = 0; i < tzstrings.length; i++ ) {
      if ( tzstrings[i].indexOf('/') > 0 ) // If string/string
        hash.put(tzstrings[i],             // Associate string with
                 new Integer(TimeZone.getTimeZone(tzstrings[i]).getRawOffset() /
                             60000));      // number of minutes from GMT
    }

    /* If a locale has an angular separator that is different from ':',
       then create a string with both chars for use in parsing, since
       city file is likely to use ':', while typed-in chars are likely
       to use the local form */
    ANGSEP2 = TextBndl.getAngSep(); // ":"
    if ( !ANGSEP2.equals(":") ) ANGSEP2 = ANGSEP2.concat(":");

    /* Similar for time separator */
    TMSEP2 = TextBndl.getTmSep(); // ":"
    if ( !TMSEP2.equals(":") ) TMSEP2 = TMSEP2.concat(":");
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.  Currently the exceptions thrown are not meant
   * to be presented to the user.
   *
   * @param ci Can be null.  If non-null, it must have non-blanks
   * @param lo Longitude in degrees:minutes (-180:00 &lt;= lo &lt;= 180:00)
   * @param la Latitude in degrees:minutes (-90:00 &lt;= la &lt;= 90:00)
   * @param zn Timezone in hours:minutes (-13:00 &lt;= zn &lt;= 13:00)
   *           or zn must be a valid JVM TimeZone string
   */
  public Location(String ci, String lo, String la, String zn) {
    double[] dlo = new double[1], dla = new double[1], dzn = new double[1];

    if ( !convertLong(lo, dlo) )
      throw new IllegalArgumentException("> +/-180");
    if ( !convertLat(la, dla) )
      throw new IllegalArgumentException("> +/-90");

    if ( zn == null || zn.trim().length() == 0 )
      throw new IllegalArgumentException("Bad tz");
    char z = zn.trim().charAt(0);
    if ( z == '-' || z == MN || Character.isDigit(z) ) {
      if ( !convertTZ(zn, dzn) )
        throw new IllegalArgumentException("> +/-13");
      else
        construct(ci, dlo[0], dla[0], dzn[0], null);
      return;
    }

    construct(ci, dlo[0], dla[0], 0.0, zn);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.  Currently the exceptions thrown are not meant
   * to be presented to the user.
.  *
   * @param ci Can be null.  If non-null, it must have non-blanks
   * @param lo Longitude in degrees (-180 &lt;= lo &lt;= 180)
   * @param la Latitude in degrees (-90 &lt;= la &lt;= 90)
   * @param zn Timezone in hours (-13 &lt;= zn &lt;= 13)
   */
  public Location(String ci, double lo, double la, double zn) {
    if ( lo < -180 || lo > 180 )
      throw new IllegalArgumentException("> +/-180");
    if ( la < -90 || la > 90 )
      throw new IllegalArgumentException("> +/-90");
    if ( zn < -13 || zn > 13 )
      throw new IllegalArgumentException("> +/-13");

    construct(ci, lo, la, zn, null);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor helper function.
   */
  private void construct(String ci, double lo, double la, double zn, String ds){
    city = ci;     // null OK
    if ( ci != null && ci.trim().length() == 0 )
      throw new IllegalArgumentException("Blank city");
    lon = lo;
    lat = la;
    zn *= 60;                // Hours to minutes
    // (int)2.99  ->  2, (int)3.01  ->  3
    // (int)-2.99 -> -2, (int)-3.01 -> -3
    if ( zn > 0 ) zn += .01; // Prevent problems with int truncation
    if ( zn < 0 ) zn -= .01; // Prevent problems with int truncation
    tz_offset = (int)zn;     // Minutes

    /* Create a timezone based on the timezone string */
    if ( ds != null ) {                        // If non null
      tzs = ds.trim();
      if ( tzs.length() > 0 ) {                // If non blank
        if ( !hash.containsKey(tzs) ) {        // If string not recognized
          ErrLogger.logError(ErrLogger.formatError(
                             TextBndl.getString("LocDB.UnknownTZ") + "  ",
                             CityDB.SOURCE, tzs));
          throw new IllegalArgumentException("Bad timezone");
        }
        else {                                 // Else good timezone string
          tz = TimeZone.getTimeZone(tzs);      // Use it to get TimeZone
          handledst = true;                    // Handles DST automatically

          tz_offset = tz.getRawOffset() / 60000;   // Update tz_offset
        }
      }
    }

    /* If unsuccessful (or no string), create a dft timezone */
    if ( tz == null ) {
      tz = new SimpleTimeZone(tz_offset * 60000, "");  // No DST
      handledst = false;                       // Does not handle auto DST
      tzs = null;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a copy of the Location object.
   */
  public Object clone() {
    Location l = null;
    try {
      l = (Location) super.clone();
    } catch ( CloneNotSupportedException e ) { /* Should never happen */
      ErrLogger.die(e.toString());        // toString() uses getMessage()
    }
    return l;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the (trimmed) city string, or if not set, "".
   */
  public String tellCity() {
    if ( city == null ) return "";
    return city.trim();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the original untrimmed city string, or if not set, "".
   */
  public String tellCity2() {
    if ( city == null ) return "";
    return city;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude and longitude.
   */
  public String tellCoordinates() {
    return new String(LAT + tellLat() + LNG + tellLong());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city string, or if not set, the latitude and longitude.
   */
  public String tellLocation() {
    if ( city != null && !city.equals("") ) return city.trim();

    return tellCoordinates();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns d as [-]degrees:minutes.
   */
  private String tellD(double d) {
    String sign;

    if ( d < 0 ) { d *= -1; sign = String.valueOf(MN); }
    else                  { sign = "";  }

    int i = (int)Math.floor(d);
    d = (d - i) * 60;
    int j = (int)Math.round(d);
    if ( j == 60 ) {
      j = 0; i++;
    }                            // ":"
    return new String(sign + i + ANGSEP + ((j < 10) ? "0" : "" ) + j);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location in degrees.
   */
  public double getLongDeg() { return lon; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location as a string ([-][DD]D:MM).
   */
  public String tellLong() { return tellD(lon); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location in degrees.
   */
  public double getLatDeg() { return lat; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location as a string ([-][D]D:MM).
   */
  public String tellLat() { return tellD(lat); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset in minutes relative to Greenwich.
   * It is negative for west of Greenwich, and ignores DST.
   */
  public int getTZOffsetMin() { return tz_offset; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich (as [-][H]H:MM).
   * It is negative for west of Greenwich, and ignores DST.
   */
  public String tellTZOffset() {
    int h = tz_offset / 60;
    int m = Math.abs(tz_offset % 60);
    return new String(h + TMSEP + ((m < 10) ? "0" : "" ) + m);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the TimeZone object associated with this Location.
   */
  public TimeZone getTZ() { return tz; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates if enclosed TimeZone auto-handles DST.  (This does not mean
   * that the TimeZone does DST, only that it has correct DST information
   * for the locale, and will do DST if applicable.)  This is equivalent
   * to saying that this object was created with a valid (JVM recognized)
   * timezone string.
   *
   * @return True if DST auto-handled, otherwise false.
   */
  public boolean handlesDST() {
    return handledst;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts a string ([+-][DD]D:MM) to longitude (in degrees (+/- 180 max)).
   *
   * @param s String to convert
   * @param d Value of s on return
   * @return True for successful conversion, otherwise false.
   */
  public static boolean convertLong(String s, double[] d) {
    return convert(s, d, 180, ANGSEP2);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts a string ([+-][D]D:MM) to latitude (in degrees (+/- 90 max)).
   *
   * @param s String to convert
   * @param d Value of s on return
   * @return True for successful conversion, otherwise false.
   */
  public static boolean convertLat(String s, double[] d) {
    return convert(s, d, 90, ANGSEP2);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts a string ([+-][H]H:MM) to hours (+/- 13 max).
   *
   * @param s String to convert
   * @param d Value of s on return
   * @return True for successful conversion, otherwise false.
   */
  public static boolean convertTZ(String s, double[] d) {
    return convert(s, d, 13, TMSEP2);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns true for successful conversion, otherwise false.
   * @param s String to parse
   * @param d Value of s on return
   * @param limit Limiting absolute value (i.e. must be positive)
   * @param sep Separation string
   */
  private static boolean convert(String s, double[] d, int limit, String sep) {
    double h, m;
    int sign = 1;

    if ( s == null ) return false;
    s = s.trim();
    if ( s.length() == 0 ) return false;
    if ( s.charAt(0) == '-' || s.charAt(0) == MN ) {
      s = s.substring(1);
      sign = -1;
    }
    if ( s.indexOf('-') >= 0 || s.indexOf(MN) >= 0 ||
         s.indexOf('.') >= 0 || s.indexOf(DP) >= 0 ) return false;

    StringTokenizer t = new StringTokenizer(s, sep, true);
    if ( t.countTokens() != 3 ) return false;
    try {
      h = Double.valueOf(t.nextToken()).doubleValue(); // "abc" throws exception
      t.nextToken();  // Throw away sep
      m = Double.valueOf(t.nextToken()).doubleValue(); // "abc" throws exception
    } catch(Exception e) { return false; }
    if ( h > limit || (h == limit && m != 0) || m >= 60 ) return false;
    // Minus sign already stripped, so don't have to worry
    // about cases like s = -00:49 (h = 0, m > 0)
    d[0] = (m / 60 + h) * sign;

    return true;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  for ( int i = 0; i < tzstrings.length; i++ )
  //    if ( hash.containsKey(tzstrings[i]) )
  //      System.out.println(tzstrings[i] + ", " + hash.get(tzstrings[i]));
  //}
}

