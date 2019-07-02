/*
 * ColorDlg.java  -  "Set color" dialog
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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * This class does the color selection dialog.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class ColorDlg extends EscapeDlg {
  private JList<String> list;
  private JRadioButton screen, print;
  private JColorChooser cc;
  private ActionListener listeners = null;
  private Preferences prefer;
  private String[] keys;
  private Color[] colors, prclrs;
  private int index = 0;    // Used for colors, select 1st color (0)
  private JPanel pview;     // Preview panel
  static private ColorDlg dlg = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Main window
   * @param prefer User preferences
   */
  public static void showDlg(Frame owner, Preferences prefer) {
    if ( dlg == null ) dlg = new ColorDlg(owner, prefer);
    else{
      dlg.loadPrefer();  // Reset any changes to color arrays
      dlg.cc.setColor(dlg.screen.isSelected() ? dlg.colors[dlg.index] :
                                                dlg.prclrs[dlg.index]);
    }

    dlg.setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   * @param owner Main window
   * @param prefer User preferences
   */
  private ColorDlg(Frame owner, Preferences prefer) {
    /* Set window name */
    super(owner, TextBndl.getString("ColorDlg.Title"), false);
    this.prefer = prefer;

    /* Add an action listener ... */
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ( !cmd.equals("Apply") )
          ColorDlg.this.close(); // Pop down dialog
        if ( cmd.equals("OK") || cmd.equals("Apply") ) {
          updatePrefer();
          if ( listeners != null )
            listeners.actionPerformed(new ActionEvent(ColorDlg.this,
                                      e.getID(), "update"));
        }
      }
    };
    listeners = AWTEventMulticaster.add(null, (ActionListener)owner);

    /* Get data to display */
    loadPrefer();  // Sets keys, colors, and prclrs

    /* Create controls for this window */
    pview = new JPanel() {      // Preview panel
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets ins = pview.getInsets(); // top, left, bottom, right brdr margins
        Dimension dim = pview.getSize();
        int wd = dim.width - ins.left - ins.right;
        int ht = dim.height - ins.top - ins.bottom;
        if ( wd > 0 && ht > 0 ) {
          g.setColor(screen.isSelected() ? ColorDlg.this.prefer.colorBackGnd() :
                                           ColorDlg.this.prefer.prclrBackGnd());
          g.fillRect(ins.left, ins.top, wd, ht);
          ht /= 3;
          wd -= 2 * ht;
          if ( wd > 0 && ht > 0 ) {
            g.setColor(screen.isSelected() ? colors[index] : prclrs[index]);
            g.fillRect(ins.left + ht, ins.top + ht, wd, ht);
          }
        }
      }
    };
      pview.setBorder(new TitledBorder(
                      TextBndl.getString("ColorDlg.Preview")));
      Insets ins = pview.getInsets();
      pview.setPreferredSize(new Dimension(100,      // Want 25 to be height
                        25 + ins.top + ins.bottom)); //  of black rectangle
    list = new JList<String>(keys);
      list.setVisibleRowCount(5);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final NFScrollPane slist = new NFScrollPane(list);
      list.setSelectedIndex(index);
      list.ensureIndexIsVisible(index);  // Must do after adding to NFScrollPane
    screen = new JRadioButton(TextBndl.getString("ColorDlg.Screen"), true);
    print  = new JRadioButton(TextBndl.getString("ColorDlg.Print"), false);
      ButtonGroup bg = new ButtonGroup();
      bg.add(screen);
      bg.add(print);
    cc = new JColorChooser(colors[index]);
      cc.getSelectionModel().addChangeListener(
        new ChangeListener() {
          public void stateChanged(ChangeEvent e) {
            if ( screen.isSelected() ) colors[index] = cc.getColor();
            else                       prclrs[index] = cc.getColor();
            pview.repaint();
          }
        }
      );
      // Sometime investigate if following has been fixed...:
      // As of Java 1.4.2, Sun "fixed" problems with the preview panel,
      // but created new bugs so that my custom preview panel no longer
      // works.  On Linux & Windoze it no longer appears, and on HP-UX
      // vestiges of the default panel appear on both sides, and the
      // custom panel grows in size on each update!
      // Various problems have been reported in bugs 5029286, 5036962,
      // 4759306, 4955116, ...
      // So I have to move my preview panel outside of the ColorChooser...
      // (On HP-UX an empty dft panel still appears above mine no matter
      // what I do...)
      //cc.setBorder(BorderFactory.createTitledBorder(
      //             TextBndl.getString("ColorDlg.Choose")));
      //cc.setPreviewPanel(pview);
      cc.setPreviewPanel(new JPanel());  // Blanks out cc's preview panel
    JPanel ccp = new JPanel(new BorderLayout());
      ccp.add(cc, BorderLayout.CENTER);
      ccp.add(pview, BorderLayout.SOUTH);
      ccp.setBorder(new TitledBorder(TextBndl.getString("ColorDlg.Choose")));

    /* Listeners for list and radio buttons */
    list.addListSelectionListener(
      new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
          index = list.getSelectedIndex();
          cc.setColor(screen.isSelected() ? colors[index] : prclrs[index]);
          // cc listener will call pview.repaint() (if color changes)
          // If mouse used, this listener called at least twice
          // (once on button down, more if mouse dragged across
          // selections, and once on button up)
        }
      }
    );
    ActionListener btnlistener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cc.setColor(screen.isSelected() ? colors[index] : prclrs[index]);
        // cc listener will call pview.repaint() only if color
        // has changed.  It will not if color is same (which
        // would not be a problem except we're switching backgrounds).
        // Therefore make sure repaint is called...
        pview.repaint();
      }
    };
    screen.addActionListener(btnlistener);
    print.addActionListener(btnlistener);

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
    HelpButton Help = new HelpButton(TextBndl.getString("Dlg.Help"),"setcolor");
    setHelpPage("setcolor");

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

    /* Create Boxes for slist and screen and print */
    Box scrprt = Box.createHorizontalBox();
      scrprt.add(Box.createHorizontalGlue());
      scrprt.add(screen);
      scrprt.add(Box.createHorizontalStrut(8));
      scrprt.add(Box.createHorizontalGlue());
      scrprt.add(print);
      scrprt.add(Box.createHorizontalGlue());
    Box box = Box.createVerticalBox();
      box.add(slist);
      box.add(Box.createVerticalStrut(4));
      box.add(scrprt);
    JPanel pan = new JPanel(new BorderLayout());
      pan.setBorder(new TitledBorder(TextBndl.getString("ColorDlg.Select")));
      pan.add(box);

    /* Add everything to window */
    // Set top, left, bottom, right (in that order)
    ((JComponent)getContentPane()).setBorder(new EmptyBorder(14, 14, 14, 14));
    ((BorderLayout)getContentPane().getLayout()).setVgap(16);
    getContentPane().add(pan, BorderLayout.NORTH);
    getContentPane().add(ccp);
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
   * Load preferences.
   */
  private void loadPrefer() {
    int i = -1;
    keys = new String[12];   // Update if # of colors changes
    colors = new Color[12];  // Update if # of colors changes
    prclrs = new Color[12];  // Update if # of colors changes
    // Note:  Keep in sync with updatePrefer() function
    keys[++i] = TextBndl.getString("ColorDlg.Const");
    colors[i] = prefer.colorConst();
    prclrs[i] = prefer.prclrConst();
    keys[++i] = TextBndl.getString("ColorDlg.ConstBound");
    colors[i] = prefer.colorConstBound();
    prclrs[i] = prefer.prclrConstBound();
    keys[++i] = TextBndl.getString("ColorDlg.Planet");
    colors[i] = prefer.colorPlanet();
    prclrs[i] = prefer.prclrPlanet();
    keys[++i] = TextBndl.getString("ColorDlg.Sun");
    colors[i] = prefer.colorSun();
    prclrs[i] = prefer.prclrSun();
    keys[++i] = TextBndl.getString("ColorDlg.Moon");
    colors[i] = prefer.colorMoon();
    prclrs[i] = prefer.prclrMoon();
    keys[++i] = TextBndl.getString("ColorDlg.CGrid");
    colors[i] = prefer.colorCGrid();
    prclrs[i] = prefer.prclrCGrid();
    keys[++i] = TextBndl.getString("ColorDlg.AGrid");
    colors[i] = prefer.colorAGrid();
    prclrs[i] = prefer.prclrAGrid();
    keys[++i] = TextBndl.getString("ColorDlg.Ecliptic");
    colors[i] = prefer.colorEcliptic();
    prclrs[i] = prefer.prclrEcliptic();
    keys[++i] = TextBndl.getString("ColorDlg.Horizon");
    colors[i] = prefer.colorHorizon();
    prclrs[i] = prefer.prclrHorizon();
    keys[++i] = TextBndl.getString("ColorDlg.DeepSky");
    colors[i] = prefer.colorDeepSky();
    prclrs[i] = prefer.prclrDeepSky();
    keys[++i] = TextBndl.getString("ColorDlg.StarName");
    colors[i] = prefer.colorStarName();
    prclrs[i] = prefer.prclrStarName();
    keys[++i] = TextBndl.getString("ColorDlg.MilkyWay");
    colors[i] = prefer.colorMilkyWay();
    prclrs[i] = prefer.prclrMilkyWay();
    // Note:  If size changes, update size of keys, colors, and prclrs arrays
    // Note:  Keep in sync with updatePrefer() function
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Update preferences.
   */
  private void updatePrefer() {
    int i = 0;
    // Note:  Keep in sync with loadPrefer() function
    prefer.colorConst(colors[i]);
    prefer.prclrConst(prclrs[i++]);
    prefer.colorConstBound(colors[i]);
    prefer.prclrConstBound(prclrs[i++]);
    prefer.colorPlanet(colors[i]);
    prefer.prclrPlanet(prclrs[i++]);
    prefer.colorSun(colors[i]);
    prefer.prclrSun(prclrs[i++]);
    prefer.colorMoon(colors[i]);
    prefer.prclrMoon(prclrs[i++]);
    prefer.colorCGrid(colors[i]);
    prefer.prclrCGrid(prclrs[i++]);
    prefer.colorAGrid(colors[i]);
    prefer.prclrAGrid(prclrs[i++]);
    prefer.colorEcliptic(colors[i]);
    prefer.prclrEcliptic(prclrs[i++]);
    prefer.colorHorizon(colors[i]);
    prefer.prclrHorizon(prclrs[i++]);
    prefer.colorDeepSky(colors[i]);
    prefer.prclrDeepSky(prclrs[i++]);
    prefer.colorStarName(colors[i]);
    prefer.prclrStarName(prclrs[i++]);
    prefer.colorMilkyWay(colors[i]);
    prefer.prclrMilkyWay(prclrs[i++]);
  }

  /* For testing */
  //static public void main(String[] args) {
  //  ColorDlg dlg = new ColorDlg(null, new Preferences());
  //  dlg.setVisible(true);
  //  System.exit(0);
  //}
}

