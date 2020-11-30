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

@SuppressWarnings("serial")
public class PausePopupDialog extends JDialog {
    private static final String imgPath = "fitnesse_dbg.png";
    private static final String title = "FitNesse - Pause";
    private static final String[] buttons = new String[]{"Continue", "Stop test"};
    private int value;
    private static Point center = null;
    private final JLabel label;
    private static boolean keepOnTop = false;

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

    PausePopupDialog(Frame owner, String title, String message, String[] buttons) {
        super(owner, title, true);

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
                    PausePopupDialog.this.setVisible(false);
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
        JCheckBoxMenuItem kotMenuItem = new JCheckBoxMenuItem("keep on top");
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
        LockCreatorWaiter lcw = new LockCreatorWaiter(message, title, buttons);
        lcw.run();
        int rv = lcw.getValue();
        return rv;
    }

    private static class LockCreatorWaiter implements Runnable {
        private volatile boolean clicked;
        private volatile Object lock;
        private int value;
        private final String message;
        private final String title;
        private final String[] buttons;

        LockCreatorWaiter(String message, String title, String[] buttons) {
            clicked = false;
            lock = new Object();
            value = -1;
            this.message = message;
            this.title = title;
            this.buttons = buttons;
        }

        public void run() {
            SwingUtilities.invokeLater(new GUIRunnable());
            while (!clicked) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }

        public synchronized int getValue() {
            return value;
        }

        private synchronized void setValue(int value) {
            this.value = value;
        }

        private class GUIRunnable implements Runnable {
            GUIRunnable() {
            }

            public void run() {
                JFrame frame = new JFrame();
                PausePopupDialog ppd = new PausePopupDialog(frame, title, message, buttons);
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
                clicked = true;
                frame.dispose();
                synchronized (lock) {
                    lock.notify();
                }
                setValue(ppd.getValue());
            }
        }
    }
}
