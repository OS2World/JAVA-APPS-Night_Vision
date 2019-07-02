/*
 * JulianDlg.java  -  "Julian Date Converter" dialog
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Julian Date Converter" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class JulianDlg extends EscapeDlg {
  private JRadioButton ad, bc;
  private JComboBox<String> year, month, day, hour, min, sec;
  private JTextField jday, wday;
  private String[] dows;
  static private JulianDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   */
  public static void showDlg(Frame owner) {
    if ( dlg == null ) dlg = new JulianDlg(owner/*, prefer*/);

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   */
  private JulianDlg(Frame owner) {
    /* Set window name */
    super(owner, "Julian Date Converter", false);
    int i;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if      ( e.getActionCommand().equals("CalcJul") )
          calcJulDate();
        else if ( e.getActionCommand().equals("CalcCal") )
          calcCalDate();
        else
          JulianDlg.this.close(); // Pop down dialog
      }
    };

    /* Setup day of week strings */
    dows = new DateFormatSymbols().getWeekdays();
    // 0: <blank>, 1: Sunday, 2: Monday, ..., 7: Saturday
    // (More properly use Calendar.SUNDAY, ... to index into the array,
    // but doubt that future Java will change these numbers)

    /* Create controls for this window */
    ad = new JRadioButton("AD", true);
    bc = new JRadioButton("BC");
      ButtonGroup bg = new ButtonGroup();
      bg.add(ad);
      bg.add(bc);
    year  = new EComboBox();
      for ( i = 1; i <= 4713; i++ ) year.addItem(Integer.toString(i));
    DateFormatSymbols dfs = new DateFormatSymbols();
    String[] months = dfs.getMonths();
    // Size of months array is 13; last element is blank
    month = new EComboBox();
      for ( i = 0; i < months.length && months[i].length() > 0; i++ )
        month.addItem(months[i]);
    day   = new EComboBox();
      for ( i =  1; i < 32; i++ ) day.addItem("" + i);
    hour  = new EComboBox();
      for ( i =  0; i < 24; i++ ) hour.addItem("" + i/10 + i%10);
    min   = new EComboBox();
      for ( i =  0; i < 60; i++ ) min.addItem("" + i/10 + i%10);
    sec   = new EComboBox();
      for ( i =  0; i < 60; i++ ) sec.addItem("" + i/10 + i%10);
    wday = new JTextField();
      wday.setEditable(false);
    jday = new JTextField();
    JButton CalcJul = new JButton("Calculate Julian Date");
      CalcJul.setActionCommand("CalcJul");
      CalcJul.addActionListener(listener);
    JButton CalcCal = new JButton("Calculate Calendar Date");
      CalcCal.setActionCommand("CalcCal");
      CalcCal.addActionListener(listener);

    /* Set dfts */
    GregorianCalendar gc = new GregorianCalendar();
    year.setSelectedIndex(gc.get(Calendar.YEAR) - 1);
    month.setSelectedIndex(gc.get(Calendar.MONTH));
    day.setSelectedIndex(gc.get(Calendar.DAY_OF_MONTH) - 1);
    //hour.setSelectedIndex(gc.get(Calendar.HOUR_OF_DAY));
    //min.setSelectedIndex(gc.get(Calendar.MINUTE));
    //sec.setSelectedIndex(gc.get(Calendar.SECOND));
    calcJulDate(); // Sets wday

    /* Create and stuff 3 boxes, one for era components, one for date
       components, and one for time components */
    Box era = Box.createHorizontalBox();
      era.add(ad);
      era.add(Box.createHorizontalStrut(3));
      era.add(bc);
      era.add(Box.createHorizontalGlue());
    Box date = Box.createHorizontalBox();
      date.add(year);
      date.add(Box.createHorizontalStrut(3));
      date.add(month);
      date.add(Box.createHorizontalStrut(3));
      date.add(day);
      date.add(Box.createHorizontalGlue());
    Box time = Box.createHorizontalBox();
      time.add(hour);
      time.add(Box.createHorizontalStrut(3));
      time.add(new JLabel(":"));
      time.add(Box.createHorizontalStrut(3));
      time.add(min);
      time.add(Box.createHorizontalStrut(3));
      time.add(new JLabel(":"));
      time.add(Box.createHorizontalStrut(3));
      time.add(sec);
      time.add(Box.createHorizontalStrut(3));
      time.add(new JLabel("UT"));
      time.add(Box.createHorizontalGlue());

    /* Create a constraints object to control placement
       and set some defaults */
    GridBagConstraints c = new GridBagConstraints();
    c.anchor = GridBagConstraints.WEST; // dft is CENTER
    c.gridwidth = c.gridheight = 1;
    c.weightx = 0.0; c.weighty = 0.0;
    // Dft c.fill = GridBagConstraints.NONE;
    // Dft c.insets = new Insets(0, 0, 0, 0);

    /* Create a JPanel to hold 3 boxes, wday, jday, and 2 buttons */
    JPanel datetime = new JPanel();
    datetime.setLayout(new GridBagLayout());
    //-----
    c.gridx = 0; c.gridy = 0;
    datetime.add(era, c);
    //-----
    c.gridx = 0; c.gridy = 1;
    c.insets = new Insets(5, 0, 5, 0);  // top, left, bottom, right
    datetime.add(date, c);
    //-----
    c.gridx = 0; c.gridy = 2;
    datetime.add(time, c);
    //-----
    c.gridx = 0; c.gridy = 3;
    c.fill = GridBagConstraints.HORIZONTAL;
    datetime.add(wday, c);
    //-----
    c.gridx = 0; c.gridy = 4;
    datetime.add(jday, c);
    //-----
    c.gridx = 0; c.gridy = 5;
    c.insets = new Insets(10, 0, 5, 0);  // top, left, bottom, right
    datetime.add(CalcJul, c);
    //-----
    c.gridx = 0; c.gridy = 6;
    c.insets = new Insets(10, 0, 0, 0); // top, left, bottom, right
    datetime.add(CalcCal, c);

    /* Create a button */
    JButton Close = new JButton("Close");
    Close.setActionCommand("Cancel");
    Close.addActionListener(listener);

    /* Create a box and add button for Close */
    Box b = Box.createHorizontalBox();
    b.add(Close);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(15);
    getContentPane().add(datetime);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(Close);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(year);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates the Julian date.
   */
  private void calcJulDate() {
    // From P. 60 of Meeus book
    int Y = year.getSelectedIndex() + 1;
    int M = month.getSelectedIndex() + 1;
    int D = day.getSelectedIndex() + 1;
    if ( bc.isSelected() )
      Y = 1 - Y;
    if ( M < 3 ) { // If January or February
      Y--;
      M += 12;
    }
    int B = 0;
    // Thursday Oct 4 1582 was followed by Friday Oct 15 1582
    // (GregorianCalendar also follows this)
    if ( Y > 1582 || (Y == 1582 && (M > 10 || (M == 10 && D > 14))) ) {
      int A = Y / 100;
      B = 2 - A + A / 4;
    }
    double julian = (int)(365.25 * (Y + 4716)) + (int)(30.6001 * (M + 1)) +
                    D + B - 1524.5;

    int hr = hour.getSelectedIndex();
    int mn = min.getSelectedIndex();
    int sc = sec.getSelectedIndex();
    julian += ((sc / 60.0 + mn) / 60.0 + hr) / 24.0;
    jday.setText(Double.toString(julian));

    /* Redo calendar date (e.g. Feb 29 -> Mar 1, Apr 31 -> May 1, ...) */
    calcCalDate();   // Calls setDOW
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates the calendar date.
   */
  private void calcCalDate() {
    // From P. 63 of Meeus book
    String jul = jday.getText();
    double julian;
    try {
      julian = Double.parseDouble(jul);
    } catch ( Exception e ) { julian = 0.0; }
    julian = Math.max(0, Math.min(julian, 3442812.0));
    if ( ! jul.equals(Double.toString(julian)) ) {
      jday.setText(Double.toString(julian));
    }

    setDOW(julian);

    julian += 0.5;
    int Z = (int)julian;  // Integer part (always >= 0)
    double F = julian - Z;       // Fractional part
    int A = Z;
    if ( Z >= 2299161 ) {
      int a = (int)((Z - 1867216.25) / 36524.25); // (> 0)
      A += 1 + a - Math.floor(a / 4);
    }

    int B = A + 1524;
    int C = (int)((B - 122.1) / 365.25);
    int D = (int)(365.25 * C);
    int E = (int)((B - D) / 30.6001);

    int dom = B - D - (int)(30.6001 * E);
    int m;
    if ( E < 14 ) { m = E - 1; }
    else          { m = E - 13; }
    int yr;
    if ( m > 2 ) { yr = C - 4716; }
    else         { yr = C - 4715; }
    day.setSelectedIndex(dom - 1);
    month.setSelectedIndex(m - 1);
    if ( yr > 0 ) { ad.setSelected(true); }
    else {
      bc.setSelected(true);
      yr = 1 - yr;   // -1 * (yr - 1);
    }
    year.setSelectedIndex(yr - 1);

    F += .00000001;  // Add approx. .001s to adjust for limited precision
    // E.g. want 12:17:37 -> convert to Julian -> convert to Cal: 12:17:*37*
    if ( F > .99999 ) F = .99999; // If necessary, trim to just over 23:59:59
    F *= 24;
    int hr = (int)(F);
    hour.setSelectedIndex(hr);
    F -= hr;
    F *= 60;
    int mn = (int)(F);
    min.setSelectedIndex(mn);
    F -= mn;
    F *= 60;
    int sc = (int)(F);
    sec.setSelectedIndex(sc);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the day-of-week readonly JTextField.
   * @param j Julian Date
   */
  private void setDOW(double j) {
    int dow = (int)(Math.floor(j + 1.5)) % 7;
    wday.setText(dows[dow + 1]);  // dows: Sunday = 1, ..., Saturday = 7
  }

  /* For testing */
  //static public void main(String[] args) {
  //  JulianDlg dlg = new JulianDlg(null);
  //  dlg.setVisible(true);
  //  System.out.println("Press 'Ctrl-c' to exit");
  //}
}

/*------------------------------------------------------------------------------

From: "Julian Date Converter"  http://aa.usno.navy.mil/data/docs/JulianDate.html

Julian dates (abbreviated JD) are simply a continuous count of days and
fractions since noon Universal Time on January 1, 4713 BCE (on the Julian
calendar). Almost 2.5 million days have transpired since this date. Julian dates
are widely used as time variables within astronomical software. Typically, a
64-bit floating point (double precision) variable can represent an epoch
expressed as a Julian date to about 1 millisecond precision. Note that the time
scale that is the basis for Julian dates is Universal Time, and that 0h UT
corresponds to a Julian date fraction of 0.5.

It is assumed that 7-day weeks have formed an uninterrupted sequence since
ancient times. Thus, the day of the week can be obtained from the remainder of
the division of the Julian date by 7.

Calendar dates - year, month, and day - are more problematic. Various calendar
systems have been in use at different times and places around the world. This
application deals with only two: the Gregorian calendar, now used universally
for civil purposes, and the Julian calendar, its predecessor in the western
world. As used here, the two calendars have identical month names and number of
days in each month, and differ only in the rule for leap years. The Julian
calendar has a leap year every fourth year, while the Gregorian calendar has a
leap year every fourth year except century years not exactly divisible by 400.

This application assumes that the changeover from the Julian calendar to the
Gregorian calendar occurred in October of 1582, according to the scheme
instituted by Pope Gregory XIII. Specifically, for dates on or before 4 October
1582, the Julian calendar is used; for dates on or after 15 October 1582, the
Gregorian calendar is used. Thus, there is a ten-day gap in calendar dates, but
no discontinuity in Julian dates or days of the week: 4 October 1582 (Julian) is
a Thursday, which begins at JD 2299159.5; and 15 October 1582 (Gregorian) is a
Friday, which begins at JD 2299160.5. The omission of ten days of calendar dates
was necessitated by the astronomical error built up by the Julian calendar over
its many centuries of use, due to its too-frequent leap years.

The changeover to the Gregorian calendar system occurred as described above only
in Roman Catholic countries, however. Adoption of the Gregorian calendar in the
rest of the world progressed slowly. For example, for England and its colonies,
the change did not occur until September 1752. (The Unix cal command for systems
manufactured in the U.S. reflects the 1752 changeover.)

- - - - - - - - - - - - - -

On Unix, "cal 9 1752" shows

 S  M Tu  W Th  F  S
       1  2 14 15 16  <- Note days 3-13 missing
17 18 19 20 21 22 23
24 25 26 27 28 29 30

- - - - - - - - - - - - - -

See also http://www.tondering.dk/claus/calendar.html
http://www.tondering.dk/claus/cal/node3.html#SECTION00324000000000000000
http://astro.nmsu.edu/~lhuber/leaphist.html

Info on UT at http://aa.usno.navy.mil/faq/docs/UT.html

------------------------------------------------------------------------------*/

