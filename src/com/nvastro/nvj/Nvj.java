/*
 * Nvj.java  -  Main frame window and starting point for Night Vision
 * Copyright (C) 2011-2019 Brian Simpson
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * This class represents the main frame window for the
 * Night Vision program, and is the starting point for
 * Night Vision.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class Nvj extends JFrame implements ActionListener,
                                MouseWheelListener,
                                AdjustmentListener {
  /* Uncomment the following lines to test a foreign language... */
  //static {     // This code must execute before any access to TextBndl...
  //  Locale.setDefault(Locale.FRENCH);
  //  System.out.println("Language = " +
  //    Locale.getDefault().getDisplayLanguage());
  //}
  static final public  String PgmName = "Night Vision";
  static final public  String PgmVersion = "5.1";
  static final private String PgmInfo = "N i g h t   V i s i o n\n\n" +
                                        "Version " + PgmVersion + "\n\n" +
                                        "Copyright (C) B. Simpson 2019\n\n" +
                                        "A Planetarium for your Computer";
  static final private String Append = TextBndl.getString("PrdInfDlg.Append");
  static final private String PgmInfo2 = (Append.length() > 0) ?
                                         (PgmInfo +"\n\n"+ Append) : PgmInfo;
  static private boolean popupEnabled = false;
  static private InitDlg initDlg;
  static final public String minjavareq = "1.5"; // Minimum Java required
  static final public String javaver = System.getProperty("java.version");
  static public Dimension dimScrn = Toolkit.getDefaultToolkit().getScreenSize();
  static public Component parentFrame = null;  // Used for parent of popup dlgs
         // so that null won't be needed (null results in minimize btn on dlg)
  static private long initDlgStartTime;
  static public String workingDir = ".";
  static public String iniDir = ".";

  private Preferences prefer;
  private JLabel status;
  private JToolBar tbar;
  private JScrollBar hbar;
  private JScrollBar vbar;
  private JScrollBar zbar;
  private JScrollBar fbar;
  private StarWin starwin;
  private JMenuBar menubar;
  private JPopupMenu popup;
  private JMenuItem id = null;
  private JMenuItem mnError = null,   puError = null;
  private JMenuItem mnRADec = null,   puRADec = null;
  private JMenuItem mnPause = null,   puPause = null;;
  private JMenuItem mnZoomin = null,  puZoomin = null;
  private JMenuItem mnZoomout = null, puZoomout = null;
  private JMenu dirAA = null;
  private JMenu dirRD = null;
  private JButton   bnZoomin = null,  bnZoomout = null;
  private JComboBox<String> timestep = null;
  private long menuCloseTime = 0;
  static private boolean showCompTimeState = true;
  static final public Insets TBBtnInsets = new Insets(1, 1, 1, 1);
  private JToggleButton stop;
  static final private String mRAD = TextBndl.getString("M.Set.RADec");
  static final private String mAA  = TextBndl.getString("M.Set.AltAz");
  static final private String mPse = TextBndl.getString("M.Time.Pause");
  static final private String mRun = TextBndl.getString("M.Time.Run");

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This is the constructor for the main window.
   *
   * @param prefer User preferences
   */
  public Nvj(Preferences prefer) {
    super(PgmName);
    this.prefer = prefer;

    /* Set up the menubar */
    menubar = new JMenuBar();
    populateMenu(menubar, null);
    setJMenuBar(menubar);

    /* Add and set up popup menu */
    if ( popupEnabled ) {
      popup = new JPopupMenu(); // Adding following doesn't work {
      //  public void processKeyEvent(KeyEvent e) {
      //    if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) { setVisible(false); }
      //  }
      //};
      populateMenu(null, popup);  // Doing both at once doesn't work:
                                  // populateMenu(menubar, popup);
    }
    else
      popup = null;

    /* Create toolbar */
    mkToolbar();   // Sets tbar
    tbar.setFloatable(false);

    /* Set up control of Error menu items */
    ErrLogger.setupErrorMenuItems(mnError, puError);

    /* Set up control of zoomin/zoomout enabled states */
    prefer.setupZoomMenuItems(mnZoomin, mnZoomout, puZoomin, puZoomout,
                              bnZoomin, bnZoomout);

    /* Create status area */
    status = new JLabel(" ") {  // " " allows initial vertical sizing of label
      /** Override to force antialiasing on. */
      public void paint(Graphics g) {
        if ( Nvj.this.prefer.antialiasing )
          ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                           RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(g);
      }
    };
    status.setBackground(Color.white);
    status.setForeground(Color.black);
    status.setOpaque(true);    // Forces status to color background

    /* Create starwindow */
    starwin = new StarWin(this, prefer);

    /* Create scrollbars */
    hbar = new NFScrollBar(JScrollBar.HORIZONTAL);
    vbar = new NFScrollBar(JScrollBar.VERTICAL);
    zbar = new NFScrollBar(JScrollBar.VERTICAL);
    fbar = new NFScrollBar(JScrollBar.HORIZONTAL);
    prefer.setupHBar(hbar);
    prefer.setupVBar(vbar);
    prefer.setupZBar(zbar);
    prefer.setupFBar(fbar);
    hbar.addAdjustmentListener(this);
    vbar.addAdjustmentListener(this);
    zbar.addAdjustmentListener(this);
    fbar.addAdjustmentListener(this);

    /* Create a constraints object to control placement of
       components, and set some defaults */
    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    // Dft value for c.anchor = CENTER

    /* Add toolbar */
    c.gridwidth = 3; c.gridheight = 1;
    c.gridx = 0; c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0; c.weighty = 0.0;
    cp.add(tbar, c);
    /* Add status */
    c.gridwidth = 3; c.gridheight = 1;
    c.gridx = 0; c.gridy = 1;
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1.0; c.weighty = 0.0;
    cp.add(status, c);
    /* Add starwin */
    c.gridwidth = c.gridheight = 1;
    c.gridx = 1; c.gridy = 3;
    c.fill = GridBagConstraints.BOTH;
    c.weightx = c.weighty = 1.0;
    cp.add(starwin, c);
    /* Add vbar */
    c.gridx = 2; c.gridy = 3;
    c.fill = GridBagConstraints.VERTICAL;
    c.weightx = 0.0; c.weighty = 1.0;
    cp.add(vbar, c);
    /* Add hbar */
    c.gridx = 1; c.gridy = 4;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0; c.weighty = 0.0;
    cp.add(hbar, c);
    /* Add zbar */
    c.gridx = 0; c.gridy = 3;
    c.fill = GridBagConstraints.VERTICAL;
    c.weightx = 0.0; c.weighty = 1.0;
    cp.add(zbar, c);
    /* Add fbar */
    c.gridx = 1; c.gridy = 2;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0; c.weighty = 0.0;
    cp.add(fbar, c);

    /* MouseWheelListener */
    starwin.addMouseWheelListener(this);
    hbar.addMouseWheelListener(this);
    vbar.addMouseWheelListener(this);
    zbar.addMouseWheelListener(this);
    fbar.addMouseWheelListener(this);

    /* Add Window Listener for closing */
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { System.exit(0); } });
    // this.setDefaultCloseOperation(DISPOSE_ON_CLOSE); Doesn't exit

    /* Add (non-Accelerator) key handling */
    addKeyBindings();

    /* Set the icon for this frame */
    setIconImage(getImageIcon("/com/nvastro/nvj/hbsmall.jpg").getImage());

    /* Set visibility of scrollbars and info window */
    updateLayout();

    /* Close initDlg */
    parentFrame = null;
    // Want initDlg to show at least 1.5 seconds (1500 ms)
    int initTime = 1500 - (int)(new Date().getTime() - initDlgStartTime);
    initDlg.closemsec((initTime > 0) ? initTime : 0);

    /* Size, position, and display */
    Rectangle rect = prefer.getMainWindow();
    int x = (int)rect.getX();
    int y = (int)rect.getY();
    int w = (int)rect.getWidth();
    int h = (int)rect.getHeight();
    w = Math.max(100, Math.min(w, dimScrn.width));
    h = Math.max(100, Math.min(h, dimScrn.height));
    x = Math.max(0, Math.min(x, dimScrn.width - w));
    y = Math.max(0, Math.min(y, dimScrn.height - h));
    setLocation(x, y);
    setSize(w, h);
    this.setVisible(true);

    /* Corrects KDE repositioning problem (Must occur after show()) */
    if ( y != getLocation().getY() ) this.setLocation(x, y);
    parentFrame = this;     // Used by certain popup messages

    /* Now that window is showing, if this is first time, show help */
    if ( ! prefer.hasIni() )
      HelpWin.showHelpPage(new Rectangle(x, y, w, h));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the text shown in the status line.
   *
   * @param str String of text to display in status line
   */
  public void setStatusLine(String str) {
    status.setText(" " + str);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates the toolbar.
   */
  private void mkToolbar() {
    JToggleButton tbtn;

    tbar = new JToolBar(PgmName);
    tbar.addSeparator(new Dimension(2, 2));
    tbar.add(mkToolBarBtn("printer.gif", TextBndl.getString("TB.Print"),
                          "print"));
    tbar.addSeparator(new Dimension(4, 4));
    tbtn = mkToolBarTBtn("constlines.gif", TextBndl.getString("TB.ConLines"));
    prefer.setConstLinesTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("constbounds.gif", TextBndl.getString("TB.ConBounds"));
    prefer.setConstBoundsTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("constnames.gif", TextBndl.getString("TB.ConNames"));
    prefer.setConstNamesTBtn(tbtn);
    tbar.add(tbtn);
    tbar.addSeparator(new Dimension(4, 4));
    tbtn = mkToolBarTBtn("cgrid.gif", TextBndl.getString("TB.CGrid"));
    prefer.setCGridTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("agrid.gif", TextBndl.getString("TB.AGrid"));
    prefer.setAGridTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("ecliptic.gif", TextBndl.getString("TB.Ecliptic"));
    prefer.setEclipticTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("horizon.gif", TextBndl.getString("TB.Horizon"));
    prefer.setHorizonTBtn(tbtn);
    tbar.add(tbtn);
    tbar.addSeparator(new Dimension(4, 4));
    tbtn = mkToolBarTBtn("saturn.gif", TextBndl.getString("TB.NS"));
    prefer.setNearSkyTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("horsehead.gif", TextBndl.getString("TB.DS"));
    prefer.setDeepSkyTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("dsname.gif", TextBndl.getString("TB.DSN"));
    prefer.setDeepSkyNmTBtn(tbtn);
    tbar.add(tbtn);
    tbtn = mkToolBarTBtn("milkyway.gif", TextBndl.getString("TB.MW"));
    prefer.setMilkyWayTBtn(tbtn);
    tbar.add(tbtn);
    tbar.addSeparator(new Dimension(4, 4));
    stop = mkToolBarTBtn("pause.gif", TextBndl.getString("TB.PseTime"));
    stop.setSelectedIcon(Nvj.getImageIcon("/com/nvastro/nvj/run.gif"));
    stop.setActionCommand("time_tgl");
    tbar.add(stop);
    tbar.add(mkToolBarBtn("timeadd.gif", TextBndl.getString("TB.AddTime"),
                          "addtime"));
    timestep = new JComboBox<String>();
    timestep.addItem(TextBndl.getString("TB.Minute"));
    timestep.addItem(TextBndl.getString("TB.Hour"));
    timestep.addItem(TextBndl.getString("TB.Day"));
    timestep.addItem(TextBndl.getString("TB.Week"));
    timestep.addItem(TextBndl.getString("TB.Month"));
    timestep.addItem(TextBndl.getString("TB.Year"));
    timestep.setFont(new Font("Dialog", Font.BOLD, 11));
    timestep.setMaximumSize(timestep.getMinimumSize());
    timestep.setSelectedIndex(1);
    tbar.add(timestep);
    tbar.add(mkToolBarBtn("timesub.gif", TextBndl.getString("TB.SubTime"),
                          "subtime"));
    if ( !showCompTimeState ) {
      tbar.add(mkToolBarBtn("computer.gif", TextBndl.getString("TB.CompTime"),
                            "comptime"));
    } else {
      tbtn = mkToolBarTBtn("computer.gif", TextBndl.getString("TB.CompTime"));
      tbtn.setActionCommand("comptime");
      prefer.setCompTimeBtn(tbtn);
      tbar.add(tbtn);
    }
    tbar.addSeparator(new Dimension(4, 4));
    bnZoomin = mkToolBarBtn("zoomin.gif", TextBndl.getString("TB.ZoomIn"),
                          "zoom_in");
    bnZoomout = mkToolBarBtn("zoomout.gif", TextBndl.getString("TB.ZoomOut"),
                          "zoom_out");
    tbar.add(bnZoomin);
    tbar.add(bnZoomout);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Makes a push button for the toolbar.
   */
  private JButton mkToolBarBtn(String img, String tip, String cmd) {
    JButton btn = new JButton(Nvj.getImageIcon("/com/nvastro/nvj/" + img));
    btn.setToolTipText(tip);
    btn.setMargin(TBBtnInsets);
    btn.setActionCommand(cmd);
    btn.addActionListener(this);
    return btn;
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Makes a toggle button for the toolbar.
   */
  private JToggleButton mkToolBarTBtn(String img, String tip) {
    JToggleButton btn =
                 new JToggleButton(Nvj.getImageIcon("/com/nvastro/nvj/" + img));
    btn.setToolTipText(tip);
    btn.setMargin(TBBtnInsets);
    btn.setActionCommand("update");
    btn.addActionListener(this);
    return btn;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Populates the menubar or the popup menu.  Set up so that both could
   * share same menu items, but unfortunately that doesn't work.  Not sure
   * why.  So only do one at a time (with null for the other).
   *
   * @param mn The JMenuBar to populate
   * @param pu The JPopupMenu to populate
   */
  private void populateMenu(JMenuBar mn, JPopupMenu pu) {
    if ( pu != null ) {
      id = new JMenuItem(TextBndl.getString("M.Ident"));
      id.addActionListener(this);
      id.setActionCommand("ident");
      pu.add(id);
    }

    /* Use a MenuListener to catch when a JMenu is deselected */
    MenuListener ml = new MenuListener() {
      public void menuSelected(MenuEvent e) {}
      public void menuDeselected(MenuEvent e) {
        Nvj.this.menuCloseTime = new Date().getTime();
      }
      public void menuCanceled(MenuEvent e) {}
    };

    JMenu file = new JMenu(TextBndl.getString("M.File"));
    file.addMenuListener(ml);
    addMenuItems(file, this,
         new String[] { TextBndl.getString("M.File.Save_loc"),
                        TextBndl.getString("M.File.Save_pref"),
                        TextBndl.getString("M.File.Save_win") },
         new String[] { "save_loc", "save_pref", "save_win" },
         null);
    file.addSeparator();
    addMenuItems(file, this,
         new String[] { TextBndl.getString("M.File.Messages") },
         new String[] { "errors" },
         null);
      if ( mn != null )
        mnError = file.getItem(file.getItemCount() - 1);
      if ( pu != null )
        puError = file.getItem(file.getItemCount() - 1);
      // Note:  Enablement handled by ErrLogger
    addMenuItems(file, this,
         new String[] { TextBndl.getString("M.File.Write") },
         new String[] { "write" },
         null);
    file.addSeparator();
    addMenuItems(file, this,
         new String[] { TextBndl.getString("M.File.Print") },
         new String[] { "print" },
         null);
    file.addSeparator();
    addMenuItems(file, this,
         new String[] { TextBndl.getString("M.File.Close") },
         new String[] { "close" },
         null);
    if ( mn != null ) mn.add(file);
    if ( pu != null ) pu.add(file);

    JMenu set = new JMenu(TextBndl.getString("M.Set"));
    set.addMenuListener(ml);
    addMenuItems(set, this,
         new String[] { TextBndl.getString("M.Set.Location") },
         new String[] { "set_loc" },
         null);
    set.addSeparator();
    addMenuItems(set, this,
         new String[] { TextBndl.getString("M.Set.Stars") },
         new String[] { "set_stars" },
         null);
    set.addSeparator();
    addMenuItems(set, this,
         new String[] { TextBndl.getString("M.Set.Fonts") },
         new String[] { "set_fonts" },
         null);
    set.addSeparator();
    addMenuItems(set, this,
         new String[] { TextBndl.getString("M.Set.Colors") },
         new String[] { "set_colors" },
         null);
    set.addSeparator();
    addMenuItems(set, this,
         new String[] { TextBndl.getString("M.Set.Windows") },
         new String[] { "set_win" },
         null);
    set.addSeparator();
    if ( mn != null ) {
      mnRADec = new JMenuItem(prefer.modeRADec ? mAA : mRAD);
      mnRADec.addActionListener(this);
      mnRADec.setActionCommand("set_rad");
      set.add(mnRADec);
    }
    if ( pu != null ) {
      puRADec = new JMenuItem(prefer.modeRADec ? mAA : mRAD);
      puRADec.addActionListener(this);
      puRADec.setActionCommand("set_rad");
      set.add(puRADec);
    }
    if ( mn != null ) mn.add(set);
    if ( pu != null ) pu.add(set);

    JMenu view = new JMenu(TextBndl.getString("M.View"));
    view.addMenuListener(ml);
    addMenuItems(view, this,
         new String[] { TextBndl.getString("M.View.Select") },
         new String[] { "select" },
         null);
    view.addSeparator();
    JMenu find = new JMenu(TextBndl.getString("M.View.Find"));
    addMenuItems(find, this,
         new String[] { TextBndl.getString("M.View.Find.Cnst"),
                        TextBndl.getString("M.View.Find.Str"),
                        TextBndl.getString("M.View.Find.DS"),
                        TextBndl.getString("M.View.Find.SS") },
         new String[] { "find_cnst", "find_str", "find_ds", "find_ss" },
         null);
    view.add(find);
    view.addSeparator();
    dirAA = new JMenu(TextBndl.getString("M.View.Dir"));
    addMenuItems(dirAA, this,
         new String[] { TextBndl.getString("M.View.Dir.N"),
                        TextBndl.getString("M.View.Dir.E"),
                        TextBndl.getString("M.View.Dir.S"),
                        TextBndl.getString("M.View.Dir.W"),
                        TextBndl.getString("M.View.Dir.Z"),
                        TextBndl.getString("M.View.Dir.H"),
                        TextBndl.getString("M.View.Dir.D") },
         new String[] { "dir_n", "dir_e", "dir_s", "dir_w",
                        "dir_z", "dir_h", "dir_d" },
         new KeyStroke[] {
                KeyStroke.getKeyStroke(KeyEvent.VK_N, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_E, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_S, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_W, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.CTRL_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK) });
    dirRD = new JMenu(TextBndl.getString("M.View.Dir"));
    addMenuItems(dirRD, this,
         new String[] { TextBndl.getString("M.View.Dir.0"),
                        TextBndl.getString("M.View.Dir.6"),
                        TextBndl.getString("M.View.Dir.12"),
                        TextBndl.getString("M.View.Dir.18"),
                        TextBndl.getString("M.View.Dir.P"),
                        TextBndl.getString("M.View.Dir.Q"),
                        TextBndl.getString("M.View.Dir.R") },
         new String[] { "dir_0", "dir_6", "dir_12", "dir_18",
                        "dir_z", "dir_h", "dir_d" },
         new KeyStroke[] {
                KeyStroke.getKeyStroke(KeyEvent.VK_0, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_9, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_8, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_7, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_H, Event.CTRL_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK) });
    view.add(dirAA);
    view.add(dirRD);
    if ( prefer.modeRADec ) dirAA.setVisible(false);
    else                    dirRD.setVisible(false);
    // It would be nice if making invisible and/or disabling would turn off
    // the shortcuts (accelerator keys), but not so lucky...
    view.addSeparator();
    addMenuItems(view, this,
         new String[] { TextBndl.getString("M.View.Zoomin"),
                        TextBndl.getString("M.View.Zoomout") },
         new String[] { "zoom_in", "zoom_out" },
         new KeyStroke[] { /* TextBndl.getString("s.View.Zoomin").charAt(0),
                              TextBndl.getString("s.View.Zoomout").charAt(0) */
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK) });
      if ( mn != null ) {
        mnZoomin  = view.getItem(view.getItemCount() - 2);
        mnZoomout = view.getItem(view.getItemCount() - 1);
      }
      if ( pu != null ) {
        puZoomin  = view.getItem(view.getItemCount() - 2);
        puZoomout = view.getItem(view.getItemCount() - 1);
      }
    view.addSeparator();
    addMenuItems(view, this,
         new String[] { TextBndl.getString("M.View.PrevView"),
                        TextBndl.getString("M.View.PrevZoom") },
         new String[] { "prev_view", "prev_zoom" },
         new KeyStroke[] { /* TextBndl.getString("s.View.PrevView").charAt(0),
                              TextBndl.getString("s.View.PrevZoom").charAt(0) */
                KeyStroke.getKeyStroke(KeyEvent.VK_P, 0),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.SHIFT_MASK) });
    view.addSeparator();
    addMenuItems(view, this,
         new String[] { TextBndl.getString("M.View.SolarSys") },
         new String[] { "solarsys" },
         new KeyStroke[] {
                KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK) });
    //view.addSeparator();
    //addMenuItems(view, this,
    //     new String[] { TextBndl.getString("M.View.Fullscrn") },
    //     new String[] { "full_scrn" },
    //     new KeyStroke[] {
    //            KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK) });
    if ( mn != null ) mn.add(view);
    if ( pu != null ) pu.add(view);

    JMenu time = new JMenu(TextBndl.getString("M.Time"));
    time.addMenuListener(ml);
    addMenuItems(time, this,
         new String[] { TextBndl.getString("M.Time.Set") },
         new String[] { "time_set" },
         null);
    time.addSeparator();
    if ( mn != null ) {
      mnPause = new JMenuItem(mPse);
      mnPause.addActionListener(this);
      mnPause.setActionCommand("time_pse");
      time.add(mnPause);
    }
    if ( pu != null ) {
      puPause = new JMenuItem(mPse);
      puPause.addActionListener(this);
      puPause.setActionCommand("time_pse");
      time.add(puPause);
    }
    time.addSeparator();
    addMenuItems(time, this,
         new String[] { TextBndl.getString("M.Time.Rates") },
         new String[] { "time_rts" },
         null);
    if ( mn != null ) mn.add(time);
    if ( pu != null ) pu.add(time);

    JMenu help = new JMenu(TextBndl.getString("M.Help"));
    addMenuItems(help, this,
         new String[] { TextBndl.getString("M.Help.Gen"),
                        TextBndl.getString("M.Help.Key") },
         new String[] { "help_gen", "help_key" },
         new KeyStroke[] {
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                null });
    help.addSeparator();
    addMenuItems(help, this,
         new String[] { TextBndl.getString("M.Help.Cnst"),
                        TextBndl.getString("M.Help.Mess"),
                        TextBndl.getString("M.Help.DSky"),
                        TextBndl.getString("M.Help.Grk") },
         new String[] { "help_cnst", "help_mess", "help_dsky", "help_grk" },
         null);
    help.addSeparator();
    addMenuItems(help, this,
         new String[] { TextBndl.getString("M.Help.About") },
         new String[] { "help_abt" },
         null);

    if ( mn != null ) mn.add(help);
    if ( pu != null ) pu.add(help);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the popup menu if enabled, otherwise will show IdentifyDlg
   * if identify is true.
   *
   * @param x x location in screen coordinates
   * @param y y location in screen coordinates
   * @param identify boolean to indicate if point is within 90 degrees
   *                 of window center
   */
  public void showPopup(int x, int y, boolean identify) {
    if ( popup != null ) {
      id.setEnabled(identify);
      Point window = getLocationOnScreen();
      x -= window.x;
      y -= window.y;
      popup.show(this, x, y);
    }
    else if ( identify )
      actionPerformed(new ActionEvent(starwin, 0, "ident"));
    // The following doesn't work.  Why?
    // //dispatchEvent(new ActionEvent(starwin, 0, "ident"));
    // dispatchEvent(new ActionEvent(this, 0, "ident"));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This is the convenience routine for adding menu items to a menu.
   */
  private void addMenuItems(JMenu menu, ActionListener listener,
                            String[] labels, String[] commands,
                            KeyStroke[] shortcuts) {
    for( int i = 0; i < labels.length; i++ ) {
      JMenuItem mi = new JMenuItem(labels[i]);
      mi.addActionListener(listener);
      if ( (commands != null) && (commands[i] != null) )
        mi.setActionCommand(commands[i]);
      if ( (shortcuts != null) && (shortcuts[i] != null) )
        mi.setAccelerator(shortcuts[i]);
      menu.add(mi);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add key bindings.
   */
  protected void addKeyBindings() {
    //Don't use RootPane (as commented out below), as it will prevent
    //the menu from seeing up/down (goes to view instead)
    //JRootPane rp = getRootPane();
    //InputMap imap = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    //ActionMap amap = rp.getActionMap();
    //--
    InputMap imap = starwin.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap amap = starwin.getActionMap();
    //--
    //JComponent cp = (JComponent)getContentPane();
    //InputMap imap = cp.getInputMap(
    //                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    //ActionMap amap = cp.getActionMap();

    amap.put("lineup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAlt('u') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,Event.SHIFT_MASK), "lineup");

    amap.put("pageup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAlt('U') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.CTRL_MASK), "pageup");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, Event.SHIFT_MASK),
             "pageup");

    amap.put("linedown", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAlt('d') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.SHIFT_MASK),
             "linedown");

    amap.put("pagedown", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAlt('D') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.CTRL_MASK),
             "pagedown");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, Event.SHIFT_MASK),
             "pagedown");

    amap.put("lineright", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAz('r') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.SHIFT_MASK),
             "lineright");

    amap.put("pageright", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAz('R') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.CTRL_MASK),
             "pageright");

    amap.put("lineleft", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAz('l') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.SHIFT_MASK),
             "lineleft");

    amap.put("pageleft", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.moveAz('L') ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.CTRL_MASK),
             "pageleft");

    amap.put("centerfld", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if ( prefer.setFld(0) ) starwin.restartpaint();
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK),
             "centerfld");

    amap.put("escape", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        starwin.escapePressed();
      }
    });
    // imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
    // The above line doesn't work when mouse is dragged, but does work
    // otherwise.  (Key presses are blocked during mouse drag?)  The
    // next line always works (based on KEY_TYPED).
    imap.put(KeyStroke.getKeyStroke('\u001B'), "escape");

    amap.put("Ctrl1", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Preferences.option1 = ! Preferences.option1;    // Toggle option1
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, Event.CTRL_MASK), "Ctrl1");

    amap.put("Ctrl2", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        Preferences.option2 = ! Preferences.option2;    // Toggle option2
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, Event.CTRL_MASK), "Ctrl2");

    // May someday relocate this here
    //amap.put("LoadDS", new AbstractAction() {
    //  public void actionPerformed(ActionEvent e) {
    //    LoadDSDlg.showDlg(Nvj.this);
    //  }
    //});
    //imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.SHIFT_MASK |
    //         Event.CTRL_MASK | Event.ALT_MASK), "LoadDS");

    amap.put("Julian", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        JulianDlg.showDlg(Nvj.this);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_J, Event.SHIFT_MASK |
             Event.CTRL_MASK | Event.ALT_MASK), "Julian");

    amap.put("Version", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        VersionDlg.showDlg(Nvj.this, prefer);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.SHIFT_MASK |
             Event.CTRL_MASK | Event.ALT_MASK), "Version");

    // Note:  "help_gen", "zoom_in", "zoom_out", "prev_view", "prev_zoom"
    // are handled via menu accelerators.  These menu accelerators
    // cancel the menu as well as invoke their actions.
    // (Except for the popup menu:  Accelerators are ignored
    // and popup menu is unaffected.)
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Updates the layout of the main window (status, scrollbars)
   */
  public void updateLayout() {
    tbar.setVisible(prefer.showToolBar());
    status.setVisible(prefer.showWinInfo());
    vbar.setVisible(prefer.showScrlAzAlt());
    hbar.setVisible(prefer.showScrlAzAlt());
    zbar.setVisible(prefer.showScrlZoom());
    fbar.setVisible(prefer.showScrlField());
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Turns ID object marker off
   */
  public void clearIDMarker() {
    starwin.clearIDMarker();  // Turn object marker off
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Go to object
   *
   * @param obj The object to go to.
   */
  public void gotoObject(SkyObject obj) { starwin.gotoObject(obj); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements MouseWheelListener interface.
   *
   * @param e MouseWheelEvent
   */
  public void mouseWheelMoved(MouseWheelEvent e) {
    int notches = e.getWheelRotation();
    Component comp = e.getComponent();

    if ( comp == (Component)hbar ) {
      int v = hbar.getValue();
      if ( notches > 0 ) {
        if ( v > 0 ) hbar.setValue(Math.max(0, v - Preferences.SCROLLINC));
      } else {
        if ( v < 360 ) hbar.setValue(Math.min(360, v + Preferences.SCROLLINC));
      }
    }
    else if ( comp == (Component)vbar ) {
      int v = vbar.getValue();
      if (notches < 0) {
        if ( v > 0 )
          vbar.setValue(Math.max(0, v - Preferences.SCROLLINC));
      } else {
        if ( v < 180 )
          vbar.setValue(Math.min(180, v + Preferences.SCROLLINC));
      }
    }
    else if ( comp == (Component)fbar ) {
      int v = fbar.getValue();
      if ( notches > 0 ) {
        if ( v > 0 ) fbar.setValue(Math.max(0, v - Preferences.SCROLLINC));
      } else {
        if ( v < 360 ) fbar.setValue(Math.min(360, v + Preferences.SCROLLINC));
      }
    }
    else {
      int v = zbar.getValue();
      if (notches < 0) {
        if ( v > 0 )
          zbar.setValue(Math.max(0, v - Preferences.ZSCRLBLK));
      } else {
        if ( v < Preferences.ZSCRLRNG )
          zbar.setValue(Math.min(Preferences.ZSCRLRNG,
                             v + Preferences.ZSCRLBLK));
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements ActionListener interface.
   *
   * @param e ActionEvent
   */
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();

    if      ( cmd.equals("save_loc") ) {
      if ( prefer.saveLocation() ) {
        OptionDlg.showMessageDialog(this, TextBndl.getString("SaveDlg.Loc"),
                                    TextBndl.getString("SaveDlg.Title"),
                                    JOptionPane.INFORMATION_MESSAGE);
        repaint();                  // Clean up menu residue
      }
    }
    else if ( cmd.equals("save_pref") ) {
      if ( prefer.savePrefer() ) {
        OptionDlg.showMessageDialog(this, TextBndl.getString("SaveDlg.Pref"),
                                    TextBndl.getString("SaveDlg.Title"),
                                    JOptionPane.INFORMATION_MESSAGE);
        repaint();                  // Clean up menu residue
      }
    }
    else if ( cmd.equals("save_win") ) {
      if ( prefer.saveMainWindow(new Rectangle(getX(), getY(),
                                               getWidth(), getHeight())) ) {
        OptionDlg.showMessageDialog(this, TextBndl.getString("SaveDlg.Win"),
                                    TextBndl.getString("SaveDlg.Title"),
                                    JOptionPane.INFORMATION_MESSAGE);
        repaint();                  // Clean up menu residue
      }
    }
    else if ( cmd.equals("errors") ) { ErrLogger.displayErrors(this); }
    else if ( cmd.equals("write") ) {
      WriteDlg.showDlg(this);
    }
    else if ( cmd.equals("print") ) { print(); }
    else if ( cmd.equals("close") ) { System.exit(0); }
    else if ( cmd.equals("set_loc") ) {
      LocationDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("set_fonts") ) {
      FontDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("set_stars") ) {
      StrPrmDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("set_colors") ) {
      ColorDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("set_win") ) {
      WinPrefDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("set_rad") ) {
      if ( mnRADec.getText().equals(mRAD) ) {
        mnRADec.setText(mAA);     // Set text for rtn to Alt/Az mode
        dirRD.setVisible(true);
        dirAA.setVisible(false);
        starwin.gotoRADec();
      }
      else {
        mnRADec.setText(mRAD);    // Set text for rtn to RA/Dec mode
        dirAA.setVisible(true);
        dirRD.setVisible(false);
        starwin.gotoAltAz();
      }
      starwin.restartpaint();
    }
    else if ( cmd.equals("select") ) {
      SelObjDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("find_cnst") ) {
      FindDlg.showDlg(this, prefer, FindDlg.CON);
    }
    else if ( cmd.equals("find_str") ) {
      FindStarDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("find_ds") ) {
      FindDSDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("find_ss") ) {
      FindDlg.showDlg(this, prefer, FindDlg.NS);
    }
    else if ( cmd.equals("dir_n") && !prefer.modeRADec ) { // North (Az = 0)
      if ( prefer.moveAz('n') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_e") && !prefer.modeRADec ) { // East
      if ( prefer.moveAz('e') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_s") && !prefer.modeRADec ) { // South
      if ( prefer.moveAz('s') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_w") && !prefer.modeRADec ) { // West
      if ( prefer.moveAz('w') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_0") &&  prefer.modeRADec ) { // 0 hours RA
      if ( prefer.moveAz('0') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_6") &&  prefer.modeRADec ) { // 6 hours RA
      if ( prefer.moveAz('w') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_12") &&  prefer.modeRADec ) { // 12 hours RA
      if ( prefer.moveAz('s') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_18") &&  prefer.modeRADec ) { // 18 hours RA
      if ( prefer.moveAz('e') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_z") ) {       // Zenith
      if ( prefer.moveAlt('z') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_h") ) {       // Horizon
      if ( prefer.moveAlt('h') ) starwin.restartpaint();
    }
    else if ( cmd.equals("dir_d") ) {       // Nadir
      if ( prefer.moveAlt('n') ) starwin.restartpaint();
    }
    else if ( cmd.equals("lineup") ) {      // Line up
      if ( prefer.moveAlt('u') ) starwin.restartpaint();
    }
    else if ( cmd.equals("linedown") ) {    // Line down
      if ( prefer.moveAlt('d') ) starwin.restartpaint();
    }
    else if ( cmd.equals("pageup") ) {      // Page up
      if ( prefer.moveAlt('U') ) starwin.restartpaint();
    }
    else if ( cmd.equals("pagedown") ) {    // Page down
      if ( prefer.moveAlt('D') ) starwin.restartpaint();
    }
    else if ( cmd.equals("lineright") ) {   // Line right
      if ( prefer.moveAz('r') ) starwin.restartpaint();
    }
    else if ( cmd.equals("lineleft") ) {    // Line left
      if ( prefer.moveAz('l') ) starwin.restartpaint();
    }
    else if ( cmd.equals("pageright") ) {   // Page right
      if ( prefer.moveAz('R') ) starwin.restartpaint();
    }
    else if ( cmd.equals("pageleft") ) {    // Page left
      if ( prefer.moveAz('L') ) starwin.restartpaint();
    }
    else if ( cmd.equals("zoom_in") ) {
      if ( prefer.zoomIn() ) starwin.restartpaint();
    }
    else if ( cmd.equals("zoom_out") ) {
      if ( prefer.zoomOut() ) starwin.restartpaint();
    }
    else if ( cmd.equals("prev_view") ) {
      if ( prefer.swapView() ) starwin.restartpaint();
    }
    else if ( cmd.equals("prev_zoom") ) {
      if ( prefer.swapZoom() ) starwin.restartpaint();
    }
    else if ( cmd.equals("solarsys") ) {
      SSWin.showWindow(starwin, prefer);
    }
    else if ( cmd.equals("full_scrn") ) {}
    else if ( cmd.equals("centerfld") ) {
      if ( prefer.setFld(0) ) starwin.restartpaint();
    }
    else if ( cmd.equals("time_set") ) {
      DateTimeDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("time_tgl") ) { // From toolbar
      if ( stop.isSelected() ) { // I.e. was toggled on
        mnPause.setText(mRun);
        starwin.freezeTime();    // Calls starwin.restartpaint();
      }
      else                     { // I.e. was toggled off
        mnPause.setText(mPse);
        starwin.thawTime();
      }
    }
    else if ( cmd.equals("time_pse") ) { // From menu
      if ( mnPause.getText().equals(mPse) ) {
        stop.setSelected(true);
        mnPause.setText(mRun);
        starwin.freezeTime();    // Calls starwin.restartpaint();
      }
      else {
        stop.setSelected(false);
        mnPause.setText(mPse);
        starwin.thawTime();
      }
    }
    else if ( cmd.equals("time_rts") ) {
      TimeRateDlg.showDlg(this, prefer);
    }
    else if ( cmd.equals("help_gen") ) {
      long now = new Date().getTime();
      if ( now - menuCloseTime < 200 ) HelpWin.showHelpPage("menu");
      else                             HelpWin.showHelpPage();
    }
    else if ( cmd.equals("help_key") ) { HelpWin.showHelpPage("keys"); }
    else if ( cmd.equals("help_cnst") ) { HelpWin.showHelpPage("const"); }
    else if ( cmd.equals("help_mess") ) { HelpWin.showHelpPage("messier"); }
    else if ( cmd.equals("help_dsky") ) { HelpWin.showHelpPage("deepsky"); }
    else if ( cmd.equals("help_grk") ) { HelpWin.showHelpPage("greek"); }
    else if ( cmd.equals("help_abt") ) {
      JTextArea ta = new JTextArea(PgmInfo2);
      ta.setEditable(false);
      ta.setOpaque(false);
      ta.setFont(getFont().deriveFont(Font.BOLD));
      ta.setAlignmentX(Component.CENTER_ALIGNMENT);
      ta.setAlignmentY(Component.CENTER_ALIGNMENT);
      OptionDlg.showMessageDialog(this, ta,
                                  TextBndl.getString("PrdInfDlg.Title"),
                                  JOptionPane.INFORMATION_MESSAGE,
                                  getImageIcon("/com/nvastro/nvj/hb.jpg"));
      //Which way to go here?  Apply to InitDlg too?
      //OptionDlg.showMessageDialog(this, PgmInfo2,
      //                            TextBndl.getString("PrdInfDlg.Title"),
      //                            JOptionPane.INFORMATION_MESSAGE,
      //                            getImageIcon("/com/nvastro/nvj/hb.jpg"));
      repaint();                  // Clean up menu residue
    }
    else if ( cmd.equals("ident") ) {
      /* starwin.identifyObject(pt) will resolve the object and return
         info in a string, and will also set the object marker on;
         pt will be updated to screen coordinates of object */
      Point pt = new Point();   // Will be set to screen coordinates;
      String s = starwin.identifyObject(pt);
      IdentifyDlg.showDlg(this, s, pt);
    }
    else if ( cmd.equals("update") ) { starwin.restartpaint(); }
    else if ( cmd.equals("escape") ) {
      //stem.out.println("escape");
      if ( popup != null && popup.isVisible() ) popup.setVisible(false);
      starwin.escapePressed();
    }
    else if ( cmd.equals("addtime") || cmd.equals("subtime") ) {
      int field;
      switch ( timestep.getSelectedIndex() ) {
        case 5:  field = Calendar.YEAR;
          break;
        case 4:  field = Calendar.MONTH;
          break;
        case 3:  field = Calendar.WEEK_OF_YEAR;
          break;
        case 2:  field = Calendar.DAY_OF_YEAR;
          break;
        case 1:  field = Calendar.HOUR_OF_DAY;
          break;
        default: field = Calendar.MINUTE;
          break;
      }
      prefer.lst.addTime(field, cmd.equals("addtime") ? true : false);
      starwin.restartpaint();
    }
    else if ( cmd.equals("comptime") ) {
      prefer.lst.setToCompDateTime();
      starwin.restartpaint();
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Print.
   */
  private void print() {
    /* Get the PrinterJob object */
    PrinterJob job = PrinterJob.getPrinterJob();
    job.setJobName(TextBndl.getString("Print.JobName"));

    /* Bring up "Page setup" dialog window (portrait/landscape, margins, ...) */
    PageFormat pf = job.defaultPage();
    PageFormat format = job.pageDialog(pf);
    if ( pf == format ) return;        // Cancel was pressed

    /* Tell the PrinterJob what to print (Doesn't bring up dialog) */
    job.setPrintable(starwin, format);

    /* Bring up "Print" dialog, allows selection of printer */
    if ( job.printDialog() ) {
      /* If you select print-to-file and the file already exists, an
         additional dialog will appear that will request overwrite or
         cancel.  This dialog occurs before "job.print()" below.
         If you choose overwrite and the file is unwritable (e.g. is
         a directory), it will be discovered after "job.print()"
         has started.  A FileNotFoundException will be generated. */
      /* The following comment refers to OS/2 printer objects that have
         been set to print-to-file.  I assume that "job.print()" will
         have begun before you see the dialog.  Not sure... */
      /* If printer has been set to print-to-file, the "Print to file" dialog
         will show here.  If you hit cancel, it takes a while to stop... */

      /* Because "job.print()" will start a process that will call
         StarWin.print multiple times, call StarWin.preparePrint to
         freeze the print parameters. */
      starwin.preparePrint(format);
      // The following does:  try { job.print(); } catch(PrinterException e)...
      PrintingDlg dlg = new PrintingDlg(this, starwin, job);
      dlg.setVisible(true);
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements AdjustmentListener interface.
   *
   * @param e AdjustmentEvent
   */
  public void adjustmentValueChanged(AdjustmentEvent e) {
    boolean moved;

    if ( e.getAdjustable() == vbar ) {
      //stem.out.println("AdjustmentType = " + e.getAdjustmentType() +
      //                 ", ID = " + e.getID() + ", Value = " + e.getValue());
      //stem.out.println("paramString = " + e.paramString());
      //stem.out.println("ValueIsAdjusting = " + vbar.getValueIsAdjusting());
      moved = prefer.mouseMoveVBar(e.getValue(), vbar.getValueIsAdjusting());
    }
    else if ( e.getAdjustable() == hbar )
      moved = prefer.mouseMoveHBar(e.getValue(), hbar.getValueIsAdjusting());
    else if ( e.getAdjustable() == zbar )
      moved = prefer.mouseMoveZBar(e.getValue(), zbar.getValueIsAdjusting());
    else   /* e.getAdjustable() == fbar */
      moved = prefer.mouseMoveFBar(e.getValue(), fbar.getValueIsAdjusting());
    if ( moved ) starwin.restartpaint();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * This is the starting point for Night Vision.
   *
   * @param args Arguments passed on command line
   */
  public static void main(String[] args) {
    time_bomb();
    checkForOldJava();
    checkForCleanup(args);

    /* Show splash window */
    initDlg = new InitDlg(PgmInfo2, "...");
    initDlg.setVisible(true);
    initDlgStartTime = new Date().getTime();
    parentFrame = initDlg;   // Used by popup error msgs

    /* Set working (& ini) directory and read star (& ...) data */
    initDlg.setMessage(TextBndl.getString("Startup.star"));
    setWorkingDir();
    StarDB.init();
    StarDsgnDB.init();

    /* Read constellation data */
    initDlg.setMessage(TextBndl.getString("Startup.const"));
    ConstLines.init();
    ConstBounds.init();

    /* Read Milky Way data */
    initDlg.setMessage(TextBndl.getString("Startup.milkyway"));
    MilkyWay.init();

    /* Read star name data */
    initDlg.setMessage(TextBndl.getString("Startup.strname"));
    StarNameDB.init();

    /* Read deep sky data */
    initDlg.setMessage(TextBndl.getString("Startup.ds"));
    DeepSkyDB.init();

    /* Read location data */
    initDlg.setMessage(TextBndl.getString("Startup.loc"));
    CityDB.init();

    /* Read preferences (from ini file) */
    initDlg.setMessage(TextBndl.getString("Startup.ini"));
    Preferences prefer = new Preferences();

    /* Read command line arguments */
    initDlg.setMessage(TextBndl.getString("Startup.start"));
    checkArgs(args, prefer);    // Do after Preferences, in case of override

    /* Start main window */
    new Nvj(prefer);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Set working directory (Windoze only).
   */
  private static void setWorkingDir() {
    final String QBase = "reg query \"HKCU\\Software\\Microsoft\\Windows\\" +
                     "CurrentVersion\\Explorer\\Shell Folders\" /v ";
    final Properties props = System.getProperties();
    String os = props.getProperty("os.name");
    String ver = props.getProperty("os.version"); // E.g. Windoze: "5.1"
    // E.g. Linux: "2.6.30.9-99.fc11.i586"
    String wdir = null, idir = null;

    //stem.out.println("os  = " + os);
    //stem.out.println("ver = " + ver);
    if ( os.toLowerCase().startsWith("windows") ) {
      String subdir = System.getProperty("file.separator", "/") +
                      "Night Vision";
      StringTokenizer t = new StringTokenizer(ver, "[., -]");
      int i = Integer.parseInt(t.nextToken());
      if ( i >= 5 ) { // Windoze XP or higher
        try {
          BufferedReader br;
          Process p;
          String query;

          /* Retrieve "Documents" directory (for txt file storage) */
          // Target line in a typical response:
          //     Personal    REG_SZ  C:\Documents and Settings\<user>\
          //                              My Documents
          // Note: www.rgagnon.com/javadetails/java-0480.html
          // runs the query in a separate thread.  Might explore sometime...
          query = QBase + "Personal";
          p = Runtime.getRuntime().exec(query);
          br = new BufferedReader
               ( new InputStreamReader( p.getInputStream() ) );
          String line;
          while ( (line = br.readLine()) != null ) {
            String[] words = line.trim().split("[ \t]+", 3);
            if ( words.length == 3 && words[0].equals("Personal") ) {
              wdir = words[2];
              break;
            }
          }

          /* Retrieve "AppData" directory (for ini file storage) */
          // Target line in a typical response:
          //     AppData     REG_SZ  C:\Documents and Settings\<user>\
          //                              Application Data
          // Win 7 typical location:
          //   C:\Users\<user>\AppData\Roaming
          query = QBase + "AppData";
          p = Runtime.getRuntime().exec(query);
          br = new BufferedReader
               ( new InputStreamReader( p.getInputStream() ) );
          while ( (line = br.readLine()) != null ) {
            String[] words = line.trim().split("[ \t]+", 3);
            if ( words.length == 3 && words[0].equals("AppData") ) {
              idir = words[2];
              break;
            }
          }
        }
        catch ( Exception e ) {}
      }

      /* If "Documents" retrieval successful, set workingDir */
      if ( wdir != null ) {
        //stem.out.println("Working directory = " + wdir);
        workingDir = wdir + subdir;
      }

      /* If "AppData" retrieval successful, set iniDir */
      if ( idir != null ) {
        //stem.out.println("Ini directory = " + idir);
        if ( new File(idir).exists() ) {
          if ( new File(idir + subdir).exists() )
            iniDir = idir + subdir;
          else {
            if ( new File(idir + subdir).mkdir() )
              iniDir = idir + subdir;
            else
              iniDir = idir;
          }
        }
      }
    } // End if Windoze
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Checks for "-cleanup" as a single arg.  If there, remove the ini file.
   * Intended to be called by Windoze uninstaller.
   */
  private static void checkForCleanup(String[] args) {
    if ( args.length == 1 && args[0].equalsIgnoreCase("-cleanup") ) {
      setWorkingDir(); // Sets iniDir
      File ini = new File(iniDir, Initor.INIFILE);
      if ( ini.exists() ) {
        ini.delete();
      }
      System.exit(0);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Checks command line arguments
   */
  private static void checkArgs(String[] args, Preferences prefer) {
    for ( int i = 0; i < args.length; i++ ) {
      /* Look for - or / */
      if ( (args[i].charAt(0) == '-' || args[i].charAt(0) == '/') &&
           args[i].length() > 1 ) {
        String arg = args[i].substring(1);
        if      ( arg.equalsIgnoreCase("noantialias") ) // Undocumented
          prefer.antialiasing = false;                  // Put in new window?
        else if ( arg.equalsIgnoreCase("option1") ) // Test/debug purposes
          Preferences.option1 = true;
        else if ( arg.equalsIgnoreCase("option2") ) // Test/debug purposes
          Preferences.option2 = true;
        else if ( arg.equalsIgnoreCase("geocentric") ) // Undocumented
          Preferences.geocentric = true;
        else if ( arg.equalsIgnoreCase("nodeltat") ) // Undocumented
          Preferences.usedeltat = false;
        else if ( arg.equalsIgnoreCase("shadehorizon") ) // Undocumented
          prefer.shadeHorizon = true;                    // Put in new window?
        //else if ( arg.equalsIgnoreCase("popup") )
        //  popupEnabled = true;
        else if ( arg.equalsIgnoreCase("swapmouse") ) // Undocumented
          prefer.swapMouse();                         // Put in new window?
        else if ( arg.equalsIgnoreCase("noctstate") ) // Undocumented
          showCompTimeState = false;                  // Eliminate eventually
      }
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Used for limiting the time of a beta release.
   */
  private static void time_bomb() {
    // int Year = 2015, Month = Calendar.APRIL;
    // Calendar cal = new GregorianCalendar();
    // int year = cal.get(Calendar.YEAR);
    // int month = cal.get(Calendar.MONTH);
    // if ( year > Year || (year == Year && month > Month) )
    //   ErrLogger.die("Beta period exceeded.");
    // DateFormatSymbols dfs = new DateFormatSymbols();
    // String[] months = dfs.getMonths();
    // System.out.println("Beta period extends through " + months[Month] +
    //                    " " + Year);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Checks if Java version is older (less than) minjavareq.
   */
  private static final void checkForOldJava() {
    if ( javaver.substring(0, 3).compareTo(minjavareq) < 0 )
      ErrLogger.die(ErrLogger.formatError(TextBndl.getString("Pgm.OldJava"),
                                          minjavareq, null));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets the image icon for the given path.
   *
   * @param path the path (should begin with '/')
   * @return the image icon or null if there was an error
   */
  public static ImageIcon getImageIcon(String path) {
    URL url = Nvj.class.getResource(path);
    if ( url == null ) return null;
    return new ImageIcon(url);
  }
}

