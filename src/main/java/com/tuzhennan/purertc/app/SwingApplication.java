package com.tuzhennan.purertc.app;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Log
public class SwingApplication {

    private Driver driver;

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

    private long parseLToRDelay() {
        String content = lToRDelayInput.getText();
        return StringUtils.isBlank(content) ? Driver.kDefaultLToRDelay : Long.parseLong(content);
    }
    private long parseRToLDelay() {
        String content = rToLDelayInput.getText();
        return StringUtils.isBlank(content) ? Driver.kDefaultRToLDelay : Long.parseLong(content);
    }
    private float parseLToRLoss() {
        String content = lToRDelayInput.getText();
        return StringUtils.isBlank(content) ? Driver.kDefaultLToRLoss : Integer.parseInt(content) / 100.f;
    }
    private float parseRToLLoss() {
        String content = rToLLossInput.getText();
        return StringUtils.isBlank(content) ? Driver.kDefaultRToLLoss : Integer.parseInt(content) / 100.f;
    }
    private long parseBandwidth() {
        String content = bandwidthInput.getText();
        return StringUtils.isBlank(content) ? Driver.kDefaultBandwidthKbps : Long.parseLong(content);
    }

    private void createDelayComponent(Box box) {
        Box box1 = Box.createHorizontalBox();
        lToRDelayLabel = new JLabel("LTR Delay: ");
        lToRDelayInput = new JTextField();
        lToRDelayBtn = new JButton("commit");
        lToRDelayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingApplication.this.driver.setLeftToRightDelayMS(parseLToRDelay());
            }
        });
        box1.add(lToRDelayLabel);
        box1.add(lToRDelayInput);
        box1.add(lToRDelayBtn);
        box.add(box1);

        Box box2 = Box.createHorizontalBox();
        rToLDelayLabel = new JLabel("RTL Delay: ");
        rToLDelayInput = new JTextField();
        rToLDelayBtn = new JButton("commit");
        rToLDelayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingApplication.this.driver.setRightToLeftDelayMS(parseRToLDelay());
            }
        });
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
        lToRLossBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingApplication.this.driver.setLeftToRightLossRatio(parseLToRLoss());
            }
        });
        box1.add(lToRLossLabel);
        box1.add(lToRLossInput);
        box1.add(lToRLossBtn);
        box.add(box1);

        Box box2 = Box.createHorizontalBox();
        rToLLossLabel = new JLabel("RTL Loss: ");
        rToLLossInput = new JTextField();
        rToLLossBtn = new JButton("commit");
        rToLLossBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingApplication.this.driver.setRightToLeftLossRatio(parseRToLLoss());
            }
        });
        box2.add(rToLLossLabel);
        box2.add(rToLLossInput);
        box2.add(rToLLossBtn);
        box.add(box2);
    }

    private void createBandwidthComponent(Box box) {
        Box box1 = Box.createHorizontalBox();
        bandwidthLabel = new JLabel("Bandwidth(kbps): ");
        bandwidthInput = new JTextField();
        bandwidthBtn = new JButton("commit");
        bandwidthBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SwingApplication.this.driver.setBandwidthKbps(parseBandwidth());
            }
        });
        box1.add(bandwidthLabel);
        box1.add(bandwidthInput);
        box1.add(bandwidthBtn);
        box.add(box1);
    }

    private void createRatelimitBox(Box box) {
        Box box1 = Box.createHorizontalBox();
        rateLimitMethodLabel = new JLabel("Ratelimit method: ");
        rateLimitMethod = new JComboBox<>();
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
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);
                driver = new Driver();
                parseAndSetAllDriverConfigurations();
                driver.asyncRun();
            }
        });
        pauseBtn = new JButton("Pause");
        stopBtn = new JButton("Stop");
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopBtn.setEnabled(false);
                startBtn.setEnabled(true);
                driver.stop();
            }
        });
        box1.add(startBtn);
        box1.add(pauseBtn);
        box1.add(stopBtn);
        box.add(box1);
    }

    private void parseAndSetAllDriverConfigurations() {
        driver.setLeftToRightDelayMS(parseLToRDelay());
        driver.setRightToLeftDelayMS(parseRToLDelay());
        driver.setLeftToRightLossRatio(parseLToRLoss());
        driver.setRightToLeftLossRatio(parseRToLLoss());
        driver.setBandwidthKbps(parseBandwidth());
    }

    private void disableSomeButton() {
        lToRDelayBtn.setEnabled(false);
        lToRLossBtn.setEnabled(false);
        rToLDelayBtn.setEnabled(false);
        rToLLossBtn.setEnabled(false);
        bandwidthBtn.setEnabled(false);
        rateLimitMethod.setEnabled(false);
        pauseBtn.setEnabled(false);
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
        disableSomeButton(); //TODO:Implement in-run-configuration
        stopBtn.setEnabled(false);

        frame.setVisible(true);
    }

    public SwingApplication() {
    }

    public static void main(String[] args) {
        SwingApplication app = new SwingApplication();
        javax.swing.SwingUtilities.invokeLater(app::createAndShowGUI);
    }
}
