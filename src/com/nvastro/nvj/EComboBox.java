/*
 * EComboBox.java  -  Override of JComboBox to handle Escape key
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

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Override of JComboBox that provides special handling of the Escape key.
 *
 * @author Brian Simpson
 */
@SuppressWarnings("serial")
public class EComboBox extends JComboBox<String> {

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   */
  public EComboBox() {
    super();
    init();
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor.
   *
   * @param items Items for ComboBox
   */
  public EComboBox(String[] items) {
    super(items);
    init();
  }

  /* <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Initialization.
   */
  private void init() {
    getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                     .put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
    Action escape = new AbstractAction("escape") {
      public void actionPerformed(ActionEvent e) {
        @SuppressWarnings("unchecked")
        JComboBox<String> cb = (JComboBox<String>)e.getSource();
        if ( cb.isPopupVisible() ) cb.hidePopup();
        else {
          // The following code sucketh, but couldn't get processKeyEvent
          // and KeyListener to work.  In fact, they never saw any KeyEvents
          // unless I posted a new KeyEvent in the system queue here.
          // But even then I couldn't get it to propagate upward...
          Component o = cb;
          while ( (o = o.getParent()) != null ) {
            if ( o instanceof EscapeDlg ) { ((EscapeDlg)o).close(); break; }
          }
        }
      }
    };
    getActionMap().put("escape", escape);
  }
}

