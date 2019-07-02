/*
 * LST.java  -  Local sidereal time
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.swing.JToggleButton;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Local sidereal time.
 *
 * @author Brian Simpson
 */
public class LST {
  /* Calendar is an abstract base class for converting between a Date        */
  /* object and a set of integer fields such as YEAR, MONTH, DAY, HOUR,      */
  /* and so on. (A Date object represents a specific instant in time         */
  /* with millisecond precision.)  GregorianCalendar is a concrete subclass. */
  /* Internally the Date class reflects coordinated universal time (UTC).    */
  /* About the only methods that may be of use (and haven't been deprecated) */
  /* are getTime() and setTime(long), which are the milliseconds relative    */
  /* to January 1, 1970, 00:00:00 UTC.                                       */
  static final private String DTSEP = TextBndl.getDtSep();
  static final private String TMSEP = TextBndl.getTmSep();
  static final private String AM = " " + TextBndl.getString("DateTimeDlg.AM");
  static final private String PM = " " + TextBndl.getString("DateTimeDlg.PM");
  private long datetime;   // Current viewing date/time at Greenwich
                           //  (Doesn't change when location changes)
                           //  (In millisec. rel. to 1-1-70 00:00:00 UTC)
  //ivate int tzoffset;    // TZ diff (milliseconds) computer to Greenwich
  //                       //  (Is negative for TZs west of Greenwich)
  //                       //  (Doesn't change when location changes)
  private Date dateOld;    // Last computer date/time
  private Location loc;    // Current viewing location
  private int timespeed;   // Time multiplier when "running"
                           //  Maybe I should call this variable "timefactor"
  private boolean running; // Indicator for simulation time "running"
  private char dst;        // DST indicator
  private GregorianCalendar gc; // Note:  DateTimeDlg counts on the same
                           // object being reused.  So if this object needs
                           // changing, update the object.  Do *not* create
                           // a new object.
  private ArrayList<JToggleButton> comptimeBtns =
    new ArrayList<JToggleButton>();
  private boolean comptimeState = false; // Toggle btn off, local = comptr time

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a Local Sidereal Time object for the given location.
   *
   * @param _loc Location for LST
   * @param _dst "0"  -&gt;  DST is off;
   *             "1"  -&gt;  DST is on;
   *             For anything else, if Location has a TimeZone that handles DST,
   *             then use auto DST, otherwise DST is off
   */
  public LST(Location _loc, String _dst) {
    timespeed = 1;
    running = true;
    dateOld = new Date();
    datetime = dateOld.getTime();   // Set datetime to current Geenwich time
    //tzoffset = TimeZone.getDefault().getRawOffset();// Diff computer-Greenwich

    gc = new GregorianCalendar(_loc.getTZ());  // For interpreting local time
    setLocation(_loc, _dst);  // Sets loc, dst, and gc
    setToCompDateTime();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up control of the select state of the "Computer time" toggle button
   * to indicate if local time = computer time.
   */
  public void setCompTimeBtn(JToggleButton btn) {
    comptimeBtns.add(btn);
    btn.setSelected(comptimeState);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the location, which contains information on name, timezone,
   * longitude, latitude.
   *
   * @param _loc Location for LST
   * @param _dst See constructor
   */
  public void setLocation(Location _loc, String _dst) {
    loc = _loc;
    setDST(_dst);         // Sets dst and gc

    if ( !comptimeBtns.isEmpty() ) {
      GregorianCalendar cmptr = new GregorianCalendar();
      calcDateTime(); // Sets datetime
      cmptr.setTime(new Date());
      gc.setTime(new Date(datetime));
      long adiff = Math.abs(datetime - cmptr.getTime().getTime());
      int hourl = gc.get(Calendar.HOUR_OF_DAY);
      int minutel = gc.get(Calendar.MINUTE);

      int hourc = cmptr.get(Calendar.HOUR_OF_DAY);
      int minutec = cmptr.get(Calendar.MINUTE);

      //stem.out.println(gc.getTime().getTime()+", "+cmptr.getTime().getTime());
      //stem.out.println("ADiff = " + adiff);
      //stem.out.println("Local:    " + hourl + ":" + minutel);
      //stem.out.println("Computer: " + hourc + ":" + minutec);
      if ( running && timespeed == 1 &&
           adiff < 10 && hourl == hourc && minutel == minutec )
           comptimeState = false;
      else comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a clone of the object.  Not thread-safe.
   */
  public Object clone() {
    LST l = new LST(loc, getDST()); // Don't clone loc, it's immutable
    l.dateOld = (Date) this.dateOld.clone();
    l.datetime = this.datetime;
    l.timespeed = this.timespeed;
    l.running = this.running;
    return l;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Updates time to match given LST, and pauses clock.
   *
   * @param l LST to match
   */
  public void updateStop(LST l) {
    this.loc       = l.loc;
    this.dst       = l.dst;
    this.gc        = l.gc;
    this.datetime  = l.datetime;
    this.dateOld   = l.dateOld;
    this.timespeed = l.timespeed;
    this.running   = l.running;
    stop();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Stops time from running.
   */
  public void stop() {
    calcDateTime();       // Update dateOld & datetime 1st
    running = false;

    if ( !comptimeBtns.isEmpty() && !comptimeState ) {
      comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Starts time running.
   */
  public void start() {
    running = true;
    dateOld = new Date();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Reports if time running.
   */
  public boolean isRunning() { return running; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets time speed factor.
   *
   * @param sp new time speed factor
   */
  public void setTimeSpeed(int sp) {
    int oldts = timespeed;

    if ( timespeed != sp ) {
      calcDateTime();     // Update dateOld & datetime 1st
      timespeed = sp;
    }

    if ( !comptimeBtns.isEmpty() && !comptimeState &&
         (!running || oldts != 1 || timespeed != 1) ) {
      comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets time speed factor.
   */
  public int getTimeSpeed() {
    return timespeed;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculate date and time.
   */
  private void calcDateTime() {  // Updates dateOld & datetime
    if ( !running ) return;

    Date dateNew = new Date();
    datetime += (dateNew.getTime() - dateOld.getTime()) * timespeed;
    dateOld = dateNew;      // Update ref to point to current date/time
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns local sidereal time in hours.
   */
  public double getLSTHrs() {
    double jd = getJulianDay();    // Does calcDateTime

    /* Separate jd into jd0, the Julian date at 0hr UTC, */
    /* and ut, the UTC hours of that day.                */
    /* (At 0hr UTC, jd0 must end with .5)                */
    double jd0 = Math.floor(jd - 0.5) + 0.5;
    double ut = jd - jd0;
    ut *= 24;

    /* Calculate Greenwich mean sidereal time (gst) using */
    /* algorithm from Practical Astronomy book (modified) */
    double t = (jd0 - 2451545.0) / 36525.0;
    double gst = 6.697374558 + t * (2400.0513369072 +
                               t * (.0000258622 + t / 580650000));
    gst = gst % 24.0;               // OK if < 0
    gst += ut * 1.00273790935;

    /* Now adjust for longitude */
    double lst = gst + loc.getLongDeg() / 15.0;
    while ( lst >= 24.0 ) lst -= 24.0;
    while ( lst  <  0.0 ) lst += 24.0;
    return lst;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Julian Day.
   */
  public double getJulianDay() {
    // 12-31-1999 00:00 UTC = Epoch 2000 January 0.0 = 2451543.5
    // 1-1-2000 00:00 UTC <=> 2451544.5
    // 1-1-2000 12:00 UTC <=> 2451545.0 <=> J2000.0
    // 1-1-1970 00:00 UTC <=> 2440587.5
    /* Julian date is independent of timezones, dst, ... */
    /* If JVM correctly configured, then no adjustment   */
    /* needed for timezones, dst, ...                    */

    calcDateTime();
                    // Don't want int division here
    return(datetime / 86400000.0 + 2440587.5);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Julian Ephemeris Day.
   */
  public double getJulianEphDay() {
    double jday = getJulianDay();

    // Add delta t (= TT - UT)
    jday += DeltaT.calcDeltaT(jday) / 86400;

    return jday;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by DateTimeDlg (and also by tellTZOffsetWithDST() below).
   */
  public GregorianCalendar getLocDateTime() {
    calcDateTime();
    gc.setTime(new Date(datetime));
    return gc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by DateTimeDlg and assumes that DateTimeDlg has
   * modified gc.
   */
  public void setLocDateTime() {
    datetime = gc.getTime().getTime();
    dateOld = new Date();

    /* If not auto-DST, check if DST was changed by DateTimeDlg */
    if ( dst != 'A' )
      dst = gc.getTimeZone().inDaylightTime(dateOld) ? '1' : '0';

    if ( !comptimeBtns.isEmpty() && !comptimeState ) {
      comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adds (or subtracts) the specified time.
   *
   * @param field Time field to change (use Calendar constants)
   * @param up Indicates if the specified field is rolled up (true) or down
   */
  public void addTime(int field, boolean up) {
    calcDateTime(); // Sets datetime
    gc.setTime(new Date(datetime));
    gc.add(field, up ? 1 : -1);
    datetime = gc.getTime().getTime();
    dateOld = new Date();

    if ( !comptimeBtns.isEmpty() ) {
      GregorianCalendar cmptr = new GregorianCalendar();
      cmptr.setTime(new Date());
      long adiff = Math.abs(datetime - cmptr.getTime().getTime());
      int hourl = gc.get(Calendar.HOUR_OF_DAY);
      int minutel = gc.get(Calendar.MINUTE);

      int hourc = cmptr.get(Calendar.HOUR_OF_DAY);
      int minutec = cmptr.get(Calendar.MINUTE);

      //stem.out.println(gc.getTime().getTime()+", "+cmptr.getTime().getTime());
      //stem.out.println("ADiff = " + adiff);
      //stem.out.println("Local:    " + hourl + ":" + minutel);
      //stem.out.println("Computer: " + hourc + ":" + minutec);
      if ( running && timespeed == 1 &&
           adiff < 10 && hourl == hourc && minutel == minutec )
           comptimeState = false;
      else comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * If input equals
   *   "0"  ->  DST is off
   *   "1"  ->  DST is on
   *    *   ->  For anything else, if Location has a TimeZone that handles DST,
   *            then use auto DST, otherwise DST is off
   */
  private void setDST(String d) {
    char c = 'A';

    if ( d != null && d.length() == 1 ) {
      if      ( d.equals("0") ) c = '0';
      else if ( d.equals("1") ) c = '1';
    }
    if ( c == 'A' && !(loc.handlesDST()) ) c = '0';
    dst = c;

    /* Now setup a GregorianCalendar */
    TimeZone tz = loc.getTZ();
    if ( dst == '0' )
      gc.setTimeZone(new SimpleTimeZone(tz.getRawOffset(), tz.getID()));
    else if ( dst == '1' )
      gc.setTimeZone(new NotSoSimpleTimeZone(tz));
    else
      gc.setTimeZone(tz);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get DST info.
   *
   * @return A  -  auto DST active
   * <br>    0  -  DST is off (no auto DST)
   * <br>    1  -  DST is on  (no auto DST)
   */
  public String getDST() {
    return new Character(dst).toString();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates whether DST is in effect.
   */
  public boolean inDST() {
    if      ( dst=='0' ) return false;
    else if ( dst=='1' ) return true;
    else                 return loc.getTZ().inDaylightTime(new Date(datetime));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the date and time as a string.
   *
   * @param ampm True for am/pm, false for 24 hour
   */
  public String tellLocDateTime(boolean ampm) {
    calcDateTime();

    /* May be able to use DateFormat, but must be able to coax it to display
       time in both 24 hour mode and am / pm, and must also change DateTimeDlg
       to use 00 instead of 12, or 24 instead of 00 if the locale displays
       hours in this fashion... */

    gc.setTime(new Date(datetime));

    int year = gc.get(Calendar.YEAR);
    int mon  = gc.get(Calendar.MONTH) + 1;
    int day  = gc.get(Calendar.DAY_OF_MONTH);
    int hr   = gc.get(Calendar.HOUR_OF_DAY);
    int mn   = gc.get(Calendar.MINUTE);

    String hour, ampms = "";
    if ( ampm ) {
      if ( hr < 12 )
        ampms = AM;      // " AM"
      else {
        ampms = PM;      // " PM"
        hr -= 12;
      }
      if ( hr == 0 ) hr = 12;
      hour = new String(" " + hr);
    }
    else {
      hour = new String(" " + hr/10 + hr%10);
    }

    return new String(year + DTSEP + mon/10 + mon%10 + DTSEP + day/10 + day%10 +
                      hour + TMSEP + mn/10 + mn%10 + ampms);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets time to current computer time.
   * Called only by DateTimeDlg, Computer Time toolbar button, and Constructor.
   */
  public void setToCompDateTime() {
    /* If not auto-DST, check if DST was changed by DateTimeDlg */
    if ( dst != 'A' )
      dst = gc.getTimeZone().inDaylightTime(dateOld) ? '1' : '0';

    GregorianCalendar now = new GregorianCalendar();
    gc.set(Calendar.YEAR,        now.get(Calendar.YEAR));
    gc.set(Calendar.MONTH,       now.get(Calendar.MONTH));
    gc.set(Calendar.DATE,        now.get(Calendar.DATE));
    gc.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
    gc.set(Calendar.MINUTE,      now.get(Calendar.MINUTE));
    gc.set(Calendar.SECOND,      now.get(Calendar.SECOND));
    gc.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND));

    // The following code may be inaccurate when at the transition
    // from DT to ST or ST to DT, but probably not worth fixing
    if ( Preferences.dstAdjust != 0 &&
         gc.getTimeZone().inDaylightTime(gc.getTime()) )
      gc.add(Calendar.MINUTE, Preferences.dstAdjust);

    datetime = gc.getTime().getTime();
    dateOld = new Date();       // Update ref
    // Note:  The change in LST will be greater than the change in
    // offset (tzoffset - tz.getRawOffset()) by a factor of 1.002737909

    if ( !comptimeBtns.isEmpty() ) {
      // Always set/reset, otherwise button will toggle select state visually
      if ( running && timespeed == 1 ) comptimeState = false;
      else                             comptimeState = true;
      for ( JToggleButton btn : comptimeBtns )
        btn.setSelected(comptimeState);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich (as [-][H]H:MM).
   * It is negative for west of Greenwich, and takes into account DST.
   * Only called by VersionDlg.
   */
  public String tellTZOffsetWithDST() {
    // Use TimeZone from gc, not loc, since gc's TimeZone changes with
    // AutoDST and DST settings, while loc's does not (see setDST)
    getLocDateTime(); // Sets gc to latest time
    // Note:  Easier to use TimeZone.getOffset(datetime), but that
    // function doesn't appear till Java 1.4
    // int tz_offset = gc.getTimeZone().getOffset(datetime) / 60000;
    int tz_offset = gc.getTimeZone().getOffset(gc.get(Calendar.ERA),
                      gc.get(Calendar.YEAR), gc.get(Calendar.MONTH),
                      gc.get(Calendar.DATE), gc.get(Calendar.DAY_OF_WEEK),
                      gc.get(Calendar.MILLISECOND) + 1000 * (
                      gc.get(Calendar.SECOND) + 60 * (gc.get(Calendar.MINUTE) +
                      60 * gc.get(Calendar.HOUR_OF_DAY)))) / 60000;
    int h = tz_offset / 60;
    int m = Math.abs(tz_offset % 60);
    return new String(h + TextBndl.getTmSep() + ((m < 10) ? "0" : "" ) + m);
  }

  /* * *   ----------   Accessors to Location information   ----------   * * */

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the Location object.
   */
  public Location getLocation() { return loc; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city string, or if not set, the latitude and longitude.
   */
  public String tellLocation() { return loc.tellLocation(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city string (or "" if not set).
   */
  public String tellCity() { return loc.tellCity(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location in degrees.
   */
  public double getLongDeg() { return loc.getLongDeg(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location as a string ([-][DD]D:MM).
   */
  public String tellLong() { return loc.tellLong(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location in degrees.
   */
  public double getLatDeg() { return loc.getLatDeg(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location as a string ([-][D]D:MM).
   */
  public String tellLat() { return loc.tellLat(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich in minutes.
   * It is negative for west of Greenwich, and ignores DST.
   */
  public double getTZOffsetMin() { return loc.getTZOffsetMin(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich (as [-][H]H:MM).
   * It is negative for west of Greenwich, and ignores DST.
   */
  public String tellTZOffset() { return loc.tellTZOffset(); }
}

/*------------------------------------------------------------------------------

Remember that the Date object measures time according to UTC.  The TimeZone
object is used to interpret this for rendering the correct time for the
zone.  DateFormat and GregorianCalendar objects use a TimeZone object.

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

Can test with
java -Duser.timezone=MST nvj.Nvj
java -Duser.timezone=MST7MDT nvj.Nvj
java -Duser.timezone="Australia/Sydney" nvj.Nvj

Some timezone articles
http://www.javaworld.com/javaworld/jw-10-2003/jw-1003-time.html
http://www.minaret.biz/tips/timezone.html

------------------------------------------------------------------------------*/

