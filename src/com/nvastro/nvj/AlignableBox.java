/*
 * AlignableBox.java  -  Extends Box container by adding alignment
 *
 * Code acquired years ago, perhaps adopted from
 * http://bugs.sun.com/view_bug.do;jsessionid=\
 * 840aea8c06811ffffffffafb72749824db84?bug_id=4386981
 */


package com.nvastro.nvj;

import javax.swing.Box;
import javax.swing.BoxLayout;


/** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
 * Extends Box container by adding alignment.
 */
@SuppressWarnings("serial")
public class AlignableBox extends Box {
  protected float alignmentX;
  protected float alignmentY;

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Constructor
   *
   * @param axis BoxLayout.X_AXIS (to display components from left to right)
   *          or BoxLayout.Y_AXIS (to display them from top to bottom)
   */
  public AlignableBox(int axis) {
    super(axis);
    alignmentX = 0.5f;
    alignmentY = 0.5f;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override of Box method.  Creates a horizontal AlignableBox.
   * Same as createHorzAlignableBox().
   */
  public static Box createHorizontalBox() {
    return new AlignableBox(BoxLayout.X_AXIS);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Override of Box method.  Creates a vertical AlignableBox.
   * Same as createVertAlignableBox().
   */
  public static Box createVerticalBox() {
    return new AlignableBox(BoxLayout.Y_AXIS);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a horizontal AlignableBox.
   */
  public static AlignableBox createHorzAlignableBox() {
    return new AlignableBox(BoxLayout.X_AXIS);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Creates a vertical AlignableBox.
   */
  public static AlignableBox createVertAlignableBox() {
    return new AlignableBox(BoxLayout.Y_AXIS);
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets current X alignment.
   *
   * @return The value of the alignmentX property
   */
  public float getAlignmentX() { return alignmentX; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets X (horizontal) alignment.
   *
   * @param alignmentX Alignment in X direction (0 - 1 inclusive)
   */
  public void setAlignmentX(float alignmentX) {
    this.alignmentX = alignmentX;
  }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Gets current Y alignment.
   *
   * @return The value of the alignmentY property
   */
  public float getAlignmentY() { return alignmentY; }

  /** <!--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~-->
   * Sets Y (vertical) alignment.
   *
   * @param alignmentY Alignment in Y direction (0 - 1 inclusive)
   */
  public void setAlignmentY(float alignmentY) {
    this.alignmentY = alignmentY;
  }
}

