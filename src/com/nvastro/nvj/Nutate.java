/*
 * Nutate.java  -  Nutation, the obliquity of the ecliptic, & aberration
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
 * Nutation, the obliquity of the ecliptic, & aberration.
 *
 * @author Brian Simpson
 */
public class Nutate implements Cloneable {
  /* Methods from "Astronomical Algorithms" 2nd Ed. by Jean Meeus */
  /* (c) 1998, second printing March 2000 by Willmann-Bell, Inc.  */

  /** Pi / 2 */ public static final double HalfPI = Math.PI / 2;
  /** J2000.0 */ static final public double J2000_0 = 2451545.0;
  static final private double D2R = Math.PI/180;
  private Matrix3x3 nut;      // Nutation matrix
  private double dpsi, dep;   // Nutation parameters (rad)
  private double ep0;         // Mean obliquity of the ecliptic (rad)
  private double cep, sep;    // Cos & sin of epsilon (ep0 + dep)

  /* Variables for aberration */
  static final private double k = 20.49522 * Math.PI / 648000; // (* Sec to rad)
  private double ec;          // Eccentricity of the Earth's orbit
  private double pi;          // Longitude of perihelion of Earth's orbit (rad)
  private double LSun;        // True geometric longitude of the sun referred
  // to the mean equinox of the date (rad); represented by a circle with a dot

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public Nutate() {
    nut = new Matrix3x3();    // Identity matrix
    sep = dpsi = dep = ep0 = 0;
    cep = 1;
    ec = pi = LSun = 0;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Produces a clone of this object.
   */
  public Object clone() {
    Nutate n = null;
    try {
      n = (Nutate) super.clone();
      // Do more than "shallow" copy
      n.nut = (Matrix3x3) nut.clone();
    } catch ( CloneNotSupportedException e ) { // Shouldn't happen
      ErrLogger.die(e.toString());
    }
    return n;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up nutation parameters and matrix.
   *
   * @param jday Julian Ephemeris Day
   */
  public void setJDay(double jday) {
    /* Calculate nutation parameters (Chapter 22, P. 144) */
    // (Using higher accuracy formula)
    int i;
    double arg;
    dpsi = dep = 0;
    double T = (jday - J2000_0) / 36525; // Julian centuries from J2000.0
    double D  = 297.85036 + T * (445267.111480 - T * (0.0019142 - T / 189474));
    double M  = 357.52772 + T * ( 35999.050340 - T * (0.0001603 + T / 300000));
    double Mp = 134.96298 + T * (477198.867398 + T * (0.0086972 + T /  56250));
    double F  =  93.27191 + T * (483202.017538 - T * (0.0036825 - T / 327270));
    double Om = 125.04452 - T * (  1934.136261 - T * (0.0020708 + T / 450000));
    T /= 10;  // Julian millennia from J2000.0
    for ( i = 0; i < n.length; i += 9 ) {
      arg = (n[i] * D + n[i+1] * M + n[i+2] * Mp + n[i+3] * F + n[i+4] * Om) %
            360 * D2R;
      dpsi += (n[i+5] + n[i+6] * T) * Math.sin(arg);
      dep  += (n[i+7] + n[i+8] * T) * Math.cos(arg);
    }
    dpsi *= D2R / 36000000; // radians
    dep  *= D2R / 36000000; // radians

    // (Simplified formula used previously)
    //double T = (jday - J2000_0) / 365250; // Julian millennia from J2000.0
    //double Omega = (125.04452 - T * 19341.36261) * D2R;
    //double LSun  = (280.4665 + T * 360007.698) * D2R;
    //double LMoon = (218.3165 + T * 4812678.813) * D2R;
    //dpsi = (-17.20 * Math.sin(Omega) -
    //          1.32 * Math.sin(2 * LSun) -
    //          0.23 * Math.sin(2 * LMoon) +
    //          0.21 * Math.sin(2 * Omega)) * D2R / 3600; // radians
    //dep  =   (9.20 * Math.cos(Omega) +
    //          0.57 * Math.cos(2 * LSun) +
    //          0.10 * Math.cos(2 * LMoon) -
    //          0.09 * Math.cos(2 * Omega)) * D2R / 3600; // radians
    //stem.out.println("dpsi= " + (dpsi * 3600 / D2R) + " seconds");
    //stem.out.println("dep = " + (dep * 3600 / D2R) + " seconds");

    /* Calculate the obliquity of the ecliptic */
    // (Using formula on P. 147)
    double U = T / 10; // 10K Julian years from J2000.0
    ep0 = 21.448 - U * (4680.93 + U * (1.55 - U * (1999.25 - U * (51.38 +
          U * (249.67 + U * (39.05 - U * (7.12 + U * (27.87 + U * (5.79 +
          U * 2.45)))))))));
    ep0 = (23 + (26 + ep0 / 60) / 60) * D2R;
    //stem.out.println("ep0 = " + (ep0 / D2R) + " degrees");
    double ep = ep0 + dep; // True obliquity of the ecliptic
    //stem.out.println("ep  = " + (ep / D2R) + " degrees");

    cep = Math.cos(ep);
    sep = Math.sin(ep);
    double ce0 = Math.cos(ep0);
    double se0 = Math.sin(ep0);
    double cdp = Math.cos(dpsi);
    double sdp = Math.sin(dpsi);

    // To nutate equatorial coordinates from J2000:
    // 1) rotate by epsilon0 about X axis (ZxY) (converts to ecliptical)
    // 2) rotate by delta psi about Z axis (XxY)
    // 3) rotate by epsilon (= epsilon0 + delta epsilon) along X axis (YxZ)
    //    (converts back to equatorial)
    // | xnew |   | 1  0    0  | | cdp -sdp 0 | | 1   0   0  | | xold |
    // | ynew | = | 0 cep -sep | | sdp  cdp 0 | | 0  ce0 se0 | | yold |
    // | znew |   | 0 sep  cep | |  0    0  1 | | 0 -se0 ce0 | | zold |

    nut.set(cdp,     -ce0*sdp,              -se0*sdp,
            cep*sdp, ce0*cep*cdp + se0*sep, se0*cep*cdp - ce0*sep,
            sep*sdp, ce0*sep*cdp - se0*cep, se0*sep*cdp + ce0*cep);

    /* Calculate variables for aberration */
    // Use "low accuracy" formulas on P. 163-164.  Accuracy is sufficient
    // for calculating aberration, as aberration amounts to arc seconds,
    // and being a very small fraction of a second off is OK.  Also
    // use formulas on P. 151.
    T *= 10; // T back to Julian centuries from J2000
    double L0 = 280.46646 + T * (36000.76983 + T * 0.0003032);
    M = ((357.52911 + T * (35999.05029 - T * 0.0001537)) % 360) * D2R;
    double C = (1.914602 - T * (0.004817 + T * 0.000014)) * Math.sin(M) +
               (0.019993 - T * 0.000101) * Math.sin(2 * M) +
                0.000289 * Math.sin(3 * M);
    LSun = (L0 + C) % 360; // 2 % 360 = 2, (-2) % 360 = -2
    if ( LSun < 0 ) LSun += 360;
    LSun *= D2R;
    ec = 0.016708634 - T * (0.000042037 + T * 0.0000001267);
    pi = (102.93735 + T * (1.71946 + T * 0.00046)) * D2R;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Retrieves the nutation matrix.
   */
  public Matrix3x3 getNutMatrix() {
    return nut;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Retrieves delta psi in radians.
   */
  public double getdpsi() {
    return dpsi;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Retrieves the true obliquity of the ecliptic (epsilon0 + delta epsilon)
   * in radians.
   */
  public double getep() {
    return (ep0 + dep);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts ecliptical into equatorial (ra/dec).
   *
   * @param lambda Ecliptical (or celestial) longitude, measured from the
   *               vernal equinox along the ecliptic
   * @param beta Ecliptical (or celestial) latitude, positive if north of the
   *             ecliptic, negative is south
   * @param ra On return, right ascension in radians
   * @param dec On return, declination in radians
   *
   * This function adjusts for nutation.
   */
  public void convEclipToEquat(double lambda, double beta,
                               double[] ra, double[] dec) {
    lambda += dpsi;
    double sinlambda = Math.sin(lambda);
    double cosbeta   = Math.cos(beta);
    double sinbeta   = Math.sin(beta);
    // -pi <= Math.atan2 <= pi, no problem if 2nd arg is 0
    ra[0] = Math.atan2(sinlambda * cep -
                       sinbeta * sep / cosbeta, Math.cos(lambda));
    dec[0] = sinbeta * cep + cosbeta * sep * sinlambda;
    if      ( dec[0] >  1 ) dec[0] =  HalfPI;
    else if ( dec[0] < -1 ) dec[0] = -HalfPI;
    else                    dec[0] = Math.asin(dec[0]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts ecliptical into equatorial (ra/dec).
   *
   * @param sc Input: Spherical ecliptical coordinates;
   *           Output: Spherical equatorial coordinates
   *
   * This function adjusts for nutation.
   */
  public void convEclipToEquat(SphereCoords sc) {
    double lambda = sc.getLambda() + dpsi;
    double sinlambda = Math.sin(lambda);
    double cosbeta   = Math.cos(sc.getBeta());
    double sinbeta   = Math.sin(sc.getBeta());
    // -pi <= Math.atan2 <= pi, no problem if 2nd arg is 0
    double ra = Math.atan2(sinlambda * cep -
                           sinbeta * sep / cosbeta, Math.cos(lambda));
    double dec = sinbeta * cep + cosbeta * sep * sinlambda;
    if      ( dec >  1 ) dec =  HalfPI;
    else if ( dec < -1 ) dec = -HalfPI;
    else                 dec = Math.asin(dec);
    sc.set(ra, dec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts equatorial into ecliptical (lambda/beta).
   *
   * @param ra Right ascension in radians
   * @param dec Declination in radians
   * @param lambda On return, ecliptical longitude in radians
   * @param beta On return, ecliptical latitude in radians
   *
   * This function adjusts for nutation.
   */
  public void convEquatToEclip(double ra, double dec,
                               double[] lambda, double[] beta) {
    double sinalpha = Math.sin(ra);
    double sindelta = Math.sin(dec);
    double cosdelta = Math.cos(dec);
    lambda[0] = Math.atan2(sinalpha * cep + sindelta * sep / cosdelta,
                           Math.cos(ra));
    beta[0] = sindelta * cep - cosdelta * sep * sinalpha;
    if      ( beta[0] >  1 ) beta[0] =  HalfPI;
    else if ( beta[0] < -1 ) beta[0] = -HalfPI;
    else                     beta[0] = Math.asin(beta[0]);
    lambda[0] -= dpsi;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts equatorial into ecliptical (lambda/beta).
   *
   * @param sc Input: Spherical equatorial coordinates;
   *           Output: Spherical ecliptical coordinates
   *
   * This function adjusts for nutation.
   */
  public void convEquatToEclip(SphereCoords sc) {
    double sinalpha = Math.sin(sc.getRA());
    double sindelta = Math.sin(sc.getDec());
    double cosdelta = Math.cos(sc.getDec());
    double lambda = Math.atan2(sinalpha * cep + sindelta * sep / cosdelta,
                               Math.cos(sc.getRA()));
    double beta = sindelta * cep - cosdelta * sep * sinalpha;
    if      ( beta >  1 ) beta =  HalfPI;
    else if ( beta < -1 ) beta = -HalfPI;
    else                  beta = Math.asin(beta);
    lambda -= dpsi;
    sc.set(lambda, beta);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adjusts ecliptical (lambda/beta) coordinates for aberration.
   * (Displacement due to Earth's orbital motion.)
   *
   * @param lambda Input: Eclip. longitude in radians before adjust;
   *               Output: Eclip. longitude in radians after adjust
   * @param beta Input: Eclip. latitude in radians before adjust;
   *             Output: Eclip. latitude in radians after adjust
   */
  public void adjustEclipForAberration(double[] lambda, double[] beta) {
    // Formula on P. 151
    //stem.out.println("Sun long = " + (LSun / D2R) + " degrees");
    lambda[0] += k * (ec * Math.cos(pi - lambda[0]) -
                      Math.cos(LSun - lambda[0])) / Math.cos(beta[0]);
    beta[0]   += k * (ec * Math.sin(pi - lambda[0]) -
                      Math.sin(LSun - lambda[0])) * Math.sin(beta[0]);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adjusts ecliptical (lambda/beta) coordinates for aberration.
   * (Displacement due to Earth's orbital motion.)
   *
   * @param sc Input: Spherical ecliptical coordinates before adjust;
   *           Output: Spherical ecliptical coordinates after adjust
   */
  public void adjustEclipForAberration(SphereCoords sc) {
    // Formula on P. 151
    //stem.out.println("Sun long = " + (LSun / D2R) + " degrees");
    double lambda = sc.getLambda();
    double beta   = sc.getBeta();
    sc.set(lambda + k * (ec * Math.cos(pi - lambda) -
                         Math.cos(LSun - lambda)) / Math.cos(beta),
           beta   + k * (ec * Math.sin(pi - lambda) -
                         Math.sin(LSun - lambda)) * Math.sin(beta));
    // For debugging
    //stem.out.println("LSun = " + (LSun * 180 / Math.PI) + " degrees");
    //stem.out.println("e = " + (ep0 + dep));
    //stem.out.println("ec = " + ec);
    //stem.out.println("pi = " + (pi * 180 / Math.PI) + " degrees");
    //stem.out.println("DLambda = " + (( k * (ec * Math.cos(pi - lambda) -
    //Math.cos(LSun - lambda))/Math.cos(beta)) * 648000/Math.PI) + " seconds");
    //stem.out.println("DBeta   = " + ((k * (ec * Math.sin(pi - lambda) -
    //Math.sin(LSun - lambda))*Math.sin(beta)) * 648000/Math.PI) + " seconds");
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adjusts equatorial (ra/dec) coordinates for aberration.
   * (Displacement due to Earth's orbital motion.)
   *
   * @param ra Input: Right ascension in radians before adjust;
   *           Output: Right ascension in radians after adjust
   * @param dec Input: Declination in radians before adjust;
   *            Output: Declination in radians after adjust
   */
  public void adjustEquatForAberration(double[] ra, double[] dec) {
    double[] lambda = new double[1], beta = new double[1];
    convEquatToEclip(ra[0], dec[0], lambda, beta);
    adjustEclipForAberration(lambda, beta);
    convEclipToEquat(lambda[0], beta[0], ra, dec);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adjusts equatorial (ra/dec) coordinates for aberration.
   * (Displacement due to Earth's orbital motion.)
   *
   * @param sc Input: Spherical equatorial coordinates before adjust;
   *           Output: Spherical equatorial coordinates after adjust
   */
  public void adjustEquatForAberration(SphereCoords sc) {
    convEquatToEclip(sc);
    adjustEclipForAberration(sc);
    convEclipToEquat(sc);
  }

  /* For testing */
  //public static void main(String[] args) {
  //  Nutate nutate = new Nutate();

  //  //nutate.setJDay(2446895.5); // P. 148
  //  nutate.setJDay(2448976.5); // P. 225
  //  Matrix3x3 inv = nutate.nut.invert();

  //  // Ending coordinates from P. 227
  //  SphereCoords sc = new SphereCoords(316.17291 * D2R, -18.88801 * D2R);
  //  // Un-nutate them
  //  Matrix3x1 n = sc.rotate(inv);

  //  System.out.println("Pre-nutated coords from ending coords");
  //  double alpha = Math.atan2(n.num[1], n.num[0]) / D2R;
  //  double delta = Math.asin(n.num[2]) / D2R;
  //  if ( alpha < 0 ) alpha += 360;
  //  System.out.println("alpha = " + alpha);
  //  System.out.println("delta = " + delta);
  //  alpha *= D2R;
  //  delta *= D2R;

  //  // Ecliptical coordinates
  //  double ca = Math.cos(alpha);
  //  double sa = Math.sin(alpha);
  //  double cd = Math.cos(delta);
  //  double sd = Math.sin(delta);
  //  double ce = Math.cos(23.439669 * D2R);
  //  double se = Math.sin(23.439669 * D2R);
  //  double lambda = Math.atan2(sa * ce + sd * se / cd, ca) / D2R;
  //  double beta = Math.asin(sd * ce - cd * se * sa) / D2R;
  //  if ( lambda < 0 ) lambda += 360;
  //  System.out.println("lambda = " + lambda);
  //  System.out.println("beta = " + beta);

  //  // Pre-nutated coordinates
  //  sc = new SphereCoords(alpha, delta);
  //  // Nutate them
  //  n = sc.rotate(nutate.nut);

  //  System.out.println("\nNutated coords (as check)");
  //  alpha = Math.atan2(n.num[1], n.num[0]) / D2R;
  //  delta = Math.asin(n.num[2]) / D2R;
  //  if ( alpha < 0 ) alpha += 360;
  //  System.out.println("alpha = " + alpha);
  //  System.out.println("delta = " + delta);
  //}

  static final private int n[] = {
     0,  0,  0,  0,  1, -171996, -1742, 92025,  89,
    -2,  0,  0,  2,  2,  -13187,   -16,  5736, -31,
     0,  0,  0,  2,  2,   -2274,    -2,   977,  -5,
     0,  0,  0,  0,  2,    2062,     2,  -895,   5,
     0,  1,  0,  0,  0,    1426,   -34,    54,  -1,
     0,  0,  1,  0,  0,     712,     1,    -7,   0,
    -2,  1,  0,  2,  2,    -517,    12,   224,  -6,
     0,  0,  0,  2,  1,    -386,    -4,   200,   0,
     0,  0,  1,  2,  2,    -301,     0,   129,  -1,
    -2, -1,  0,  2,  2,     217,    -5,   -95,   3,
    -2,  0,  1,  0,  0,    -158,     0,     0,   0,
    -2,  0,  0,  2,  1,     129,     1,   -70,   0,
     0,  0, -1,  2,  2,     123,     0,   -53,   0,
     2,  0,  0,  0,  0,      63,     0,     0,   0,
     0,  0,  1,  0,  1,      63,     1,   -33,   0,
     2,  0, -1,  2,  2,     -59,     0,    26,   0,
     0,  0, -1,  0,  1,     -58,    -1,    32,   0,
     0,  0,  1,  2,  1,     -51,     0,    27,   0,
    -2,  0,  2,  0,  0,      48,     0,     0,   0,
     0,  0, -2,  2,  1,      46,     0,   -24,   0,
     2,  0,  0,  2,  2,     -38,     0,    16,   0,
     0,  0,  2,  2,  2,     -31,     0,    13,   0,
     0,  0,  2,  0,  0,      29,     0,     0,   0,
    -2,  0,  1,  2,  2,      29,     0,   -12,   0,
     0,  0,  0,  2,  0,      26,     0,     0,   0,
    -2,  0,  0,  2,  0,     -22,     0,     0,   0,
     0,  0, -1,  2,  1,      21,     0,   -10,   0,
     0,  2,  0,  0,  0,      17,    -1,     0,   0,
     2,  0, -1,  0,  1,      16,     0,    -8,   0,
    -2,  2,  0,  2,  2,     -16,     1,     7,   0,
     0,  1,  0,  0,  1,     -15,     0,     9,   0,
    -2,  0,  1,  0,  1,     -13,     0,     7,   0,
     0, -1,  0,  0,  1,     -12,     0,     6,   0,
     0,  0,  2, -2,  0,      11,     0,     0,   0,
     2,  0, -1,  2,  1,     -10,     0,     5,   0,
     2,  0,  1,  2,  2,      -8,     0,     3,   0,
     0,  1,  0,  2,  2,       7,     0,    -3,   0,
    -2,  1,  1,  0,  0,      -7,     0,     0,   0,
     0, -1,  0,  2,  2,      -7,     0,     3,   0,
     2,  0,  0,  2,  1,      -7,     0,     3,   0,
     2,  0,  1,  0,  0,       6,     0,     0,   0,
    -2,  0,  2,  2,  2,       6,     0,    -3,   0,
    -2,  0,  1,  2,  1,       6,     0,    -3,   0,
     2,  0, -2,  0,  1,      -6,     0,     3,   0,
     2,  0,  0,  0,  1,      -6,     0,     3,   0,
     0, -1,  1,  0,  0,       5,     0,     0,   0,
    -2, -1,  0,  2,  1,      -5,     0,     3,   0,
    -2,  0,  0,  0,  1,      -5,     0,     3,   0,
     0,  0,  2,  2,  1,      -5,     0,     3,   0,
    -2,  0,  2,  0,  1,       4,     0,     0,   0,
    -2,  1,  0,  2,  1,       4,     0,     0,   0,
     0,  0,  1, -2,  0,       4,     0,     0,   0,
    -1,  0,  1,  0,  0,      -4,     0,     0,   0,
    -2,  1,  0,  0,  0,      -4,     0,     0,   0,
     1,  0,  0,  0,  0,      -4,     0,     0,   0,
     0,  0,  1,  2,  0,       3,     0,     0,   0,
     0,  0, -2,  2,  2,      -3,     0,     0,   0,
    -1, -1,  1,  0,  0,      -3,     0,     0,   0,
     0,  1,  1,  0,  0,      -3,     0,     0,   0,
     0, -1,  1,  2,  2,      -3,     0,     0,   0,
     2, -1, -1,  2,  2,      -3,     0,     0,   0,
     0,  0,  3,  2,  2,      -3,     0,     0,   0,
     2, -1,  0,  2,  2,      -3,     0,     0,   0
  };
}

