/*
 * FontDlg.java  -  "Set fonts" dialog
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * This class does the font selection dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class FontDlg extends EscapeDlg implements ItemListener {
  private JList<String> list;
  private EComboBox font, size, style;
  private PreView pview;
  private ActionListener listeners = null;
  private Preferences prefer;
  private String[] keys;
  private Font[] fnts;
  private int index = 0;    // Used for fnts, select 1st font (0)
  private final String[] sizes = new String[] { "6", "8", "10", "12", "14",
          /* Want increasing continuous even numbers */   "16", "18", "20" };
  private final int sizeL, sizeH;
  private boolean disableFeedback = false;
  static private FontDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new FontDlg(owner, prefer);
    else {
      dlg.loadPrefer();  // Reset any changes to font array
      dlg.setComboBoxes();
    }

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private FontDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("FontDlg.Title"), false);
    this.prefer = prefer;
    sizeL = Integer.parseInt(sizes[0]);
    sizeH = Integer.parseInt(sizes[sizes.length - 1]);

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          FontDlg.this.close(); // Pop down dialog
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(FontDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Get data to display */
    loadPrefer();  // Sets keys and fonts
    String[] fonts = LocalGraphics.getFontFamilyNames();

    /* Create controls for this window */
    list = new JList<String>(keys);
      list.setVisibleRowCount(5);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final NFScrollPane slist = new NFScrollPane(list);
      list.setSelectedIndex(index);
      list.ensureIndexIsVisible(index);  // Must do after adding to NFScrollPane
      list.addListSelectionListener(
        new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
            index = list.getSelectedIndex();
            setComboBoxes();   // Handles pview.repaint();
            // If mouse used, this listener called at least twice
            // (once on button down, more if mouse dragged across
            // selections, and once on button up)
          }
        }
      );
    final JLabel fnlbl = new JLabel(TextBndl.getString("FontDlg.Font"));
    final JLabel szlbl = new JLabel(TextBndl.getString("FontDlg.Size"));
    final JLabel stlbl = new JLabel(TextBndl.getString("FontDlg.Style"));
    font = new EComboBox(fonts);
      font.setMaximumRowCount(9);
      font.addItemListener(this);
    size = new EComboBox(sizes);
      size.setMaximumRowCount(5);
      size.addItemListener(this);
    // Note:  Font.PLAIN = 0, Font.BOLD = 1, Font.ITALIC = 2
    style = new EComboBox(new String[] {
                          TextBndl.getString("FontDlg.Plain"),
                          TextBndl.getString("FontDlg.Bold"),
                          TextBndl.getString("FontDlg.Italic"),
                          TextBndl.getString("FontDlg.BldItal") });
      style.setMaximumRowCount(4);
      style.addItemListener(this);
    JPanel fnpnl = new JPanel(new GridLayout(2, 1));
      fnpnl.add(fnlbl); fnpnl.add(font);
    JPanel szpnl = new JPanel(new GridLayout(2, 1));
      szpnl.add(szlbl); szpnl.add(size);
    JPanel stpnl = new JPanel(new GridLayout(2, 1));
      stpnl.add(stlbl); stpnl.add(style);
    pview = new PreView();
    setComboBoxes();   // Must do after creating pview
    JPanel choose = new JPanel(new BorderLayout());
      ((BorderLayout)choose.getLayout()).setVgap(6);
      choose.setBorder(new TitledBorder(
                       TextBndl.getString("FontDlg.Choose")));
      choose.add(fnpnl, BorderLayout.WEST);
      choose.add(szpnl, BorderLayout.CENTER);
      choose.add(stpnl, BorderLayout.EAST);
      choose.add(pview, BorderLayout.SOUTH);
    JPanel select = new JPanel(new BorderLayout());
      select.setBorder(new TitledBorder(
                       TextBndl.getString("FontDlg.Select")));
      select.add(slist);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"), "setfont");
    setHelpPage("setfont");

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
    ((BorderLayout)getContentPane().getLayout()).setVgap(16);
    getContentPane().add(select, BorderLayout.NORTH);
    getContentPane().add(choose);
    getContentPane().add(b, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(OK);

    /* Finally, set the dialog to its preferred size. */
    pack();
    setResizable(false);
    setLocationRelativeTo(owner);

    /* Set which component receives focus first */
    setFirstFocus(list);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set ComboBoxes.
   */
  private void setComboBoxes() {
    disableFeedback = true;
    font.setSelectedItem(fnts[index].getName());
    int sz = fnts[index].getSize();
    if ( sz % 2 == 1) sz -= 1;   // Don't want odd numbers
    sz = Math.max(sizeL, Math.min(sz, sizeH));
    size.setSelectedItem(Integer.toString(sz));
    style.setSelectedIndex(fnts[index].getStyle());
    pview.repaint();
    disableFeedback = false;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This method implements the ItemListener for the ComboBoxes.
   */
  public void itemStateChanged(ItemEvent e) {
    if ( disableFeedback || e.getStateChange() != ItemEvent.SELECTED ) return;

    // Note:  Font.PLAIN = 0, Font.BOLD = 1, Font.ITALIC = 2
    fnts[index] = new Font((String)font.getSelectedItem(),
                           style.getSelectedIndex(),
                           Integer.parseInt((String)size.getSelectedItem()));
    pview.repaint();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Load preferences.
   */
  private void loadPrefer() {
    keys = new String[7]; // Update if # of fonts changes
    fnts = new Font[7];   // Update if # of fonts changes
    keys[0] = TextBndl.getString("FontDlg.Const");
    fnts[0] = prefer.fontConst();
    keys[1] = TextBndl.getString("FontDlg.StarName");
    fnts[1] = prefer.fontStarName();
    keys[2] = TextBndl.getString("FontDlg.SolarSys");
    fnts[2] = prefer.fontSolarSys();
    keys[3] = TextBndl.getString("FontDlg.DeepSky");
    fnts[3] = prefer.fontDeepSky();
    keys[4] = TextBndl.getString("FontDlg.CGrid");
    fnts[4] = prefer.fontCGrid();
    keys[5] = TextBndl.getString("FontDlg.AGrid");
    fnts[5] = prefer.fontAGrid();
    keys[6] = TextBndl.getString("FontDlg.Horizon");
    fnts[6] = prefer.fontHorizon();
    // Note:  If size changes, update size of keys and fnts arrays
    // Note:  Keep in sync with updatePrefer() function
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    int i = 0;
    // Note:  Keep in sync with loadPrefer() function
    prefer.fontConst(fnts[i++]);
    prefer.fontStarName(fnts[i++]);
    prefer.fontSolarSys(fnts[i++]);
    prefer.fontDeepSky(fnts[i++]);
    prefer.fontCGrid(fnts[i++]);
    prefer.fontAGrid(fnts[i++]);
    prefer.fontHorizon(fnts[i++]);
  }

  /* For testing */
  //static public void main(String[] args) {
  //  Preferences prefer = new Preferences();
  //  FontDlg dlg = new FontDlg(null, prefer);
  //  dlg.setVisible(true);
  //  System.exit(0);
  //}

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Font preview area class.
   */
  final class PreView extends JPanel {
    Insets ins;
    String text;

    public PreView() {
      setOpaque(true);   // Informs Swing's paint system that painting of
      // underlying components is not necessary and that this component
      // will paint all pixels within its rectangle.

      int height = 2 * getFontMetrics(new Font("Dialog", Font.PLAIN,
                       Integer.parseInt(sizes[sizes.length - 1]))).getHeight();
      text = TextBndl.getString("FontDlg.Sample");
      setBorder(new TitledBorder(TextBndl.getString("FontDlg.Preview")));
      ins = getInsets();
      setPreferredSize(new Dimension(300, height + ins.top + ins.bottom));
    }

    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setFont(fnts[index]);
      FontMetrics fm = g.getFontMetrics();
      Dimension dim = getSize();
      int wd = dim.width - ins.left - ins.right;
      int ht = dim.height - ins.top - ins.bottom;
      if ( wd > 0 && ht > 0 ) {
        g.setColor(Color.white);
        g.fillRect(ins.left, ins.top, wd, ht);
        g.setColor(Color.black);
        Shape oldClip = g.getClip();
        g.setClip(ins.left, ins.top, wd, ht);
        int w = fm.stringWidth(text);
        if ( wd > w ) w = ins.left + (wd - w) / 2;
        else          w = ins.left;
        if ( FontDlg.this.prefer.antialiasing )
          ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawString(text, w, ins.top + (ht + fm.getAscent()) / 2);
        g.setClip(oldClip);
      }
    }
  }
}

