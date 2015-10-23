package edu.dsu.spi;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Scanner;

public class HelpWindow extends JFrame {
    public static final String HELP_FILE_NAME = "readme.html";

    private JEditorPane epHelp;
    private boolean helpFileLoaded = false;

    public HelpWindow() {
        super("Suave Pseudo-Interpreter Help");
        initialize();
        try {
            readHelpToPane();
        } catch (Exception e) {
            // do nothing at first
        }
        hideWindow();
    }

    private void initialize() {
        this.setMinimumSize(new Dimension(400, 300));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("pipe_icon.png")));

        epHelp = new JEditorPane();
        epHelp.setPreferredSize(new Dimension(500, 400));
        epHelp.setContentType("text/html");
        epHelp.setEditable(false);
        JScrollPane spHelp = new JScrollPane(epHelp);
        this.getContentPane().add(spHelp);

        this.pack();
    }

    private void readHelpToPane() throws Exception {
        Scanner helpScanner = null;
        try {
            helpScanner = new Scanner(new File(HELP_FILE_NAME));
            epHelp.setText(helpScanner.useDelimiter("\\Z").next()); // read entire file into Help editor pane
            helpFileLoaded = true;
        } catch (Exception e) {
            helpFileLoaded = false;
            throw new Exception("Could not open the help file " + HELP_FILE_NAME + " from " + System.getProperty("user.dir"));
        } finally {
            if (helpScanner != null) {
                helpScanner.close();
            }
        }
    }

    public void showWindow() throws Exception {
        if (!helpFileLoaded) {
            readHelpToPane();
        }

        epHelp.scrollRectToVisible(new Rectangle(0, 0));
        this.setVisible(true);
    }

    public void hideWindow() {
        this.setVisible(false);
    }
}
