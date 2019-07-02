/*
 * LoadDSDlg.java  -  "Load Deep Sky" dialog
 * Copyright (C) 2011-2014 Brian Simpson
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

import java.awt.*;
import javax.swing.*;
import java.io.File;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * "Load Deep Sky" dialog.
 *
 * @author Brian Simpson
 */
public class LoadDSDlg {
  static private JFileChooser fc = null;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Shows the dialog.
   *
   * @param owner Parent window
   */
  public static File showDlg(Component owner) {
    String path;
    int ret;

    if ( fc == null ) {
      if ( Nvj.workingDir == null || !Nvj.workingDir.equals("") ) path = ".";
      else path = Nvj.workingDir;

      fc = new JFileChooser(path);
      fc.setDialogTitle("Select a Deep sky object file");
    }

    ret = fc.showOpenDialog(owner);

    if ( ret == JFileChooser.APPROVE_OPTION ) {
      return fc.getSelectedFile();
    }
    return null;
  }
}

