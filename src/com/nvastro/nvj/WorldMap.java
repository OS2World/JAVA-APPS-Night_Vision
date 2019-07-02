/*
 * WorldMap.java  -  World map component
 * Copyright (C) 2016-2017 Brian Simpson
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * The following class describes the world map component
 * used by the LocationDlg class.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class WorldMap extends JPanel {
  private Image image;
  private BufferedImage bimage;
  int width, height;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public WorldMap() {
    image = Nvj.getImageIcon("/com/nvastro/nvj/worldmap.jpg").getImage();
    bimage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                               BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = bimage.createGraphics();
    g2.drawImage(image, null, null);
    g2.dispose();
    //stem.out.println("width = " + bimage.getWidth());
    width = bimage.getWidth();
    height = bimage.getHeight();
    Dimension dim = new Dimension(width, height);
    setPreferredSize(dim);
    setMinimumSize(dim);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        toWorldCoords(x, y);

        WorldMap.this.drawImage(x, y);
      }
    });
    setFocusable(false); // Seems unneeded
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(bimage, 0, 0, null);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws the image, with or without cross-hairs.
   */
  private void drawImage(int x, int y) {
    Graphics2D g2 = bimage.createGraphics();
    g2.drawImage(image, null, null);
    if ( x >= 0 && y >= 0 ) {
      g2.setPaint(Color.black);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setStroke(new BasicStroke(1.5f));
      g2.drawLine(x - 6, y, x - 2, y);
      g2.drawLine(x + 2, y, x + 6, y);
      g2.drawLine(x, y - 6, x, y - 2);
      g2.drawLine(x, y + 2, x, y + 6);
    }
    g2.dispose();
    repaint();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by mouse adapter to inform LocationDlg of new cross-hairs location.
   */
  private void toWorldCoords(int x, int y) {
    double lat, lng;

    // Adding 0.5 puts location in center of pixel
    lat = (1 - 2 * (y + 0.5) / height) * 90;
    lng = (2 * (x + 0.5) / width - 1) * 180;

    lat = Math.max(-90,  Math.min(lat, 90));
    lng = Math.max(-180, Math.min(lng, 180));

    //stem.out.println("Clicked at: lat = " + lat + ", lng = " + lng);

    Container parent = this;
    while ((parent = parent.getParent()) != null)
      if (parent instanceof LocationDlg) {
        ((LocationDlg)parent).wmChanged(lat, lng);
        break;
      }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by LocationDlg to set or remove the cross-hairs.
   */
  public void setWmLocation(double lat, double lng) {
    int x, y;

    //stem.out.println("setWmLocation called with " + lat + ", " + lng);
    if ( lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 ) {
      x = (int)((180 + lng) / 360 * width);
      y = (int)((90  - lat) / 180 * height);

      x = Math.max(0, Math.min(x, width  - 1));
      y = Math.max(0, Math.min(y, height - 1));

      drawImage(x, y);
    }
    else
      drawImage(-1, -1);  // Draw image with no cross-hairs
  }

  /* For testing */
  //public static void main(String[] args) {
  //  JFrame frame = new JFrame("WorldMap");
  //  Container cp = frame.getContentPane();
  //  WorldMap wm = new WorldMap();
  //  cp.add(wm);
  //
  //  frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  //  frame.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //
  //  frame.pack();
  //  frame.setVisible(true);
  //  wm.setWmLocation(40.0, -105.0);
  //}
}

