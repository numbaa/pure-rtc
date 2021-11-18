package com.tuzhennan.purertc.app;

import javax.swing.*;

public class SwingApplication {
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("HelloSwing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel("It works.");
        frame.getContentPane().add(label);
        frame.pack();
        frame.setVisible(true);
    }
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
