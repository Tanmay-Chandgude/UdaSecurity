package com.udacity.catpoint.application;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.data.SensorType;
import com.udacity.catpoint.service.SecurityService;
import com.udacity.catpoint.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Panel that allows users to add sensors to their system.
 * Sensors may be manually set to "active" and "inactive" to test the system.
 */
public class SensorPanel extends JPanel implements StatusListener {

    private SecurityService securityService;

    private JLabel panelLabel = new JLabel("Sensor Management");
    private JLabel newSensorName = new JLabel("Name:");
    private JLabel newSensorType = new JLabel("Sensor Type:");
    private JTextField newSensorNameField = new JTextField();
    private JComboBox<SensorType> newSensorTypeDropdown =
            new JComboBox<>(SensorType.values());

    private JButton addNewSensorButton =
            new JButton("Add New Sensor");

    private JPanel sensorListPanel;
    private JPanel newSensorPanel;

    public SensorPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());

        this.securityService = securityService;

        // Register listener
        securityService.addStatusListener(this);

        panelLabel.setFont(StyleService.HEADING_FONT);

        addNewSensorButton.addActionListener(e ->
                addSensor(new Sensor(
                        newSensorNameField.getText(),
                        (SensorType) newSensorTypeDropdown.getSelectedItem()
                ))
        );

        newSensorPanel = buildAddSensorPanel();

        sensorListPanel = new JPanel();
        sensorListPanel.setLayout(new MigLayout());

        updateSensorList(sensorListPanel);

        add(panelLabel, "wrap");
        add(newSensorPanel, "span");
        add(sensorListPanel, "span");
    }

    /**
     * Builds panel for adding sensors
     */
    private JPanel buildAddSensorPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());

        p.add(newSensorName);
        p.add(newSensorNameField, "width 50:100:200");

        p.add(newSensorType);
        p.add(newSensorTypeDropdown, "wrap");

        p.add(addNewSensorButton, "span 3");

        return p;
    }

    /**
     * Refresh sensor list UI
     */
    private void updateSensorList(JPanel p) {
        p.removeAll();

        securityService.getSensors()
                .stream()
                .sorted()
                .forEach(s -> {

                    JLabel sensorLabel =
                            new JLabel(String.format(
                                    "%s(%s): %s",
                                    s.getName(),
                                    s.getSensorType(),
                                    s.getActive() ? "Active" : "Inactive"
                            ));

                    JButton sensorToggleButton =
                            new JButton(
                                    s.getActive()
                                            ? "Deactivate"
                                            : "Activate"
                            );

                    JButton sensorRemoveButton =
                            new JButton("Remove Sensor");

                    sensorToggleButton.addActionListener(
                            e -> setSensorActivity(s, !s.getActive())
                    );

                    sensorRemoveButton.addActionListener(
                            e -> removeSensor(s)
                    );

                    p.add(sensorLabel, "width 300:300:300");
                    p.add(sensorToggleButton, "width 100:100:100");
                    p.add(sensorRemoveButton, "wrap");
                });

        p.repaint();
        p.revalidate();
    }

    /**
     * Toggle sensor state
     */
    private void setSensorActivity(Sensor sensor, Boolean isActive) {
        securityService.changeSensorActivationStatus(sensor, isActive);
        updateSensorList(sensorListPanel);
    }

    /**
     * Add sensor
     */
    private void addSensor(Sensor sensor) {
        if (securityService.getSensors().size() < 4) {
            securityService.addSensor(sensor);
            updateSensorList(sensorListPanel);
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "To add more than 4 sensors, please subscribe to our Premium Membership!"
            );
        }
    }

    /**
     * Remove sensor
     */
    private void removeSensor(Sensor sensor) {
        securityService.removeSensor(sensor);
        updateSensorList(sensorListPanel);
    }

    // ===== StatusListener methods =====

    @Override
    public void notify(AlarmStatus status) {
        updateSensorList(sensorListPanel);
    }

    @Override
    public void sensorStatusChanged() {
        updateSensorList(sensorListPanel);
    }

    @Override
    public void catDetected(boolean catDetected) {
        // No UI update needed here
    }
}