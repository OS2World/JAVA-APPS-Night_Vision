/*
 * InitDlg.java  -  Initialization dialog
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * MultiLineLabel.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
class MultiLineLabel extends JPanel {
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public MultiLineLabel(String s) {
    String line;
    boolean nline = true;  // Used so that "\nNight\n\nVision"
                           // will be interpreted as 4 lines:
                           // "<blank>", "Night", "<blank>", "Vision"

    /* Create a dummy button for the sole purpose of getting its color,
       as its color is usually darker than that of a standard JLabel */
    JButton dummy = new JButton("dummy");
    Color clr = dummy.getForeground();

    Box a = Box.createVerticalBox();
    StringTokenizer t = new StringTokenizer(s, "\n", true);
    int num_lines = t.countTokens();
    while ( num_lines-- > 0 ) {
      line = t.nextToken();
      if ( line.equals("\n") ) {
        if ( nline == false ) nline = true;
        else
          a.add(new JLabel(" ")); // Must be at least 1 space
      }
      else {
        JLabel l = new JLabel(line);
        l.setForeground(clr);
        a.add(l);
        nline = false;
      }
    }
    add(a);
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Initialization dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class InitDlg extends JDialog {
  private JLabel message;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param info (Multi-line) text that is placed to right of image
   * @param msg (Single-line) text that is placed at bottom of window
   */
  public InitDlg(String info, String msg) {
    /* Set window name */
    super((Frame)null, Nvj.PgmName, false);

    /* Handle closing (change dft HIDE_ON_CLOSE) */
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    /* Handle Escape */
    addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
          System.exit(0);
        }
      }
    });

    /* Get comet image */
    JLabel imagelabel = new JLabel(Nvj.getImageIcon("/com/nvastro/nvj/hb.jpg"));

    /* Create controls for this window */
    MultiLineLabel mll = new MultiLineLabel(info);
    message = new JLabel(msg, SwingConstants.CENTER);

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(12, 4, 12, 4));
    ((BorderLayout)getContentPane().getLayout()).setVgap(10);
    JPanel imgtxt = new JPanel(); // Has FlowLayout
    ((FlowLayout)(imgtxt.getLayout())).setHgap(10);
    imgtxt.add(imagelabel);
    imgtxt.add(mll);
    getContentPane().add(imgtxt, BorderLayout.CENTER);
    getContentPane().add(message, BorderLayout.SOUTH);

    /* Set its icon */
    // Note: setIconImage is a Frame method, but this is a JDialog;
    // nevertheless it works on Linux and Windoze and some Mac/Java
    // combinations, but caused a fatal exception on one Mac.
    // Perhaps switch to a JFrame to avoid the try block.
    try {
      setIconImage(Nvj.getImageIcon("/com/nvastro/nvj/hbsmall.jpg").getImage());
    } catch (NoSuchMethodError e) {}

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width  - getWidth())  / 2;
    int y = (dim.height - getHeight()) / 2;
    setLocation(x, y);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the message at the bottom of the window.
   *
   * @param s Message
   */
  public void setMessage(String s) {
    message.setText(s);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Closes (disposes) the window.
   *
   * @param ms Delay in milliseconds to delay closing (thread sleeps)
   */
  public void closemsec(int ms) {
    if ( ms > 0 ) {
      try {
        Thread.sleep(ms);
      } catch (Exception e) {}
    }
    dispose();
  }

  /* For testing */
  //public static void main(String[] args) {
  //  String PgmInfo = "N i g h t   V i s i o n\n\n" +
  //                   "Version 0.0\n\n" +
  //                   "Copyright (C) B. Simpson 2003\n\n" +
  //                   "Astronomy Program for Java";
  //  InitDlg dlg = new InitDlg(PgmInfo, "Starting...");
  //  dlg.show();
  //  try { Thread.currentThread().sleep(2000); } catch (Exception e) {}
  //  dlg.setMessage("Update 1!");
  //  try { Thread.currentThread().sleep(2000); } catch (Exception e) {}
  //  dlg.setMessage("Update 2!");
  //
  //  dlg.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //}
}

