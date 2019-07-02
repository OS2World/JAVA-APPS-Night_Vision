/*
 * Greek.java  -  The Greek class
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


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * The Greek class - All static functions.
 * <pre>
 *  "Alpha",   "Beta",    "Gamma",   "Delta",   "Epsilon",   "Zeta",
 *  "Eta",     "Theta",   "Iota",    "Kappa",   "Lambda",    "Mu",
 *  "Nu",      "Xi",      "Omicron", "Pi",      "Rho",       "Sigma",
 *  "Tau",     "Upsilon", "Phi",     "Chi",     "Psi",       "Omega"
 * </pre>
 *
 * @author Brian Simpson
 */
public class Greek {
  /* 2-3 letter (untranslated) Greek names used for reading in external star
     DBs (as opposed to what is presented to the user, which comes from
     Text.properties and can be translated) */
  static final String[] byr = {
    "alp", "bet", "gam", "del", "eps", "zet", "eta", "the",
    "iot", "kap", "lam", "mu",  "nu",  "xi",  "omi", "pi",
    "rho", "sig", "tau", "ups", "phi", "chi", "psi", "ome" };

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Greek character name with the first letter capitalized.
   *
   * @param i Bounds: 1 &lt;= i &lt;= 24
   */
  static public String tellGreek(int i) {
    if ( i < 1 || i > 24 ) return "";
    return TextBndl.getString("Grk" + i);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Greek character name in all lower case letters.
   *
   * @param i Bounds: 1 &lt;= i &lt;= 24
   */
  static public String tellgreek(int i) {
    if ( i < 1 || i > 24 ) return "";
    return TextBndl.getString("Grk" + i).toLowerCase();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Greek character name in all lower case letters
   * truncated to no more than 3 letters.
   *
   * @param i Bounds: 1 &lt;= i &lt;= 24
   */
  static public String tell3greek(int i) {
    if ( i < 1 || i > 24 ) return "";
    String s = TextBndl.getString("Grk" + i);
    int j = s.length();
    if ( j > 3 ) j = 3;   // -> j = 2 or 3
    return s.toLowerCase().substring(0, j);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the upper case Greek unicode character.
   *
   * @param i Bounds: 1 &lt;= i &lt;= 24 (otherwise return blank)
   */
  static public char getGreek(int i) {
    if ( i < 1 || i > 24 ) return (char)' ';
    if ( i <= 17 ) return (char)(i + 0x0390);
    else           return (char)(i + 0x0391);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the lower case Greek unicode character.
   *
   * @param i Bounds: 1 &lt;= i &lt;= 24 (otherwise return blank)
   */
  static public char getgreek(int i) {
    if ( i < 1 || i > 24 ) return (char)' ';
    if ( i <= 17 ) return (char)(i + 0x03B0);
    else           return (char)(i + 0x03B1);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a number corresponding to a 2-3 letter designation for a
   * Greek character.
   * Used by StarDB while reading external star DBs.
   *
   * @param str 2-3 letters; e.g. "alp", "bet", pi" (case insensitive)
   * @return number 1 - 24, 0 if no match
   */
  static public int getGNum(String str) {
    int i;
    for ( i = 0; i < 24; i++ )
      if ( str.equalsIgnoreCase(byr[i]) ) break;
    if ( i == 24 ) return 0;
    return ++i;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  System.out.println(tellGreek(1));
  //  System.out.println(tellgreek(1));
  //  System.out.println(tell3greek(1));
  //  System.out.println(tellGreek(24));
  //  System.out.println(tellgreek(24));
  //  System.out.println(tell3greek(24));
  //  System.out.println(tellGreek(getGNum("alp")));
  //}
}

