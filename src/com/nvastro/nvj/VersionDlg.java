/*
 * VersionDlg.java  -  "Version" dialog
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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Version" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class VersionDlg extends EscapeDlg {
  static private VersionDlg dlg = null;
  private ROTextArea ta = null;
  private Preferences prefer = null;
  private DecNumFormat jd_format; // Format for displaying Julian Day
  private DecNumFormat dt_format; // Format for displaying Delta T
  static final Properties props = System.getProperties();
  static final String[] keys = {
    "java.vendor",
    "java.version",
    "java.runtime.name",
    "java.runtime.version",
    "java.vm.name",
    "java.vm.version",
    "java.vm.info",
    "os.arch",
    "os.name",
    "os.version",
    "user.dir",
    "user.timezone"
  };

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner The owner window (main window)
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new VersionDlg(owner, prefer);
    dlg.setText();
    dlg.ta.setCaretPosition(0);     // Make sure 1st line visible
    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the text.
   */
  private void setText() {
    ta.setText(null);
    for ( int i = 0; i < keys.length; i++ ) {
      ta.append(keys[i] + " = " + props.getProperty(keys[i]) + "\n");
    }

    TimeZone tz = TimeZone.getDefault();
    ta.append("TZ ID = " + tz.getID());
    ta.append(", Std Offset = " +
              (tz.getRawOffset() / 60000) + " minutes\n");
    ta.append("DST Offset = " +
              (tz.getDSTSavings() / 60000) + " minutes, currently ");
    if ( ! tz.inDaylightTime(new Date()) ) ta.append("not ");
    ta.append("in DST\n");
    // Note:  The System property "user.timezone" can be blank
    // and yet still have a default TimeZone.  (Though it appears
    // if set, it drives what the default TimeZone is.)
    // Note:  To try other timezones, try something like:
    // java -Duser.timezone=Europe/London nvj.Nvj
    // (Select values from nvlocations.txt)
    if ( Nvj.workingDir != null && !Nvj.workingDir.equals("") &&
         !Nvj.workingDir.equals(".") )
      ta.append("working dir = " + Nvj.workingDir + "\n");
    if ( Nvj.iniDir != null && !Nvj.iniDir.equals("") &&
         !Nvj.iniDir.equals(".") )
      ta.append("ini dir = " + Nvj.iniDir + "\n");

    ta.append("- Program variables -");
    ta.append("\nVersion = " + Nvj.PgmVersion);
    ta.append("\nLocal Date/Time = " +
              prefer.lst.tellLocDateTime(!prefer.is24Hr()) +
              (prefer.lst.inDST() ? " (DST), " : ", ") +
              (prefer.lst.isRunning() ? "is running at " +
                 prefer.lst.getTimeSpeed() + "X" : "is stopped"));
    ta.append("\nStandard timezone offset = " + prefer.tellTZOffset());
    ta.append("\nActual timezone offset = " + prefer.lst.tellTZOffsetWithDST());
    double JD = prefer.lst.getJulianDay();
    double JED = prefer.lst.getJulianEphDay();
    ta.append("\nJulian Day = " + jd_format.format(JD));
    ta.append("\nJulian (Ephemeris) Day = " + jd_format.format(JED));
    ta.append("\nDelta t = " + dt_format.format((JED - JD) * 86400)+" seconds");
    SphereCoords time = new SphereCoords(prefer.lst.getLSTHrs()*Math.PI/12, 0);
    ta.append("\nLocal Sidereal Time = " + time.tellRAHrMnScT());
    ta.append("\nLocation = " + prefer.tellLong() + ", " + prefer.tellLat());
    String city = prefer.tellCity();
    if ( city.length() > 0 ) ta.append(", " + city);
    ta.append("\nGeocentric = " + (Preferences.geocentric ? "true" : "false"));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner The owner window (main window)
   */
  private VersionDlg(final Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, "Java version and program variables", false);
    this.prefer = prefer;

    /* Set up number formats */
    jd_format = new DecNumFormat("0.0000000"); // (rounds)
    dt_format = new DecNumFormat("0.00"); // (rounds)

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if ( e.getActionCommand().equals("Update") ) setText();
        else VersionDlg.this.close(); // Pop down dialog
      }
    };

    /* Create controls for this window */
    ta = new ROTextArea(null, 11 /* instead of keys.length */, 45);

    /* Create some buttons */
    JButton Close = new JButton(TextBndl.getString("Dlg.Close"));
    Close.setActionCommand("Cancel");
    Close.addActionListener(listener);
    JButton Update = new JButton("Update");
    Update.setActionCommand("Update");
    Update.addActionListener(listener);

    /* Create a Box and add buttons for Close & Update */
    Box b = Box.createHorizontalBox();
    b.add(Close);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    b.add(Update);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(16);
    getContentPane().add(ta);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(Close);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(true);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(Close);
  }
}

