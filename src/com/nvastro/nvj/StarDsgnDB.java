/*
 * StarDsgnDB.java  -  Star designations database and methods
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

import java.util.ArrayList;
import java.util.Collections;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Helper class for star designations.
 *
 * @author Brian Simpson
 */
class StarDsgn implements Comparable<StarDsgn> {
  int dsgn;    // Number used for sorting
  int index;   // Index into StarDB
  String name; // Alphanumeric name (All except Flamsteed)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public StarDsgn(int dsgn, String name, int index) {
    this.dsgn  = dsgn;
    this.name  = name;
    this.index = index;
  }

  public int compareTo(StarDsgn sd) {
    int result = 0;

    // Sort 1st by dsgn number, then by name, finally by
    // StarDB number (so brighter stars come 1st)
    if      ( dsgn  > sd.dsgn  ) return  1;
    else if ( dsgn  < sd.dsgn  ) return -1;
    else if ( name != null && sd.name != null ) {
      result = name.compareToIgnoreCase(sd.name);
    }
    if ( result != 0 )           return result;
    else if ( index > sd.index ) return  1;
    else                         return -1;
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * In Java cannot directly create an array of a Generic type
 * (ArrayList<StarDsgn>).  This is a workaround...
 *
 * @author bsimpson
 */
@SuppressWarnings("serial")
class StarDsgnArray extends ArrayList<StarDsgn>{}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Star designations database and methods.  This is basically a helper class
 * for the FindStarDlg class.
 *
 * @author Brian Simpson
 */
public class StarDsgnDB {
  static private ArrayList<StarDsgn>[] bayer = new StarDsgnArray[88];
  static private ArrayList<StarDsgn>[] flmst = new StarDsgnArray[88];
  static private boolean initialized = false;
  static private String nothing = "";
  static private StarDB stardb;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public StarDsgnDB() {
    if ( initialized == false ) {
      init();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initializes the star designations database.  Called by Nvj during
   * program startup.
   */
  public static void init() {
    int i, j, k, numstars;
    int[] con = new int[1],
          grk = new int[1],
          flm = new int[1];
    String byr;

    if ( initialized == false ) {
      stardb = new StarDB();

      for ( i = 0; i < 88; i++ ) {
        bayer[i] = new StarDsgnArray();
        flmst[i] = new StarDsgnArray();
      }

      numstars = stardb.getNumberOfStars();

      for ( i = 0; i < numstars; i++ ) {
        if ( stardb.getMag100(i) > 800 ) break;

        byr = stardb.getDesignations(i, con, grk, flm);
        if ( con[0] > 0 ) {
          // con[0]: none = 0, And = 1, Ser = 76,77, Vul = 89
          if ( --con[0] > 75 ) con[0]--; // Decrement con[0] and combine Serpens
          // so that: And = 0, Ser = 75, Vul = 87

          if ( grk[0] > 0 ) {
            bayer[con[0]].add(new StarDsgn(grk[0], byr, i));
          }
          else if ( byr != null ) {
            // 500 is larger than largest grk[0], so these
            // will end up sorted after the previous section
            bayer[con[0]].add(new StarDsgn(500, byr, i));
          }

          if ( flm[0] > 0 ) {
            flmst[con[0]].add(new StarDsgn(flm[0], null, i));
          }
        }
      }

      StarDsgn stara, starb;
      for ( i = 0; i < 88; i++ ) {
        Collections.sort(bayer[i]);
        Collections.sort(flmst[i]);

        for ( j = 0; j < bayer[i].size() - 1; j++ ) {
          stara = (StarDsgn)(bayer[i].get(j));

          for ( k = j + 1; k < bayer[i].size(); k++ ) {
            starb = (StarDsgn)(bayer[i].get(k));

            if ( stara.dsgn == starb.dsgn &&
                 (stara.dsgn < 500 || stara.name.equals(starb.name)) ) {
              bayer[i].remove(k--);  // Remove dimmer duplicate
            }
            else break; // Out of inner loop, to increment j
          }
        }

        for ( j = 0; j < flmst[i].size() - 1; j++ ) {
          stara = (StarDsgn)(flmst[i].get(j));

          for ( k = j + 1; k < flmst[i].size(); k++ ) {
              starb = (StarDsgn)(flmst[i].get(k));

            if ( stara.dsgn == starb.dsgn )
              flmst[i].remove(k--);  // Remove dimmer duplicate
            else break; // Out of inner loop, to increment j
          }
        }

        bayer[i].trimToSize();
        flmst[i].trimToSize();
      }

      initialized = true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns number of star designations for a given constellation.
   * Used by the "Find star" window.
   *
   * @param con Index of constellation (0 - 87:  0 = And, 75 = Ser, 87 = Vul)
   */
  public int getNumberOfDsgns(int con) {
    if ( con >= 0 && con <= 87 )
      return bayer[con].size() + flmst[con].size();
    else return 0;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns name of star designation for a given constellation and dsgn index.
   * Used by the "Find star" window.
   *
   * @param con Index of constellation (0 - 87:  0 = And, 75 = Ser, 87 = Vul)
   * @param idx Index of of designation within constellation
   */
  public String tellDesignation(int con, int idx) {
    if ( con >= 0 && con <= 87 && idx >= 0 ) {
      if ( idx < bayer[con].size() )
        return bayer[con].get(idx).name;

      idx -= bayer[con].size();
      if ( idx < flmst[con].size() )
        return Integer.toString(flmst[con].get(idx).dsgn);
    }
    return nothing;  // Should not happen
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns StarDB index for a given constellation and dsgn index.
   * Used by the "Find star" window.
   *
   * @param con Index of constellation (0 - 87:  0 = And, 75 = Ser, 87 = Vul)
   * @param idx Index of of designation within constellation
   */
  public int getStarIndex(int con, int idx) {
    if ( con >= 0 && con <= 87 && idx >= 0 ) {
      if ( idx < bayer[con].size() )
        return ((StarDsgn)(bayer[con].get(idx))).index;

      idx -= bayer[con].size();
      if ( idx < flmst[con].size() )
        return ((StarDsgn)(flmst[con].get(idx))).index;
    }
    return -1;  // Should not happen
  }
}

