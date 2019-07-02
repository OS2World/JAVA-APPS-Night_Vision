/*
 * LocalGraphics.java  -  Static graphics methods for local environment
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

import java.awt.*;
import java.awt.image.*;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Static graphics methods for local environment.
 * getBufferedImage and getFontFamilyNames.
 *
 * @author Brian Simpson
 */
public class LocalGraphics {
  private final static GraphicsEnvironment ge =
                       GraphicsEnvironment.getLocalGraphicsEnvironment();
  private final static GraphicsDevice gd =
                       ge.getDefaultScreenDevice();
  private final static GraphicsConfiguration gc =
                       gd.getDefaultConfiguration();
  private final static String[] fontfamilynames =
                       ge.getAvailableFontFamilyNames();

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * No constructor available.
   */
  private LocalGraphics() {}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets a BufferedImage of the specified width and height.
   *
   * @param w Width
   * @param h Height
   */
  public static BufferedImage getBufferedImage(int w, int h) {
    return gc.createCompatibleImage(w, h, Transparency.BITMASK);
    // There is also Transparency.TRANSLUCENT
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns an array of font family names
   */
  public static String[] getFontFamilyNames() {
    return fontfamilynames;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  GraphicsConfiguration[] configurations = gd.getConfigurations();
  //
  //  System.out.println("Default screen device: " + gd.getIDstring());
  //  for ( int i = 0; i < configurations.length; i++ ) {
  //    System.out.println("  Configuration " + (i + 1));
  //    System.out.println("  " + configurations[i].getColorModel());
  //  }
  //
  //  System.out.println("\nCompare to default configuration");
  //  System.out.println("  " + gc.getColorModel());
  //
  //  System.out.println("");
  //  String[] FontFamNames = getFontFamilyNames();
  //  for ( int i = 0; i < FontFamNames.length; i++ )
  //    System.out.println("Font Family Name " + i + " = " + FontFamNames[i]);
  //}
}

