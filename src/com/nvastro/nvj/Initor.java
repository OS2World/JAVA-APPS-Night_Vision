/*
 * Initor.java  -  Ini file reader and writer
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

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JOptionPane;
// Color


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Ini file reader and writer.  All static methods.
 *
 * @author Brian Simpson
 */
public class Initor {
  static final public String INIFILE = "nvj.ini";
  static private File inifile;
  static private TreeMap<String, String> initable;
  // There appears to be 3 different candidates for a hash table:
  //   Hashtable  -  Internally synchronized
  //   HashMap    -  Not internally synchronized
  //   TreeMap    -  Not internally synchronized
  // HashMap and TreeMap may be externally synchronized if needed for
  // a multi-threaded environment.  However, since only my main thread
  // will ever be using this class, synchronization is not needed.
  // TreeMap was chosen since it can easily produce an alphabetical
  // arrangement of keys.
  static private String hdrtext = "";
  static private boolean initialized = false;
  static private boolean noIni = false;

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private Initor() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialization.
   */
  public static void init() {
    int i;
    String line, key, value;

    if ( initialized == false ) {
      /* Setup File */
      inifile = new File(Nvj.iniDir, INIFILE);

      /* Get header text */
      String hdr = TextBndl.getString("IniFile.Hdr");
      StringTokenizer t = new StringTokenizer(hdr, "\n");
      while ( t.hasMoreTokens() )
        hdrtext = hdrtext + "# " + t.nextToken() + "\n";

      initable = new TreeMap<String, String>();

      /* Load up Hashtable from file */
      try {                 // FileReader can throw FileNotFoundException
        BufferedReader in = new BufferedReader(new FileReader(inifile));

        while ( (line = in.readLine()) != null ) {  // Can throw IOException
          if ( (i = line.indexOf('#')) >= 0 ) line = line.substring(0, i);
          line = line.trim();
          if ( line.length() == 0 ) continue;

          /* Look for "key = value" */
          if ( (i = line.indexOf('=')) >= 0 ) {
            key = line.substring(0, i).trim();
            value = line.substring(i+1).trim();  // trim() does spaces and tabs
            if ( key.length() > 0 && value.length() > 0 )
              initable.put(key, value);
          }
        }

        in.close();
      }
      catch ( Exception e ) {
        // If a file exists but is unreadable, can get FileNotFoundException
        // instead of an IOException, so discern difference here...
        if ( inifile.exists() ) {
          String msg;
          msg = ErrLogger.formatError(TextBndl.getString("IniFile.RdErr"),
                                      INIFILE, null);
          ErrLogger.logError(msg);
          OptionDlg.showMessageDialog(Nvj.parentFrame, msg, Nvj.PgmName,
                                      JOptionPane.ERROR_MESSAGE);
        }
        else {
          noIni = true;

          /* Create a more or less empty ini file, so that it will exist
             from now on.  Won't worry about any errors that might happen. */
          try {
            BufferedWriter out = new BufferedWriter(new FileWriter(inifile));

            /* Write header */
            out.write(hdrtext, 0, hdrtext.length());

            out.close();
          }
          catch ( Exception e2 ) {}
        }
      }

      initialized = true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Can be used to determine if this is first invocation of program.
   *
   * @return True if ini existed when program started
   */
  public static boolean hasIni() {
    if ( initialized == false ) init();

    return !noIni;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Saves the Hashtable into the initialization file.
   *
   * @return True if successful
   */
  public static boolean save() {
    boolean rc = true;

    if ( initialized == false ) init();

    try {                   // FileWriter can throw IOException
      BufferedWriter out = new BufferedWriter(new FileWriter(inifile));

      /* Write header */
      out.write(hdrtext, 0, hdrtext.length());

      /* Write "key = value" pairs */
      for ( Iterator<String> iter = initable.keySet().iterator();
            iter.hasNext(); ) {
        String key = (String)iter.next();
        //System.out.println(key + " = " + initable.get(key));
        String str = new String(key + " = " + initable.get(key) + "\n");
        out.write(str, 0, str.length());
      }

      out.close();
    }
    catch ( Exception e ) {
      String msg;
      msg = ErrLogger.formatError(TextBndl.getString("IniFile.WrtErr"),
                                  INIFILE, null);
      ErrLogger.logError(msg);
      OptionDlg.showMessageDialog(Nvj.parentFrame, msg, Nvj.PgmName,
                                  JOptionPane.ERROR_MESSAGE);
      Nvj.parentFrame.repaint();  // Clean up menu residue
      rc = false;
    }

    return rc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates/overwrites a key/value pair in the Hashtable.
   */
  public static void set(String key, String value) {
    if ( initialized == false ) init();

    if ( key != null && value != null )
      initable.put(key, value);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the value for the given key.
   *
   * @return null if not in table.
   */
  public static String get(String key) {
    if ( initialized == false ) init();

    return (String)initable.get(key);   // Rtns null if not in table
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets a boolean value for the given key.
   */
  public static void setBoolean(String key, boolean value) {
    set(key, value ? "1" : "0");        // Does init() if necessary
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets a boolean value for the given key.
   *
   * @return False if not in table
   */
  public static boolean getBoolean(String key) {
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 || s.equals("0") ) return false;
    else                                                 return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets a boolean value for the given key.
   *
   * @return dft if not in table
   */
  public static boolean getBoolean(String key, boolean dft) {
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 ) return dft;
    else if ( s.equals("0") )           return false;
    else                                return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets an int value for the given key.
   */
  public static void setInt(String key, int i) {
    set(key, Integer.toString(i));      // Does init() if necessary
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets an int value for the given key.
   *
   * @return dft if not in table
   */
  public static int getInt(String key, int dft) {
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 ) return dft;
    int t;
    try                   { t = Integer.parseInt(s); }
    catch ( Exception e ) { t = dft; }
    return t;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets a color value for the given key.
   */
  public static void setColor(String key, Color c) {
    int i = c.getRGB() & 0xffffff;      // Remove alpha part
    // (Bits 31-24 were alpha, 23-16 are red, 15-8 are green, 7-0 are blue)
    String h = new String("0000000" + Integer.toHexString(i).toUpperCase());
    i = h.length();
    set(key, h.substring(i - 6));       // Does init() if necessary
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets an opaque color value for the given key.
   *
   * @return dft if not in table
   */
  public static Color getColor(String key, Color dft) {
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 ) return dft;
    Color c;
    try                   { c = new Color(Integer.parseInt(s, 16)); }
    catch ( Exception e ) { c = dft; }    // (alpha bits ignored)
    return c;
    // (Bits 31-24 are alpha, which default to 255,
    // 23-16 are red, 15-8 are green, 7-0 are blue)
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets a font value for the given key.
   */
  public static void setFont(String key, Font f) {
    String size = Integer.toString(f.getSize());
    String style = Integer.toString(f.getStyle());
    set(key, size + "," + style + "," + f.getName());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets a font value for the given key.
   *
   * @return dft if not in table
   */
  public static Font getFont(String key, Font dft) {
    int size, style;
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 ) return dft;
    StringTokenizer t = new StringTokenizer(s, ",", false);
    if ( 3 != t.countTokens() ) return dft;
    try                   { size = Integer.parseInt(t.nextToken()); }
    catch ( Exception e ) { size = 12; }
    if ( size % 2 == 1 ) size -= 1;  // Don't want odd numbers
    if ( size < 6 || size > 20 ) size = 12;
    try                   { style = Integer.parseInt(t.nextToken()); }
    catch ( Exception e ) { style = 0; }
    if ( style < 0 || style > 3 ) style = 0;
    // Might want to compare against getAvailableFontFamilyNames();
    return new Font(t.nextToken(), style, size);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets a (trimmed) String value for the given key.
   */
  public static void setString(String key, String s) {
    if ( s == null ) s = "";            // Prevent trim of null
    set(key, s.trim());                 // Does init() if necessary
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets a (trimmed) String value for the given key.
   *
   * @return dft if not in table
   */
  public static String getString(String key, String dft) {
    String s = get(key);                // Does init() if necessary
    if ( s == null || s.length() == 0 ) return dft;
    else                                return s.trim();
  }

  /* For testing */
  //static public void main(String[] args) {
  //  String a, b;
  //
  //  Initor.init();
  //
  //  a = get("keyx");
  //  if ( a != null ) System.out.println("Darn!");
  //
  //  set("key1", "value1");
  //  set("key2", "value1");
  //  set("key2", "value2");
  //  a = get("key1");
  //  b = get("key2");
  //  if ( !a.equals("value1") || !b.equals("value2") )
  //    System.out.println("Darn!");
  //  save();
  //  System.exit(0);
  //}
}

