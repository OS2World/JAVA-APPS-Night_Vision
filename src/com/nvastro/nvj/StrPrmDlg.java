/*
 * StrPrmDlg.java  -  "Set star parameters" dialog
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

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
// Ellipse2D


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Set star parameters" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class StrPrmDlg extends EscapeDlg implements ChangeListener {
  private JLabel zolbl, zilbl;
  private JSlider zolimmag, zilimmag;
  private JSlider dim, bright;
  private int minmag10, maxmag10; // Min & max limits for "Limiting magnitude"
  private ActionListener listeners = null;
  private Preferences prefer;
  static private StrPrmDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new StrPrmDlg(owner, prefer);
    else {
      dlg.loadPrefer();
    }

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   */
  private StrPrmDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("StrPrmDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          StrPrmDlg.this.close();     // Pop down dialog
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(StrPrmDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Create controls for this window */
    { StarDB stardb = new StarDB(); maxmag10 = stardb.getMaxMag100() / 10; }
    if ( maxmag10 > Preferences.MAXMAG10 ) maxmag10 = Preferences.MAXMAG10;
    minmag10 = Preferences.MINMAG10;        // Probably 20 (represents 2.0)
    if ( maxmag10 < minmag10 ) maxmag10 = minmag10; // Just in case star DB
                                                    // has no stars >= 2
    zolbl = new JLabel("99.9"); // Use large # for setting min size
    zilbl = new JLabel("99.9"); // Use large # for setting min size
    zolimmag = new JSlider(minmag10, maxmag10, minmag10);
    zilimmag = new JSlider(minmag10, maxmag10, maxmag10);
    zolimmag.setMajorTickSpacing(1);
    zilimmag.setMajorTickSpacing(1);
    zolimmag.setSnapToTicks(true);
    zilimmag.setSnapToTicks(true);
    zolimmag.setPaintTicks(false);
    zilimmag.setPaintTicks(false);
    zolimmag.addChangeListener(this);
    zilimmag.addChangeListener(this);
    JLabel br = new JLabel(TextBndl.getString("StrPrmDlg.Bright"));
    JLabel dm = new JLabel(TextBndl.getString("StrPrmDlg.Dim"));
    bright = new JSlider(1, StarImages.NUMIMAGES, 1);
    dim    = new JSlider(1, StarImages.NUMIMAGES, 1);
    bright.setInverted(true);  // Put highest values on left side
    dim.setInverted(true);     // Put highest values on left side
    bright.setMajorTickSpacing(1);
    dim.setMajorTickSpacing(1);
    bright.setSnapToTicks(true);
    dim.setSnapToTicks(true);
    bright.setPaintTicks(false);
    dim.setPaintTicks(false);
    bright.addChangeListener(this);
    dim.addChangeListener(this);
    Stars stars = new Stars(prefer);
    loadPrefer();  // Sets JSliders

    /* Create a constraints object to control placement
       and set some defaults */
    GridBagConstraints c = new GridBagConstraints();
    // Dft c.anchor = GridBagConstraints.CENTER;
    // Dft c.gridwidth = c.gridheight = 1;
    // Dft c.fill = GridBagConstraints.NONE;
    // Dft c.insets = new Insets(0, 0, 0, 0);
    //----------
    JPanel m = new JPanel(new GridBagLayout());
    c.gridx = 0; c.gridy = 0;
    c.insets = new Insets(0, 7, 6, 7); // T, L, B, R
    c.gridwidth = 2;
    m.add(new JLabel(TextBndl.getString("StrPrmDlg.ZoMag")), c);
    c.gridwidth = 1;
    //-----
    c.gridx = 0; c.gridy = 1;
    m.add(zolbl, c);
    //-----
    c.gridx = 1; c.gridy = 1;
    m.add(zolimmag, c);
    //-----
    c.gridx = 0; c.gridy = 2;
    c.gridwidth = 2;
    m.add(new JLabel(TextBndl.getString("StrPrmDlg.ZiMag")), c);
    c.gridwidth = 1;
    //-----
    c.gridx = 0; c.gridy = 3;
    m.add(zilbl, c);
    //-----
    c.gridx = 1; c.gridy = 3;
    m.add(zilimmag, c);
    //----- (Attempt to keep 1st column width constant)
    c.insets = new Insets(0, 0, 0, 0);  // T, L, B, R
    c.gridx = 0; c.gridy = 4;
    Dimension dspacer = zolbl.getPreferredSize();
    m.add(Box.createHorizontalStrut(dspacer.width + 25), c);
    //-----
    JPanel p = new JPanel(new GridBagLayout());
    c.insets = new Insets(0, 0, 6, 14); // T, L, B, R
    c.gridx = 0; c.gridy = 4;
    p.add(br, c);
    //-----
    c.insets = new Insets(6, 0, 0, 14); // T, L, B, R
    c.gridx = 0; c.gridy = 6;
    p.add(dm, c);
    //-----
    c.insets = new Insets(0, 0, 6, 0); // T, L, B, R
    c.anchor = GridBagConstraints.EAST;
    c.gridx = 1; c.gridy = 4;
    p.add(bright, c);
    //-----
    c.insets = new Insets(6, 0, 0, 0); // T, L, B, R
    c.gridx = 1; c.gridy = 6;
    p.add(dim, c);
    //-----
    c.insets = new Insets(0, 0, 0, 0); // T, L, B, R
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1; c.gridy = 5;
    p.add(stars, c);

    /* Create some buttons */
    JButton OK = new JButton(TextBndl.getString("Dlg.OK"));
    OK.setActionCommand("OK");
    OK.addActionListener(listener);
    JButton Apply = new JButton(TextBndl.getString("Dlg.Apply"));
    Apply.setActionCommand("Apply");
    Apply.addActionListener(listener);
    JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
    Cancel.setActionCommand("Cancel");
    Cancel.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "setstar");
    setHelpPage("setstar");

    /* Create a box and add buttons for OK, Apply, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(OK);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Apply);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Cancel);
    b.add(Box.createHorizontalStrut(7));
    b.add(Box.createHorizontalGlue());
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(m, BorderLayout.NORTH);
    getContentPane().add(p);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(zolimmag);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ChangeListener interface.
   */
  public void stateChanged(ChangeEvent e) {
    JSlider source = (JSlider)(e.getSource());

    if ( source == bright || source == dim ) {
      int brightSize = bright.getValue();
      int dimSize    = dim.getValue();

      if ( dimSize > brightSize ) {
        if ( source == bright ) dim.setValue(brightSize);
        else                    bright.setValue(dimSize);
      }
    }
    else {
      int zo = zolimmag.getValue();
      int zi = zilimmag.getValue();

      if ( source == zolimmag ) {
        zolbl.setText(Double.toString(zo/10.0));
        if ( zo > zi ) zilimmag.setValue(zo);
      }
      else {
        zilbl.setText(Double.toString(zi/10.0));
        if ( zo > zi ) zolimmag.setValue(zi);
      }
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    int zomag10 = prefer.getZoLimMag10();
    if      ( zomag10 < minmag10 ) zomag10 = minmag10;
    else if ( zomag10 > maxmag10 ) zomag10 = maxmag10;
    zolimmag.setValue(zomag10);
    zolbl.setText(Double.toString(zomag10/10.0));

    int zimag10 = prefer.getZiLimMag10();
    if      ( zimag10 < minmag10 ) zimag10 = minmag10;
    else if ( zimag10 > maxmag10 ) zimag10 = maxmag10;
    zilimmag.setValue(zimag10);
    zilbl.setText(Double.toString(zimag10/10.0));

    bright.setValue(Math.max(1,   // Clamp just to make sure...
                    Math.min(StarImages.NUMIMAGES, prefer.getSzBright())));
    dim.setValue(Math.max(1,
                 Math.min(StarImages.NUMIMAGES, prefer.getSzDim())));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    prefer.setZiLimMag10(zilimmag.getValue());
    prefer.setZoLimMag10(zolimmag.getValue());
    prefer.setSzStar(bright.getValue(), dim.getValue());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Star size view strip.
   */
  final class Stars extends JPanel {
    private Color backgnd;
    private Image[] stars;
    private int offset;

    public Stars(Preferences prefer) {
      setOpaque(true);   // Informs Swing's paint system that painting of
      // underlying components is not necessary and that this component
      // will paint all pixels within its rectangle.

      setPreferredSize(new Dimension(20, 20)); // dummy, desired height

      backgnd = prefer.colorBackGnd();
      stars = new StarImages().getImages(prefer.colorStar(), backgnd);
      offset = StarImages.OFFSET;
    }

    boolean bmpStars = prefer.getBmpStars(); // Are we painting bmps?
    /* Override paint to avoid handling a border */
    public void paint(Graphics g) {
      Dimension dim = getSize();
      if ( dim.width > 0 && dim.height > 0 ) {
        Ellipse2D.Float circle = new Ellipse2D.Float();
        g.setColor(backgnd);
        g.fillRect(0, 0, dim.width, dim.height);
        if ( !bmpStars ) {
          Graphics2D g2 = (Graphics2D)g;
          g2.setPaint(prefer.colorStar());
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
        }
        int y = dim.height / 2;
        // Step size is dim.width / stars.length
        int x0 = dim.width / (stars.length * 2) - 1; // -1 is fudge
        for ( int i = 0; i < stars.length; i++ )
          if ( !bmpStars ) {
            circle.setFrame(x0 + i*(dim.width+2)/stars.length - (11-i)/2.0,
                            y - (11-i)/2.0,
                            11-i+0.2, 11-i+0.2); // See StarDB.java for method;
                            // +0.2 cheats a bit so that dimmest star can be
                            // seen; for some reason it's harder to see here
                            // than on the main star window
            ((Graphics2D)g).fill(circle);
          }
          else
            g.drawImage(stars[stars.length - 1 - i],
                        x0 + i*(dim.width+2)/stars.length - offset, y - offset,
                        null);           // +2 is fudge

        if ( !bmpStars ) {
          Graphics2D g2 = (Graphics2D)g;
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
        }
      }
    }
  }

  /* For testing */
  //public static void main(String[] args) {
  //  StrPrmDlg dlg = new StrPrmDlg(null, new Preferences());
  //  dlg.setVisible(true);
  //  System.exit(0);
  //}
}

