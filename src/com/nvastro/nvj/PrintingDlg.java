/*
 * PrintingDlg.java  -  "Printing" dialog
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Printing" dialog.
 *
 * @author Brian Simpson
 */
/* Can't extend EscapeDlg as we need to kill the print job before the dialog */
@SuppressWarnings("serial")
public class PrintingDlg extends JDialog {
  StarWin starwin;
  PrinterJob job;
  boolean cancelled = false;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param parent Main window
   * @param starwin Star window within main window
   * @param job PrinterJob
   */
  public PrintingDlg(final Frame parent, StarWin starwin, PrinterJob job) {
    /* Set window name */
    super(parent, TextBndl.getString("PrintingDlg.Title"), true);
    this.starwin = starwin;
    this.job = job;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        PrintingDlg.this.cancelled = true;
        PrintingDlg.this.job.cancel();
        PrintingDlg.this.starwin.cancelPrint(); // Call after job.cancel()
        PrintingDlg.this.dispose(); // Pop down dialog
      }
    };

    /* Look for the Escape key */
    KeyAdapter keyAdapter = new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
          PrintingDlg.this.cancelled = true;
          PrintingDlg.this.job.cancel();
          PrintingDlg.this.starwin.cancelPrint(); // Call after job.cancel()
          PrintingDlg.this.dispose(); // Pop down dialog
        }
      }
    };
    addKeyListener(keyAdapter);

    /* Handle closing (change dft HIDE_ON_CLOSE) */
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    /* Create controls for this window */
    JLabel lab = new JLabel(TextBndl.getString("PrintingDlg.Msg"));

    /* Create some buttons */
    final JButton Cancel = new JButton(TextBndl.getString("Dlg.Cancel"));
    Cancel.addActionListener(listener);

    /* Create a Box and add buttons for Close & Help */
    Box b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    b.add(Cancel);
    b.add(Box.createHorizontalGlue());

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(20);
    getContentPane().add(lab);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(Cancel);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(parent);

    /* Some window functions */
    addWindowListener(new WindowAdapter() {
      /* Set which component receives focus first */
      public void windowOpened(WindowEvent e) {
        Cancel.requestFocus();
      }

      /* Clean up menu residue, just in case */
      public void windowClosed(WindowEvent e) {
        if ( parent != null ) parent.repaint();
      }
    });

    /* Fire off the print job */
    final SwingWorker3 worker = new SwingWorker3() {
      public Object construct() {
        try {
          PrintingDlg.this.job.print();
        } catch(PrinterException e) {
          // On Linux, exception was generated when print was cancelled.
          // PrintingDlg.this.job.isCancelled() doesn't work here
          if ( ! PrintingDlg.this.cancelled ) {
            // The following works even after PrintingDlg has gone away
            // (at least on Linux...)
            OptionDlg.showMessageDialog(PrintingDlg.this, e.getMessage(),
                                        TextBndl.getString("PrintErr.Title"),
                                        JOptionPane.ERROR_MESSAGE);
          }
        }

        return null;
      }

      /* The following runs in the event dispatching thread */
      public void finished() {
        PrintingDlg.this.dispose();
      }
    };
    worker.start();
  }
}

