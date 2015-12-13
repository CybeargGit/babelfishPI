package edu.dsu.bpi;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public class AboutDialog extends JDialog {

    private static final String ABOUT_MESSAGE = "<h1>Babelfish Pseudocode Interpreter</h1><br>" +
            "Created by Nicholas Brosz<br>" +
            "Based on \"Visual Interpreter\" by Brent Van Aartsen<br>" +
            "Babelfish icon from clker.com<br><br>" +
            "For bugs, errors, or feature requests, please email nsbrosz@pluto.dsu.edu<br><br>" +
            "Released under the MIT open source license.<br>" +
            "<a href='https://github.com/CybeargGit/babelfishPI'>https://github.com/CybeargGit/babelfishPI</a>";

    public AboutDialog(JFrame parent) {
        super(parent, "About BabelfishPI");
        initialize();
    }

    private void initialize() {
        this.setPreferredSize(new Dimension(400, 300));
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("icon.png")));

        JPanel aboutPanel = new JPanel();
        JTextPane aboutPane = new JTextPane();
        aboutPane.setContentType("text/html");
        aboutPane.setText("<html><head><style>h1{text-align: center; font-size: 16pt;}</style></head><body>" + ABOUT_MESSAGE + "</body></html>");
        aboutPane.setPreferredSize(new Dimension(300, 250));
        aboutPane.setEditable(false);
        aboutPane.setBackground(null);
        aboutPane.setBorder(null);
        aboutPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException e1) {
                            // do nothing
                        }
                    }
                }
            }
        });
        aboutPanel.add(aboutPane);

        getContentPane().add(aboutPanel);

        this.pack();
    }

    @Override
    public void show() {
        super.show();
        this.getParent().setEnabled(false); // lock parent frame
    }

    @Override
    public void hide() {
        this.getParent().setEnabled(true); // unlock parent frame
        super.hide();
    }
}
