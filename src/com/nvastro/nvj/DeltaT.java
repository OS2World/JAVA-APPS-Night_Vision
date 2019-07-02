/*
 * DeltaT.java  -  Determines delta t (= TT - UT)
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

// NumberFormat (used in main)


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Determines delta t (= TT - UT).
 * <p>
 * For years between 1620 and 2014
 * (last few years are (wild) guesses), table from
 * http://www.phys.uu.nl/~vgent/astro/deltatime.htm
 * is used (adjusted for lunar acceleration = -25.7376 arcsec/cy/cy),
 * with Bessel Interpolation for intermediate values.
 * This table agrees very closely with table from Jean Meeus'
 * Astronomical Algorithms (2nd ed. 1998) in 1900s, and diverges to
 * only 3 seconds at 1620 (most likely due to slightly different
 * lunar acceleration).
 * <p>
 * Values before 1620 use a formula from JPL Horizons (according to
 * above website, and derived from Stephenson &amp; Houlden (1986)),
 * since it closely matches table value at 1620.  (Small adjustment is
 * made between years 1600 - 1620 to get an exact match at 1620.)
 * <p>
 * Values after 2014 use formula from Jean Meeus' Astronomical Algorithms
 * (2nd ed. 1998), from Chapront, Chapront-Touz&eacute;, &amp; Francou (1997).
 * Adjustments are made through year 2100 for smooth transition at 2014.
 * <p>
 * Values before 948 use formula from Jean Meeus' Astronomical Algorithms
 * (2nd ed. 1998), from Chapront, Chapront-Touz&eacute;, &amp; Francou (1997).
 * Small change done to constant term to get exact match to succeeding
 * formula at 948.
 *
 * @author Brian Simpson
 */
public class DeltaT {
  // Web sources for data and methods
  // http://www.phys.uu.nl/~vgent/astro/deltatime.htm (temp. unavailable,
  //   see http://www.phys.uu.nl/~vgent/homepage.htm)
  // ftp://maia.usno.navy.mil/ser7/finals.all
  // ftp://maia.usno.navy.mil/ser7/readme.finals
  // http://maia.usno.navy.mil/ser7/tai-utc.dat  - Leap Second Table
  // http://sunearth.gsfc.nasa.gov/eclipse/SEhelp/deltaT.html
  // http://sunearth.gsfc.nasa.gov/eclipse/SEhelp/deltaT2.html
  // http://baas.lamost.org/calc/matlab/deltat.m
  // http://www.lunar-occultations.com/iota/occultdeltaat.htm
  // http://user.online.be/felixverbelen/dt.htm
  // http://www.maths.abdn.ac.uk/~igc/tch/engbook/node64.html
  // http://home.att.net/~srschmitt/bessel_interpolation.html

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private DeltaT() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates delta t
   *
   * @param julian Julian date
   * @return DeltaT in seconds
   */
  static public double calcDeltaT(double julian) {
    // Look at Meeus book and above formulas to discern
    // some method to this madness...
    double u, d;
    double Y = 2000 + (julian - 2451545) / 365.25;
    // Y is derived from the Julian day and represents the calendar year.
    // Won't worry about slight mismatch between Julian year and calendar year
    // since it's only 3 days every 4 centuries, and DeltaT will not change
    // appreciably over 3 days...

    if ( ! Preferences.usedeltat ) return 0;

    if ( Y >= YSTOP ) {
      // No matter what I do here it's pure fantasy
      // The following is an abandoned method...
      //u = Y - YSTOP;
      //int p = dt[dtlength-1] - dt[dtlength-2];
      //d = (dt[dtlength-101] - (dt[dtlength-1] - 100 * p)) * 1e-4;
      //d = 0.01 * (dt[dtlength-1] + u * (p + u * d));

      u = (Y - 2000) / 100.0;
      d = 102 + u * (102 + u * 25.3);

      // Allow to 2100 to smooth large transition
      if ( Y < 2100 ) {
        u = (YSTOP - 2000) / 100.0;
        d += ((Y - 2100)/(YSTOP - 2100)) *
             (dt[dtlength-1]/100.0 - (102 + u * (102 + u * 25.3)));
      }
    }
    else if ( Y < 948 ) {
      u = (Y - 2000) / 100.0;
      //  2178.45936 (instead of 2177) to get smooth transition
      d = 2178.45936 + u * (497 + u * 44.1);
    }
    else if ( Y < YSTART ) {
      u = (Y - 2000) / 100.0;
      d = 50.6 + u * (67.5 + u * 22.5);

      // Allow 20 years to achieve a smooth transition at YSTART
      if ( Y > YSTART - 20 ) {
        u = (YSTART - 2000) / 100.0;
        d += ((Y - YSTART + 20)/20) *
             (dt[0]/100.0 - (50.6 + u * (67.5 + u * 22.5)));
      }
    }
    else { // YSTOP <= Y < YSTOP
      int x = (int)Y - YSTART;   // (always >= 0)
      u = Y - (int)Y;            // 0 <= u < 1  (fractional year)
      d = dt[x] + u * (d1[x] + u * (d2[x] + u * (d3[x] + u * d4[x])));
      d *= 0.01; // Convert centiseconds to seconds
    }

    return d;
  }

  /* For testing */
  //static public void main(String[] args) {
  //  double Y, j;
  //  NumberFormat yr_fmt;
  //
  //  yr_fmt = NumberFormat.getInstance();
  //  if ( yr_fmt instanceof DecimalFormat ) {
  //    ((DecimalFormat)yr_fmt).applyPattern("0.00"); // (rounds)
  //  }
  //
  //  System.out.println("YSTART = " + YSTART + ", YSTOP = " + YSTOP +
  //                     ", Table size = " + dtlength);
  //  System.out.println("\nEvery 180 days from 1500 to 2200");
  //  for ( j = 2268920; j < 2524595; j += 180 ) {
  //    Y = 2000 + (j - 2451545) / 365.25;
  //    System.out.println(yr_fmt.format(Y) + " - " + calcDeltaT(j));
  //  }
  //  System.out.println("\nCheck transition around " + 948);
  //  for ( Y = 948 - 2; Y <= 948 + 2; Y += .1 ) {
  //    j = (Y - 2000) * 365.25 + 2451545;
  //    System.out.println(yr_fmt.format(Y) + " - " +
  //                       yr_fmt.format(calcDeltaT(j)));
  //  }
  //  System.out.println("\nCheck transition around " + YSTART);
  //  for ( Y = YSTART - 2; Y <= YSTART + 2; Y += .1 ) {
  //    j = (Y - 2000) * 365.25 + 2451545;
  //    System.out.println(yr_fmt.format(Y) + " - " +
  //                       yr_fmt.format(calcDeltaT(j)));
  //  }
  //  System.out.println("\nCheck transition around " + YSTOP);
  //  for ( Y = YSTOP - 2; Y <= YSTOP + 2; Y += .1 ) {
  //    j = (Y - 2000) * 365.25 + 2451545;
  //    System.out.println(yr_fmt.format(Y) + " - " +
  //                       yr_fmt.format(calcDeltaT(j)));
  //  }
  //  System.out.println("\nEvery 100 years");
  //  for ( Y = 1000; Y <= 3000; Y += 100 ) {
  //    j = (Y - 2000) * 365.25 + 2451545;
  //    System.out.println(yr_fmt.format(Y) + " - " +
  //                       yr_fmt.format(calcDeltaT(j)));
  //  }
  //  //stem.out.println("0: " + d1[0] + ", " + d2[0] +
  //  //                  ", " + d3[0] + ", " + d4[0]);
  //  //stem.out.println("1: " + d1[1] + ", " + d2[1] +
  //  //                  ", " + d3[1] + ", " + d4[1]);
  //  //stem.out.println("2: " + d1[2] + ", " + d2[2] +
  //  //                  ", " + d3[2] + ", " + d4[2]);
  //}

  static final private int dt[] = { // Values are in centiseconds
    // 1620 - 1699
    12400, 11900, 11500, 11000, 10600, 10200,  9800,  9500,  9100,  8800,
     8500,  8200,  7900,  7700,  7400,  7200,  7000,  6700,  6500,  6300,
     6200,  6000,  5800,  5700,  5500,  5400,  5300,  5100,  5000,  4900,
     4800,  4700,  4600,  4500,  4400,  4300,  4200,  4100,  4000,  3800,
     3700,  3600,  3500,  3400,  3300,  3200,  3100,  3000,  2800,  2700,
     2600,  2500,  2400,  2300,  2200,  2100,  2000,  1900,  1800,  1700,
     1600,  1500,  1400,  1400,  1300,  1200,  1200,  1100,  1100,  1000,
     1000,  1000,   900,   900,   900,   900,   900,   900,   900,   900,
    // 1700 - 1799
      900,   900,   900,   900,   900,   900,   900,   900,  1000,  1000,
     1000,  1000,  1000,  1000,  1000,  1000,  1000,  1100,  1100,  1100,
     1100,  1100,  1100,  1100,  1100,  1100,  1100,  1100,  1100,  1100,
     1100,  1100,  1100,  1100,  1200,  1200,  1200,  1200,  1200,  1200,
     1200,  1200,  1200,  1200,  1300,  1300,  1300,  1300,  1300,  1300,
     1300,  1400,  1400,  1400,  1400,  1400,  1400,  1400,  1500,  1500,
     1500,  1500,  1500,  1500,  1500,  1600,  1600,  1600,  1600,  1600,
     1600,  1600,  1600,  1600,  1600,  1700,  1700,  1700,  1700,  1700,
     1700,  1700,  1700,  1700,  1700,  1700,  1700,  1700,  1700,  1700,
     1700,  1700,  1600,  1600,  1600,  1600,  1500,  1500,  1400,  1400,
    // 1800 - 1899
     1370,  1340,  1310,  1290,  1270,  1260,  1250,  1250,  1250,  1250,
     1250,  1250,  1250,  1250,  1250,  1250,  1250,  1240,  1230,  1220,
     1200,  1170,  1140,  1110,  1060,  1020,   960,   910,   860,   800,
      750,   700,   660,   630,   600,   580,   570,   560,   560,   560,
      570,   580,   590,   610,   620,   630,   650,   660,   680,   690,
      710,   720,   730,   740,   750,   760,   770,   770,   780,   780,
      788,   782,   754,   697,   640,   602,   541,   410,   292,   182,
      161,    10,  -102,  -128,  -269,  -324,  -364,  -454,  -471,  -511,
     -540,  -542,  -520,  -546,  -546,  -579,  -563,  -564,  -580,  -566,
     -587,  -601,  -619,  -664,  -644,  -647,  -609,  -576,  -466,  -374,
    // 1900 - 1999
     -272,  -154,    -2,   124,   264,   386,   537,   614,   775,   913,
     1046,  1153,  1336,  1465,  1601,  1720,  1824,  1906,  2025,  2095,
     2116,  2225,  2241,  2303,  2349,  2362,  2386,  2449,  2434,  2408,
     2402,  2400,  2387,  2395,  2386,  2393,  2373,  2392,  2396,  2402,
     2433,  2483,  2530,  2570,  2624,  2677,  2728,  2778,  2825,  2871,
     2915,  2957,  2997,  3036,  3072,  3107,  3135,  3168,  3218,  3268,
     3315,  3359,  3400,  3447,  3503,  3573,  3654,  3743,  3829,  3920,
     4018,  4117,  4223,  4337,  4449,  4548,  4646,  4752,  4853,  4959,
     5054,  5138,  5217,  5296,  5379,  5434,  5487,  5532,  5582,  5630,
     5686,  5757,  5831,  5912,  5998,  6078,  6163,  6229,  6297,  6347,
    // 2000 - 2014 (2010 - 2014 are (wild) guesses)
     6383,  6409,  6430,  6447,  6457,  6469,  6485,  6515,  6546,  6578,
     6607,  6632,  6700,  6840,  7000
  };
  static final private int dtlength = dt.length;
  static final private int YSTART = 1620;
  static final private int YSTOP  = YSTART + dtlength - 1;  // (2014)
  static final private double[] d1 = new double[dtlength-1];
  static final private double[] d2 = new double[dtlength-1];
  static final private double[] d3 = new double[dtlength-1];
  static final private double[] d4 = new double[dtlength-1];
  static {
    int i;
    double u;

    // - May want to change table values before 1955.5 -
    // (See http://www.phys.uu.nl/~vgent/astro/deltatime.htm)
    // Table values before 1955.5 were reduced with Brown’s lunar theory with
    // an adopted lunar acceleration parameter (n') of -26.0 arcsec/cy/cy.
    // For other values of the lunar acceleration parameter, the values listed
    // above before 1955.5 should be corrected by:
    //   dt_new(centisec) = dt_old(centisec) - 91.072 * (n' + 26.0) * u^2
    // with u = (year - 1955.5)/100, or the time measured in centuries since
    // 1 July 1955.  Values after 1955.5 remain unchanged as they were obtained
    // from observations that were compared directly against Atomic Time (TAI).
    // (E.g. if n' = 25.8 and year = 1620, new dt[0] = 12400 - 205 = 12195)
    // Use n' = -25.7376 arcsec/cy/cy
    for ( i = 0; i <= 1955 - YSTART; i++ ) {
      u = (YSTART + i - 1955.5)/100;
      dt[i] -= 23.8973 * u * u;
    }

    for ( i = 0; i < dtlength-1; i++ ) // 1st differences
      d1[i] = dt[i+1] - dt[i];
    for ( i = 0; i < dtlength-2; i++ ) // 2nd differences
      d2[i] = d1[i+1] - d1[i];
    for ( i = 0; i < dtlength-3; i++ ) // 3rd differences
      d3[i] = d2[i+1] - d2[i];
    for ( i = 0; i < dtlength-4; i++ ) // 4th differences
      d4[i] = d3[i+1] - d3[i];

    // Set up linear interpolation at end
    d2[dtlength-2] = d3[dtlength-2] = d4[dtlength-2] = 0;

    // Set up quadratic interpolation next to end
    d2[dtlength-3] = (d2[dtlength-3] + d2[dtlength-4])/4;
    d1[dtlength-3] -= d2[dtlength-3];
    d4[dtlength-3]  = d3[dtlength-3] = 0;

    // Set up 4th order interpolation in middle
    for ( i = dtlength-4; i >=2; i-- ) { // Polynomial coefficients
      d4[i] = d4[i-1] + d4[i-2];
      d3[i] = d3[i-1];
      d2[i] = d2[i] + d2[i-1];
      d1[i] = d1[i] - d2[i]/4 + d3[i]/12 + d4[i]/24; // Coefficient of u
      d2[i] = d2[i]/4 - d3[i]/4 - d4[i]/48;          // Coefficient of u^2
      d3[i] = d3[i]/6 - d4[i]/24;                    // Coefficient of u^3
      d4[i] = d4[i]/48;                              // Coefficient of u^4
    }

    // Set up quadratic interpolation next to beginning
    d2[1] = (d2[1] + d2[0])/4;
    d1[1] -= d2[1];
    d4[1]  = d3[1] = 0;

    // Set up linear interpolation at beginning
    d2[0] = d3[0] = d4[0] = 0;
  }
}

/*------------------------------------------------------------------------------

The algorithm used is Bessel's Interpolation Formula, using up to the 4th
difference.  This allows calculation of values between two points using the
influence of 6 evenly spaced points, (2 on either side of the original 2).

       x0    y0
                   d1[0]
       x1    y1             d2[0]
                   d1[1]             d3[0]
       x2    y2             d2[1]             d4[0]
  u                d1[2]             d3[1]
       x3    y3             d2[2]             d4[1]
                   d1[3]             d3[2]
       x4    y4             d2[3]
                   d1[4]
       x5    y5

x values in this case represent successive years, y values are Delta T at
the beginning of the year.  u = fractional year.  d1 = 1st differences, ...

Delta T at year x2.u is:

  y = y2 + u(d1[2]) + u(u - 1)(d2[1] + d2[2])/4 + u(u - 1)(u - .5)(d3[1])/6
         + u(u + 1)(u - 1)(u - 2)(d4[0] + d4[1])/48

    = y2 + u(d1[2] - (d2[1] + d2[2])/4 + d3[1]/12 + (d4[0] + d4[1])/24)
         + u^2((d2[1] + d2[2])/4 - d3[1]/4 - (d4[0] + d4[1])/48)
         + u^3(d3[1]/6 - (d4[0] + d4[1])/24)
         + u^4((d4[0] + d4[1])/48)

Note that at u = 0:  x = x2, y = y2 exactly,
      and at u = 1:  x = x3, y = y3 exactly
The other 4 points cannot be met exactly since we are using a 4th order
polynomial to map 6 points.  (Which is as it should be.  A 5th order poly
could hit all 6 points, but could be subject to oscillations...)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

Additional documentation
------------------------

UTC = UT1 - deltaUT1
TAI = UTC + deltaAT
TT  = TAI + 32.184s

UT1 = Based on earth's (variable) rotation (corrected for polar motion).
UTC = Coordinated Universal Time, which is derived from atomic time,
      and designed to follow UT1 with +/- 0.9s (by adding leap seconds).
TAI = International Atomic Time, based on SI second.  Officially introduced
      in January 1972, but it's been available since 1955.
TT  = Terrestrial Time (replaced TDT, formerly Ephemeris Time) is the
      "theoretical timescale of apparent geocentric ephemerides of bodies
      in the solar system".  Used by NV to plot planets, ...

deltaUT1 is kept with +/- 0.9s by adding leap seconds to UTC.
deltaAT is the accumulated leap seconds, so it is an integer, and its
      value at J2000 is 32 (10 before Jan72 plus 22 after).
When a leap second occurs, 1 is added to both deltaUT1 and deltaAT.

From 1-1-1999 (last leap second of 1900s) to 1-1-2006 (first leap second
of 2000s) the following expression holds:
  TT = UTC + 64.184s
Thus J2000.0 (1-1-2000 12.0 TT) occurred at 1-1-2000 11:58:55.816 UTC.

From 1-1-2006 (last leap second) to whenever the next leap second is added
(none so far as of 1-17-2008), the following expression holds:
  TT = UTC + 65.184s

TT = UT1 + deltaT = UTC + deltaUT1 + deltaT

This program estimates deltaT, which is the difference between TT and
UT1 (UT).  Night Vision then accepts the computer time as UT1 (though
the computer should be at UTC if the clock has been set accurately) and
adds deltaT to get TT.  This results in a slight error of -deltaUT1.
Ideally Night Vision should use UTC for slightly better accuracy, but
that would entail adding the leap second table, which is only good for
past leap seconds, not future ones.

According to the table in this program, at J2000.0:
  TT = UT1 + 63.83s
Thus J2000.0 (1-1-2000 12.0 TT) occurred at 1-1-2000 11:58:56.17 UT1.

At J2000.0:
deltaUT1 = UT1 - UTC = (TT - 63.83) - (TT - 64.184) = .354 seconds
which agrees very closely with Internet sources.

Note:  This file uses UT and UT1 interchangeably.  However UT has 3
variations:
UT0 = Based on observations of stars from various ground stations.
UT1 = UT0 adjusted for polar motion.
UT2 = UT1 adjusted for seasonal variations (obsolete).

As an aside, GPS satellites use GPS time (which doesn't add leap seconds):
GPST = 0 at 0h UTC on 1-6-1980 (initial epoch)
     = TAI - 19s
From 1-6-1980 to 1-1-1999 13 leap seconds have occurred, thus
GPST = UTC + 13s   from 1-1-1999 till 1-1-2006.
GPST = UTC + 14s   from 1-1-2006 till 1-1-2009.
GPST = UTC + 15s   from 1-1-2009 till when next leap second occurs.

Leap second table
-----------------
 1972 JUL 1
 1973 JAN 1
 1974 JAN 1
 1975 JAN 1
 1976 JAN 1
 1977 JAN 1
 1978 JAN 1
 1979 JAN 1
 1980 JAN 1
 1981 JUL 1
 1982 JUL 1
 1983 JUL 1
 1985 JUL 1
 1988 JAN 1
 1990 JAN 1
 1991 JAN 1
 1992 JUL 1
 1993 JUL 1
 1994 JUL 1
 1996 JAN 1
 1997 JUL 1
 1999 JAN 1
 2006 JAN 1
 2009 JAN 1

Table generation
----------------

Extract column "Bull. A UT1-UTC" or "Bull. B UT1-UTC" for
Jan 1 from ftp://maia.usno.navy.mil/ser7/finals.all  (columns
defined in ftp://maia.usno.navy.mil/ser7/readme.finals).
Not sure which column is preferred, but they agree closely.
Fill in the following sequence:

1999:  64.184 -  .717 = 63.467    x 100 ~= 6347
2000:  64.184 -  .354 = 63.830    x 100 ~= 6383
2001:  64.184 -  .093 = 64.091    x 100 ~= 6409
2002:  64.184 - -.116 = 64.300    x 100 ~= 6430
2003:  64.184 - -.289 = 64.473    x 100 ~= 6447
2004:  64.184 - -.390 = 64.574    x 100 ~= 6457
2005:  64.184 - -.504 = 64.688    x 100 ~= 6469

2006:  65.184 -  .339 = 64.845    x 100 ~= 6485
2007:  65.184 -  .038 = 65.146    x 100 ~= 6515
2008:  65.184 - -.273 = 65.457    x 100 ~= 6546

2009:  66.184 -  .407 = 65.777    x 100 ~= 6578
2010:  66.184 -  .114 = 66.070    x 100 ~= 6607
2011:  66.184 - -.141 = 66.325    x 100 ~= 6632

The right hand column goes into the dt array above.

------------------------------------------------------------------------------*/

