package com.tuzhennan.purertc.app;

import javax.swing.*;

public class SwingApplication {

    private JLabel lToRDelayLabel;
    private JTextField lToRDelayInput;
    private JButton lToRDelayBtn;

    private JLabel rToLDelayLabel;
    private JTextField rToLDelayInput;
    private JButton rToLDelayBtn;

    private JLabel lToRLossLabel;
    private JTextField lToRLossInput;
    private JButton lToRLossBtn;

    private JLabel rToLLossLabel;
    private JTextField rToLLossInput;
    private JButton rToLLossBtn;

    private JLabel bandwidthLabel;
    private JTextField bandwidthInput;
    private JButton bandwidthBtn;

    private JLabel rateLimitMethodLabel;
    private JComboBox<String> rateLimitMethod;

    private JButton startBtn;
    private JButton pauseBtn;
    private JButton stopBtn;

    private void createDelayComponent(Box box) {
        Box box1 = Box.createHorizontalBox();
        lToRDelayLabel = new JLabel("LTR Delay: ");
        lToRDelayInput = new JTextField();
        lToRDelayBtn = new JButton("commit");
        box1.add(lToRDelayLabel);
        box1.add(lToRDelayInput);
        box1.add(lToRDelayBtn);
        box.add(box1);

        Box box2 = Box.createHorizontalBox();
        rToLDelayLabel = new JLabel("RTL Delay: ");
        rToLDelayInput = new JTextField();
        rToLDelayBtn = new JButton("commit");
        box2.add(rToLDelayLabel);
        box2.add(rToLDelayInput);
        box2.add(rToLDelayBtn);
        box.add(box2);
    }

    private void createLossComponent(Box box) {
        Box box1 = Box.createHorizontalBox();
        lToRLossLabel = new JLabel("LTR Loss: ");
        lToRLossInput = new JTextField();
        lToRLossBtn = new JButton("commit");
        box1.add(lToRLossLabel);
        box1.add(lToRLossInput);
        box1.add(lToRLossBtn);
        box.add(box1);

        Box box2 = Box.createHorizontalBox();
        rToLLossLabel = new JLabel("RTL Loss: ");
        rToLLossInput = new JTextField();
        rToLLossBtn = new JButton("commit");
        box2.add(rToLLossLabel);
        box2.add(rToLLossInput);
        box2.add(rToLLossBtn);
        box.add(box2);
    }

    private void createBandwidthComponent(Box box) {
        Box box1 = Box.createHorizontalBox();
        bandwidthLabel = new JLabel("Bandwidth(bps): ");
        bandwidthInput = new JTextField();
        bandwidthBtn = new JButton("commit");
        box1.add(bandwidthLabel);
        box1.add(bandwidthInput);
        box1.add(bandwidthBtn);
        box.add(box1);
    }

    private void createRatelimitBox(Box box) {
        Box box1 = Box.createHorizontalBox();
        rateLimitMethodLabel = new JLabel("Bandwidth(bps): ");
        rateLimitMethod = new JComboBox<String>();
        rateLimitMethod.addItem("Fixed Window");
        rateLimitMethod.addItem("Sliding Window");
        rateLimitMethod.addItem("Leaky Bucket");
        rateLimitMethod.addItem("Token Bucket");
        box1.add(rateLimitMethodLabel);
        box1.add(rateLimitMethod);
        box.add(box1);
    }

    private void createController(Box box) {
        Box box1 = Box.createHorizontalBox();
        startBtn = new JButton("Start");
        pauseBtn = new JButton("Pause");
        stopBtn = new JButton("Stop");
        box1.add(startBtn);
        box1.add(pauseBtn);
        box1.add(stopBtn);
        box.add(box1);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("PureRTC");
        frame.setLocationRelativeTo(null);
        frame.setSize(400, 300);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Box vBox = Box.createVerticalBox();
        frame.add(vBox);

        createDelayComponent(vBox);
        createLossComponent(vBox);
        createBandwidthComponent(vBox);
        createRatelimitBox(vBox);
        createController(vBox);

        frame.setVisible(true);
    }
    public static void main(String[] args) {
        SwingApplication app = new SwingApplication();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                app.createAndShowGUI();
            }
        });
    }
}
