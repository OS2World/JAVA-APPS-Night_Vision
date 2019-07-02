/*
 * LocationDlg.java  -  "Set location" dialog
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
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
// Keymap, PlainDocument


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Set location" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class LocationDlg extends EscapeDlg
                         implements ListSelectionListener, ItemListener {
  private JList<String> list;
  private LatLong lng, lat, tz;
  private JCheckBox autodst, dstloc, dstcor;
  private WorldMap wm;
  private CityDB cities;
  private JButton OKLoc, OKCor;
  private ActionListener listeners = null;
  private Preferences prefer;
  static private LocationDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new LocationDlg(owner, prefer);
    else {
      dlg.loadPrefer();
    }

    dlg.setVisible(true);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Updates the dialog if it has been created.  (Does not matter if it is
   * currently showing.)  Should be called only by DateTimeDlg.
   */
  public static void updateDlg() {
    if ( dlg != null ) {
      dlg.dstloc.setSelected(dlg.prefer.inDST());
      dlg.dstcor.setSelected(dlg.prefer.inDST());
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private LocationDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("LocationDlg.Title"), false);
    this.prefer = prefer;
    cities = new CityDB();
    boolean noCities;
    int i, j;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if ( e.getActionCommand().equals("OKLoc") ) updatePreferFromList();
        if ( e.getActionCommand().equals("OKCor") ) {
          if ( updatePreferFromCoords() == false ) // Updates prefer if true
            return; // Don't close dlg
        }
        LocationDlg.this.close(); // Pop down dialog
        if ( listeners != null && (e.getActionCommand().equals("OKLoc") ||
                                   e.getActionCommand().equals("OKCor")) )
          listeners.actionPerformed(new ActionEvent(LocationDlg.this,
                                    e.getID(), "update"));
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Look for double clicks */
    MouseListener mouseListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if ( e.getClickCount() == 2 ) {
          //int index = list.locationToIndex(e.getPoint());
          updatePreferFromList();
          LocationDlg.this.close(); // Pop down dialog
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(LocationDlg.this,
                                      e.getID(), "update"));
        }
        // The following added because if a city was selected, and then
        // focus is placed elsewhere (causing the crosshairs on the map
        // to move to a different location), and then the same city is
        // selected again, valueChanged is *not* called (and thus the
        // crosshairs are *not* moved back to the city)
        else if ( e.getClickCount() == 1 ) {
          int i = list.getSelectedIndex();
          if ( i >= 0 )
            wm.setWmLocation(cities.getLatDeg(i), cities.getLongDeg(i));
            // Note: focusListener takes care of default button
        }
      }
    };

    /* Handle manipulation of default button */
    FocusListener focusListener = new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        if ( e.getComponent().getParent().getParent() == lng ||
             e.getComponent().getParent().getParent() == lat ||
             e.getComponent().getParent().getParent() == tz  ||
             e.getSource() == dstcor ||
             e.getSource() == OKCor ) {
          getRootPane().setDefaultButton(OKCor);
        }
        else
          getRootPane().setDefaultButton(OKLoc);
      }
    };

    /* Get data to display */
    String[] data;
    j = cities.getNumberOfCities();
    if ( j > 0 ) {
      noCities = false;
      data = new String[j];
      for ( i = 0; i < j; i++ )
        data[i] = cities.tellName(i);
    }
    else {
      noCities = true;
      data = new String[1];
      data[0] = TextBndl.getString("LocDB.None");
    }

    /* Create controls for this window */
    wm = new WorldMap(); // Do before loading list
    list = new JList<String>(data);
      list.setVisibleRowCount(6);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    NFScrollPane slist = new NFScrollPane(list);
    if ( noCities ) {
      list.setEnabled(false);
    }
    else {
      list.addListSelectionListener(this);
      list.addMouseListener(mouseListener);
      list.addFocusListener(focusListener);
    }
    lng = new LatLong(LatLong.LNG);
      lng.addFocusListener(focusListener);
    lat = new LatLong(LatLong.LAT);
      lat.addFocusListener(focusListener);
    tz  = new LatLong(LatLong.TMZ);
      tz.addFocusListener(focusListener);
    JLabel LOR = new JLabel(TextBndl.getString("LocationDlg.Or"));
    JLabel LON = new JLabel(TextBndl.getString("LocationDlg.Long"));
    JLabel LAT = new JLabel(TextBndl.getString("LocationDlg.Lat"));
    JLabel LTZ = new JLabel(TextBndl.getString("LocationDlg.TZ"));
    autodst = new JCheckBox(TextBndl.getString("LocationDlg.AutoDst"));
      autodst.addFocusListener(focusListener);
      autodst.addItemListener(this);
    dstloc = new JCheckBox(TextBndl.getString("LocationDlg.Dst"));
      dstloc.addFocusListener(focusListener);
    dstcor = new JCheckBox(TextBndl.getString("LocationDlg.Dst"));
      dstcor.addFocusListener(focusListener);

    /* Create the city panel.  It will contain citybox which contains
       slist and citydst, which is a box that contains dst stuff. */
    AlignableBox citydst = AlignableBox.createHorzAlignableBox();
      citydst.add(autodst);
      citydst.add(Box.createHorizontalStrut(6));
      citydst.add(Box.createHorizontalGlue());
      citydst.add(dstloc);
      citydst.add(Box.createHorizontalStrut(6));
      citydst.add(Box.createHorizontalGlue());
    Box citybox = Box.createVerticalBox();
      slist.setAlignmentX(0.0f);
      citydst.setAlignmentX(0.0f);
      citybox.add(Box.createVerticalStrut(2));
      citybox.add(slist);
      citybox.add(Box.createVerticalStrut(6));
      citybox.add(citydst);
      citybox.add(Box.createVerticalStrut(6));
    JPanel citypanel = new JPanel(new BorderLayout());
      citypanel.setBorder(new TitledBorder(
                          TextBndl.getString("LocationDlg.Loc")));
      citypanel.add(citybox);

    /* Create the coordinate panel.
       1st create a constraints object to control placement
       and set some defaults. */
    GridBagConstraints c = new GridBagConstraints();
    // Dft c.anchor = GridBagConstraints.CENTER;
    // Dft c.gridwidth = c.gridheight = 1;
    // Dft c.fill = GridBagConstraints.NONE;
    // Dft c.insets = new Insets(0, 0, 0, 0);
    //----------
    JPanel coordpanel = new JPanel(new GridBagLayout());
    Insets lInsets = new Insets(0, 7, 6, 4); // T, L, B, R
    Insets rInsets = new Insets(0, 4, 6, 7);
    c.gridx = 0; c.gridy = 0; c.insets = lInsets;
    coordpanel.add(LAT, c);
    c.gridx = 1; c.gridy = 0; c.insets = rInsets;
    coordpanel.add(lat, c);
    c.gridx = 0; c.gridy = 1; c.insets = lInsets;
    coordpanel.add(LON, c);
    c.gridx = 1; c.gridy = 1; c.insets = rInsets;
    coordpanel.add(lng, c);
    c.gridx = 2; c.gridy = 0; c.insets = new Insets(0, 15, 6, 4);
    coordpanel.add(LTZ, c);
    c.gridx = 3; c.gridy = 0; c.insets = new Insets(0, 4, 6, 7);
    coordpanel.add(tz, c);
    c.gridx = 2; c.gridy = 1; c.insets = new Insets(0, 7, 6, 7);
    c.gridwidth = 2;
    //c.anchor = GridBagConstraints.WEST;
    coordpanel.add(dstcor, c);
      coordpanel.setBorder(new TitledBorder(
                           TextBndl.getString("LocationDlg.Cor")));

    /* Create a box to hold the city and coordinate panels. */
    Box a = Box.createVerticalBox();
      citypanel.setAlignmentX(0.5f);
      LOR.setAlignmentX(0.5f);
      coordpanel.setAlignmentX(0.5f);
      a.add(wm);
      a.add(Box.createVerticalStrut(12));
      a.add(citypanel);
      a.add(Box.createVerticalStrut(8));
      a.add(LOR);
      a.add(Box.createVerticalStrut(8));
      a.add(coordpanel);

    /* Create some buttons */
    OKLoc = new JButton(TextBndl.getString("LocationDlg.OKLoc"));
      OKLoc.setActionCommand("OKLoc");
      OKLoc.addActionListener(listener);
      OKLoc.addFocusListener(focusListener);
    OKCor = new JButton(TextBndl.getString("LocationDlg.OKCor"));
      OKCor.setActionCommand("OKCor");
      OKCor.addActionListener(listener);
      OKCor.addFocusListener(focusListener);
    JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
      Cancel.setActionCommand("Cancel");
      Cancel.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "setloc");
    setHelpPage("setloc");

    /* Create a Box and add buttons for OK's, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(OKLoc);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(OKCor);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(Cancel);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(a);
    getContentPane().add(b, BorderLayout.SOUTH);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(noCities ? (Component)lng : (Component)list);

    /* Load in data to controls, setup enabled/disabled */
    loadPrefer();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ListSelectionListener interface
   * for single click on list.
   */
  public void valueChanged(ListSelectionEvent e) {
    //stem.out.println("List selection = " + list.getSelectedIndex());
    int i = list.getSelectedIndex();

    /* Check if list cleared */
    if ( i < 0 ) {       // No cities or none selected
      autodst.setEnabled(false);
      dstloc.setEnabled(false);
      OKLoc.setEnabled(false);
      return;
    }

    /* If we're here, a city was selected */
    boolean newdst = cities.getLocation(i).getTZ().inDaylightTime(
                     prefer.getLocDateTime().getTime());
    dstloc.setSelected(newdst);
    dstcor.setSelected(newdst);
    lng.setValue(cities.getLongDeg(i));
    lat.setValue(cities.getLatDeg(i));
    tz.setValue(cities.getTZOffsetMin(i) / 60.0);
    OKLoc.setEnabled(true);
    if ( cities.handlesDST(i) ) {
      autodst.setSelected(true);
      autodst.setEnabled(true);
      dstloc.setEnabled(false);
    }
    else {
      autodst.setSelected(false);
      autodst.setEnabled(false);
      dstloc.setEnabled(true);
    }
    wm.setWmLocation(cities.getLatDeg(i), cities.getLongDeg(i));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ItemListener interface
   * for toggling of autodst.
   */
  public void itemStateChanged(ItemEvent e) {
    dstloc.setEnabled(!autodst.isSelected());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by wm when it has been clicked upon.  New coordinates are then
   * forwarded to lat, lng, and tz.
   */
  public void wmChanged(double newLat, double newLng) {
    //stem.out.println("wm Changed: lat = " + newLat + ", lng = " + newLng);
    lat.setValue(newLat);
    lng.setValue(newLng);
    tz.setValue(Math.floor((newLng + 7.5) / 15));

    // If OKLoc is default, that means focus is in the location area
    // and should be shifted to the coordinate area
    if ( getRootPane().getDefaultButton() == OKLoc ) {
      lat.setFocusDeg();  // Sets OKCor as default button
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by lat or lng (or tz) when a change has occurred.  New values
   * are then forwarded up to the worldmap.
   */
  public void coordChanged() {
    double newLat = lat.getValue();
    double newLng = lng.getValue();

    wm.setWmLocation(newLat, newLng);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    /* Set list selection */
    int i, j, k;
    k = -1;  // No selection (yet)
    j = cities.getNumberOfCities();
    Location prefLocation = prefer.getLocation();
    if ( j > 0 ) {
      for ( i = 0; i < j; i++ ) {
        if ( cities.getLocation(i) == prefLocation ) { k = i; }
      }
    }
    if ( ! list.isSelectionEmpty() ) list.clearSelection(); // This call
      // will cause a valueChanged only if there was an existing selection,
      // so can not count on valueChanged being called.  However now
      // any setSelectedIndex will cause a valueChanged to occur...
    if ( k >= 0 ) {        // Cities are available, *and* one is selected
      list.setSelectedIndex(k);  // Do here before setting up autodst since
        // setSelectedIndex causes valueChanged which messes with autodst.
        // This is a good thing, as the code handling valueChanged will
        // disable autodst if the location doesn't handle DST.  If it
        // isn't disabled, then the following code will set its selected
        // state based on user preferences.
      list.ensureIndexIsVisible(k);
      if ( ! prefer.getDST().equals("A") ) { // User doesn't want auto-DST
        autodst.setSelected(false);
      }
    }
    else {                 // No cities available, or none selected
      // clearSelection triggers call to valueChanged, which will
      // disable the appropriate components, but only if there was an
      // existing selection.  So (re-)do the following anyway:
      autodst.setEnabled(false);
      dstloc.setEnabled(false);
      OKLoc.setEnabled(false);
    }

    /* More user preferences */
    lng.setValue(prefer.getLongDeg());
    lat.setValue(prefer.getLatDeg());
    tz.setValue(prefer.getTZOffsetMin() / 60.0);
    dstloc.setSelected(prefer.inDST());
    dstcor.setSelected(prefer.inDST());

    /* Make sure a disabled component does not receive focus */
    if ( j > 0 ) list.requestFocus();
    else          lng.requestFocus();
  }

  /*----------------------------------------------------------------------------
    The following two functions, updatePreferFromList(), and
    updatePreferFromCoords(), change the location on earth and/or the
    timezone, but are designed to have no effect on the instance in time.
    That is, the time inside the LST object, which is based on Greenwich,
    will not change.  Some consequences:
    - A change in location will shift the stars and other objects as if
      you were suddenly transported to this location.
    - A change in location from Denver to Dallas will increment the time
      reported in the status window by 1 hour, because the time in Dallas
      is 1 hour later.  (However the time in Greenwich did not change.)
    - Changing from DST to ST at the same location will not shift the
      stars, but will change the time reported in the status window.
      (The instance in time did not change; only how it was interpreted.)
  ----------------------------------------------------------------------------*/

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences from list.
   */
  private void updatePreferFromList() {
    int i = list.getSelectedIndex();
    prefer.setLocation(cities.getLocation(i), autodst.isSelected() ? "A" :
                                               dstloc.isSelected() ? "1" : "0");
    DateTimeDlg.updateDlg();  // Update DateTimeDlg with latest dst info
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences from coordinates.
   */
  private boolean updatePreferFromCoords() {
    double[] la = new double[1];
    double[] lo = new double[1];
    double[] ti = new double[1];
    String errstr;

    errstr = lat.getValue(la);
    if ( errstr.length() > 0 ) {
      OptionDlg.showMessageDialog(this,
                                  errstr,
                                  TextBndl.getString("LocationDlgE.LatTitle"),
                                  JOptionPane.ERROR_MESSAGE);
      return false;
    }

    errstr = lng.getValue(lo);
    if ( errstr.length() > 0 ) {
      OptionDlg.showMessageDialog(this,
                                  errstr,
                                  TextBndl.getString("LocationDlgE.LngTitle"),
                                  JOptionPane.ERROR_MESSAGE);
      return false;
    }

    errstr =  tz.getValue(ti);
    if ( errstr.length() > 0 ) {
      OptionDlg.showMessageDialog(this,
                                  errstr,
                                  TextBndl.getString("LocationDlgE.TmzTitle"),
                                  JOptionPane.ERROR_MESSAGE);
      return false;
    }

    prefer.setLocation(new Location(null, lo[0], la[0], ti[0]),
                       dstcor.isSelected() ? "1" : "0");
    DateTimeDlg.updateDlg();  // Update DateTimeDlg with latest dst info
    return true;
  }

  /* For testing */
  //static public void main(String[] args) {
  //  Preferences prefer = new Preferences();
  //  //showDlg(null, prefer);
  //  LocationDlg dlg = new LocationDlg(null, prefer);
  //  // The following allows exiting via system menu (but not cancel/escape)
  //  dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
  //  dlg.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //  dlg.setVisible(true);
  //}
}

