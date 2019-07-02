/*
 * StarWin.java  -  Star window
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.JComponent;
import javax.swing.Timer;


/*------------------------------------------------------------------------------
Good info in
 http://java.sun.com/products/jfc/tsc/articles/painting/
It discusses transparency, and what paint functions
to override, ...
Says that JRootPane's double-buffering is set to true, which
was confirmed (by using getRootPane().isDoubleBuffered()).
This implies that all descendants will be buffered in same off-screen
image before being shown.
------------------------------------------------------------------------------*/


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Star window.  Display area for stars, constellations, planets, ...
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class StarWin extends JComponent implements Printable, Runnable,
                             MouseListener, MouseMotionListener {
  Nvj frame;
  StarDB stardb;
  StarNameDB starnamedb;
  DeepSkyDB deepskydb;
  NearSkyDB nearskydb;
  ConstLines constlines;
  ConstBounds constbounds;
  MilkyWay milkyway;
  Horizon horizon;
  Preferences prefer;
  MapParms mp,         // mp is used for painting (including markers),
           idmp,       // idmp is frozen snapshot of mp for id purposes,
           mprint;     // mprint is frozen snapshot of mp for printing
  private double xpoffset, ypoffset;  // Printer offsets
  private SkyObject idObject = null,
                    fObject  = null,
                    fObject2 = null;
  private SphereCoords idsc = new SphereCoords(),       // J2000 ID coordinates
                       idscPN_old = new SphereCoords(); // Previous idsc coords
                                           // that are precessed/nutated
  private SphereCoords dragsc = new SphereCoords(),  // Dragged spher. coords
                       savesc = new SphereCoords();  // Save in case Esc pressed
  private boolean prevID = false;          // True if previous ID done
  private Timer timer;                     // New image update timer
  private Timer timer2;                    // Updates scrn during image build
  private Timer timer3;                    // 1 second tmr for found objects
  private int   timer3cntr = 0;            // Counter for timer3 events
  private double dftPelsPerRadian;         // Default pels (pixels) per radian
  private boolean mouserect = false;       // Captures select/rect btn down
  private boolean mousedrag = false;       // Captures select/drag btn down
  private int startx, starty, lastx, lasty;// For mousing around
  private Dimension scrn;                  // Stores screen dimensions in pels
  private BufferedImage bufImage;          // Off-screen image of entire screen
  private Graphics2D bufGraph;             // 2D graphics for bufImage
  private boolean timerRinging = false;    // Time to update if true
  private boolean preferUpdated = true;    // True implies repaint window
                                           // because Preferences has changed
  private boolean silent = false;          // Silent image build
  private Thread imageMaker = null;        // 2nd (image building) thread
  private int startImageMaker = 0;         // Count of 2nd thread starts
  private ScopeMon scopemon;               // Scope monitor (has thread)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param f Main window class
   * @param prefer User preferences
   */
  public StarWin(Nvj f, Preferences prefer) {
    frame = f;
    this.prefer = prefer;
    mp = new MapParms(prefer);  // The same Preferences object used in the
             // user interface dialogs will also be used by the 2nd thread
             // for painting onto the off-screen buffer.  See MapParms.java
             // for discussion.

    setBackground(prefer.colorBackGnd());
    setOpaque(true);   // Informs Swing's paint system that painting of
    // underlying components is not necessary and that this component
    // will paint all pixels within its rectangle.

    stardb = new StarDB();
    starnamedb = new StarNameDB();
    deepskydb = new DeepSkyDB();
    nearskydb = new NearSkyDB();
    constlines = new ConstLines();
    constbounds = new ConstBounds();
    milkyway = new MilkyWay();
    horizon = new Horizon();

    /* Get some screen data */
    scrn = Nvj.dimScrn;     // Dimensions of screen
    // Let's make dft size to be 95% of width
    //  (but no more than 130% of height)
    dftPelsPerRadian = Math.min((0.95 * scrn.width),
                                (1.30 * scrn.height)) / Math.PI;

    /* Create an off-screen buffer */
    bufImage = LocalGraphics.getBufferedImage(scrn.width, scrn.height);
    bufGraph = bufImage.createGraphics();

    /* Create a timer for signaling screen updates per the user's
       preferred update rate */
    ActionListener tmrlistener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        StarWin.this.timerRinging = true;
        StarWin.this.repaint();
      }
    };
    /* Start the timer */
    timer = new Timer(prefer.getUpdatePeriod() * 1000, tmrlistener);
    timer.setInitialDelay(prefer.getUpdatePeriod() * 1000);
    timer.setCoalesce(true);
    timer.start();

    /* Create another timer listener to signal updating of screen
       as off-screen image is being built */
    ActionListener tmrlistener2 = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        /* Don't signal a repaint of the window (image xfer from bufGraph)
           while a new image is being built unless the new image is the result
           of user action (then it's OK if a partially built image is shown).
           But if a new image is being built because time has advanced then
           don't paint partial images to the screen but rather wait until
           image is complete.  (I.e. "silent" build) */
        if ( !StarWin.this.silent )  // If not "silent"
          StarWin.this.repaint();    // then paint partial image
      }
    };
    /* Setup the second timer */
    timer2 = new Timer(500, tmrlistener2);
    timer2.setInitialDelay(100);
    timer2.setCoalesce(true);

    /* Create third timer for found objects */
     ActionListener tmrlistener3 = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        StarWin.this.clearFindMarker();
      }
    };
    /* Setup the third timer (Flashes on and off for found objects) */
    timer3 = new Timer(1000, tmrlistener3);
    timer3.setCoalesce(true);
    timer3.setRepeats(true);

    /* Declare "this" to be a MouseListener and a MouseMotionListener */
    addMouseListener(this);
    addMouseMotionListener(this);

    /* Set up scope monitor (prefer added for Giampiero's request
       to set location) */
    scopemon = new ScopeMon(this, prefer);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Part of MouseListener interface.  Does nothing.
   */
  public void mouseClicked(MouseEvent e) {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Part of MouseListener interface.  Does nothing.
   */
  public void mouseEntered(MouseEvent e) {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Part of MouseListener interface.  Does nothing.
   */
  public void mouseExited(MouseEvent e) {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Handles mouse pressed for MouseListener interface.
   */
  public void mousePressed(MouseEvent e) {
    /* Popup button */
    if ( (e.getModifiers() & prefer.popupBtn()) != 0 )         // Popup button
      mousePopup(e);

    /* Select button pressed */
    else if ( (e.getModifiers() & prefer.selectBtn()) != 0 ) { // Other button
      if ( e.isShiftDown() || e.isControlDown() ) {
        mouserect = true;
        startx = lastx = e.getX(); starty = lasty = e.getY();
      }
      else {
        mousedrag = true;
        mp.xy2aa(e.getX(), e.getY(), dragsc);
        savesc.set(prefer.getAz(), prefer.getAlt());
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Handles mouse released for MouseListener interface.
   */
  public void mouseReleased(MouseEvent e) {
    /* Popup button */
    if ( (e.getModifiers() & prefer.popupBtn()) != 0 )         // Popup button
      mousePopup(e);

    /* Select button released */
    else if ( (e.getModifiers() & prefer.selectBtn()) != 0 ) { // Other button
      if ( mouserect ) {
        mouserect = false;
        lastx = e.getX(); lasty = e.getY();
        int w = Math.abs(startx - lastx);
        int h = Math.abs(starty - lasty);
        double ratio = 1.0;
        if ( Math.max(w, h) > 7 )  // If longest side > 7 pels
          ratio = Math.sqrt(mp.getWidth() * mp.getHeight() / ((double)(w * h)));
        SphereCoords aa = new SphereCoords();
        mp.xy2aa((lastx + startx) / 2, (lasty + starty) / 2, aa);
        prefer.setAzAltZoom(aa, ratio);
        restartpaint();  // (setAzAltZoom doesn't always restartpaint)
      }
      else if ( mousedrag ) {
        mousedrag = false;
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Handles mouse drag events for MouseMotionListener interface.
   */
  public void mouseDragged(MouseEvent e) {
    if ( mouserect ) {
      Graphics g = getGraphics();
      g.setXORMode(prefer.colorBackGnd());
      g.setColor(prefer.colorZoomBox());
      drawRectStart2Last(g);            // Erase previous rectangle
      lastx = e.getX(); lasty = e.getY();
      drawRectStart2Last(g);            // Draw new rectangle
      g.dispose();
    }
    else if ( mousedrag ) {
      SphereCoords newsc = new SphereCoords();
      mp.xy2aa(e.getX(), e.getY(), newsc);

      double diffaz  = dragsc.getAz()  - newsc.getAz();
      double diffalt = dragsc.getAlt() - newsc.getAlt();
      double newaz  = (prefer.getAz()  + diffaz)  / MapParms.Deg2Rad;
      double newalt = (prefer.getAlt() + diffalt) / MapParms.Deg2Rad;

      if ( newalt > 90 ) newalt = 90;
      else if ( newalt < -90 ) newalt = -90;
      while ( newaz > 360 ) newaz -= 360;
      while ( newaz <  0  ) newaz += 360;

      prefer.setAzAltZoom(newaz, newalt, 1.0);
      restartpaint();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Part of MouseMotionListener interface.  Does nothing.
   */
  public void mouseMoved(MouseEvent e) {}

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by mousePressed and mouseReleased.
   */
  private void mousePopup(MouseEvent e) {
    if ( e.getID() == prefer.popupEvent() ) {                // Press/Release
      /* Save: idmp  - a "snapshot" of mp,
         and:  idsc  - the ra/dec of the position clicked,
         to use later if the identify function is requested. */
      idmp = (MapParms) mp.clone();
      boolean identify = idmp.xy2rd(e.getX(), e.getY(), idsc); // J2000 coords
      // If identify = true, point is within 90 degrees of window center
      Point window = getLocationOnScreen();  // Screen coordinates of window
      frame.showPopup(window.x + e.getX(), window.y + e.getY(), identify);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Cancels zoom rectangle.
   */
  public void escapePressed() {
    if ( mouserect ) {
      mouserect = false;
      repaint();
    }
    else if ( mousedrag ) {
      mousedrag = false;
      prefer.setAzAltZoom(savesc, 1.0);
      restartpaint();
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws a rectangle between (startx, starty) and (lastx, lasty)
   * if endpoints are different.  (Ensures that rectangle is always
   * drawn even if "width" and "height" are negative. drawRect() and
   * draw(Rectangle2D.Float) won't handle negative widths & heights.)
   */
  private void drawRectStart2Last(Graphics g) {
    if ( startx != lastx || starty != lasty ) {
      g.drawRect(Math.min(startx,  lastx), Math.min(starty,  lasty),
                 Math.abs(startx - lastx), Math.abs(starty - lasty));
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns a string identifying an object.
   *
   * @param pt The x,y position (screen coordinates) of the object as a
   *           return value.  (I.e. the Point passed in is updated.)
   *           If object is no longer visible pt will be given negative
   *           coordinates.
   */
  public String identifyObject(Point pt) {
    double [] sepStar = new double[1],
              sepNS = new double[1],
              sepDS   = new double[1];
    StringBuffer strStar = new StringBuffer(),
                 strNS = new StringBuffer(),
                 strDS   = new StringBuffer(),
                 buf;

    /* If old marker showing, clear it */
    if ( idObject != null ) clearIDMarker();

    /* Find closest star, NearSky object, and DeepSky object
       Note:  idmp (a "snapshot" of mp) and idsc (J2000 ra/dec coordinates
       of position that was clicked) were set previously. */
    int st = stardb.findNearestStar(idmp, idsc, sepStar, strStar);
    int ns = nearskydb.findNearestNS(idmp, idsc, sepNS, strNS);
    int ds = deepskydb.findNearestDS(idmp, idsc, sepDS, strDS);

    /* OK, who's closest... */
    if ( ns >= 0 ) {
      idObject = new SkyObject(SkyObject.NS, ns);
      buf = strNS;
    }
    else if ( ds >= 0 && ((st < 0) || (sepDS[0] < sepStar[0])) ) {
      idObject = new SkyObject(SkyObject.DS, ds);
      buf = strDS;
    }
    else if ( st >= 0 ) {
      idObject = new SkyObject(SkyObject.STAR, st);
      buf = strStar;
    }
    else {  // "No object"
      idObject = new SkyObject(idsc);
      buf = new StringBuffer(IdentifyDlg.NOOBJ + "\n");
    }

    /* Change clicked coordinates to object's coordinates: idsc is set
       to J2000 coordinates, idscPN is precessed, nutated, ... */
    SphereCoords idscPN = idObject.getAppLocation(idmp, idsc);

    /* Display coordinates */
    buf.append(IdentifyDlg.RA);
    buf.append(idscPN.tellRAHrMnScT()).append("\n");
    buf.append(IdentifyDlg.DEC);
    buf.append(idscPN.tellDecDgMnSc()).append("\n");

    /* Calculate new Az/Alt */
    SphereCoords aa = new SphereCoords();
    idmp.rd2aa(idscPN, aa); // App ra/dec to az/alt
    buf.append(IdentifyDlg.AZ);
    buf.append(aa.tellAzDgMn()).append("\n");
    buf.append(IdentifyDlg.ALT);
    buf.append(aa.tellAltDgMn()).append("\n");

    /* Display time */
    buf.append(IdentifyDlg.TIME);
    buf.append(idmp.lst.tellLocDateTime(!prefer.is24Hr())).append("\n");

    /* Display J2000 coordinates */
    buf.append(IdentifyDlg.J2000RA);
    buf.append(idsc.tellRAHrMnScT()).append("\n");
    buf.append(IdentifyDlg.J2000DEC);
    buf.append(idsc.tellDecDgMnSc());

    /* Display distance from previous click */
    // Use idscPN rather than idsc so that precessed/nutated distances
    // can be measured when the date/time changes
    if ( prevID ) {
      buf.append("\n");
      buf.append(IdentifyDlg.SEP);
      buf.append(idscPN.tellDistanceFrom(idscPN_old));
    }
    else prevID = true;
    idscPN_old.set(idscPN);

    /* Draw id marker on screen */
    Graphics g = getGraphics();
    g.setXORMode(prefer.colorBackGnd());
    g.setColor(prefer.colorZoomBox());
    drawIDMarker(g);
    g.dispose();

    // Make sure there is no trailing "\n" (which leaves a blank line in dlg).

    /* Update pt to latest screen coordinates */
    float[] x = new float[1], y = new float[1];
    if ( mp.rd2xyhit(idsc, x, y) != 1 ) {     // If not in window
      pt.x = pt.y = -1;
    }
    else {
      Point window = getLocationOnScreen();  // Screen coordinates of window
      // In the following rounding by truncation, x[0] & y[0] >= 0
      pt.x = (int)(x[0] + 0.5f) + window.x;
      pt.y = (int)(y[0] + 0.5f) + window.y;
    }

    return buf.toString();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Clears the id marker.  Called when "Identify object" window goes away.
   */
  public void clearIDMarker() {
    Graphics g = getGraphics();
    g.setXORMode(prefer.colorBackGnd());
    g.setColor(prefer.colorZoomBox());
    drawIDMarker(g);
    g.dispose();

    idObject = null;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Clear find marker.
   */
  private void clearFindMarker() {
    if ( timer3cntr++ == 0 ) fObject2 = fObject;   // Store fObject
    if ( timer3cntr % 2 != 0 ) fObject = null;     // Turn off find ptr
    else                       fObject = fObject2; // Turn on find ptr
    if ( timer3cntr >= 9 ) {
      fObject = fObject2 = null;
      timer3cntr = 0;
      timer3.stop();
    }

    // For whatever reason, does not XOR the diagonal line cleanly.
    //Graphics g = getGraphics();
    //g.setXORMode(prefer.colorBackGnd());
    //g.setColor(prefer.colorZoomBox());
    //drawFindMarker(g);
    //g.dispose();
    // So just do a repaint.
    repaint();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw ID marker.
   */
  private void drawIDMarker(Graphics g) {
    float[] x = new float[1];
    float[] y = new float[1];
    int xd, yd;

    if ( idObject != null ) {
      if ( mp.rd2xyhit(idObject.getJ2000Location(mp), x, y)
           == 1 ) {
        xd = (int)(x[0] + 0.5f);
        yd = (int)(y[0] + 0.5f);
        g.drawLine(xd - 15, yd, xd - 5, yd);
        g.drawLine(xd + 5, yd, xd + 15, yd);
        g.drawLine(xd, yd - 15, xd, yd - 5);
        g.drawLine(xd, yd + 5, xd, yd + 15);
      }
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw find marker.
   */
  private void drawFindMarker(Graphics g) {
    float[] x = new float[1];
    float[] y = new float[1];
    int xd, yd;

    if ( fObject != null && fObject.markit() ) {
      if ( mp.rd2xyhit(fObject.getJ2000Location(mp), x, y)
           == 1 ) {
        xd = (int)(x[0] + 0.5f);
        yd = (int)(y[0] + 0.5f);
        g.drawLine(xd - 15, yd + 15, xd - 2, yd + 2);
        g.drawLine(xd - 9, yd + 1, xd - 1, yd + 1);
        g.drawLine(xd - 1, yd + 1, xd - 1, yd + 9);
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called to locate object on screen.
   * Called from FindDlg (goes through Nvj).
   *
   * @param skyobj Object in sky
   */
  public void gotoObject(SkyObject skyobj) {
    SphereCoords aa = new SphereCoords();

    MapParms tmp = new MapParms(prefer);
    tmp.update(getSize(), dftPelsPerRadian);  // Updates rotation, ...
    if ( prefer.modeRADec ) {
      aa.set(skyobj.getAppLocation(tmp, null));
      aa.set(Rotation.TwoPI - aa.getRA(), aa.getDec());
      }
    else
      tmp.rd2aa(skyobj.getAppLocation(tmp, null), aa);

    if ( timer3.isRunning() ) timer3.stop();

    prefer.setAzAltZoom(aa, 1.0);
    restartpaint();
    fObject = skyobj;
    timer3cntr = 0;
    timer3.start();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Switches mode from RA-Dec to Alt-Az.
   */
  public void gotoAltAz() {
    SphereCoords rd = new SphereCoords(mp.prefer.getRARad(),
                                       mp.prefer.getDecRad());
    SphereCoords aa = new SphereCoords();
    mp.rd2aa(rd, aa);
    //stem.out.println("RA = " + rd.tellRAHrMnT() +
    //                 ", Dec = " + rd.tellDecDgMn());
    //stem.out.println("Az = " + aa.tellAzDgMn() +
    //                 ", Alt = " + aa.tellAltDgMn());
    prefer.modeRADec = false;
    prefer.setAzAltZoom(aa, 1.0);
    prefer.clearPrevious();
    restartpaint();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Switches mode from Alt-Az to RA-Dec.
   */
  public void gotoRADec() {
    SphereCoords aa = new SphereCoords(prefer.getAz(), prefer.getAlt());
    SphereCoords rd = new SphereCoords();
    mp.aa2rd(aa, rd);
    //stem.out.println("Az = " + aa.tellAzDgMn() +
    //                 ", Alt = " + aa.tellAltDgMn());
    //stem.out.println("RA = " + rd.tellRAHrMnT() +
    //                 ", Dec = " + rd.tellDecDgMn());
    // Before setting the azimuth scrollbar, keep in mind it ranges from 0
    // (left, corresponding to 24hrs) to 360 (right, corresponding to 0hrs);
    // We need to subtract ra from 2pi (360) before setting the az scrollbar
    rd.set(MapParms.TwoPI - rd.getRA(), rd.getDec());
    prefer.modeRADec = true;
    prefer.setAzAltZoom(rd, 1.0);
    prefer.clearPrevious();
    restartpaint();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Freezes time.
   */
   public void freezeTime() {
     timer.stop();
     prefer.lst.stop();

     /* Update the window to show the latest time */
     restartpaint();
   }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Thaws time.
   */
   public void thawTime() {
     prefer.lst.start();
     timer.setDelay(prefer.getUpdatePeriod() * 1000);
     timer.setInitialDelay(prefer.getUpdatePeriod() * 1000);
     timer.start();
   }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calls repaint() after setting flag noting preferences were updated.
   */
  public void restartpaint() {
    preferUpdated = true;
    repaint();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Paint function.
   *
   * @param g Graphics context to use for painting
   */
  public void paint(Graphics g) {
    /* Note:  Because this component is specified as opaque, always
       draw when paint is called.  The UI thread appears to call paint
       many more times than is necessary, perhaps in the drawing of
       other frame components. */

    /* Determine if we need to restart the image build process
       1) Has orientation or zoom-level changed
       2) Has preference options changed
       3) Has window size/shape changed
       4) Has timer gone off
    */
    Dimension size = getSize();
    boolean rebuild = preferUpdated || // Handles orientation, zoom, & prefer
                      (size.width != mp.getWidth()) ||
                      (size.height != mp.getHeight());
    if ( timerRinging && !rebuild ) {
      rebuild = true;
      silent = true;
    }
    else
      silent = false;

    /* Paint the window (xfer image from bufImage) */
    g.drawImage(bufImage, 0, 0, null);
    drawMarkers(g);

    if ( rebuild ) {
      /* if ( preferUpdated ) */ preferUpdated = false;
      if ( timer.isRunning() ) {
        if ( timer.getDelay() == prefer.getUpdatePeriod() * 1000 )
          timer.restart();
        else {
          //stem.out.println("Period = " + prefer.getUpdatePeriod());
          timer.stop();
          timer.setDelay(prefer.getUpdatePeriod() * 1000);
          timer.setInitialDelay(prefer.getUpdatePeriod() * 1000);
          timer.start();
        }
      }
      timerRinging = false;

      /* Set up MapParms */
      mp.cancelDrawing();
      synchronized ( mp ) {
        mp.update(size, dftPelsPerRadian);
        mp.notify();
      }

      /* Update status line */
      frame.setStatusLine(prefer.tellView() + ", " +
                          TextBndl.getString("Pgm.St.LocTime") + " " +
                          mp.lst.tellLocDateTime(!prefer.is24Hr()));

      /* Notify Solar System window, if it's up, that it may need to
         update it's contents.  Time may have been updated via a timer,
         or done through the GUI. */
      SSWin.updateTime();
    }

    /* Start/restart 2nd thread */
    if ( imageMaker == null || !imageMaker.isAlive() ) {
      if ( imageMaker != null )
        // A handy way of testing this is to alter MilkyWay such
        // that the GeneralPath is closed without any points
        ErrLogger.logError(TextBndl.getString("Pgm.T2Fail"));

      if ( ++startImageMaker <= 6 ) { // 1 start + 5 restarts
        imageMaker = new Thread(this);
        imageMaker.setDaemon(true);
        imageMaker.start();
      }
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the XOR stuff (markers, drag rectangle).
   */
  private void drawMarkers(Graphics g) {
    /* Note:  Could not get XOR mode to work correctly by writing on
       bufImage and xfering to window.  Results were incorrect and
       inconsistent.  Only when a menu dropped down over the XOR'd
       area did it show correctly.  Thus do XOR'ing only on window... */
    if ( (fObject != null && fObject.markit()) || idObject != null ||
         scopemon.isValid() || mouserect ) {
      g.setXORMode(prefer.colorBackGnd());
      g.setColor(prefer.colorZoomBox());

      drawIDMarker(g);
      drawFindMarker(g);
      scopemon.draw(mp, g);

      /* If we are dragging a mouse, better show rectangle */
      if ( mouserect ) {
        drawRectStart2Last(g);
      }
      g.setPaintMode();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * 2nd thread does construction of image.
   */
  public void run() {
    Thread me = Thread.currentThread();
    me.setPriority(me.getPriority() - 2);

    mp.g = bufGraph;

    while ( true ) {
      synchronized ( mp ) {

        mp.g.setColor(mp.prefer.colorBackGnd());
        mp.g.fillRect(0, 0, scrn.width, scrn.height);

        /* Set up clip shapes.  (By the way, for whatever reason,
           performance seems faster with a clip) */
        mp.clip1 = new Rectangle(0, 0, mp.getWidth(), mp.getHeight());
        mp.g.setClip(mp.clip1);
        double diameter = Math.PI * mp.pelsPerRadian;  // 180 degrees
        double radius = diameter / 2;                  //  90 degrees
        double diagonal = Math.sqrt(mp.getWidth() * mp.getWidth() +
                                    mp.getHeight() * mp.getHeight());
        mp.clip2 = ( diameter > diagonal ) ? null :
                   new Ellipse2D.Double(mp.getMidX() - radius,
                                        mp.getMidY() - radius,
                                        diameter, diameter);

        /* Some rendering hints */
        // Couldn't get KEY_TEXT_ANTIALIASING to work, at least with
        // the version of Java I was using when this code was written...
        // (I did try an experiment where only KEY_TEXT_ANTIALIASING
        // was used (KEY_ANTIALIASING untouched), but it didn't work)
        // (maybe I also needed to do FRACTIONALMETRICS?)
        // mp.g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        //                       mp.prefer.antialiasing ?
        //                       RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
        //                       RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        // Turn off antialiasing for now, turn it on later as needed
        mp.g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);

        timer2.start();

        milkyway.draw(mp);
        horizon.draw(mp);
        CGrid.draw(mp);
        AGrid.draw(mp);
        Ecliptic.draw(mp);
        constlines.draw(mp);
        constbounds.draw(mp);
        Constellation.draw(mp);
        deepskydb.draw(mp);
        stardb.draw(mp);
        starnamedb.draw(mp);
        nearskydb.draw(mp);

        timer2.stop();
        repaint();

        /* Put thread to sleep until next notify */
        try { mp.wait(); } catch ( Exception e ) {}
      } // End synchronized
    } // End while ( true )
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Prepare for printing:  Since print will be called multiple times,
   * freeze critical parameters.  Do not call this function for a subsequent
   * print until the previous print has finished processing.
   *
   * @param format Page format
   */
  public void preparePrint(PageFormat format) {
    // format.getWidth() & format.getHeight() return the paper size.
    // format.getImageableWidth() & format.getImageableHeight() return
    // the size of the imageable area, and format.getImageableX() &
    // format.getImageableY() return the x,y offset of the imageable area
    // relative to the paper where 0,0 is the paper's upper left corner.
    // All return values are in points, and there are 72 points per inch.
    // The imageable area has no direct correspondence to the area that
    // the printer can render.  It's OK to draw outside of the imageable
    // area (tested on WinNT & Linux).  The imageable area can be set
    // under WinNT with the Page Setup dialog.  (Not on Linux, yet.)
    // There appears to be no way of determining the maximum area that
    // the printer can render into.  For now I will ignore the imageable
    // area specified in format, and will write to a square area whose
    // side is 85% of the shorter of the paper width or height.  This
    // seems to match best with the OS/2 version.
    //stem.out.println("W = " +format.getWidth()+ ", H = " +format.getHeight());
    //stem.out.println("IW = " + format.getImageableWidth() + ", IH = " +
    //                 format.getImageableHeight());
    //stem.out.println(format.getImageableX() + ", " + format.getImageableY());
    //stem.out.println("Or = " + format.getOrientation());
    // Someday may want to write my own Page Setup dialog, where I can
    // directly control how the user specifies the margins.  Would need
    // to include a method of specifying the paper size (as is done
    // in the existing WinNT & Linux dialogs).  Code for this is likely
    // to be found in the 5 part article by Jean-Pierre Dube at:
    // (the e in Dube has accent mark (\xe9) - Javadoc chokes on it)
    // http://www.javaworld.com/javaworld/jw-10-2000/jw-1020-print.html
    // The article says that JDK 1.4 will also allow you to set the
    // paper source.

    double w = format.getWidth();
    double h = format.getHeight();
    double side = Math.min(w, h) * 0.85;
    if ( h > w ) {    // Portrait (getOrientation() returns PORTRAIT)
      ypoffset = xpoffset = (w - side) / 2;
    }
    else {
      xpoffset = (w - side) / 2;
      ypoffset = (h - side) / 2;
    }
    int s = (int)side;

    /* Determine (from screen) how many radians to show (within limits) */
    double rad = Math.max(mp.getWidth(), mp.getHeight()) / mp.pelsPerRadian;
    // pi/20 (9 degrees) <= rad <= 1.1 * pi (198 degrees)
    // (198 degrees hopefully allows room for Horizon markers)
    rad = Math.max(Math.PI / 20, Math.min(rad, Math.PI * 1.1));

    /* Set up mprint */
    mprint = (MapParms) mp.clone();
    mprint.lst.stop();
    mprint.printing = true;
    mprint.setSize(new Dimension(s, s));
    mprint.pelsPerRadian = s / rad;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This function might help pull out of a cancelled print job.
   * Should be called after PrinterJob.cancel() so that a half-baked
   * job doesn't print.  (Might happen if called before PrinterJob.cancel()
   * and PrinterJob.cancel() was called a bit late.)
   */
  public void cancelPrint() {
    mprint.cancelDrawing();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Print function.  Is called multiple times per print.
   *
   * @param g Graphics context to use for painting
   * @param format Page format
   * @param pagenum Page number
   */
  public int print(Graphics g, PageFormat format, int pagenum) {
    int txth, boxh, boxul, boxur, boxll, boxlr, offx, offy, padx;

    if ( pagenum > 0 ) return Printable.NO_SUCH_PAGE;

    final Graphics2D g2 = (Graphics2D)g;
    mprint.g = g2;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    // The following command screws up measuring text for the 4 text boxes,
    // but at one time it seemed to help.  Comment it for now...
    //.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
    //                  RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    // The following two are defaults
    //.setComposite(AlphaComposite.SrcOver);
    //.setPaintMode();

    // Note:  Initial clip region is a rectangle formed from
    // getImageableX, getImageableY, getImageableWidth, getImageableHeight.
    // (xoffset, yoffset, width, height of imageable area,
    // where x,y = 0,0 refers to corner of paper.)
    // A translate operation will move the origin, and so the
    // clip area is "moved" also (relative to the new coordinate
    // system), though it will be in the same place relative
    // to the paper (and relative to anything drawn before).
    // A scale operation of 0.5 will reduce the size of the printed
    // objects (rectangles, fonts, ...) by half, and the clip area
    // will automatically be adjusted so that getImageableX,Y,
    // getImageableWidth,Height are doubled, leaving the clip area
    // relative to the paper the same, and the origin doesn't move.
    // The above information is for reference purposes since the
    // imageable area that corresponds with the Graphics2D object's
    // initial clip area is ignored; I set my own.

    /* Move origin so that 0,0 is at upper left corner of the image rendered
       (Not necessarily the same as Java's imageable area). */
    g2.translate(xpoffset, ypoffset);

    /* Set up a complex clipping area */
    // According to http://java.sun.com/100percent/cookbook.pdf
    // use "Sans-serif" when you want "Helvetica"
    // (I suspect it meant to say "SansSerif" as it is
    // in the Font.java source file)
    Font boxfont = new Font("SansSerif", Font.BOLD, 12);
    g2.setFont(boxfont);
    FontMetrics fm = g2.getFontMetrics(boxfont);
    txth = fm.getAscent() + fm.getDescent();
    boxh = (txth * 5) / 4;     // Rectangle height
    padx = txth / 2;           // Padding of rectangle width
    offx = (boxh * 3) / 16;    // X offset of text within rectangle
    offy = (boxh * 3) / 4;     // Y offset of text within rectangle
    boxul = fm.stringWidth(Nvj.PgmName) + padx;
    boxur = fm.stringWidth(mprint.lst.tellLocDateTime(!prefer.is24Hr())) +
            padx;
    boxll = fm.stringWidth(mprint.prefer.tellLocation()) + padx;
    boxlr = fm.stringWidth(mprint.prefer.tellView()) + padx;
    /* Make box size adjustments if called for */
    int pct = Preferences.boxAdjustPct + 100;
    if ( pct != 100 ) {
      boxul = (boxul * pct) / 100;
      boxur = (boxur * pct) / 100;
      boxll = (boxll * pct) / 100;
      boxlr = (boxlr * pct) / 100;
      boxh = (boxh * pct) / 100;
      offx = (offx * pct) / 100;
      offy = (offy * pct) / 100;
    }
    /* - - */
    mprint.clip1 = new Area(new Rectangle(0, 0, mprint.getWidth(),
                                                mprint.getHeight()));
    ((Area)mprint.clip1).subtract(new Area(new Rectangle(0, 0, boxul, boxh)));
    ((Area)mprint.clip1).subtract(new Area(new Rectangle(
                                  mprint.getWidth() - boxur, 0, boxur, boxh)));
    ((Area)mprint.clip1).subtract(new Area(new Rectangle(
                                  0, mprint.getHeight() - boxh, boxll, boxh)));
    ((Area)mprint.clip1).subtract(new Area(new Rectangle(
                                  mprint.getWidth() - boxlr,
                                  mprint.getHeight() - boxh, boxlr, boxh)));
    g2.setClip(mprint.clip1);
    double diameter = Math.PI * mprint.pelsPerRadian;  // 180 degrees
    double radius = diameter / 2;                      //  90 degrees
    double diagonal = Math.sqrt(mprint.getWidth() * mprint.getWidth() +
                                mprint.getHeight() * mprint.getHeight());
    mprint.clip2 = ( diameter > diagonal ) ? null :
                   new Ellipse2D.Double(mprint.getMidX() - radius,
                                        mprint.getMidY() - radius,
                                        diameter, diameter);

    /* Color the background if non-white */
    if ( !mprint.prefer.prclrBackGnd().equals(Color.white) ) {
      g2.setColor(mprint.prefer.prclrBackGnd());
      g2.fillRect(0, 0, mprint.getWidth(), mprint.getHeight());
    }

    /* Now draw the astronomy stuff */
    milkyway.draw(mprint);
    horizon.draw(mprint);
    CGrid.draw(mprint);
    AGrid.draw(mprint);
    Ecliptic.draw(mprint);
    constlines.draw(mprint);
    constbounds.draw(mprint);
    Constellation.draw(mprint);
    deepskydb.draw(mprint);
    stardb.draw(mprint);
    starnamedb.draw(mprint);
    nearskydb.draw(mprint);

    /* Set clip, color, & font for rectangles */
    g2.setClip(new Rectangle(0, 0, mprint.getWidth(), mprint.getHeight()));
    g2.setColor(Color.black);
    g2.setFont(boxfont);

    /* Draw rectangles and text */
    g2.drawRect(0, 0, boxul, boxh);
    g2.drawString(Nvj.PgmName, offx, offy);
    g2.drawRect(mprint.getWidth() - boxur, 0, boxur, boxh);
    g2.drawString(mprint.lst.tellLocDateTime(!prefer.is24Hr()),
                  mprint.getWidth() - boxur + offx, offy);
    g2.drawRect(0, mprint.getHeight() - boxh, boxll, boxh);
    g2.drawString(mprint.prefer.tellLocation(),
                  offx, mprint.getHeight() - boxh + offy);
    g2.drawRect(mprint.getWidth()-boxlr, mprint.getHeight()-boxh, boxlr, boxh);
    g2.drawString(mprint.prefer.tellView(), mprint.getWidth() - boxlr + offx,
                                            mprint.getHeight() - boxh + offy);

    /* Relax the clip before drawing final enclosing rectangle */
    g2.setClip(new Rectangle(-1, -1, mprint.getWidth() + 2,
                                     mprint.getHeight() + 2));
    g2.drawRect(0, 0, mprint.getWidth(), mprint.getHeight());

    return Printable.PAGE_EXISTS;
  }
}

/*------------------------------------------------------------------------------

Sequence from mouse click to identification
-------------------------------------------

mousePressed or mouseReleased call mousePopup, which sets idmp & idsc (J2000),
which calls Nvj.showPopup which calls Nvj.actionPerformed with an "ident"
command.  This calls identifyObject to gather info (using idmp & idsc), and
places it in IdentifyDlg.


If someday I want to switch to LineMetrics for print
----------------------------------------------------

(Java 2D Graphics p. 132 suggests replacing FontMetrics with LineMetrics.)

import java.awt.font.LineMetrics;
import java.awt.font.FontRenderContext;

    <Assume g2 font has been set>
    FontRenderContext frc = g2.getFontRenderContext();
    LineMetrics lm = boxfont.getLineMetrics(Nvj.PgmName, frc);
    txth = (int)(lm.getAscent() + lm.getDescent());
    boxh = (txth * 5) / 4;     // Rectangle height
    padx = txth / 2;           // Padding of rectangle width
    offx = (boxh * 3) / 16;    // X offset of text within rectangle
    offy = (boxh * 3) / 4;     // Y offset of text within rectangle
    boxul = (int)(boxfont.getStringBounds(Nvj.PgmName, frc).getWidth()) + padx;
    boxur = (int)(boxfont.getStringBounds(
      mprint.lst.tellLocDateTime(!prefer.is24Hr()), frc).getWidth()) + padx;
    boxll = (int)(boxfont.getStringBounds(
      mprint.prefer.tellLocation(), frc).getWidth()) + padx;
    boxlr = (int)(boxfont.getStringBounds(
      mprint.prefer.tellView(), frc).getWidth()) + padx;

------------------------------------------------------------------------------*/

