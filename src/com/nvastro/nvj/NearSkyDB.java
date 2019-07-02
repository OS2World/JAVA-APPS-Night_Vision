/*
 * NearSkyDB.java  -  Near sky object database and methods
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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.StringTokenizer;
import java.util.Vector;


/* Methods from "Astronomical Algorithms" 2nd Ed. by Jean Meeus */
/* (c) 1998, second printing March 2000 by Willmann-Bell, Inc.  */

/*------------------------------------------------------------------------------
Precise definition of J2000.0 (found at http://www.answers.com/topic/j2000-0):
"It is precisely Julian date 2451545.0 TT, or January 1 2000, 12h TT.
This is equivalent to January 1 2000, 11:59:27.816 TAI or
January 1 2000, 11:58:55.816 UTC."    (Delta T = 64.184 seconds)
------------------------------------------------------------------------------*/

/*------------------------------------------------------------------------------
ICRS (& ICRF) vs FK5 (from http://aa.usno.navy.mil/faq/docs/ICRS_doc.html):
"The orientation of the ICRS axes is consistent with the equator and equinox
of J2000.0 represented by the FK5, within the errors of the latter. Since,
at J2000.0, the errors of the FK5 are significantly worse than those of
Hipparcos, the ICRS can be considered to be a refinement of the FK5 system
at (or near) that epoch."
"Because of its consistency with previous reference systems, implementation of
the ICRS will be transparent to any applications with accuracy requirements
of no better than 0.1 arcseconds near epoch J2000.0. That is, for applications
of this accuracy, the distinctions between the ICRS, FK5, and dynamical equator
and equinox of J2000.0 are not significant."
More info (from http://www.iers.org/iers/products/icrf/):
"The International Celestial Reference Frame (ICRF) realizes an ideal reference
system, the International Celestial Reference System (ICRS), by precise
equatorial coordinates of extragalactic radio sources observed in Very Long
Baseline Interferometry (VLBI) programmes. The Hipparcos catalogue which
includes all the FK5 stars was astrometically aligned to ICRF and provides
the primary realization of ICRS at optical wavelengths."
------------------------------------------------------------------------------*/


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Near sky object database and methods.
 *
 * @author Brian Simpson
 */
public class NearSkyDB {
  static private Vector<Planet> nsobjects = new Vector<Planet>();
  static private DecNumFormat au_format; // Format for displaying AU
  static private DecNumFormat km_format; // Format for displaying KM
  static private DecNumFormat mg_format; // Format for displaying planet mag
                 // mg_format is also used for displaying planet angular size
  static private DecNumFormat sm_format; // Format for displaying sun, moon size
  static private boolean geocentric = false;
  static private boolean initialized = false;
  static final private String NOTHING = "";
  static final private double D2R = MapParms.Deg2Rad; // Degrees to radians
  static final private double H2R = Math.PI / 12; // RA hours to radians
  static final private double S2R = Math.PI / 648000; // Seconds to radians
  /* A factor used for generating planet magnitudes */
  static final private double MAGFCTR = 5 / Math.log(10.0);
  /* Some constants for parallax adjustment */
  static final private double PAR = Math.sin(8.794 * S2R); // Parallax
                              // constant on P.279 (sin of 8.794")
  //atic final private double E = 6378140; // Equatorial radius in meters (P.82)
  static final private double P2E = 6356.755 / 6378.14; // Polar to equatorial
  static final public  double AU2KM = 149597870.691;

  private double JDate = -1;  // Jul. date for L, B, R, A, D, A2000, D2000, ...
  private double L;           // Heliocentric ecliptic longitude of earth (rad)
  private double B;           // Heliocentric ecliptic latitude of earth (rad)
  private double R;           // "Radius vector" of earth (AU)
  private double RcosBcosL;   // R * Math.cos(B) * Math.cos(L)
  private double RcosBsinL;   // R * Math.cos(B) * Math.sin(L)
  private double RsinB;       // R * Math.sin(B)
  // A, D, A2000, D2000 used only for moon phase
  private double A, D;        // Geocentric RA & Dec of Sun (rad)
  private double A2000, D2000;// Geocentric J2000 RA & Dec of Sun (rad)

  /* Some values for parallax adjustment */
  private Location loc = null;// Location for the following
  private double lstrad;      // LST in radians
  private double rhosinphip;  // Rho * sin of geocentric latitude (P. 82)
  private double rhocosphip;  // Rho * cos of geocentric latitude (P. 82)

  /* Variables used to suppress drawing objects */
  private boolean doneSetSuppress = false;
  private boolean suppress[] = { false, false, false, false, false,
                                 false, false, false, false, false };

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public NearSkyDB() {
    if ( initialized == false ) {
      init();
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialization.
   */
  private void init() {
    /* Add the planets */
    nsobjects.addElement(Planet.Earth);
    nsobjects.addElement(Planet.Mercury);
    nsobjects.addElement(Planet.Venus);
    nsobjects.addElement(Planet.Mars);
    nsobjects.addElement(Planet.Jupiter);
    nsobjects.addElement(Planet.Saturn);
    nsobjects.addElement(Planet.Uranus);
    nsobjects.addElement(Planet.Neptune);
    nsobjects.addElement(Planet.EJ2000);   // (for Pluto)

    nsobjects.trimToSize();
    au_format = new DecNumFormat("0.000 "); // (rounds)
    km_format = new DecNumFormat("000,000 "); // (rounds)
    mg_format = new DecNumFormat("0.0"); // (rounds)
    sm_format = new DecNumFormat("0.00"); // (rounds)
    if ( Preferences.geocentric ) geocentric = true;
    initialized = true;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns ra, &amp; dec (J2000 or apparent), and distance for specified
   * object.  If apparent coordinates are desired, corrections for precession,
   * nutation, light-time, aberration, and parallax are applied.  If J2000
   * coordinates are desired, corrections for light-time and parallax are
   * applied.
   *
   * @param object Object whose coordinates are desired
   *               <br>0 = Mercury, ..., 7 = Pluto, 8 = Sun, 9 = Moon
   * @param mp Mapping parameters (includes Julian date/time, ecliptic)
   * @param app If true, return apparent coordinates, else J2000
   * @param ra On return, has right ascension in radians
   * @param dec On return, has declination in radians
   * @param dist On return, has distance from earth in AU
   * @param sdist On return, has distance from sun in AU (planets only)
   */
  private void getCoordinates(int object, MapParms mp, boolean app,
                              double[] ra, double[] dec,
                              double[] dist, double[] sdist) {
    double[] l = new double[1];
    double[] b = new double[1];
    double[] r = new double[1];
    double x, y, z;  // All in AU
    double cosl, sinl, cosb, sinb;
    double tmpx, tmpy;
    double j; // Julian date
    double t; // Julian millennia from epoch J2000.0
    double[] lambda = new double[1];
    double[] beta   = new double[1];

    if ( object < 0 || object > 9 ) return;

    j = mp.lst.getJulianEphDay();
    t = (j - 2451545.0) / 365250; // Julian millennia from J2000.0

    /* Calculate earth coordinates and sun RA & Dec (if necessary) */
    if ( JDate != j || loc != mp.lst.getLocation() ) {
      JDate = j;
      Planet.Earth.calcHelioCentricCoord(t, l, b, r);
      //stem.out.println("earth l = " + (l[0] / D2R) + " degrees");
      //stem.out.println("earth b = " + (b[0] / D2R) + " degrees");
      L = l[0];  B = b[0];  R = r[0];
      double cosB = Math.cos(B);
      RcosBcosL = R * cosB * Math.cos(L);
      RcosBsinL = R * cosB * Math.sin(L);
      RsinB     = R * Math.sin(B);

      // Calculate Sun RA & Dec (reverse earth's coords) for moon phase
      // See "Higher accuracy" method in chap. 25  (L & B precessed)
      mp.convEclipToEquat(L + Math.PI, -B, ra, dec); // Handles nutation
      // A, D, A2000, D2000 used only for moon phase
      // (Could do FK5 conversion, but this slight correction probably
      // not worth it.  Can also correct for light-time, parallax...)
      A = ra[0]; D = dec[0];          // Sun's precessed nutated coordinates
      mp.unPrecessNutate(ra, dec);             // Convert to J2000
      A2000 = ra[0]; D2000 = dec[0];  // Sun's J2000 coordinates

      /* Set up parallax variables (See P. 81-82) */
      loc = mp.lst.getLocation();
      lstrad = mp.lst.getLSTHrs() * H2R; // LST in radians
      double phi = mp.lst.getLatDeg() * D2R; // Geographic latitude
      double u = Math.atan(Math.tan(phi) * P2E);
      // Ht is meters above sea level (when I get it implemented...)
      rhosinphip = P2E * Math.sin(u); // + Ht * Math.sin(phi) / E;
      rhocosphip = Math.cos(u); // + Ht * Math.cos(phi) / E;
    }

    /* Determine coordinates of object */
    if ( object >= 0 && object <= 7 ) { /* If object = planet */
      nsobjects.elementAt(object+1).calcHelioCentricCoord(t, l, b, r);
      cosl = Math.cos(l[0]);
      sinl = Math.sin(l[0]);
      cosb = Math.cos(b[0]);
      sinb = Math.sin(b[0]);

      if ( object < 7 ) {  // If planet (other than Pluto), Using Chap. 33, P225
        x = r[0] * cosb * cosl - RcosBcosL;
        y = r[0] * cosb * sinl - RcosBsinL;
        z = r[0] * sinb - RsinB;
        dist[0] = Math.sqrt(x*x + y*y + z*z); // Distance from earth

        // 2nd pass to adjust for light-time
        t -= dist[0] * 1.5812507324e-8; // (0.0057755183 / 365250)
        nsobjects.elementAt(object+1).calcHelioCentricCoord(t,l,b,r);
        cosl = Math.cos(l[0]);
        sinl = Math.sin(l[0]);
        cosb = Math.cos(b[0]);
        sinb = Math.sin(b[0]);
        x = r[0] * cosb * cosl - RcosBcosL;
        y = r[0] * cosb * sinl - RcosBsinL;
        z = r[0] * sinb - RsinB;
        dist[0] = Math.sqrt(x*x + y*y + z*z); // Distance from earth
        sdist[0] = r[0];                      // Distance from sun

        // Calculate ecliptical coordinates
        lambda[0] = Math.atan2(y, x); // No problem if x = 0;     Precessed
        beta[0] = Math.atan(z / Math.sqrt(x * x + y * y));   // Coordinates
        // Adjust for aberration
        if ( app ) mp.adjustEclipForAberration(lambda, beta);
        // FK5 conversion
        convToFK5(j, lambda, beta);
        // Convert to equatorial
        mp.convEclipToEquat(lambda[0], beta[0], ra, dec); // Handles nutation
      }
      else {               // Else Pluto, Using Chap. 37, P266 (& P172-175)
        x = -r[0] * cosb * cosl;   // Convert
        y = -r[0] * cosb * sinl;   //   EJ2000 to
        z = -r[0] * sinb;          //     SunJ2000

        /* Convert sun coord's from "ecliptical dynamical reference from (VSOP)
           of J2000.0" to the "equatorial FK5 J2000.0 reference frame" P.174 */
        tmpx = x + 0.000000440360 * y - 0.000000190919 * z;
        tmpy = -0.000000479966 * x + 0.917482137087 * y - 0.397776982902 * z;
        z = 0.397776982202 * y + 0.917482137087 * z;
        y = tmpy; x = tmpx;

        /* Get Pluto's J2000 coordinates */
        Pluto.calcPluto2000(j, x, y, z, ra, dec, dist, sdist);

        mp.precessNutate(ra, dec);
        if ( app ) mp.adjustEquatForAberration(ra, dec);
      }
    }
    else if ( object == 8 ) {/* Else if object = Sun (Chap 25, P166) */
      dist[0] = R;
      lambda[0] = L + Math.PI;  // Reverse earth's
      beta[0] = -B;             //   coordinates
      // FK5 conversion
      convToFK5(j, lambda, beta);
      // Adjust for aberration
      if ( app ) mp.adjustEclipForAberration(lambda, beta);
      // Convert to equatorial
      mp.convEclipToEquat(lambda[0], beta[0], ra, dec); // Handles nutation
    }
    else {                   /* Else object = moon (i.e. object = 9) (P. 337) */
      Moon.getCoordinates(j, lambda, beta, dist);       // Precessed coordinates
      mp.convEclipToEquat(lambda[0], beta[0], ra, dec); // Handles nutation
      // Apparently light-time adjustment not needed
      // FK5 adjustment does not apply
      // No adjustment for aberration needed, as moon moves with earth about sun
    }

    // Adjust for parallax
    adjustMoonForParallax(ra, dec, dist);
    if ( ! app ) mp.unPrecessNutate(ra, dec);           // Convert to J2000
  }

  ///* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
  // * Adjusts geocentric coordinates into topocentric coordinates. (P. 279)
  // *
  // * @param ra Input: RA before adjust; Output: RA after adjust
  // * @param dec Input: Declination before adjust; Output: Dec after adjust
  // * @param dist Distance in AU
  // */
  //private void adjustForParallax(double[] ra, double[] dec, double dist) {
  //  if ( geocentric ) { return; }
  //  double sinpi = PAR / dist;
  //  //stem.out.println("pi = " + (Math.asin(sinpi) / D2R));
  //  double hrangle = lstrad - ra[0]; // Hour angle in radians
  //  //stem.out.println("hrangle = " + (hrangle / D2R));
  //  double coshrangle = Math.cos(hrangle);
  //  double cosdelta = Math.cos(dec[0]);
  //  double dalpha = Math.atan2(-rhocosphip * sinpi * Math.sin(hrangle),
  //         cosdelta - rhocosphip * sinpi * coshrangle);
  //  ra[0] += dalpha;
  //  //stem.out.println("dalpha = " + (dalpha * / S2R));
  //  dec[0] = Math.atan2(Math.cos(dalpha) *
  //           (Math.sin(dec[0]) - rhosinphip * sinpi),
  //           cosdelta - rhocosphip * sinpi * coshrangle);
  //}

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * See previous functions for comments.
   */
  private void adjustMoonForParallax(double[] ra, double[] dec, double[] dist) {
    if ( geocentric ) { return; }
    double sinpi = PAR / dist[0];
    //stem.out.println("pi = " + (Math.asin(sinpi) / D2R));
    double hrangle = lstrad - ra[0]; // Hour angle in radians
    //stem.out.println("hrangle = " + (hrangle / D2R));
    double coshrangle = Math.cos(hrangle);
    double sinhrangle = Math.sin(hrangle);
    double cosdelta = Math.cos(dec[0]);
    double sindelta = Math.sin(dec[0]);
    double dalpha = Math.atan2(-rhocosphip * sinpi * sinhrangle,
           cosdelta - rhocosphip * sinpi * coshrangle);
    ra[0] += dalpha;
    //stem.out.println("dalpha = " + (dalpha * / S2R));
    dec[0] = Math.atan2(Math.cos(dalpha) *
             (sindelta - rhosinphip * sinpi),
             cosdelta - rhocosphip * sinpi * coshrangle);

    // P. 280
    double A = cosdelta * sinhrangle;
    double B = cosdelta * coshrangle - rhocosphip * sinpi;
    double C = sindelta - rhosinphip * sinpi;
    double q = Math.sqrt(A * A + B * B + C * C);
    dist[0] *= q; // P. 391
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * FK5 conversion.  (P.219)
   *
   * @param julian Julian day
   * @param lambda Input: Eclip. longitude in radians before adjust;
   *               Output: Eclip. longitude in radians after adjust
   * @param beta Input: Eclip. latitude in radians before adjust;
   *             Output: Eclip. latitude in radians after adjust
   */
  private void convToFK5(double julian, double[] lambda, double[] beta) {
    double T = (julian - 2451545.0) / 36525; // Jul. cent. from J2000.0
    double lp = lambda[0] - T * (1.397 + T * 0.00031) * D2R;
    double coslp = Math.cos(lp);
    double sinlp = Math.sin(lp);
    double tanb  = Math.tan(beta[0]);
    lambda[0] += (0.03916 * (coslp + sinlp) * tanb - 0.09033) * S2R;
    beta[0] += 0.03916 * (coslp - sinlp) * S2R;
    //stem.out.println("dl = " + (0.03916 * (coslp + sinlp) * tanb - 0.09033));
    //stem.out.println("db = " + (0.03916 * (coslp - sinlp)));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets heliocentric coordinates for solar system view.
   *
   * @param p Planet (0 - 8  for  Mercury - Pluto)
   * @param t Julian millennia from J2000.0
   * @param l On return, has heliocentric ecliptical longitude in radians
   * @param b On return, has heliocentric ecliptical latitude in radians
   * @param r On return, has radius vector in AU
   */
  public void getHCCoordinates(int p, double t, double[] l,
                               double[] b, double[] r) {
    if ( p < 0 || p > 8 ) {
      l[0] = b[0] = r[0] = 0;
      return;
    }

    if ( p == 2 ) p = 0;   // Earth is first element in nsobjects
    else if ( p < 2 ) p++; // Mercury and Venus are next two elements

    if ( p < 8 )
      nsobjects.elementAt(p).calcHelioCentricCoord(t, l, b, r);
    else /* p == 8 (Pluto) */
      Pluto.calcHelioCentricCoord(t, l, b, r);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns planet magnitude.  (P.283-286)
   *
   * @param object Planet number (M=0 V=1 M=2 J=3 S=4 U=5 N=6 P=7)
   * @param edist Distance from earth
   * @param sdist Distance from sun
   * @param mp Mapping parameters (For Saturn only - need Julian day)
   * @param sc SphereCoords of planet (RA,Dec - for Saturn only)
   */
  private String getPlanetMag(int object, double edist, double sdist,
                              MapParms mp, SphereCoords sc) {
    double sedist = sdist * edist;
    double i = (sdist * sdist + edist * edist - R * R) / (2 * sedist);
    i = Math.max(-1.0, Math.min(i, 1.0)); // Make sure -1 <= i <= 1
    i = Math.acos(i) / D2R;  // Degrees

    double mag = MAGFCTR * Math.log(sedist);
    if      ( object == 0 )  // Mercury
      mag += -0.42 + i * (0.0380 - i * (0.000273 - i * 0.000002));
    else if ( object == 1 )  // Venus
      mag += -4.40 + i * (0.0009 + i * (0.000239 - i * 0.00000065));
    else if ( object == 2 )  // Mars
      mag += -1.52 + i * 0.016;
    else if ( object == 3 )  // Jupiter
      mag += -9.40 + i * 0.005;
    else if ( object == 4 ) {// Saturn
      mag += -8.88;

      // See Chap. 45 - Ring of Saturn
      double T = (mp.lst.getJulianEphDay() - 2451545.0) / 36525; // Jul. cent.
      double I     =  28.075216 - T * (0.012998 - T * 0.000004); // Ring incl.
      double Omega = 169.508470 + T * (1.394681 + T * 0.000412); // Asc. node

      // Convert coordinates from equatorial to ecliptical
      mp.convEquatToEclip(sc);
      // B is the Saturnicentric latitude of the Earth (sinb = sin(B))
      double sinB = Math.sin(I * D2R) * Math.cos(sc.getBeta()) *
                    Math.sin(sc.getLambda() - Omega * D2R) -
                    Math.cos(I * D2R) * Math.sin(sc.getBeta());
                    //stem.out.println("B = " + (Math.asin(sinB) / D2R));
                    //stem.out.println("i = " + i);
                    //stem.out.println("edist = " + edist);
                    //stem.out.println("sdist = " + sdist);
      // Since B is well within +/- 90 degrees, we can assume that
      // sin|B| will always be positive.  Ensure sinB is positive:
      sinB = Math.abs(sinB);

      // Using formula on P. 286, with i as an approx. of deltaU
      mag += 0.044 * Math.abs(i) - sinB * (2.60 - sinB * 1.25);
    }
    else if ( object == 5 )  // Uranus
      mag += -7.19;
    else if ( object == 6 )  // Neptune
      mag += -6.87;
    else if ( object == 7 )  // Pluto
      mag += -1.00;
    else return "";

    return mg_format.format(mag);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns angular size in seconds.  (Chapter 55)
   *
   * @param object Planet number (M=0 V=1 M=2 J=3 S=4 U=5 N=6 P=7 S=8 M=9)
   * @param dist Distance from earth in AUs
   */
  private String getAngularSize(int object, double dist) {
    double s = 0.0;

    switch ( object ) {
      case 0:    // Mercury
        s = 3.36;
        break;
      case 1:    // Venus
        s = 8.41;   // Includes clouds; use 8.34 for crust
        break;
      case 2:    // Mars
        s = 4.68;
        break;
      case 3:    // Jupiter
        s = 98.44;
        break;
      case 4:    // Saturn
        s = 82.73;
        break;
      case 5:    // Uranus
        s = 35.02;
        break;
      case 6:    // Neptune
        s = 33.50;
        break;
      case 7:    // Pluto
        s = 2.07;
        break;
      case 8:    // Sun
        s = 959.63;
        break;
      case 9:    // Moon
        s = 358473400 / AU2KM; // P. 391
        break;
    }
    if ( object >= 8 )
      return sm_format.format(2 * s / dist / 60) + IdentifyDlg.Min;
    else
      return mg_format.format(2 * s / dist) + IdentifyDlg.Sec;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the number of near sky objects.
   */
  public int getNumberOfObjects() { return 10; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the name of the object.
   *
   * @param object Index into near sky database
   * @return "" if out of range
   */
  public String tellName(int object) {
    if ( object >= 0 && object < 8 )
      return nsobjects.elementAt(object + 1).getName();
    else if ( object == 8 )
      return TextBndl.getString("NS.Sun");
    else if ( object == 9 )
      return TextBndl.getString("NS.Moon");
    else
      return NOTHING;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the type of object.
   *
   * @param object Index into near sky database
   * @return "" if out of range
   */
  public String tellType(int object) {
    if ( object >= 0 && object < 8 )
      return IdentifyDlg.PLANET;
    else if ( object == 8 )
      return IdentifyDlg.STAR;
    else if ( object == 9 )
      return IdentifyDlg.MOON;
    else
      return NOTHING;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the J2000 location.
   * Called by SkyObject.
   *
   * @param i Index into near sky database
   * @param mp Mapping parameters (includes Julian date/time, ecliptic)
   * @return Coordinates of object (containing RA/Dec in radians)
   */
  public SphereCoords getJ2000Location(int i, MapParms mp) {
    double[] ra    = new double[1];
    double[] dec   = new double[1];
    double[] dist  = new double[1];
    double[] sdist = new double[1];

    if ( i < 0 || i >= 10 ) return new SphereCoords(0.0, 0.0);
    getCoordinates(i, mp, false, ra, dec, dist, sdist);
    return new SphereCoords(ra[0], dec[0]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the apparent location.
   * Called by SkyObject.
   *
   * @param i Index into near sky database
   * @param mp Mapping parameters (includes Julian date/time, ecliptic)
   * @param J2000Coords If non-null on input, returns J2000 coordinates
   * @return Coordinates of object (containing RA/Dec in radians)
   */
  public SphereCoords getAppLocation(int i, MapParms mp,
                                     SphereCoords J2000Coords) {
    double[] ra    = new double[1];
    double[] dec   = new double[1];
    double[] dist  = new double[1];
    double[] sdist = new double[1];

    if ( i < 0 || i >= 10 ) return new SphereCoords(0.0, 0.0);
    if ( J2000Coords != null ) {
      getCoordinates(i, mp, false, ra, dec, dist, sdist);
      J2000Coords.set(ra[0], dec[0]);
    }
    getCoordinates(i, mp, true, ra, dec, dist, sdist);
    return new SphereCoords(ra[0], dec[0]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Finds nearest near sky object to specified coordinates.
   *
   * @param mp Mapping parameters
   * @param sc Specified coordinates
   * @param sep On return, set at angular distance in radians from sc
   * @param str On return, contains text on object
   * @return Index of object, or -1
   */
  public int findNearestNS(MapParms mp, SphereCoords sc, double[] sep,
                           StringBuffer str) {
    float[] x = new float[1];    // dummy variable
    float[] y = new float[1];    // dummy variable
    double dtmp;
    double ra = sc.getRA();
    double dec = sc.getDec();
    double[] r1 = new double[1];
    double[] d1 = new double[1];
    double[] dist = new double[1];
    double[] sdist = new double[1];
    double distance = 0, // Distance from earth
           sundist = 0;  // Distance from sun
    SphereCoords radec = new SphereCoords();

    int close = -1;                       // No close object found (yet)
    if ( ! mp.prefer.drawNearSky() ) return close;

    /* Planets have radius of 3 pels, Sun & Moon have radius of 5 pels */
    /* Reject objects farther than 4 pels */
    sep[0] = 4 / mp.pelsPerRadian;   // Radians

    for ( int i = 0; i < 10; i++ ) {
      getCoordinates(i, mp, false, r1, d1, dist, sdist);

      dtmp = MapParms.separation(r1[0], d1[0], ra, dec);
      if ( sep[0] > dtmp && mp.rd2xyhit(r1[0], d1[0], x, y) == 1 ) {
        sep[0] = dtmp;
        distance = dist[0];
        sundist = sdist[0];
        radec.set(r1[0], d1[0]);
        close = i;
      }
    }

    if ( close >= 0 ) {
      str.append(IdentifyDlg.TYPE + tellType(close) + "\n");

      str.append(IdentifyDlg.NAME + tellName(close) + "\n");


      if ( close == 8 ) {      // Sun = 8
        str.append(IdentifyDlg.SPECT + "G2\n"); // G2V
        str.append(IdentifyDlg.MAG + "-27\n");  // 26.7, 26.74, 26.8, ...
      }
      else if ( close == 9 ) { // Moon = 9
        // Could use a DecimalFormat to format to a pattern of "0%",
        // but not sure if 0 and 100 can be easily discerned below
        // over full range of locales...
        int phase = (int) // phase (illumination) is in percent
                    Math.round(Moon.getIllumFrac(r1[0],d1[0],A2000,D2000) *100);
        str.append(IdentifyDlg.ILLUM + phase + TextBndl.getPercentChar() + " ");
        double diff = (r1[0] - A2000) % (2 * Math.PI);
        if ( diff < 0 ) diff += (2 * Math.PI);
        if ( phase == 100 )
          str.append(IdentifyDlg.FULL + "\n");
        else if ( phase == 0 )
          str.append(IdentifyDlg.NEW + "\n");
        else if ( diff < Math.PI )
          str.append(IdentifyDlg.WAX + "\n");
        else
          str.append(IdentifyDlg.WANE + "\n");
      }
      else {                   // Planet
        str.append(IdentifyDlg.MAG +
                   getPlanetMag(close, distance, sundist, mp, radec) + "\n");
      }

      str.append(IdentifyDlg.DIST);
      if ( close == 9 ) {      // Moon = 9
        str.append(km_format.format(distance*AU2KM) + IdentifyDlg.DISTKM +"\n");
      } else {                 // Sun or planet
        str.append(au_format.format(distance) + IdentifyDlg.DISTAU + "\n");
      }

      // Add Angular size
      str.append(IdentifyDlg.ANGSZ);
      str.append(getAngularSize(close, distance) + "\n");
    }
    return close;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up list of objects to suppress (i.e. not draw).
   */
  private void setSuppress(MapParms mp) {
    StringTokenizer t = new StringTokenizer(mp.prefer.nSkySuppress);

    while ( t.hasMoreElements() ) {
      String s = t.nextToken().toLowerCase();
      // Note: Keep numbering in sync with draw function
      if      ( s.equals("mercury") ) suppress[0] = true;
      else if ( s.equals("venus") )   suppress[1] = true;
      else if ( s.equals("mars") )    suppress[2] = true;
      else if ( s.equals("jupiter") ) suppress[3] = true;
      else if ( s.equals("saturn") )  suppress[4] = true;
      else if ( s.equals("uranus") )  suppress[5] = true;
      else if ( s.equals("neptune") ) suppress[6] = true;
      else if ( s.equals("pluto") )   suppress[7] = true;
      else if ( s.equals("sun") )     suppress[8] = true;
      else if ( s.equals("moon") )    suppress[9] = true;
    }

    doneSetSuppress = true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the near sky objects.
   *
   * @param mp Mapping parameters
   */
  public void draw(MapParms mp) {
    float[] x = new float[1];
    float[] y = new float[1];
    int i, s, o, t;
    double[] ra    = new double[1];
    double[] dec   = new double[1];
    double[] dist  = new double[1];
    double[] sdist = new double[1];
    double[] nearra  = new double[2];
    double[] neardec = new double[2];
    boolean near[] = { false, false };

    if ( !mp.prefer.drawNearSky() || !mp.isDrawing() ) return;

    mp.g.setColor(mp.printing ? mp.prefer.prclrPlanet() :
                                mp.prefer.colorPlanet());
    s = 8;
    mp.g.setFont(mp.prefer.fontSolarSys());
    FontMetrics fm = mp.g.getFontMetrics();
    int yoffset = fm.getDescent() + 1;  // Descent + 1 pixel

    if ( !doneSetSuppress ) setSuppress(mp);

    // Do in this order:  7 6 5 4 3 2 1 0 8 0 1 9
    // (Pluto, ..., Mars, Venus, Mercury, Sun, Mercury, Venus, Moon)
    // so that nearer objects are drawn over farther objects
    // Note: Keep numbering in sync with setSuppress function
    for ( int j = 7; j >= -4; j-- ) {
      if      ( j >=  0 ) i = j;
      else if ( j == -1 ) {
        i = 8;    // Sun
        s = 12;
        mp.g.setColor(mp.printing ? mp.prefer.prclrSun() :
                                    mp.prefer.colorSun());
      }
      else if ( j == -2 ) {
        i = 0;    // Mercury
        s = 8;
        mp.g.setColor(mp.printing ? mp.prefer.prclrPlanet() :
                                    mp.prefer.colorPlanet());
      }
      else if ( j == -3 ) {
        i = 1;    // Venus
      }
      else    /*j == -4*/ {
        i = 9;    // Moon
        s = 12;
        mp.g.setColor(mp.printing ? mp.prefer.prclrMoon() :
                                    mp.prefer.colorMoon());
      }
      if ( suppress[i] ) continue;

      /* Calculate coordinates (ra, dec) */
      if ( j != -2 && j != -3 ) {   // If not 2nd pass for Mercury or Venus
        getCoordinates(i, mp, true, ra, dec, dist, sdist);
        if ( (i == 0 || i == 1) &&  // If 1st pass for Mercury or Venus
             dist[0] < 1 ) {        //   and nearer than Sun
          near[i] = true;
          nearra[i]  = ra[0];
          neardec[i] = dec[0];
          continue;
        }
      }
      else {                        // Else 2nd pass for Mercury or Venus
        if ( near[i] == false ) continue;
        ra[0] = nearra[i];
        dec[0] = neardec[i];
      }

      /* Convert coordinates to (x, y) and draw if in window */
      if ( mp.rd2xyhit(ra[0], dec[0], x, y, true) > 0 ) {
        // If painting, turn on antialiasing to draw circle
        if ( !mp.printing )
          mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

        // Note:  To render properly, painted circles must have a
        // diameter of s+1, while printed circles must have a diameter
        // of s.  (At least for fillOval())  Not sure why...
        o = s / 2;
        t = mp.printing ? s : s + 1;
        if ( i == 9 ) {         // If moon
          float[] x2 = new float[1];   // (x, y) location
          float[] y2 = new float[1];   //   for 1 degree north
          mp.rd2xydist(ra[0], dec[0] + 0.018, x2, y2, true); // 1 degree north
          drawPhasedObject(x[0], y[0], t,
                           Moon.getIllumFrac(ra[0], dec[0], A, D),
                           Math.atan2(y[0] - y2[0], x[0] - x2[0]) -
                           Moon.getPositionAngle(ra[0], dec[0], A, D),
                           mp.g);
        }
        else if ( i == 8 ) {    // Else if sun
          // Use same method as moon (for same size) w/o phase & rotation
          drawPhasedObject(x[0], y[0], t, 1.0, 0.0, mp.g);
        }
        else {                  // Else planet
          //mp.g.fillOval((int)(x[0] - o + 0.5f), (int)(y[0] - o + 0.5f), t, t);
          drawPhasedObject(x[0], y[0], t, 1.0, 0.0, mp.g);
        }

        // If painting and antialiasing is off, turn it off for text
        if ( !mp.printing && !mp.prefer.antialiasing )
          mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_OFF);

        mp.g.drawString(tellName(i), x[0] - fm.stringWidth(tellName(i)) / 2,
                        y[0] - o - yoffset);
      }
    }
    // If painting, make sure antialiasing is off (it will be turned on
    if ( !mp.printing )                            // as needed)
      mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw phased object (Sun, moon, planets; phase for moon only).
   *
   * @param x x location
   * @param y y location
   * @param s size
   * @param i percent illuminated
   * @param a angle
   * @param g2 2D graphics
   */
  private void drawPhasedObject(float x, float y, int s, double i, double a,
                                Graphics2D g2) {
    // It seemed like a good idea to snap the coordinates to the
    // nearest pixel so that ID crosshairs are centered, but then
    // during eclipse the sun and moon bounce around relative
    // to each other, so decided against it...
    // x = (int)(x + 0.5f);
    // y = (int)(y + 0.5f);

    Ellipse2D.Float ellipse = new Ellipse2D.Float();
    float h = s/2;
    i = Math.max(0, Math.min(i, 1));  // 0 <= i <= 1

    AffineTransform trans = g2.getTransform();
    g2.translate(x, y);
    g2.rotate(a);
    ellipse.setFrame(-h, -h, s, s);
    Area a1 = new Area(ellipse);
    g2.fill(a1);
    if ( i < .95 && Preferences.mnphases ) {
      Color c = g2.getColor();
      g2.setColor(new Color(c.getRed()/2,c.getGreen()/2,c.getBlue()/2));
      if ( i > .05 ) {
        Area a2 = new Area(new Rectangle2D.Float(-h, -h, h, s));
        a1.subtract(a2);
        if ( i < 0.45 || i > 0.55 ) {
          a2.reset();
          if ( i < 0.5 ) {
            ellipse.setFrame(-h + s * (i), -h, s * (1 - 2 * i), s);
            a2.add(new Area(ellipse));
            a1.add(a2);
          } else {
            ellipse.setFrame(-h + s * (1 - i), -h, s * (2 * i - 1), s);
            a2.add(new Area(ellipse));
            a1.subtract(a2);
          }
        }
      }
      g2.fill(a1);
      g2.setColor(c); // Reset color for drawing name
    }
    g2.setTransform(trans);
  }

  /* For testing */
  //static public void main(String[] args) {
  //  SphereCoords sc = new SphereCoords();
  //  double[] ra    = new double[1];
  //  double[] dec   = new double[1];
  //  double[] dist  = new double[1];
  //  double[] sdist = new double[1];
  //  Preferences prefer = new Preferences();
  //  GregorianCalendar gc = prefer.lst.getLocDateTime();
  //  gc.setTimeZone(TimeZone.getTimeZone("GMT"));  // "GMT" is a misnomer
  //  gc.clear(); // Clear time fields
  //  gc.set(1992, 11, 20, 0, 0); // Year, month (0 based), day, hour, minute
  //  prefer.lst.stop();
  //  prefer.lst.setLocDateTime(); // Uses gc; not sure how DST is handled
  //  MapParms mp = new MapParms(prefer);
  //  mp.update(new Dimension(100, 100), 100.0); // Bogus numbers
  //  geocentric = true;  // Do Geocentric analysis
  //  Preferences.usedeltat = false;  // Delta T = 0
  //  // 2448908.5 = 1992 October 13.0 TT (Terrestrial Time)
  //  // 2448976.5 = 1992 December 20.0 TT (for Venus - P. 225-227)
  //  // 2448724.5 = 1992 April 12.0 TT (for Moon - P. 342-343)
  //  // 2452879.636805556 = 2003 August 28 3h17m UT (P. 280)
  //  // 2446895.5 = 1987 April 10.0 TT (P. 148)
  //  // 2086308.0 = 1000-Jan-01 12:00
  //  // 2268933.0 = 1500-Jan-01 12:00
  //  // 2415021.0 = 1900-Jan-01 12:00
  //  // 2451545.0 = 2000-Jan-01 12:00
  //  // 2488070.0 = 2100-Jan-01 12:00
  //  // 2634167.0 = 2500-Jan-01 12:00
  //  // 2816423.0 = 2999-Jan-01 12:00
  //
  //  // Report date/time, Julian, & delta t
  //  System.out.println("Date/time = " + mp.lst.tellLocDateTime(false));
  //  double jday = mp.lst.getJulianDay();
  //  System.out.println("Julian Day = " + jday);
  //  System.out.println("Delta t = " + DeltaT.calcDeltaT(jday) + " seconds");
  //  System.out.println("Julian Ephemeris Day = " + mp.lst.getJulianEphDay());
  //
  //  NearSkyDB db =  new NearSkyDB();
  //  for ( int i = 0; i < db.getNumberOfObjects(); i++ ) {
  //    System.out.println("- " + db.tellName(i) + " -");
  //
  //    db.getCoordinates(i, mp, true, ra, dec, dist, sdist);  // Apparent
  //    System.out.println("Dist = " + dist[0] + " AU");
  //    sc.set(ra[0], dec[0]);
  //
  //    if ( ra[0] < 0 ) ra[0] += 2*Math.PI;
  //    System.out.println("RA   = " + (ra[0] / D2R) + " degrees");
  //    System.out.println("Dec  = " + (dec[0] / D2R) + " degrees");
  //    System.out.println(sc.tellRAHrMnScT() + ", " + sc.tellDecDgMnSc());
  //
  //    db.getCoordinates(i, mp, false, ra, dec, dist, sdist); // J2000
  //    sc.set(ra[0], dec[0]);
  //
  //    if ( ra[0] < 0 ) ra[0] += 2*Math.PI;
  //    System.out.println("RA   = " + (ra[0] / D2R) + " degrees");
  //    System.out.println("Dec  = " + (dec[0] / D2R) + " degrees");
  //    System.out.println(sc.tellRAHrMnScT() + ", " + sc.tellDecDgMnSc());
  //  }
  //}
}


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Helper class for planetary coordinates.
 *
 * @author Brian Simpson
 */
class lbr {
  public double a, b, c;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  lbr(long a, double b, double c) {
    this.a = (double)a;
    this.b = b;
    this.c = c;
  }
}


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Class for Planetary coordinates.
 *
 * @author Brian Simpson
 */
class Planet {
  static final private double PI2 = Math.PI * 2;
  private lbr[][][] data;
  private String name;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param name Name of planet
   * @param data lbr data for planet (from "Astronomical Algorithms" book)
   */
  Planet(String name, lbr[][][] data) {
    this.name = TextBndl.getString(name);
    this.data = data;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the planet's name.
   *
   * @return Name of planet
   */
  public String getName() { return name; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates heliocentric coordinates.
   *
   * @param t Julian millennia from J2000.0
   * @param l On return, has heliocentric ecliptical longitude in radians
   * @param b On return, has heliocentric ecliptical latitude in radians
   * @param r On return, has radius vector in AU
   */
  public void calcHelioCentricCoord(double t, double[] l, double[] b,
                                    double[] r) {
    for ( int i = 0; i < 3; i++ ) {
      double sum = 0;
      for ( int j = data[i].length - 1; j >= 0; j-- ) {
        sum *= t;
        for ( int k = data[i][j].length - 1; k >= 0; k-- )
          sum += data[i][j][k].a * Math.cos(data[i][j][k].b +
                                            data[i][j][k].c * t);
          // Note:  Tried %'ing the arg of cos with PI2, with jul = 2996000.0
          // (~ 3490AD), and difference to final RA/Dec was in 14th dec. place
          // -> Perhaps can get rid of most/all of %180 and %PI2
      }
      sum /= 1e8;
      if ( i == 0 ) {
        sum = sum % PI2; // 2 % 3 = 2, (-2) % 3 = -2
        if ( sum < 0 ) sum += PI2;
        l[0] = sum;
      }
      else if ( i == 1 ) b[0] = sum;
      else               r[0] = sum;
    }
  }

  static final private lbr earth[][][] = {
    {
      { // L0
      new lbr(175347046, 0, 0),
      new lbr(  3341656, 4.6692568, 6283.0758500),
      new lbr(    34894, 4.62610, 12566.15170),
      new lbr(     3497, 2.7441, 5753.3849),
      new lbr(     3418, 2.8289, 3.5231),
      new lbr(     3136, 3.6277, 77713.7715),
      new lbr(     2676, 4.4181, 7860.4194),
      new lbr(     2343, 6.1352, 3930.2097),
      new lbr(     1324, 0.7425, 11506.7698),
      new lbr(     1273, 2.0371, 529.6910),
      new lbr(     1199, 1.1096, 1577.3435),
      new lbr(      990, 5.233, 5884.927),
      new lbr(      902, 2.045, 26.298),
      new lbr(      857, 3.508, 398.149),
      new lbr(      780, 1.179, 5223.694),
      new lbr(      753, 2.533, 5507.553),
      new lbr(      505, 4.583, 18849.228),
      new lbr(      492, 4.205, 775.523),
      new lbr(      357, 2.920, 0.067),
      new lbr(      317, 5.849, 11790.629),
      new lbr(      284, 1.899, 796.298),
      new lbr(      271, 0.315, 10977.079),
      new lbr(      243, 0.345, 5486.778),
      new lbr(      206, 4.806, 2544.314),
      new lbr(      205, 1.869, 5573.143),
      new lbr(      202, 2.458, 6069.777),
      new lbr(      156, 0.833, 213.299),
      new lbr(      132, 3.411, 2942.463),
      new lbr(      126, 1.083, 20.775),
      new lbr(      115, 0.645, 0.980),
      new lbr(      103, 0.636, 4694.003),
      new lbr(      102, 0.976, 15720.839),
      new lbr(      102, 4.267, 7.114),
      new lbr(       99, 6.21, 2146.17),
      new lbr(       98, 0.68, 155.42),
      new lbr(       86, 5.98, 161000.69),
      new lbr(       85, 1.30, 6275.96),
      new lbr(       85, 3.67, 71430.70),
      new lbr(       80, 1.81, 17260.15),
      new lbr(       79, 3.04, 12036.46),
      new lbr(       75, 1.76, 5088.63),
      new lbr(       74, 3.50, 3154.69),
      new lbr(       74, 4.68, 801.82),
      new lbr(       70, 0.83, 9437.76),
      new lbr(       62, 3.98, 8827.39),
      new lbr(       61, 1.82, 7084.90),
      new lbr(       57, 2.78, 6286.60),
      new lbr(       56, 4.39, 14143.50),
      new lbr(       56, 3.47, 6279.55),
      new lbr(       52, 0.19, 12139.55),
      new lbr(       52, 1.33, 1748.02),
      new lbr(       51, 0.28, 5856.48),
      new lbr(       49, 0.49, 1194.45),
      new lbr(       41, 5.37, 8429.24),
      new lbr(       41, 2.40, 19651.05),
      new lbr(       39, 6.17, 10447.39),
      new lbr(       37, 6.04, 10213.29),
      new lbr(       37, 2.57, 1059.38),
      new lbr(       36, 1.71, 2352.87),
      new lbr(       36, 1.78, 6812.77),
      new lbr(       33, 0.59, 17789.85),
      new lbr(       30, 0.44, 83996.85),
      new lbr(       30, 2.74, 1349.87),
      new lbr(       25, 3.16, 4690.48) },
      { // L1
      new lbr(628331966747L, 0, 0),
      new lbr(   206059, 2.678235, 6283.075850),
      new lbr(     4303, 2.6351, 12566.1517),
      new lbr(      425, 1.590, 3.523),
      new lbr(      119, 5.796, 26.298),
      new lbr(      109, 2.966, 1577.344),
      new lbr(       93, 2.59, 18849.23),
      new lbr(       72, 1.14, 529.69),
      new lbr(       68, 1.87, 398.15),
      new lbr(       67, 4.41, 5507.55),
      new lbr(       59, 2.89, 5223.69),
      new lbr(       56, 2.17, 155.42),
      new lbr(       45, 0.40, 796.30),
      new lbr(       36, 0.47, 775.52),
      new lbr(       29, 2.65, 7.11),
      new lbr(       21, 5.34, 0.98),
      new lbr(       19, 1.85, 5486.78),
      new lbr(       19, 4.97, 213.30),
      new lbr(       17, 2.99, 6275.96),
      new lbr(       16, 0.03, 2544.31),
      new lbr(       16, 1.43, 2146.17),
      new lbr(       15, 1.21, 10977.08),
      new lbr(       12, 2.83, 1748.02),
      new lbr(       12, 3.26, 5088.63),
      new lbr(       12, 5.27, 1194.45),
      new lbr(       12, 2.08, 4694.00),
      new lbr(       11, 0.77, 553.57),
      new lbr(       10, 1.30, 6286.60),
      new lbr(       10, 4.24, 1349.87),
      new lbr(        9, 2.70, 242.73),
      new lbr(        9, 5.64, 951.72),
      new lbr(        8, 5.30, 2352.87),
      new lbr(        6, 2.65, 9437.76),
      new lbr(        6, 4.67, 4690.48) },
      { // L2
      new lbr(    52919, 0, 0),
      new lbr(     8720, 1.0721, 6283.0758),
      new lbr(      309, 0.867, 12566.152),
      new lbr(       27, 0.05, 3.52),
      new lbr(       16, 5.19, 26.30),
      new lbr(       16, 3.68, 155.42),
      new lbr(       10, 0.76, 18849.23),
      new lbr(        9, 2.06, 77713.77),
      new lbr(        7, 0.83, 775.52),
      new lbr(        5, 4.66, 1577.34),
      new lbr(        4, 1.03, 7.11),
      new lbr(        4, 3.44, 5573.14),
      new lbr(        3, 5.14, 796.30),
      new lbr(        3, 6.05, 5507.55),
      new lbr(        3, 1.19, 242.73),
      new lbr(        3, 6.12, 529.69),
      new lbr(        3, 0.31, 398.15),
      new lbr(        3, 2.28, 553.57),
      new lbr(        2, 4.38, 5223.69),
      new lbr(        2, 3.75, 0.98) },
      { // L3
      new lbr(      289, 5.844, 6283.076),
      new lbr(       35, 0, 0),
      new lbr(       17, 5.49, 12566.15),
      new lbr(        3, 5.20, 155.42),
      new lbr(        1, 4.72, 3.52),
      new lbr(        1, 5.30, 18849.23),
      new lbr(        1, 5.97, 242.73) },
      { // L4
      new lbr(      114, 3.142, 0),
      new lbr(        8, 4.13, 6283.08),
      new lbr(        1, 3.84, 12566.15) },
      { // L5
      new lbr(        1, 3.14, 0) }
    },
    {
      { // B0
      new lbr(      280, 3.199, 84334.662),
      new lbr(      102, 5.422, 5507.553),
      new lbr(       80, 3.88, 5223.69),
      new lbr(       44, 3.70, 2352.87),
      new lbr(       32, 4.00, 1577.34) },
      { // B1
      new lbr(        9, 3.90, 5507.55),
      new lbr(        6, 1.73, 5223.69) }
    },
    {
      { // R0
      new lbr(100013989, 0, 0),
      new lbr(  1670700, 3.0984635, 6283.0758500),
      new lbr(    13956, 3.05525, 12566.15170),
      new lbr(     3084, 5.1985, 77713.7715),
      new lbr(     1628, 1.1739, 5753.3849),
      new lbr(     1576, 2.8469, 7860.4194),
      new lbr(      925, 5.453, 11506.770),
      new lbr(      542, 4.564, 3930.210),
      new lbr(      472, 3.661, 5884.927),
      new lbr(      346, 0.964, 5507.553),
      new lbr(      329, 5.900, 5223.694),
      new lbr(      307, 0.299, 5573.143),
      new lbr(      243, 4.273, 11790.629),
      new lbr(      212, 5.847, 1577.344),
      new lbr(      186, 5.022, 10977.079),
      new lbr(      175, 3.012, 18849.228),
      new lbr(      110, 5.055, 5486.778),
      new lbr(       98, 0.89, 6069.78),
      new lbr(       86, 5.69, 15720.84),
      new lbr(       86, 1.27, 161000.69),
      new lbr(       65, 0.27, 17260.15),
      new lbr(       63, 0.92, 529.69),
      new lbr(       57, 2.01, 83996.85),
      new lbr(       56, 5.24, 71430.70),
      new lbr(       49, 3.25, 2544.31),
      new lbr(       47, 2.58, 775.52),
      new lbr(       45, 5.54, 9437.76),
      new lbr(       43, 6.01, 6275.96),
      new lbr(       39, 5.36, 4694.00),
      new lbr(       38, 2.39, 8827.39),
      new lbr(       37, 0.83, 19651.05),
      new lbr(       37, 4.90, 12139.55),
      new lbr(       36, 1.67, 12036.46),
      new lbr(       35, 1.84, 2942.46),
      new lbr(       33, 0.24, 7084.90),
      new lbr(       32, 0.18, 5088.63),
      new lbr(       32, 1.78, 398.15),
      new lbr(       28, 1.21, 6286.60),
      new lbr(       28, 1.90, 6279.55),
      new lbr(       26, 4.59, 10447.39) },
      { // R1
      new lbr(   103019, 1.107490, 6283.075850),
      new lbr(     1721, 1.0644, 12566.1517),
      new lbr(      702, 3.142, 0),
      new lbr(       32, 1.02, 18849.23),
      new lbr(       31, 2.84, 5507.55),
      new lbr(       25, 1.32, 5223.69),
      new lbr(       18, 1.42, 1577.34),
      new lbr(       10, 5.91, 10977.08),
      new lbr(        9, 1.42, 6275.96),
      new lbr(        9, 0.27, 5486.78) },
      { // R2
      new lbr(     4359, 5.7846, 6283.0758),
      new lbr(      124, 5.579, 12566.152),
      new lbr(       12, 3.14, 0),
      new lbr(        9, 3.63, 77713.77),
      new lbr(        6, 1.87, 5573.14),
      new lbr(        3, 5.47, 18849.23) },
      { // R3
      new lbr(      145, 4.273, 6283.076),
      new lbr(        7, 3.92, 12566.15) },
      { // R4
      new lbr(        4, 2.56, 6283.08) }
    }
  };
  static final private lbr mercury[][][] = {
    {
      { // L0
      new lbr(440250710, 0, 0),
      new lbr( 40989415, 1.48302034, 26087.90314157),
      new lbr(  5046294, 4.4778549, 52175.8062831),
      new lbr(   855347, 1.165203, 78263.709425),
      new lbr(   165590, 4.119692, 104351.612566),
      new lbr(    34562, 0.77931, 130439.51571),
      new lbr(     7583, 3.7135, 156527.4188),
      new lbr(     3560, 1.5120, 1109.3786),
      new lbr(     1803, 4.1033, 5661.3320),
      new lbr(     1726, 0.3583, 182615.3220),
      new lbr(     1590, 2.9951, 25028.5212),
      new lbr(     1365, 4.5992, 27197.2817),
      new lbr(     1017, 0.8803, 31749.2352),
      new lbr(      714, 1.541, 24978.525),
      new lbr(      644, 5.303, 21535.950),
      new lbr(      451, 6.050, 51116.424),
      new lbr(      404, 3.282, 208703.225),
      new lbr(      352, 5.242, 20426.571),
      new lbr(      345, 2.792, 15874.618),
      new lbr(      343, 5.765, 955.600),
      new lbr(      339, 5.863, 25558.212),
      new lbr(      325, 1.337, 53285.185),
      new lbr(      273, 2.495, 529.691),
      new lbr(      264, 3.917, 57837.138),
      new lbr(      260, 0.987, 4551.953),
      new lbr(      239, 0.113, 1059.382),
      new lbr(      235, 0.267, 11322.664),
      new lbr(      217, 0.660, 13521.751),
      new lbr(      209, 2.092, 47623.853),
      new lbr(      183, 2.629, 27043.503),
      new lbr(      182, 2.434, 25661.305),
      new lbr(      176, 4.536, 51066.428),
      new lbr(      173, 2.452, 24498.830),
      new lbr(      142, 3.360, 37410.567),
      new lbr(      138, 0.291, 10213.286),
      new lbr(      125, 3.721, 39609.655),
      new lbr(      118, 2.781, 77204.327),
      new lbr(      106, 4.206, 19804.827) },
      { // L1
      new lbr(2608814706223L, 0, 0),
      new lbr(  1126008, 6.2170397, 26087.9031416),
      new lbr(   303471, 3.055655, 52175.806283),
      new lbr(    80538, 6.10455, 78263.70942),
      new lbr(    21245, 2.83532, 104351.61257),
      new lbr(     5592, 5.8268, 130439.5157),
      new lbr(     1472, 2.5185, 156527.4188),
      new lbr(      388, 5.480, 182615.322),
      new lbr(      352, 3.052, 1109.379),
      new lbr(      103, 2.149, 208703.225),
      new lbr(       94, 6.12, 27197.28),
      new lbr(       91, 0.00, 24978.52),
      new lbr(       52, 5.62, 5661.33),
      new lbr(       44, 4.57, 25028.52),
      new lbr(       28, 3.04, 51066.43),
      new lbr(       27, 5.09, 234791.13) },
      { // L2
      new lbr(    53050, 0, 0),
      new lbr(    16904, 4.69072, 26087.90314),
      new lbr(     7397, 1.3474, 52175.8063),
      new lbr(     3018, 4.4564, 78263.7094),
      new lbr(     1107, 1.2623, 104351.6126),
      new lbr(      378, 4.320, 130439.516),
      new lbr(      123, 1.069, 156527.419),
      new lbr(       39, 4.08, 182615.32),
      new lbr(       15, 4.63, 1109.38),
      new lbr(       12, 0.79, 208703.23) },
      { // L3
      new lbr(      188, 0.035, 52175.806),
      new lbr(      142, 3.125, 26087.903),
      new lbr(       97, 3.00, 78263.71),
      new lbr(       44, 6.02, 104351.61),
      new lbr(       35, 0, 0),
      new lbr(       18, 2.78, 130439.52),
      new lbr(        7, 5.82, 156527.42),
      new lbr(        3, 2.57, 182615.32) },
      { // L4
      new lbr(      114, 3.1416, 0),
      new lbr(        3, 2.03, 26087.90),
      new lbr(        2, 1.42, 78263.71),
      new lbr(        2, 4.50, 52175.81),
      new lbr(        1, 4.50, 104351.61),
      new lbr(        1, 1.27, 130439.52) },
      { // L5
      new lbr(        1, 3.14, 0) }
    },
    {
      { // B0
      new lbr( 11737529, 1.98357499, 26087.90314157),
      new lbr(  2388077, 5.0373896, 52175.8062831),
      new lbr(  1222840, 3.1415927, 0),
      new lbr(   543252, 1.796444, 78263.709425),
      new lbr(   129779, 4.832325, 104351.612566),
      new lbr(    31867, 1.58088, 130439.51571),
      new lbr(     7963, 4.6097, 156527.4188),
      new lbr(     2014, 1.3532, 182615.3220),
      new lbr(      514, 4.378, 208703.225),
      new lbr(      209, 2.020, 24978.525),
      new lbr(      208, 4.918, 27197.282),
      new lbr(      132, 1.119, 234791.128),
      new lbr(      121, 1.813, 53285.185),
      new lbr(      100, 5.657, 20426.571) },
      { // B1
      new lbr(   429151, 3.501698, 26087.903142),
      new lbr(   146234, 3.141593, 0),
      new lbr(    22675, 0.01515, 52175.80628),
      new lbr(    10895, 0.48540, 78263.70942),
      new lbr(     6353, 3.4294, 104351.6126),
      new lbr(     2496, 0.1605, 130439.5157),
      new lbr(      860, 3.185, 156527.419),
      new lbr(      278, 6.210, 182615.322),
      new lbr(       86, 2.95, 208703.23),
      new lbr(       28, 0.29, 27197.28),
      new lbr(       26, 5.98, 234791.13) },
      { // B2
      new lbr(    11831, 4.79066, 26087.90314),
      new lbr(     1914, 0, 0),
      new lbr(     1045, 1.2122, 52175.8063),
      new lbr(      266, 4.434, 78263.709),
      new lbr(      170, 1.623, 104351.613),
      new lbr(       96, 4.80, 130439.52),
      new lbr(       45, 1.61, 156527.42),
      new lbr(       18, 4.67, 182615.32),
      new lbr(        7, 1.43, 208703.23) },
      { // B3
      new lbr(      235, 0.354, 26087.903),
      new lbr(      161, 0, 0),
      new lbr(       19, 4.36, 52175.81),
      new lbr(        6, 2.51, 78263.71),
      new lbr(        5, 6.14, 104351.61),
      new lbr(        3, 3.12, 130439.52),
      new lbr(        2, 6.27, 156527.42) },
      { // B4
      new lbr(        4, 1.75, 26087.90),
      new lbr(        1, 3.14, 0) }
    },
    {
      { // R0
      new lbr( 39528272, 0, 0),
      new lbr(  7834132, 6.1923372, 26087.9031416),
      new lbr(   795526, 2.959897, 52175.806283),
      new lbr(   121282, 6.010642, 78263.709425),
      new lbr(    21922, 2.77820, 104351.61257),
      new lbr(     4354, 5.8289, 130439.5157),
      new lbr(      918, 2.597, 156527.419),
      new lbr(      290, 1.424, 25028.521),
      new lbr(      260, 3.028, 27197.282),
      new lbr(      202, 5.647, 182615.322),
      new lbr(      201, 5.592, 31749.235),
      new lbr(      142, 6.253, 24978.525),
      new lbr(      100, 3.734, 21535.950) },
      { // R1
      new lbr(   217348, 4.656172, 26087.903142),
      new lbr(    44142, 1.42386, 52175.80628),
      new lbr(    10094, 4.47466, 78263.70942),
      new lbr(     2433, 1.2423, 104351.6126),
      new lbr(     1624, 0, 0),
      new lbr(      604, 4.293, 130439.516),
      new lbr(      153, 1.061, 156527.419),
      new lbr(       39, 4.11, 182615.32) },
      { // R2
      new lbr(     3118, 3.0823, 26087.9031),
      new lbr(     1245, 6.1518, 52175.8063),
      new lbr(      425, 2.926, 78263.709),
      new lbr(      136, 5.980, 104351.613),
      new lbr(       42, 2.75, 130439.52),
      new lbr(       22, 3.14, 0),
      new lbr(       13, 5.80, 156527.42) },
      { // R3
      new lbr(       33, 1.68, 26087.90),
      new lbr(       24, 4.63, 52175.81),
      new lbr(       12, 1.39, 78263.71),
      new lbr(        5, 4.44, 104351.61),
      new lbr(        2, 1.21, 130439.52) }
    }
  };
  static final private lbr venus[][][] = {
    {
      { // L0
      new lbr(317614667, 0, 0),
      new lbr(  1353968, 5.5931332, 10213.2855462),
      new lbr(    89892, 5.30650, 20426.57109),
      new lbr(     5477, 4.4163, 7860.4194),
      new lbr(     3456, 2.6996, 11790.6291),
      new lbr(     2372, 2.9938, 3930.2097),
      new lbr(     1664, 4.2502, 1577.3435),
      new lbr(     1438, 4.1575, 9683.5946),
      new lbr(     1317, 5.1867, 26.2983),
      new lbr(     1201, 6.1536, 30639.8566),
      new lbr(      769, 0.816, 9437.763),
      new lbr(      761, 1.950, 529.691),
      new lbr(      708, 1.065, 775.523),
      new lbr(      585, 3.998, 191.448),
      new lbr(      500, 4.123, 15720.839),
      new lbr(      429, 3.586, 19367.189),
      new lbr(      327, 5.677, 5507.553),
      new lbr(      326, 4.591, 10404.734),
      new lbr(      232, 3.163, 9153.904),
      new lbr(      180, 4.653, 1109.379),
      new lbr(      155, 5.570, 19651.048),
      new lbr(      128, 4.226, 20.775),
      new lbr(      128, 0.962, 5661.332),
      new lbr(      106, 1.537, 801.821) },
      { // L1
      new lbr(1021352943053L, 0, 0),
      new lbr(    95708, 2.46424, 10213.28555),
      new lbr(    14445, 0.51625, 20426.57109),
      new lbr(      213, 1.795, 30639.857),
      new lbr(      174, 2.655, 26.298),
      new lbr(      152, 6.106, 1577.344),
      new lbr(       82, 5.70, 191.45),
      new lbr(       70, 2.68, 9437.76),
      new lbr(       52, 3.60, 775.52),
      new lbr(       38, 1.03, 529.69),
      new lbr(       30, 1.25, 5507.55),
      new lbr(       25, 6.11, 10404.73) },
      { // L2
      new lbr(    54127, 0, 0),
      new lbr(     3891, 0.3451, 10213.2855),
      new lbr(     1338, 2.0201, 20426.5711),
      new lbr(       24, 2.05, 26.30),
      new lbr(       19, 3.54, 30639.86),
      new lbr(       10, 3.97, 775.52),
      new lbr(        7, 1.52, 1577.34),
      new lbr(        6, 1.00, 191.45) },
      { // L3
      new lbr(      136, 4.804, 10213.286),
      new lbr(       78, 3.67, 20426.57),
      new lbr(       26, 0, 0) },
      { // L4
      new lbr(      114, 3.1416, 0),
      new lbr(        3, 5.21, 20426.57),
      new lbr(        2, 2.51, 10213.29) },
      { // L5
      new lbr(        1, 3.14, 0) }
    },
    {
      { // B0
      new lbr(  5923638, 0.2670278, 10213.2855462),
      new lbr(    40108, 1.14737, 20426.57109),
      new lbr(    32815, 3.14159, 0),
      new lbr(     1011, 1.0895, 30639.8566),
      new lbr(      149, 6.254, 18073.705),
      new lbr(      138, 0.860, 1577.344),
      new lbr(      130, 3.672, 9437.763),
      new lbr(      120, 3.705, 2352.866),
      new lbr(      108, 4.539, 22003.915) },
      { // B1
      new lbr(   513348, 1.803643, 10213.285546),
      new lbr(     4380, 3.3862, 20426.5711),
      new lbr(      199, 0, 0),
      new lbr(      197, 2.530, 30639.857) },
      { // B2
      new lbr(    22378, 3.38509, 10213.28555),
      new lbr(      282, 0, 0),
      new lbr(      173, 5.256, 20426.571),
      new lbr(       27, 3.87, 30639.86) },
      { // B3
      new lbr(      647, 4.992, 10213.286),
      new lbr(       20, 3.14, 0),
      new lbr(        6, 0.77, 20426.57),
      new lbr(        3, 5.44, 30639.86) },
      { // B4
      new lbr(       14, 0.32, 10213.29) }
    },
    {
      { // R0
      new lbr( 72334821, 0, 0),
      new lbr(   489824, 4.021518, 10213.285546),
      new lbr(     1658, 4.9021, 20426.5711),
      new lbr(     1632, 2.8455, 7860.4194),
      new lbr(     1378, 1.1285, 11790.6291),
      new lbr(      498, 2.587, 9683.595),
      new lbr(      374, 1.423, 3930.210),
      new lbr(      264, 5.529, 9437.763),
      new lbr(      237, 2.551, 15720.839),
      new lbr(      222, 2.013, 19367.189),
      new lbr(      126, 2.728, 1577.344),
      new lbr(      119, 3.020, 10404.734) },
      { // R1
      new lbr(    34551, 0.89199, 10213.28555),
      new lbr(      234, 1.772, 20426.571),
      new lbr(      234, 3.142, 0) },
      { // R2
      new lbr(     1407, 5.0637, 10213.2855),
      new lbr(       16, 5.47, 20426.57),
      new lbr(       13, 0, 0) },
      { // R3
      new lbr(       50, 3.22, 10213.29) },
      { // R4
      new lbr(        1, 0.92, 10213.29) }
    }
  };
  static final private lbr mars[][][] = {
    {
      { // L0
      new lbr(620347712, 0, 0),
      new lbr( 18656368, 5.05037100, 3340.61242670),
      new lbr(  1108217, 5.4009984, 6681.2248534),
      new lbr(    91798, 5.75479, 10021.83728),
      new lbr(    27745, 5.97050, 3.52312),
      new lbr(    12316, 0.84956, 2810.92146),
      new lbr(    10610, 2.93959, 2281.23050),
      new lbr(     8927, 4.1570, 0.0173),
      new lbr(     8716, 6.1101, 13362.4497),
      new lbr(     7775, 3.3397, 5621.8429),
      new lbr(     6798, 0.3646, 398.1490),
      new lbr(     4161, 0.2281, 2942.4634),
      new lbr(     3575, 1.6619, 2544.3144),
      new lbr(     3075, 0.8570, 191.4483),
      new lbr(     2938, 6.0789, 0.0673),
      new lbr(     2628, 0.6481, 3337.0893),
      new lbr(     2580, 0.0300, 3344.1355),
      new lbr(     2389, 5.0390, 796.2980),
      new lbr(     1799, 0.6563, 529.6910),
      new lbr(     1546, 2.9158, 1751.5395),
      new lbr(     1528, 1.1498, 6151.5339),
      new lbr(     1286, 3.0680, 2146.1654),
      new lbr(     1264, 3.6228, 5092.1520),
      new lbr(     1025, 3.6933, 8962.4553),
      new lbr(      892, 0.183, 16703.062),
      new lbr(      859, 2.401, 2914.014),
      new lbr(      833, 4.495, 3340.630),
      new lbr(      833, 2.464, 3340.595),
      new lbr(      749, 3.822, 155.420),
      new lbr(      724, 0.675, 3738.761),
      new lbr(      713, 3.663, 1059.382),
      new lbr(      655, 0.489, 3127.313),
      new lbr(      636, 2.922, 8432.764),
      new lbr(      553, 4.475, 1748.016),
      new lbr(      550, 3.810, 0.980),
      new lbr(      472, 3.625, 1194.447),
      new lbr(      426, 0.554, 6283.076),
      new lbr(      415, 0.497, 213.299),
      new lbr(      312, 0.999, 6677.702),
      new lbr(      307, 0.381, 6684.748),
      new lbr(      302, 4.486, 3532.061),
      new lbr(      299, 2.783, 6254.627),
      new lbr(      293, 4.221, 20.775),
      new lbr(      284, 5.769, 3149.164),
      new lbr(      281, 5.882, 1349.867),
      new lbr(      274, 0.542, 3340.545),
      new lbr(      274, 0.134, 3340.680),
      new lbr(      239, 5.372, 4136.910),
      new lbr(      236, 5.755, 3333.499),
      new lbr(      231, 1.282, 3870.303),
      new lbr(      221, 3.505, 382.897),
      new lbr(      204, 2.821, 1221.849),
      new lbr(      193, 3.357, 3.590),
      new lbr(      189, 1.491, 9492.146),
      new lbr(      179, 1.006, 951.718),
      new lbr(      174, 2.414, 553.569),
      new lbr(      172, 0.439, 5486.778),
      new lbr(      160, 3.949, 4562.461),
      new lbr(      144, 1.419, 135.065),
      new lbr(      140, 3.326, 2700.715),
      new lbr(      138, 4.301, 7.114),
      new lbr(      131, 4.045, 12303.068),
      new lbr(      128, 2.208, 1592.596),
      new lbr(      128, 1.807, 5088.629),
      new lbr(      117, 3.128, 7903.073),
      new lbr(      113, 3.701, 1589.073),
      new lbr(      110, 1.052, 242.729),
      new lbr(      105, 0.785, 8827.390),
      new lbr(      100, 3.243, 11773.377) },
      { // L1
      new lbr(334085627474L, 0, 0),
      new lbr(  1458227, 3.6042605, 3340.6124267),
      new lbr(   164901, 3.926313, 6681.224853),
      new lbr(    19963, 4.26594, 10021.83728),
      new lbr(     3452, 4.7321, 3.5231),
      new lbr(     2485, 4.6128, 13362.4497),
      new lbr(      842, 4.459, 2281.230),
      new lbr(      538, 5.016, 398.149),
      new lbr(      521, 4.994, 3344.136),
      new lbr(      433, 2.561, 191.448),
      new lbr(      430, 5.316, 155.420),
      new lbr(      382, 3.539, 796.298),
      new lbr(      314, 4.963, 16703.062),
      new lbr(      283, 3.160, 2544.314),
      new lbr(      206, 4.569, 2146.165),
      new lbr(      169, 1.329, 3337.089),
      new lbr(      158, 4.185, 1751.540),
      new lbr(      134, 2.233, 0.980),
      new lbr(      134, 5.974, 1748.016),
      new lbr(      118, 6.024, 6151.534),
      new lbr(      117, 2.213, 1059.382),
      new lbr(      114, 2.129, 1194.447),
      new lbr(      114, 5.428, 3738.761),
      new lbr(       91, 1.10, 1349.87),
      new lbr(       85, 3.91, 553.57),
      new lbr(       83, 5.30, 6684.75),
      new lbr(       81, 4.43, 529.69),
      new lbr(       80, 2.25, 8962.46),
      new lbr(       73, 2.50, 951.72),
      new lbr(       73, 5.84, 242.73),
      new lbr(       71, 3.86, 2914.01),
      new lbr(       68, 5.02, 382.90),
      new lbr(       65, 1.02, 3340.60),
      new lbr(       65, 3.05, 3340.63),
      new lbr(       62, 4.15, 3149.16),
      new lbr(       57, 3.89, 4136.91),
      new lbr(       48, 4.87, 213.30),
      new lbr(       48, 1.18, 3333.50),
      new lbr(       47, 1.31, 3185.19),
      new lbr(       41, 0.71, 1592.60),
      new lbr(       40, 2.73, 7.11),
      new lbr(       40, 5.32, 20043.67),
      new lbr(       33, 5.41, 6283.08),
      new lbr(       28, 0.05, 9492.15),
      new lbr(       27, 3.89, 1221.85),
      new lbr(       27, 5.11, 2700.72) },
      { // L2
      new lbr(    58016, 2.04979, 3340.61243),
      new lbr(    54188, 0, 0),
      new lbr(    13908, 2.45742, 6681.22485),
      new lbr(     2465, 2.8000, 10021.8373),
      new lbr(      398, 3.141, 13362.450),
      new lbr(      222, 3.194, 3.523),
      new lbr(      121, 0.543, 155.420),
      new lbr(       62, 3.49, 16703.06),
      new lbr(       54, 3.54, 3344.14),
      new lbr(       34, 6.00, 2281.23),
      new lbr(       32, 4.14, 191.45),
      new lbr(       30, 2.00, 796.30),
      new lbr(       23, 4.33, 242.73),
      new lbr(       22, 3.45, 398.15),
      new lbr(       20, 5.42, 553.57),
      new lbr(       16, 0.66, 0.98),
      new lbr(       16, 6.11, 2146.17),
      new lbr(       16, 1.22, 1748.02),
      new lbr(       15, 6.10, 3185.19),
      new lbr(       14, 4.02, 951.72),
      new lbr(       14, 2.62, 1349.87),
      new lbr(       13, 0.60, 1194.45),
      new lbr(       12, 3.86, 6684.75),
      new lbr(       11, 4.72, 2544.31),
      new lbr(       10, 0.25, 382.90),
      new lbr(        9, 0.68, 1059.38),
      new lbr(        9, 3.83, 20043.67),
      new lbr(        9, 3.88, 3738.76),
      new lbr(        8, 5.46, 1751.54),
      new lbr(        7, 2.58, 3149.16),
      new lbr(        7, 2.38, 4136.91),
      new lbr(        6, 5.48, 1592.60),
      new lbr(        6, 2.34, 3097.88) },
      { // L3
      new lbr(     1482, 0.4443, 3340.6124),
      new lbr(      662, 0.885, 6681.225),
      new lbr(      188, 1.288, 10021.837),
      new lbr(       41, 1.65, 13362.45),
      new lbr(       26, 0, 0),
      new lbr(       23, 2.05, 155.42),
      new lbr(       10, 1.58, 3.52),
      new lbr(        8, 2.00, 16703.06),
      new lbr(        5, 2.82, 242.73),
      new lbr(        4, 2.02, 3344.14),
      new lbr(        3, 4.59, 3185.19),
      new lbr(        3, 0.65, 553.57) },
      { // L4
      new lbr(      114, 3.1416, 0),
      new lbr(       29, 5.64, 6681.22),
      new lbr(       24, 5.14, 3340.61),
      new lbr(       11, 6.03, 10021.84),
      new lbr(        3, 0.13, 13362.45),
      new lbr(        3, 3.56, 155.42),
      new lbr(        1, 0.49, 16703.06),
      new lbr(        1, 1.32, 242.73) },
      { // L5
      new lbr(        1, 3.14, 0),
      new lbr(        1, 4.04, 6681.22) }
    },
    {
      { // B0
      new lbr(  3197135, 3.7683204, 3340.6124267),
      new lbr(   298033, 4.106170, 6681.224853),
      new lbr(   289105, 0, 0),
      new lbr(    31366, 4.44651, 10021.83728),
      new lbr(     3484, 4.7881, 13362.4497),
      new lbr(      443, 5.026, 3344.136),
      new lbr(      443, 5.652, 3337.089),
      new lbr(      399, 5.131, 16703.062),
      new lbr(      293, 3.793, 2281.230),
      new lbr(      182, 6.136, 6151.534),
      new lbr(      163, 4.264, 529.691),
      new lbr(      160, 2.232, 1059.382),
      new lbr(      149, 2.165, 5621.843),
      new lbr(      143, 1.182, 3340.595),
      new lbr(      143, 3.213, 3340.630),
      new lbr(      139, 2.418, 8962.455) },
      { // B1
      new lbr(   350069, 5.368478, 3340.612427),
      new lbr(    14116, 3.14159, 0),
      new lbr(     9671, 5.4788, 6681.2249),
      new lbr(     1472, 3.2021, 10021.8373),
      new lbr(      426, 3.408, 13362.450),
      new lbr(      102, 0.776, 3337.089),
      new lbr(       79, 3.72, 16703.06),
      new lbr(       33, 3.46, 5621.84),
      new lbr(       26, 2.48, 2281.23) },
      { // B2
      new lbr(    16727, 0.60221, 3340.61243),
      new lbr(     4987, 3.1416, 0),
      new lbr(      302, 5.559, 6681.225),
      new lbr(       26, 1.90, 13362.45),
      new lbr(       21, 0.92, 10021.84),
      new lbr(       12, 2.24, 3337.09),
      new lbr(        8, 2.25, 16703.06) },
      { // B3
      new lbr(      607, 1.981, 3340.612),
      new lbr(       43, 0, 0),
      new lbr(       14, 1.80, 6681.22),
      new lbr(        3, 3.45, 10021.84) },
      { // B4
      new lbr(       13, 0, 0),
      new lbr(       11, 3.46, 3340.61),
      new lbr(        1, 0.50, 6681.22) }
    },
    {
      { // R0
      new lbr(153033488, 0, 0),
      new lbr( 14184953, 3.47971284, 3340.61242670),
      new lbr(   660776, 3.817834, 6681.224853),
      new lbr(    46179, 4.15595, 10021.83728),
      new lbr(     8110, 5.5596, 2810.9215),
      new lbr(     7485, 1.7724, 5621.8429),
      new lbr(     5523, 1.3644, 2281.2305),
      new lbr(     3825, 4.4941, 13362.4497),
      new lbr(     2484, 4.9255, 2942.4634),
      new lbr(     2307, 0.0908, 2544.3144),
      new lbr(     1999, 5.3606, 3337.0893),
      new lbr(     1960, 4.7425, 3344.1355),
      new lbr(     1167, 2.1126, 5092.1520),
      new lbr(     1103, 5.0091, 398.1490),
      new lbr(      992, 5.839, 6151.534),
      new lbr(      899, 4.408, 529.691),
      new lbr(      807, 2.102, 1059.382),
      new lbr(      798, 3.448, 796.298),
      new lbr(      741, 1.499, 2146.165),
      new lbr(      726, 1.245, 8432.764),
      new lbr(      692, 2.134, 8962.455),
      new lbr(      633, 0.894, 3340.595),
      new lbr(      633, 2.924, 3340.630),
      new lbr(      630, 1.287, 1751.540),
      new lbr(      574, 0.829, 2914.014),
      new lbr(      526, 5.383, 3738.761),
      new lbr(      473, 5.199, 3127.313),
      new lbr(      348, 4.832, 16703.062),
      new lbr(      284, 2.907, 3532.061),
      new lbr(      280, 5.257, 6283.076),
      new lbr(      276, 1.218, 6254.627),
      new lbr(      275, 2.908, 1748.016),
      new lbr(      270, 3.764, 5884.927),
      new lbr(      239, 2.037, 1194.447),
      new lbr(      234, 5.105, 5486.778),
      new lbr(      228, 3.255, 6872.673),
      new lbr(      223, 4.199, 3149.164),
      new lbr(      219, 5.583, 191.448),
      new lbr(      208, 5.255, 3340.545),
      new lbr(      208, 4.846, 3340.680),
      new lbr(      186, 5.699, 6677.702),
      new lbr(      183, 5.081, 6684.748),
      new lbr(      179, 4.184, 3333.499),
      new lbr(      176, 5.953, 3870.303),
      new lbr(      164, 3.799, 4136.910) },
      { // R1
      new lbr(  1107433, 2.0325052, 3340.6124267),
      new lbr(   103176, 2.370718, 6681.224853),
      new lbr(    12877, 0, 0),
      new lbr(    10816, 2.70888, 10021.83728),
      new lbr(     1195, 3.0470, 13362.4497),
      new lbr(      439, 2.888, 2281.230),
      new lbr(      396, 3.423, 3344.136),
      new lbr(      183, 1.584, 2544.314),
      new lbr(      136, 3.385, 16703.062),
      new lbr(      128, 6.043, 3337.089),
      new lbr(      128, 0.630, 1059.382),
      new lbr(      127, 1.954, 796.298),
      new lbr(      118, 2.998, 2146.165),
      new lbr(       88, 3.42, 398.15),
      new lbr(       83, 3.86, 3738.76),
      new lbr(       76, 4.45, 6151.53),
      new lbr(       72, 2.76, 529.69),
      new lbr(       67, 2.55, 1751.54),
      new lbr(       66, 4.41, 1748.02),
      new lbr(       58, 0.54, 1194.45),
      new lbr(       54, 0.68, 8962.46),
      new lbr(       51, 3.73, 6684.75),
      new lbr(       49, 5.73, 3340.60),
      new lbr(       49, 1.48, 3340.63),
      new lbr(       48, 2.58, 3149.16),
      new lbr(       48, 2.29, 2914.01),
      new lbr(       39, 2.32, 4136.91) },
      { // R2
      new lbr(    44242, 0.47931, 3340.61243),
      new lbr(     8138, 0.8700, 6681.2249),
      new lbr(     1275, 1.2259, 10021.8373),
      new lbr(      187, 1.573, 13362.450),
      new lbr(       52, 3.14, 0),
      new lbr(       41, 1.97, 3344.14),
      new lbr(       27, 1.92, 16703.06),
      new lbr(       18, 4.43, 2281.23),
      new lbr(       12, 4.53, 3185.19),
      new lbr(       10, 5.39, 1059.38),
      new lbr(       10, 0.42, 796.30) },
      { // R3
      new lbr(     1113, 5.1499, 3340.6124),
      new lbr(      424, 5.613, 6681.225),
      new lbr(      100, 5.997, 10021.837),
      new lbr(       20, 0.08, 13362.45),
      new lbr(        5, 3.14, 0),
      new lbr(        3, 0.43, 16703.06) },
      { // R4
      new lbr(       20, 3.58, 3340.61),
      new lbr(       16, 4.05, 6681.22),
      new lbr(        6, 4.46, 10021.84),
      new lbr(        2, 4.84, 13362.45) }
    }
  };
  static final private lbr jupiter[][][] = {
    {
      { // L0
      new lbr( 59954691, 0, 0),
      new lbr(  9695899, 5.0619179, 529.6909651),
      new lbr(   573610, 1.444062, 7.113547),
      new lbr(   306389, 5.417347, 1059.381930),
      new lbr(    97178, 4.14265, 632.78374),
      new lbr(    72903, 3.64043, 522.57742),
      new lbr(    64264, 3.41145, 103.09277),
      new lbr(    39806, 2.29377, 419.48464),
      new lbr(    38858, 1.27232, 316.39187),
      new lbr(    27965, 1.78455, 536.80451),
      new lbr(    13590, 5.77481, 1589.07290),
      new lbr(     8769, 3.6300, 949.1756),
      new lbr(     8246, 3.5823, 206.1855),
      new lbr(     7368, 5.0810, 735.8765),
      new lbr(     6263, 0.0250, 213.2991),
      new lbr(     6114, 4.5132, 1162.4747),
      new lbr(     5305, 4.1863, 1052.2684),
      new lbr(     5305, 1.3067, 14.2271),
      new lbr(     4905, 1.3208, 110.2063),
      new lbr(     4647, 4.6996, 3.9322),
      new lbr(     3045, 4.3168, 426.5982),
      new lbr(     2610, 1.5667, 846.0828),
      new lbr(     2028, 1.0638, 3.1814),
      new lbr(     1921, 0.9717, 639.8973),
      new lbr(     1765, 2.1415, 1066.4955),
      new lbr(     1723, 3.8804, 1265.5675),
      new lbr(     1633, 3.5820, 515.4639),
      new lbr(     1432, 4.2968, 625.6702),
      new lbr(      973, 4.098, 95.979),
      new lbr(      884, 2.437, 412.371),
      new lbr(      733, 6.085, 838.969),
      new lbr(      731, 3.806, 1581.959),
      new lbr(      709, 1.293, 742.990),
      new lbr(      692, 6.134, 2118.764),
      new lbr(      614, 4.109, 1478.867),
      new lbr(      582, 4.540, 309.278),
      new lbr(      495, 3.756, 323.505),
      new lbr(      441, 2.958, 454.909),
      new lbr(      417, 1.036, 2.448),
      new lbr(      390, 4.897, 1692.166),
      new lbr(      376, 4.703, 1368.660),
      new lbr(      341, 5.715, 533.623),
      new lbr(      330, 4.740, 0.048),
      new lbr(      262, 1.877, 0.963),
      new lbr(      261, 0.820, 380.128),
      new lbr(      257, 3.724, 199.072),
      new lbr(      244, 5.220, 728.763),
      new lbr(      235, 1.227, 909.819),
      new lbr(      220, 1.651, 543.918),
      new lbr(      207, 1.855, 525.759),
      new lbr(      202, 1.807, 1375.774),
      new lbr(      197, 5.293, 1155.361),
      new lbr(      175, 3.730, 942.062),
      new lbr(      175, 3.226, 1898.351),
      new lbr(      175, 5.910, 956.289),
      new lbr(      158, 4.365, 1795.258),
      new lbr(      151, 3.906, 74.782),
      new lbr(      149, 4.377, 1685.052),
      new lbr(      141, 3.136, 491.558),
      new lbr(      138, 1.318, 1169.588),
      new lbr(      131, 4.169, 1045.155),
      new lbr(      117, 2.500, 1596.186),
      new lbr(      117, 3.389, 0.521),
      new lbr(      106, 4.554, 526.510) },
      { // L1
      new lbr(52993480757L, 0, 0),
      new lbr(   489741, 4.220667, 529.690965),
      new lbr(   228919, 6.026475, 7.113547),
      new lbr(    27655, 4.57266, 1059.38193),
      new lbr(    20721, 5.45939, 522.57742),
      new lbr(    12106, 0.16986, 536.80451),
      new lbr(     6068, 4.4242, 103.0928),
      new lbr(     5434, 3.9848, 419.4846),
      new lbr(     4238, 5.8901, 14.2271),
      new lbr(     2212, 5.2677, 206.1855),
      new lbr(     1746, 4.9267, 1589.0729),
      new lbr(     1296, 5.5513, 3.1814),
      new lbr(     1173, 5.8565, 1052.2684),
      new lbr(     1163, 0.5145, 3.9322),
      new lbr(     1099, 5.3070, 515.4639),
      new lbr(     1007, 0.4648, 735.8765),
      new lbr(     1004, 3.1504, 426.5982),
      new lbr(      848, 5.758, 110.206),
      new lbr(      827, 4.803, 213.299),
      new lbr(      816, 0.586, 1066.495),
      new lbr(      725, 5.518, 639.897),
      new lbr(      568, 5.989, 625.670),
      new lbr(      474, 4.132, 412.371),
      new lbr(      413, 5.737, 95.979),
      new lbr(      345, 4.242, 632.784),
      new lbr(      336, 3.732, 1162.475),
      new lbr(      234, 4.035, 949.176),
      new lbr(      234, 6.243, 309.278),
      new lbr(      199, 1.505, 838.969),
      new lbr(      195, 2.219, 323.505),
      new lbr(      187, 6.086, 742.990),
      new lbr(      184, 6.280, 543.918),
      new lbr(      171, 5.417, 199.072),
      new lbr(      131, 0.626, 728.763),
      new lbr(      115, 0.680, 846.083),
      new lbr(      115, 5.286, 2118.764),
      new lbr(      108, 4.493, 956.289),
      new lbr(       80, 5.82, 1045.15),
      new lbr(       72, 5.34, 942.06),
      new lbr(       70, 5.97, 532.87),
      new lbr(       67, 5.73, 21.34),
      new lbr(       66, 0.13, 526.51),
      new lbr(       65, 6.09, 1581.96),
      new lbr(       59, 0.59, 1155.36),
      new lbr(       58, 0.99, 1596.19),
      new lbr(       57, 5.97, 1169.59),
      new lbr(       57, 1.41, 533.62),
      new lbr(       55, 5.43, 10.29),
      new lbr(       52, 5.73, 117.32),
      new lbr(       52, 0.23, 1368.66),
      new lbr(       50, 6.08, 525.76),
      new lbr(       47, 3.63, 1478.87),
      new lbr(       47, 0.51, 1265.57),
      new lbr(       40, 4.16, 1692.17),
      new lbr(       34, 0.10, 302.16),
      new lbr(       33, 5.04, 220.41),
      new lbr(       32, 5.37, 508.35),
      new lbr(       29, 5.42, 1272.68),
      new lbr(       29, 3.36, 4.67),
      new lbr(       29, 0.76, 88.87),
      new lbr(       25, 1.61, 831.86) },
      { // L2
      new lbr(    47234, 4.32148, 7.11355),
      new lbr(    38966, 0, 0),
      new lbr(    30629, 2.93021, 529.69097),
      new lbr(     3189, 1.0550, 522.5774),
      new lbr(     2729, 4.8455, 536.8045),
      new lbr(     2723, 3.4141, 1059.3819),
      new lbr(     1721, 4.1873, 14.2271),
      new lbr(      383, 5.768, 419.485),
      new lbr(      378, 0.760, 515.464),
      new lbr(      367, 6.055, 103.093),
      new lbr(      337, 3.786, 3.181),
      new lbr(      308, 0.694, 206.186),
      new lbr(      218, 3.814, 1589.073),
      new lbr(      199, 5.340, 1066.495),
      new lbr(      197, 2.484, 3.932),
      new lbr(      156, 1.406, 1052.268),
      new lbr(      146, 3.814, 639.897),
      new lbr(      142, 1.634, 426.598),
      new lbr(      130, 5.837, 412.371),
      new lbr(      117, 1.414, 625.670),
      new lbr(       97, 4.03, 110.21),
      new lbr(       91, 1.11, 95.98),
      new lbr(       87, 2.52, 632.78),
      new lbr(       79, 4.64, 543.92),
      new lbr(       72, 2.22, 735.88),
      new lbr(       58, 0.83, 199.07),
      new lbr(       57, 3.12, 213.30),
      new lbr(       49, 1.67, 309.28),
      new lbr(       40, 4.02, 21.34),
      new lbr(       40, 0.62, 323.51),
      new lbr(       36, 2.33, 728.76),
      new lbr(       29, 3.61, 10.29),
      new lbr(       28, 3.24, 838.97),
      new lbr(       26, 4.50, 742.99),
      new lbr(       26, 2.51, 1162.47),
      new lbr(       25, 1.22, 1045.15),
      new lbr(       24, 3.01, 956.29),
      new lbr(       19, 4.29, 532.87),
      new lbr(       18, 0.81, 508.35),
      new lbr(       17, 4.20, 2118.76),
      new lbr(       17, 1.83, 526.51),
      new lbr(       15, 5.81, 1596.19),
      new lbr(       15, 0.68, 942.06),
      new lbr(       15, 4.00, 117.32),
      new lbr(       14, 5.95, 316.39),
      new lbr(       14, 1.80, 302.16),
      new lbr(       13, 2.52, 88.87),
      new lbr(       13, 4.37, 1169.59),
      new lbr(       11, 4.44, 525.76),
      new lbr(       10, 1.72, 1581.96),
      new lbr(        9, 2.18, 1155.36),
      new lbr(        9, 3.29, 220.41),
      new lbr(        9, 3.32, 831.86),
      new lbr(        8, 5.76, 846.08),
      new lbr(        8, 2.71, 533.62),
      new lbr(        7, 2.18, 1265.57),
      new lbr(        6, 0.50, 949.18) },
      { // L3
      new lbr(     6502, 2.5986, 7.1135),
      new lbr(     1357, 1.3464, 529.6910),
      new lbr(      471, 2.475, 14.227),
      new lbr(      417, 3.245, 536.805),
      new lbr(      353, 2.974, 522.577),
      new lbr(      155, 2.076, 1059.382),
      new lbr(       87, 2.51, 515.46),
      new lbr(       44, 0, 0),
      new lbr(       34, 3.83, 1066.50),
      new lbr(       28, 2.45, 206.19),
      new lbr(       24, 1.28, 412.37),
      new lbr(       23, 2.98, 543.92),
      new lbr(       20, 2.10, 639.90),
      new lbr(       20, 1.40, 419.48),
      new lbr(       19, 1.59, 103.09),
      new lbr(       17, 2.30, 21.34),
      new lbr(       17, 2.60, 1589.07),
      new lbr(       16, 3.15, 625.67),
      new lbr(       16, 3.36, 1052.27),
      new lbr(       13, 2.76, 95.98),
      new lbr(       13, 2.54, 199.07),
      new lbr(       13, 6.27, 426.60),
      new lbr(        9, 1.76, 10.29),
      new lbr(        9, 2.27, 110.21),
      new lbr(        7, 3.43, 309.28),
      new lbr(        7, 4.04, 728.76),
      new lbr(        6, 2.52, 508.35),
      new lbr(        5, 2.91, 1045.15),
      new lbr(        5, 5.25, 323.51),
      new lbr(        4, 4.30, 88.87),
      new lbr(        4, 3.52, 302.16),
      new lbr(        4, 4.09, 735.88),
      new lbr(        3, 1.43, 956.29),
      new lbr(        3, 4.36, 1596.19),
      new lbr(        3, 1.25, 213.30),
      new lbr(        3, 5.02, 838.97),
      new lbr(        3, 2.24, 117.32),
      new lbr(        2, 2.90, 742.99),
      new lbr(        2, 2.36, 942.06) },
      { // L4
      new lbr(      669, 0.853, 7.114),
      new lbr(      114, 3.142, 0),
      new lbr(      100, 0.743, 14.227),
      new lbr(       50, 1.65, 536.80),
      new lbr(       44, 5.82, 529.69),
      new lbr(       32, 4.86, 522.58),
      new lbr(       15, 4.29, 515.46),
      new lbr(        9, 0.71, 1059.38),
      new lbr(        5, 1.30, 543.92),
      new lbr(        4, 2.32, 1066.50),
      new lbr(        4, 0.48, 21.34),
      new lbr(        3, 3.00, 412.37),
      new lbr(        2, 0.40, 639.90),
      new lbr(        2, 4.26, 199.07),
      new lbr(        2, 4.91, 625.67),
      new lbr(        2, 4.26, 206.19),
      new lbr(        1, 5.26, 1052.27),
      new lbr(        1, 4.72, 95.98),
      new lbr(        1, 1.29, 1589.07) },
      { // L5
      new lbr(       50, 5.26, 7.11),
      new lbr(       16, 5.25, 14.23),
      new lbr(        4, 0.01, 536.80),
      new lbr(        2, 1.10, 522.58),
      new lbr(        1, 3.14, 0) }
    },
    {
      { // B0
      new lbr(  2268616, 3.5585261, 529.6909651),
      new lbr(   110090, 0, 0),
      new lbr(   109972, 3.908093, 1059.381930),
      new lbr(     8101, 3.6051, 522.5774),
      new lbr(     6438, 0.3063, 536.8045),
      new lbr(     6044, 4.2588, 1589.0729),
      new lbr(     1107, 2.9853, 1162.4747),
      new lbr(      944, 1.675, 426.598),
      new lbr(      942, 2.936, 1052.268),
      new lbr(      894, 1.754, 7.114),
      new lbr(      836, 5.179, 103.093),
      new lbr(      767, 2.155, 632.784),
      new lbr(      684, 3.678, 213.299),
      new lbr(      629, 0.643, 1066.495),
      new lbr(      559, 0.014, 846.083),
      new lbr(      532, 2.703, 110.206),
      new lbr(      464, 1.173, 949.176),
      new lbr(      431, 2.608, 419.485),
      new lbr(      351, 4.611, 2118.764),
      new lbr(      132, 4.778, 742.990),
      new lbr(      123, 3.350, 1692.166),
      new lbr(      116, 1.387, 323.505),
      new lbr(      115, 5.049, 316.392),
      new lbr(      104, 3.701, 515.464),
      new lbr(      103, 2.319, 1478.867),
      new lbr(      102, 3.153, 1581.959) },
      { // B1
      new lbr(   177352, 5.701665, 529.690965),
      new lbr(     3230, 5.7794, 1059.3819),
      new lbr(     3081, 5.4746, 522.5774),
      new lbr(     2212, 4.7348, 536.8045),
      new lbr(     1694, 3.1416, 0),
      new lbr(      346, 4.746, 1052.268),
      new lbr(      234, 5.189, 1066.495),
      new lbr(      196, 6.186, 7.114),
      new lbr(      150, 3.927, 1589.073),
      new lbr(      114, 3.439, 632.784),
      new lbr(       97, 2.91, 949.18),
      new lbr(       82, 5.08, 1162.47),
      new lbr(       77, 2.51, 103.09),
      new lbr(       77, 0.61, 419.48),
      new lbr(       74, 5.50, 515.46),
      new lbr(       61, 5.45, 213.30),
      new lbr(       50, 3.95, 735.88),
      new lbr(       46, 0.54, 110.21),
      new lbr(       45, 1.90, 846.08),
      new lbr(       37, 4.70, 543.92),
      new lbr(       36, 6.11, 316.39),
      new lbr(       32, 4.92, 1581.96) },
      { // B2
      new lbr(     8094, 1.4632, 529.6910),
      new lbr(      813, 3.1416, 0),
      new lbr(      742, 0.957, 522.577),
      new lbr(      399, 2.899, 536.805),
      new lbr(      342, 1.447, 1059.382),
      new lbr(       74, 0.41, 1052.27),
      new lbr(       46, 3.48, 1066.50),
      new lbr(       30, 1.93, 1589.07),
      new lbr(       29, 0.99, 515.46),
      new lbr(       23, 4.27, 7.11),
      new lbr(       14, 2.92, 543.92),
      new lbr(       12, 5.22, 632.78),
      new lbr(       11, 4.88, 949.18),
      new lbr(        6, 6.21, 1045.15) },
      { // B3
      new lbr(      252, 3.381, 529.691),
      new lbr(      122, 2.733, 522.577),
      new lbr(       49, 1.04, 536.80),
      new lbr(       11, 2.31, 1052.27),
      new lbr(        8, 2.77, 515.46),
      new lbr(        7, 4.25, 1059.38),
      new lbr(        6, 1.78, 1066.50),
      new lbr(        4, 1.13, 543.92),
      new lbr(        3, 3.14, 0) },
      { // B4
      new lbr(       15, 4.53, 522.58),
      new lbr(        5, 4.47, 529.69),
      new lbr(        4, 5.44, 536.80),
      new lbr(        3, 0, 0),
      new lbr(        2, 4.52, 515.46),
      new lbr(        1, 4.20, 1052.27) },
      { // B5
      new lbr(        1, 0.09, 522.58) }
    },
    {
      { // R0
      new lbr(520887429, 0, 0),
      new lbr( 25209327, 3.49108640, 529.69096509),
      new lbr(   610600, 3.841154, 1059.381930),
      new lbr(   282029, 2.574199, 632.783739),
      new lbr(   187647, 2.075904, 522.577418),
      new lbr(    86793, 0.71001, 419.48464),
      new lbr(    72063, 0.21466, 536.80451),
      new lbr(    65517, 5.97996, 316.39187),
      new lbr(    30135, 2.16132, 949.17561),
      new lbr(    29135, 1.67759, 103.09277),
      new lbr(    23947, 0.27458, 7.11355),
      new lbr(    23453, 3.54023, 735.87651),
      new lbr(    22284, 4.19363, 1589.07290),
      new lbr(    13033, 2.96043, 1162.47470),
      new lbr(    12749, 2.71550, 1052.26838),
      new lbr(     9703, 1.9067, 206.1855),
      new lbr(     9161, 4.4135, 213.2991),
      new lbr(     7895, 2.4791, 426.5982),
      new lbr(     7058, 2.1818, 1265.5675),
      new lbr(     6138, 6.2642, 846.0828),
      new lbr(     5477, 5.6573, 639.8973),
      new lbr(     4170, 2.0161, 515.4639),
      new lbr(     4137, 2.7222, 625.6702),
      new lbr(     3503, 0.5653, 1066.4955),
      new lbr(     2617, 2.0099, 1581.9593),
      new lbr(     2500, 4.5518, 838.9693),
      new lbr(     2128, 6.1275, 742.9901),
      new lbr(     1912, 0.8562, 412.3711),
      new lbr(     1611, 3.0887, 1368.6603),
      new lbr(     1479, 2.6803, 1478.8666),
      new lbr(     1231, 1.8904, 323.5054),
      new lbr(     1217, 1.8017, 110.2063),
      new lbr(     1015, 1.3867, 454.9094),
      new lbr(      999, 2.872, 309.278),
      new lbr(      961, 4.549, 2118.764),
      new lbr(      886, 4.148, 533.623),
      new lbr(      821, 1.593, 1898.351),
      new lbr(      812, 5.941, 909.819),
      new lbr(      777, 3.677, 728.763),
      new lbr(      727, 3.988, 1155.361),
      new lbr(      655, 2.791, 1685.052),
      new lbr(      654, 3.382, 1692.166),
      new lbr(      621, 4.823, 956.289),
      new lbr(      615, 2.276, 942.062),
      new lbr(      562, 0.081, 543.918),
      new lbr(      542, 0.284, 525.759) },
      { // R1
      new lbr(  1271802, 2.6493751, 529.6909651),
      new lbr(    61662, 3.00076, 1059.38193),
      new lbr(    53444, 3.89718, 522.57742),
      new lbr(    41390, 0, 0),
      new lbr(    31185, 4.88277, 536.80451),
      new lbr(    11847, 2.41330, 419.48464),
      new lbr(     9166, 4.7598, 7.1135),
      new lbr(     3404, 3.3469, 1589.0729),
      new lbr(     3203, 5.2108, 735.8765),
      new lbr(     3176, 2.7930, 103.0928),
      new lbr(     2806, 3.7422, 515.4639),
      new lbr(     2677, 4.3305, 1052.2684),
      new lbr(     2600, 3.6344, 206.1855),
      new lbr(     2412, 1.4695, 426.5982),
      new lbr(     2101, 3.9276, 639.8973),
      new lbr(     1646, 5.3095, 1066.4955),
      new lbr(     1641, 4.4163, 625.6702),
      new lbr(     1050, 3.1611, 213.2991),
      new lbr(     1025, 2.5543, 412.3711),
      new lbr(      806, 2.678, 632.784),
      new lbr(      741, 2.171, 1162.475),
      new lbr(      677, 6.250, 838.969),
      new lbr(      567, 4.577, 742.990),
      new lbr(      485, 2.469, 949.176),
      new lbr(      469, 4.710, 543.918),
      new lbr(      445, 0.403, 323.505),
      new lbr(      416, 5.368, 728.763),
      new lbr(      402, 4.605, 309.278),
      new lbr(      347, 4.681, 14.227),
      new lbr(      338, 3.168, 956.289),
      new lbr(      261, 5.343, 846.083),
      new lbr(      247, 3.923, 942.062),
      new lbr(      220, 4.842, 1368.660),
      new lbr(      203, 5.600, 1155.361),
      new lbr(      200, 4.439, 1045.155),
      new lbr(      197, 3.706, 2118.764),
      new lbr(      196, 3.759, 199.072),
      new lbr(      184, 4.265, 95.979),
      new lbr(      180, 4.402, 532.872),
      new lbr(      170, 4.846, 526.510),
      new lbr(      146, 6.130, 533.623),
      new lbr(      133, 1.322, 110.206),
      new lbr(      132, 4.512, 525.759) },
      { // R2
      new lbr(    79645, 1.35866, 529.69097),
      new lbr(     8252, 5.7777, 522.5774),
      new lbr(     7030, 3.2748, 536.8045),
      new lbr(     5314, 1.8384, 1059.3819),
      new lbr(     1861, 2.9768, 7.1135),
      new lbr(      964, 5.480, 515.464),
      new lbr(      836, 4.199, 419.485),
      new lbr(      498, 3.142, 0),
      new lbr(      427, 2.228, 639.897),
      new lbr(      406, 3.783, 1066.495),
      new lbr(      377, 2.242, 1589.073),
      new lbr(      363, 5.368, 206.186),
      new lbr(      342, 6.099, 1052.268),
      new lbr(      339, 6.127, 625.670),
      new lbr(      333, 0.003, 426.598),
      new lbr(      280, 4.262, 412.371),
      new lbr(      257, 0.963, 632.784),
      new lbr(      230, 0.705, 735.877),
      new lbr(      201, 3.069, 543.918),
      new lbr(      200, 4.429, 103.093),
      new lbr(      139, 2.932, 14.227),
      new lbr(      114, 0.787, 728.763),
      new lbr(       95, 1.70, 838.97),
      new lbr(       86, 5.14, 323.51),
      new lbr(       83, 0.06, 309.28),
      new lbr(       80, 2.98, 742.99),
      new lbr(       75, 1.60, 956.29),
      new lbr(       70, 1.51, 213.30),
      new lbr(       67, 5.47, 199.07),
      new lbr(       62, 6.10, 1045.15),
      new lbr(       56, 0.96, 1162.47),
      new lbr(       52, 5.58, 942.06),
      new lbr(       50, 2.72, 532.87),
      new lbr(       45, 5.52, 508.35),
      new lbr(       44, 0.27, 526.51),
      new lbr(       40, 5.95, 95.98) },
      { // R3
      new lbr(     3519, 6.0580, 529.6910),
      new lbr(     1073, 1.6732, 536.8045),
      new lbr(      916, 1.413, 522.577),
      new lbr(      342, 0.523, 1059.382),
      new lbr(      255, 1.196, 7.114),
      new lbr(      222, 0.952, 515.464),
      new lbr(       90, 3.14, 0),
      new lbr(       69, 2.27, 1066.50),
      new lbr(       58, 1.41, 543.92),
      new lbr(       58, 0.53, 639.90),
      new lbr(       51, 5.98, 412.37),
      new lbr(       47, 1.58, 625.67),
      new lbr(       43, 6.12, 419.48),
      new lbr(       37, 1.18, 14.23),
      new lbr(       34, 1.67, 1052.27),
      new lbr(       34, 0.85, 206.19),
      new lbr(       31, 1.04, 1589.07),
      new lbr(       30, 4.63, 426.60),
      new lbr(       21, 2.50, 728.76),
      new lbr(       15, 0.89, 199.07),
      new lbr(       14, 0.96, 508.35),
      new lbr(       13, 1.50, 1045.15),
      new lbr(       12, 2.61, 735.88),
      new lbr(       12, 3.56, 323.51),
      new lbr(       11, 1.79, 309.28),
      new lbr(       11, 6.28, 956.29),
      new lbr(       10, 6.26, 103.09),
      new lbr(        9, 3.45, 838.97) },
      { // R4
      new lbr(      129, 0.084, 536.805),
      new lbr(      113, 4.249, 529.691),
      new lbr(       83, 3.30, 522.58),
      new lbr(       38, 2.73, 515.46),
      new lbr(       27, 5.69, 7.11),
      new lbr(       18, 5.40, 1059.38),
      new lbr(       13, 6.02, 543.92),
      new lbr(        9, 0.77, 1066.50),
      new lbr(        8, 5.68, 14.23),
      new lbr(        7, 1.43, 412.37),
      new lbr(        6, 5.12, 639.90),
      new lbr(        5, 3.34, 625.67),
      new lbr(        3, 3.40, 1052.27),
      new lbr(        3, 4.16, 728.76),
      new lbr(        3, 2.90, 426.60) },
      { // R5
      new lbr(       11, 4.75, 536.80),
      new lbr(        4, 5.92, 522.58),
      new lbr(        2, 5.57, 515.46),
      new lbr(        2, 4.30, 543.92),
      new lbr(        2, 3.69, 7.11),
      new lbr(        2, 4.13, 1059.38),
      new lbr(        2, 5.49, 1066.50) }
    }
  };
  static final private lbr saturn[][][] = {
    {
      { // L0
      new lbr( 87401354, 0, 0),
      new lbr( 11107660, 3.96205090, 213.29909544),
      new lbr(  1414151, 4.5858152, 7.1135470),
      new lbr(   398379, 0.521120, 206.185548),
      new lbr(   350769, 3.303299, 426.598191),
      new lbr(   206816, 0.246584, 103.092774),
      new lbr(    79271, 3.84007, 220.41264),
      new lbr(    23990, 4.66977, 110.20632),
      new lbr(    16574, 0.43719, 419.48464),
      new lbr(    15820, 0.93809, 632.78374),
      new lbr(    15054, 2.71670, 639.89729),
      new lbr(    14907, 5.76903, 316.39187),
      new lbr(    14610, 1.56519, 3.93215),
      new lbr(    13160, 4.44891, 14.22709),
      new lbr(    13005, 5.98119, 11.04570),
      new lbr(    10725, 3.12940, 202.25340),
      new lbr(     6126, 1.7633, 277.0350),
      new lbr(     5863, 0.2366, 529.6910),
      new lbr(     5228, 4.2078, 3.1814),
      new lbr(     5020, 3.1779, 433.7117),
      new lbr(     4593, 0.6198, 199.0720),
      new lbr(     4006, 2.2448, 63.7359),
      new lbr(     3874, 3.2228, 138.5175),
      new lbr(     3269, 0.7749, 949.1756),
      new lbr(     2954, 0.9828, 95.9792),
      new lbr(     2461, 2.0316, 735.8765),
      new lbr(     1758, 3.2658, 522.5774),
      new lbr(     1640, 5.5050, 846.0828),
      new lbr(     1581, 4.3727, 309.2783),
      new lbr(     1391, 4.0233, 323.5054),
      new lbr(     1124, 2.8373, 415.5525),
      new lbr(     1087, 4.1834, 2.4477),
      new lbr(     1017, 3.7170, 227.5262),
      new lbr(      957, 0.507, 1265.567),
      new lbr(      853, 3.421, 175.166),
      new lbr(      849, 3.191, 209.367),
      new lbr(      789, 5.007, 0.963),
      new lbr(      749, 2.144, 853.196),
      new lbr(      744, 5.253, 224.345),
      new lbr(      687, 1.747, 1052.268),
      new lbr(      654, 1.599, 0.048),
      new lbr(      634, 2.299, 412.371),
      new lbr(      625, 0.970, 210.118),
      new lbr(      580, 3.093, 74.782),
      new lbr(      546, 2.127, 350.332),
      new lbr(      543, 1.518, 9.561),
      new lbr(      530, 4.449, 117.320),
      new lbr(      478, 2.965, 137.033),
      new lbr(      474, 5.475, 742.990),
      new lbr(      452, 1.044, 490.334),
      new lbr(      449, 1.290, 127.472),
      new lbr(      372, 2.278, 217.231),
      new lbr(      355, 3.013, 838.969),
      new lbr(      347, 1.539, 340.771),
      new lbr(      343, 0.246, 0.521),
      new lbr(      330, 0.247, 1581.959),
      new lbr(      322, 0.961, 203.738),
      new lbr(      322, 2.572, 647.011),
      new lbr(      309, 3.495, 216.480),
      new lbr(      287, 2.370, 351.817),
      new lbr(      278, 0.400, 211.815),
      new lbr(      249, 1.470, 1368.660),
      new lbr(      227, 4.910, 12.530),
      new lbr(      220, 4.204, 200.769),
      new lbr(      209, 1.345, 625.670),
      new lbr(      208, 0.483, 1162.475),
      new lbr(      208, 1.283, 39.357),
      new lbr(      204, 6.011, 265.989),
      new lbr(      185, 3.503, 149.563),
      new lbr(      184, 0.973, 4.193),
      new lbr(      182, 5.491, 2.921),
      new lbr(      174, 1.863, 0.751),
      new lbr(      165, 0.440, 5.417),
      new lbr(      149, 5.736, 52.690),
      new lbr(      148, 1.535, 5.629),
      new lbr(      146, 6.231, 195.140),
      new lbr(      140, 4.295, 21.341),
      new lbr(      131, 4.068, 10.295),
      new lbr(      125, 6.277, 1898.351),
      new lbr(      122, 1.976, 4.666),
      new lbr(      118, 5.341, 554.070),
      new lbr(      117, 2.679, 1155.361),
      new lbr(      114, 5.594, 1059.382),
      new lbr(      112, 1.105, 191.208),
      new lbr(      110, 0.166, 1.484),
      new lbr(      109, 3.438, 536.805),
      new lbr(      107, 4.012, 956.289),
      new lbr(      104, 2.192, 88.866),
      new lbr(      103, 1.197, 1685.052),
      new lbr(      101, 4.965, 269.921) },
      { // L1
      new lbr(21354295596L, 0, 0),
      new lbr(  1296855, 1.8282054, 213.2990954),
      new lbr(   564348, 2.885001, 7.113547),
      new lbr(   107679, 2.277699, 206.185548),
      new lbr(    98323, 1.08070, 426.59819),
      new lbr(    40255, 2.04128, 220.41264),
      new lbr(    19942, 1.27955, 103.09277),
      new lbr(    10512, 2.74880, 14.22709),
      new lbr(     6939, 0.4049, 639.8973),
      new lbr(     4803, 2.4419, 419.4846),
      new lbr(     4056, 2.9217, 110.2063),
      new lbr(     3769, 3.6497, 3.9322),
      new lbr(     3385, 2.4169, 3.1814),
      new lbr(     3302, 1.2626, 433.7117),
      new lbr(     3071, 2.3274, 199.0720),
      new lbr(     1953, 3.5639, 11.0457),
      new lbr(     1249, 2.6280, 95.9792),
      new lbr(      922, 1.961, 227.526),
      new lbr(      706, 4.417, 529.691),
      new lbr(      650, 6.174, 202.253),
      new lbr(      628, 6.111, 309.278),
      new lbr(      487, 6.040, 853.196),
      new lbr(      479, 4.988, 522.577),
      new lbr(      468, 4.617, 63.736),
      new lbr(      417, 2.117, 323.505),
      new lbr(      408, 1.299, 209.367),
      new lbr(      352, 2.317, 632.784),
      new lbr(      344, 3.959, 412.371),
      new lbr(      340, 3.634, 316.392),
      new lbr(      336, 3.772, 735.877),
      new lbr(      332, 2.861, 210.118),
      new lbr(      289, 2.733, 117.320),
      new lbr(      281, 5.744, 2.448),
      new lbr(      266, 0.543, 647.011),
      new lbr(      230, 1.644, 216.480),
      new lbr(      192, 2.965, 224.345),
      new lbr(      173, 4.077, 846.083),
      new lbr(      167, 2.597, 21.341),
      new lbr(      136, 2.286, 10.295),
      new lbr(      131, 3.441, 742.990),
      new lbr(      128, 4.095, 217.231),
      new lbr(      109, 6.161, 415.552),
      new lbr(       98, 4.73, 838.97),
      new lbr(       94, 3.48, 1052.27),
      new lbr(       92, 3.95, 88.87),
      new lbr(       87, 1.22, 440.83),
      new lbr(       83, 3.11, 625.67),
      new lbr(       78, 6.24, 302.16),
      new lbr(       67, 0.29, 4.67),
      new lbr(       66, 5.65, 9.56),
      new lbr(       62, 4.29, 127.47),
      new lbr(       62, 1.83, 195.14),
      new lbr(       58, 2.48, 191.96),
      new lbr(       57, 5.02, 137.03),
      new lbr(       55, 0.28, 74.78),
      new lbr(       54, 5.13, 490.33),
      new lbr(       51, 1.46, 536.80),
      new lbr(       47, 1.18, 149.56),
      new lbr(       47, 5.15, 515.46),
      new lbr(       46, 2.23, 956.29),
      new lbr(       44, 2.71, 5.42),
      new lbr(       40, 0.41, 269.92),
      new lbr(       40, 3.89, 728.76),
      new lbr(       38, 0.65, 422.67),
      new lbr(       38, 2.53, 12.53),
      new lbr(       37, 3.78, 2.92),
      new lbr(       35, 6.08, 5.63),
      new lbr(       34, 3.21, 1368.66),
      new lbr(       33, 4.64, 277.03),
      new lbr(       33, 5.43, 1066.50),
      new lbr(       33, 0.30, 351.82),
      new lbr(       32, 4.39, 1155.36),
      new lbr(       31, 2.43, 52.69),
      new lbr(       30, 2.84, 203.00),
      new lbr(       30, 6.19, 284.15),
      new lbr(       30, 3.39, 1059.38),
      new lbr(       29, 2.03, 330.62),
      new lbr(       28, 2.74, 265.99),
      new lbr(       26, 4.51, 340.77) },
      { // L2
      new lbr(   116441, 1.179879, 7.113547),
      new lbr(    91921, 0.07425, 213.29910),
      new lbr(    90592, 0, 0),
      new lbr(    15277, 4.06492, 206.18555),
      new lbr(    10631, 0.25778, 220.41264),
      new lbr(    10605, 5.40964, 426.59819),
      new lbr(     4265, 1.0460, 14.2271),
      new lbr(     1216, 2.9186, 103.0928),
      new lbr(     1165, 4.6094, 639.8973),
      new lbr(     1082, 5.6913, 433.7117),
      new lbr(     1045, 4.0421, 199.0720),
      new lbr(     1020, 0.6337, 3.1814),
      new lbr(      634, 4.388, 419.485),
      new lbr(      549, 5.573, 3.932),
      new lbr(      457, 1.268, 110.206),
      new lbr(      425, 0.209, 227.526),
      new lbr(      274, 4.288, 95.979),
      new lbr(      162, 1.381, 11.046),
      new lbr(      129, 1.566, 309.278),
      new lbr(      117, 3.881, 853.196),
      new lbr(      105, 4.900, 647.011),
      new lbr(      101, 0.893, 21.341),
      new lbr(       96, 2.91, 316.39),
      new lbr(       95, 5.63, 412.37),
      new lbr(       85, 5.73, 209.37),
      new lbr(       83, 6.05, 216.48),
      new lbr(       82, 1.02, 117.32),
      new lbr(       75, 4.76, 210.12),
      new lbr(       67, 0.46, 522.58),
      new lbr(       66, 0.48, 10.29),
      new lbr(       64, 0.35, 323.51),
      new lbr(       61, 4.88, 632.78),
      new lbr(       53, 2.75, 529.69),
      new lbr(       46, 5.69, 440.83),
      new lbr(       45, 1.67, 202.25),
      new lbr(       42, 5.71, 88.87),
      new lbr(       32, 0.07, 63.74),
      new lbr(       32, 1.67, 302.16),
      new lbr(       31, 4.16, 191.96),
      new lbr(       27, 0.83, 224.34),
      new lbr(       25, 5.66, 735.88),
      new lbr(       20, 5.94, 217.23),
      new lbr(       18, 4.90, 625.67),
      new lbr(       17, 1.63, 742.99),
      new lbr(       16, 0.58, 515.46),
      new lbr(       14, 0.21, 838.97),
      new lbr(       14, 3.76, 195.14),
      new lbr(       12, 4.72, 203.00),
      new lbr(       12, 0.13, 234.64),
      new lbr(       12, 3.12, 846.08),
      new lbr(       11, 5.92, 536.80),
      new lbr(       11, 5.60, 728.76),
      new lbr(       11, 3.20, 1066.50),
      new lbr(       10, 4.99, 422.67),
      new lbr(       10, 0.26, 330.62),
      new lbr(       10, 4.15, 860.31),
      new lbr(        9, 0.46, 956.29),
      new lbr(        8, 2.14, 269.92),
      new lbr(        8, 5.25, 429.78),
      new lbr(        8, 4.03, 9.56),
      new lbr(        7, 5.40, 1052.27),
      new lbr(        6, 4.46, 284.15),
      new lbr(        6, 5.93, 405.26) },
      { // L3
      new lbr(    16039, 5.73945, 7.11355),
      new lbr(     4250, 4.5854, 213.2991),
      new lbr(     1907, 4.7608, 220.4126),
      new lbr(     1466, 5.9133, 206.1855),
      new lbr(     1162, 5.6197, 14.2271),
      new lbr(     1067, 3.6082, 426.5982),
      new lbr(      239, 3.861, 433.712),
      new lbr(      237, 5.768, 199.072),
      new lbr(      166, 5.116, 3.181),
      new lbr(      151, 2.736, 639.897),
      new lbr(      131, 4.743, 227.526),
      new lbr(       63, 0.23, 419.48),
      new lbr(       62, 4.74, 103.09),
      new lbr(       40, 5.47, 21.34),
      new lbr(       40, 5.96, 95.98),
      new lbr(       39, 5.83, 110.21),
      new lbr(       28, 3.01, 647.01),
      new lbr(       25, 0.99, 3.93),
      new lbr(       19, 1.92, 853.20),
      new lbr(       18, 4.97, 10.29),
      new lbr(       18, 1.03, 412.37),
      new lbr(       18, 4.20, 216.48),
      new lbr(       18, 3.32, 309.28),
      new lbr(       16, 3.90, 440.83),
      new lbr(       16, 5.62, 117.32),
      new lbr(       13, 1.18, 88.87),
      new lbr(       11, 5.58, 11.05),
      new lbr(       11, 5.93, 191.96),
      new lbr(       10, 3.95, 209.37),
      new lbr(        9, 3.39, 302.16),
      new lbr(        8, 4.88, 323.51),
      new lbr(        7, 0.38, 632.78),
      new lbr(        6, 2.25, 522.58),
      new lbr(        6, 1.06, 210.12),
      new lbr(        5, 4.64, 234.64),
      new lbr(        4, 3.14, 0),
      new lbr(        4, 2.31, 515.46),
      new lbr(        3, 2.20, 860.31),
      new lbr(        3, 0.59, 529.69),
      new lbr(        3, 4.93, 224.34),
      new lbr(        3, 0.42, 625.67),
      new lbr(        2, 4.77, 330.62),
      new lbr(        2, 3.35, 429.78),
      new lbr(        2, 3.20, 202.25),
      new lbr(        2, 1.19, 1066.50),
      new lbr(        2, 1.35, 405.26),
      new lbr(        2, 4.16, 223.59),
      new lbr(        2, 3.07, 654.12) },
      { // L4
      new lbr(     1662, 3.9983, 7.1135),
      new lbr(      257, 2.984, 220.413),
      new lbr(      236, 3.902, 14.227),
      new lbr(      149, 2.741, 213.299),
      new lbr(      114, 3.142, 0),
      new lbr(      110, 1.515, 206.186),
      new lbr(       68, 1.72, 426.60),
      new lbr(       40, 2.05, 433.71),
      new lbr(       38, 1.24, 199.07),
      new lbr(       31, 3.01, 227.53),
      new lbr(       15, 0.83, 639.90),
      new lbr(        9, 3.71, 21.34),
      new lbr(        6, 2.42, 419.48),
      new lbr(        6, 1.16, 647.01),
      new lbr(        4, 1.45, 95.98),
      new lbr(        4, 2.12, 440.83),
      new lbr(        3, 4.09, 110.21),
      new lbr(        3, 2.77, 412.37),
      new lbr(        3, 3.01, 88.87),
      new lbr(        3, 0.00, 853.20),
      new lbr(        3, 0.39, 103.09),
      new lbr(        2, 3.78, 117.32),
      new lbr(        2, 2.83, 234.64),
      new lbr(        2, 5.08, 309.28),
      new lbr(        2, 2.24, 216.48),
      new lbr(        2, 5.19, 302.16),
      new lbr(        1, 1.55, 191.96) },
      { // L5
      new lbr(      124, 2.259, 7.114),
      new lbr(       34, 2.16, 14.23),
      new lbr(       28, 1.20, 220.41),
      new lbr(        6, 1.22, 227.53),
      new lbr(        5, 0.24, 433.71),
      new lbr(        4, 6.23, 426.60),
      new lbr(        3, 2.97, 199.07),
      new lbr(        3, 4.29, 206.19),
      new lbr(        2, 6.25, 213.30),
      new lbr(        1, 5.28, 639.90),
      new lbr(        1, 0.24, 440.83),
      new lbr(        1, 3.14, 0) }
    },
    {
      { // B0
      new lbr(  4330678, 3.6028443, 213.2990954),
      new lbr(   240348, 2.852385, 426.598191),
      new lbr(    84746, 0, 0),
      new lbr(    34116, 0.57297, 206.18555),
      new lbr(    30863, 3.48442, 220.41264),
      new lbr(    14734, 2.11847, 639.89729),
      new lbr(     9917, 5.7900, 419.4846),
      new lbr(     6994, 4.7360, 7.1135),
      new lbr(     4808, 5.4331, 316.3919),
      new lbr(     4788, 4.9651, 110.2063),
      new lbr(     3432, 2.7326, 433.7117),
      new lbr(     1506, 6.0130, 103.0928),
      new lbr(     1060, 5.6310, 529.6910),
      new lbr(      969, 5.204, 632.784),
      new lbr(      942, 1.396, 853.196),
      new lbr(      708, 3.803, 323.505),
      new lbr(      552, 5.131, 202.253),
      new lbr(      400, 3.359, 227.526),
      new lbr(      319, 3.626, 209.367),
      new lbr(      316, 1.997, 647.011),
      new lbr(      314, 0.465, 217.231),
      new lbr(      284, 4.886, 224.345),
      new lbr(      236, 2.139, 11.046),
      new lbr(      215, 5.950, 846.083),
      new lbr(      209, 2.120, 415.552),
      new lbr(      207, 0.730, 199.072),
      new lbr(      179, 2.954, 63.736),
      new lbr(      141, 0.644, 490.334),
      new lbr(      139, 4.595, 14.227),
      new lbr(      139, 1.998, 735.877),
      new lbr(      135, 5.245, 742.990),
      new lbr(      122, 3.115, 522.577),
      new lbr(      116, 3.109, 216.480),
      new lbr(      114, 0.963, 210.118) },
      { // B1
      new lbr(   397555, 5.332900, 213.299095),
      new lbr(    49479, 3.14159, 0),
      new lbr(    18572, 6.09919, 426.59819),
      new lbr(    14801, 2.30586, 206.18555),
      new lbr(     9644, 1.6967, 220.4126),
      new lbr(     3757, 1.2543, 419.4846),
      new lbr(     2717, 5.9117, 639.8973),
      new lbr(     1455, 0.8516, 433.7117),
      new lbr(     1291, 2.9177, 7.1135),
      new lbr(      853, 0.436, 316.392),
      new lbr(      298, 0.919, 632.784),
      new lbr(      292, 5.316, 853.196),
      new lbr(      284, 1.619, 227.526),
      new lbr(      275, 3.889, 103.093),
      new lbr(      172, 0.052, 647.011),
      new lbr(      166, 2.444, 199.072),
      new lbr(      158, 5.209, 110.206),
      new lbr(      128, 1.207, 529.691),
      new lbr(      110, 2.457, 217.231),
      new lbr(       82, 2.76, 210.12),
      new lbr(       81, 2.86, 14.23),
      new lbr(       69, 1.66, 202.25),
      new lbr(       65, 1.26, 216.48),
      new lbr(       61, 1.25, 209.37),
      new lbr(       59, 1.82, 323.51),
      new lbr(       46, 0.82, 440.83),
      new lbr(       36, 1.82, 224.34),
      new lbr(       34, 2.84, 117.32),
      new lbr(       33, 1.31, 412.37),
      new lbr(       32, 1.19, 846.08),
      new lbr(       27, 4.65, 1066.50),
      new lbr(       27, 4.44, 11.05) },
      { // B2
      new lbr(    20630, 0.50482, 213.29910),
      new lbr(     3720, 3.9983, 206.1855),
      new lbr(     1627, 6.1819, 220.4126),
      new lbr(     1346, 0, 0),
      new lbr(      706, 3.039, 419.485),
      new lbr(      365, 5.099, 426.598),
      new lbr(      330, 5.279, 433.712),
      new lbr(      219, 3.828, 639.897),
      new lbr(      139, 1.043, 7.114),
      new lbr(      104, 6.157, 227.526),
      new lbr(       93, 1.98, 316.39),
      new lbr(       71, 4.15, 199.07),
      new lbr(       52, 2.88, 632.78),
      new lbr(       49, 4.43, 647.01),
      new lbr(       41, 3.16, 853.20),
      new lbr(       29, 4.53, 210.12),
      new lbr(       24, 1.12, 14.23),
      new lbr(       21, 4.35, 217.23),
      new lbr(       20, 5.31, 440.83),
      new lbr(       18, 0.85, 110.21),
      new lbr(       17, 5.68, 216.48),
      new lbr(       16, 4.26, 103.09),
      new lbr(       14, 3.00, 412.37),
      new lbr(       12, 2.53, 529.69),
      new lbr(        8, 3.32, 202.25),
      new lbr(        7, 5.56, 209.37),
      new lbr(        7, 0.29, 323.51),
      new lbr(        6, 1.16, 117.32),
      new lbr(        6, 3.61, 860.31) },
      { // B3
      new lbr(      666, 1.990, 213.299),
      new lbr(      632, 5.698, 206.186),
      new lbr(      398, 0, 0),
      new lbr(      188, 4.338, 220.413),
      new lbr(       92, 4.84, 419.48),
      new lbr(       52, 3.42, 433.71),
      new lbr(       42, 2.38, 426.60),
      new lbr(       26, 4.40, 227.53),
      new lbr(       21, 5.85, 199.07),
      new lbr(       18, 1.99, 639.90),
      new lbr(       11, 5.37, 7.11),
      new lbr(       10, 2.55, 647.01),
      new lbr(        7, 3.46, 316.39),
      new lbr(        6, 4.80, 632.78),
      new lbr(        6, 0.02, 210.12),
      new lbr(        6, 3.52, 440.83),
      new lbr(        5, 5.64, 14.23),
      new lbr(        5, 1.22, 853.20),
      new lbr(        4, 4.71, 412.37),
      new lbr(        3, 0.63, 103.09),
      new lbr(        2, 3.72, 216.48) },
      { // B4
      new lbr(       80, 1.12, 206.19),
      new lbr(       32, 3.12, 213.30),
      new lbr(       17, 2.48, 220.41),
      new lbr(       12, 3.14, 0),
      new lbr(        9, 0.38, 419.48),
      new lbr(        6, 1.56, 433.71),
      new lbr(        5, 2.63, 227.53),
      new lbr(        5, 1.28, 199.07),
      new lbr(        1, 1.43, 426.60),
      new lbr(        1, 0.67, 647.01),
      new lbr(        1, 1.72, 440.83),
      new lbr(        1, 6.18, 639.90) },
      { // B5
      new lbr(        8, 2.82, 206.19),
      new lbr(        1, 0.51, 220.41) }
    },
    {
      { // R0
      new lbr(955758136, 0, 0),
      new lbr( 52921382, 2.39226220, 213.29909544),
      new lbr(  1873680, 5.2354961, 206.1855484),
      new lbr(  1464664, 1.6476305, 426.5981909),
      new lbr(   821891, 5.935200, 316.391870),
      new lbr(   547507, 5.015326, 103.092774),
      new lbr(   371684, 2.271148, 220.412642),
      new lbr(   361778, 3.139043, 7.113547),
      new lbr(   140618, 5.704067, 632.783739),
      new lbr(   108975, 3.293136, 110.206321),
      new lbr(    69007, 5.94100, 419.48464),
      new lbr(    61053, 0.94038, 639.89729),
      new lbr(    48913, 1.55733, 202.25340),
      new lbr(    34144, 0.19519, 277.03499),
      new lbr(    32402, 5.47085, 949.17561),
      new lbr(    20937, 0.46349, 735.87651),
      new lbr(    20839, 1.52103, 433.71174),
      new lbr(    20747, 5.33256, 199.07200),
      new lbr(    15298, 3.05944, 529.69097),
      new lbr(    14296, 2.60434, 323.50542),
      new lbr(    12884, 1.64892, 138.51750),
      new lbr(    11993, 5.98051, 846.08283),
      new lbr(    11380, 1.73106, 522.57742),
      new lbr(     9796, 5.2048, 1265.5675),
      new lbr(     7753, 5.8519, 95.9792),
      new lbr(     6771, 3.0043, 14.2271),
      new lbr(     6466, 0.1773, 1052.2684),
      new lbr(     5850, 1.4552, 415.5525),
      new lbr(     5307, 0.5974, 63.7359),
      new lbr(     4696, 2.1492, 227.5262),
      new lbr(     4044, 1.6401, 209.3669),
      new lbr(     3688, 0.7802, 412.3711),
      new lbr(     3461, 1.8509, 175.1661),
      new lbr(     3420, 4.9455, 1581.9593),
      new lbr(     3401, 0.5539, 350.3321),
      new lbr(     3376, 3.6953, 224.3448),
      new lbr(     2976, 5.6847, 210.1177),
      new lbr(     2885, 1.3876, 838.9693),
      new lbr(     2881, 0.1796, 853.1964),
      new lbr(     2508, 3.5385, 742.9901),
      new lbr(     2448, 6.1841, 1368.6603),
      new lbr(     2406, 2.9656, 117.3199),
      new lbr(     2174, 0.0151, 340.7709),
      new lbr(     2024, 5.0541, 11.0457) },
      { // R1
      new lbr(  6182981, 0.2584352, 213.2990954),
      new lbr(   506578, 0.711147, 206.185548),
      new lbr(   341394, 5.796358, 426.598191),
      new lbr(   188491, 0.472157, 220.412642),
      new lbr(   186262, 3.141593, 0),
      new lbr(   143891, 1.407449, 7.113547),
      new lbr(    49621, 6.01744, 103.09277),
      new lbr(    20928, 5.09246, 639.89729),
      new lbr(    19953, 1.17560, 419.48464),
      new lbr(    18840, 1.60820, 110.20632),
      new lbr(    13877, 0.75886, 199.07200),
      new lbr(    12893, 5.94330, 433.71174),
      new lbr(     5397, 1.2885, 14.2271),
      new lbr(     4869, 0.8679, 323.5054),
      new lbr(     4247, 0.3930, 227.5262),
      new lbr(     3252, 1.2585, 95.9792),
      new lbr(     3081, 3.4366, 522.5774),
      new lbr(     2909, 4.6068, 202.2534),
      new lbr(     2856, 2.1673, 735.8765),
      new lbr(     1988, 2.4505, 412.3711),
      new lbr(     1941, 6.0239, 209.3669),
      new lbr(     1581, 1.2919, 210.1177),
      new lbr(     1340, 4.3080, 853.1964),
      new lbr(     1316, 1.2530, 117.3199),
      new lbr(     1203, 1.8665, 316.3919),
      new lbr(     1091, 0.0753, 216.4805),
      new lbr(      966, 0.480, 632.784),
      new lbr(      954, 5.152, 647.011),
      new lbr(      898, 0.983, 529.691),
      new lbr(      882, 1.885, 1052.268),
      new lbr(      874, 1.402, 224.345),
      new lbr(      785, 3.064, 838.969),
      new lbr(      740, 1.382, 625.670),
      new lbr(      658, 4.144, 309.278),
      new lbr(      650, 1.725, 742.990),
      new lbr(      613, 3.033, 63.736),
      new lbr(      599, 2.549, 217.231),
      new lbr(      503, 2.130, 3.932) },
      { // R2
      new lbr(   436902, 4.786717, 213.299095),
      new lbr(    71923, 2.50070, 206.18555),
      new lbr(    49767, 4.97168, 220.41264),
      new lbr(    43221, 3.86940, 426.59819),
      new lbr(    29646, 5.96310, 7.11355),
      new lbr(     4721, 2.4753, 199.0720),
      new lbr(     4142, 4.1067, 433.7117),
      new lbr(     3789, 3.0977, 639.8973),
      new lbr(     2964, 1.3721, 103.0928),
      new lbr(     2556, 2.8507, 419.4846),
      new lbr(     2327, 0, 0),
      new lbr(     2208, 6.2759, 110.2063),
      new lbr(     2188, 5.8555, 14.2271),
      new lbr(     1957, 4.9245, 227.5262),
      new lbr(      924, 5.464, 323.505),
      new lbr(      706, 2.971, 95.979),
      new lbr(      546, 4.129, 412.371),
      new lbr(      431, 5.178, 522.577),
      new lbr(      405, 4.173, 209.367),
      new lbr(      391, 4.481, 216.480),
      new lbr(      374, 5.834, 117.320),
      new lbr(      361, 3.277, 647.011),
      new lbr(      356, 3.192, 210.118),
      new lbr(      326, 2.269, 853.196),
      new lbr(      207, 4.022, 735.877),
      new lbr(      204, 0.088, 202.253),
      new lbr(      180, 3.597, 632.784),
      new lbr(      178, 4.097, 440.825),
      new lbr(      154, 3.135, 625.670),
      new lbr(      148, 0.136, 302.165),
      new lbr(      133, 2.594, 191.958),
      new lbr(      132, 5.933, 309.278) },
      { // R3
      new lbr(    20315, 3.02187, 213.29910),
      new lbr(     8924, 3.1914, 220.4126),
      new lbr(     6909, 4.3517, 206.1855),
      new lbr(     4087, 4.2241, 7.1135),
      new lbr(     3879, 2.0106, 426.5982),
      new lbr(     1071, 4.2036, 199.0720),
      new lbr(      907, 2.283, 433.712),
      new lbr(      606, 3.175, 227.526),
      new lbr(      597, 4.135, 14.227),
      new lbr(      483, 1.173, 639.897),
      new lbr(      393, 0, 0),
      new lbr(      229, 4.698, 419.485),
      new lbr(      188, 4.590, 110.206),
      new lbr(      150, 3.202, 103.093),
      new lbr(      121, 3.768, 323.505),
      new lbr(      102, 4.710, 95.979),
      new lbr(      101, 5.819, 412.371),
      new lbr(       93, 1.44, 647.01),
      new lbr(       84, 2.63, 216.48),
      new lbr(       73, 4.15, 117.32),
      new lbr(       62, 2.31, 440.83),
      new lbr(       55, 0.31, 853.20),
      new lbr(       50, 2.39, 209.37),
      new lbr(       45, 4.37, 191.96),
      new lbr(       41, 0.69, 522.58),
      new lbr(       40, 1.84, 302.16),
      new lbr(       38, 5.94, 88.87),
      new lbr(       32, 4.01, 21.34) },
      { // R4
      new lbr(     1202, 1.4150, 220.4126),
      new lbr(      708, 1.162, 213.299),
      new lbr(      516, 6.240, 206.186),
      new lbr(      427, 2.469, 7.114),
      new lbr(      268, 0.187, 426.598),
      new lbr(      170, 5.959, 199.072),
      new lbr(      150, 0.480, 433.712),
      new lbr(      145, 1.442, 227.526),
      new lbr(      121, 2.405, 14.227),
      new lbr(       47, 5.57, 639.90),
      new lbr(       19, 5.86, 647.01),
      new lbr(       17, 0.53, 440.83),
      new lbr(       16, 2.90, 110.21),
      new lbr(       15, 0.30, 419.48),
      new lbr(       14, 1.30, 412.37),
      new lbr(       13, 2.09, 323.51),
      new lbr(       11, 0.22, 95.98),
      new lbr(       11, 2.46, 117.32),
      new lbr(       10, 3.14, 0),
      new lbr(        9, 1.56, 88.87),
      new lbr(        9, 2.28, 21.34),
      new lbr(        9, 0.68, 216.48),
      new lbr(        8, 1.27, 234.64) },
      { // R5
      new lbr(      129, 5.913, 220.413),
      new lbr(       32, 0.69, 7.11),
      new lbr(       27, 5.91, 227.53),
      new lbr(       20, 4.95, 433.71),
      new lbr(       20, 0.67, 14.23),
      new lbr(       14, 2.67, 206.19),
      new lbr(       14, 1.46, 199.07),
      new lbr(       13, 4.59, 426.60),
      new lbr(        7, 4.63, 213.30),
      new lbr(        5, 3.61, 639.90),
      new lbr(        4, 4.90, 440.83),
      new lbr(        3, 4.07, 647.01),
      new lbr(        3, 4.66, 191.96),
      new lbr(        3, 0.49, 323.51),
      new lbr(        3, 3.18, 419.48),
      new lbr(        2, 3.70, 88.87),
      new lbr(        2, 3.32, 95.98),
      new lbr(        2, 0.56, 117.32) }
    }
  };
  static final private lbr uranus[][][] = {
    {
      { // L0
      new lbr(548129294, 0, 0),
      new lbr(  9260408, 0.8910642, 74.7815986),
      new lbr(  1504248, 3.6271926, 1.4844727),
      new lbr(   365982, 1.899622, 73.297126),
      new lbr(   272328, 3.358237, 149.563197),
      new lbr(    70328, 5.39254, 63.73590),
      new lbr(    68893, 6.09292, 76.26607),
      new lbr(    61999, 2.26952, 2.96895),
      new lbr(    61951, 2.85099, 11.04570),
      new lbr(    26469, 3.14152, 71.81265),
      new lbr(    25711, 6.11380, 454.90937),
      new lbr(    21079, 4.36059, 148.07872),
      new lbr(    17819, 1.74437, 36.64856),
      new lbr(    14613, 4.73732, 3.93215),
      new lbr(    11163, 5.82682, 224.34480),
      new lbr(    10998, 0.48865, 138.51750),
      new lbr(     9527, 2.9552, 35.1641),
      new lbr(     7546, 5.2363, 109.9457),
      new lbr(     4220, 3.2333, 70.8494),
      new lbr(     4052, 2.2775, 151.0477),
      new lbr(     3490, 5.4831, 146.5943),
      new lbr(     3355, 1.0655, 4.4534),
      new lbr(     3144, 4.7520, 77.7505),
      new lbr(     2927, 4.6290, 9.5612),
      new lbr(     2922, 5.3524, 85.8273),
      new lbr(     2273, 4.3660, 70.3282),
      new lbr(     2149, 0.6075, 38.1330),
      new lbr(     2051, 1.5177, 0.1119),
      new lbr(     1992, 4.9244, 277.0350),
      new lbr(     1667, 3.6274, 380.1278),
      new lbr(     1533, 2.5859, 52.6902),
      new lbr(     1376, 2.0428, 65.2204),
      new lbr(     1372, 4.1964, 111.4302),
      new lbr(     1284, 3.1135, 202.2534),
      new lbr(     1282, 0.5427, 222.8603),
      new lbr(     1244, 0.9161, 2.4477),
      new lbr(     1221, 0.1990, 108.4612),
      new lbr(     1151, 4.1790, 33.6796),
      new lbr(     1150, 0.9334, 3.1814),
      new lbr(     1090, 1.7750, 12.5302),
      new lbr(     1072, 0.2356, 62.2514),
      new lbr(      946, 1.192, 127.472),
      new lbr(      708, 5.183, 213.299),
      new lbr(      653, 0.966, 78.714),
      new lbr(      628, 0.182, 984.600),
      new lbr(      607, 5.432, 529.691),
      new lbr(      559, 3.358, 0.521),
      new lbr(      524, 2.013, 299.126),
      new lbr(      483, 2.106, 0.963),
      new lbr(      471, 1.407, 184.727),
      new lbr(      467, 0.415, 145.110),
      new lbr(      434, 5.521, 183.243),
      new lbr(      405, 5.987, 8.077),
      new lbr(      399, 0.338, 415.552),
      new lbr(      396, 5.870, 351.817),
      new lbr(      379, 2.350, 56.622),
      new lbr(      310, 5.833, 145.631),
      new lbr(      300, 5.644, 22.091),
      new lbr(      294, 5.839, 39.618),
      new lbr(      252, 1.637, 221.376),
      new lbr(      249, 4.746, 225.829),
      new lbr(      239, 2.350, 137.033),
      new lbr(      224, 0.516, 84.343),
      new lbr(      223, 2.843, 0.261),
      new lbr(      220, 1.922, 67.668),
      new lbr(      217, 6.142, 5.938),
      new lbr(      216, 4.778, 340.771),
      new lbr(      208, 5.580, 68.844),
      new lbr(      202, 1.297, 0.048),
      new lbr(      199, 0.956, 152.532),
      new lbr(      194, 1.888, 456.394),
      new lbr(      193, 0.916, 453.425),
      new lbr(      187, 1.319, 0.160),
      new lbr(      182, 3.536, 79.235),
      new lbr(      173, 1.539, 160.609),
      new lbr(      172, 5.680, 219.891),
      new lbr(      170, 3.677, 5.417),
      new lbr(      169, 5.879, 18.159),
      new lbr(      165, 1.424, 106.977),
      new lbr(      163, 3.050, 112.915),
      new lbr(      158, 0.738, 54.175),
      new lbr(      147, 1.263, 59.804),
      new lbr(      143, 1.300, 35.425),
      new lbr(      139, 5.386, 32.195),
      new lbr(      139, 4.260, 909.819),
      new lbr(      124, 1.374, 7.114),
      new lbr(      110, 2.027, 554.070),
      new lbr(      109, 5.706, 77.963),
      new lbr(      104, 5.028, 0.751),
      new lbr(      104, 1.458, 24.379),
      new lbr(      103, 0.681, 14.978) },
      { // L1
      new lbr(7502543122L, 0, 0),
      new lbr(   154458, 5.242017, 74.781599),
      new lbr(    24456, 1.71256, 1.48447),
      new lbr(     9258, 0.4284, 11.0457),
      new lbr(     8266, 1.5022, 63.7359),
      new lbr(     7842, 1.3198, 149.5632),
      new lbr(     3899, 0.4648, 3.9322),
      new lbr(     2284, 4.1737, 76.2661),
      new lbr(     1927, 0.5301, 2.9689),
      new lbr(     1233, 1.5863, 70.8494),
      new lbr(      791, 5.436, 3.181),
      new lbr(      767, 1.996, 73.297),
      new lbr(      482, 2.984, 85.827),
      new lbr(      450, 4.138, 138.517),
      new lbr(      446, 3.723, 224.345),
      new lbr(      427, 4.731, 71.813),
      new lbr(      354, 2.583, 148.079),
      new lbr(      348, 2.454, 9.561),
      new lbr(      317, 5.579, 52.690),
      new lbr(      206, 2.363, 2.448),
      new lbr(      189, 4.202, 56.622),
      new lbr(      184, 0.284, 151.048),
      new lbr(      180, 5.684, 12.530),
      new lbr(      171, 3.001, 78.714),
      new lbr(      158, 2.909, 0.963),
      new lbr(      155, 5.591, 4.453),
      new lbr(      154, 4.652, 35.164),
      new lbr(      152, 2.942, 77.751),
      new lbr(      143, 2.590, 62.251),
      new lbr(      121, 4.148, 127.472),
      new lbr(      116, 3.732, 65.220),
      new lbr(      102, 4.188, 145.631),
      new lbr(      102, 6.034, 0.112),
      new lbr(       88, 3.99, 18.16),
      new lbr(       88, 6.16, 202.25),
      new lbr(       81, 2.64, 22.09),
      new lbr(       72, 6.05, 70.33),
      new lbr(       69, 4.05, 77.96),
      new lbr(       59, 3.70, 67.67),
      new lbr(       47, 3.54, 351.82),
      new lbr(       44, 5.91, 7.11),
      new lbr(       43, 5.72, 5.42),
      new lbr(       39, 4.92, 222.86),
      new lbr(       36, 5.90, 33.68),
      new lbr(       36, 3.29, 8.08),
      new lbr(       36, 3.33, 71.60),
      new lbr(       35, 5.08, 38.13),
      new lbr(       31, 5.62, 984.60),
      new lbr(       31, 5.50, 59.80),
      new lbr(       31, 5.46, 160.61),
      new lbr(       30, 1.66, 447.80),
      new lbr(       29, 1.15, 462.02),
      new lbr(       29, 4.52, 84.34),
      new lbr(       27, 5.54, 131.40),
      new lbr(       27, 6.15, 299.13),
      new lbr(       26, 4.99, 137.03),
      new lbr(       25, 5.74, 380.13) },
      { // L2
      new lbr(    53033, 0, 0),
      new lbr(     2358, 2.2601, 74.7816),
      new lbr(      769, 4.526, 11.046),
      new lbr(      552, 3.258, 63.736),
      new lbr(      542, 2.276, 3.932),
      new lbr(      529, 4.923, 1.484),
      new lbr(      258, 3.691, 3.181),
      new lbr(      239, 5.858, 149.563),
      new lbr(      182, 6.218, 70.849),
      new lbr(       54, 1.44, 76.27),
      new lbr(       49, 6.03, 56.62),
      new lbr(       45, 3.91, 2.45),
      new lbr(       45, 0.81, 85.83),
      new lbr(       38, 1.78, 52.69),
      new lbr(       37, 4.46, 2.97),
      new lbr(       33, 0.86, 9.56),
      new lbr(       29, 5.10, 73.30),
      new lbr(       24, 2.11, 18.16),
      new lbr(       22, 5.99, 138.52),
      new lbr(       22, 4.82, 78.71),
      new lbr(       21, 2.40, 77.96),
      new lbr(       21, 2.17, 224.34),
      new lbr(       17, 2.54, 145.63),
      new lbr(       17, 3.47, 12.53),
      new lbr(       12, 0.02, 22.09),
      new lbr(       11, 0.08, 127.47),
      new lbr(       10, 5.16, 71.60),
      new lbr(       10, 4.46, 62.25),
      new lbr(        9, 4.26, 7.11),
      new lbr(        8, 5.50, 67.67),
      new lbr(        7, 1.25, 5.42),
      new lbr(        6, 3.36, 447.80),
      new lbr(        6, 5.45, 65.22),
      new lbr(        6, 4.52, 151.05),
      new lbr(        6, 5.73, 462.02) },
      { // L3
      new lbr(      121, 0.024, 74.782),
      new lbr(       68, 4.12, 3.93),
      new lbr(       53, 2.39, 11.05),
      new lbr(       46, 0, 0),
      new lbr(       45, 2.04, 3.18),
      new lbr(       44, 2.96, 1.48),
      new lbr(       25, 4.89, 63.74),
      new lbr(       21, 4.55, 70.85),
      new lbr(       20, 2.31, 149.56),
      new lbr(        9, 1.58, 56.62),
      new lbr(        4, 0.23, 18.16),
      new lbr(        4, 5.39, 76.27),
      new lbr(        4, 0.95, 77.96),
      new lbr(        3, 4.98, 85.83),
      new lbr(        3, 4.13, 52.69),
      new lbr(        3, 0.37, 78.71),
      new lbr(        2, 0.86, 145.63),
      new lbr(        2, 5.66, 9.56) },
      { // L4
      new lbr(      114, 3.142, 0),
      new lbr(        6, 4.58, 74.78),
      new lbr(        3, 0.35, 11.05),
      new lbr(        1, 3.42, 56.62) }
    },
    {
      { // B0
      new lbr(  1346278, 2.6187781, 74.7815986),
      new lbr(    62341, 5.08111, 149.56320),
      new lbr(    61601, 3.14159, 0),
      new lbr(     9964, 1.6160, 76.2661),
      new lbr(     9926, 0.5763, 73.2971),
      new lbr(     3259, 1.2612, 224.3448),
      new lbr(     2972, 2.2437, 1.4845),
      new lbr(     2010, 6.0555, 148.0787),
      new lbr(     1522, 0.2796, 63.7359),
      new lbr(      924, 4.038, 151.048),
      new lbr(      761, 6.140, 71.813),
      new lbr(      522, 3.321, 138.517),
      new lbr(      463, 0.743, 85.827),
      new lbr(      437, 3.381, 529.691),
      new lbr(      435, 0.341, 77.751),
      new lbr(      431, 3.554, 213.299),
      new lbr(      420, 5.213, 11.046),
      new lbr(      245, 0.788, 2.969),
      new lbr(      233, 2.257, 222.860),
      new lbr(      216, 1.591, 38.133),
      new lbr(      180, 3.725, 299.126),
      new lbr(      175, 1.236, 146.594),
      new lbr(      174, 1.937, 380.128),
      new lbr(      160, 5.336, 111.430),
      new lbr(      144, 5.962, 35.164),
      new lbr(      116, 5.739, 70.849),
      new lbr(      106, 0.941, 70.328),
      new lbr(      102, 2.619, 78.714) },
      { // B1
      new lbr(   206366, 4.123943, 74.781599),
      new lbr(     8563, 0.3382, 149.5632),
      new lbr(     1726, 2.1219, 73.2971),
      new lbr(     1374, 0, 0),
      new lbr(     1369, 3.0686, 76.2661),
      new lbr(      451, 3.777, 1.484),
      new lbr(      400, 2.848, 224.345),
      new lbr(      307, 1.255, 148.079),
      new lbr(      154, 3.786, 63.736),
      new lbr(      112, 5.573, 151.048),
      new lbr(      111, 5.329, 138.517),
      new lbr(       83, 3.59, 71.81),
      new lbr(       56, 3.40, 85.83),
      new lbr(       54, 1.70, 77.75),
      new lbr(       42, 1.21, 11.05),
      new lbr(       41, 4.45, 78.71),
      new lbr(       32, 3.77, 222.86),
      new lbr(       30, 2.56, 2.97),
      new lbr(       27, 5.34, 213.30),
      new lbr(       26, 0.42, 380.13) },
      { // B2
      new lbr(     9212, 5.8004, 74.7816),
      new lbr(      557, 0, 0),
      new lbr(      286, 2.177, 149.563),
      new lbr(       95, 3.84, 73.30),
      new lbr(       45, 4.88, 76.27),
      new lbr(       20, 5.46, 1.48),
      new lbr(       15, 0.88, 138.52),
      new lbr(       14, 2.85, 148.08),
      new lbr(       14, 5.07, 63.74),
      new lbr(       10, 5.00, 224.34),
      new lbr(        8, 6.27, 78.71) },
      { // B3
      new lbr(      268, 1.251, 74.782),
      new lbr(       11, 3.14, 0),
      new lbr(        6, 4.01, 149.56),
      new lbr(        3, 5.78, 73.30) },
      { // B4
      new lbr(        6, 2.85, 74.78) }
    },
    {
      { // R0
      new lbr(1921264848L, 0, 0),
      new lbr( 88784984, 5.60377527, 74.78159857),
      new lbr(  3440836, 0.3283610, 73.2971259),
      new lbr(  2055653, 1.7829517, 149.5631971),
      new lbr(   649322, 4.522473, 76.266071),
      new lbr(   602248, 3.860038, 63.735898),
      new lbr(   496404, 1.401399, 454.909367),
      new lbr(   338526, 1.580027, 138.517497),
      new lbr(   243508, 1.570866, 71.812653),
      new lbr(   190522, 1.998094, 1.484473),
      new lbr(   161858, 2.791379, 148.078724),
      new lbr(   143706, 1.383686, 11.045700),
      new lbr(    93192, 0.17437, 36.64856),
      new lbr(    89806, 3.66105, 109.94569),
      new lbr(    71424, 4.24509, 224.34480),
      new lbr(    46677, 1.39977, 35.16409),
      new lbr(    39026, 3.36235, 277.03499),
      new lbr(    39010, 1.66971, 70.84945),
      new lbr(    36755, 3.88649, 146.59425),
      new lbr(    30349, 0.70100, 151.04767),
      new lbr(    29156, 3.18056, 77.75054),
      new lbr(    25786, 3.78538, 85.82730),
      new lbr(    25620, 5.25656, 380.12777),
      new lbr(    22637, 0.72519, 529.69097),
      new lbr(    20473, 2.79640, 70.32818),
      new lbr(    20472, 1.55589, 202.25340),
      new lbr(    17901, 0.55455, 2.96895),
      new lbr(    15503, 5.35405, 38.13304),
      new lbr(    14702, 4.90434, 108.46122),
      new lbr(    12897, 2.62154, 111.43016),
      new lbr(    12328, 5.96039, 127.47180),
      new lbr(    11959, 1.75044, 984.60033),
      new lbr(    11853, 0.99343, 52.69020),
      new lbr(    11696, 3.29826, 3.93215),
      new lbr(    11495, 0.43774, 65.22037),
      new lbr(    10793, 1.42105, 213.29910),
      new lbr(     9111, 4.9964, 62.2514),
      new lbr(     8421, 5.2535, 222.8603),
      new lbr(     8402, 5.0388, 415.5525),
      new lbr(     7449, 0.7949, 351.8166),
      new lbr(     7329, 3.9728, 183.2428),
      new lbr(     6046, 5.6796, 78.7138),
      new lbr(     5524, 3.1150, 9.5612),
      new lbr(     5445, 5.1058, 145.1098),
      new lbr(     5238, 2.6296, 33.6796),
      new lbr(     4079, 3.2206, 340.7709),
      new lbr(     3919, 4.2502, 39.6175),
      new lbr(     3802, 6.1099, 184.7273),
      new lbr(     3781, 3.4584, 456.3938),
      new lbr(     3687, 2.4872, 453.4249),
      new lbr(     3102, 4.1403, 219.8914),
      new lbr(     2963, 0.8298, 56.6224),
      new lbr(     2942, 0.4239, 299.1264),
      new lbr(     2940, 2.1464, 137.0330),
      new lbr(     2938, 3.6766, 140.0020),
      new lbr(     2865, 0.3100, 12.5302),
      new lbr(     2538, 4.8546, 131.4039),
      new lbr(     2364, 0.4425, 554.0700),
      new lbr(     2183, 2.9404, 305.3462) },
      { // R1
      new lbr(  1479896, 3.6720571, 74.7815986),
      new lbr(    71212, 6.22601, 63.73590),
      new lbr(    68627, 6.13411, 149.56320),
      new lbr(    24060, 3.14159, 0),
      new lbr(    21468, 2.60177, 76.26607),
      new lbr(    20857, 5.24625, 11.04570),
      new lbr(    11405, 0.01848, 70.84945),
      new lbr(     7497, 0.4236, 73.2971),
      new lbr(     4244, 1.4169, 85.8273),
      new lbr(     3927, 3.1551, 71.8127),
      new lbr(     3578, 2.3116, 224.3448),
      new lbr(     3506, 2.5835, 138.5175),
      new lbr(     3229, 5.2550, 3.9322),
      new lbr(     3060, 0.1532, 1.4845),
      new lbr(     2564, 0.9808, 148.0787),
      new lbr(     2429, 3.9944, 52.6902),
      new lbr(     1645, 2.6535, 127.4718),
      new lbr(     1584, 1.4305, 78.7138),
      new lbr(     1508, 5.0600, 151.0477),
      new lbr(     1490, 2.6756, 56.6224),
      new lbr(     1413, 4.5746, 202.2534),
      new lbr(     1403, 1.3699, 77.7505),
      new lbr(     1228, 1.0470, 62.2514),
      new lbr(     1033, 0.2646, 131.4039),
      new lbr(      992, 2.172, 65.220),
      new lbr(      862, 5.055, 351.817),
      new lbr(      744, 3.076, 35.164),
      new lbr(      687, 2.499, 77.963),
      new lbr(      647, 4.473, 70.328),
      new lbr(      624, 0.863, 9.561),
      new lbr(      604, 0.907, 984.600),
      new lbr(      575, 3.231, 447.796),
      new lbr(      562, 2.718, 462.023),
      new lbr(      530, 5.917, 213.299),
      new lbr(      528, 5.151, 2.969) },
      { // R2
      new lbr(    22440, 0.69953, 74.78160),
      new lbr(     4727, 1.6990, 63.7359),
      new lbr(     1682, 4.6483, 70.8494),
      new lbr(     1650, 3.0966, 11.0457),
      new lbr(     1434, 3.5212, 149.5632),
      new lbr(      770, 0, 0),
      new lbr(      500, 6.172, 76.266),
      new lbr(      461, 0.767, 3.932),
      new lbr(      390, 4.496, 56.622),
      new lbr(      390, 5.527, 85.827),
      new lbr(      292, 0.204, 52.690),
      new lbr(      287, 3.534, 73.297),
      new lbr(      273, 3.847, 138.517),
      new lbr(      220, 1.964, 131.404),
      new lbr(      216, 0.848, 77.963),
      new lbr(      205, 3.248, 78.714),
      new lbr(      149, 4.898, 127.472),
      new lbr(      129, 2.081, 3.181) },
      { // R3
      new lbr(     1164, 4.7345, 74.7816),
      new lbr(      212, 3.343, 63.736),
      new lbr(      196, 2.980, 70.849),
      new lbr(      105, 0.958, 11.046),
      new lbr(       73, 1.00, 149.56),
      new lbr(       72, 0.03, 56.62),
      new lbr(       55, 2.59, 3.93),
      new lbr(       36, 5.65, 77.96),
      new lbr(       34, 3.82, 76.27),
      new lbr(       32, 3.60, 131.40) },
      { // R4
      new lbr(       53, 3.01, 74.78),
      new lbr(       10, 1.91, 56.62) }
    }
  };
  static final private lbr neptune[][][] = {
    {
      { // L0
      new lbr(531188633, 0, 0),
      new lbr(  1798476, 2.9010127, 38.1330356),
      new lbr(  1019728, 0.4858092, 1.4844727),
      new lbr(   124532, 4.830081, 36.648563),
      new lbr(    42064, 5.41055, 2.96895),
      new lbr(    37715, 6.09222, 35.16409),
      new lbr(    33785, 1.24489, 76.26607),
      new lbr(    16483, 0.00008, 491.55793),
      new lbr(     9199, 4.9375, 39.6175),
      new lbr(     8994, 0.2746, 175.1661),
      new lbr(     4216, 1.9871, 73.2971),
      new lbr(     3365, 1.0359, 33.6796),
      new lbr(     2285, 4.2061, 4.4534),
      new lbr(     1434, 2.7834, 74.7816),
      new lbr(      900, 2.076, 109.946),
      new lbr(      745, 3.190, 71.813),
      new lbr(      506, 5.748, 114.399),
      new lbr(      400, 0.350, 1021.249),
      new lbr(      345, 3.462, 41.102),
      new lbr(      340, 3.304, 77.751),
      new lbr(      323, 2.248, 32.195),
      new lbr(      306, 0.497, 0.521),
      new lbr(      287, 4.505, 0.048),
      new lbr(      282, 2.246, 146.594),
      new lbr(      267, 4.889, 0.963),
      new lbr(      252, 5.782, 388.465),
      new lbr(      245, 1.247, 9.561),
      new lbr(      233, 2.505, 137.033),
      new lbr(      227, 1.797, 453.425),
      new lbr(      170, 3.324, 108.461),
      new lbr(      151, 2.192, 33.940),
      new lbr(      150, 2.997, 5.938),
      new lbr(      148, 0.859, 111.430),
      new lbr(      119, 3.677, 2.448),
      new lbr(      109, 2.416, 183.243),
      new lbr(      103, 0.041, 0.261),
      new lbr(      103, 4.404, 70.328),
      new lbr(      102, 5.705, 0.112) },
      { // L1
      new lbr(3837687717L, 0, 0),
      new lbr(    16604, 4.86319, 1.48447),
      new lbr(    15807, 2.27923, 38.13304),
      new lbr(     3335, 3.6820, 76.2661),
      new lbr(     1306, 3.6732, 2.9689),
      new lbr(      605, 1.505, 35.164),
      new lbr(      179, 3.453, 39.618),
      new lbr(      107, 2.451, 4.453),
      new lbr(      106, 2.755, 33.680),
      new lbr(       73, 5.49, 36.65),
      new lbr(       57, 1.86, 114.40),
      new lbr(       57, 5.22, 0.52),
      new lbr(       35, 4.52, 74.78),
      new lbr(       32, 5.90, 77.75),
      new lbr(       30, 3.67, 388.47),
      new lbr(       29, 5.17, 9.56),
      new lbr(       29, 5.17, 2.45),
      new lbr(       26, 5.25, 168.05) },
      { // L2
      new lbr(    53893, 0, 0),
      new lbr(      296, 1.855, 1.484),
      new lbr(      281, 1.191, 38.133),
      new lbr(      270, 5.721, 76.266),
      new lbr(       23, 1.21, 2.97),
      new lbr(        9, 4.43, 35.16),
      new lbr(        7, 0.54, 2.45) },
      { // L3
      new lbr(       31, 0, 0),
      new lbr(       15, 1.35, 76.27),
      new lbr(       12, 6.04, 1.48),
      new lbr(       12, 6.11, 38.13) },
      { // L4
      new lbr(      114, 3.142, 0) }
    },
    {
      { // B0
      new lbr(  3088623, 1.4410437, 38.1330356),
      new lbr(    27780, 5.91272, 76.26607),
      new lbr(    27624, 0, 0),
      new lbr(    15448, 3.50877, 39.61751),
      new lbr(    15355, 2.52124, 36.64856),
      new lbr(     2000, 1.5100, 74.7816),
      new lbr(     1968, 4.3778, 1.4845),
      new lbr(     1015, 3.2156, 35.1641),
      new lbr(      606, 2.802, 73.297),
      new lbr(      595, 2.129, 41.102),
      new lbr(      589, 3.187, 2.969),
      new lbr(      402, 4.169, 114.399),
      new lbr(      280, 1.682, 77.751),
      new lbr(      262, 3.767, 213.299),
      new lbr(      254, 3.271, 453.425),
      new lbr(      206, 4.257, 529.691),
      new lbr(      140, 3.530, 137.033) },
      { // B1
      new lbr(   227279, 3.807931, 38.133036),
      new lbr(     1803, 1.9758, 76.2661),
      new lbr(     1433, 3.1416, 0),
      new lbr(     1386, 4.8256, 36.6486),
      new lbr(     1073, 6.0805, 39.6175),
      new lbr(      148, 3.858, 74.782),
      new lbr(      136, 0.478, 1.484),
      new lbr(       70, 6.19, 35.16),
      new lbr(       52, 5.05, 73.30),
      new lbr(       43, 0.31, 114.40),
      new lbr(       37, 4.89, 41.10),
      new lbr(       37, 5.76, 2.97),
      new lbr(       26, 5.22, 213.30) },
      { // B2
      new lbr(     9691, 5.5712, 38.1330),
      new lbr(       79, 3.63, 76.27),
      new lbr(       72, 0.45, 36.65),
      new lbr(       59, 3.14, 0),
      new lbr(       30, 1.61, 39.62),
      new lbr(        6, 5.61, 74.78) },
      { // B3
      new lbr(      273, 1.017, 38.133),
      new lbr(        2, 0, 0),
      new lbr(        2, 2.37, 36.65),
      new lbr(        2, 5.33, 76.27) },
      { // B4
      new lbr(        6, 2.67, 38.13) }
    },
    {
      { // R0
      new lbr(3007013206L, 0, 0),
      new lbr( 27062259, 1.32999459, 38.13303564),
      new lbr(  1691764, 3.2518614, 36.6485629),
      new lbr(   807831, 5.185928, 1.484473),
      new lbr(   537761, 4.521139, 35.164090),
      new lbr(   495726, 1.571057, 491.557929),
      new lbr(   274572, 1.845523, 175.166060),
      new lbr(   135134, 3.372206, 39.617508),
      new lbr(   121802, 5.797544, 76.266071),
      new lbr(   100895, 0.377027, 73.297126),
      new lbr(    69792, 3.79617, 2.96895),
      new lbr(    46688, 5.74938, 33.67962),
      new lbr(    24594, 0.50802, 109.94569),
      new lbr(    16939, 1.59422, 71.81265),
      new lbr(    14230, 1.07786, 74.78160),
      new lbr(    12012, 1.92062, 1021.24889),
      new lbr(     8395, 0.6782, 146.5943),
      new lbr(     7572, 1.0715, 388.4652),
      new lbr(     5721, 2.5906, 4.4534),
      new lbr(     4840, 1.9069, 41.1020),
      new lbr(     4483, 2.9057, 529.6910),
      new lbr(     4421, 1.7499, 108.4612),
      new lbr(     4354, 0.6799, 32.1951),
      new lbr(     4270, 3.4134, 453.4249),
      new lbr(     3381, 0.8481, 183.2428),
      new lbr(     2881, 1.9860, 137.0330),
      new lbr(     2879, 3.6742, 350.3321),
      new lbr(     2636, 3.0976, 213.2991),
      new lbr(     2530, 5.7984, 490.0735),
      new lbr(     2523, 0.4863, 493.0424),
      new lbr(     2306, 2.8096, 70.3282),
      new lbr(     2087, 0.6186, 33.9402) },
      { // R1
      new lbr(   236339, 0.704980, 38.133036),
      new lbr(    13220, 3.32015, 1.48447),
      new lbr(     8622, 6.2163, 35.1641),
      new lbr(     2702, 1.8814, 39.6175),
      new lbr(     2155, 2.0943, 2.9689),
      new lbr(     2153, 5.1687, 76.2661),
      new lbr(     1603, 0, 0),
      new lbr(     1464, 1.1842, 33.6796),
      new lbr(     1136, 3.9189, 36.6486),
      new lbr(      898, 5.241, 388.465),
      new lbr(      790, 0.533, 168.053),
      new lbr(      760, 0.021, 182.280),
      new lbr(      607, 1.077, 1021.249),
      new lbr(      572, 3.401, 484.444),
      new lbr(      561, 2.887, 498.671) },
      { // R2
      new lbr(     4247, 5.8991, 38.1330),
      new lbr(      218, 0.346, 1.484),
      new lbr(      163, 2.239, 168.053),
      new lbr(      156, 4.594, 182.280),
      new lbr(      127, 2.848, 35.164) },
      { // R3
      new lbr(      166, 4.552, 38.133) }
    }
  };
  static final private lbr earthj2000[][][] = {
    {
      { // L0
      new lbr(175347046, 0, 0),
      new lbr(  3341656, 4.6692568, 6283.0758500),
      new lbr(    34894, 4.62610, 12566.15170),
      new lbr(     3497, 2.7441, 5753.3849),
      new lbr(     3418, 2.8289, 3.5231),
      new lbr(     3136, 3.6277, 77713.7715),
      new lbr(     2676, 4.4181, 7860.4194),
      new lbr(     2343, 6.1352, 3930.2097),
      new lbr(     1324, 0.7425, 11506.7698),
      new lbr(     1273, 2.0371, 529.6910),
      new lbr(     1199, 1.1096, 1577.3435),
      new lbr(      990, 5.233, 5884.927),
      new lbr(      902, 2.045, 26.298),
      new lbr(      857, 3.508, 398.149),
      new lbr(      780, 1.179, 5223.694),
      new lbr(      753, 2.533, 5507.553),
      new lbr(      505, 4.583, 18849.228),
      new lbr(      492, 4.205, 775.523),
      new lbr(      357, 2.920, 0.067),
      new lbr(      317, 5.849, 11790.629),
      new lbr(      284, 1.899, 796.298),
      new lbr(      271, 0.315, 10977.079),
      new lbr(      243, 0.345, 5486.778),
      new lbr(      206, 4.806, 2544.314),
      new lbr(      205, 1.869, 5573.143),
      new lbr(      202, 2.458, 6069.777),
      new lbr(      156, 0.833, 213.299),
      new lbr(      132, 3.411, 2942.463),
      new lbr(      126, 1.083, 20.775),
      new lbr(      115, 0.645, 0.980),
      new lbr(      103, 0.636, 4694.003),
      new lbr(      102, 0.976, 15720.839),
      new lbr(      102, 4.267, 7.114),
      new lbr(       99, 6.21, 2146.17),
      new lbr(       98, 0.68, 155.42),
      new lbr(       86, 5.98, 161000.69),
      new lbr(       85, 1.30, 6275.96),
      new lbr(       85, 3.67, 71430.70),
      new lbr(       80, 1.81, 17260.15),
      new lbr(       79, 3.04, 12036.46),
      new lbr(       75, 1.76, 5088.63),
      new lbr(       74, 3.50, 3154.69),
      new lbr(       74, 4.68, 801.82),
      new lbr(       70, 0.83, 9437.76),
      new lbr(       62, 3.98, 8827.39),
      new lbr(       61, 1.82, 7084.90),
      new lbr(       57, 2.78, 6286.60),
      new lbr(       56, 4.39, 14143.50),
      new lbr(       56, 3.47, 6279.55),
      new lbr(       52, 0.19, 12139.55),
      new lbr(       52, 1.33, 1748.02),
      new lbr(       51, 0.28, 5856.48),
      new lbr(       49, 0.49, 1194.45),
      new lbr(       41, 5.37, 8429.24),
      new lbr(       41, 2.40, 19651.05),
      new lbr(       39, 6.17, 10447.39),
      new lbr(       37, 6.04, 10213.29),
      new lbr(       37, 2.57, 1059.38),
      new lbr(       36, 1.71, 2352.87),
      new lbr(       36, 1.78, 6812.77),
      new lbr(       33, 0.59, 17789.85),
      new lbr(       30, 0.44, 83996.85),
      new lbr(       30, 2.74, 1349.87),
      new lbr(       25, 3.16, 4690.48) },
      { // L1
      new lbr(628307584999L, 0, 0),
      new lbr(   206059, 2.678235, 6283.075850),
      new lbr(     4303, 2.6351, 12566.1517),
      new lbr(      425, 1.590, 3.523),
      new lbr(      119, 5.796, 26.298),
      new lbr(      109, 2.966, 1577.344),
      new lbr(       93, 2.59, 18849.23),
      new lbr(       72, 1.14, 529.69),
      new lbr(       68, 1.87, 398.15),
      new lbr(       67, 4.41, 5507.55),
      new lbr(       59, 2.89, 5223.69),
      new lbr(       56, 2.17, 155.42),
      new lbr(       45, 0.40, 796.30),
      new lbr(       36, 0.47, 775.52),
      new lbr(       29, 2.65, 7.11),
      new lbr(       21, 5.34, 0.98),
      new lbr(       19, 1.85, 5486.78),
      new lbr(       19, 4.97, 213.30),
      new lbr(       17, 2.99, 6275.96),
      new lbr(       16, 0.03, 2544.31),
      new lbr(       16, 1.43, 2146.17),
      new lbr(       15, 1.21, 10977.08),
      new lbr(       12, 2.83, 1748.02),
      new lbr(       12, 3.26, 5088.63),
      new lbr(       12, 5.27, 1194.45),
      new lbr(       12, 2.08, 4694.00),
      new lbr(       11, 0.77, 553.57),
      new lbr(       10, 1.30, 6286.60),
      new lbr(       10, 4.24, 1349.87),
      new lbr(        9, 2.70, 242.73),
      new lbr(        9, 5.64, 951.72),
      new lbr(        8, 5.30, 2352.87),
      new lbr(        6, 2.65, 9437.76),
      new lbr(        6, 4.67, 4690.48) },
      { // L2
      new lbr(     8722, 1.0725, 6283.0758),
      new lbr(      991, 3.1416, 0),
      new lbr(      295, 0.437, 12566.152),
      new lbr(       27, 0.05, 3.52),
      new lbr(       16, 5.19, 26.30),
      new lbr(       16, 3.69, 155.42),
      new lbr(        9, 0.30, 18849.23),
      new lbr(        9, 2.06, 77713.77),
      new lbr(        7, 0.83, 775.52),
      new lbr(        5, 4.66, 1577.34),
      new lbr(        4, 1.03, 7.11),
      new lbr(        4, 3.44, 5573.14),
      new lbr(        3, 5.14, 796.30),
      new lbr(        3, 6.05, 5507.55),
      new lbr(        3, 1.19, 242.73),
      new lbr(        3, 6.12, 529.69),
      new lbr(        3, 0.30, 398.15),
      new lbr(        3, 2.28, 553.57),
      new lbr(        2, 4.38, 5223.69),
      new lbr(        2, 3.75, 0.98) },
      { // L3
      new lbr(      289, 5.842, 6283.076),
      new lbr(       21, 6.05, 12566.15),
      new lbr(        3, 5.20, 155.42),
      new lbr(        3, 3.14, 0),
      new lbr(        1, 4.72, 3.52),
      new lbr(        1, 5.97, 242.73),
      new lbr(        1, 5.54, 18849.23) },
      { // L4
      new lbr(        8, 4.14, 6283.08),
      new lbr(        1, 3.28, 12566.15) }
    },
    {
      { // B0
      new lbr(      280, 3.199, 84334.662),
      new lbr(      102, 5.422, 5507.553),
      new lbr(       80, 3.88, 5223.69),
      new lbr(       44, 3.70, 2352.87),
      new lbr(       32, 4.00, 1577.34) },
      { // B1
      new lbr(   227778, 3.413766, 6283.075850),
      new lbr(     3806, 3.3706, 12566.1517),
      new lbr(     3620, 0, 0),
      new lbr(       72, 3.33, 18849.23),
      new lbr(        8, 3.89, 5507.55),
      new lbr(        8, 1.79, 5223.69),
      new lbr(        6, 5.20, 2352.87) },
      { // B2
      new lbr(     9721, 5.1519, 6283.07585),
      new lbr(      233, 3.1416, 0),
      new lbr(      134, 0.644, 12566.152),
      new lbr(        7, 1.07, 18849.23) },
      { // B3
      new lbr(      276, 0.595, 6283.076),
      new lbr(       17, 3.14, 0),
      new lbr(        4, 0.12, 12566.15) },
      { // B4
      new lbr(        6, 2.27, 6283.08),
      new lbr(        1, 0, 0) }
    },
    {
      { // R0
      new lbr(100013989, 0, 0),
      new lbr(  1670700, 3.0984635, 6283.0758500),
      new lbr(    13956, 3.05525, 12566.15170),
      new lbr(     3084, 5.1985, 77713.7715),
      new lbr(     1628, 1.1739, 5753.3849),
      new lbr(     1576, 2.8469, 7860.4194),
      new lbr(      925, 5.453, 11506.770),
      new lbr(      542, 4.564, 3930.210),
      new lbr(      472, 3.661, 5884.927),
      new lbr(      346, 0.964, 5507.553),
      new lbr(      329, 5.900, 5223.694),
      new lbr(      307, 0.299, 5573.143),
      new lbr(      243, 4.273, 11790.629),
      new lbr(      212, 5.847, 1577.344),
      new lbr(      186, 5.022, 10977.079),
      new lbr(      175, 3.012, 18849.228),
      new lbr(      110, 5.055, 5486.778),
      new lbr(       98, 0.89, 6069.78),
      new lbr(       86, 5.69, 15720.84),
      new lbr(       86, 1.27, 161000.69),
      new lbr(       65, 0.27, 17260.15),
      new lbr(       63, 0.92, 529.69),
      new lbr(       57, 2.01, 83996.85),
      new lbr(       56, 5.24, 71430.70),
      new lbr(       49, 3.25, 2544.31),
      new lbr(       47, 2.58, 775.52),
      new lbr(       45, 5.54, 9437.76),
      new lbr(       43, 6.01, 6275.96),
      new lbr(       39, 5.36, 4694.00),
      new lbr(       38, 2.39, 8827.39),
      new lbr(       37, 0.83, 19651.05),
      new lbr(       37, 4.90, 12139.55),
      new lbr(       36, 1.67, 12036.46),
      new lbr(       35, 1.84, 2942.46),
      new lbr(       33, 0.24, 7084.90),
      new lbr(       32, 0.18, 5088.63),
      new lbr(       32, 1.78, 398.15),
      new lbr(       28, 1.21, 6286.60),
      new lbr(       28, 1.90, 6279.55),
      new lbr(       26, 4.59, 10447.39) },
      { // R1
      new lbr(   103019, 1.107490, 6283.075850),
      new lbr(     1721, 1.0644, 12566.1517),
      new lbr(      702, 3.142, 0),
      new lbr(       32, 1.02, 18849.23),
      new lbr(       31, 2.84, 5507.55),
      new lbr(       25, 1.32, 5223.69),
      new lbr(       18, 1.42, 1577.34),
      new lbr(       10, 5.91, 10977.08),
      new lbr(        9, 1.42, 6275.96),
      new lbr(        9, 0.27, 5486.78) },
      { // R2
      new lbr(     4359, 5.7846, 6283.0758),
      new lbr(      124, 5.579, 12566.152),
      new lbr(       12, 3.14, 0),
      new lbr(        9, 3.63, 77713.77),
      new lbr(        6, 1.87, 5573.14),
      new lbr(        3, 5.47, 18849.23) },
      { // R3
      new lbr(      145, 4.273, 6283.076),
      new lbr(        7, 3.92, 12566.15) },
      { // R4
      new lbr(        4, 2.56, 6283.08) }
    }
  };

  static final public Planet Earth   = new Planet("NS.Earth", earth);
  static final public Planet Mercury = new Planet("NS.Mercury", mercury);
  static final public Planet Venus   = new Planet("NS.Venus", venus);
  static final public Planet Mars    = new Planet("NS.Mars", mars);
  static final public Planet Jupiter = new Planet("NS.Jupiter", jupiter);
  static final public Planet Saturn  = new Planet("NS.Saturn", saturn);
  static final public Planet Uranus  = new Planet("NS.Uranus", uranus);
  static final public Planet Neptune = new Planet("NS.Neptune", neptune);
  static final public Planet EJ2000  = new Planet("NS.Pluto", earthj2000);
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Class for Pluto's coordinates.
 *
 * @author Brian Simpson
 */
class Pluto {
  static final private double D2R = MapParms.Deg2Rad;
  // e2k is the mean obliquity of the ecliptic at epoch J2000.0 (23.4392911 deg)
  static final double sine2k = 0.397777156;  // sin e2k
  static final double cose2k = 0.917482062;  // cos e2k

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates J2000 coordinates for Pluto.
   *
   * @param j Julian date
   * @param sunx Sun's J2000 equatorial rectangular coordinate X in AUs
   * @param suny Sun's J2000 equatorial rectangular coordinate Y in AUs
   * @param sunz Sun's J2000 equatorial rectangular coordinate Z in AUs
   * @param ra On return, has right ascension in radians
   * @param dec On return, has declination in radians
   * @param dist On return, has distance from earth in AUs
   * @param sdist On return, has distance from sun in AUs
   */
  /* Method from "Astronomical Algorithms" 2nd Edition by Jean Meeus */
  /* (c) March 2000 by Willmann-Bell, Inc.     Chapter 37            */
  // Book says method only valid for 1885 - 2099 (< 1 revolution).
  // I tried an approx. Pluto year of 90712 days (= 248.36 Julian years).
  // +/- 4 of these years yielded results within a fraction of a degree
  // within J2000.  (Good enough for me.)  For example:
  // j            l            b            r
  // 2814393.0    250.66953    11.15479     30.23689    (+~4 revolutions)
  // 2451545.0    250.54623    11.16146     30.22323    J2000.0
  // 2088697.0    250.38769    11.19406     30.22172    (-~4 revolutions)
  // Perhaps better results if tweaked 90712 days, but planet perturbations
  // will limit how close results can get.  Thus it appears that even though
  // the following method is valid (accurate) for 1885 - 2099, it does not
  // produce bad results for 1000 - 3000.
  static public void calcPluto2000(double j, double sunx, double suny,
                                   double sunz, double[] ra, double[] dec,
                                   double[] dist, double[] sdist) {
    double T, J, S, P;
    double alpha, tau = 0, l, b, r = 0, cosl, sinl, cosb, sinb;
    double x, y, z;
    double xi = 0, eta = 0, zeta = 0;
    int i;

    for ( int loop = 1; loop <= 2; loop++ ) {
      j -= tau;
      T = (j - 2451545) / 36525;
      J = 34.35 + 3034.9057 * T;
      S = 50.08 + 1222.1138 * T;
      P = 238.96 + 144.9600 * T;
      l = b = r = 0;
      for ( i = 0; i < plutotbl.length; i += 9 ) {
        alpha = plutotbl[i] * J + plutotbl[i+1] * S + plutotbl[i+2] * P;
        alpha = (alpha % 360) * D2R;  // 2 % 3 = 2, (-2) % 3 = -2
        l += plutotbl[i+3] * Math.sin(alpha) + plutotbl[i+4] * Math.cos(alpha);
        b += plutotbl[i+5] * Math.sin(alpha) + plutotbl[i+6] * Math.cos(alpha);
        r += plutotbl[i+7] * Math.sin(alpha) + plutotbl[i+8] * Math.cos(alpha);
      }
      l = 238.958116 + 144.96 * T + l / 1e6;
      b = -3.908239 + b / 1e6;
      r = 40.7241346 + r / 1e7;
      l %= 360;  // 2 % 3 = 2, (-2) % 3 = -2

      l *= D2R;
      b *= D2R;
      cosl = Math.cos(l);
      sinl = Math.sin(l);
      cosb = Math.cos(b);
      sinb = Math.sin(b);
      x = r * cosl * cosb;
      y = r * (sinl * cosb * cose2k - sinb * sine2k);
      z = r * (sinl * cosb * sine2k + sinb * cose2k);

      // Apply formula 33.10 from P. 229
      xi = sunx + x;
      eta = suny + y;
      zeta = sunz + z;
      dist[0] = Math.sqrt(xi*xi + eta*eta + zeta*zeta);
      tau = 0.0057755183 * dist[0];
    }
    ra[0] = Math.atan2(eta, xi);
    dec[0] = Math.asin(zeta / dist[0]);
    sdist[0] = r;
    //stem.out.println("alpha = " + (ra[0]/D2R) + ", delta = " + (dec[0]/D2R));
    //stem.out.println("dist = " + dist[0]);
    //stem.out.println("sdist = " + sdist[0]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates heliocentric coordinates for solar system view.
   *
   * @param t Julian millennia from J2000.0
   * @param l On return, has heliocentric ecliptical longitude in radians
   * @param b On return, has heliocentric ecliptical latitude in radians
   * @param r On return, has radius vector in AU
   */
  static public void calcHelioCentricCoord(double t, double[] l,
                                           double[] b, double[] r) {
    double J, S, P;
    double alpha;

    t *= 10;    // Convert Julian millennia to centuries (from J2000.0)
    J = 34.35 + 3034.9057 * t;
    S = 50.08 + 1222.1138 * t;
    P = 238.96 + 144.9600 * t;
    l[0] = b[0] = r[0] = 0;
    for ( int i = 0; i < plutotbl.length; i += 9 ) {
      alpha = plutotbl[i] * J + plutotbl[i+1] * S + plutotbl[i+2] * P;
      alpha = (alpha % 360) * D2R;  // 2 % 3 = 2, (-2) % 3 = -2
      l[0] += plutotbl[i+3] * Math.sin(alpha) + plutotbl[i+4] * Math.cos(alpha);
      b[0] += plutotbl[i+5] * Math.sin(alpha) + plutotbl[i+6] * Math.cos(alpha);
      r[0] += plutotbl[i+7] * Math.sin(alpha) + plutotbl[i+8] * Math.cos(alpha);
    }
    l[0] = 238.958116 + 144.96 * t + l[0] / 1e6;
    b[0] = -3.908239 + b[0] / 1e6;
    r[0] = 40.7241346 + r[0] / 1e7;

    // Do quick and dirty conversion to current epoch
    l[0] += t * 360 / 260; // A 360 degree advance every 26000 years

    l[0] %= 360;  // 2 % 3 = 2, (-2) % 3 = -2

    l[0] *= D2R;
    b[0] *= D2R;
  }

  static final private int plutotbl[] = {
  //Argument      Longitude           Latitude          Radius vector
  //J  S  P      A         B         A         B         A         B
    0, 0, 1,-19799805, 19850055, -5452852,-14974862, 66865439, 68951812,
    0, 0, 2,   897144, -4954829,  3527812,  1672790,-11827535,  -332538,
    0, 0, 3,   611149,  1211027, -1050748,   327647,  1593179, -1438890,
    0, 0, 4,  -341243,  -189585,   178690,  -292153,   -18444,   483220,
    0, 0, 5,   129287,   -34992,    18650,   100340,   -65977,   -85431,
    0, 0, 6,   -38164,    30893,   -30697,   -25823,    31174,    -6032,
    0, 1,-1,    20442,    -9987,     4878,    11248,    -5794,    22161,
    0, 1, 0,    -4063,    -5071,      226,      -64,     4601,     4032,
    0, 1, 1,    -6016,    -3336,     2030,     -836,    -1729,      234,
    0, 1, 2,    -3956,     3039,       69,     -604,     -415,      702,
    0, 1, 3,     -667,     3572,     -247,     -567,      239,      723,
    0, 2,-2,     1276,      501,      -57,        1,       67,      -67,
    0, 2,-1,     1152,     -917,     -122,      175,     1034,     -451,
    0, 2, 0,      630,    -1277,      -49,     -164,     -129,      504,
    1,-1, 0,     2571,     -459,     -197,      199,      480,     -231,
    1,-1, 1,      899,    -1449,      -25,      217,        2,     -441,
    1, 0,-3,    -1016,     1043,      589,     -248,    -3359,      265,
    1, 0,-2,    -2343,    -1012,     -269,      711,     7856,    -7832,
    1, 0,-1,     7042,      788,      185,      193,       36,    45763,
    1, 0, 0,     1199,     -338,      315,      807,     8663,     8547,
    1, 0, 1,      418,      -67,     -130,      -43,     -809,     -769,
    1, 0, 2,      120,     -274,        5,        3,      263,     -144,
    1, 0, 3,      -60,     -159,        2,       17,     -126,       32,
    1, 0, 4,      -82,      -29,        2,        5,      -35,      -16,
    1, 1,-3,      -36,      -29,        2,        3,      -19,       -4,
    1, 1,-2,      -40,        7,        3,        1,      -15,        8,
    1, 1,-1,      -14,       22,        2,       -1,       -4,       12,
    1, 1, 0,        4,       13,        1,       -1,        5,        6,
    1, 1, 1,        5,        2,        0,       -1,        3,        1,
    1, 1, 3,       -1,        0,        0,        0,        6,       -2,
    2, 0,-6,        2,        0,        0,       -2,        2,        2,
    2, 0,-5,       -4,        5,        2,        2,       -2,       -2,
    2, 0,-4,        4,       -7,       -7,        0,       14,       13,
    2, 0,-3,       14,       24,       10,       -8,      -63,       13,
    2, 0,-2,      -49,      -34,       -3,       20,      136,     -236,
    2, 0,-1,      163,      -48,        6,        5,      273,     1065,
    2, 0, 0,        9,      -24,       14,       17,      251,      149,
    2, 0, 1,       -4,        1,       -2,        0,      -25,       -9,
    2, 0, 2,       -3,        1,        0,        0,        9,       -2,
    2, 0, 3,        1,        3,        0,        0,       -8,        7,
    3, 0,-2,       -3,       -1,        0,        1,        2,      -10,
    3, 0,-1,        5,       -3,        0,        0,       19,       35,
    3, 0, 0,        0,        0,        1,        0,       10,        3
  };
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Class for lunar coordinates.
 *
 * @author Brian Simpson
 */
class Moon {
  static final private double D2R = MapParms.Deg2Rad;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates Precessed coordinates for the Moon.
   *
   * @param jde Julian date
   * @param lambda On return, has ecliptical longitude in radians
   * @param beta On return, has ecliptical latitude in radians
   * @param dist On return, has distance between earth-moon centers in AUs
   */
  /* Method from "Astronomical Algorithms" 2nd Edition by Jean Meeus */
  /* (c) March 2000 by Willmann-Bell, Inc.     Chapter 47            */
  static public void getCoordinates(double jde, double[] lambda, double[] beta,
                                    double[] dist) {
    double arg, sin, cos;
    double Lambda, Beta, Delta;
    int i, j;

    double T = (jde - 2451545) / 36525;

    double Lp = 218.3164477 + T * (481267.88123421 - T * (0.0015786 -
                T / (538841 - 65194000 / T)));
    double D  = 297.8501921 + T * (445267.1114034 - T * (0.0018819 -
                T / (545868 - 113065000 / T)));
    double M  = 357.5291092 + T * (35999.0502909 - T * (0.0001536 -
                T / 24490000));
    double Mp = 134.9633964 + T * (477198.8675055 + T * (0.0087414 +
                T / (69699 - 14712000 / T)));
    double F  = 93.2720950 + T * (483202.0175233 - T * (0.0036539 +
                T / (3526000 - 863310000 / T)));
    double A1 = 119.75 + 131.849 * T;
    double A2 = 53.09 + 479264.290 * T;
    double A3 = 313.45 + 481266.484 * T;
    double E  = 1 - T * (0.002516 + T * 0.0000074);
    Lp %= 360; // 2 % 3 = 2, (-2) % 3 = -2
    D  %= 360;
    M  %= 360;
    Mp %= 360;
    F  %= 360;
    A1 %= 360;
    A2 %= 360;
    A3 %= 360;

    Lambda = 0;
    Beta = 0;
    Delta = 0;
    for ( i = 0; i < lr.length; i += 6 ) {
      arg = (lr[i] * D + lr[i+1] * M + lr[i+2] * Mp + lr[i+3] * F) % 360 * D2R;
      sin = lr[i+4] * Math.sin(arg);
      cos = lr[i+5] * Math.cos(arg);
      j = lr[i+1] * lr[i+1];
      if ( j > 0 ) {
        sin *= E;
        cos *= E;
      }
      if ( j > 1 ) {
        sin *= E;
        cos *= E;
      }
      Lambda += sin;
      Delta += cos;
    }
    for ( i = 0; i < b.length; i += 5 ) {
      arg = (b[i] * D + b[i+1] * M + b[i+2] * Mp + b[i+3] * F) % 360 * D2R;
      sin = b[i+4] * Math.sin(arg);
      j = b[i+1] * b[i+1];
      if ( j > 0 ) {
        sin *= E;
      }
      if ( j > 1 ) {
        sin *= E;
      }
      Beta += sin;
    }
    Lambda +=  3958 * Math.sin(A1 * D2R) +
               1962 * Math.sin((Lp - F) * D2R) +
                318 * Math.sin(A2 * D2R);
    Beta   += -2235 * Math.sin(Lp * D2R) +
                382 * Math.sin(A3 * D2R) +
                175 * Math.sin((A1 - F) * D2R) +
                175 * Math.sin((A1 + F) * D2R) +
                127 * Math.sin((Lp - Mp) * D2R) +
               -115 * Math.sin((Lp + Mp) * D2R);
    Lambda = Lambda / 1000000 + Lp;
    Beta /= 1000000;
    Delta = 385000.56 + Delta / 1000;
    //stem.out.println("Lambda = " + Lambda + " degrees");
    //stem.out.println("Beta = " + Beta + " degrees");
    //stem.out.println("Delta = " + Delta + " km");

    lambda[0] = Lambda * D2R;
    beta[0] = Beta * D2R;
    dist[0] = Delta / NearSkyDB.AU2KM; // km to AU
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the illuminated fraction of the moon.
   *
   * @param ra1 Right ascension of moon (or sun) in rad
   * @param dec1 Declination of moon (or sun) in rad
   * @param ra2 Right ascension of sun (or moon) in rad
   * @param dec2 Declination of sun (or moon) in rad
   */
  static public double getIllumFrac(double ra1, double dec1,
                                    double ra2, double dec2) {
    // Uses slightly simplified formula that doesn't take into account
    // the relative distances of the sun and the moon
    double cospsi = Math.sin(dec1) * Math.sin(dec2) +
                    Math.cos(dec1) * Math.cos(dec2) * Math.cos(ra1 - ra2);
    return (1 - cospsi) / 2;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the position angle of the moon's bright limb in radians.
   *
   * @param ram Right ascension of moon in rad
   * @param decm Declination of moon in rad
   * @param ras Right ascension of sun in rad
   * @param decs Declination of sun in rad
   */
  static public double getPositionAngle(double ram, double decm,
                                        double ras, double decs) {
    double num = Math.cos(decs) * Math.sin(ras - ram);
    double den = Math.sin(decs) * Math.cos(decm) -
                 Math.cos(decs) * Math.sin(decm) * Math.cos(ras - ram);
    return Math.atan2(num, den);
  }

  static final private int lr[] = {
    0,  0,  1,  0, 6288774, -20905355,
    2,  0, -1,  0, 1274027,  -3699111,
    2,  0,  0,  0,  658314,  -2955968,
    0,  0,  2,  0,  213618,   -569925,
    0,  1,  0,  0, -185116,     48888,
    0,  0,  0,  2, -114332,     -3149,
    2,  0, -2,  0,   58793,    246158,
    2, -1, -1,  0,   57066,   -152138,
    2,  0,  1,  0,   53322,   -170733,
    2, -1,  0,  0,   45758,   -204586,
    0,  1, -1,  0,  -40923,   -129620,
    1,  0,  0,  0,  -34720,    108743,
    0,  1,  1,  0,  -30383,    104755,
    2,  0,  0, -2,   15327,     10321,
    0,  0,  1,  2,  -12528,         0,
    0,  0,  1, -2,   10980,     79661,
    4,  0, -1,  0,   10675,    -34782,
    0,  0,  3,  0,   10034,    -23210,
    4,  0, -2,  0,    8548,    -21636,
    2,  1, -1,  0,   -7888,     24208,
    2,  1,  0,  0,   -6766,     30824,
    1,  0, -1,  0,   -5163,     -8379,
    1,  1,  0,  0,    4987,    -16675,
    2, -1,  1,  0,    4036,    -12831,
    2,  0,  2,  0,    3994,    -10445,
    4,  0,  0,  0,    3861,    -11650,
    2,  0, -3,  0,    3665,     14403,
    0,  1, -2,  0,   -2689,     -7003,
    2,  0, -1,  2,   -2602,         0,
    2, -1, -2,  0,    2390,     10056,
    1,  0,  1,  0,   -2348,      6322,
    2, -2,  0,  0,    2236,     -9884,
    0,  1,  2,  0,   -2120,      5751,
    0,  2,  0,  0,   -2069,         0,
    2, -2, -1,  0,    2048,     -4950,
    2,  0,  1, -2,   -1773,      4130,
    2,  0,  0,  2,   -1595,         0,
    4, -1, -1,  0,    1215,     -3958,
    0,  0,  2,  2,   -1110,         0,
    3,  0, -1,  0,    -892,      3258,
    2,  1,  1,  0,    -810,      2616,
    4, -1, -2,  0,     759,     -1897,
    0,  2, -1,  0,    -713,     -2117,
    2,  2, -1,  0,    -700,      2354,
    2,  1, -2,  0,     691,         0,
    2, -1,  0, -2,     596,         0,
    4,  0,  1,  0,     549,     -1423,
    0,  0,  4,  0,     537,     -1117,
    4, -1,  0,  0,     520,     -1571,
    1,  0, -2,  0,    -487,     -1739,
    2,  1,  0, -2,    -399,         0,
    0,  0,  2, -2,    -381,     -4421,
    1,  1,  1,  0,     351,         0,
    3,  0, -2,  0,    -340,         0,
    4,  0, -3,  0,     330,         0,
    2, -1,  2,  0,     327,         0,
    0,  2,  1,  0,    -323,      1165,
    1,  1, -1,  0,     299,         0,
    2,  0,  3,  0,     294,         0,
    2,  0, -1, -2,       0,      8752
  };
  static final private int b[] = {
    0,  0,  0,  1, 5128122,
    0,  0,  1,  1,  280602,
    0,  0,  1, -1,  277693,
    2,  0,  0, -1,  173237,
    2,  0, -1,  1,   55413,
    2,  0, -1, -1,   46271,
    2,  0,  0,  1,   32573,
    0,  0,  2,  1,   17198,
    2,  0,  1, -1,    9266,
    0,  0,  2, -1,    8822,
    2, -1,  0, -1,    8216,
    2,  0, -2, -1,    4324,
    2,  0,  1,  1,    4200,
    2,  1,  0, -1,   -3359,
    2, -1, -1,  1,    2463,
    2, -1,  0,  1,    2211,
    2, -1, -1, -1,    2065,
    0,  1, -1, -1,   -1870,
    4,  0, -1, -1,    1828,
    0,  1,  0,  1,   -1794,
    0,  0,  0,  3,   -1749,
    0,  1, -1,  1,   -1565,
    1,  0,  0,  1,   -1491,
    0,  1,  1,  1,   -1475,
    0,  1,  1, -1,   -1410,
    0,  1,  0, -1,   -1344,
    1,  0,  0, -1,   -1335,
    0,  0,  3,  1,    1107,
    4,  0,  0, -1,    1021,
    4,  0, -1,  1,     833,
    0,  0,  1, -3,     777,
    4,  0, -2,  1,     671,
    2,  0,  0, -3,     607,
    2,  0,  2, -1,     596,
    2, -1,  1, -1,     491,
    2,  0, -2,  1,    -451,
    0,  0,  3, -1,     439,
    2,  0,  2,  1,     422,
    2,  0, -3, -1,     421,
    2,  1, -1,  1,    -366,
    2,  1,  0,  1,    -351,
    4,  0,  0,  1,     331,
    2, -1,  1,  1,     315,
    2, -2,  0, -1,     302,
    0,  0,  1,  3,    -283,
    2,  1,  1, -1,    -229,
    1,  1,  0, -1,     223,
    1,  1,  0,  1,     223,
    0,  1, -2, -1,    -220,
    2,  1, -1, -1,    -220,
    1,  0,  1,  1,    -185,
    2, -1, -2, -1,     181,
    0,  1,  2,  1,    -177,
    4,  0, -2, -1,     176,
    4, -1, -1, -1,     166,
    1,  0,  1, -1,    -164,
    4,  0,  1, -1,     132,
    1,  0, -1, -1,    -119,
    4, -1,  0, -1,     115,
    2, -2,  0,  1,     107
  };
}

/*------------------------------------------------------------------------------

Note:  Periodic terms formula calculates Heliocentric coordinates,
which are converted to ecliptical geocentric coordinates.

Planets except Pluto - Chap. 33, P225

  Use periodic terms formula to calculate Earth
  Use periodic terms formula to calculate planet
  Calculate light-time
  Use periodic terms formula to calculate planet
  Convert to ecliptical
  Adjust for aberration
  FK5 conversion
  Convert to equatorial (& adjust for nutation)
  Adjust for parallax

Pluto - Chap. 37, P266

  Use periodic terms formula to calculate EarthJ2000
  Reverse coordinates to get Sun, then FK5 conversion
  Use Pluto formula to calculate PlutoJ2000
  Calculate light-time
  Use Pluto formula to calculate PlutoJ2000
  Convert to equatorial
  Precess and Nutate
  Adjust for aberration (to eclip, adjust, to equat)
  Adjust for parallax

Sun - Chap. 25, P166

  Use periodic terms formula to calculate Earth
  Reverse ecliptical coordinates to get Sun
  FK5 conversion
  Adjust for aberration
  Convert to equatorial
  Adjust for parallax

Moon - P337

  Use complex moon formula to get ecliptical coordinates
  Convert to equatorial
  Adjust for parallax
  (No adjustment for light-time, FK5, or aberration)

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

Regression test for planet positions
------------------------------------

Start NV with /geocentric /nodeltat
Set to 0 time zone (e.g. go to North Pole)
Pause time, and set to 1992/12/20 00:00 (DT, JDE = 2448976.5)

Object    Distance   App RA & Dec             J2000 RA & Dec
------    --------   ----------------------   ----------------------
Mercury   1.216 AU   16h33m59.3s -20d53m32s   16h34m24.4s -20d54m25s
Venus     0.911 AU   21h 4m41.5s -18d53m17s   21h 5m 5.2s -18d51m37s
Mars      0.648 AU    7h48m35.3s  24d35m35s    7h48m58.2s  24d34m40s
Jupiter   5.599 AU   12h47m 9.6s  -3d41m55s   12h47m30.7s  -3d44m 8s
Saturn    10.514 AU  21h11m41.8s -17d15m41s   21h12m 5.2s -17d13m58s
Uranus    20.500 AU  19h13m48.6s -22d46m13s   19h14m14.2s -22d45m30s
Neptune   31.113 AU  19h17m14.6s -21d34m15s   19h17m39.9s -21d33m30s
Pluto     30.502 AU  15h41m11.2s  -5d 5m57s   15h41m33.6s  -5d 7m16s
Sun       0.984 AU   17h52m49.9s -23d25m46s   17h53m15.9s -23d25m53s
Moon      378437 KM  14h23m33.2s -18d 0m20s   14h23m55.5s -18d 2m10s

For comparison purposes, use Nasa Ephemeris Generator at
http://ssd.jpl.nasa.gov/horizons.cgi, 1992/12/19 23:59 UT, which with
59.2 seconds delta t, is approx 1992/12/20 00:00 DT.  (Note:  Recent
Nasa upgrade allows setting TT (DT) time.)

Object    Distance   App RA & Dec             J2000 RA & Dec
------    --------   ----------------------   ----------------------
Mercury   1.216 AU   16h33m59.3s -20d53m32s   16h34m24.4s -20d54m25s
Venus     0.911 AU   21h 4m41.5s -18d53m17s   21h 5m 5.2s -18d51m37s
Mars      0.648 AU    7h48m35.4s  24d35m34s    7h48m58.3s  24d34m38s
Jupiter   5.599 AU   12h47m 9.6s  -3d41m55s   12h47m30.7s  -3d44m 9s
Saturn    10.514 AU  21h11m41.8s -17d15m41s   21h12m 5.3s -17d13m58s
Uranus    20.500 AU  19h13m48.6s -22d46m13s   19h14m14.2s -22d45m30s
Neptune   31.113 AU  19h17m14.5s -21d34m15s   19h17m39.9s -21d33m30s
Pluto     30.502 AU  15h41m11.2s  -5d 5m57s   15h41m33.6s  -5d 7m16s
Sun       0.984 AU   17h52m49.9s -23d25m47s   17h53m15.9s -23d25m53s
Moon      378411 KM  14h23m33.0s -18d 0m19s   14h23m56.1s -18d 2m15s


- Additional comparisons for years 1000, 1500, 2100, 2500, 2999 -

The apparent coordinates agree closely, usually differing by no more
than a couple of digits in the last position, and matching exactly
about half the time.

The J2000 coordinates have greater mismatches.  Not sure why.  Is
Nasa using a different model for precession/nutation?  There is
close agreement for years 1000 and 2100, decent agreement for years
1500 and 2500, and some divergence at 2999 (~2 arcmin).

J2000 coordinates are astrometric, that is, they are corrected for
light-time and (lack of) precession, but not for aberration or nutation.
(Not sure what is standard for J2000, but this is the convention
of NV and Nasa Ephem. Gen.  Also correction for diurnal parallax is
done, though in this comparison geocentric is used.)

Note:  The Nasa Ephem. Gen. does not estimate future delta-t,
but rather pauses it at 64.18 seconds.  Also distances appear to be
from observer to object, not geocentric (unless observer is geocentric).

NV:    1000-Jan-01 12:26  DeltaT = 0
Nasa:  1000-Jan-01 12:00  JD = 2086308.00  DeltaT = 1577.6sec = 26.29min
Object    App RA & Dec             J2000 RA & Dec
------    ----------------------   ----------------------
Moon      13h 5m18.5s  -6d 7m20s   13h57m46.3s -11d15m 8s
          13h 5m18.7s  -6d 7m25s   13h57m46.6s -11d15m14s
Sun       19h10m47.4s -22d34m10s   20h 9m51.5s -20d12m19s
          19h10m47.4s -22d34m10s   20h 9m51.6s -20d12m19s
Mercury   18h51m10.1s -24d51m53s   19h51m35.4s -22d55m31s
          18h51m10.1s -24d51m53s   19h51m35.6s -22d55m30s
Venus     21h33m22.4s -16d18m15s   22h27m16.7s -11d28m30s
          21h33m22.5s -16d18m16s   22h27m16.8s -11d28m28s
Mars       7h58m 2.8s  25d 7m48s    8h56m54.0s  21d47m16s
           7h58m 2.9s  25d 7m48s    8h56m54.1s  21d47m15s

NV:    1500-Jan-01 12:03  DeltaT = 0
Nasa:  1500-Jan-01 12:00  JD = 2268933.00  DeltaT = 174.9sec = 2.92min
Object    App RA & Dec             J2000 RA & Dec
------    ----------------------   ----------------------
Moon      19h40m19.5s -24d37m13s   20h10m15.8s -23d16m40s
          19h40d19.0s -24d37m14s   20h10m17.1s -23d16m31s
Sun       19h26m21.2s -22d 1m 7s   19h55m56.0s -20d49m41s
          19h26m21.2s -22d 1m 7s   19h55m56.3s -20d49m36s
Mercury   19h15m12.3s -24d16m10s   19h45m21.4s -23d11m58s
          19h15m12.3s -24d16m10s   19h45m21.7s -23d11m54s
Venus     19h21m21.7s -23d 0m58s   19h51m11.5s -21d52m45s
          19h21m21.7s -23d 0m58s   19h51m11.8s -21d52m41s
Mars       1h29m28.8s  10d17m27s    1h56m 4.2s  12d47m57s
           1h29m28.8s  10d17m28s    1h56m 4.3s  12d48m07s

NV:    2100-Jan-01 12:01  DeltaT = 0
Nasa:  2100-Jan-01 12:00  JD = 2488070.00  DeltaT = 64.18sec = 1.07min
Object    App RA & Dec             J2000 RA & Dec
------    ----------------------   ----------------------
Moon      11h 3m20.9s   6d34m13s   10h58m 8.9s   7d 6m31s
          11h 3m21.0s   6d34m14s   10h58m 8.5s   7d 6m34s
Sun       18h48m20.2s -22d57m50s   18h42m18.1s -23d 4m14s
          18h48m20.2s -22d57m50s   18h42m18.1s -23d 4m14s
Mercury   19h22m47.2s -24d12m39s   19h16m44.0s -24d23m52s
          19h22m47.2s -24d12m39s   19h16m44.1s -24d23m52s
Venus     21h34m42.0s -16d20m25s   21h29m11.2s -16d46m56s
          21h34m42.0s -16d20m24s   21h29m11.2s -16d46m55s
Mars       1h49m13.7s  12d16m16s    1h43m52.8s  11d46m17s
           1h49m13.8s  12d16m17s    1h43m52.9s  11d46m18s

NV:    2500-Jan-01 12:01  DeltaT = 0
Nasa:  2500-Jan-01 12:00  JD = 2634167.00  DeltaT = 64.18sec = 1.07min
Object    App RA & Dec             J2000 RA & Dec
------    ----------------------   ----------------------
Moon      18h37m53.7s -18d13m53s   18h 8m32.5s -18d30m57s
          18h37m54.2s -18d13m50s   18h 8m35.2s -18d30m52s
Sun       18h47m54.1s -22d54m57s   18h17m32.9s -23d18m54s
          18h47m54.1s -22d54m57s   18h17m33.8s -23d18m50s
Mercury   18h12m 3.5s -24d 9m44s   17h41m25.0s -24d 7m30s
          18d12m 3.5s -24d 9m44s   17h41m26.0s -24d 7m31s
Venus     20h15m35.4s -18d 0m41s   19h46m41.0s -19d25m 7s
          20h15m35.4s -18d 0m41s   19h46m41.7s -19d24m51s
Mars      19h22m27.6s -22d59m36s   18h52m12.0s -23d48m10s
          19d22m27.7s -22d59m36s   18h52m13.0s -23d48m 1s

NV:    2999-Jan-01 12:01  DeltaT = 0
Nasa:  2999-Jan-01 12:00  JD = 2816423.00  DeltaT = 64.18sec = 1.07min
Object    App RA & Dec             J2000 RA & Dec
------    ----------------------   ----------------------
Moon      13h 7m12.2s -11d 6m27s   12h15m12.4s  -5d36m38s
          13h 7m12.3s -11d 6m28s   12h15m12.9s  -5d39m27s
Sun       18h47m28.7s -22d51m43s   17h46m35.7s -23d16m39s
          18h47m28.7s -22d51m43s   17h46m40.5s -23d16m30s
Mercury   17h27m 3.5s -21d58m47s   16h27m18.6s -20d28m 1s
          17h27m 3.5s -21d58m47s   16h27m22.7s -20d28m49s
Venus     15h31m54.1s -15d38m56s   14h36m34.8s -11d46m17s
          15h31m54.2s -15d38m56s   14h36m36.5s -11d48m15s
Mars       1h42m 2.7s  11d 7m38s    0h49m32.8s   5d50m32s
           1h42m 3.1s  11d 7m41s    0h49m33.6s   5d53m14s

Nasa gives the following messages for Jupiter - Pluto, so they were
omitted in this analysis:

 No ephemeris for target "Jupiter" prior to A.D. 1924-DEC-29 23:59:36.3837 UT
 No ephemeris for target "Uranus" prior to A.D. 1980-JAN-02 23:59:08.8160 UT
 No ephemeris for target "Neptune" prior to A.D. 1979-DEC-29 23:59:09.8161 UT
 No ephemeris for target "Pluto" prior to A.D. 1978-JUN-01 23:59:10.8151 UT
 No ephemeris for target "Jupiter" after A.D. 2049-DEC-31 23:58:55.8161 UT
 No ephemeris for target "Uranus" after A.D. 2025-JAN-03 23:58:55.8160 UT
 No ephemeris for target "Neptune" after A.D. 2048-DEC-28 23:58:55.8162 UT
 No ephemeris for target "Pluto" after A.D. 2024-DEC-30 23:58:55.8161 UT


- Diurnal parallax test -

Start NV (Do not do /geocentric /nodeltat)
Set to 0 time zone (e.g. go to North Pole)
Pause time, and set to 2003/09/10 00:00 (UT, JD = 2452892.5)

Object    Distance   App RA & Dec             J2000 RA & Dec
------    --------   ----------------------   ----------------------
- NV      Delta t = 64.54 seconds
- (Moscow using N.E.G. coords) 37:34E,55:45N
Moon      387403 KM  22h46m58.4s -13d40m 4s   22h46m47.4s -13d41m 7s
Mars      0.386 AU   22h25m 7.0s -16d27m 4s   22h24m54.5s -16d28m11s
Uranus    19.065 AU  22h10m13.5s -12d 8m44s   22h10m 1.2s -12d 9m49s
- Buenos Aires 58:26W, 34:36S
Moon      384782 KM  22h51m 1.2s -12d24m35s   22h50m50.3s -12d25m39s
Mars      0.386 AU   22h25m 8.6s -16d26m35s   22h24m56.1s -16d27m42s
Uranus    19.065 AU  22h10m13.5s -12d 8m43s   22h10m 1.2s -12d 9m48s

- Nasa Ephem. Gen.   Delta t = 64.18
- Moscow 37°34'14.2''E, 55°45'20.2''N, 178.985 m
Moon      387413 KM  22h46m58.5s -13d40m 3s   22h46m46.2s -13d41m14s
Mars      0.386 AU   22h25m 6.9s -16d27m 3s   22h24m54.4s -16d28m11s
Uranus    19.065 AU  22h10m13.5s -12d 8m44s   22h10m 1.2s -12d 9m49s
- Buenos Aires 58°26'04.6''W, 34°36'18.7''S, 42.4592 m
Moon      384791 KM  22h51m 1.3s -12d24m33s   22h50m49.1s -12d25m45s
Mars      0.386 AU   22h25m 8.5s -16d26m34s   22h24m56.0s -16d27m42s
Uranus    19.065 AU  22h10m13.5s -12d 8m44s   22h10m 1.2s -12d 9m49s


- Az/Alt test -

Must have a reasonably accurate value for delta t.  The time set
for the measurement will place the earth at the proper angle of
rotation, while the delta t offset will place the near sky objects
at the proper positions.

Start NV (Do not do /geocentric /nodeltat)
Set to 0 time zone (e.g. go to North Pole)
Pause time, and set to 1990/01/01 12:00 (UT, JD = 2447893.0)

Object    App RA & Dec               Az & Alt
------    -------------------------  ---------------------
- NV      DeltaT = 56.86 seconds, JDE = 2447893.0006581
Long/Lat  0/0
Mercury   19h50m 6.7s  -20d23m34s    142d23m     63d54m
Uranus    18h25m15.6s  -23d35m 9s    190d19m     66d 0m
Long/Lat  0/44
Mercury   19h50m 6.6s  -20d23m41s    162d56m     23d49m
Uranus    18h25m15.6s  -23d35m 9s    184d31m     22d17m
Long/Lat  0/88
Mercury   19h50m 6.4s  -20d23m41s    163d33m    -18d29m
Uranus    18h25m15.6s  -23d35m 9s    184d30m    -21d36m
- Nasa Ephem. Gen.   DeltaT = 57.183948 seconds
Long/Lat  0/0
Mercury   19h50m 6.77s -20d23m34.6s  142d22m54s  63d54m 5s (142.3817  63.9013)
Uranus    18h25m15.57s -23d35m 8.7s  190d20m 1s  66d 0m 4s (190.3336  66.0012)
Long/Lat  0/44
Mercury   19h50m 6.70s -20d23m41.0s  162d55m51s  23d49m22s (162.9308  23.8228)
Uranus    18h25m15.56s -23d35m 9.0s  184d31m20s  22d17m 4s (184.5223  22.2845)
Long/Lat  0/88
Mercury   19h50m 6.52s -20d23m41.6s  163d33m21s -18d28m40s (163.5559 -18.4777)
Uranus    18h25m15.55s -23d35m 9.0s  184d30m06s -21d35m32s (184.5016 -21.5921)

------------------------------------------------------------------------------*/

