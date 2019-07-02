/*
 * SkyObject.java  -  Class for generic object in sky
 * Copyright (C) 2011-2014 Brian Simpson
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

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * *
Note:  Might want to make this class an interface
       and then implement the interface into StarDB,
       DeepSkyDB, NearSkyDB, and Constellation.
       (A found object number would be added to each...)
* * * * * * * * * * * * * * * * * * * * * * * * * * * * */


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Class for generic object in sky.
 *
 * @author Brian Simpson
 */
public class SkyObject {
  final static private String BAD_TYPE = "Bad SkyObject type.";
  final static private String BAD_NUM  = "Bad SkyObject num.";
  /** = 0 */ final static public int STAR   = 0;
  /** = 1 */ final static public int STARNM = 1;
  /** = 2 */ final static public int DS     = 2;
  /** = 3 */ final static public int NS     = 3;
  /** = 4 */ final static public int CON    = 4;
  /** = 5 */ final static public int NONE   = 5;  // Keep as last number
  private int type;
  private int num;
  private StarDB stardb = null;
  private StarNameDB starnmdb = null;
  private DeepSkyDB dsdb = null;
  private NearSkyDB nsdb = null;
  // Will use static Constellation functions
  private SphereCoords noobject;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor of objects other than NONE
   *
   * @param type Type of object (DS, NS, ...)
   * @param num Number of object within it respective database
   */
  public SkyObject(int type, int num) {
    if ( type < 0 || type >= NONE )     // Should never happen
      //throw new IllegalArgumentException(BAD_TYPE);
      ErrLogger.die(BAD_TYPE);

    switch ( type ) {
      case STAR:
        stardb = new StarDB();
        if ( num >= stardb.getNumberOfStars() ) num = -1;
        break;

      case STARNM:
        starnmdb = new StarNameDB();
        if ( num >= starnmdb.getNumberOfNames() ) num = -1;
        break;

      case DS:
        dsdb = new DeepSkyDB();
        if ( num >= dsdb.getNumberOfObjects() ) num = -1;
        break;

      case NS:
        nsdb = new NearSkyDB();
        if ( num >= nsdb.getNumberOfObjects() ) num = -1;

      case CON:
        if ( num >= Constellation.getNumberOfObjects() ) num = -1;
    }
    if ( num < 0 )                      // Should never happen
      //throw new IllegalArgumentException(BAD_NUM);
      ErrLogger.die(BAD_NUM);

    this.type = type;
    this.num  = num;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor for no object, just coordinates
   *
   * @param sc The spherical coordiantes for the object
   */
  public SkyObject(SphereCoords sc) {
    this.type = NONE;
    this.noobject = new SphereCoords(sc);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns object type (NS, DS, ...).
   */
  public int getType() {
    return type;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns J2000 location as a SphereCoords object.
   *
   * @param mp Mapping parameters (includes Julian date/time to be used)
   * @return Coordinates of object (containing RA/Dec in radians)
   */
  public SphereCoords getJ2000Location(MapParms mp) {
    switch ( type ) {
      case STAR:
        return stardb.getJ2000Location(num);

      case STARNM:
        return starnmdb.getJ2000Location(num);

      case DS:
        return dsdb.getJ2000Location(num);

      case NS:
        return nsdb.getJ2000Location(num, mp);

      case CON:
        return Constellation.getJ2000Location(num);

      default:  /* case NONE: */
        return noobject;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns apparent location as a SphereCoords object.
   *
   * @param mp Mapping parameters (includes Julian date/time to be used)
   * @return Coordinates of object (containing RA/Dec in radians)
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   */
  public SphereCoords getAppLocation(MapParms mp, SphereCoords J2000Coords) {
    switch ( type ) {
      case STAR:
        return stardb.getAppLocation(num, mp, J2000Coords);

      case STARNM:
        return starnmdb.getAppLocation(num, mp, J2000Coords);

      case DS:
        return dsdb.getAppLocation(num, mp, J2000Coords);

      case NS:
        return nsdb.getAppLocation(num, mp, J2000Coords);

      case CON:
        return Constellation.getAppLocation(num, mp, J2000Coords);

      default:  /* case NONE: */
        if ( J2000Coords != null ) J2000Coords.set(noobject);
        return noobject.rotateRADec(mp.getPrecessNutate());
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns indicator as to whether object should be marked on a
   * find operation.
   */
  public boolean markit() {
    switch ( type ) {
      case CON:
        return false;

      default:
        return true;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns indicator as to whether object can be viewed under
   * current Preferences.  (E.g. limiting magnitude, ...).
   * Applicable for find operations.
   *
   * @param prefer User preferences
   * @return True if viewable, false if not
   */
  public boolean isViewable(Preferences prefer) {
    switch ( type ) {
      case STAR:
        return stardb.isViewable(num, prefer);

      case STARNM:
        return starnmdb.isViewable(num, prefer);

      case DS:
        return dsdb.isViewable(num, prefer);

      case NS:
        return prefer.drawNearSky();

      case CON:
        return true;

      default:  /* case NONE: */
        return false;
    }
  }
}

