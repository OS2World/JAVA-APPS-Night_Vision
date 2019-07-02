/*
 * View.java  -  Direction of view
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

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Direction of view.
 *
 * @author Brian Simpson
 */
public class View {
  // Note:  I am using step and block interchangeably, which refers
  //        to "Block Increment", as opposed to "Unit Increment".
  // ZOOMFACTR is the zoom factor for 'z' (zoomin) and 'Z' (zoomout)
  final static private double ZOOMFACTR = 1.31;          // Zoom step factor
  final static private int    MINZMSTEP = -3;            // -3 steps for min
  final static private int    MAXZMSTEP = 9;             // 9 steps for max
  final static private double MINZOOM = Math.exp(Math.log(ZOOMFACTR)*MINZMSTEP);
                                                         // 1.31 ^ (-3)
  final static private double MAXZOOM = Math.exp(Math.log(ZOOMFACTR)*MAXZMSTEP);
                                                         // 1.31 ^ 9
  // It appears best to keep scroll numbers >= 0
  // (This may no longer apply with JScrollBar's)
  final static public int     ZSCRLRNG = 180;            // zbar range (0-180)
  // Best to have ZSCRLRNG evenly divisible by ZSCRLBLK and
  // ZSCRLRNG / ZSCRLBLK = MAXZMSTEP - MINZMSTEP
  // This keeps the scroll range an integral number of scroll steps and
  // (step via 'z' press) = (step via mouse click on scrollbar)
  final static public int     ZSCRLBLK = ZSCRLRNG / (MAXZMSTEP - MINZMSTEP);
                                                         // zbar block incr = 15
  final static private int    ZSCRLNOM = ZSCRLBLK * MAXZMSTEP;
                                                         // Nom scroll position
  final static private double ZMSC = ZSCRLBLK / Math.log(ZOOMFACTR);
  // The following is for the other 3 scrollbars
  final static public int SCROLLINC = 15;  // 15 degrees block increment (step)

  /** Deg to rad factor */ public static final double Deg2Rad = Math.PI / 180;

  final static private String AZ  = TextBndl.getString("Pgm.St.Az") + " ";
  final static private String ALT = ", " + TextBndl.getString("Pgm.St.Alt")+" ";
  final static private String RA  = TextBndl.getString("Pgm.St.RA") + " ";
  final static private String DEC = ", " + TextBndl.getString("Pgm.St.Dec")+" ";

  public boolean modeRADec = false;

  private double az, oldAz, oldAz2;               // 0 <= az <= 360
  private double alt, oldAlt, oldAlt2;            // -90 <= alt <= 90
  private double zoom, oldZoom, oldZoom2;         // MINZOOM <= zoom <= MAXZOOM
  private int fld;                                // -180 <= fld <= 180
  private JScrollBar vbar = null;
  private JScrollBar hbar = null;
  private JScrollBar zbar = null;
  private JScrollBar fbar = null;
  private JMenuItem mnzi = null, mnzo = null, puzi = null, puzo = null;
  private JButton bnzi = null, bnzo = null;
  private boolean ignoreFeedback = false;
  private boolean mouseMoving = false;
  /* Experiments using Java 1.2.2 on Linux
     When scrollbar is moved by dragging scroll tab, clicking to side of
     tab, or by clicking on either arrow, always got an AdjustmentEvent
     with AdjustmentType = 5 (TRACK).  getValue() returned the current
     value of the tab (and is the only thing of value...).
     (Calling setValue(...) on scrollbar also results in AdjustmentEvent
     with AdjustmentType = 5, and appears indistinguishable from mousing.
     Must use ignoreFeedback variable to distinguish from mouse event.)
     Querying the scrollbar itself for getValueIsAdjusting() did yield
     info on start and stop of drag.  The following illustrates:

     Click on down arrow of vertical scrollbar:
                                     ValueIsAdjusting = false    Value = 1

     Click just below tab:
                                     ValueIsAdjusting = true     Value = 1
                                     ValueIsAdjusting = true     Value = 16
                                     ValueIsAdjusting = false    Value = 16

     Drag tab downward:
                                     ValueIsAdjusting = true     Value = 16
                                     ValueIsAdjusting = true     Value = 21
                                     ValueIsAdjusting = true     Value = 40
                                     ValueIsAdjusting = false    Value = 40

     Will use mouseMoving to keep track of dragging in progress.
     -> If ValueIsAdjusting = true and mouseMoving = false, set mouseMoving
     -> If ValueIsAdjusting = false and mouseMoving = true, clr mouseMoving
  */

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public View() {
    az = oldAz = oldAz2 = 180;
    alt = oldAlt = oldAlt2 = 90;
    zoom = oldZoom = oldZoom2 = 1.0;
    fld = 0;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up vertical scrollbar.
   *
   * @param v Altitude scrollbar
   */
  public void setupVBar(JScrollBar v) {
    vbar = v;
    if ( vbar != null ) {
      /* scroll value of 0   = top    of scrollbar ->  90 degrees alt
         scroll value of 180 = bottom of scrollbar -> -90 degrees alt */
      vbar.setValues((int)(90.5 - alt), SCROLLINC, 0, 180 + SCROLLINC);
      vbar.setUnitIncrement(1); vbar.setBlockIncrement(SCROLLINC);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up horizontal scrollbar.
   *
   * @param h Horizontal scrollbar
   */
  public void setupHBar(JScrollBar h) {
    hbar = h;
    if ( hbar != null ) {
      hbar.setValues((int)(az + 0.5), SCROLLINC, 0, 360 + SCROLLINC);
      hbar.setUnitIncrement(1); hbar.setBlockIncrement(SCROLLINC);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up zoom scrollbar.
   *
   * @param z Zoom scrollbar
   */
  public void setupZBar(JScrollBar z) {
    zbar = z;
    if ( zbar != null ) {
      /* scroll value of     0    = top    of scrollbar -> max zoom
         scroll value of ZSCRLBLK = bottom of scrollbar -> min zoom */
      zbar.setValues(ZSCRLNOM, ZSCRLBLK, 0, ZSCRLRNG + ZSCRLBLK);
      zbar.setUnitIncrement(1); zbar.setBlockIncrement(ZSCRLBLK);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up field rotation scrollbar.
   *
   * @param f Field rotation scrollbar
   */
  public void setupFBar(JScrollBar f) {
    fbar = f;
    if ( fbar != null ) {
      fbar.setValues(fld + 180, SCROLLINC, 0, 360 + SCROLLINC);
      fbar.setUnitIncrement(1); fbar.setBlockIncrement(SCROLLINC);
    }
  }

  /* public void setupMBar() {} */

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns Azimuth in radians (0 - 2pi).  (It is up to the caller to ensure
   * Alt-Az mode.)
   */
  public double getAz() { return az * Deg2Rad; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns Altitude in radians (-pi/2 - pi/2).
   */
  public double getAlt() { return alt * Deg2Rad; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns Right Ascension in radians (0 - 2pi).  (It is up to the caller to
   * ensure RA-Dec mode.)
   */
  public double getRARad() {
    // The azimuth scrollbar ranges from from 0 (left) to 360 (right) whether
    // we are in Alt-Az or RA-Dec modes.  In Alt-Az mode azimuth may be read
    // directly from the scrollbar, but in RA-Dec mode we must subtract the
    // scrollbar value from 360 (because RA progresses in opposite direction).
    return ((360 - az) * Deg2Rad);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns Declination in radians (-pi/2 - pi/2).
   */
  public double getDecRad() { return (alt * Deg2Rad); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the current zoom.
   */
  public double getZoom() { return zoom; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the current zoom as a factor from 0.0 to 1.0.  0.0 represents
   * MINZOOM (zoomed all the way out), and 1.0 is reached just before MAXZOOM
   * (zoomed all the way in), and stays at 1.0 until MAXZOOM.  This function
   * is used to add stars and deep sky objects as the user is zooming in, and
   * the offset just before MAXZOOM adds a little padding to show all stars
   * and objects before MAXZOOM.
   */
  public double getZoomFactor() {
    return Math.min(1.05 * (zoom - MINZOOM) / (MAXZOOM - MINZOOM), 1.0);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the current field rotation.
   */
  public int getFld() { return fld; }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  private void push() { oldAz = az; oldAlt = alt; oldZoom = zoom; }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  private void push2() { oldAz2 = az; oldAlt2 = alt; oldZoom2 = zoom; }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  private void setVScrollBar() {
    if ( vbar != null ) {
      ignoreFeedback = true;
      vbar.setValue((int)(90.5 - alt));
      ignoreFeedback = false;
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  private void setHScrollBar() {
    if ( hbar != null ) {
      ignoreFeedback = true;
      hbar.setValue((int)(az + 0.5));
      ignoreFeedback = false;
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Moves the zoom scrollbar to match the value of zoom.
   */
  private void setZoomScrollBar() {
    if ( zbar != null ) {
      ignoreFeedback = true;
      // See bottom of file for derivation
      zbar.setValue(Math.max(0, Math.min(ZSCRLRNG,
                    (int)(ZSCRLNOM + 0.5 - ZMSC * Math.log(zoom)))));
      ignoreFeedback = false;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets up references to zoomin and zoomout menu items so that they can be
   * manipulated when zoom limits have been reached or left.
   *
   * @param mnzi Zoomin menu item
   * @param mnzo Zoomout menu item
   * @param puzi Zoomin popup menu item
   * @param puzo Zoomout popup menu item
   * @param bnzi Zoomin toolbar button
   * @param bnzo Zoomout toolbar button
   */
  public void setupZoomMenuItems(JMenuItem mnzi, JMenuItem mnzo,
                                 JMenuItem puzi, JMenuItem puzo,
                                 JButton   bnzi, JButton   bnzo) {
    this.mnzi = mnzi;
    this.mnzo = mnzo;
    this.puzi = puzi;
    this.puzo = puzo;
    this.bnzi = bnzi;
    this.bnzo = bnzo;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Changes zoomin and zoomout menu item enabled states.
   * Be sure that zbar is properly position before calling this function.
   */
  private void setZoomMenuItems() {
    if ( isMaxZoom() ) {
      if ( mnzi != null ) mnzi.setEnabled(false);
      if ( puzi != null ) puzi.setEnabled(false);
      if ( bnzi != null ) bnzi.setEnabled(false);
    }
    else {
      if ( mnzi != null ) mnzi.setEnabled(true);
      if ( puzi != null ) puzi.setEnabled(true);
      if ( bnzi != null ) bnzi.setEnabled(true);
    }
    if ( isMinZoom() ) {
      if ( mnzo != null ) mnzo.setEnabled(false);
      if ( puzo != null ) puzo.setEnabled(false);
      if ( bnzo != null ) bnzo.setEnabled(false);
    }
    else {
      if ( mnzo != null ) mnzo.setEnabled(true);
      if ( puzo != null ) puzo.setEnabled(true);
      if ( bnzo != null ) bnzo.setEnabled(true);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns boolean indicating if at maximum zoom level.
   */
  public boolean isMaxZoom() {
    if ( zbar != null ) return (0 == zbar.getValue());   // 0 = top of scrollbar
    else                return (zoom > 0.999 * MAXZOOM);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns boolean indicating if at minimum zoom level.
   */
  public boolean isMinZoom() {
    if ( zbar != null ) return (ZSCRLRNG == zbar.getValue()); // ZSCRLRNG = btm
    else                return (zoom < 1.001 * MINZOOM);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Zooms in.  Called when "Zoom in" menuitem has been selected
   * (or 'z' pressed).
   *
   * @return True if zoom changed (to signal repaint), else false
   */
  public boolean zoomIn() { return setZoom(ZOOMFACTR); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Zooms out.  Called when "Zoom out" menuitem has been selected
   * (or 'Z' pressed).
   *
   * @return True if zoom changed (to signal repaint), else false
   */
  public boolean zoomOut() { return setZoom(1/ZOOMFACTR); }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Multiplies zoom by the specified zoom factor.
   * Returns true if zoom changed.
   */
  private boolean setZoom(double zf) {
    if ( zf < 1.001 && zf > .999 ) return false;
    if ( zf > 1     && isMaxZoom() ) return false;
    if ( zf < 1     && isMinZoom() ) return false;
    push(); push2();
    zoom = Math.max(MINZOOM, Math.min(zoom * zf, MAXZOOM));
    setZoomScrollBar();
    setZoomMenuItems();  // Must be after setZoomScrollBar()
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called to set az, alt, and zoom (by mouse and find).
   *
   * @param az Azimuth in degrees
   * @param alt Altitude in degrees
   * @param zf Zoom factor (with which zoom is multipled by)
   */
  public void setAzAltZoom(double az, double alt, double zf) {
    if ( setZoom(zf) == false )    // Calls setZoomMenuItems if zoom changed
      push();
    this.alt = Math.max(-90, Math.min(alt, 90));
    while ( az  <  0 ) az += 360;
    while ( az > 360 ) az -= 360;
    this.az = az;
    setVScrollBar();
    setHScrollBar();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called to set spherical coordinates and zoom (by mouse and find).
   *
   * @param sc Spherical coordinates
   * @param zf Zoom factor (with which zoom is multipled by)
   */
  public void setAzAltZoom(SphereCoords sc, double zf) {
    setAzAltZoom(sc.getAz() / Deg2Rad, sc.getAlt() / Deg2Rad, zf);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when 'p' is pressed (or moused).
   *
   * @return True if position and/or zoom changed
   */
  public boolean swapView() {
    double tempAz, tempAlt, tempZoom;

    if ( alt == oldAlt && zoom == oldZoom && ((az - oldAz) % 360) == 0 ) {
      if ( az != oldAz) {
        tempAz = oldAz;  oldAz = az;  az = tempAz;
        setHScrollBar();
      }
      return false;
    }

    tempAz = oldAz;  tempAlt = oldAlt;  tempZoom = oldZoom;
    push();
    if ( zoom != tempZoom ) push2();
    az = tempAz;  alt = tempAlt;  zoom = tempZoom;
    setHScrollBar();
    setVScrollBar();
    setZoomScrollBar();
    setZoomMenuItems();  // Must be after setZoomScrollBar()
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when 'P' is pressed (or moused).
   *
   * @return True if position and/or zoom changed
   */
  public boolean swapZoom() {
    double tempAz, tempAlt, tempZoom;

    if ( alt == oldAlt2 && zoom == oldZoom2 && ((az - oldAz2) % 360) == 0 ) {
      if ( az != oldAz2) {
        tempAz = oldAz2;  oldAz2 = az;  az = tempAz;
        setHScrollBar();
      }
      return false;
    }

    tempAz = oldAz2;  tempAlt = oldAlt2;  tempZoom = oldZoom2;
    push();
    push2();
    az = tempAz;  alt = tempAlt;  zoom = tempZoom;
    setHScrollBar();
    setVScrollBar();
    setZoomScrollBar();
    setZoomMenuItems();  // Must be after setZoomScrollBar()
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when azimuth scrollbar is moved via mouse click or keystroke
   * or via a menu selection.
   *
   * @param c Type of change
   * @return True if azimuth changed
   */
  public boolean moveAz(char c) {
    boolean rc = false;

    if ( c == 'l' || c == 'L' )
    {
      if ( az > 0 )
      {
        push();
        if ( c == 'l' ) az = Math.max(0, az - 1);
        else            az = Math.max(0, az - SCROLLINC);
        rc = true;
      }
    }
    else if ( c == 'r' || c == 'R' )
    {
      if ( az < 360 )
      {
        push();
        if ( c == 'r' ) az = Math.min(360, az + 1);
        else            az = Math.min(360, az + SCROLLINC);
        rc = true;
      }
    }
    else if ( c == 'n' )
    {
      if ( az != 0 && az != 360 )
      {
        push();
        az = 0;
        rc = true;
      }
    }
    else if ( c == 's' && az != 180 )
    {
      push();
      az = 180;
      rc = true;
    }
    else if ( c == 'e' && az != 90 )
    {
      push();
      az = 90;
      rc = true;
    }
    else if ( c == 'w' && az != 270 )
    {
      push();
      az = 270;
      rc = true;
    }
    else if ( c == '0' && az != 360 ) // 0 hours RA (at Az = 360)
    {
      push();
      az = 360;
      rc = true;
    }
    if ( rc ) setHScrollBar();
    return rc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when altitude scrollbar is moved via mouse click or keystroke
   * or via a menu selection.
   *
   * @param c Type of change
   * @return True if altitude changed
   */
  public boolean moveAlt(char c) {
    boolean rc = false;

    if ( c == 'd' || c == 'D' )
    {
      if ( alt > -90 )
      {
        push();
        if ( c == 'd' ) alt = Math.max(-90, alt - 1);
        else            alt = Math.max(-90, alt - SCROLLINC);
        rc = true;
      }
    }
    else if ( c == 'u' || c == 'U' )
    {
      if ( alt < 90 )
      {
        push();
        if ( c == 'u' ) alt = Math.min(90, alt + 1);
        else            alt = Math.min(90, alt + SCROLLINC);
        rc = true;
      }
    }
    else if ( c == 'h' && alt != 0 )
    {
      push();
      alt = 0;
      rc = true;
    }
    else if ( c == 'z' && alt != 90 )
    {
      push();
      alt = 90;
      rc = true;
    }
    else if ( c == 'n' && alt != -90 )
    {
      push();
      alt = -90;
      rc = true;
    }
    if ( rc ) setVScrollBar();
    return rc;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the fld value and the Field Rotation scrollbar.
   *
   * @param deg New rotation setting (-180 &lt;= deg &lt;= 180)
   * @return True if fld changed
   */
  public boolean setFld(int deg) {
    if ( fld == deg ) return false;

    fld = Math.max(0, Math.min(deg, 360));
    if ( fbar != null ) {
      ignoreFeedback = true;
      fbar.setValue(fld + 180);
      ignoreFeedback = false;
    }
    return true;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when horizontal scrollbar has been moused
   * (any mouse activity, including arrow clicks &amp; dragging),
   * or when it has been set (which then generates an
   * AdjustmentEvent indistinguishable from mousing).
   *
   * @param sv Current scrollbar value
   * @param adjusting Value from getValueIsAdjusting()
   * @return True if scroll changed, false otherwise
   */
  public boolean mouseMoveHBar(int sv, boolean adjusting) { // sv = scroll value
    if ( ignoreFeedback ) return false;   // Ignore setting of scrollbar
    if ( adjusting ) {
      if ( !mouseMoving ) {
        mouseMoving = true;  push();
      }
    }
    else { // Not adjusting
      if ( mouseMoving )
        mouseMoving = false;
      else
        push();
    }
    int newaz = (sv<0) ? 0 : ((sv>360) ? 360 : sv);
    if ( this.az != newaz ) { this.az = newaz; return true; }
    return false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when vertical scrollbar has been moused
   * (any mouse activity, including arrow clicks &amp; dragging),
   * or when it has been set (which then generates an
   * AdjustmentEvent indistinguishable from mousing).
   *
   * @param sv Current scrollbar value
   * @param adjusting Value from getValueIsAdjusting()
   * @return True if scroll changed, false otherwise
   */
  public boolean mouseMoveVBar(int sv, boolean adjusting) { // sv = scroll value
    if ( ignoreFeedback ) return false;   // Ignore setting of scrollbar
    if ( adjusting ) {
      if ( !mouseMoving ) {
        mouseMoving = true;  push();
      }
    }
    else { // Not adjusting
      if ( mouseMoving )
        mouseMoving = false;
      else
        push();
    }
    sv = 90 - sv;
    int newalt = (sv<-90) ? -90 : ((sv>90) ? 90 : sv);
    if ( this.alt != newalt ) { this.alt = newalt; return true; }
    return false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when zoom scrollbar has been moused
   * (any mouse activity, including arrow clicks &amp; dragging),
   * or when it has been set (which then generates an
   * AdjustmentEvent indistinguishable from mousing).
   *
   * @param sv Current scrollbar value
   * @param adjusting Value from getValueIsAdjusting()
   * @return True if scroll changed, false otherwise
   */
  public boolean mouseMoveZBar(int sv, boolean adjusting) { // sv = scroll value
    if ( ignoreFeedback ) return false;   // Ignore setting of scrollbar
    if ( adjusting ) {
      if ( !mouseMoving ) {
        mouseMoving = true;
        push(); push2();
      }
    }
    else { // Not adjusting
      if ( mouseMoving )
        mouseMoving = false;
      else {
        push(); push2();
      }
    }
    double oldzoom = zoom;
    // See bottom of file for derivation
    zoom = Math.max(MINZOOM,
           Math.min(Math.exp((ZSCRLNOM - sv) / ZMSC), MAXZOOM));
    if ( zoom != oldzoom ) { setZoomMenuItems(); return true; }
    return false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called when field rotation scrollbar has been moused
   * (any mouse activity, including arrow clicks &amp; dragging),
   * or when it has been set (which then generates an
   * AdjustmentEvent indistinguishable from mousing).
   *
   * @param sv Current scrollbar value
   * @param adjusting Value from getValueIsAdjusting()
   * @return True if scroll changed, false otherwise
   */
  public boolean mouseMoveFBar(int sv, boolean adjusting) { // sv = scroll value
    if ( ignoreFeedback ) return false;   // Ignore setting of scrollbar
    if ( adjusting ) {
      if ( !mouseMoving ) {
        mouseMoving = true;
      }
    }
    else { // Not adjusting
      if ( mouseMoving )
        mouseMoving = false;
    }
    int newfld = (sv<0) ? 0 : ((sv>360) ? 360 : sv) - 180;
    if ( this.fld != newfld ) { this.fld = newfld; return true; }
    return false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Clears previous view and zoom.
   */
  public void clearPrevious() {
    oldAz = oldAz2 = az;
    oldAlt = oldAlt2 = alt;
    oldZoom = oldZoom2 = zoom;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns Alt/Az or RA/Dec (depending on modeRADec) as a string.
   */
  public String tellView() {
    SphereCoords sc;

    if ( modeRADec) {
      sc = new SphereCoords((360 - az) * Deg2Rad, alt * Deg2Rad);
      return RA + sc.tellRAHrMnT() + DEC + sc.tellDecDgMn();
    }
    else {
      sc = new SphereCoords(az * Deg2Rad, alt * Deg2Rad);
      return AZ + sc.tellAzDgMn() + ALT + sc.tellAltDgMn();
    }
  }
}

/*------------------------------------------------------------------------------

Derivation of zoom formulas
---------------------------

For each increment of the zoom scrollbar, want zoom to multiply by a constant
factor.  This is an exponential relationship.  A slight complication arises
from the fact that the top of the scrollbar (max zoom) is 0, and the bottom
(min zoom) is 180.  In the following, each scroll block increment changes
the zoom by a factor of 1.31, and from nom there is 9 steps to max, and 3
steps to min.  Dividing the range by the number of steps gives a step size
(block increment) of 15.
                             min     nom     max

scroll:    logarithmic --->  180     135       0    -------------
               ^                                                |
               |                 -3      0       +9             v
 zoom:         ------------  1.31    1.31    1.31   <---- exponential

(Note:  135 is 9/12 of 180)

               0 - 180
scroll = 135 + -------- log ( zoom )   =   135 - 15 (ln zoom) / (ln 1.31)
               9 - (-3)    1.31

(Change 135 to 135.5 and truncate to int for rounding.)

        (ln 1.31) (135 - scroll) / 15           (135 - scroll) / 15
zoom = e                                =   1.31


In more general terms
---------------------

scroll = ZSCRLNOM - ZMSC * ln(zoom)

        ((ZSCRLNOM - scroll) / ZMSC)
zoom = e

where
                            MAXZMSTEP
ZSCRLNOM = ZSCRLRNG * --------------------- = ZSCRLBLK * MAXZMSTEP
                      MAXZMSTEP - MINZMSTEP

               ZSCRLRNG                            ZSCRLBLK
ZMSC = --------------------------------------- = -------------
       (MAXZMSTEP - MINZMSTEP) * ln(ZOOMFACTR)   ln(ZOOMFACTR)

ZOOMFACTR = 1.31, MAXZMSTEP = 9, MINZMSTEP = -3, ZSCRLRNG = 180
Derived: ZSCRLBLK = 15, ZSCRLNOM = 135, ZMSC = 55.549972328

------------------------------------------------------------------------------*/

