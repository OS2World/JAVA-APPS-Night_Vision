/*
 * DateTimeDlg.java  -  "Set date/time" dialog
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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
// DateFormatSymbols
// GregorianCalendar


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Set date/time" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class DateTimeDlg extends EscapeDlg implements ItemListener {
  static final private int YR0 = 1000;
  static final private int YRL = 3000;
  private EComboBox month, day, year, hour, min;
  private JCheckBox dst, hr24;
  private JRadioButton am, pm, no;
  private GregorianCalendar gc;
  private ActionListener listeners = null;
  private Preferences prefer;
  static private DateTimeDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new DateTimeDlg(owner, prefer);
    else {
      dlg.loadPrefer();
    }

    dlg.setVisible(true);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Updates the dialog if it has been created.  (Does not matter if it is
   * currently showing.)  Should be called only by LocationDlg.
   */
  public static void updateDlg() {
    if ( dlg != null ) {
      dlg.dst.setSelected(dlg.prefer.inDST());
      dlg.dst.setVisible(dlg.prefer.getDST().equals("A") ? false : true);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private DateTimeDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("DateTimeDlg.Title"), false);
    this.prefer = prefer;
    int i;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          DateTimeDlg.this.close(); // Pop down dialog
        if ( !cmd.equals("Cancel") ) {
          if ( cmd.equals("Cmp") ) updatePrefer2();
          else                     updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(DateTimeDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Create controls for this window */
    DateFormatSymbols dfs = new DateFormatSymbols();
    String[] months = dfs.getMonths();
    // Warning:  Size of months array is 13; last element is blank
    month = new EComboBox();
      for ( i = 0; i < months.length && months[i].length() > 0; i++ )
        month.addItem(months[i]);
      month.addItemListener(this);
    day   = new EComboBox();
    year  = new EComboBox();
      for ( i = YR0; i <= YRL; i++ ) year.addItem(Integer.toString(i));
      year.addItemListener(this);
    hour  = new EComboBox();
    min   = new EComboBox();
      for ( i =  0; i < 60; i++ ) min.addItem("" + i/10 + i%10);
    JLabel Dt = new JLabel(TextBndl.getString("DateTimeDlg.Date"));
    JLabel HM = new JLabel(TextBndl.getString("DateTimeDlg.HrMn"));
    JLabel Cln = new JLabel(TextBndl.getTmSep());  // ":"
    am = new JRadioButton(TextBndl.getString("DateTimeDlg.AM"));
    pm = new JRadioButton(TextBndl.getString("DateTimeDlg.PM"));
    no = new JRadioButton();  // Dummy button
      ButtonGroup bg = new ButtonGroup();
      bg.add(am);
      bg.add(pm);
      bg.add(no);
    hr24 = new JCheckBox(TextBndl.getString("DateTimeDlg.24"));
      hr24.addItemListener(this);
    dst = new JCheckBox(TextBndl.getString("DateTimeDlg.Dst"));
    JButton Cmp = new JButton(TextBndl.getString("DateTimeDlg.Cmp"));
    Cmp.setActionCommand("Cmp");
    Cmp.addActionListener(listener);
    loadPrefer();  // Sets/updates gc and the above controls

    /* Figure out order of date components */
    EComboBox d1 = month, d2 = day, d3 = year;
    String order = TextBndl.getString("DateTimeDlg.Ordr").toUpperCase();//"DMY"
    if ( order.length() == 3 && order.indexOf('M') >= 0 &&
         order.indexOf('D') >= 0 && order.indexOf('Y') >= 0 ) {
      switch ( order.charAt(0) ) {
        case 'M': d1 = month; break;
        case 'D': d1 = day;   break;
        case 'Y': d1 = year;  break;
      }
      switch ( order.charAt(1) ) {
        case 'M': d2 = month; break;
        case 'D': d2 = day;   break;
        case 'Y': d2 = year;  break;
      }
      switch ( order.charAt(2) ) {
        case 'M': d3 = month; break;
        case 'D': d3 = day;   break;
        case 'Y': d3 = year;  break;
      }
    }

    /* Create and stuff 3 boxes, one for date components, one for time
       components, and one for ampm components */
    Box date = Box.createHorizontalBox();
      date.add(Dt);
      date.add(Box.createHorizontalStrut(4));
      date.add(d1);
      date.add(Box.createHorizontalStrut(3));
      date.add(d2);
      date.add(Box.createHorizontalStrut(3));
      date.add(d3);
      date.add(Box.createHorizontalGlue());
    Box time = Box.createHorizontalBox();
      time.add(HM);
      time.add(Box.createHorizontalStrut(4));
      time.add(hour);
      time.add(Box.createHorizontalStrut(2));
      time.add(Cln);
      time.add(Box.createHorizontalStrut(2));
      time.add(min);
      time.add(Box.createHorizontalStrut(6));
      date.add(Box.createHorizontalGlue());
    Box ampm = Box.createHorizontalBox();
      ampm.add(am);
      time.add(Box.createHorizontalStrut(4));
      ampm.add(pm);
      date.add(Box.createHorizontalGlue());

    /* Create a constraints object to control placement
       and set some defaults */
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    c.anchor = GridBagConstraints.WEST; // dft is CENTER
    c.gridwidth = c.gridheight = 1;
    c.weightx = 0.0; c.weighty = 0.0;
    // Dft c.fill = GridBagConstraints.NONE;
    // Dft c.insets = new Insets(0, 0, 0, 0);

    /* Create a JPanel to hold 3 boxes, dst, hr24, and the Cmp button */
    JPanel datetime = new JPanel();
    datetime.setLayout(new GridBagLayout());
    //-----
    c.gridwidth = 2; c.gridheight = 1;
    c.gridx = 0; c.gridy = 0;
    c.insets = new Insets(0, 0, 5, 0);  // top, left, bottom, right
    datetime.add(date, c);
    //-----
    c.gridwidth = 1; c.gridheight = 1;
    c.gridx = 0; c.gridy = 1;
    c.insets = new Insets(5, 0, 5, 2);  // top, left, bottom, right
    datetime.add(time, c);
    //-----
    c.gridx = 1; c.gridy = 1;
    c.insets = new Insets(5, 2, 5, 0);  // top, left, bottom, right
    datetime.add(ampm, c);
    //-----
    c.gridx = 0; c.gridy = 2;
    c.insets = new Insets(5, 0, 5, 2);  // top, left, bottom, right
    datetime.add(dst, c);
    //-----
    c.gridx = 1; c.gridy = 2;
    c.insets = new Insets(5, 2, 5, 0);  // top, left, bottom, right
    datetime.add(hr24, c);
    //-----
    c.gridwidth = 2; c.gridheight = 1;
    c.gridx = 0; c.gridy = 3;
    c.insets = new Insets(10, 0, 0, 0);  // top, left, bottom, right
    c.fill = GridBagConstraints.HORIZONTAL;
    datetime.add(Cmp, c);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"datetime");
    setHelpPage("datetime");
    // Squeeze them a bit
    Insets insets = OK.getMargin();
    insets.left /= 2;
    insets.right /= 2;
    OK.setMargin(insets);
    Apply.setMargin(insets);
    Cancel.setMargin(insets);
    Help.setMargin(insets);

    /* Create a box and add buttons for OK, Apply, Cancel, & Help */
    Box b = Box.createHorizontalBox();
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

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(15);
    getContentPane().add(datetime);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(d1);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns days in month.
   */
  private int daysInMonth(int month, int year) { // month is 0 based
    if ( month > 6 ) month--;                    // > July
    if ( (month % 2) == 0 ) return 31;
    else if ( month != 1 ) return 30;            // ! February
    else {                                       // = February
      return 28 + (gc.isLeapYear(year) ? 1 : 0);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ItemListener interface.
   * Called when month, year, or hr24 has changed.
   */
  public void itemStateChanged(ItemEvent e) {
    if ( e.getSource() == hr24 ) {
      loadHour(hour.getSelectedIndex() + (pm.isSelected() ? 12 : 0),
               hr24.isSelected());
    }
    else {
      int yr = year.getSelectedIndex() + YR0;
      int mn = month.getSelectedIndex();
      int dy = day.getSelectedIndex() + 1;
      int dim = daysInMonth(mn, yr);
      if ( dy > dim ) dy = dim;
      day.removeAllItems();
      for ( int i = 1; i <= dim; i++ ) day.addItem(Integer.toString(i));
      day.setSelectedIndex(dy - 1);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * If h24 = true   ->  24 hour mode
   *          false  ->  12 hour mode
   * 0 <= h <= 23    (h is always specified in 24 hour mode)
   */
  private void loadHour(int h, boolean h24) {
    int i;

    hour.removeAllItems();
    am.setEnabled(!h24);
    pm.setEnabled(!h24);
    if ( h24 ) {
      no.setSelected(true);
      h %= 24;
      for ( i =  0; i < 24; i++ ) hour.addItem("" + i/10 + i%10);
    }
    else {
      if ( h > 11 ) pm.setSelected(true);
      else          am.setSelected(true);
      h %= 12;
      hour.addItem("12");
      for ( i =  1; i < 12; i++ ) hour.addItem("" + i/10 + i%10);
    }
    if ( h < 0 ) h = 0;  // Note:  (-2)%3 yields -2, not +1 as in Perl
    hour.setSelectedIndex(h);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    gc = prefer.getLocDateTime();  // Same GregorianCalendar object
    // always returned;  This call updates the time it represents.

    month.setSelectedIndex(gc.get(Calendar.MONTH)); // 0 based

    day.removeAllItems();
    int dim = daysInMonth(gc.get(Calendar.MONTH), gc.get(Calendar.YEAR));
    for ( int i = 1; i <= dim; i++ ) day.addItem(Integer.toString(i));
    day.setSelectedIndex(gc.get(Calendar.DAY_OF_MONTH) - 1);

    int boundedyear = Math.max(YR0, Math.min(gc.get(Calendar.YEAR), YRL));
    year.setSelectedIndex(boundedyear - YR0);

    min.setSelectedIndex(gc.get(Calendar.MINUTE));

    hr24.setSelected(prefer.is24Hr());

    dst.setSelected(prefer.inDST());
    dst.setVisible(prefer.getDST().equals("A") ? false : true);

    loadHour(gc.get(Calendar.HOUR_OF_DAY), prefer.is24Hr());
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called only by updatePrefer and updatePrefer2 *before* setting the time.
   */
  private void checkDST24Hr() {
    if ( dst.isVisible() ) {   // If dst checkbox is visible, that means
      // that we have manual dst, which is either on or off.
      boolean dstold = prefer.inDST();
      if ( dstold != dst.isSelected() ) {
        TimeZone tz = gc.getTimeZone();
        gc.setTimeZone(dstold ?
                       new SimpleTimeZone(tz.getRawOffset(), tz.getID()) :
                       new NotSoSimpleTimeZone(tz));
      }
    }

    prefer.set24Hr(hr24.isSelected());
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences, called when "OK" activated.
   */
  private void updatePrefer() {
    // Unnecessary to "freshen" gc by calling prefer.getLocDateTime() (again)
    // since the same object is always used by LST in Preferences.
    // (Specifically, any actions with LocationDlg will not cause the
    // object pointed to by gc to not reflect the one in Preferences's LST.)

    checkDST24Hr();

    gc.set(Calendar.YEAR, year.getSelectedIndex() + YR0);
    gc.set(Calendar.MONTH, month.getSelectedIndex());
    gc.set(Calendar.DAY_OF_MONTH, day.getSelectedIndex() + 1);
    gc.set(Calendar.HOUR_OF_DAY, hour.getSelectedIndex() +
           (pm.isSelected() ? 12 : 0));
    gc.set(Calendar.MINUTE, min.getSelectedIndex());
    gc.clear(Calendar.SECOND);
    gc.clear(Calendar.MILLISECOND);
    prefer.setLocDateTime();

    LocationDlg.updateDlg();  // Update LocationDlg with latest dst info
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences, called when "Set to computer ..." activated.
   */
  private void updatePrefer2() {
    checkDST24Hr();

    prefer.setToCompDateTime();

    LocationDlg.updateDlg();  // Update LocationDlg with latest dst info
  }

  /* For testing */
  //static public void main(String[] args) {
  //  Preferences prefer = new Preferences();
  //  DateTimeDlg dlg = new DateTimeDlg(null, prefer);
  //  dlg.setVisible(true);
  //  System.out.println("Press 'Ctrl-c' to exit");
  //}
}

/*------------------------------------------------------------------------------

2-19-02 test on my slow P90 showed that a significant amount of time
during construction is taken by adding the items to the JComboBoxes.
Tried an experiment where JComboBox was created with array of Integers
(for year) already there, but there was no speedup, perhaps a slowdown.

------------------------------------------------------------------------------*/

