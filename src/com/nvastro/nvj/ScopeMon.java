/*
 * ScopeMon.java  -  Scope Monitor
 * Copyright (C) 2012-2013 Brian Simpson
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

import java.io.*; // File, BufferedReader
import java.awt.*;
import javax.swing.*;


public class ScopeMon implements Runnable {
  private JComponent starwin = null;
  private Preferences prefer = null;
  private SphereCoords coords = new SphereCoords(-1, -1);
  private boolean valid = false;
  private int numerrors = 0;
  private final int maxerrors = 10;
  private boolean desiredLocatSyntaxShown = false;
  private boolean desiredCoordSyntaxShown = false;

  private String scopein = null;

  static final private double TwoPI =  Math.PI * 2;
  static final private double TooPI = (Math.PI * 2) * 1.00000001;
  static final private double HafPI = (Math.PI / 2) * 1.00000001;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param win StarWin component
   */
  public ScopeMon(JComponent win, Preferences pref) {
    starwin = win;
    prefer = pref;

    scopein = Preferences.ScopeIn();
    //stem.out.println("scopein = " + scopein);

    if ( scopein != null && scopein.length() > 0 ) {
      if ( new File(scopein).exists() ) {
        Thread scopein = new Thread(this);
        scopein.setDaemon(true);
        scopein.start();
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Separate thread for checking for input from pipe.
   */
  public void run() {
    BufferedReader in = null;
    String input;

    Thread me = Thread.currentThread();
    me.setPriority(me.getPriority() - 2);

    try {
      // This will block if there is no writer to pipe
      in = new BufferedReader(new FileReader(scopein));
      //stem.out.println("Connected to pipe");

      while ( true ) {
        input = in.readLine(); // Doesn't block; null if nothing there
        if ( input == null ) Thread.sleep(250);
        else {
          //stem.out.println(input);
          parse(input);
        }
      }
    } catch(Exception e) {}
  } // End run

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Parse string from input pipe.
   * (Called by run - runs in its thread)
   */
  private void parse(String in) {
    double ra = -1, dec = -1, ep;

    String IN = in.trim().toUpperCase();

    /* Location mod for Giampiero */
    if ( IN.matches("LONG.*") ) {
      //stem.out.println("LONG found");

      /* Look for something like: "LONG:+12:39 LAT:+41:43 TZ:+1:00 DST:1" */
      if ( IN.matches(
        "LONG: ?[+-]?[\\d:]+ LAT: ?[+-]?[\\d:]+ TZ: ?[+-]?[\\d:]+ DST: ?[01]") )
      {
        double[] lo = new double[1];
        double[] la = new double[1];
        double[] ti = new double[1];

        String lng = IN.substring(5, IN.indexOf("LAT")).trim();
        String lat = IN.substring(IN.indexOf("LAT")+4, IN.indexOf("TZ")).trim();
        String tz  = IN.substring(IN.indexOf("TZ")+3, IN.indexOf("DST")).trim();
        String dst = IN.substring(IN.indexOf("DST")+4);
        //stem.out.println("Long = "  + lng +
        //                 ", Lat = " + lat +
        //                 ", TZ = "  + tz +
        //                 ", Dst = " + dst);

        if ( !Location.convertLong(lng, lo) )
          logError("Incorrect longitude: " + lng, null);
        else if ( !Location.convertLat(lat, la) )
          logError("Incorrect latitude: " + lat, null);
        else if ( !Location.convertTZ(tz, ti) )
          logError("Incorrect timezone: " + tz, null);
        else {
          prefer.setLocation(new Location(null, lo[0], la[0], ti[0]), dst);
          DateTimeDlg.updateDlg();  // Update DateTimeDlg with latest dst info
          // restartpaint flags an update of Preferences, then calls repaint
          if ( starwin != null ) ((StarWin)starwin).restartpaint();
          //stem.out.println("New location");
        }

      }
      else {
        logError("Invalid syntax: " + in, desiredLocatSyntaxShown ? null :
           " (Require something like: LONG:+12:39 LAT:+41:43 TZ:+1:00 DST:1)");
        desiredLocatSyntaxShown = true;
      }

      return; // Always return so that an invalid location
              // doesn't turn off scope marker (below)
    }

    /* Look for something like: "RA:2.65452 Dec:0.20887 Epoch:2000.0" */
    if ( IN.matches("RA: ?-?[\\d\\.]+ DEC: ?-?[\\d\\.]+ EPOCH: ?[\\d\\.]+") ) {
      String[] strs = in.trim().split("[: ]+");
      ra  = Double.parseDouble(strs[1]);
      dec = Double.parseDouble(strs[3]);
      ep  = Double.parseDouble(strs[5]);
      //stem.out.println("RA = "      + ra +
      //                 ", Dec = "   + dec +
      //                 ", Epoch = " + ep);

      if ( ra < 0 ) ra += TwoPI;            // Fix
                                               // this
      if ( ra < 0 || ra > TooPI ) {               // some
        logError("RA out of range (0 - 2PI)", null); // time
        ra = dec = -1;
      }
      else if ( dec < -HafPI || dec > HafPI ) {
        logError("DEC out of range (-PI/2 - PI/2)", null);
        ra = dec = -1;
      }
      else if ( ep != 2000 ) {
        logError("EPOCH not 2000", null);
        ra = dec = -1;
      }
    }
    else {
      logError("Invalid syntax: " + in, desiredCoordSyntaxShown ? null :
         " (Require something like: RA:2.65452 Dec:0.20887 Epoch:2000.0)");
      desiredCoordSyntaxShown = true;
    }

    set(ra, dec);
    //stem.out.println("RA = " + ra + ", Dec = " + dec);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Logs input errors from pipe.  (Up to limit)
   */
  private void logError(String msg1, String msg2) {
    if ( ++numerrors <= maxerrors ) {
      ErrLogger.logError(msg1);
      if ( msg2 != null ) ErrLogger.logError(msg2);
    }

    if ( numerrors == maxerrors )
      ErrLogger.logError("Max pipe input errors reached. " +
                         "No more errors will be reported");
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets coordinates.
   * (Called by parse - runs in "run" thread)
   */
  private synchronized void set(double ra, double dec) {
    if ( coords.getRA()  != ra || coords.getDec() != dec ) {
      coords.set(ra, dec);
      valid = (ra >= 0) ? true : false;
      // repaint() can be called outside of event dispatch thread
      if ( starwin != null ) starwin.repaint();
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets coordinates.
   * (Called by draw - runs in event dispatch thread)
   */
  private synchronized SphereCoords get() {
    return coords;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns validity of coordinates.
   * (Called when in event dispatch thread)
   */
  public synchronized boolean isValid() {
    return valid;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw scope marker.
   * (Runs in event dispatch thread)
   */
  public void draw(MapParms mp, Graphics g) {
    float[] x = new float[1];
    float[] y = new float[1];
    int xd, yd;

    SphereCoords c = get();
    if ( c.getRA() >= 0 ) {
      if ( mp.rd2xyhit(c, x, y) == 1 ) {
        xd = (int)(x[0] + 0.5f);
        yd = (int)(y[0] + 0.5f);
        g.drawOval(xd - 15, yd - 15, 30, 30);
        g.drawLine(xd - 15, yd, xd - 11, yd);
        g.drawLine(xd + 15, yd, xd + 11, yd);
        g.drawLine(xd, yd + 15, xd, yd + 11);
        g.drawLine(xd, yd - 15, xd, yd - 11);
      }
    }
  }
}

