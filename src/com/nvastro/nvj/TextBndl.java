/*
 * TextBndl.java  -  Handles localizable text in Text.properties
 * Copyright (C) 2011-2012 Brian Simpson
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

import java.util.*;
import java.text.DecimalFormatSymbols;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Handles localizable text in Text.properties.
 * @author Brian Simpson
 */
public class TextBndl {
  static private ResourceBundle bundle = null;
  static private char dp = 0;
  static private char minus = 0;
  static private char percent = 0;
  static private String dtsep = null, tmsep = null, angsep = null, hrsym = null;
  static private String degsym = null, minsym = null, secsym = null;

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available
   */
  private TextBndl() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the text string referenced by the key string.
   *
   * @param key key string
   */
  static public String getString(String key) {
    if ( bundle == null ) {
      bundle = ResourceBundle.getBundle("com/nvastro/nvj/Text");
    }
    return bundle.getString(key);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the text string referenced by the key string and adds 2 spaces.
   *
   * @param key key string
   */
  static public String getStringS2(String key) {
    String s = getString(key);
    if ( s.length() == 0 ) return s;   // Don't add 2 spaces in this case
    else return s + "  ";
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the decimal point for the default locale.
   */
  static public char getDPChar() {
    if ( dp == 0 ) getSymbols();
    return dp;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the minus sign for the default locale.
   */
  static public char getMinusChar() {
    if ( minus == 0 ) getSymbols();
    return minus;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the percent symbol for the default locale.
   */
  static public char getPercentChar() {
    if ( percent == 0 ) getSymbols();
    return percent;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the date separator (year/month/day) from the text file
   * as a 1 char string.
   */
  static public String getDtSep() {
    if ( dtsep == null ) getSymbols();
    return dtsep;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the date separator char (year/month/day) from the text file.
   */
  static public char getDtSepChar() { return getDtSep().charAt(0); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the time separator (hours:minutes) from the text file
   * as a string.
   */
  static public String getTmSep() {
    if ( tmsep == null ) getSymbols();
    return tmsep;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the time separator char (hours:minutes) from the text file.
   */
  static public char getTmSepChar() { return getTmSep().charAt(0); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the angle separator (degees:minutes:seconds) from the text file
   * as a string.
   */
  static public String getAngSep() {
    if ( angsep == null ) getSymbols();
    return angsep;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the hour symbol (typically "h") from the text file as a string.
   */
  static public String getHrSym() {
    if ( hrsym == null ) getSymbols();
    return hrsym;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the degree symbol (typically "d" or "°") from the text file
   * as a string.
   */
  static public String getDegSym() {
    if ( degsym == null ) getSymbols();
    return degsym;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the minute symbol (typically "m" or "'") from the text file
   * as a string.
   */
  static public String getMinSym() {
    if ( minsym == null ) getSymbols();
    return minsym;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the second symbol (typically "s" or """) from the text file
   * as a string.
   */
  static public String getSecSym() {
    if ( secsym == null ) getSymbols();
    return secsym;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the angle separator char (degees:minutes:seconds) from the text file.
   */
  static public char getAngSepChar() { return getAngSep().charAt(0); }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get symbols.
   */
  static private void getSymbols() {
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
    dp = dfs.getDecimalSeparator();
    minus = dfs.getMinusSign();
    percent = dfs.getPercent();

    dtsep = getSymbol("Pgm.DtSep", '/');
    tmsep = getSymbol("Pgm.TmSep", ':');
    angsep = getSymbol("Pgm.AngSep", ':');
    hrsym  = getSymbol("Pgm.Hr",  'h');
    degsym = getSymbol("Pgm.Deg", 'd');
    minsym = getSymbol("Pgm.Min", 'm');
    secsym = getSymbol("Pgm.Sec", 's');
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get symbol.
   */
  static private String getSymbol(String s, char dft) {
    String t = getString(s).trim();
    if ( t.length() > 0 ) return t.substring(0, 1);
    else                  return String.valueOf(dft);
  }
}

