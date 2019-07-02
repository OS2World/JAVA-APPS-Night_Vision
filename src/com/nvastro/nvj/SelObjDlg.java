/*
 * SelObjDlg.java  -  "Select object" dialog
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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Select object" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class SelObjDlg extends EscapeDlg
             implements ItemListener, ChangeListener {
  private JCheckBox starnames, bayer, flamsteed, nearsky, deepsky,
                    dsnames, milkyway, conlines, conbounds, connames,
                    cgrid, cgridlbls, agrid, agridlbls, ecliptic, horizon;
  private JLabel zolbl, zilbl;
  private JSlider zolimmag, zilimmag;
  private JRadioButton confull, conabbr;
  private ActionListener listeners = null;
  private Preferences prefer;
  static private SelObjDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new SelObjDlg(owner, prefer);
    dlg.readPrefer();
    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   */
  private SelObjDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("SelObjDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          SelObjDlg.this.close();    // Pop down dialog
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(SelObjDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Create controls for this window */
    starnames = new JCheckBox(TextBndl.getString("SelObjDlg.Strnames"));
    bayer     = new JCheckBox(TextBndl.getString("SelObjDlg.Bayer"));
    flamsteed = new JCheckBox(TextBndl.getString("SelObjDlg.Flamsteed"));
    nearsky   = new JCheckBox(TextBndl.getString("SelObjDlg.NS"));
    deepsky   = new JCheckBox(TextBndl.getString("SelObjDlg.DS"));
    dsnames   = new JCheckBox(TextBndl.getString("SelObjDlg.DSNames"));
    zolbl     = new JLabel("99.9"); // Use large # for setting min size
    zilbl     = new JLabel("99.9"); // Use large # for setting min size
    zolimmag  = new JSlider();
    zilimmag  = new JSlider();
    zolimmag.setMajorTickSpacing(1);
    zilimmag.setMajorTickSpacing(1);
    zolimmag.setSnapToTicks(true);
    zilimmag.setSnapToTicks(true);
    zolimmag.setPaintTicks(false);
    zilimmag.setPaintTicks(false);
    zolimmag.addChangeListener(this);
    zilimmag.addChangeListener(this);
    milkyway  = new JCheckBox(TextBndl.getString("SelObjDlg.MW"));
    conlines  = new JCheckBox(TextBndl.getString("SelObjDlg.ConLines"));
    conbounds = new JCheckBox(TextBndl.getString("SelObjDlg.ConBounds"));
    connames  = new JCheckBox(TextBndl.getString("SelObjDlg.ConNames"));
    confull   = new JRadioButton(TextBndl.getString("SelObjDlg.ConFull"));
    conabbr   = new JRadioButton(TextBndl.getString("SelObjDlg.ConAbbr"));
    cgrid     = new JCheckBox(TextBndl.getString("SelObjDlg.CGrid"));
    cgridlbls = new JCheckBox(TextBndl.getString("SelObjDlg.CGridLabels"));
    agrid     = new JCheckBox(TextBndl.getString("SelObjDlg.AGrid"));
    agridlbls = new JCheckBox(TextBndl.getString("SelObjDlg.AGridLabels"));
    ecliptic  = new JCheckBox(TextBndl.getString("SelObjDlg.Ecliptic"));
    horizon   = new JCheckBox(TextBndl.getString("SelObjDlg.Horizon"));
    ButtonGroup bg = new ButtonGroup();
    bg.add(confull);
    bg.add(conabbr);
    deepsky.addItemListener(this);
    connames.addItemListener(this);
    cgrid.addItemListener(this);
    agrid.addItemListener(this);

    /* Create a box to hold west (left side) controls */
    Box w = Box.createVerticalBox();
    starnames.setAlignmentX(0.0f);
    w.add(starnames);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    bayer.setAlignmentX(0.0f);
    w.add(bayer);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    flamsteed.setAlignmentX(0.0f);
    w.add(flamsteed);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    cgrid.setAlignmentX(0.0f);
    w.add(cgrid);
    w.add(Box.createVerticalStrut(5));
    AlignableBox cl = AlignableBox.createHorzAlignableBox();
    cl.setAlignmentX(0.0f);
      cl.add(Box.createHorizontalStrut(16));
      cl.add(cgridlbls);
    w.add(cl);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    agrid.setAlignmentX(0.0f);
    w.add(agrid);
    w.add(Box.createVerticalStrut(5));
    AlignableBox al = AlignableBox.createHorzAlignableBox();
    al.setAlignmentX(0.0f);
      al.add(Box.createHorizontalStrut(16));
      al.add(agridlbls);
    w.add(al);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    ecliptic.setAlignmentX(0.0f);
    w.add(ecliptic);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    horizon.setAlignmentX(0.0f);
    w.add(horizon);
    w.add(Box.createVerticalStrut(10)); w.add(Box.createVerticalGlue());
    nearsky.setAlignmentX(0.0f);
    w.add(nearsky);

    /* Create a box to hold east (right side) controls */
    Box e = Box.createVerticalBox();
    deepsky.setAlignmentX(0.0f);
    e.add(deepsky);
    e.add(Box.createVerticalStrut(5));
    // Create a Box/JPanel/GridBagLayout to hold dsnames & sliders
    AlignableBox n = AlignableBox.createHorzAlignableBox();
    n.setAlignmentX(0.0f);
     JPanel ds = new JPanel();
      ds.setLayout(new GridBagLayout());
      GridBagConstraints g = new GridBagConstraints();
      g.insets = new Insets(0, 0, 10, 0); // top, left, bottom, right
      // Row 1
      g.gridx = 0; g.gridy = 0;
      g.gridwidth = 2; // (g.gridheight = 1;)
      g.anchor = GridBagConstraints.WEST; // Dft is CENTER
      ds.add(dsnames, g);
      // Row 2
      g.gridx = 0; g.gridy = 1;
      g.anchor = GridBagConstraints.CENTER;
      ds.add(new JLabel(TextBndl.getString("SelObjDlg.ZoMag")), g);
      // Row 3
      g.gridx = 0; g.gridy = 2;
      g.gridwidth = 1;
      ds.add(zolbl, g);
      g.gridx = 1;
      ds.add(zolimmag, g);
      // Row 4
      g.gridx = 0; g.gridy = 3;
      g.gridwidth = 2;
      ds.add(new JLabel(TextBndl.getString("SelObjDlg.ZiMag")), g);
      // Row 5
      g.gridx = 0; g.gridy = 4;
      g.gridwidth = 1;
      ds.add(zilbl, g);
      g.gridx = 1;
      ds.add(zilimmag, g);
      // Row 6 (Keeps column 1 width constant when changing "9.9" to "10.0")
      g.gridx = 0; g.gridy = 5;
      Box spacer = Box.createHorizontalBox();
      Dimension dim = zolbl.getPreferredSize();
      spacer.add(Box.createHorizontalStrut(dim.width));
      g.insets = new Insets(0, 0, 0, 0); // top, left, bottom, right
      ds.add(spacer, g);
      // Keep same vertical dimension
      Dimension dimds = ds.getPreferredSize();
      ds.setMaximumSize(dimds);
     n.add(Box.createHorizontalStrut(16));
     n.add(ds);
    e.add(n);
    e.add(Box.createVerticalStrut(10)); e.add(Box.createVerticalGlue());
    milkyway.setAlignmentX(0.0f);
    e.add(milkyway);
    e.add(Box.createVerticalStrut(10)); e.add(Box.createVerticalGlue());
    conlines.setAlignmentX(0.0f);
    e.add(conlines);
    e.add(Box.createVerticalStrut(10)); e.add(Box.createVerticalGlue());
    conbounds.setAlignmentX(0.0f);
    e.add(conbounds);
    e.add(Box.createVerticalStrut(10)); e.add(Box.createVerticalGlue());
    connames.setAlignmentX(0.0f);
    e.add(connames);
    e.add(Box.createVerticalStrut(5));
    AlignableBox c = AlignableBox.createHorzAlignableBox();
    c.setAlignmentX(0.0f);
      c.add(Box.createHorizontalStrut(16));
      c.add(confull);
      c.add(Box.createHorizontalStrut(8));
      c.add(conabbr);
    e.add(c);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "selobj");
    setHelpPage("selobj");

    /* Create a Box and add buttons for OK, Apply, Cancel, & Help */
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
    getContentPane().add(Box.createHorizontalStrut(40));
    getContentPane().add(w, BorderLayout.WEST);
    getContentPane().add(e, BorderLayout.EAST);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(starnames);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements ItemListener.
   */
  public void itemStateChanged(ItemEvent e) {
    dsnames.setEnabled(deepsky.isSelected());
    zolbl.setEnabled(deepsky.isSelected());
    zilbl.setEnabled(deepsky.isSelected());
    zolimmag.setEnabled(deepsky.isSelected());
    zilimmag.setEnabled(deepsky.isSelected());
    confull.setEnabled(connames.isSelected());
    conabbr.setEnabled(connames.isSelected());
    cgridlbls.setEnabled(cgrid.isSelected());
    agridlbls.setEnabled(agrid.isSelected());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ChangeListener interface.
   */
  public void stateChanged(ChangeEvent e) {
    int zo = zolimmag.getValue();
    int zi = zilimmag.getValue();

    if ( zolimmag == (JSlider)(e.getSource()) ) {
      zolbl.setText(Double.toString(zo/10.0));
      if ( zo > zi) zilimmag.setValue(zo);
    }
    else {
      zilbl.setText(Double.toString(zi/10.0));
      if ( zo > zi) zolimmag.setValue(zi);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Read preferences.
   */
  private void readPrefer() {
    int minmag10, maxmag10;
    int zoLimMag10 = prefer.getZoDSLimMag10();
    int ziLimMag10 = prefer.getZiDSLimMag10();

    // Because DS DB is reloadable, read latest values
    { DeepSkyDB dsdb = new DeepSkyDB(); minmag10 = dsdb.getMinMag100() / 10;
                                        maxmag10 = dsdb.getMaxMag100() / 10; }
    zoLimMag10 = Math.min(Math.max(minmag10, zoLimMag10), maxmag10);
    ziLimMag10 = Math.min(Math.max(minmag10, ziLimMag10), maxmag10);
    zolimmag.setMinimum(minmag10);
    zolimmag.setMaximum(maxmag10);
    zolimmag.setValue(zoLimMag10);
    zilimmag.setMinimum(minmag10);
    zilimmag.setMaximum(maxmag10);
    zilimmag.setValue(ziLimMag10);
    zolbl.setText(Double.toString(zoLimMag10/10.0));
    zilbl.setText(Double.toString(ziLimMag10/10.0));

    starnames.setSelected(prefer.drawStarNames());
    bayer.setSelected(prefer.drawBayer());
    flamsteed.setSelected(prefer.drawFlamsteed());
    nearsky.setSelected(prefer.drawNearSky());
    deepsky.setSelected(prefer.drawDeepSky());
    dsnames.setSelected(prefer.drawDeepSkyNames());
    milkyway.setSelected(prefer.drawMilkyWay());
    conlines.setSelected(prefer.drawConstLines());
    conbounds.setSelected(prefer.drawConstBounds());
    connames.setSelected(prefer.drawConstNames());
    if ( prefer.drawConstNFull() ) confull.setSelected(true);
    else                           conabbr.setSelected(true);
    confull.setSelected(prefer.drawConstNFull());
    cgrid.setSelected(prefer.drawCGrid());
    cgridlbls.setSelected(prefer.drawCGridLabels());
    agrid.setSelected(prefer.drawAGrid());
    agridlbls.setSelected(prefer.drawAGridLabels());
    ecliptic.setSelected(prefer.drawEcliptic());
    horizon.setSelected(prefer.drawHorizon());

    dsnames.setEnabled(prefer.drawDeepSky());
    zolbl.setEnabled(deepsky.isSelected());
    zilbl.setEnabled(deepsky.isSelected());
    zolimmag.setEnabled(deepsky.isSelected());
    zilimmag.setEnabled(deepsky.isSelected());
    confull.setEnabled(prefer.drawConstNames());
    conabbr.setEnabled(prefer.drawConstNames());
    cgridlbls.setEnabled(prefer.drawCGrid());
    agridlbls.setEnabled(prefer.drawAGrid());
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    prefer.drawStarNames(starnames.isSelected());
    prefer.drawBayer(bayer.isSelected());
    prefer.drawFlamsteed(flamsteed.isSelected());
    prefer.drawNearSky(nearsky.isSelected());
    prefer.drawDeepSky(deepsky.isSelected());
    prefer.drawDeepSkyNames(dsnames.isSelected());
    prefer.setZoDSLimMag10(zolimmag.getValue());
    prefer.setZiDSLimMag10(zilimmag.getValue());
    prefer.drawMilkyWay(milkyway.isSelected());
    prefer.drawConstLines(conlines.isSelected());
    prefer.drawConstBounds(conbounds.isSelected());
    prefer.drawConstNames(connames.isSelected());
    prefer.drawConstNFull(confull.isSelected());
    prefer.drawCGrid(cgrid.isSelected());
    prefer.drawCGridLabels(cgridlbls.isSelected());
    prefer.drawAGrid(agrid.isSelected());
    prefer.drawAGridLabels(agridlbls.isSelected());
    prefer.drawEcliptic(ecliptic.isSelected());
    prefer.drawHorizon(horizon.isSelected());
  }
}

