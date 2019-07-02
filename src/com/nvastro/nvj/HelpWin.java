/*
 * HelpWin.java  -  Help window
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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Custom focus traversal policy to keep HelpWin focus carefully controlled.
 * This keeps the focus away from a JTextArea that the HTMLEditorKit
 * apparently uses.
 *
 * @author Brian Simpson
 */
class FTP extends FocusTraversalPolicy {
  Component r, f, t;

  static public void setFTP(JFrame frame,
                            Component r, Component f, Component t) {
    frame.setFocusTraversalPolicy(new FTP(r, f, t));
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public FTP(Component r, Component f, Component t) {
    this.r = r;
    this.f = f;
    this.t = t;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get component after.
   */
  public Component getComponentAfter(Container focusCycleRoot, Component c) {
    if      ( c == r ) {
      if      ( f.isEnabled() ) return f;
      else if ( t.isEnabled() ) return t;
      else                      return r;
    }
    else if ( c == f ) {
      if      ( t.isEnabled() ) return t;
      else if ( r.isEnabled() ) return r;
      else                      return f;
    }
    else { /* c == t */
      if      ( r.isEnabled() ) return r;
      else if ( f.isEnabled() ) return f;
      else                      return t;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get component before.
   */
  public Component getComponentBefore(Container focusCycleRoot, Component c) {
    if      ( c == r ) {
      if      ( t.isEnabled() ) return t;
      else if ( f.isEnabled() ) return f;
      else                      return r;
    }
    else if ( c == f ) {
      if      ( r.isEnabled() ) return r;
      else if ( t.isEnabled() ) return t;
      else                      return f;
    }
    else { /* c == t */
      if      ( f.isEnabled() ) return f;
      else if ( r.isEnabled() ) return r;
      else                      return t;
    }
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get first component.
   */
  public Component getFirstComponent(Container focusCycleRoot) {
    return t;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get last component.
   */
  public Component getLastComponent(Container focusCycleRoot) {
    return f;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Get default component.
   */
  public Component getDefaultComponent(Container focusCycleRoot) {
    return t;
  }
}

/*============================================================================*/

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Read only version of JEditorPane, with focus "enhancements".
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
class HelpPane extends JEditorPane {
  static final private String nopage =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n<HTML>\n" +
    "<HEAD></HEAD>\n<BODY BGCOLOR=\"#ffffff\">\n<font size=\"+2\">\n" +
    TextBndl.getString("HelpWin.NoPage") + "\n</BODY></HTML>\n";

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param url The url (in String format) to display.  Null, 0 length,
   * and bad url's handled.
   */
  public HelpPane(String url) {
    super();

    setPage(url);

    setEditable(false);

    //Associated with making FocusTraversalPolicy approach work
    try { setFocusable(false); }
    catch ( Exception e ) { }

    /* Disable the following keys so they won't get consumed here */
    // Set to none, rather than remove, otherwise parent bindings still visible
    InputMap imap = getInputMap(JComponent.WHEN_FOCUSED);
    imap.put(KeyStroke.getKeyStroke("UP"), "none");
    imap.put(KeyStroke.getKeyStroke("DOWN"), "none");
    imap.put(KeyStroke.getKeyStroke("LEFT"), "none");
    imap.put(KeyStroke.getKeyStroke("RIGHT"), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, Event.CTRL_MASK), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END,  Event.CTRL_MASK), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.CTRL_MASK), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,Event.CTRL_MASK), "none");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override to return false so that HelpPane is not focus cycle root.
   */
  public boolean isFocusCycleRoot() { return false; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override of super.setPage(String).  If error, an error page will be set.
   *
   * @param url The url (in String format) to display.  Null, 0 length,
   * and bad url's handled.
   */
  public void setPage(String url) {
    // Note:  Experimentation has shown that sometimes several setPage's
    //        may have to be done to "recover" from a setPage with a bad url.
    boolean badurl = false;

    if ( url != null && url.trim().length() > 0 ) {
      try {
        super.setPage(url.trim());
      } catch ( Exception e ) {
        badurl = true;
      }
    }
    else badurl = true;

    if ( badurl ) {
      setContentType("text/html");
      setText(nopage);
    }
  }
}

/*============================================================================*/

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * A scrollable version of HelpPane.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
class ScrollHelpPane extends NFScrollPane {
  private HelpPane hp;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public ScrollHelpPane(String url) {
    hp = new HelpPane(url);
    setViewportView(hp);
    addKeyBindings();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the page.
   */
  public void setPage(String url) {
    hp.setPage(url);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Adds a HyperlinkListener to the HelpPane.
   */
  public void addHyperlinkListener(HyperlinkListener listener) {
    hp.addHyperlinkListener(listener);
  }

  /* The following two overrides appear to have no effect in present config. */
  //public boolean isFocusTraversable() { return false; }
  //public boolean isRequestFocusEnabled() { return false; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override of requestFocus(), transfers focus to HelpPane.
   */
  public void requestFocus() {
    hp.requestFocus();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add key bindings.
   */
  // This and next 2 functions borrowed from ROTextArea (see it for details...)
  protected void addKeyBindings() {
    InputMap source = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    InputMap target = getInputMap(WHEN_IN_FOCUSED_WINDOW);

    /* Allow standard bindings to work */
    copyKeyBinding(KeyEvent.VK_UP,        0,               source, target);
    copyKeyBinding(KeyEvent.VK_DOWN,      0,               source, target);
    copyKeyBinding(KeyEvent.VK_LEFT,      0,               source, target);
    copyKeyBinding(KeyEvent.VK_RIGHT,     0,               source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_UP,   0,               source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_DOWN, 0,               source, target);
    copyKeyBinding(KeyEvent.VK_HOME,      Event.CTRL_MASK, source, target);
    copyKeyBinding(KeyEvent.VK_END,       Event.CTRL_MASK, source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_UP,   Event.CTRL_MASK, source, target);
    copyKeyBinding(KeyEvent.VK_PAGE_DOWN, Event.CTRL_MASK, source, target);

    /* Create 2 new bindings */
    copyKeyBinding(KeyEvent.VK_PAGE_UP,   Event.CTRL_MASK, source,
                   KeyEvent.VK_LEFT,      Event.CTRL_MASK, target);
    copyKeyBinding(KeyEvent.VK_PAGE_DOWN, Event.CTRL_MASK, source,
                   KeyEvent.VK_RIGHT,     Event.CTRL_MASK, target);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Copy key binding.
   */
  protected void copyKeyBinding(int code, int mod,
                                InputMap source, InputMap target) {
    KeyStroke stroke = KeyStroke.getKeyStroke(code, mod);
    target.put(stroke, source.get(stroke));
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Copy key binding.
   */
  protected void copyKeyBinding(int code1, int mod1, InputMap source,
                                int code2, int mod2, InputMap target) {
    KeyStroke stroke1 = KeyStroke.getKeyStroke(code1, mod1);
    KeyStroke stroke2 = KeyStroke.getKeyStroke(code2, mod2);
    target.put(stroke2, source.get(stroke1));
  }
}

/*============================================================================*/

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Help window.
 *
 * @author Brian Simpson
 */
/*------------------------------------------------------------------------------
 * Conventions:
 * "url"  does not refer to a URL object, but rather the String it represents,
 *     as in:  file:com/nvastro/nvj/help/welcome.html
 * "page" refers to base name of the above "url", as in:  welcome
 *
 * Note:  Assumes toc reached only via toc button, not through
 * hyperlink or invocation.
 -----------------------------------------------------------------------------*/
@SuppressWarnings("serial")
public class HelpWin extends JFrame {
  static private HelpWin hw = null;
  private JButton forward, reverse, toc;
  static private int active = -1;   // Index of active url (-1 = none)
                                    // Doesn't increment if toc pressed
  static private int end    = -1;   // Index of last url (-1 = none)
  static private Vector<String> urls = new Vector<String>(); // Set of urls
         // in String format; No nulls should be used since
         // ((String)null).equals(...) throws an exception
         // (when I compare urls...)
  private ScrollHelpPane view = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the default page.
   */
  public static void showHelpPage() { showHelpPage(null, null); }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the specified page.
   *
   * @param page The page to display (e.g. "welcome")
   */
  public static void showHelpPage(String page) { showHelpPage(page, null);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the default page, placing the help window so as to avoid
   * the specified rectangle (presumably the main window).
   *
   * @param rectm The rectangle (in screen coordinates) to avoid (Only works
   *  if this is the first invocation)
   */
  public static void showHelpPage(Rectangle rectm) {
    showHelpPage(null, rectm);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the specified page, placing the help window so as to avoid
   * the specified rectangle (presumably the main window).
   *
   * @param page The page to display (e.g. "welcome")
   * @param rectm The rectangle (in screen coordinates) to avoid (Only works
   *  if this is the first invocation)
   */
  public static void showHelpPage(String page, Rectangle rectm) {
    if ( page == null || page.trim().equals("") )
      page = "welcome";

    String url = urlFromPage(page.trim());

    if ( hw == null )
      hw = new HelpWin(url, rectm);
    else {
      if ( active < 0 || !((String)urls.elementAt(active)).equals(url) ) {
        if ( ++active < urls.size() ) urls.set(active, url);
        else                          urls.add(url);
      }
      end = active;

      //stem.out.println("active = " + active);
      //stem.out.println("url = " + (String)urls.elementAt(active));
      hw.view.setPage(url);
      hw.toc.setEnabled(true);  // Enable before disable (focus reasons)
      hw.reverse.setEnabled((active > 0) ? true : false);
      hw.forward.setEnabled(false);
    }

    if ( hw.getState() == Frame.ICONIFIED ) hw.setState(Frame.NORMAL);
    hw.setVisible(true);
    hw.toFront();
    //hw.view.requestFocus();
    hw.toc.requestFocus();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Private constructor.
   */
  private HelpWin(String url, Rectangle rectm) {
    super(TextBndl.getString("HelpWin.Title"));

    /* Handle closing (keep dft HIDE_ON_CLOSE) */
    // setDefaultCloseOperation(HIDE_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) { // Called when closed
        Nvj.parentFrame.repaint();               //   with system menu
        // Clears up any residue if exposing any of the parent window,
        // which at one time seemed necessary with early versions of
        // Java, but is likely no longer necessary
      }

      /* Sets focus when window is first opened */
      public void windowOpened(WindowEvent e) {
        //view.requestFocus();
        toc.requestFocus();
      }
    });

    /* Set up close (hide) via Escape keystroke */
    Action close = new AbstractAction("close") {
      public void actionPerformed(ActionEvent e) {
        HelpWin.this.setVisible(false);
        Nvj.parentFrame.repaint();   // Cleans up residue
        // (Causes exception if Helpwin run standalone from main below)
      }
    };
    JRootPane rp = getRootPane();
    rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                     .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
    rp.getActionMap().put("close", close);

    /* Create the ScrollHelpPane pane */
    view = new ScrollHelpPane(url);
    // Size adjusted to just fit in 640x480
    view.setPreferredSize(new Dimension(620,410));
    view.setMinimumSize(new Dimension(10,10));

    /* Set url tracking */
    // If no urls  or  a different url is active
    if ( active < 0 || !((String)urls.elementAt(active)).equals(url) ) {
      if ( ++active < urls.size() ) urls.set(active, url);
      else                          urls.add(url);
    }
    end = active;

    /* Add an action listener to monitor button activations */
    //ActionListener listener = new ActionListener() {
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        if      ( e.getActionCommand().equals(">") ) active++;
        else if ( e.getActionCommand().equals("<") ) {
          if ( toc.isEnabled() ) active--;  // Decrement if toc not showing
        }
        else    /*e.getActionCommand().equals("^")*/ {
          String newurl = urlFromPage("toc");

          //stem.out.println("active = " + active);
          //stem.out.println("url = " + newurl);
          view.setPage(newurl);  // (toc page is effectively active+1)
          reverse.setEnabled(true);  // Enable before disable (focus reasons)
          //forward.setEnabled((active < end) ? true : false);
          toc.setEnabled(false);
          return;
        }

        //stem.out.println("active = " + active);
        //stem.out.println("url = " + (String)urls.elementAt(active));
        view.setPage((String)urls.elementAt(active));
        toc.setEnabled(true);  // Enable before disable (focus reasons)
        forward.setEnabled((active < end) ? true : false);
        reverse.setEnabled((active >   0) ? true : false);
      }
    };

    /* Add hyperlink listeners */
    HyperlinkListener hl = new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if ( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
          String newurl = e.getURL().toString();

          /* If different from url at active then increment active */
          if ( ! newurl.equals((String)urls.elementAt(active)) ) { // If diff
            if ( ++active < urls.size() ) urls.set(active, newurl);
            else                          urls.add(newurl);
          }
          /* Else do nothing (unless toc is showing) */
          else if ( toc.isEnabled() ) return; // Btn enabled, so toc not showing
          /* Else toc is showing (proceed w/o incrementing active) */
          end = active;

          //stem.out.println("active = " + active);
          //stem.out.println("url = " + newurl);
          view.setPage(newurl);
          toc.setEnabled(true);  // Enable before disable (focus reasons)
          forward.setEnabled(false);
          reverse.setEnabled((active > 0) ? true : false);
          // Note:  Must disable forward *before* setting reverse, just in
          // case it reverse has focus and gets disabled, as in:
          // 0: Help first comes up with welcome page, 1: click "^",
          // 2: Select 2nd link in toc, 3: click "<", 4: click "^",
          // 5: Select 1st link (welcome)  ->  both "<" & ">" become
          // disabled and "^" becomes enabled.  Apparently only one
          // focus switch occurs, and that happens when disabling "<".
          // If ">" has not been disabled yet, it receives focus which
          // remains there even after it is disabled!
        }
      }
    };
    view.addHyperlinkListener(hl);

    /* For debugging purposes */
    //ActionListener tmrlistener = new ActionListener() {
    //  public void actionPerformed(ActionEvent e) {
    //    Component owner = HelpWin.this.getFocusOwner();
    //    if ( owner != null )
    //      System.out.println("Focus owner = " + owner.toString());
    //    else System.out.println("Focus owner = null");
    //    if ( owner instanceof JTextArea )
    //      if ( owner.getParent() != null )
    //       System.out.println("Parent = " + owner.getParent().toString());
    //      else System.out.println("Parent = null");
    //  }
    //};
    /* Start the timer */
    //Timer timer = new Timer(2000, tmrlistener);
    //timer.setInitialDelay(2000);
    //timer.setCoalesce(true);
    //timer.start();

    /* Set the icon for this frame */
    setIconImage(Nvj.getImageIcon("/com/nvastro/nvj/hbsmall.jpg").getImage());

    /* Create a ToolBar and add buttons */
    JToolBar tb = new JToolBar(SwingConstants.HORIZONTAL);
    tb.setFloatable(false);
    removeNavigationBindings(tb);
    reverse = tb.add(action);
    reverse.setMargin(Nvj.TBBtnInsets);
    // @$#%*! Sun Java 1.4 for Linux intermittently screws up png images on btn!
    // @$#%*! HP-UX Java 1.4 screws up also!
    //reverse.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/reverse.png"));
    reverse.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/reverse.gif"));
    reverse.setActionCommand("<");
    reverse.setToolTipText(TextBndl.getString("HelpWin.Previous"));
    forward = tb.add(action);
    forward.setMargin(Nvj.TBBtnInsets);
    //forward.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/forward.png"));
    forward.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/forward.gif"));
    forward.setActionCommand(">");
    forward.setToolTipText(TextBndl.getString("HelpWin.Next"));
    tb.addSeparator(new Dimension(6, 6));
    toc = tb.add(action);
    toc.setMargin(Nvj.TBBtnInsets);
    //toc.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/up.png"));
    toc.setIcon(Nvj.getImageIcon("/com/nvastro/nvj/up.gif"));
    toc.setActionCommand("^");
    toc.setToolTipText(TextBndl.getString("HelpWin.TOC"));
    toc.setEnabled(true);  // Enable before disable (focus reasons)
    forward.setEnabled(false);
    reverse.setEnabled((active > 0) ? true : false);
    // See notes in HyperlinkListener section for proper order of above
    // statements, though here it is probably moot.

    /* Add everything to window */
    getContentPane().add(view);
    getContentPane().add(tb, BorderLayout.NORTH);

    /* Use custom focus handling (Do after buttons defined) */
    // Must actively try to keep focus away from view
    setFocusTraversalPolicy(new FTP(reverse, forward, toc));

    /* Finally, set the dialog to its preferred size */
    pack();
    // The following code was aimed at making sure that the help window
    // does not hide the main window when NV is invoked the very 1st time
    if ( rectm != null && Nvj.dimScrn != null ) {
      // rectm = rectangle of main window, Nvj.dimScrn = screen dimension
      int x, y; // x,y of help window
      // Determine initial x,y based on position of main window within screen
      int xm = (int)(rectm.getX() + rectm.getWidth() / 2.0);  // Middle of main
      int ym = (int)(rectm.getY() + rectm.getHeight() / 2.0); // Middle of main
      if ( xm < Nvj.dimScrn.width / 2 )
           x = (int)(rectm.getX() + rectm.getWidth());
      else x = (int)rectm.getX() - getWidth();
      if ( ym < Nvj.dimScrn.height / 2 )
           y = (int)(rectm.getY() + rectm.getHeight());
      else y = (int)rectm.getY() - getHeight();
      // Adjust x,y to fit within screen
      x = Math.max(0, Math.min(x, Nvj.dimScrn.width - getWidth()));
      y = Math.max(0, Math.min(y, Nvj.dimScrn.height - getHeight()));
      // Now set the location
      setLocation(x, y);
    }
    setVisible(true);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Url from page.
   */
  private static String urlFromPage(String page) {
    if ( page == null ) return "";
    URL u = Nvj.class.getResource("/com/nvastro/nvj/help/" +
                                  page.trim() + ".html");
    if ( u == null ) return "";
    else             return u.toString();
  }

  /* --- */
  //private static String pageFromURL(String url) {
  //  int start = url.lastIndexOf('/');
  //  int end   = url.lastIndexOf('.');
  //  if ( start > 0 && end > 0 && end > start && url.endsWith(".html") )
  //       return url.substring(start + 1, end);
  //  else return null;
  //}

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Remove navigation bindings.
   */
  protected void removeNavigationBindings(JComponent comp) {
    InputMap map =
      comp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    removeKeyBinding("LEFT", map);
    removeKeyBinding("RIGHT", map);
    removeKeyBinding("UP", map);
    removeKeyBinding("DOWN", map);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Remove key binding.
   */
  protected void removeKeyBinding(String key, InputMap map) {
    map.put(KeyStroke.getKeyStroke(key), "none");
  }

  /* For testing */
  //public static void main(String[] args) {
  //  String line = null;
  //  if ( args.length != 0 ) showHelpPage(args[0]);
  //  else                    showHelpPage();
  //  System.out.println("Program may be exited by pressing Ctrl-c");
  //  BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
  //  while ( true ) {
  //    System.out.print("Enter a page (or quit (or nothing) to exit):  ");
  //    try {
  //      line = in.readLine();
  //    } catch ( IOException e ) {
  //      System.err.println("Read failure");
  //      System.exit(1);
  //    }
  //    if ( line == null || line.trim().equals("") || line.equals("quit") )
  //      break;
  //    showHelpPage(line.trim());
  //  }
  //  System.exit(0);
  //}
}

/*------------------------------------------------------------------------------

Pros and Cons of this approach vs JavaHelp
(http://java.sun.com/products/javahelp/index.html)

JavaHelp problems
-----------------

F1 doesn't work until some manipulation has occurred (such as bringing up
a menu).

Help window goes away when a dialog is dismissed if help was pressed from
the dialog.  (Doesn't matter if help was already up when dialog brought up.)

Requires its own separate jar file.

Keyboard navigation is buggy.  Can generate runtime exceptions!

Can't set system icon.

From main window bring up help window - help window responds to mouse.
From main, bring up modal dlg - help window doesn't respond to mouse.
On dlg, press help button - help window is rebuilt as child of dlg,
and it now responds to mouse.  Cancel dlg - help window cancels also.

Scrolling up can garble text.

Font stinks on Linux.  (Can this be changed?)

Homegrown approach issues
-------------------------

JavaHelp has much more functionality.

Tried an approach using HTML frames where toc was on left side of
window.  It worked partially.  Can't remember what problems were.
(JEditorPane documentation shows how to deal with *frame* events)

Font stinks on Linux.  (Can this be changed?)

------------------------------------------------------------------------------*/

