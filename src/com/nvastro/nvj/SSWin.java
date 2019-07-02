/*
 * SSWin.java  -  Solar System window
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
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage; // For 2nd thread
import java.util.Calendar;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
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
import javax.swing.JScrollBar;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Vector3 class.  Used for describing 3 dimensional vectors
 * and their interactions.
 *
 * @author Brian Simpson
 */
class Vector3 {
  public double x, y, z;

  Vector3() {
    x = y = z = 1;
  }

  Vector3(double x, double y, double z) {
    this.x = x; this.y = y; this.z = z;
  }

  double magnitude() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  double dotproduct(Vector3 v) {
    return (x * v.x + y * v.y + z * v.z);
  }

  Vector3 crossproduct(Vector3 v) {
    return new Vector3(y * v.z - z * v.y,
                       z * v.x - x * v.z,
                       x * v.y - y * v.x);
  }

  Vector3 multiply(double m) {
    return new Vector3(m * x, m * y, m * z);
  }

  Vector3 add(Vector3 v) {
    return new Vector3(x + v.x, y + v.y, z + v.z);
  }

  Vector3 sub(Vector3 v) {
    return new Vector3(x - v.x, y - v.y, z - v.z);
  }

  double angle(Vector3 v) {
    double num = this.dotproduct(v);
    double denom = this.magnitude() * v.magnitude();
    if ( denom == 0 || num > denom ) return 0.0;
    else return Math.acos(num / denom);
  }

  double angleindegrees(Vector3 v) {
    return (angle(v) * 180 / Math.PI);
  }
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Orbit data class.  Basically just a public structure to hold orbit info.
 *
 * @author Brian Simpson
 */
class OrbitData {
  public double p;     // Semiparameter (AU)
  public double e;     // Eccentricity
  public double i;     // Inclination (radians)
  public double Omega; // RA of ascending node (radians)
  public double omega; // Argument of periapsis (radians)
  public double nu;    // True anomaly (radians)

  public double t0;    // Julian time for orbit

  public double[] x;   // x position every 2 degrees
  public double[] y;   // y position every 2 degrees
  public double[] z;   // z position every 2 degrees

  public int near;

  public String name;

  OrbitData(String name) {
    this.name = TextBndl.getString(name);
    x = new double[180];
    y = new double[180];
    z = new double[180];
    t0 = 0;
  }
}

/*------------------------------------------------------------------------------
 * Warning:  The following process of computing orbital elements is somewhat
 * overly simplified, but still produces reasonable visual results...
 -----------------------------------------------------------------------------*/

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Solar System window.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class SSWin extends JFrame implements ActionListener,
                                  Runnable,  // For 2nd thread
                                  MouseWheelListener,
                                  AdjustmentListener {
  static private SSWin sswin = null;
  private JComponent starwin;          // starwin within main window
  private Preferences prefer;          // User preferences
  private JScrollBar rbar;             // Rotate scrollbar
  private JScrollBar tbar;             // Tilt scrollbar
  private JScrollBar zbar;             // Zoom scrollbar
  private JComponent ssw;              // Client area
  private JMenuItem inner, outer, all, scroll, time;
  private JMenu separator;
  private Component space1, space2, space3, space4;
  private JButton timeadd, timesub;
  private JToggleButton timecptr;
  private JComboBox<String> timestep;
  private JLabel localtime;
  private boolean showinner, showouter, showscroll, showtime;
  private double zoomfactor;
  private int wScrn, hScrn;            // Width & height of screen

  // Drawing parameters
  private int tilt, rotate, zoom; // ints are atomic
  private volatile double jd;     // Volatile for atomic access among threads

  // For 2nd thread
  /* This window was originally written single threaded to debug the
     orbital calculations.  Once the correct image was built, the image
     building routines were moved to a second thread (even though GUI
     response was still good on a slow computer despite all the numerical
     calculations).  Ability to run single threaded retained.  */
  final static boolean Thread2 = true; // Set to false for single threaded
  private BufferedImage bufImage1;     // Off-screen image of entire screen
  private BufferedImage bufImage2;     // Off-screen image of entire screen
  private Graphics2D bufGraph1;        // 2D graphics for bufImage1
  private Graphics2D bufGraph2;        // 2D graphics for bufImage2
  private BufferedImage xfrImage;      // Will point to bufImage1 or 2
  private Graphics2D bldGraph;         // Will point to bufGraph2 or 1
  private boolean update = false;      // Access only when synchronized

  // Need Java 1.5: private Vector<OrbitData> orbits;
  private Vector<OrbitData> orbits;
  private NearSkyDB planets;

  // Canonical time unit ("Fundamentals of Astrodynamics"
  // by Bate, Mueller, White; 1971, P. 429)
  final static private double TU = 58.132821; // 58.132821 days
  // Choosing a time unit of 58.132821 days allows mu to be unity:
  final static private double mu = 1.0;  // AU^3/TU^2
  final static private double TwoPI = 2 * Math.PI;
  final static private double D2R = Math.PI / 180; // Degrees to radians
  final static private Vector3 zaxis = new Vector3(0, 0, 1);

  // Precession adjustment factor = amount the vernal equinox regresses
  // in radians per day (based on 360 degrees over 26000 years)
  final static private double PAF = D2R * 360 / (26000 * 365.25);

  // Table for planet 1/20 orbital periods (exactness unnecessary)
  final static private double[] T2 = { // Derive 1/20 orbit in days using
    // year data from http://en.wikipedia.org/wiki/Orbital_period
    //  Merc  Venus  Earth   Mars  Juptr  Satrn  Urans  Neptn  Pluto
    // 0.241, 0.615,   1,   1.881, 11.87, 29.45, 84.07, 164.9, 248.1 years
    // 88.03, 224.6, 365.2, 687.0,  4336, 10757, 30707, 60230, 90619 days
       4.401, 11.23, 18.26, 34.35, 216.8, 537.8,  1535,  3011,  4531 }; // /20
  // For looking up planet names
  final static private String[] Names = { // Keep in sync with above
    "NS.Mercury", "NS.Venus", "NS.Earth", "NS.Mars", "NS.Jupiter",
    "NS.Saturn", "NS.Uranus", "NS.Neptune", "NS.Pluto" };
  final static private int NumPlanets = T2.length;

  final static private int TSCRLMAX  = 360; // Max value for vert (tilt) scroll
  final static private int RSCRLMAX  = 360; // Max value for horz(rotate) scroll
  final static private int ZSCRLMAX  = 180; // Max value for zoom scroll
  final static private int SCROLLINC = 15;  // 15 degrees block increment (step)

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the Solar System window.
   */
  public static void showWindow(JComponent starwin, Preferences prefer) {
    if ( sswin == null )
      sswin = new SSWin(starwin, prefer);

    if ( sswin.getState() == Frame.ICONIFIED ) sswin.setState(Frame.NORMAL);
    sswin.setVisible(true);
    sswin.toFront();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   */
  private SSWin(JComponent starwin, Preferences prefer) {
    super(TextBndl.getString("SSWin.Title"));
    this.starwin = starwin;
    this.prefer = prefer;
    jd = prefer.lst.getJulianEphDay();
    wScrn = Nvj.dimScrn.width;  // Width of screen
    hScrn = Nvj.dimScrn.height; // Height of screen

    /* Handle closing (keep dft HIDE_ON_CLOSE) */
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) // Called when closed
      {                                        // with system menu
        if ( Nvj.parentFrame != null ) // If created from main NV window
          Nvj.parentFrame.repaint();   // Clean up any residue if exposing
          // any of the parent window, which at one time seemed necessary
          // with early versions of Java, but is likely no longer necessary
        else System.exit(0);           // Else created from main below
      }
    });

    /* Set the icon for this frame */
    setIconImage(Nvj.getImageIcon("/com/nvastro/nvj/hbsmall.jpg").getImage());

    /* Set up planets (position data) and orbits (orbit data) */
    planets = new NearSkyDB();
    orbits = new Vector<OrbitData>(NumPlanets);
    for ( int i = 0; i < NumPlanets; i++ )
      orbits.addElement(new OrbitData(Names[i]));

    /* Create a client area to display Sun, planets, and orbits */
    ssw = new JComponent() {
      @Override
      public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        if ( !Thread2 ) {
          g.translate(w/2, h/2);
          SSWin.this.paintSS((Graphics2D)g);
        }
        else {
          int x = (SSWin.this.wScrn - w) / 2;
          int y = (SSWin.this.hScrn - h) / 2;
          synchronized ( ssw ) {
            // Not sure if synchronized is needed, but playing it safe
            // to prevent 2nd thread from changing xfrImage reference
            g.drawImage(SSWin.this.xfrImage, 0, 0, w, h,
                        x, y, x + w, y + h, null);
          }
        }
      }
    };

    /* Create menubar */
    JMenuBar menubar = new JMenuBar();
    JMenu file = new JMenu(TextBndl.getString("M.File"));
    JMenuItem save = new JMenuItem(TextBndl.getString("M.File.Save_win"));
    save.setActionCommand("save");
    save.addActionListener(this);
    file.add(save);
    file.addSeparator();
    JMenuItem close = new JMenuItem(TextBndl.getString("M.File.Close"));
    close.setActionCommand("close");
    close.addActionListener(this);
    file.add(close);
    menubar.add(file);
    JMenu options = new JMenu(TextBndl.getString("M.Opt"));
    inner  = new JMenuItem(TextBndl.getString("M.Opt.Inner"));
    inner.setActionCommand("inner");
    inner.addActionListener(this);
    outer  = new JMenuItem(TextBndl.getString("M.Opt.Outer"));
    outer.setActionCommand("outer");
    outer.addActionListener(this);
    all    = new JMenuItem(TextBndl.getString("M.Opt.All"));
    all.setActionCommand("all");
    all.addActionListener(this);
    scroll = new JMenuItem(TextBndl.getString("M.Opt.ScrollOn"));
    scroll.setActionCommand("scroll");
    scroll.addActionListener(this);
    showscroll = false;  // false means scrollbars aren't showing
                         // and menu item says "Show scrollbars"
    time = new JMenuItem(TextBndl.getString("M.Opt.TimeCtrlOff"));
    time.setActionCommand("time");
    time.addActionListener(this);
    showtime = true; // true means time controls are showing
                     // and menu item says "Hide time controls"
    options.add(inner);
    options.add(outer);
    showinner = showouter = true;
    all.setEnabled(false);
    options.add(all);
    options.add(scroll);
    options.add(time);
    menubar.add(options);
    JMenu help = new JMenu(TextBndl.getString("M.Help"));
    JMenuItem help_gen = new JMenuItem(TextBndl.getString("M.Help.Gen"));
    help_gen.setActionCommand("help_gen");
    help_gen.addActionListener(this);
    help_gen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
    help.add(help_gen);
    JMenuItem help_ss = new JMenuItem(TextBndl.getString("M.Help.SS"));
    help_ss.setActionCommand("help_ss");
    help_ss.addActionListener(this);
    help_ss.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
    help.add(help_ss);
    menubar.add(help);

    /* Time controls */
    Insets btnInsets = new Insets(0, 0, 0, 0);
    menubar.add(space1 = Box.createHorizontalStrut(5));
    separator = new JMenu("|");
    separator.setEnabled(false);
    menubar.add(separator);
    menubar.add(space2 = Box.createHorizontalStrut(10));
    timeadd = new JButton(Nvj.getImageIcon("/com/nvastro/nvj/timeadd.gif"));
    timeadd.setMargin(btnInsets);
    timeadd.setToolTipText(TextBndl.getString("TB.AddTime"));
    timeadd.setActionCommand("timeadd");
    timeadd.addActionListener(this);
    menubar.add(timeadd);
    timestep = new JComboBox<String>();
    timestep.addItem(TextBndl.getString("TB.Hour"));
    timestep.addItem(TextBndl.getString("TB.Day"));
    timestep.addItem(TextBndl.getString("TB.Week"));
    timestep.addItem(TextBndl.getString("TB.Month"));
    timestep.addItem(TextBndl.getString("TB.Year"));
    timestep.setFont(new Font("Dialog", Font.BOLD, 11));
    timestep.setMaximumSize(timestep.getMinimumSize());
    timestep.setSelectedIndex(1);
    menubar.add(timestep);
    timesub = new JButton(Nvj.getImageIcon("/com/nvastro/nvj/timesub.gif"));
    timesub.setMargin(btnInsets);
    timesub.setToolTipText(TextBndl.getString("TB.SubTime"));
    timesub.setActionCommand("timesub");
    timesub.addActionListener(this);
    menubar.add(timesub);
    menubar.add(space3 = Box.createHorizontalStrut(2));
    timecptr = new JToggleButton(
      Nvj.getImageIcon("/com/nvastro/nvj/computer.gif"));
    prefer.setCompTimeBtn(timecptr);
    timecptr.setMargin(btnInsets);
    timecptr.setToolTipText(TextBndl.getString("TB.CompTime"));
    timecptr.setActionCommand("timecptr");
    timecptr.addActionListener(this);
    menubar.add(timecptr);
    menubar.add(space4 = Box.createHorizontalStrut(7));
    localtime = new JLabel(prefer.lst.tellLocDateTime(!prefer.is24Hr()));
    menubar.add(localtime);

    setJMenuBar(menubar);

    /* Create scrollbars */
    tbar = new NFScrollBar(JScrollBar.VERTICAL);
    rbar = new NFScrollBar(JScrollBar.HORIZONTAL);
    zbar = new NFScrollBar(JScrollBar.VERTICAL);

    /* Setup tilt scrollbar */
    /* scroll value of 0        = top    of scrollbar -> looking down
       scroll value of TSCRLMAX = bottom of scrollbar -> looking down */
    tilt = 0;
    tbar.setValues(tilt, SCROLLINC, 0, TSCRLMAX + SCROLLINC);
    tbar.setUnitIncrement(1); tbar.setBlockIncrement(SCROLLINC);

    /* Setup rotate scrollbar */
    /* scroll value of 0        = left  side of scrollbar
       scroll value of RSCRLMAX = right side of scrollbar */
    rotate = 180;
    rbar.setValues(rotate, SCROLLINC, 0, RSCRLMAX + SCROLLINC);
    rbar.setUnitIncrement(1); rbar.setBlockIncrement(SCROLLINC);

    /* Setup zoom scrollbar */
    /* scroll value of 0        = top    of scrollbar -> max zoom
       scroll value of ZSCRLMAX = bottom of scrollbar -> min zoom */
    zoom = ZSCRLMAX - 2 * SCROLLINC;
    zbar.setValues(zoom, SCROLLINC, 0, ZSCRLMAX + SCROLLINC);
    zbar.setUnitIncrement(1); zbar.setBlockIncrement(SCROLLINC);

    /* Setup zoomfactor to reflect monitor resolution */
    // Want both Pluto's orbit at minimum zoom and Mar's orbit
    // at maximum zoom to fit comfortably on screen.
    // A zoomfactor of 1 gave acceptable results on a 900 pixel
    // high monitor.  Scale it similarly for other size monitors.
    zoomfactor = Math.min(wScrn, hScrn) / 900.0;

    /* If Thread2 create off-screen buffers and start 2nd thread */
    if ( Thread2 ) {
      // Create 2 image buffers and associated Graphics objects
      bufImage1 = LocalGraphics.getBufferedImage(wScrn, hScrn);
      bufGraph1 = bufImage1.createGraphics();
      bufImage2 = LocalGraphics.getBufferedImage(wScrn, hScrn);
      bufGraph2 = bufImage2.createGraphics();

      // Clear the initial image that will be xfer'd to screen
      bufGraph1.setColor(prefer.colorBackGnd());
      bufGraph1.fillRect(0, 0, wScrn, hScrn);

      // Translate coordinates so that (0, 0) is in the middle
      bufGraph1.translate(wScrn/2, hScrn/2);
      bufGraph2.translate(wScrn/2, hScrn/2);

      // Set up pointers (to be changed after each image build)
      xfrImage = bufImage1; // Image to be xfer'd to screen
      bldGraph = bufGraph2; // Graphics to be written to

      // Start 2nd thread
      Thread imageBuilder = new Thread(this);
      imageBuilder.setDaemon(true);
      imageBuilder.start();
    }

    /* Setup listener */
    tbar.addAdjustmentListener(this);
    rbar.addAdjustmentListener(this);
    zbar.addAdjustmentListener(this);

    /* Create a constraints object to control placement of
       components, and set some defaults */
    Container cp = getContentPane();
    cp.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 0);
    // Dft value for c.anchor = CENTER

    /* Add ssw */
    c.gridwidth = c.gridheight = 1;
    c.gridx = 1; c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    c.weightx = c.weighty = 1.0;
    cp.add(ssw, c);
    /* Add tbar */
    c.gridx = 2; c.gridy = 0;
    c.fill = GridBagConstraints.VERTICAL;
    c.weightx = 0.0; c.weighty = 1.0;
    cp.add(tbar, c);
    /* Add rbar */
    c.gridx = 1; c.gridy = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1.0; c.weighty = 0.0;
    cp.add(rbar, c);
    /* Add zbar */
    c.gridx = 0; c.gridy = 0;
    c.fill = GridBagConstraints.VERTICAL;
    c.weightx = 0.0; c.weighty = 1.0;
    cp.add(zbar, c);

    /* MouseWheelListener */
    ssw.addMouseWheelListener(this);
    zbar.addMouseWheelListener(this);
    rbar.addMouseWheelListener(this);
    tbar.addMouseWheelListener(this);

    /* MouseInputAdapter for mouse dragging */
    MouseInputAdapter mia = new MouseInputAdapter() {
      boolean moving = false;
      int x, y, w, h, t, r;
      @Override
      public void mousePressed(MouseEvent e) {
        if ( e.getButton() == MouseEvent.BUTTON1 ) {
          w = ssw.getWidth();
          h = ssw.getHeight();
          t = tbar.getValue();
          r = rbar.getValue();
          x = e.getX();
          y = e.getY();
          moving = true;
        }
      }
      @Override
      public void mouseDragged(MouseEvent e) {
        if ( moving ) {
          int tnew = (t + 4 * 180 * (e.getY() - y) / (h * 3));
          while ( tnew > 360 ) tnew -= 360;
          while ( tnew <  0  ) tnew += 360;
          tbar.setValue(tnew);

          int rnew = (r + 4 * 360 * (e.getX() - x) / (w * 3));
          while ( rnew > 360 ) rnew -= 360;
          while ( rnew <  0  ) rnew += 360;
          rbar.setValue(rnew);
        }
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        if ( e.getButton() == MouseEvent.BUTTON1 ) {
          moving = false;
        }
      }
    };
    ssw.addMouseListener(mia);
    ssw.addMouseMotionListener(mia);

    /* Add key handling */
    addKeyBindings();

    /* Size, position, and display */
    tbar.setVisible(showscroll);
    rbar.setVisible(showscroll);
    zbar.setVisible(showscroll);
    space1.setVisible(showtime);
    space2.setVisible(showtime);
    space3.setVisible(showtime);
    space4.setVisible(showtime);
    separator.setVisible(showtime);
    timeadd.setVisible(showtime);
    timestep.setVisible(showtime);
    timesub.setVisible(showtime);
    timecptr.setVisible(showtime);
    localtime.setVisible(showtime);
    Rectangle rect = prefer.getSSWindow();
    int x = (int)rect.getX();
    int y = (int)rect.getY();
    int w = (int)rect.getWidth();
    int h = (int)rect.getHeight();
    w = Math.max(100, Math.min(w, wScrn));
    h = Math.max(100, Math.min(h, hScrn));
    x = Math.max(0, Math.min(x, wScrn - w));
    y = Math.max(0, Math.min(y, hScrn - h));
    setLocation(x, y);
    setSize(w, h);
    setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add key bindings.
   */
  protected void addKeyBindings() {
    InputMap imap = ssw.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap amap = ssw.getActionMap();

    amap.put("lineup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = tbar.getValue();
        if ( --v < 0 ) v += TSCRLMAX;
        tbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,Event.SHIFT_MASK), "lineup");

    amap.put("pageup", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = tbar.getValue();
        v -= SCROLLINC;
        if ( v < 0 ) v += TSCRLMAX;
        tbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.CTRL_MASK), "pageup");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, Event.SHIFT_MASK),
             "pageup");

    amap.put("linedown", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = tbar.getValue();
        if ( ++v > TSCRLMAX ) v -= TSCRLMAX;
        tbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.SHIFT_MASK),
             "linedown");

    amap.put("pagedown", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = tbar.getValue();
        v += SCROLLINC;
        if ( v > TSCRLMAX ) v -= TSCRLMAX;
        tbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.CTRL_MASK),
             "pagedown");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, Event.SHIFT_MASK),
             "pagedown");

    amap.put("lineright", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = rbar.getValue();
        if ( ++v > RSCRLMAX ) v -= RSCRLMAX;
        rbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.SHIFT_MASK),
             "lineright");

    amap.put("pageright", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = rbar.getValue();
        v += SCROLLINC;
        if ( ++v > RSCRLMAX ) v -= RSCRLMAX;
        rbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Event.CTRL_MASK),
             "pageright");

    amap.put("lineleft", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = rbar.getValue();
        if ( --v < 0 ) v += RSCRLMAX;
        rbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.SHIFT_MASK),
             "lineleft");

    amap.put("pageleft", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = rbar.getValue();
        v -= SCROLLINC;
        if ( --v < 0 ) v += RSCRLMAX;
        rbar.setValue(v);
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.CTRL_MASK),
             "pageleft");

    amap.put("zoomin", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = zbar.getValue();
        if ( v > 0 ) zbar.setValue(Math.max(0, v - SCROLLINC));
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), "zoomin");

    amap.put("zoomout", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        int v = zbar.getValue();
        if ( v < ZSCRLMAX ) zbar.setValue(Math.min(ZSCRLMAX, v + SCROLLINC));
      }
    });
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.SHIFT_MASK),"zoomout");
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements AdjustmentListener interface.
   *
   * @param e AdjustmentEvent
   */
  public void adjustmentValueChanged(AdjustmentEvent e) {
    boolean moved = false;

    int sv = e.getValue();

    if ( e.getAdjustable() == tbar ) {
      if ( tilt != sv ) { tilt = sv; moved = true; }
    }
    else if ( e.getAdjustable() == rbar ) {
      if ( rotate != sv ) { rotate = sv; moved = true; }
    }
    else if ( e.getAdjustable() == zbar ) {
      if ( zoom != sv ) { zoom = sv; moved = true; }
    }
    if ( moved ) restartPaint();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Called locally and by StarWin when time may have updated, either via a
   * timer or by user action via the GUI.
   */
  static public void updateTime() {
    if ( sswin != null ) {
      sswin.localtime.setText(
        sswin.prefer.lst.tellLocDateTime(!sswin.prefer.is24Hr()));
      double j = sswin.prefer.lst.getJulianEphDay();
      if ( sswin.jd != j ) {  // If time really changed
        sswin.jd = j;
        sswin.restartPaint();
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Calls ssw.repaint() (eventually if 2nd thread).
   */
  public void restartPaint() {
    if ( !Thread2 ) {
      ssw.repaint();
    }
    else {
      synchronized ( ssw ) {
        // "update" is a flag to indicate that parameters have changed.
        // It can be argued that a stable copy should be made of parameters:
        //   zoom, tilt, rotate, and jd
        // so they won't be changed by the GUI thread while being accessed
        // by the image build thread.  However their access is atomic
        // ((double)jd is volatile to ensure this), therefore it's OK if
        // both threads use the same parameters, and the image build thread
        // will always have the latest values.  For atomic info see:
        // http://download.oracle.com/javase/tutorial/essential/\
        //   concurrency/atomic.html

        update = true; // True means a new image should be built
        //stem.out.print("t"); // Debugging aid
        ssw.notify();  // Release 2nd thread if it is waiting
        // (which happens when this thread leaves synchronized )
        // http://download.oracle.com/javase/1.4.2/docs/api/java/lang/\
        //   Object.html#notify%28%29
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * For 2nd thread.
   */
  public void run() {
    Thread me = Thread.currentThread();
    me.setPriority(me.getPriority() - 2);

    while ( true ) {
      paintSS(bldGraph);

      synchronized ( ssw ) {
        /* New image built, swap buffers to show it */
        if ( xfrImage == bufImage1 ) {
          xfrImage = bufImage2;
          bldGraph = bufGraph1;
        } else {
          xfrImage = bufImage1;
          bldGraph = bufGraph2;
        }

        ssw.repaint();  // Tell GUI thread to paint new image

        /* If update has been set while image was being built, proceed
           immediately to rebuild another one; else wait for notify */
        if ( update == false ) {
          try { ssw.wait(); } catch ( Exception e ) {} // Wait for notify
        }
        update = false;
        //stem.out.print("f"); // Debugging aid
      }
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Paints the solar system window.
   *
   * @param g Graphics2D context to use for painting
   */
  private void paintSS(Graphics2D g) {
    g.setColor(prefer.colorBackGnd());
    if ( !Thread2 ) {
      g.fillRect(-getWidth()/2, -getHeight()/2,
                  getWidth(),    getHeight());
    }
    else {
      g.fillRect(-wScrn/2, -hScrn/2, wScrn, hScrn);
    }

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                       RenderingHints.VALUE_ANTIALIAS_ON);

    Ellipse2D.Float ellipse = new Ellipse2D.Float();

    double rotation = (rotate - 180) * Math.PI / 180; // Shift by 180 degrees

    int tlt = tilt;  // Don't want the GUI thread to change the value of tilt
    // between the next 2 statements, so copy it to tlt and use the copy
    double costilt = Math.cos(tlt * Math.PI / 180);
    double sintilt = Math.sin(tlt * Math.PI / 180);
    double asintilt = Math.abs(sintilt);

    // The following (done by trial & error) produces a good zoom range
    double zm = zoomfactor * 7 * Math.exp((ZSCRLMAX - zoom)/50.0);

    // t = Julian millennia from J2000.0
    double t = (jd - 2451545.0) / 365250;

    int planet;
    OrbitData orbit;
    for ( planet = 0; planet < NumPlanets; planet++ ) {
      // Need Java 1.5: orbit = orbits.elementAt(planet);
      orbit = (OrbitData)orbits.elementAt(planet);

      /* Generate Keplerian orbit elements */
      genKepOrbElements(planet, t, orbit);

      /* Method for deriving screen coordinates from Keplerian orbit elements
         1) Orbit will be initially layed out on an xy plane where
            periapsis occurs along the positive x axis.  The derived (x,y)
            values will be represented by (rp,rq), and they will be scaled
            by zoom (vertical scrollbar on left side).
         2) (rp,rq) will be multiplied by rot, a rotation matrix representing
            Omega, omega, i, and rotation (RA of ascending node, argument of
            periapsis, inclination, horizontal scrollbar on bottom).
         3) The resulting (x,y,z) values will be rotated by tilt (vertical
            scrollbar on right side).  The final values are:
            orbit.x[]  ->  x pixels from sun (positive is to right of sun)
            orbit.y[]  ->  y pixels from sun (positive is below sun)
            orbit.z[]  ->  used to determine if nearer/farther from sun
      */

      /* Implement rotation matrix */
      double[][] rot = new double[3][2]; // Normally [3][3] but since orbit
        // will be 1st laid out in xy plane, last column unneeded
      double cosin = Math.cos(orbit.i);
      double sinin = Math.sin(orbit.i);
      double cosOm = Math.cos(orbit.Omega + rotation);
      double sinOm = Math.sin(orbit.Omega + rotation);
      double cosom = Math.cos(orbit.omega);
      double sinom = Math.sin(orbit.omega);
      rot[0][0] =   cosOm * cosom - sinOm * sinom * cosin;
      rot[0][1] = - cosOm * sinom - sinOm * cosom * cosin;
      /* [0][2] =   sinOm * sinin;                          Not needed */
      rot[1][0] =   sinOm * cosom + cosOm * sinom * cosin;
      rot[1][1] = - sinOm * sinom + cosOm * cosom * cosin;
      /* [1][2] = - cosOm * sinin;                          Not needed */
      rot[2][0] =   sinom * sinin;
      rot[2][1] =   cosom * sinin;
      /* [2][2] =   cosin;                                  Not needed */

      double p = orbit.p * zm;
      double nu = orbit.nu;
      double cosnu, sinnu, rp, rq, x, y, z;
      for ( int n = 0; n < 180; n++ ) {
        cosnu = Math.cos(nu);
        sinnu = Math.sin(nu);
        // Pre-rotated              p
        //   orbit    r = ---------------------
        // (xy plane)     1 + orbit.e * cos(nu)
        rp = p * cosnu / (1 + orbit.e * cosnu);  // x component
        rq = p * sinnu / (1 + orbit.e * cosnu);  // y component
        // rw = 0;                               // z component = 0

        x =   rot[0][0] * rp + rot[0][1] * rq /* + rot[0][2] * rw */;
        y = - rot[1][0] * rp - rot[1][1] * rq /* - rot[1][2] * rw */;
         // - since y > 0 is downward on monitor screen...
        z =   rot[2][0] * rp + rot[2][1] * rq /* + rot[2][2] * rw */;

        orbit.x[n] = x;
        orbit.y[n] = y * costilt - z * sintilt;
        orbit.z[n] = z * costilt + y * sintilt;

        nu += .0349066; // Approx. 2 degree increment
      }
    }

    /* Find point in each orbit where z transitions from - to + */
    for ( planet = 0; planet < NumPlanets; planet++ ) {
      // Need Java 1.5: orbit = orbits.elementAt(planet);
      orbit = (OrbitData)orbits.elementAt(planet);
      orbit.near = -1;
      boolean z, zold = true;
      for ( int n = 0; n < 360; n++ ) {
        z = (orbit.z[n%180] >= 0);
        if ( z == true && zold == false ) {
          orbit.near = n%180;
          break;
        }
        zold = z;
      }
      if ( orbit.near == -1 ) // Just in case...
        orbit.near = 0;       // (Shouldn't happen)
    }

    // Draw horizontal dash at transition point (debugging purposes)
    //g.setColor(Color.green);
    //double x, y;
    //for ( planet = 0; planet < NumPlanets; planet++ ) {
    //  if ( planet < 4 && !showinner ) continue;
    //  if ( planet > 3 && !showouter ) continue;
    //  // Need Java 1.5: orbit = orbits.elementAt(planet);
    //  orbit = (OrbitData)orbits.elementAt(planet);
    //  if ( orbit.near >= 0 ) {
    //    x = orbit.x[orbit.near];
    //    y = orbit.y[orbit.near];
    //    g.drawLine((int)(x-5), (int)(y), (int)(x+5), (int)(y));
    //  }
    //}

    // Note: Each step along orbits = ~ 2 degrees
    // (each orbit defined by 180 points)

    /* Draw orbital arcs and planets farther than sun */
    for ( planet = NumPlanets-1; planet >= 0; planet-- ) {
      if ( planet < 4 && !showinner ) continue;
      if ( planet > 3 && !showouter ) continue;
      // Need Java 1.5: orbit = orbits.elementAt(planet);
      orbit = (OrbitData)orbits.elementAt(planet);
      drawOrbitArc(g, orbit,  90,  97, 0.0);
      drawOrbitArc(g, orbit,  97, 112, -20*asintilt);
      drawOrbitArc(g, orbit, 112, 127, -35*asintilt);
      drawOrbitArc(g, orbit, 127, 142, -40*asintilt);
      drawOrbitArc(g, orbit, 142, 157, -35*asintilt);
      drawOrbitArc(g, orbit, 157, 172, -20*asintilt);
      drawOrbitArc(g, orbit, 172, 180, 0.0);

      /* Draw planet if farther than sun */
      if ( orbit.z[0] < 0 ) {
        g.setColor(prefer.colorPlanet());
        ellipse.setFrame(orbit.x[0] - 4, orbit.y[0] - 4, 9, 9);
        g.fill(ellipse);
      }
    }

    /* Draw sun */
    g.setColor(prefer.colorSun());
    ellipse.setFrame(-6, -6, 13, 13);
    g.fill(ellipse);

    /* Draw orbital arcs and planets nearer than sun */
    for ( planet = 0; planet < NumPlanets; planet++ ) {
      if ( planet < 4 && !showinner ) continue;
      if ( planet > 3 && !showouter ) continue;
      // Need Java 1.5: orbit = orbits.elementAt(planet);
      orbit = (OrbitData)orbits.elementAt(planet);
      drawOrbitArc(g, orbit,  0,  7, 0.0);
      drawOrbitArc(g, orbit,  7, 22, 20*asintilt);
      drawOrbitArc(g, orbit, 22, 37, 35*asintilt);
      drawOrbitArc(g, orbit, 37, 52, 40*asintilt);
      drawOrbitArc(g, orbit, 52, 67, 35*asintilt);
      drawOrbitArc(g, orbit, 67, 82, 20*asintilt);
      drawOrbitArc(g, orbit, 82, 90, 0.0);

      /* Draw planet if nearer than sun (or at same distance) */
      if ( orbit.z[0] >= 0 ) {
        g.setColor(prefer.colorPlanet());
        ellipse.setFrame(orbit.x[0] - 4, orbit.y[0] - 4, 9, 9);
        g.fill(ellipse);
      }
    }

    /* Planet names */
    FontMetrics fm = g.getFontMetrics();
    int yoffset = fm.getDescent() + 5;  // Descent + 5 pixels
    g.setColor(prefer.colorPlanet());
    for ( planet = 0; planet < NumPlanets; planet++ ) {
      if ( planet < 4 && !showinner ) continue;
      if ( planet > 3 && !showouter ) continue;
      // Need Java 1.5: orbit = orbits.elementAt(planet);
      orbit = (OrbitData)orbits.elementAt(planet);
      String name = orbit.name;
      g.drawString(name, (int)(orbit.x[0]) - fm.stringWidth(name) / 2,
                         (int)(orbit.y[0]) - yoffset);
    }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Draws a portion of an orbit.
   *
   * @param g Graphics2D context to use for painting
   * @param orbit OrbitData class containing orbit position points
   * @param start Starting point of arc
   * @param stop Ending point of arc
   * @param adjust Brightness adjustment (0 = half planet brightness, 100 =
   *        planet brightness, -100 = no brightness)
   */
  private void drawOrbitArc(Graphics2D g, OrbitData orbit,
                            int start, int stop, double adjust) {
    GeneralPath gp = new GeneralPath(GeneralPath.WIND_NON_ZERO, stop-start);
    double x, y;
    Color planet = prefer.colorPlanet();
    Color bckgnd = prefer.colorBackGnd();
    g.setColor(new Color((int)((planet.getRed()   * (100 + adjust) +
                                bckgnd.getRed()   * (100 - adjust)) / 200.0),
                         (int)((planet.getGreen() * (100 + adjust) +
                                bckgnd.getGreen() * (100 - adjust)) / 200.0),
                         (int)((planet.getBlue()  * (100 + adjust) +
                                bckgnd.getBlue()  * (100 - adjust)) / 200.0)));
    for ( int n = start; n <= stop; n++ ) {
      x = orbit.x[(orbit.near + n) % 180];
      y = orbit.y[(orbit.near + n) % 180];
      if ( n == start ) gp.moveTo((float)x, (float)y);
      else              gp.lineTo((float)x, (float)y);
    }
    g.draw(gp);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Implements MouseWheelListener interface.
   *
   * @param e MouseWheelEvent
   */
  public void mouseWheelMoved(MouseWheelEvent e) {
    int notches = e.getWheelRotation();
    Component comp = e.getComponent();

    if ( comp == (Component)rbar ) {
      int v = rbar.getValue();
      if ( notches > 0 ) {
        v -= SCROLLINC;
        if ( v < 0 ) v += RSCRLMAX;
      } else {
        v += SCROLLINC;
        if ( v > RSCRLMAX ) v -= RSCRLMAX;
      }
      rbar.setValue(v);
    }
    else if ( comp == (Component)tbar ) {
      int v = tbar.getValue();
      if ( notches < 0 ) {
        v -= SCROLLINC;
        if ( v < 0 ) v += TSCRLMAX;
      } else {
        v += SCROLLINC;
        if ( v > TSCRLMAX ) v -= TSCRLMAX;
      }
      tbar.setValue(v);
    }
    else {
      int v = zbar.getValue();
      if ( notches < 0 ) {
        if ( v > 0 ) zbar.setValue(Math.max(0, v - SCROLLINC));
      } else {
        if ( v < ZSCRLMAX ) zbar.setValue(Math.min(ZSCRLMAX, v + SCROLLINC));
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

    if      ( cmd.equals("inner") ) {
      showinner = true;
      showouter = false;
      inner.setEnabled(false);
      outer.setEnabled(true);
      all.setEnabled(true);
      restartPaint();
    }
    else if ( cmd.equals("outer") ) {
      showinner = false;
      showouter = true;
      inner.setEnabled(true);
      outer.setEnabled(false);
      all.setEnabled(true);
      restartPaint();
    }
    else if ( cmd.equals("all") ) {
      showinner = true;
      showouter = true;
      inner.setEnabled(true);
      outer.setEnabled(true);
      all.setEnabled(false);
      restartPaint();
    }
    else if ( cmd.equals("scroll") ) {
      showscroll = !showscroll;
      scroll.setText(showscroll ? TextBndl.getString("M.Opt.ScrollOff")
                                : TextBndl.getString("M.Opt.ScrollOn"));
      tbar.setVisible(showscroll);
      rbar.setVisible(showscroll);
      zbar.setVisible(showscroll);
      repaint();
    }
    else if ( cmd.equals("time") ) {
      showtime = !showtime;
      time.setText(showtime ? TextBndl.getString("M.Opt.TimeCtrlOff")
                            : TextBndl.getString("M.Opt.TimeCtrlOn"));
      space1.setVisible(showtime);
      space2.setVisible(showtime);
      space3.setVisible(showtime);
      space4.setVisible(showtime);
      separator.setVisible(showtime);
      timeadd.setVisible(showtime);
      timestep.setVisible(showtime);
      timesub.setVisible(showtime);
      timecptr.setVisible(showtime);
      localtime.setVisible(showtime);
      repaint();
    }
    else if ( cmd.equals("timeadd") || cmd.equals("timesub") ) {
      int field;
      switch ( timestep.getSelectedIndex() ) {
        case 4:  field = Calendar.YEAR;
          break;
        case 3:  field = Calendar.MONTH;
          break;
        case 2:  field = Calendar.WEEK_OF_YEAR;
          break;
        case 1:  field = Calendar.DAY_OF_YEAR;
          break;
        default: field = Calendar.HOUR_OF_DAY;
          break;
      }
      prefer.lst.addTime(field, cmd.equals("timeadd") ? true : false);
      updateTime();
      if ( starwin != null ) ((StarWin)starwin).restartpaint();
    }
    else if ( cmd.equals("timecptr") ) {
      prefer.lst.setToCompDateTime();
      updateTime();
      if ( starwin != null ) ((StarWin)starwin).restartpaint();
    }
    else if ( cmd.equals("save") ) {
      if ( prefer.saveSSWindow(new Rectangle(getX(), getY(),
                                             getWidth(), getHeight())) ) {
        OptionDlg.showMessageDialog(this, TextBndl.getString("SaveDlg.Win"),
                                    TextBndl.getString("SaveDlg.Title"),
                                    JOptionPane.INFORMATION_MESSAGE);
        repaint();                  // Clean up
      }
    }
    else if ( cmd.equals("help_gen") ) {
      HelpWin.showHelpPage();
    }
    else if ( cmd.equals("help_ss") ) {
      HelpWin.showHelpPage("ss");
    }
    else if ( cmd.equals("close") ) {
      if ( Nvj.parentFrame != null ) { // If created from main NV window
        sswin.setVisible(false);
        Nvj.parentFrame.repaint();
      }
      else System.exit(0);             // Else created from main below
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Generates Keplerian orbit elements.
   *
   * @param planet Planet (0 = Mercury, 1 = Venus, ...)
   * @param t Julian millennia from J2000.0
   * @param orbit OrbitData class for returning Keplerian orbit elements
   */
  private void genKepOrbElements(int planet, double t, OrbitData orbit) {
    double[] l = new double[1];
    double[] b = new double[1];
    double[] r = new double[1];

    /* No need for recalc if not much time has passed */
    if ( (Math.abs(t - orbit.t0)) < (T2[planet] / 65745000) ) return;
    orbit.t0 = t;
    //stem.out.println(planet); // Debugging aid

    // Get heliocentric ecliptical coordinates of present position
    planets.getHCCoordinates(planet, t, l, b, r);
    Vector3 pos1 = new Vector3(r[0] * Math.cos(l[0]) * Math.cos(b[0]),
                               r[0] * Math.sin(l[0]) * Math.cos(b[0]),
                               r[0] * Math.sin(b[0]));

    // Get heliocentric ecliptical coordinates at a future position of
    // approximately 1/20 orbit, to be used only to derive velocity at
    // the above present position.  Note:  Present and future coordinates
    // are based on their respective epoch and vernal equinox reference.
    // Due to precession the equinox regresses about 1 degree every 72
    // years, causing future coordinates to have inflated longitude
    // values (relative to present), which must be adjusted for...
    planets.getHCCoordinates(planet, t + T2[planet]/365250, l, b, r);
    l[0] -= T2[planet] * PAF;  // Quick and dirty adjustment for
            // equinox precession (mainly for outer planets)
    Vector3 pos2 = new Vector3(r[0] * Math.cos(l[0]) * Math.cos(b[0]),
                               r[0] * Math.sin(l[0]) * Math.cos(b[0]),
                               r[0] * Math.sin(b[0]));

    // Use the Lambert-Gauss algorithm to derive velocity
    Vector3 vel1 = lambertGauss(pos1, pos2, T2[planet]/TU);

    // Convert position and velocity to Keplerian orbit elements
    cartToKep(pos1, vel1, orbit);

    // Uncomment for debugging
    //stem.out.println("Planet = " + orbit.name);
    //stem.out.println("T2 adjustment = " + (T2[planet] * PAF / D2R));
    //stem.out.println("dt = " + T2[planet] + " days, angle = " +
    //                 pos1.angleindegrees(pos2) + " degrees");
    //stem.out.println("p = " + orbit.p +
    //               ", e = " + orbit.e +
    //               ", i = " + orbit.i +
    //               ", Omega = " + (orbit.Omega / D2R) +
    //               ", omega = " + (orbit.omega / D2R) +
    //               ", nu = " + (orbit.nu / D2R));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Calculates initial velocity from an initial position, a second position,
   * and the time difference between them.  The "short way" is assumed.
   * Uses "Lambert-Gauss's Solution" algorithm from "Fundamentals of
   * Astrodynamics and Applications" by Vallado (2nd Ed., P. 460).
   *
   * @param pos1 Initial position (AU)
   * @param pos2 Second position (AU)
   * @param dt Time difference between positions (TU)
   */
  static public Vector3 lambertGauss(Vector3 pos1, Vector3 pos2, double dt) {
    double mag1 = pos1.magnitude();
    double mag2 = pos2.magnitude();
    double mag12 = mag1 * mag2;
    double sqrtmag12 = Math.sqrt(mag12);

    // delta nu - the angle between the two positions
    double dnu = Math.acos(pos1.dotproduct(pos2) / mag12);
    double cosdnu = Math.cos(dnu);
    double coshalfdnu = Math.cos(dnu / 2);

    double l = (mag1 + mag2) / (4 * sqrtmag12 * coshalfdnu) - 0.5;
    double m = mu * dt * dt / Math.pow((2 * sqrtmag12 * coshalfdnu), 3);

    double x1, x2, y = 1, yold;
    int n = 0;
    do {
      yold = y;
      x1 = m / (y * y) - l;
      // Shorter series:
      // x2 = 4 * (1 + 6*x1 * (1 + 8*x1 * (1 + 10*x1/9) / 7) /5 ) / 3;
      // Slightly longer series (not much justification):
      // x2 = 4 * (1 + 6*x1 * (1 + 8*x1 * (1 + 10*x1 * (1 + 12*x1/11)
      //        / 9) / 7) /5 ) / 3;
      x2 = 4 * (1 + 6*x1 * (1 + 8*x1 * (1 + 10*x1 * (1 + 12*x1 * (1 + 14*x1/13)
             / 11) / 9) / 7) /5 ) / 3;
      y  = 1 + x2 * (l + x1);
    } while ( Math.abs((y - yold) / y) > 1E-14 && ++n < 100 );
    //stem.out.println("n = " + n + ", x1 = " + x1);

    double coshalfdE = 1 - 2 * x1;
    double p = mag12 * (1 - cosdnu) /
               (mag1 + mag2 - 2 * sqrtmag12 * coshalfdnu * coshalfdE);

    double f = 1 - mag2 * (1 - cosdnu) / p;
    double g = mag12 * Math.sin(dnu) / Math.sqrt(mu * p);

    return pos2.sub(pos1.multiply(f)).multiply(1/g);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Converts Cartesian position and velocity to Keplerian values.
   * Uses "ELORB" algorithm from "Fundamentals of Astrodynamics and
   * Applications" by Vallado (2nd Ed., P. 120).
   *
   * @param pos Position (AU)
   * @param vel Velocity (AU/TU)
   * @param orbit OrbitData class for returning Keplerian orbit elements
   */
  static public void cartToKep(Vector3 pos, Vector3 vel, OrbitData orbit) {

    double posmag = pos.magnitude();
    double velmag = vel.magnitude();

    Vector3 h = pos.crossproduct(vel); // Angular momentum
    double hmag = h.magnitude();

    Vector3 n = zaxis.crossproduct(h); // Node vector
    double nmag = n.magnitude();

    /* Eccentricity */
    Vector3 ecc = pos.multiply(velmag * velmag - mu / posmag).sub(
                 vel.multiply(pos.dotproduct(vel))).multiply(1 / mu);
    orbit.e = ecc.magnitude();

    /* Semiparameter (calculation assumes orbit.e != 0) */
    double xi = velmag * velmag / 2 - mu / posmag;
    orbit.p = mu * (orbit.e * orbit.e - 1) / (2 * xi);

    /* Inclination */
    orbit.i = Math.acos(h.z / hmag);

    /* Right Ascension of ascending node */
    orbit.Omega = Math.acos(n.x / nmag);
    if ( n.y < 0 ) orbit.Omega = TwoPI - orbit.Omega;

    /* Argument of periapsis */
    orbit.omega = Math.acos(n.dotproduct(ecc) / (nmag * orbit.e));
    if ( ecc.z < 0 ) orbit.omega = TwoPI - orbit.omega;

    /* True anomaly */
    orbit.nu = Math.acos(ecc.dotproduct(pos) / (orbit.e * posmag));
    if ( pos.dotproduct(vel) < 0 ) orbit.nu = TwoPI - orbit.nu;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * main
   */
  static public void main(String[] args) {
    SSWin.showWindow(null, new Preferences());
  }
}
