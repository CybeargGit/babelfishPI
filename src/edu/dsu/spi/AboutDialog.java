package edu.dsu.spi;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends JDialog {

    private static final String ABOUT_MESSAGE = "<h1>Suave Pseudo-Interpreter</h1><br>" +
            "Created by Nicholas Brosz<br>" +
            "Based on \"Visual Interpreter\" by Brent Van Aartsen<br><br>" +
            "For bugs, errors, or feature requests, please email nsbrosz@pluto.dsu.edu<br><br>" +
            "Released under the MIT open source license.";

    public AboutDialog(JFrame parent) {
        super(parent, "About Suave Pseudo-Interpreter");
        initialize();
    }

    private void initialize() {
        this.setPreferredSize(new Dimension(400, 300));
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("pipe_icon.png")));

        JPanel aboutPanel = new JPanel();
        JLabel label = new JLabel();
        label.setText("<html><head><style>h1{text-align: center; font-size: 16pt;}</style></head><body>" + ABOUT_MESSAGE + "</body></html>");
        label.setPreferredSize(new Dimension(300, 200));
        aboutPanel.add(label);

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
