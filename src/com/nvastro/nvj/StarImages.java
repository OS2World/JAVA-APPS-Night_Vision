/*
 * StarImages.java  -  Creates the set of star images
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


/*------------------------------------------------------------------------------

Star sizes (images)
-------------------

                        -                =               -=-
 0     =          1    -=-         2    ===         3    ===
                        -                =               -=-


                        -                =                =-
      ===              ===              ===             -===
 4    ===         5   -===-        6   =====        7   =====
      ===              ===              ===              ===-
                        -                =               -=

      -=-              -==              ===
     -===-            ====-            =====
 8   =====        9   =====       10   =====
     -===-            -====            =====
      -=-              ==-              ===

------------------------------------------------------------------------------*/


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Creates the set of star images.
 *
 * @author Brian Simpson
 */
public class StarImages {
  /** Number of images */
  public final static int NUMIMAGES = 11;
  /** Width and height of each image */
  public final static int WH = 5;
  /** x and y distance from corner to center */
  public final static int OFFSET = 2;
  private BufferedImage[] stars;
  private Color color = null, bkclr = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor (does nothing).
   */
  public StarImages() {
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns the collection of deep sky images.
   *
   * @param c Color of images
   */
  public BufferedImage[] getImages(Color c, Color b) {
    if ( color == null || !color.equals(c) ||
         bkclr == null || !bkclr.equals(b) ) {
      /* See DeepSkyImages for method to madness */
      color = c;
      bkclr = b;
      Color dim = new Color((c.getRed()*128   + b.getRed()*127)/255,
                            (c.getGreen()*128 + b.getGreen()*127)/255,
                            (c.getBlue()*128  + b.getBlue()*127)/255);

      stars = new BufferedImage[NUMIMAGES];
      for ( int i = 0; i < NUMIMAGES; i++ ) {
        stars[i] = LocalGraphics.getBufferedImage(WH, WH);
        Graphics2D g = stars[i].createGraphics();
        g.setColor(dim);
        switch ( i ) {
          case 1:
            g.drawLine(2, 1, 2, 3); g.drawLine(1, 2, 3, 2);
            break;
          case 3:
            g.drawLine(1, 1, 3, 1); g.drawLine(1, 3, 3, 3);
            break;
          case 5:
            g.drawLine(2, 0, 2, 4); g.drawLine(0, 2, 4, 2);
            break;
          case 8:
          case 9:
            g.drawLine(1, 0, 3, 4); g.drawLine(0, 3, 4, 1);
            // no break here
          case 7:
            g.drawLine(3, 0, 1, 4); g.drawLine(0, 1, 4, 3);
            break;
        }
        g.setColor(color);
        switch ( i ) {
          case 0:
          case 1:
            g.drawLine(2, 2, 2, 2);   // Center point
            break;
          case 2:
          case 3:
            g.drawLine(2, 1, 2, 3); g.drawLine(1, 2, 3, 2);
            break;
          case 9:
            g.drawLine(3, 0, 1, 4); g.drawLine(0, 1, 4, 3);
            // no break here
          case 6:
          case 7:
          case 8:
            g.drawLine(2, 0, 2, 4); g.drawLine(0, 2, 4, 2);
            // no break here
          case 4:
          case 5:
            g.fillRect(1, 1, 3, 3);   // x, y, w, h
            break;
          case 10:
            g.fillRect(0, 1, 5, 3); g.fillRect(1, 0, 3, 5);
            break;
        }
        g.dispose();
      }
    }
    return stars;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  final StarImages si = new StarImages();
  //  si.getImages(Color.white);
  //
  //  JFrame frame = new JFrame("Stars");
  //
  //  JPanel allstars = new JPanel() {
  //    protected void paintComponent(Graphics g) {
  //      //super.paintComponent(g);
  //
  //      g.setColor(Color.black);
  //      g.fillRect(0, 0, getWidth(), getHeight());
  //
  //      int start = 30;
  //      int r = start, c = start;
  //      for ( int j = 0; j < NUMIMAGES; j++ ) {
  //        g.drawImage(si.stars[j], c, r, WH * 8, WH * 8, null);
  //        c += 100;
  //        if ( c > 400 ) {
  //          c = start;
  //          r += 100;
  //        }
  //      }
  //    }
  //  };
  //  allstars.setPreferredSize(new Dimension(400, 300)); // width, height
  //
  //  frame.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //
  //  frame.getContentPane().add(allstars);
  //  frame.pack();
  //  frame.show();
  //}
}

