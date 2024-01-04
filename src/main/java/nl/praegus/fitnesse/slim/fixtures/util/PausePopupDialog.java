package nl.praegus.fitnesse.slim.fixtures.util;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.prefs.Preferences;


@SuppressWarnings("serial")
public class PausePopupDialog extends JDialog {
  private static final String imgPath = "fitnesse_dbg.png";
  private static final String title = "FitNesse - Pause";
  private static final String[] buttons = new String[]{"Continue", "Stop test"};
  private int value;
  private static Point center = null;
  private final JLabel label;
  private static boolean keepOnTop;
  private volatile boolean clicked = false;
  private final Object lock = new Object();

  static {
    keepOnTop = getKeepOnTopDefault();
  }

  private synchronized void setValue(int value) {
    this.value = value;
  }

  public synchronized int getValue() {
    return value;
  }

  public static synchronized void setCenter(int x, int y) {
    if (PausePopupDialog.center == null) {
      PausePopupDialog.center = new Point(x, y);
    } else {
      PausePopupDialog.center.setLocation(x, y);
    }
  }

  public static synchronized Point getCenter() {
    return center;
  }

  private static Preferences getPreferences() {
    Preferences prefs = Preferences.userRoot().node(PausePopupDialog.class.getName());
    return prefs;
  }

  public static void setKeepOnTop(String value) {
    PausePopupDialog.keepOnTop = Boolean.parseBoolean(value);
  }

  public static boolean getKeepOnTop() {
    return PausePopupDialog.keepOnTop;
  }

  public static void setKeepOnTopDefault(String value) {
    getPreferences().put("keepOnTop", value);    // store the string value
  }

  public static boolean getKeepOnTopDefault() {
    String s = getPreferences().get("keepOnTop", "true");
    return Boolean.parseBoolean(s);
  }

  PausePopupDialog(Frame owner, String title, String message, String[] buttons) {
    super(owner, title, false);

    setMinimumSize(new Dimension(20, 20));
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    ImageIcon imageIcon = null;
    java.net.URL imgURL = getClass().getClassLoader().getResource(imgPath);
    if (imgURL != null) {
      imageIcon = new ImageIcon(imgURL);
      this.getOwner().setIconImage(imageIcon.getImage());
    } else {
      System.err.println("Couldn't find icon resource: " + imgPath);
    }

    setAlwaysOnTop(true);
    addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        if (!keepOnTop) {
          setAlwaysOnTop(false);
        }
      }
    });

    addComponentListener(new ComponentAdapter() {
      public void componentMoved(ComponentEvent e) {
        Component comp = e.getComponent();
        setCenter(comp.getX() + comp.getWidth() / 2, comp.getY() + comp.getHeight() / 2);
      }
    });

    Container cp = getContentPane();

    // CENTER:
    label = new JLabel(message, SwingConstants.CENTER);
    label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    cp.add(label, BorderLayout.CENTER);
    label.addMouseListener(new MyPopupListener(createLabelMenu()));

    // BOTTOM:
    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.LINE_AXIS));
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

    // glue:
    bottomPanel.add(Box.createHorizontalGlue());
    for (int i = 0; i < buttons.length; ++i) {
      final int _i = i;
      AbstractAction action = new AbstractAction(buttons[i]) {
        public void actionPerformed(ActionEvent e) {
          setValue(_i);
          clicked = true;
          synchronized (lock) {
            lock.notify();
          }
          PausePopupDialog.this.dispose();
        }
      };
      JButton button = new JButton(action);
      bottomPanel.add(button);
      if (i < buttons.length - 1) {
        // rigid area:
        bottomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
      }
    }
    // glue:
    bottomPanel.add(Box.createHorizontalGlue());
    //
    cp.add(bottomPanel, BorderLayout.SOUTH);
  }

  JPopupMenu createLabelMenu() {
    JPopupMenu popupMenu = new JPopupMenu();
    //
    JMenuItem ctcMenuItem = new JMenuItem("Copy to clipboard");
    ctcMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        StringSelection stringSelection = new StringSelection(label.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
      }
    });
    popupMenu.add(ctcMenuItem);
    //
    JCheckBoxMenuItem kotMenuItem = new JCheckBoxMenuItem("Always on top");
    kotMenuItem.setSelected(keepOnTop);
    kotMenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        keepOnTop = kotMenuItem.isSelected();
        setAlwaysOnTop(keepOnTop);
      }
    });
    popupMenu.add(kotMenuItem);
    return popupMenu;
  }

  private static class MyPopupListener extends MouseAdapter {
    private final JPopupMenu popupMenu;

    public MyPopupListener(JPopupMenu popupMenu) {
      this.popupMenu = popupMenu;
    }

    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
      if (e.isPopupTrigger()) {
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  public static int pause(String message) {
    int rv = pause(message, PausePopupDialog.title);
    return rv;
  }

  public static int pause(String message, String title) {
    int rv = pause(message, title, PausePopupDialog.buttons);
    return rv;
  }

  public static int pause(String message, String title, String[] buttons) {
    GUIRunnable guiRunnable = new GUIRunnable(message, title, buttons);
    try {
      SwingUtilities.invokeAndWait(guiRunnable);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    PausePopupDialog ppd = guiRunnable.ppd;
    ppd.clicked = false;
    while (!ppd.clicked) {
      synchronized (ppd.lock) {
        try {
          ppd.lock.wait();
        } catch (InterruptedException ie) {
          break;
        }
      }
    }
    return ppd.getValue();
  }

  private static class GUIRunnable implements Runnable {
    private String message;
    private String title;
    private String[] buttons;
    public volatile PausePopupDialog ppd;
    GUIRunnable(String message, String title, String[] buttons) {
      this.message = message;
      this.title = title;
      this.buttons = buttons;
      this.ppd = null;
    }

    public void run() {
      JFrame frame = new JFrame();
      ppd = new PausePopupDialog(frame, title, message, buttons);
      ppd.pack();
      Point center = getCenter();
      if (center == null) {
        ppd.setLocationRelativeTo(null);
      } else {
        Dimension dim = ppd.getSize();
        int x = center.x - dim.width / 2;
        int y = center.y - dim.height / 2;
        ppd.setLocation(x, y);
      }
      ppd.setVisible(true);
    }
  }
}
