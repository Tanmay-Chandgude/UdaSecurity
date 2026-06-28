package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.image.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners = new HashSet<>();

    private boolean catDetected = false;

    public SecurityService(SecurityRepository securityRepository,
                           ImageService imageService) {

        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    public void setArmingStatus(ArmingStatus armingStatus) {

        securityRepository.setArmingStatus(armingStatus);

        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        if (armingStatus == ArmingStatus.ARMED_HOME
                || armingStatus == ArmingStatus.ARMED_AWAY) {

            for (Sensor sensor : Set.copyOf(securityRepository.getSensors())) {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            }

            statusListeners.forEach(StatusListener::sensorStatusChanged);
        }

        if (armingStatus == ArmingStatus.ARMED_HOME
                && catDetected) {

            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void catDetected(Boolean cat) {

        this.catDetected = cat;

        // Cat + armed home -> alarm
        if (cat
                && getArmingStatus() == ArmingStatus.ARMED_HOME) {

            setAlarmStatus(AlarmStatus.ALARM);
        }

        // No cat -> NO_ALARM only if no sensors active
        else if (!cat
                && securityRepository.getSensors()
                .stream()
                .noneMatch(Sensor::getActive)) {

            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    private void handleSensorActivated() {

        if (securityRepository.getArmingStatus()
                == ArmingStatus.DISARMED) {
            return;
        }

        switch (securityRepository.getAlarmStatus()) {

            case NO_ALARM ->
                    setAlarmStatus(AlarmStatus.PENDING_ALARM);

            case PENDING_ALARM ->
                    setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void handleSensorDeactivated() {

        // Only pending alarm can return to no alarm
        if (securityRepository.getAlarmStatus()
                == AlarmStatus.PENDING_ALARM) {

            boolean allInactive =
                    securityRepository.getSensors()
                            .stream()
                            .noneMatch(Sensor::getActive);

            if (allInactive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
    }

    public void changeSensorActivationStatus(
            Sensor sensor,
            Boolean active) {

        // If already alarm -> sensor changes do nothing
        if (securityRepository.getAlarmStatus()
                == AlarmStatus.ALARM) {

            sensor.setActive(active);
            securityRepository.updateSensor(sensor);
            return;
        }

        // Active sensor triggered again while pending
        if (sensor.getActive()
                && active
                && securityRepository.getAlarmStatus()
                == AlarmStatus.PENDING_ALARM) {

            setAlarmStatus(AlarmStatus.ALARM);
        }

        // Sensor activated
        else if (!sensor.getActive() && active) {
            handleSensorActivated();
        }

        // Sensor deactivated
        else if (sensor.getActive() && !active) {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
            handleSensorDeactivated();
            return;
        }

        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }

    public void processImage(
            BufferedImage currentCameraImage) {

        catDetected(
                imageService.imageContainsCat(
                        currentCameraImage,
                        50.0f
                )
        );
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}