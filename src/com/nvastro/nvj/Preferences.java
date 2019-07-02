/*
 * Preferences.java  -  User preferences
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
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.GregorianCalendar;

import javax.swing.JToggleButton;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Holds true/false state; may be tied with JToggleButton.
 *
 * @author Brian Simpson
 */
class ToggleBtn {
  JToggleButton btn = null;
  boolean state = false;

  public ToggleBtn() {
  }

  public void setButton(JToggleButton button) {
    btn = button;
    btn.setSelected(state);
  }

  public void set(boolean val) {
    if ( btn == null ) state = val;
    else               btn.setSelected(val);
  }

  public boolean get() {
    return (btn == null) ? state : btn.isSelected();
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * User preferences.
 *
 * @author Brian Simpson
 */
public class Preferences extends View implements Cloneable {
  // Must declare "implements Cloneable" since clone() method calls
  // super.clone() which requires this.  Otherwise will get
  // CloneNotSupportedException thrown.
  public final static int MINMAG10 = 20;      // 2nd magnitude minimum
  public final static int MAXMAG10 = 200;     // 20th magnitude maximum
  public final static int MAXTIMEFACTOR = 10080;
  private ToggleBtn constLines = new ToggleBtn(),
                    constNames = new ToggleBtn(),
                    constBounds = new ToggleBtn();
  private boolean constNFull, bayer, flamsteed, starNames;
  private ToggleBtn cGrid = new ToggleBtn(),
                    aGrid = new ToggleBtn(),
                    ecliptic = new ToggleBtn(),
                    horizon = new ToggleBtn();
  private boolean cGridLabels, aGridLabels;
  private ToggleBtn nearSky = new ToggleBtn(),
                    deepSky = new ToggleBtn(),
                    deepSkyNm = new ToggleBtn(),
                    milkyWay = new ToggleBtn();
  public String nSkySuppress;
  private boolean ampm;
  private boolean toolBar, winInfo, scrlAzAlt, scrlZoom, scrlField;
  private int updatePeriod;
  private int zoLimMag10, ziLimMag10, szBright, szDim;
  private int zoDSLimMag10, ziDSLimMag10;
  private boolean bmpStars;
  private int winLt, winTp, winWd, winHt;
  private int ssLt, ssTp, ssWd, ssHt;
  static public int dstAdjust = 0; // Test purposes
  static public int boxAdjustPct = 0;
  private Color clrBackGnd, clrConst, clrConstBound, clrStar, clrStarName,
    clrDeepSky, clrCGrid, clrAGrid, clrEcliptic, clrHorizon, clrSun,
    clrPlanet, clrMoon, clrMilkyWay;
  private Color pclrBackGnd, pclrConst, pclrConstBound, pclrStar, pclrStarName,
    pclrDeepSky, pclrCGrid, pclrAGrid, pclrEcliptic, pclrHorizon, pclrSun,
    pclrPlanet, pclrMoon, pclrMilkyWay;
  private Font fntConst, fntStarName, fntSolarSys, fntDeepSky, fntCGrid,
    fntAGrid, fntHorizon, fntStarLabel = null;
  static private String slewout = null; // null means no Slew btn
  static private String scopein = null; // null means no scope monitoring
  // The following appears to be the dft font for a new JPanel
  private final static Font dftFont = new Font("Dialog", Font.PLAIN, 12);
  private final static Font grdFont = new Font("Dialog", Font.PLAIN, 10);
  private final static Font conFont = new Font("Dialog", Font.BOLD |
                                                         Font.ITALIC, 12);
  public LST lst;
  public boolean antialiasing = true;
  static public boolean option1 = false, option2 = false;
  static public boolean mnphases = true;
  static public boolean geocentric = false;
  static public boolean usedeltat = true;
  public boolean shadeHorizon = false;
  //last printer, ...
  /*- Initialization file keys -----------------------------------------------*/
  private final static String keyConstLines = "ConstLines";
  private final static String keyConstNames = "ConstNames";
  private final static String keyConstNFull = "ConstNameFull";
  private final static String keyConstBounds = "ConstBounds";
  private final static String keyBayer = "Bayer";
  private final static String keyFlamsteed = "Flamsteed";
  private final static String keyStarNames = "StarNames";
  private final static String keyCGrid = "CGrid";
  private final static String keyCGridLabels = "CGridLabels";
  private final static String keyAGrid = "AGrid";
  private final static String keyAGridLabels = "AGridLabels";
  private final static String keyEcliptic = "Ecliptic";
  private final static String keyHorizon = "Horizon";
  private final static String keyNearSky = "NearSky";
  private final static String keyNSkySuppress = "NearSkySuppress";
  private final static String keyDeepSky = "DeepSky";
  private final static String keyDeepSkyNames = "DeepSkyNames";
  private final static String keyMilkyWay = "MilkyWay";
  private final static String keyShadeHorizon = "ShadeHorizon";
  private final static String keyMoonPhases = "MoonPhases";

  private final static String keyRADecMode = "RADecMode";

  private final static String keyToolBar = "ToolBar";
  private final static String keyWinInfo = "InfoWindow";
  private final static String keyScrlAzAlt = "ScrollAzAlt";
  private final static String keyScrlZoom = "ScrollZoom";
  private final static String keyScrlField = "ScrollField";

  private final static String keyBackGndClr = "BackGndColor";
  private final static String keyConstClr = "ConstColor";
  private final static String keyConstBoundClr = "ConstBoundColor";
  private final static String keyStarClr = "StarColor";
  private final static String keyStarNameClr = "StarNameColor";
  private final static String keyDeepSkyClr = "DeepSkyColor";
  private final static String keyCGridClr = "CGridColor";
  private final static String keyAGridClr = "AGridColor";
  private final static String keyEclipticClr = "EclipticColor";
  private final static String keyHorizonClr = "HorizonColor";
  private final static String keySunClr = "SunColor";
  private final static String keyPlanetClr = "PlanetColor";
  private final static String keyMoonClr = "MoonColor";
  private final static String keyMilkyWayClr = "MilkyWayColor";

  private final static String keyBackGndPClr = "BackGndPrColor";
  private final static String keyConstPClr = "ConstPrColor";
  private final static String keyConstBoundPClr = "ConstBoundPrColor";
  private final static String keyStarPClr = "StarPrColor";
  private final static String keyStarNamePClr = "StarNamePrColor";
  private final static String keyDeepSkyPClr = "DeepSkyPrColor";
  private final static String keyCGridPClr = "CGridPrColor";
  private final static String keyAGridPClr = "AGridPrColor";
  private final static String keyEclipticPClr = "EclipticPrColor";
  private final static String keyHorizonPClr = "HorizonPrColor";
  private final static String keySunPClr = "SunPrColor";
  private final static String keyPlanetPClr = "PlanetPrColor";
  private final static String keyMoonPClr = "MoonPrColor";
  private final static String keyMilkyWayPClr = "MilkyWayPrColor";

  private final static String keyConstFnt = "ConstFont";
  private final static String keyStarNameFnt = "StarNameFont";
  private final static String keySolarSysFnt = "SolarSystemFont";
  private final static String keyDeepSkyFnt = "DeepSkyFont";
  private final static String keyCGridFnt = "CGridFont";
  private final static String keyAGridFnt = "AGridFont";
  private final static String keyHorizonFnt = "HorizonFont";

  private final static String keyCity = "City";
  private final static String keyLongitude = "Longitude";
  private final static String keyLatitude = "Latitude";
  private final static String keyTZOffset = "TZOffset";
  private final static String keyDST = "DST";
  // keyDSTAdjust undocumented, mainly for test purposes
  private final static String keyDSTAdjust = "DSTAdjustMin";    // Minutes

  private final static String keyBoxAdjustPct = "BoxAdjustPct"; // Percentage

  private final static String keyAmPm = "AMPM";
  private final static String keyZoLimMag10 = "ZoLimMag10";
  private final static String keyZiLimMag10 = "ZiLimMag10";
  private final static String keySzBright = "StarSizeBright";
  private final static String keySzDim = "StarSizeDim";
  private final static String keyStarOld = "StarOld";
  private final static String keyZoDSLimMag10 = "ZoDSLimMag10";
  private final static String keyZiDSLimMag10 = "ZiDSLimMag10";
  private final static String keyUpdtPer = "UpdatePeriod";
  private final static String keyWinLt = "MainWinLeft";
  private final static String keyWinTp = "MainWinTop";
  private final static String keyWinWd = "MainWinWidth";
  private final static String keyWinHt = "MainWinHeight";
  private final static String keySSLt  = "MainSSLeft";
  private final static String keySSTp  = "MainSSTop";
  private final static String keySSWd  = "MainSSWidth";
  private final static String keySSHt  = "MainSSHeight";

  private final static String keySlew  = "SlewOut";
  private final static String keyScope = "ScopeIn";

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public Preferences() {
    CityDB cities = new CityDB();
    loadPrefer();

    /* Set up lst, after first setting up the viewing location */
    Location loc = cities.getLocationForCity(Initor.getString(keyCity, null));
    if ( loc == null ) {   // No city or city not in DB, check coordinates
      try {
        loc = new Location(null,
                           Initor.getString(keyLongitude, null),
                           Initor.getString(keyLatitude,  null),
                           Initor.getString(keyTZOffset,  null));
      }
      catch ( Exception e ) {
        // No valid city or coordinates, look for Niwot in CityDB so that
        // it's Location object is used (and LocationDlg works correctly)
        loc = cities.getLocationForCity("Niwot, Colorado, USA");

        if ( loc == null )  // If not in DB, create new Location object
          loc = new Location("Niwot, Colorado, USA", "-105:10", "40:06",
                             "America/Denver");
      }
    }
    dstAdjust = Math.max(-120, Math.min(Initor.getInt(keyDSTAdjust, 0), 120));
    // Note:  Set dstAdjust before creating lst so that it sees dstAdjust
    lst = new LST(loc, Initor.getString(keyDST, "A"));

    // Size adjust for the 4 corner boxes on a print
    boxAdjustPct = Math.max(-10,Math.min(Initor.getInt(keyBoxAdjustPct, 0),10));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a clone of the object.
   */
  public Object clone() {
    Preferences p = null;
    try {
      p = (Preferences) super.clone();
    } catch ( CloneNotSupportedException e ) { /* Should never happen */
      ErrLogger.die(e.toString());        // toString() uses getMessage()
    }
    p.lst = (LST) lst.clone();
    return p;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Loads preferences from the ini file.
   * This function contains variable defaults.
   * It could be merged into the constructor (or kept
   * separate for clarity...).
   * Errors are handled in the Initor class.
   */
  private void loadPrefer() {
    constLines.set ( Initor.getBoolean(keyConstLines,   true));
    constNames.set ( Initor.getBoolean(keyConstNames,   true));
    constNFull     = Initor.getBoolean(keyConstNFull,   true);
    constBounds.set( Initor.getBoolean(keyConstBounds,  true));
    bayer          = Initor.getBoolean(keyBayer,        false);
    flamsteed      = Initor.getBoolean(keyFlamsteed,    false);
    starNames      = Initor.getBoolean(keyStarNames,    false);
    cGrid.set      ( Initor.getBoolean(keyCGrid,        false));
    cGridLabels    = Initor.getBoolean(keyCGridLabels,  true);
    aGrid.set      ( Initor.getBoolean(keyAGrid,        false));
    aGridLabels    = Initor.getBoolean(keyAGridLabels,  true);
    ecliptic.set   ( Initor.getBoolean(keyEcliptic,     false));
    horizon.set    ( Initor.getBoolean(keyHorizon,      true));
    nearSky.set    ( Initor.getBoolean(keyNearSky,      true));
    deepSky.set    ( Initor.getBoolean(keyDeepSky,      true));
    deepSkyNm.set  ( Initor.getBoolean(keyDeepSkyNames, false));
    milkyWay.set   ( Initor.getBoolean(keyMilkyWay,     true));
    shadeHorizon   = Initor.getBoolean(keyShadeHorizon, false);
    mnphases       = Initor.getBoolean(keyMoonPhases,   mnphases);

    modeRADec      = Initor.getBoolean(keyRADecMode,    false);

    toolBar        = Initor.getBoolean(keyToolBar,      true);
    winInfo        = Initor.getBoolean(keyWinInfo,      true);
    scrlAzAlt      = Initor.getBoolean(keyScrlAzAlt,    false);
    scrlZoom       = Initor.getBoolean(keyScrlZoom,     false);
    scrlField      = Initor.getBoolean(keyScrlField,    false);

    // Black = Color(0, 0, 0); White = Color(255, 255, 255)

    clrBackGnd     = Initor.getColor(keyBackGndClr, Color.black);
    clrConst       = Initor.getColor(keyConstClr, new Color(91,91,103));
    clrConstBound  = Initor.getColor(keyConstBoundClr, new Color(0,80,120));
    clrStar        = Initor.getColor(keyStarClr, Color.white);
    clrStarName    = Initor.getColor(keyStarNameClr, new Color(153,255,153));
    clrDeepSky     = Initor.getColor(keyDeepSkyClr, new Color(255,204,204));
    clrCGrid       = Initor.getColor(keyCGridClr, new Color(0,71,71));
    clrAGrid       = Initor.getColor(keyAGridClr, new Color(0,88,0));
    clrEcliptic    = Initor.getColor(keyEclipticClr, new Color(132,132,26));
    clrHorizon     = Initor.getColor(keyHorizonClr, new Color(0,90,90));
    clrSun         = Initor.getColor(keySunClr, Color.yellow);
    clrPlanet      = Initor.getColor(keyPlanetClr, new Color(0, 198,146));
    clrMoon        = Initor.getColor(keyMoonClr, new Color(170,170,170));
    clrMilkyWay    = Initor.getColor(keyMilkyWayClr, new Color(32,48,60));

    pclrBackGnd    = Initor.getColor(keyBackGndPClr, Color.white);
    pclrConst      = Initor.getColor(keyConstPClr, new Color(204,204,255));
    pclrConstBound = Initor.getColor(keyConstBoundPClr, new Color(0,102,153));
    pclrStar       = Initor.getColor(keyStarPClr, Color.black);
    pclrStarName   = Initor.getColor(keyStarNamePClr, new Color(153,255,153));
    pclrDeepSky    = Initor.getColor(keyDeepSkyPClr, new Color(255,204,204));
    pclrCGrid      = Initor.getColor(keyCGridPClr, new Color(204,255,255));
    pclrAGrid      = Initor.getColor(keyAGridPClr, new Color(204,255,204));
    pclrEcliptic   = Initor.getColor(keyEclipticPClr, new Color(246,246,127));
    pclrHorizon    = Initor.getColor(keyHorizonPClr, new Color(196,240,240));
    pclrSun        = Initor.getColor(keySunPClr, Color.yellow);
    pclrPlanet     = Initor.getColor(keyPlanetPClr, Color.green);
    pclrMoon       = Initor.getColor(keyMoonPClr, Color.gray);
    pclrMilkyWay   = Initor.getColor(keyMilkyWayPClr, new Color(204,204,204));

    fntConst       = Initor.getFont(keyConstFnt, conFont);
    fntStarName    = Initor.getFont(keyStarNameFnt, dftFont);
    fntSolarSys    = Initor.getFont(keySolarSysFnt, dftFont);
    fntDeepSky     = Initor.getFont(keyDeepSkyFnt, dftFont);
    fntCGrid       = Initor.getFont(keyCGridFnt, grdFont);
    fntAGrid       = Initor.getFont(keyAGridFnt, grdFont);
    fntHorizon     = Initor.getFont(keyHorizonFnt, dftFont);

    ampm           = Initor.getBoolean(keyAmPm, false);

    nSkySuppress   = Initor.getString(keyNSkySuppress, "");

    zoLimMag10     = Initor.getInt(keyZoLimMag10, 60);  // 6th mag for now...
    ziLimMag10     = Initor.getInt(keyZiLimMag10, 110); // 11th mag for now...
    szBright       = Initor.getInt(keySzBright, StarImages.NUMIMAGES-2);
    szDim          = Initor.getInt(keySzDim, 1);
    setSzStar(szBright, szDim);
    bmpStars       = Initor.getBoolean(keyStarOld, false); // Old (bitmap) stars
    zoDSLimMag10   = Initor.getInt(keyZoDSLimMag10, 70); // 6th mag for now...
    ziDSLimMag10   = Initor.getInt(keyZiDSLimMag10,120); // 12th mag for now...
    updatePeriod   = Initor.getInt(keyUpdtPer, 60); // Dft 60 seconds
    updatePeriod   = Math.max(1, Math.min(updatePeriod, 120));
    winLt          = Initor.getInt(keyWinLt, 100); // Dft dist from left side
    winTp          = Initor.getInt(keyWinTp, 100); // Dft distance from top
    winWd          = Initor.getInt(keyWinWd, 575); // Dft width of window
    winHt          = Initor.getInt(keyWinHt, 450); // Dft height of window
    ssLt           = Initor.getInt(keySSLt,  150); // Dft dist from left side
    ssTp           = Initor.getInt(keySSTp,  150); // Dft distance from top
    ssWd           = Initor.getInt(keySSWd,  525); // Dft width of window
    ssHt           = Initor.getInt(keySSHt,  420); // Dft height of window

    String slew    = Initor.getString(keySlew, "").trim();
    // Or do:      = Initor.getString(keySlew, "slewout").trim();
    //             if having a slew button on by default is desired
    if ( slew.length() > 0 ) {
      if ( slew.substring(0, 1).equals(System.getProperty("file.separator")) )
        slewout = slew;
      else
        slewout = System.getProperty("user.home") +
                  System.getProperty("file.separator") + slew;
    }
    // else slewout (already) null

    String scope   = Initor.getString(keyScope, "").trim();
    // Or do:      = Initor.getString(keyScope, "scopein").trim();
    //             if having a scope input pipe by default is desired
    if ( scope.length() > 0 ) {
      if ( scope.substring(0, 1).equals(System.getProperty("file.separator")) )
        scopein = scope;
      else
        scopein = System.getProperty("user.home") +
                  System.getProperty("file.separator") + scope;
    }
    // else scopein (already) null
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Saves viewing location in the ini file.
   * Errors are handled in the Initor class.
   *
   * @return True if successful
   */
  public boolean saveLocation() {
    Initor.setString(keyCity, tellCity());
    Initor.setString(keyLongitude, tellLong());
    Initor.setString(keyLatitude, tellLat());
    Initor.setString(keyTZOffset, tellTZOffset());
    Initor.setString(keyDST, getDST());

    return Initor.save();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Saves selected preferences in the ini file.
   * Errors are handled in the Initor class.
   *
   * @return True if successful
   */
  public boolean savePrefer() {
    Initor.setBoolean(keyConstLines, constLines.get());
    Initor.setBoolean(keyConstNames, constNames.get());
    Initor.setBoolean(keyConstNFull, constNFull);
    Initor.setBoolean(keyConstBounds, constBounds.get());
    Initor.setBoolean(keyBayer, bayer);
    Initor.setBoolean(keyFlamsteed, flamsteed);
    Initor.setBoolean(keyStarNames, starNames);
    Initor.setBoolean(keyCGrid, cGrid.get());
    Initor.setBoolean(keyCGridLabels, cGridLabels);
    Initor.setBoolean(keyAGrid, aGrid.get());
    Initor.setBoolean(keyAGridLabels, aGridLabels);
    Initor.setBoolean(keyEcliptic, ecliptic.get());
    Initor.setBoolean(keyHorizon, horizon.get());
    Initor.setBoolean(keyNearSky, nearSky.get());
    Initor.setBoolean(keyDeepSky, deepSky.get());
    Initor.setBoolean(keyDeepSkyNames, deepSkyNm.get());
    Initor.setBoolean(keyMilkyWay, milkyWay.get());

    Initor.setBoolean(keyRADecMode, modeRADec);

    Initor.setBoolean(keyToolBar, toolBar);
    Initor.setBoolean(keyWinInfo, winInfo);
    Initor.setBoolean(keyScrlAzAlt, scrlAzAlt);
    Initor.setBoolean(keyScrlZoom, scrlZoom);
    Initor.setBoolean(keyScrlField, scrlField);

    //itor.setColor(keyBackGndClr, clrBackGnd);  Intent. omitted for now...
    Initor.setColor(keyConstClr, clrConst);
    Initor.setColor(keyConstBoundClr, clrConstBound);
    //itor.setColor(keyStarClr, clrStar);  Intentionally omitted for now...
    Initor.setColor(keyStarNameClr, clrStarName);
    Initor.setColor(keyDeepSkyClr, clrDeepSky);
    Initor.setColor(keyCGridClr, clrCGrid);
    Initor.setColor(keyAGridClr, clrAGrid);
    Initor.setColor(keyEclipticClr, clrEcliptic);
    Initor.setColor(keyHorizonClr, clrHorizon);
    Initor.setColor(keySunClr, clrSun);
    Initor.setColor(keyPlanetClr, clrPlanet);
    Initor.setColor(keyMoonClr, clrMoon);
    Initor.setColor(keyMilkyWayClr, clrMilkyWay);

    //itor.setColor(keyBackGndPClr, pclrBackGnd);  Intent. omitted for now...
    Initor.setColor(keyConstPClr, pclrConst);
    Initor.setColor(keyConstBoundPClr, pclrConstBound);
    //itor.setColor(keyStarPClr, pclrStar);  Intentionally omitted for now...
    Initor.setColor(keyStarNamePClr, pclrStarName);
    Initor.setColor(keyDeepSkyPClr, pclrDeepSky);
    Initor.setColor(keyCGridPClr, pclrCGrid);
    Initor.setColor(keyAGridPClr, pclrAGrid);
    Initor.setColor(keyEclipticPClr, pclrEcliptic);
    Initor.setColor(keyHorizonPClr, pclrHorizon);
    Initor.setColor(keySunPClr, pclrSun);
    Initor.setColor(keyPlanetPClr, pclrPlanet);
    Initor.setColor(keyMoonPClr, pclrMoon);
    Initor.setColor(keyMilkyWayPClr, pclrMilkyWay);

    Initor.setFont(keyConstFnt, fntConst);
    Initor.setFont(keyStarNameFnt, fntStarName);
    Initor.setFont(keySolarSysFnt, fntSolarSys);
    Initor.setFont(keyDeepSkyFnt, fntDeepSky);
    Initor.setFont(keyCGridFnt, fntCGrid);
    Initor.setFont(keyAGridFnt, fntAGrid);
    Initor.setFont(keyHorizonFnt, fntHorizon);

    Initor.setBoolean(keyAmPm, ampm);

    if ( nSkySuppress.length() > 0 )
      Initor.setString(keyNSkySuppress, nSkySuppress);

    Initor.setInt(keyZoLimMag10, zoLimMag10);
    Initor.setInt(keyZiLimMag10, ziLimMag10);
    Initor.setInt(keySzBright, szBright);
    Initor.setInt(keySzDim, szDim);
    Initor.setInt(keyZoDSLimMag10, zoDSLimMag10);
    Initor.setInt(keyZiDSLimMag10, ziDSLimMag10);
    Initor.setInt(keyUpdtPer, updatePeriod);

    // Not sure if slewout value should be overwritten.  The original value,
    // if there, may have been altered with a full path when read in.
    // Could let original value stay in the ini file, or write the new
    // value with full path.  Not sure...
    // ( slewout.length() > 0 )
    //itor.setString(keySlew, slewout);  Intentionally omitted for now...
    //itor.setString(keyScope, scopein); Same for scope monitoring...

    return Initor.save();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Saves main window size &amp; position in the ini file.
   * Errors are handled in the Initor class.
   *
   * @return True if successful
   */
  public boolean saveMainWindow(Rectangle r) {
    winLt = (int)r.getX();      winTp = (int)r.getY();
    winWd = (int)r.getWidth();  winHt = (int)r.getHeight();

    Initor.setInt(keyWinLt, winLt);
    Initor.setInt(keyWinTp, winTp);
    Initor.setInt(keyWinWd, winWd);
    Initor.setInt(keyWinHt, winHt);

    return Initor.save();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Saves solar system window size &amp; position in the ini file.
   * Errors are handled in the Initor class.
   *
   * @return True if successful
   */
  public boolean saveSSWindow(Rectangle r) {
    ssLt = (int)r.getX();      ssTp = (int)r.getY();
    ssWd = (int)r.getWidth();  ssHt = (int)r.getHeight();

    Initor.setInt(keySSLt, ssLt);
    Initor.setInt(keySSTp, ssTp);
    Initor.setInt(keySSWd, ssWd);
    Initor.setInt(keySSHt, ssHt);

    return Initor.save();
  }

  public void setConstLinesTBtn(JToggleButton btn)
                 { constLines.setButton(btn); }
  public void setConstNamesTBtn(JToggleButton btn)
                 { constNames.setButton(btn); }
  public void setConstBoundsTBtn(JToggleButton btn)
                 { constBounds.setButton(btn); }
  public void setCGridTBtn(JToggleButton btn) { cGrid.setButton(btn); }
  public void setAGridTBtn(JToggleButton btn) { aGrid.setButton(btn); }
  public void setEclipticTBtn(JToggleButton btn) { ecliptic.setButton(btn); }
  public void setHorizonTBtn(JToggleButton btn) { horizon.setButton(btn); }
  public void setNearSkyTBtn(JToggleButton btn) { nearSky.setButton(btn); }
  public void setDeepSkyTBtn(JToggleButton btn) { deepSky.setButton(btn); }
  public void setDeepSkyNmTBtn(JToggleButton btn) { deepSkyNm.setButton(btn); }
  public void setMilkyWayTBtn(JToggleButton btn) { milkyWay.setButton(btn); }

  public void setCompTimeBtn(JToggleButton btn) { lst.setCompTimeBtn(btn); }

  public boolean drawConstLines() { return constLines.get(); }
  public void    drawConstLines(boolean b) { constLines.set(b); }
  public boolean drawConstNames() { return constNames.get(); }
  public void    drawConstNames(boolean b) { constNames.set(b); }
  public boolean drawConstNFull() { return constNFull; }
  public void    drawConstNFull(boolean b) { constNFull = b; }
  public boolean drawConstBounds() { return constBounds.get(); }
  public void    drawConstBounds(boolean b) { constBounds.set(b); }
  public boolean drawBayer() { return bayer; }
  public void    drawBayer(boolean b) { bayer = b; }
  public boolean drawFlamsteed() { return flamsteed; }
  public void    drawFlamsteed(boolean b) { flamsteed = b; }
  public boolean drawStarNames() { return starNames; }
  public void    drawStarNames(boolean b) { starNames = b; }
  public boolean drawCGrid() { return cGrid.get(); }
  public void    drawCGrid(boolean b) { cGrid.set(b); }
  public boolean drawCGridLabels() { return cGridLabels; }
  public void    drawCGridLabels(boolean b) { cGridLabels = b; }
  public boolean drawAGrid() { return aGrid.get(); }
  public void    drawAGrid(boolean b) { aGrid.set(b); }
  public boolean drawAGridLabels() { return aGridLabels; }
  public void    drawAGridLabels(boolean b) { aGridLabels = b; }
  public boolean drawEcliptic() { return ecliptic.get(); }
  public void    drawEcliptic(boolean b) { ecliptic.set(b); }
  public boolean drawHorizon() { return horizon.get(); }
  public void    drawHorizon(boolean b) { horizon.set(b); }
  public boolean drawNearSky() { return nearSky.get(); }
  public void    drawNearSky(boolean b) { nearSky.set(b); }
  public boolean drawDeepSky() { return deepSky.get(); }
  public void    drawDeepSky(boolean b) { deepSky.set(b); }
  public boolean drawDeepSkyNames() { return deepSkyNm.get(); }
  public void    drawDeepSkyNames(boolean b) { deepSkyNm.set(b); }
  public boolean drawMilkyWay() { return milkyWay.get(); }
  public void    drawMilkyWay(boolean b) { milkyWay.set(b); }

  public boolean showToolBar() { return toolBar; }
  public void    showToolBar(boolean s) { toolBar = s; }
  public boolean showWinInfo() { return winInfo; }
  public void    showWinInfo(boolean s) { winInfo = s; }
  public boolean showScrlAzAlt() { return scrlAzAlt; }
  public void    showScrlAzAlt(boolean s) { scrlAzAlt = s; }
  public boolean showScrlZoom() { return scrlZoom; }
  public void    showScrlZoom(boolean s) { scrlZoom = s; }
  public boolean showScrlField() { return scrlField; }
  public void    showScrlField(boolean s) { scrlField = s; }

  public Color   colorBackGnd() { return clrBackGnd; }
  public void    colorBackGnd(Color c) { clrBackGnd = c; }
  //blic Color   colorForeGnd() { return clrForeGnd; }
  //blic void    colorForeGnd(Color c) { clrForeGnd = c; }
  public Color   colorZoomBox() { return new Color(204, 204, 204); }
  public Color   prclrBackGnd() { return pclrBackGnd; }
  public void    prclrBackGnd(Color c) { pclrBackGnd = c; }

  public Color   colorConst() { return clrConst; }
  public void    colorConst(Color c) { clrConst = c; }
  public Color   colorConstBound() { return clrConstBound; }
  public void    colorConstBound(Color c) { clrConstBound = c; }
  public Color   colorStar() { return clrStar; }
  public void    colorStar(Color c) { clrStar = c; }
  public Color   colorStarName() { return clrStarName; }
  public void    colorStarName(Color c) { clrStarName = c; }
  public Color   colorDeepSky() { return clrDeepSky; }
  public void    colorDeepSky(Color c) { clrDeepSky = c; }
  public Color   colorCGrid() { return clrCGrid; }
  public void    colorCGrid(Color c) { clrCGrid = c; }
  public Color   colorAGrid() { return clrAGrid; }
  public void    colorAGrid(Color c) { clrAGrid = c; }
  public Color   colorEcliptic() { return clrEcliptic; }
  public void    colorEcliptic(Color c) { clrEcliptic = c; }
  public Color   colorHorizon() { return clrHorizon; }
  public void    colorHorizon(Color c) { clrHorizon = c; }
  public Color   colorSun() { return clrSun; }
  public void    colorSun(Color c) { clrSun = c; }
  public Color   colorPlanet() { return clrPlanet; }
  public void    colorPlanet(Color c) { clrPlanet = c; }
  public Color   colorMoon() { return clrMoon; }
  public void    colorMoon(Color c) { clrMoon = c; }
  public Color   colorMilkyWay() { return clrMilkyWay; }
  public void    colorMilkyWay(Color c) { clrMilkyWay = c; }

  public Color   prclrConst() { return pclrConst; }
  public void    prclrConst(Color c) { pclrConst = c; }
  public Color   prclrConstBound() { return pclrConstBound; }
  public void    prclrConstBound(Color c) { pclrConstBound = c; }
  public Color   prclrStar() { return pclrStar; }
  public void    prclrStar(Color c) { pclrStar = c; }
  public Color   prclrStarName() { return pclrStarName; }
  public void    prclrStarName(Color c) { pclrStarName = c; }
  public Color   prclrDeepSky() { return pclrDeepSky; }
  public void    prclrDeepSky(Color c) { pclrDeepSky = c; }
  public Color   prclrCGrid() { return pclrCGrid; }
  public void    prclrCGrid(Color c) { pclrCGrid = c; }
  public Color   prclrAGrid() { return pclrAGrid; }
  public void    prclrAGrid(Color c) { pclrAGrid = c; }
  public Color   prclrEcliptic() { return pclrEcliptic; }
  public void    prclrEcliptic(Color c) { pclrEcliptic = c; }
  public Color   prclrHorizon() { return pclrHorizon; }
  public void    prclrHorizon(Color c) { pclrHorizon = c; }
  public Color   prclrSun() { return pclrSun; }
  public void    prclrSun(Color c) { pclrSun = c; }
  public Color   prclrPlanet() { return pclrPlanet; }
  public void    prclrPlanet(Color c) { pclrPlanet = c; }
  public Color   prclrMoon() { return pclrMoon; }
  public void    prclrMoon(Color c) { pclrMoon = c; }
  public Color   prclrMilkyWay() { return pclrMilkyWay; }
  public void    prclrMilkyWay(Color c) { pclrMilkyWay = c; }

  public Font    fontConst() { return fntConst; }
  public void    fontConst(Font f) { fntConst = f; }
  public Font    fontStarName() { return fntStarName; }
  public void    fontStarName(Font f) { fntStarName = f; }
  public Font    fontSolarSys() { return fntSolarSys; }
  public void    fontSolarSys(Font f) { fntSolarSys = f; }
  public Font    fontDeepSky() { return fntDeepSky; }
  public void    fontDeepSky(Font f) { fntDeepSky = f; }
  public Font    fontHorizon() { return fntHorizon; }
  public void    fontHorizon(Font f) { fntHorizon = f; }
  public Font    fontCGrid() { return fntCGrid; }
  public void    fontCGrid(Font f) { fntCGrid = f; }
  public Font    fontAGrid() { return fntAGrid; }
  public void    fontAGrid(Font f) { fntAGrid = f; }

  static public String SlewOut() { return slewout; }
  static public String ScopeIn() { return scopein; }

  /* StarLabel is for Bayer and Flamsteed star labels */
  public Color   colorStarLabel() { return clrStarName; }
  public Color   prclrStarLabel() { return pclrStarName; }
  public Font    fontStarLabel() {
    if ( fntStarLabel == null ||
         fntStarLabel.getSize() != fntStarName.getSize() )
      fntStarLabel = new Font("Dialog", Font.PLAIN, fntStarName.getSize());
    return fntStarLabel;
  }

  public boolean is24Hr() { return !ampm; }
  public void set24Hr(boolean h) { ampm = !h; }
  public int  getUpdatePeriod() { return updatePeriod; }
  public void setUpdatePeriod(int u) {
    updatePeriod = u;
  }
  public int  getTimeSpeed() { return lst.getTimeSpeed(); }
  public void setTimeSpeed(int s) {
    if ( s >  MAXTIMEFACTOR ) s =  MAXTIMEFACTOR;
    if ( s < -MAXTIMEFACTOR ) s = -MAXTIMEFACTOR;
    lst.setTimeSpeed(s);
  }
  public boolean hasIni() { return Initor.hasIni(); }
  public int getZoLimMag10() { return zoLimMag10; }
  public void setZoLimMag10(int m) {
    if      ( m < MINMAG10 ) m = MINMAG10;
    else if ( m > MAXMAG10 ) m = MAXMAG10;
    zoLimMag10 = m;
  }
  public int getZiLimMag10() { return ziLimMag10; }
  public void setZiLimMag10(int m) {
    if      ( m < MINMAG10 ) m = MINMAG10;
    else if ( m > MAXMAG10 ) m = MAXMAG10;
    ziLimMag10 = m;
  }
  public int getSzBright() { return szBright; }
  public int getSzDim() { return szDim; }
  public void setSzStar(int bright, int dim) {
    /* 1 <= szDim <= szBright <= NUMIMAGES */
    szDim    = Math.max(1,     Math.min(dim,    StarImages.NUMIMAGES));
    szBright = Math.max(szDim, Math.min(bright, StarImages.NUMIMAGES));
  }
  public boolean getBmpStars() { return bmpStars; }
  public int getZoDSLimMag10() { return zoDSLimMag10; }
  public void setZoDSLimMag10(int m) {
    if      ( m < MINMAG10 ) m = MINMAG10;
    else if ( m > MAXMAG10 ) m = MAXMAG10;
    zoDSLimMag10 = m;
  }
  public int getZiDSLimMag10() { return ziDSLimMag10; }
  public void setZiDSLimMag10(int m) {
    if      ( m < MINMAG10 ) m = MINMAG10;
    else if ( m > MAXMAG10 ) m = MAXMAG10;
    ziDSLimMag10 = m;
  }
  public Rectangle getMainWindow() {
    // Sanity checking will be done by calling function
    return new Rectangle(winLt, winTp, winWd, winHt);
  }
  public Rectangle getSSWindow() {
    // Sanity checking will be done by calling function
    return new Rectangle(ssLt, ssTp, ssWd, ssHt);
  }

  /* Mouse button stuff */
  private static int selectMask = MouseEvent.BUTTON1_MASK;  // Left Btn
  private static int popupMask  = MouseEvent.BUTTON3_MASK;  // Right Btn
  public int selectBtn() { return selectMask; }
  public int popupBtn()  { return popupMask; }
  public int popupEvent() { return MouseEvent.MOUSE_RELEASED;/* or _PRESSED */ }
  public void swapMouse() {
    selectMask = MouseEvent.BUTTON3_MASK;  // Right Btn
    popupMask  = MouseEvent.BUTTON1_MASK;  // Left Btn
  }

  /* * *   -------------   Override of View superclass   -------------   * * */

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the current field rotation.
   */
  public int getFld() {
    if ( ! showScrlField() ) return 0;
    else                     return super.getFld();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the fld value and the Field Rotation scrollbar.
   */
  public boolean setFld(int deg) {
    if ( ! showScrlField() ) return false;
    else                     return super.setFld(deg);
  }

  /* * *   ----------     Accessors to LST information &    ----------   * * */
  /* * *   ----------   Accessors to Location information   ----------   * * */

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Used only by DateTimeDlg.
   */
  public GregorianCalendar getLocDateTime() { return lst.getLocDateTime(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Used only by DateTimeDlg.
   */
  public void setLocDateTime() { lst.setLocDateTime(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Used only by DateTimeDlg.
   */
  public void setToCompDateTime() { lst.setToCompDateTime(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the location associated with the LST.
   */
  public void setLocation(Location loc, String dst) {
    lst.setLocation(loc, dst);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the location associated with the LST.
   */
  public Location getLocation() {
    return lst.getLocation();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get DST info.
   *
   * @return A  -  auto DST active
   * <br>    0  -  DST is off (no auto DST)
   * <br>    1  -  DST is on  (no auto DST)
   */
  public String getDST() { return lst.getDST(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Indicates whether DST is in effect.
   */
  public boolean inDST() { return lst.inDST(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city string, or if not set, the latitude and longitude.
   */
  public String tellLocation() { return lst.tellLocation(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the city string (or "" if not set).
   */
  public String tellCity() { return lst.tellCity(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location in degrees.
   */
  public double getLongDeg() { return lst.getLongDeg(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the longitude of this location as a string ([-][DD]D:MM).
   */
  public String tellLong() { return lst.tellLong(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location in degrees.
   */
  public double getLatDeg() { return lst.getLatDeg(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the latitude of this location as a string ([-][D]D:MM).
   */
  public String tellLat() { return lst.tellLat(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich in minutes.
   * It is negative for west of Greenwich, and ignores DST.
   */
  public double getTZOffsetMin() { return lst.getTZOffsetMin(); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the time zone offset relative to Greenwich (as [-][H]H:MM).
   * It is negative for west of Greenwich, and ignores DST.
   */
  public String tellTZOffset() { return lst.tellTZOffset(); }
}

