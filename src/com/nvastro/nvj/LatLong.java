/*
 * LatLong.java  -  Latitude and Longitude component
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.text.MessageFormat;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * The following class describes the latitude or longitude component
 * used by the LocationDlg class.  Additionally (temporarily?) it can
 * be used to display timezone offset.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class LatLong extends JPanel
                     implements DocumentListener, ActionListener {
  // SWITCH TO enum WHEN MINIMUM JAVA REQ'D IS UPPED TO 1.5!
  static final int LAT = 0;
  static final int LNG = 1;
  static final int TMZ = 2;
  private int type; // LAT, LNG, or TMZ
  private int max; // Set to 90, 180, or 13 based on type
  private FixedLengthTextField deg, min, sec;
  private JRadioButton rb1, rb2;  // "N" or "W" or "-", "S" or "E" or "+"
  private boolean ignoreUpdate;  // To inhibit response back to LocationDlg
  static final private String HRSYM  = TextBndl.getHrSym();  // "h"
  static final private String DEGSYM = TextBndl.getDegSym(); // "d"
  static final private String MINSYM = TextBndl.getMinSym(); // "m"
  static final private String SECSYM = TextBndl.getSecSym(); // "s"
  static final private double LARGENUM = 1000000;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public LatLong(int type) {
    this.type = type;
    boolean secondsFld = false; // Eventually will be set to true!

    ignoreUpdate = true;  // Don't send an update response back to LocationDlg

    if      ( type == LAT ) max =  90;
    else if ( type == LNG ) max = 180;
    else                  { max =  13; secondsFld = false; }

    AlignableBox hbox = AlignableBox.createHorzAlignableBox();
    if ( secondsFld )
      sec = new FixedLengthTextField("0.0", 59, secondsFld);
    min = new FixedLengthTextField("0", 59, false);
    deg = new FixedLengthTextField("0", max, false);
    hbox.add(deg);
    if ( type != TMZ ) hbox.add(new JLabel(DEGSYM));
    else               hbox.add(new JLabel(HRSYM));
    hbox.add(Box.createHorizontalStrut(4));
    hbox.add(min);
    hbox.add(new JLabel(MINSYM));
    if ( secondsFld ) {
      hbox.add(Box.createHorizontalStrut(4));
      hbox.add(sec);
      hbox.add(new JLabel(SECSYM));
    }
    hbox.add(Box.createHorizontalStrut(10));

    if ( type == LAT || type == LNG ) {
      rb1 = new JRadioButton((type == LAT) ? "N" : "W");
      // The following code line s/b a choice of "S" or "E"
      // but is done this way to ensure leftmost radio buttons
      // are the same width; Fixed with setText below
      rb2 = new JRadioButton((type == LAT) ? "W" : "N");
      Dimension dim1 = rb1.getPreferredSize();
      Dimension dim2 = rb2.getPreferredSize();
      if ( dim1.width < dim2.width ) {
        dim1.width = dim2.width;
        rb1.setPreferredSize(dim1);
        rb1.setMinimumSize(dim1);
      }
      rb2.setText((type == LAT) ? "S" : "E");
    }
    else {
      rb1 = new JRadioButton("-");
      rb2 = new JRadioButton("+");
    }
    hbox.add(rb1);
    hbox.add(Box.createHorizontalStrut(2));
    hbox.add(rb2);
    ButtonGroup bg = new ButtonGroup();
    bg.add(rb1);
    bg.add(rb2);
    rb1.setSelected(true);
    rb1.addActionListener(this);
    rb2.addActionListener(this);

    add(hbox);

    ignoreUpdate = false;  // Restore normal functionality
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Utility method to create an error string with the supplied value.
   * @param key Identifier for string in Text.properties
   * @param val Value to be inserted into the string
   */
  private String format(String key, double val) {
    String str;
    Object[] args = { new Double(val) };
    try {
      str = MessageFormat.format(TextBndl.getString(key), args);
    } catch ( Exception e ) { str = key; }
    return str;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called to hilite a field if there is an error.
   * @param fld Field to hilite
   */
  private void hiliteField(JTextField fld) {
    // fld.selectAll() puts cursor after hilite so do this instead
    fld.setCaretPosition(fld.getText().length());
    fld.moveCaretPosition(0);
    fld.requestFocus();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by LocationDlg when new coordinates are to be accepted.  An error
   * string is returned if there is an error.
   */
  public String getValue(double[] val) {
    double d, m, s;
    boolean minus;

    try {
      d = Double.valueOf(deg.getText()).doubleValue();
    } catch ( Exception e ) {
      d = 0;  // Can be here if field is blank
      deg.setText("0");
    }
    if ( d > max ) {
      hiliteField(deg);
      return new String((type == TMZ) ?
                        TextBndl.getString("LocationDlgE.HrsBig") :
                        format("LocationDlgE.DegBig", max));
    }

    try {
      m = Double.valueOf(min.getText()).doubleValue();
    } catch ( Exception e ) {
      m = 0;  // Can be here if field is blank
      min.setText("0");
    }
    if ( m > 0 && d == max ) {
      hiliteField(min);
      return new String((type == TMZ) ?
                        TextBndl.getString("LocationDlgE.HrsMn0") :
                        format("LocationDlgE.DegMn0", max));
    }
    if ( m > 59 ) {
      hiliteField(min);
      return new String(TextBndl.getString("LocationDlgE.MinBig"));
    }

    try {
      s = (sec == null) ? 0.0 :
          Double.valueOf(sec.getText()).doubleValue();
    } catch ( Exception e ) {
      s = 0;  // Can be here if field is blank
      sec.setText("0.0");
    }
    if ( s > 0 && d == max ) {
      hiliteField(sec);
      return new String(format("LocationDlgE.DegSec", max));
    }
    if ( s > 59.9 ) {
      hiliteField(sec);
      return new String(TextBndl.getString("LocationDlgE.SecBig"));
    }

    val[0] = (((s / 60) + m) / 60) + d;
    if ( val[0] != 0 ) {
      minus = (type == LAT) ? rb2.isSelected() : rb1.isSelected();
      if ( minus ) val[0] *= -1.0;
    }

    return new String("");  // No error string to report
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by LocationDlg to get a value to forward on to the worldmap.
   */
  public double getValue() {
    double d, m, s, val;
    boolean minus;

    try {
      d = Double.valueOf(deg.getText()).doubleValue();
    } catch ( Exception e ) {
      d = 0;  // Can be here if field is blank
    }
    if ( d > max ) {
      return LARGENUM;
    }

    try {
      m = Double.valueOf(min.getText()).doubleValue();
    } catch ( Exception e ) {
      m = 0;  // Can be here if field is blank
    }
    if ( (m > 0 && d == max) || m > 59 ) {
      return LARGENUM;
    }

    try {
      s = (sec == null) ? 0.0 :
          Double.valueOf(sec.getText()).doubleValue();
    } catch ( Exception e ) {
      s = 0;  // Can be here if field is blank
    }
    if ( (s > 0 && d == max) || s > 59.9 ) {
      return LARGENUM;
    }

    val = (((s / 60) + m) / 60) + d;
    if ( val != 0 ) {
      minus = (type == LAT) ? rb2.isSelected() : rb1.isSelected();
      if ( minus ) val *= -1.0;
    }

    return val;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by LocationDlg to get a value in the fields.
   */
  public void setValue(double val)
  {
    int d, m, s, t;
    boolean minus = false;

    ignoreUpdate = true;  // Don't send an update response back to LocationDlg

    if ( val < 0.0 ) {
      val *= -1.0;
      minus = true;
    }
    if ( val > max ) val = max;

    d = (int)Math.floor(val);
    val = (val - d) * 60;
    m = (int)Math.floor(val);
    val = (val - m) * 60;
    s = (int)Math.floor(val);
    val = (val - s) * 10;
    t = (int)Math.round(val);
    if ( t == 10 ) {
      t = 0; m++;
      if ( m == 60 ) {
        m = 0; d++;
      }
    }

    // Write to GUI fields
    if ( sec != null ) {
      sec.setText(Integer.toString(s) + "." +
                  Integer.toString(t));
    } else if ( s >= 30 ) {
      m++;
      if ( m == 60 ) {
        m = 0; d++;
      }
    }
    min.setText(Integer.toString(m));
    deg.setText(Integer.toString(d));
    if ( !(type == LAT) ) minus = !minus;
    if ( minus ) rb2.setSelected(true);
    else         rb1.setSelected(true);

    ignoreUpdate = false;  // Restore normal functionality
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called by LocationDlg to set the focus in the degree field.  This is
   * done when the focus is in the location list area and the worldmap
   * has been clicked upon, and is to move the focus to the coordinates area.
   */
  public void setFocusDeg() {
    deg.requestFocusInWindow();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements 1/3 of DocumentListener.  Called during insert in JTextField.
   */
  public void insertUpdate(DocumentEvent e) {
    informParentOfChange();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements 1/3 of DocumentListener.  Called during delete in JTextField.
   */
  public void removeUpdate(DocumentEvent e) {
    informParentOfChange();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements 1/3 of DocumentListener.  Not useful here.
   */
  public void changedUpdate(DocumentEvent e) { }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements ActionListener.  Called when JRadioButtons clicked.
   */
  public void actionPerformed(ActionEvent e) {
    informParentOfChange();
  }

  private void informParentOfChange() {
    if ( ignoreUpdate ) return;

    Container parent = this;

    while ( (parent = parent.getParent()) != null )
      if ( parent instanceof LocationDlg ) {
        ((LocationDlg)parent).coordChanged();
        break;
      }
  }

  /**
   * Called by LocationDlg so that its focusListener will receive FocusEvents
   * from this component.
   */
  public void addFocusListener(FocusListener focusListener) {
    super.addFocusListener(focusListener);
    deg.addFocusListener(focusListener);
    min.addFocusListener(focusListener);
    if ( sec != null )
      sec.addFocusListener(focusListener);
    rb1.addFocusListener(focusListener);
    rb2.addFocusListener(focusListener);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * The following inner class is a fixed length subclass of the
   * JTextField, utilizing the FixedLengthDocument model below.
   */
  class FixedLengthTextField extends JTextField {
    /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
     * Constructor.
     */
    public FixedLengthTextField(int maxnum, boolean secondsFld) {
      this("0", maxnum, secondsFld);
    }

    /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
     * Constructor.
     */
    public FixedLengthTextField(String text, int maxnum, boolean secondsFld) {
      super(new FixedLengthDocument(maxnum, secondsFld), text, 0);
      setHorizontalAlignment(JTextField.RIGHT);

      // Set width (number of columns)
      if ( maxnum == 180 || maxnum == 90 ) {
        setColumns(3); // Want lat & lng degree fields to be same width
      }
      else {
        if ( secondsFld ) setColumns(3); // Wide enough for "59.9"
        else              setColumns(2);
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * The following inner class describes the document model for
   * the lng and lat text fields, which limit # of characters
   * to a fixed length.  Used in the FixedLengthTextField above.
   */
  class FixedLengthDocument extends PlainDocument {
    private int maxLength;  // Max chars in field
    private boolean secondsFld; // True if this is for a seconds field

    /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
     * Constructor.
     */
    public FixedLengthDocument(int maxNum, boolean secondsFld) {
      // Expecting maxNum to be 180, 90, or 59; ignored if secondsFld true
      this.secondsFld = secondsFld;
      if ( secondsFld )         this.maxLength = 4; // "##.#"
      else if ( maxNum == 180 ) this.maxLength = 3;
      else                      this.maxLength = 2;
      addDocumentListener(LatLong.this);
    }

    /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
     * Override of insertString.
     */
    public void insertString(int offset, String str, AttributeSet a)
                throws BadLocationException {
      if (str == null) { return; }

      if ( getLength() + str.length() > maxLength ) {
        Toolkit.getDefaultToolkit().beep();
      }
      else {
        for ( int i = 0; i < str.length(); i++ ) {
          char c = str.charAt(i);
          if ( !Character.isDigit(c) && (c != '.' || !secondsFld) ) {
            Toolkit.getDefaultToolkit().beep();
            return;
          }
        }
        super.insertString(offset, str, a);
      }
    }
  }

  /* For testing */
  //public static void main(String[] args) {
  //  JFrame frame = new JFrame("LatLong test");
  //  Container cp = frame.getContentPane();
  //  AlignableBox vbox = AlignableBox.createVertAlignableBox();
  //
  //  vbox.add(new JLabel(" Latitude:"));
  //  LatLong lat = new LatLong(LAT);
  //  lat.setAlignmentX(0.0f);
  //  vbox.add(lat);
  //
  //  vbox.add(new JLabel(" Longitude:"));
  //  LatLong lng = new LatLong(LNG);
  //  lng.setAlignmentX(0.0f);
  //  vbox.add(lng);
  //
  //  vbox.add(new JLabel(" Timezone:"));
  //  LatLong tmz = new LatLong(TMZ);
  //  tmz.setAlignmentX(0.0f);
  //  vbox.add(tmz);
  //
  //  cp.add(vbox);
  //
  //  frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  //  frame.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //
  //  lat.setValue(-50);
  //  lng.setValue(-99);
  //  tmz.setValue(-11);
  //  frame.pack();
  //  frame.setVisible(true);
  //  double[] val = new double[1];
  //  lat.getValue(val);
  //  System.out.println("Lat = " + val[0]);
  //  lng.getValue(val);
  //  System.out.println("Lng = " + val[0]);
  //  tmz.getValue(val);
  //  System.out.println("Tmz = " + val[0]);
  //}
}

