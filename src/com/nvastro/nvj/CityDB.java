/*
 * CityDB.java  -  City database and methods
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.swing.JOptionPane;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * City database and methods.
 *
 * @author Brian Simpson
 */
public class CityDB {
  /** Name of city database file (nvlocations.txt) */
  static public final String SOURCE = "nvlocations.txt";
  static private Vector<Location> cities = new Vector<Location>();
  static private int num = 0;
  static private boolean initialized = false;
  //#------ Location name --------|--Long-|--Lat-|-Timezone--------
  //Addis Ababa, Ethiopia            38:47   9:00 Africa/Addis_Ababa
  //Adelaide, Australia             138:35 -34:54 Australia/Adelaide
  //Anchorage, Alaska              -149:53  61:13 America/Anchorage
  //North Pole                        0:00  90:00 0:00
  // 1 based:  ' ' must occur at columns 31, 39, and 46
  // 0 based:  ' ' must occur at columns 30, 38, and 45
  // Line length must be >= 47
  // Name width = 30, Long = 7, Lat = 6
  //
  // Note:  If "Location name" field is widened, need to make modifications
  //        here, in Text.properties ("LocationDlg.List"), in nvlocations.txt,
  //        and in LocationDlg.java.

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public CityDB() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Reads city database file.  Called by Nvj during program startup.
   */
  public static void init() {
    if ( initialized == false ) {
      File source = new File(Nvj.workingDir, SOURCE);
      BufferedReader in = null;
      String line;
      Location loc;

      try {                 // FileReader can throw FileNotFoundException
        if ( source.exists() )
          in = new BufferedReader(new FileReader(source));
        else
          in = new BufferedReader(new InputStreamReader(
                   Nvj.class.getResourceAsStream("/com/nvastro/nvj/" +SOURCE)));

        while ( (line = in.readLine()) != null ) {  // Can throw IOException
          /* line will not contain \r or \n or \0 */
          if ( line.length() == 0 || line.charAt(0) == '#' ) continue;
          if ( line.trim().length() == 0 ) continue;

          if ( line.length() >= 47 && line.charAt(30) == ' ' &&
               line.charAt(38) == ' ' && line.charAt(45) == ' ' &&
               line.substring(0, 30).trim().length() > 0 ) {
            try {
              loc = new Location(line.substring(0, 30),
                                 line.substring(31, 38),
                                 line.substring(39, 45),
                                 line.substring(46));
              cities.addElement(loc);
              num++; // Or: if ( ++num >= 10000 ) break;
              continue;
            }
            catch ( Exception e ) { }
          }
          ErrLogger.logError(ErrLogger.formatError(
                    TextBndl.getString("LocDB.Reject"), SOURCE, "  " + line));
        }

        in.close();
      }
      catch ( Exception e ) {
        String msg = ErrLogger.formatError(TextBndl.getString("LocDB.RdErr"),
                     SOURCE, null);
        ErrLogger.logError(msg);
        OptionDlg.showMessageDialog(Nvj.parentFrame, msg, Nvj.PgmName,
                                    JOptionPane.ERROR_MESSAGE);

        cities.removeAllElements();
        num = 0;
      }
      finally {
        if ( in != null ) try { in.close(); } catch(IOException e) { ; }
      }

      cities.trimToSize();
      initialized = true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns number of cities.
   */
  public int getNumberOfCities() {
    return num;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a string containing the original untrimmed city name along with
   * longitude, latitude, and timezone information for the specified index.
   *
   * @param i Index into city database
   * @return "" If i out of range
   */
  public String tellAll(int i) {
    if ( i < 0 || i >= num ) return "";
    Location loc = (Location)cities.elementAt(i);
    String lo = pad(loc.tellLong(), 8);
    String la = pad(loc.tellLat(), 7);
    String tz = pad(loc.tellTZOffset(), 7);
    return (loc.tellCity2() + lo + la + tz); // Assumes tellCity2() rts 30 chars
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Prepends blanks until string is desired length.
   */
  private String pad(String s, int p) {
    StringBuffer sb = new StringBuffer(s);
    while ( sb.length() < p ) sb.insert(0, ' ');
    return sb.toString();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city name.
   *
   * @param i Index into city database
   * @return "" If i out of range
   */
  public String tellName(int i) {
    if ( i < 0 || i >= num ) return "";
    return ((Location)cities.elementAt(i)).tellCity();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location as a string ([-][DD]D:MM).
   *
   * @param i Index into city database
   * @return "" If i out of range
   */
  public String tellLong(int i) {
    if ( i < 0 || i >= num ) return "";
    return ((Location)cities.elementAt(i)).tellLong();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location in degrees.
   */
  public double getLongDeg(int i) {
    if ( i < 0 || i >= num ) return 0;
    return ((Location)cities.elementAt(i)).getLongDeg();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location as a string ([-][D]D:MM).
   *
   * @param i Index into city database
   * @return "" If i out of range
   */
  public String tellLat(int i) {
    if ( i < 0 || i >= num ) return "";
    return ((Location)cities.elementAt(i)).tellLat();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location in degrees.
   */
  public double getLatDeg(int i) {
    if ( i < 0 || i >= num ) return 0;
    return ((Location)cities.elementAt(i)).getLatDeg();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich (as [-][H]H:MM).
   * It is negative for west of Greenwich, and ignores DST.
   *
   * @param i Index into city database
   * @return "" If i out of range
   */
  public String tellTZOffset(int i) {
    if ( i < 0 || i >= num ) return "";
    return ((Location)cities.elementAt(i)).tellTZOffset();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich in minutes.
   * It is negative for west of Greenwich, and ignores DST.
   */
  public double getTZOffsetMin(int i) {
    if ( i < 0 || i >= num ) return 0;
    return ((Location)cities.elementAt(i)).getTZOffsetMin();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a Location object for the specified index.
   *
   * @param i Index into city database
   * @return null If i out of range
   */
  public Location getLocation(int i) {
    if ( i < 0 || i >= num ) return null;
    return ((Location)cities.elementAt(i));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a Location object for the specified city (string is trimmed).
   *
   * @param city City to look for
   * @return null If city not found
   */
  public Location getLocationForCity(String city) {
    int i;

    if ( city == null ) return null;
    String ct = city.trim();

    for ( i = 0; i < num; i++ )
      if ( ((Location)cities.elementAt(i)).tellCity().equals(ct) ) break;

    if ( i < num ) return ((Location)cities.elementAt(i));
    else           return null;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates if Location's TimeZone auto-handles DST.  (This does not mean
   * that the TimeZone does DST, only that it has correct DST information
   * for the locale, and will do DST if applicable.)  This is equivalent
   * to saying that this object was created with a valid (JVM recognized)
   * timezone string.
   *
   * @param i Index into city database
   */
  public boolean handlesDST(int i) {
    if ( i < 0 || i >= num ) return false;
    return ((Location)cities.elementAt(i)).handlesDST();
  }

  /* For testing */
  //public static void main(String args[]) {
  //  System.out.println("Reading cities...");
  //  CityDB citydb = new CityDB();
  //  int num = citydb.getNumberOfCities();
  //  System.out.println("The " + num + " cities are:");
  //  for ( int i = 0; i < num; i++ ) {
  //    System.out.println(citydb.tellAll(i));
  //  }
  //}
}

