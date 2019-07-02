/*
 * IdentifyDlg.java  -  "Object identification" dialog
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Object identification" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class IdentifyDlg extends EscapeDlg implements Runnable {
  static final public String TYPE   = TextBndl.getStringS2("IdentifyDlg.Type");
  static final public String PLANET = TextBndl.getString("IdentifyDlg.Type.Pl");
  static final public String MOON   = TextBndl.getString("IdentifyDlg.Type.Mn");
  static final public String ASTEROID=TextBndl.getString("IdentifyDlg.Type.As");
  static final public String COMET  = TextBndl.getString("IdentifyDlg.Type.Co");
  static final public String STAR   = TextBndl.getString("IdentifyDlg.Type.St");
  static final public String NAME   = TextBndl.getStringS2("IdentifyDlg.Name");
  static final public String DESGN  = TextBndl.getStringS2("IdentifyDlg.Desgn");
  static final public String MAG    = TextBndl.getStringS2("IdentifyDlg.Mag");
  static final public String SPECT  = TextBndl.getStringS2("IdentifyDlg.Spect");
  static final public String DIST   = TextBndl.getStringS2("IdentifyDlg.Dist");
  static final public String DISTAU = TextBndl.getString("IdentifyDlg.DistAU");
  static final public String DISTKM = TextBndl.getString("IdentifyDlg.DistKM");
  static final public String ANGSZ  = TextBndl.getStringS2("IdentifyDlg.AngSz");
  static final public String RA    = TextBndl.getStringS2("IdentifyDlg.RA");
  static final public String DEC   = TextBndl.getStringS2("IdentifyDlg.Dec");
  static final public String AZ    = TextBndl.getStringS2("IdentifyDlg.Az");
  static final public String ALT   = TextBndl.getStringS2("IdentifyDlg.Alt");
  static final public String TIME  = TextBndl.getStringS2("IdentifyDlg.Time");
  static final public String SEP   = TextBndl.getStringS2("IdentifyDlg.Sep");
  static final public String NOOBJ = TextBndl.getString("IdentifyDlg.NoObj");
  static final public String ILLUM = TextBndl.getStringS2("IdentifyDlg.Illum");
  static final public String NEW   = TextBndl.getString("IdentifyDlg.New");
  static final public String WAX   = TextBndl.getString("IdentifyDlg.Wax");
  static final public String FULL  = TextBndl.getString("IdentifyDlg.Full");
  static final public String WANE  = TextBndl.getString("IdentifyDlg.Wane");
  static final public String NEARSTAR = TextBndl.getStringS2(
                                        "IdentifyDlg.NearStar");
  static final public String J2000RA  = TextBndl.getString("IdentifyDlg.J2000")
                                        + " " + RA;
  static final public String J2000DEC = TextBndl.getString("IdentifyDlg.J2000")
                                        + " " + DEC;
  static final public String RAHour = TextBndl.getString("IdentifyDlg.RAHour");
  static final public String RAMin  = TextBndl.getString("IdentifyDlg.RAMin");
  static final public String RASec  = TextBndl.getString("IdentifyDlg.RASec");
  static final public String Deg    = TextBndl.getString("IdentifyDlg.Deg");
  static final public String Min    = TextBndl.getString("IdentifyDlg.Min");
  static final public String Sec    = TextBndl.getString("IdentifyDlg.Sec");
  static private IdentifyDlg dlg = null;
  static private PrintWriter pw = null;
  private ROTextArea ta = null;
  private String SlewOut; // Name of output pipe
  private JButton Slew;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner The owner window (main window)
   * @param data The data to display in the window
   * @param pt The location of the object in screen coordinates
   */
  public static void showDlg(Frame owner, String data, Point pt) {
    if ( dlg == null ) dlg = new IdentifyDlg(owner, data, pt);
    else {
      dlg.ta.setText(data);
      dlg.ta.setCaretPosition(0);     // Make sure 1st line visible
      if ( ! dlg.isShowing() ) dlg.setLocationRelativeTo(owner, pt);
    }
    dlg.setSlew(data);
    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner The owner window (main window)
   * @param data The data to display in the window
   * @param pt The location of the object in screen coordinates
   */
  private IdentifyDlg(final Frame owner, String data, Point pt) {
    /* Set window name */
    super(owner, TextBndl.getString("IdentifyDlg.Title"), false);

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if ( e.getActionCommand().equals("Close") )
          IdentifyDlg.this.close(); // Pop down dialog & clearIDMarker
        else
          synchronized ( IdentifyDlg.this.Slew ) {
            IdentifyDlg.this.Slew.notify();
          }
      }
    };
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { // Called when closed
        ((Nvj)owner).clearIDMarker();            //   with system menu
      }
    });

    /* Get data to display (testing purposes...) */
    //data = "First line\nSecond line\nThird line\n4\n5\n6\n7\n8\n9\n" +
                  //"An extremely long line.....An Extremely long line. " +
                  //"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW";

    /* Set SlewOut */
    SlewOut = Preferences.SlewOut();
    if ( SlewOut != null && SlewOut.length() == 0 ) SlewOut = null;

    /* Create controls for this window */
    ta = new ROTextArea(data, 7, 30);
    ta.setCaretPosition(0);      // Make sure 1st line visible

    /* Create some buttons */
    JButton Close = new JButton(TextBndl.getString("Dlg.Close"));
    Close.addActionListener(listener);
    Close.setActionCommand("Close");
    Slew = new JButton(TextBndl.getString("IdentifyDlg.Slew"));
    Slew.setActionCommand("Slew");
    Slew.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "id");
    setHelpPage("id");

    /* Create a Box and add buttons for Close & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(Close);
    b.add(Box.createHorizontalStrut(10));
    b.add(Box.createHorizontalGlue());
    if ( SlewOut != null ) {
      b.add(Slew);
      b.add(Box.createHorizontalStrut(10));
      b.add(Box.createHorizontalGlue());
    }
    b.add(Help);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(ta);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(Close);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(true);
    setLocationRelativeTo(owner, pt);

    /* Set which component receives focus first */
    setFirstFocus(ta);

    /* Set up 2nd thread if Slewing enabled */
    if ( SlewOut != null ) {
      Thread imageBuilder = new Thread(this);
      imageBuilder.setDaemon(true);
      imageBuilder.start();
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override of close to ensure that IDMarker is cleared.
   */
  public void close() {
    super.close();
    ((Nvj)owner).clearIDMarker();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set slew info.
   */
  private void setSlew(String data) {
    String name = "", type = "", des = "", ra = "", dec = "", mag = "";
    boolean below = true;

    // Here is an example of what is in data:
    // Type:  Star
    // Name:  Betelgeuse
    // Designation(s):  Alpha Ori, 58 Ori
    // Spectral:  M0
    // Magnitude:  0.57
    // Right Ascension:  5h 55m 50.6s
    // Declination:  7d 24m 22s
    // Azimuth:  115d 5m
    // Altitude:  37d 1m
    // Time:  2012/04/14 14:15
    // J2000 Right Ascension:  5h 55m 10.3s
    // J2000 Declination:  7d 24m 25s
    // Angular separation from
    // previous identify:  64d 47m 28s

    // Note:  Need to replace with text from Text.properties

    String[] texts = data.split("\n");
    for ( int i = 0; i < texts.length; i++ ) {
      if      ( texts[i].startsWith(NAME) ) {
        name  = texts[i].substring(NAME.length()).trim();
      }
      else if ( texts[i].startsWith(DESGN) ) {
        des   = texts[i].substring(DESGN.length()).trim();
      }
      else if ( texts[i].startsWith(TYPE) ) {
        type  = texts[i].substring(TYPE.length()).trim();
      }
      else if ( texts[i].startsWith(J2000RA) ) {
        ra    = texts[i].substring(J2000RA.length()).trim();
      }
      else if ( texts[i].startsWith(J2000DEC) ) {
        dec   = texts[i].substring(J2000DEC.length()).trim();
      }
      else if ( texts[i].startsWith(MAG) ) {
        mag   = texts[i].substring(MAG.length()).trim();
      }
      else if ( texts[i].startsWith(ALT) ) {
        below = texts[i].substring(ALT.length()).matches(".*-.*" ) ?
                true : false;
      }
    }

    if ( name.length() == 0 && (des.length() != 0 || type.length() != 0) ) {
      if ( des.length() != 0 ) name = des;
      else                     name = type;
    }
    name = name.replaceFirst(",.*", "").trim();

    if ( name.length() != 0 && ra.length() != 0 &&
         dec.length() != 0 && !below ) {
      ra = ra.replaceAll("[hm] ", ":");
      ra = ra.replaceAll("s", "");
      dec = dec.replaceAll("[dm] ", ":");
      dec = dec.replaceAll("s", "");
      name = name + ",," + ra + "," + dec + "," + mag + ",2000,0";

      synchronized ( Slew ) {
        Slew.setActionCommand(name);
      }
      Slew.setEnabled(true);
    }
    else
      Slew.setEnabled(false);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * 2nd thread writes slew info.
   */
  public void run() {
    String output;

    Thread me = Thread.currentThread();
    me.setPriority(me.getPriority() - 2);

    while ( true ) {
      synchronized ( Slew ) {
        try { Slew.wait(); } catch ( Exception e ) {} // Wait for notify
        output = Slew.getActionCommand();
      }

      if ( pw == null ) {
        try {
          // Thread will hang here if SlewOut is a pipe
          // and there is not a reader
          pw = new PrintWriter(new BufferedOutputStream(
               new FileOutputStream(SlewOut)));
        } catch(Exception e) {}
      }
      if ( pw != null ) {
        pw.println(output);
        pw.flush();
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Positions window to owner, avoiding the object clicked on.
   *
   * @param owner The owner window (main window)
   * @param pt Screen coordinates of object clicked on
   */
  public void setLocationRelativeTo(Frame owner, Point pt) {
    if ( dlg == null ) super.setLocationRelativeTo(owner);

    if ( pt.x < 0 || pt.y < 0 ) return;  // Object no longer visible,
                  // no need to worry about covering it up

    /* getLocationOnScreen() causes an exception since window is
       not yet showing.  However getLocation() does work, but not as
       advertized.  It returns screen location, not location relative
       to owner.  (Maybe because it isn't showing yet?) */
    Point window = getLocation();  // Screen coordinates of window
    //stem.out.println("x = " + window.x + ", y = " + window.y);
    int width = getWidth();
    int height = getHeight();
    int buffer = width / 20;   // Buffer zone set at dlg width / 20
    if ( window.x < pt.x + buffer &&
         window.x + width > pt.x - buffer &&
         window.y < pt.y + buffer &&
         window.y + height > pt.y - buffer ) {
      int midpt = window.x + width / 2;
      int scrnwidth = Nvj.dimScrn.width;
      if ( midpt < pt.x ) {          // If on left side of pt
        if ( pt.x - width - buffer > 0 )
          window.x = pt.x - width - buffer;
        else
          window.x = pt.x + buffer;
      }
      else {                         // Else of right side of pt
        if ( pt.x + width + buffer < scrnwidth )
          window.x = pt.x + buffer;
        else
          window.x = pt.x - width - buffer;
      }
      setLocation(window);  // Normally relative to owner, but since
      // this dlg is not yet showing, relative to screen
    }
  }
}

