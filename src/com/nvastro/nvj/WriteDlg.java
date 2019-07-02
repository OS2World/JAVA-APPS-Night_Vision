/*
 * WriteDlg.java  -  "Write user files" dialog
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.MessageFormat;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Write user files" dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class WriteDlg extends EscapeDlg implements ItemListener {
  private JCheckBox locations, starnames, deepsky;
  private JButton OK;
  static private WriteDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   */
  public static void showDlg(Frame owner) {
    if ( dlg == null ) dlg = new WriteDlg(owner);
    else {
      dlg.locations.setSelected(false);
      dlg.starnames.setSelected(false);
      dlg.deepsky.setSelected(false);
    }

    dlg.setVisible(true);
    dlg.locations.requestFocus();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   */
  private WriteDlg(Frame owner) {
    /* Set window name */
    super(owner, TextBndl.getString("WriteDlg.Title"), false);

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if ( e.getActionCommand().equals("OK") && ! writeFiles() ) return;
        WriteDlg.this.close(); // Pop down dialog
      }
    };

    /* Create controls for this window */
    locations = new JCheckBox(format("WriteDlg.loc", CityDB.SOURCE));
    starnames = new JCheckBox(format("WriteDlg.stn", StarNameDB.SOURCE));
    deepsky   = new JCheckBox(format("WriteDlg.ds",  DeepSkyDB.SOURCE));
    locations.addItemListener(this);
    starnames.addItemListener(this);
    deepsky.addItemListener(this);
    JLabel text = new JLabel(TextBndl.getString("WriteDlg.Text"));

    /* Create a box to hold labels and comboboxes */
    Box a = Box.createVerticalBox();
    a.add(locations);
    a.add(Box.createVerticalStrut(6));
    a.add(starnames);
    a.add(Box.createVerticalStrut(6));
    a.add(deepsky);
    a.add(Box.createVerticalStrut(8));
    a.add(text);

    /* Create some buttons */
    OK = new JButton(TextBndl.getString("WriteDlg.Write"));
    OK.setActionCommand("OK");
    OK.addActionListener(listener);
    OK.setEnabled(false);            // OK initially disabled
    JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
    Cancel.setActionCommand("Cancel");
    Cancel.addActionListener(listener);
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"write");
    setHelpPage("write");

    /* Create a box and add buttons for OK, Cancel, & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(OK);
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
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(locations);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements the ItemListener interface.
   */
  public void itemStateChanged(ItemEvent e) {
    OK.setEnabled(locations.isSelected() || starnames.isSelected() ||
         deepsky.isSelected());
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   *
   */
  private String format(String key, String file) {
    String str;
    Object[] args = { file };
    try {
      str = MessageFormat.format(TextBndl.getString(key), args);
    } catch ( Exception e ) { str = key; }
    return str;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns true for (at least 1) successful write, otherwise false
   */
  private boolean writeFiles() {
    StringBuffer s = new StringBuffer();

    /* If target directory specified but doesn't exist, create it */
    if ( Nvj.workingDir != null && !Nvj.workingDir.equals("") &&
         !Nvj.workingDir.equals(".") )
      if ( !(new File(Nvj.workingDir).exists()) )
        if ( !(new File(Nvj.workingDir).mkdirs()) ) {
          OptionDlg.showMessageDialog(this,
                                      format("WriteDlg2.ErrDir",
                                             Nvj.workingDir),
                                      TextBndl.getString("WriteDlg2.Title"),
                                      JOptionPane.ERROR_MESSAGE);
          return false;
        }

    if ( locations.isSelected() && writeFile(CityDB.SOURCE) )
      s.append("\n  " + CityDB.SOURCE);
    if ( starnames.isSelected() && writeFile(StarNameDB.SOURCE) )
      s.append("\n  " + StarNameDB.SOURCE);
    if ( deepsky.isSelected()   && writeFile(DeepSkyDB.SOURCE) )
      s.append("\n  " + DeepSkyDB.SOURCE);

    if ( s.length() > 0 ) {
      if ( Nvj.workingDir != null && !Nvj.workingDir.equals("") &&
           !Nvj.workingDir.equals(".") )
        s.append("\n" + TextBndl.getString("WriteDlg3.Dir") + " " +
                 Nvj.workingDir );

      OptionDlg.showMessageDialog(this,
                                  TextBndl.getString("WriteDlg3.Written") + s,
                                  TextBndl.getString("WriteDlg3.Title"),
                                  JOptionPane.INFORMATION_MESSAGE);
      return true;
    }
    else return false;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Returns true for successful write, otherwise false
   */
  private boolean writeFile(String file) {
    String line;
    BufferedReader in = null;
    PrintWriter out = null;
    boolean rc = true;

    File target = new File(Nvj.workingDir, file);
    if ( target.exists() ) {
      if ( !target.isFile() || !target.canWrite() ) {
        OptionDlg.showMessageDialog(this,
                                    format("WriteDlg2.CantWrite", file),
                                    TextBndl.getString("WriteDlg2.Title"),
                                    JOptionPane.ERROR_MESSAGE);
        rc = false;
      }
      else if ( OptionDlg.showConfirmDialog(this,
                                     format("WriteDlg2.Exists", file),
                                     TextBndl.getString("WriteDlg2.Title"),
                                     JOptionPane.YES_NO_OPTION,
                                     JOptionPane.QUESTION_MESSAGE)
           != JOptionPane.YES_OPTION ) {
           // != YES_OPTION correcty handles Esc pressed,
           // == NO_OPTION does not
        rc = false;
      }
    }

    if ( rc ) {
      try {
        out = new PrintWriter(new FileWriter(target));

        in = new BufferedReader(new InputStreamReader(
                 Nvj.class.getResourceAsStream("/com/nvastro/nvj/" + file)));
        while ( (line = in.readLine()) != null ) {  // Can throw IOException
          /* line will not contain \r or \n or \0 */
          out.println(line);  // Adds the appropriate new line char(s)
        }
      }
      catch ( Exception e ) {
        OptionDlg.showMessageDialog(this,
                                    format("WriteDlg2.ErrWrite", file),
                                    TextBndl.getString("WriteDlg2.Title"),
                                    JOptionPane.ERROR_MESSAGE);
        rc = false;
      }
      finally {
        if ( in  != null ) try {  in.close(); } catch(IOException e) { ; }
        if ( out != null ) /* */ out.close(); //catch(IOException e) { ; }
      }
    }
    return rc;
  }

  /* For testing */
  //public static void main(String[] args) {
  //  final JFrame frame = new JFrame("Dialog tester");
  //
  //  JButton btn1 = new JButton("Press this button to bring up dialog");
  //  btn1.addActionListener(new ActionListener() {
  //    public void actionPerformed(ActionEvent e) {
  //      WriteDlg.showDlg(frame);
  //    }
  //  });
  //  frame.getContentPane().add(btn1);
  //
  //  JButton btn2 = new JButton("Quit");
  //  btn2.addActionListener(new ActionListener() {
  //    public void actionPerformed(ActionEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //  frame.getContentPane().add(btn2, BorderLayout.SOUTH);
  //
  //  frame.addWindowListener(new WindowAdapter() {
  //    public void windowClosing(WindowEvent e) {
  //      System.exit(0);
  //    }
  //  });
  //  frame.pack();
  //  frame.show();
  //}
}

