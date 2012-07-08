package org.crsh.text;

import org.crsh.shell.ui.Decoration;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public abstract class Style implements Serializable {

  public static final Style RESET = new Style(null, null, null) {
    @Override
    public void writeAnsiTo(Appendable appendable) throws IOException {
      appendable.append("\033[0m");
    }
  };

  public static Style create(Decoration decoration, Color foreground, Color background) {
    return new Style(decoration, foreground, background) {
      @Override
      public void writeAnsiTo(Appendable appendable) throws IOException {
        if (decoration != null|| foreground != null || background != null) {
          appendable.append("\033[");
          boolean appended = false;
          if (decoration != null) {
            appendable.append(Integer.toString(decoration.code));
            appended = true;
          }
          if (foreground != null) {
            if (appended) {
              appendable.append(";");
            }
            appendable.append(Integer.toString(foreground.code(30)));
            appended = true;
          }
          if (background != null) {
            if (appended) {
              appendable.append(";");
            }
            appendable.append(Integer.toString(background.code(40)));
          }
          appendable.append("m");
        }
        else {
          //
        }
      }
    };
  }

  /** . */
  protected final Decoration decoration;

  /** . */
  protected final Color foreground;

  /** . */
  protected final Color background;

  private Style(Decoration decoration, Color foreground, Color background) {

    this.decoration = decoration;
    this.foreground = foreground;
    this.background = background;

  }

  public Decoration getDecoration() {
    return decoration;
  }

  public Color getForeground() {
    return foreground;
  }

  public Color getBackground() {
    return background;
  }

  public CharSequence asAnsiSequence() {
    StringBuilder sb = new StringBuilder();
    try {
      writeAnsiTo(sb);
    }
    catch (IOException e) {
      // Should not happen
      throw new AssertionError(e);
    }
    return sb.toString();
  }

  public abstract void writeAnsiTo(Appendable appendable) throws IOException;

}
