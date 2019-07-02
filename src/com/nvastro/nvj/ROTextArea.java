/*
 * ROTextArea.java  -  Override of NFScrollPane to create read only JTextArea
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

import java.awt.Dimension;
import java.awt.Event;
import java.awt.FontMetrics;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Override of JTextArea with special key handling.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
class JTA extends JTextArea {
  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public JTA (String text, int rows, int columns) {
    super(text, rows, columns);

    setEditable(false);

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
    // The following allows Enter to traverse up hierarchy
    // (as long as processKeyEvent doesn't mess with Enter)
    // (processKeyEvent sees keys before InputMap does, and if
    // super.processKeyEvent not called, InputMap never sees the key)
    // Note:  In Java 1.3, doing nothing in processKeyEvent would
    // cause key to traverse up hierarchy.  In 1.4 it doesn't.
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");

    /* Set up tab focus traversal */
    try {
      Set<KeyStroke> forward = new HashSet<KeyStroke>();
      Set<KeyStroke> reverse = new HashSet<KeyStroke>();
      forward.add(KeyStroke.getKeyStroke("TAB"));
      forward.add(KeyStroke.getKeyStroke("ctrl TAB"));
      reverse.add(KeyStroke.getKeyStroke("shift TAB"));
      reverse.add(KeyStroke.getKeyStroke("ctrl shift TAB"));
      setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                            forward);
      setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                            reverse);
    }
    catch ( Exception e ) { }
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Do not: Override to keep text area out of focus loop
   * Originally overridden, but then could not do page up/down or line up/down
   * via keyboard (only mouse).  Thus need to keep in focus loop.
   */
  /* public boolean isFocusTraversable() { return false; } */
}

/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Override of NFScrollPane to create read only JTextArea.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class ROTextArea extends NFScrollPane /* JScrollPane */ {
  private JTextArea ta;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param text Text to be displayed
   * @param rows Number of rows for text
   * @param columns Number of columns for text
   */
  public ROTextArea(String text, int rows, int columns) {
    ta = new JTA("", rows, columns);
    setViewportView(ta);
    FontMetrics fm = ta.getFontMetrics(ta.getFont());
    Dimension dim = ta.getPreferredSize();
    dim.height += fm.getDescent();
    setPreferredSize(dim);
    ta.setRows(0);
    ta.setColumns(0);
    ta.setText(text);
    addKeyBindings();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Appends the given text to the end of the document.
   *
   * @param str Text that is appended
   */
  public void append(String str) {
    ta.append(str);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Replaces existing text with the given text.
   *
   * @param str Text that will replace existing text
   */
  public void setText(String str) {
    ta.setText(str);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets text.
   */
  public String getText() {
    return ta.getText();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets the position of the text insertion caret (which is not visible,
   * but makes the associated text visible, scrolling if necessary).
   *
   * @param position Caret position
   */
  public void setCaretPosition(int position) {
    ta.setCaretPosition(position);
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Add key bindings.
   */
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


/*------------------------------------------------------------------------------

Default InputMap bindings for a JTextArea
-----------------------------------------

- - - WHEN_FOCUSED - - -
None
- - - Parent of WHEN_FOCUSED - - -
keyCode Down-P           =>  javax.swing.text....
keyCode Page Down-P      =>  javax.swing.text....
keyCode Page Up-P        =>  javax.swing.text....
keyCode Up-P             =>  javax.swing.text....
keyCode Enter-P          =>  javax.swing.text....
keyCode Tab-P            =>  javax.swing.text....
- - - Parent of Parent of WHEN_FOCUSED - - -
None
- - - Parent of Parent of Parent of WHEN_FOCUSED - - -
keyCode ShiftEnd-P       =>  selection-end-line
keyCode Ctrl+ShiftEnd-P  =>  selection-end
keyCode ShiftHome-P      =>  selection-begin-line
keyCode ShiftLeft-P      =>  selection-backward
keyCode Ctrl+ShiftHome-P =>  selection-begin
keyCode ShiftUp-P        =>  selection-up
keyCode ShiftRight-P     =>  selection-forward
keyCode Ctrl+ShiftLeft-P =>  selection-previous-word
keyChar ^H-P             =>  delete-previous         (Just ^H in Java 1.4)
keyCode Tab-P            =>  insert-tab
keyCode Up-P             =>  caret-up
keyCode ShiftDown-P      =>  selection-down
keyCode Enter-P          =>  insert-break
keyCode Down-P           =>  caret-down
keyCode Left-P           =>  caret-backward
keyCode Right-P          =>  caret-forward
keyCode Ctrl+ShiftRight-P =>  selection-next-word
keyCode ShiftUp-P        =>  selection-up
keyCode ShiftDown-P      =>  selection-down
keyCode ShiftLeft-P      =>  selection-backward
keyCode ShiftRight-P     =>  selection-forward
keyCode CtrlSpace-P      =>  activate-link-action
keyCode Page Up-P        =>  page-up
keyCode Ctrl+ShiftO-P    =>  toggle-componentOrientation
keyCode Page Down-P      =>  page-down
keyCode End-P            =>  caret-end-line
keyCode CtrlEnd-P        =>  caret-end
keyCode Home-P           =>  caret-begin-line
keyCode CtrlLeft-P       =>  caret-previous-word
keyCode Left-P           =>  caret-backward
keyCode Up-P             =>  caret-up
keyCode CtrlHome-P       =>  caret-begin
keyCode Right-P          =>  caret-forward
keyCode CtrlRight-P      =>  caret-next-word
keyCode CtrlT-P          =>  next-link-action
keyCode Down-P           =>  caret-down
keyCode CtrlLeft-P       =>  caret-previous-word
keyCode CtrlV-P          =>  paste-from-clipboard
keyCode CtrlRight-P      =>  caret-next-word
keyCode Ctrl+ShiftLeft-P =>  selection-previous-word
keyCode CtrlX-P          =>  cut-to-clipboard
keyCode Ctrl+ShiftRight-P =>  selection-next-word
keyCode Ctrl+ShiftT-P    =>  previous-link-action
keyCode CtrlA-P          =>  select-all
keyCode Delete-P         =>  delete-next
keyCode CtrlC-P          =>  copy-to-clipboard
keyCode Copy-P           =>  copy-to-clipboard
keyCode Ctrl\-P          =>  unselect
keyCode Ctrl+ShiftPage Up-P =>  selection-page-left
keyCode Paste-P          =>  paste-from-clipboard
keyCode Cut-P            =>  cut-to-clipboard
keyCode ShiftPage Up-P   =>  selection-page-up
keyCode Ctrl+ShiftPage Down-P =>  selection-page-right
keyCode ShiftPage Down-P =>  selection-page-down

- - - WHEN_ANCESTOR_OF_FOCUSED_COMPONENT - - -
None

- - - WHEN_IN_FOCUSED_WINDOW - - -
None

Note:  The JTextArea ActionMap contains more actions than
are listed above, such as beep, select-paragraph, ...
(The values of the InputMap are keys of the ActionMap)


Default InputMap bindings for a JScrollPane
-------------------------------------------

- - - WHEN_FOCUSED - - -
None

- - - WHEN_ANCESTOR_OF_FOCUSED_COMPONENT - - -
None
- - - Parent of WHEN_ANCESTOR_OF_FOCUSED_COMPONENT - - -
keyCode Page Up-P        =>  scrollUp
keyCode Left-P           =>  unitScrollLeft
keyCode CtrlPage Down-P  =>  scrollRight
keyCode Page Down-P      =>  scrollDown
keyCode Right-P          =>  unitScrollRight
keyCode Left-P           =>  unitScrollLeft
keyCode CtrlEnd-P        =>  scrollEnd
keyCode Up-P             =>  unitScrollUp
keyCode Right-P          =>  unitScrollRight
keyCode Down-P           =>  unitScrollDown
keyCode CtrlHome-P       =>  scrollHome
keyCode CtrlPage Up-P    =>  scrollLeft
keyCode Up-P             =>  unitScrollUp
keyCode Down-P           =>  unitScrollDown

- - - WHEN_IN_FOCUSED_WINDOW - - -
None


Java 1.3 ~identical to 1.4 except the single case noted.

Some useful info may be found in "Keyboard Bindings in Swing"
http://java.sun.com/products/jfc/tsc/special_report/kestrel/keybindings.html
(from link in JComponent page)

------------------------------------------------------------------------------*/

