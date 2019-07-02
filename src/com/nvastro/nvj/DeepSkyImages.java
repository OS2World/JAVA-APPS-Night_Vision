/*
 * DeepSkyImages.java  -  Creates the set of deep sky images
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Creates the set of deep sky images.
 *
 * @author Brian Simpson
 */
public class DeepSkyImages {
  /** Number of images */
  public final static int NUMIMAGES = 11;
  /** Width and height of each image */
  public final static int WH = 11;
  /** x and y distance from corner to center */
  public final static int OFFSET = 5;
  private BufferedImage[] dsobjects;
  private Color color = null, bkclr = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor (does nothing).
   */
  public DeepSkyImages() {
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the collection of deep sky images.
   *
   * @param c Color of images
   */
  public BufferedImage[] getImages(Color c, Color b) {
    if ( color == null || !color.equals(c) ||
         bkclr == null || !bkclr.equals(b) ) {
      /* Color codes for diagrams
         "=" = Color(204, 204, 204)  (brighter)
         "-" = Color(128, 128, 128)  (dimmer)
         Note: 128/204 = 160/255  (Method to madness below) */
      color = c;
      bkclr = b;
      Color dim = new Color((c.getRed()*160   + b.getRed()*95)/255,
                            (c.getGreen()*160 + b.getGreen()*95)/255,
                            (c.getBlue()*160  + b.getBlue()*95)/255);

      dsobjects = new BufferedImage[NUMIMAGES];
      for ( int i = 0; i < NUMIMAGES; i++ ) {
        dsobjects[i] = LocalGraphics.getBufferedImage(WH, WH);
        Graphics2D g = dsobjects[i].createGraphics();
        /* The following order must match that of DeepSkyDB.java.
           (Perhaps it should be moved here permanently)
           static String[] types = { "OB", "PN", "DN", "DK", "GC", "OC",
                                     "S2", "SG", "EG", "IG", "GA" }; */
        switch ( i ) {
          case 0:                 /* Generic object */
            /*  -------------
                |           |
                |     =     |
                |  =     =  |
                |    = =    |
                |           |
            OB  | = = = = = |
                |           |
                |    = =    |
                |  =     =  |
                |     =     |
                |           |
                ------------- */
            g.setColor(color);
            drawPoint(g, 5, 1);
            drawPoint(g, 2, 2); drawPoint(g, 8, 2);
            drawPoint(g, 4, 3); drawPoint(g, 6, 3);
            drawPoint(g, 1, 5); drawPoint(g, 3, 5); drawPoint(g, 5, 5);
            drawPoint(g, 7, 5); drawPoint(g, 9, 5);
            drawPoint(g, 4, 7); drawPoint(g, 6, 7);
            drawPoint(g, 2, 8); drawPoint(g, 8, 8);
            drawPoint(g, 5, 9);
            break;
          case 1:                 /* Planetary nebula */
            /*  -------------
                |     =     |
                |     =     |
                |    ===    |
                |   =- -=   |
                |  =-   -=  |
            PN  |===     ===|
                |  =-   -=  |
                |   =- -=   |
                |    ===    |
                |     =     |
                |     =     |
                ------------- */
            g.setColor(dim);
            g.drawLine(3, 4, 4, 3); g.drawLine(3, 6, 4, 7);
            g.drawLine(6, 3, 7, 4); g.drawLine(7, 6, 6, 7);
            g.setColor(color);
            g.drawLine(5, 1, 1, 5); g.drawLine(1, 5, 5, 9);
            g.drawLine(5, 1, 9, 5); g.drawLine(9, 5, 5, 9);
            g.drawLine(0, 5, 2, 5); g.drawLine(8, 5, 10, 5);
            g.drawLine(5, 0, 5, 2); g.drawLine(5, 8, 5, 10);
            break;
          case 2:                 /* Diffuse nebula */
            /*  -------------
                |           |
                | ========= |
                | =       = |
                | =       = |
                | =       = |
            DN  | =       = |
                | =       = |
                | =       = |
                | =       = |
                | ========= |
                |           |
                ------------- */
            g.setColor(color);
            g.drawRect(1, 1, 9, 9);     // x, y, w, h
            break;
          case 3:                 /* Dark nebula */
            /*  -------------
                |           |
                | =       = |
                |  =     =  |
                |   =   =   |
                |    = =    |
            DK  |     =     |
                |    = =    |
                |   =   =   |
                |  =     =  |
                | =       = |
                |           |
                ------------- */
            g.setColor(color);
            g.drawLine(1, 1, 9, 9); g.drawLine(1, 9, 9, 1);
            break;
          case 4:                 /* Globular cluster */
            /*  -------------
                |           |
                |   =====   |
                |  =- = -=  |
                | =-  =  -= |
                | =   =   = |
            GC  | ========= |
                | =   =   = |
                | =-  =  -= |
                |  =- = -=  |
                |   =====   |
                |           |
                ------------- */
            g.setColor(dim);
            g.drawLine(2, 3, 3, 2); g.drawLine(7, 2, 8, 3);
            g.drawLine(2, 7, 3, 8); g.drawLine(7, 8, 8, 7);
            g.setColor(color);
            g.drawLine(1, 3, 3, 1); g.drawLine(7, 1, 9, 3);
            g.drawLine(1, 7, 3, 9); g.drawLine(7, 9, 9, 7);
            g.drawLine(1, 4, 1, 6); g.drawLine(9, 4, 9, 6);
            g.drawLine(4, 1, 6, 1); g.drawLine(4, 9, 6, 9);
            g.drawLine(1, 5, 9, 5); g.drawLine(5, 1, 5, 9);
            break;
          case 5:                 /* Open cluster */
            /*  -------------
                |     =     |
                | -= -=- =- |
                | ==  -  == |
                |           |
                | -       - |
            OC  |==-     -==|
                | -       - |
                |           |
                | ==  -  == |
                | -= -=- =- |
                |     =     |
                ------------- */
            g.setColor(color);
            g.fillRect(1, 1, 2, 2); g.fillRect(8, 1, 2, 2);
            g.fillRect(1, 8, 2, 2); g.fillRect(8, 8, 2, 2);
            g.drawLine(5, 0, 5, 1); g.drawLine(5, 9, 5, 10);
            g.drawLine(0, 5, 1, 5); g.drawLine(9, 5, 10, 5);
            g.setColor(dim);
            drawPoint(g, 1, 1); drawPoint(g, 9, 1);
            drawPoint(g, 4, 1); drawPoint(g, 5, 2); drawPoint(g, 6, 1);
            drawPoint(g, 1, 4); drawPoint(g, 2, 5); drawPoint(g, 1, 6);
            drawPoint(g, 9, 4); drawPoint(g, 8, 5); drawPoint(g, 9, 6);
            drawPoint(g, 1, 9); drawPoint(g, 9, 9);
            drawPoint(g, 4, 9); drawPoint(g, 5, 8); drawPoint(g, 6, 9);
            break;
          case 6:                 /* Double star */
            /*  -------------
                |           |
                |           |
                |           |
                |   =   =   |
                |    = =    |
            S2  |     =     |
                |    = =    |
                |   =   =   |
                |           |
                |           |
                |           |
                ------------- */
            g.setColor(color);
            g.drawLine(3, 3, 7, 7); g.drawLine(3, 7, 7, 3);
            break;
          case 7:                 /* Spiral galaxy */
          case 8:                 /* Elliptical galaxy */
          case 9:                 /* Irregular galaxy */
          case 10:                /* Generic galaxy */
            /*  -------------
                |           |
                |     -     |
                |  -=====-  |
                | ==     == |
                |=-       -=|
            GA  |=         =|
                |=-       -=|
                | ==     == |
                |  -=====-  |
                |     -     |
                |           |
                ------------- */
            g.setColor(dim);
            drawPoint(g, 5, 1); drawPoint(g, 5, 9);
            drawPoint(g, 1, 4); drawPoint(g, 9, 4);
            drawPoint(g, 1, 6); drawPoint(g, 9, 6);
            g.drawLine(2, 2, 8, 2); g.drawLine(2, 8, 8, 8);
            g.setColor(color);
            //g.draw(new Ellipse2D.Double(0.0, 1.0, 10.0, 6.0));
            g.drawLine(3, 2, 7, 2); g.drawLine(3, 8, 7, 8);
            g.drawLine(1, 3, 2, 3); g.drawLine(8, 3, 9, 3);
            g.drawLine(1, 7, 2, 7); g.drawLine(8, 7, 9, 7);
            g.drawLine(0, 4, 0, 6); g.drawLine(10, 4, 10, 6);
            break;
        }
        g.dispose();
      }
    }
    return dsobjects;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draw a point.
   */
  private final static void drawPoint(Graphics2D g, int x, int y) {
    g.drawLine(x, y, x, y);
  }

  /* For testing */
  //public static void main(String[] args) {
  //  final DeepSkyImages ds = new DeepSkyImages();
  //  ds.getImages(new Color(204,204,204));
  //
  //  JFrame frame = new JFrame("DeepSky objects");
  //
  //  JPanel allobjects = new JPanel() {
  //    protected void paintComponent(Graphics g) {
  //      //super.paintComponent(g);
  //
  //      g.setColor(Color.black);
  //      g.fillRect(0, 0, getWidth(), getHeight());
  //
  //      int start = 30;
  //      int r = start, c = start;
  //      for ( int j = 0; j < NUMIMAGES; j++ ) {
  //        g.drawImage(ds.dsobjects[j], c, r, WH * 8, WH * 8, null);
  //        g.drawImage(ds.dsobjects[j], c-10, r-10, null);
  //        c += 100;
  //        if ( c > 400 ) {
  //          c = start;
  //          r += 100;
  //        }
  //      }
  //    }
  //  };
  //  allobjects.setPreferredSize(new Dimension(440, 320)); // width, height
  //
  //  frame.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //
  //  frame.getContentPane().add(allobjects);
  //  frame.pack();
  //  frame.show();
  //}
}

/*------------------------------------------------------------------------------

Method for "dim" color
----------------------

Deep sky objects are two colors, bright and dim.  Bright is the principal
color, while dim is used to anti-alias (soften) the edges.  Range is from
0 (black) to 255 (white).

Originally deep sky objects had a brightness of 204, with dim edges of 128,
against a black background (0).  When color at various brightnesses was added,
dim was calculated as bright*128/204, = bright*160/255.

When using a white background, this formula changes to
bright*160/255 + 95/255

Against any background, use (bright*160 + background*95)/255

Not sure where original 204 and 128 came from...

------------------------------------------------------------------------------*/

