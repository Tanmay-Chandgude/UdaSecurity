package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.image.ImageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // ---------------- SENSOR ACTIVATION ----------------

    @Test
    void activateSensor_whenArmedHomeAndNoAlarm_setsPendingAlarm() {

        Sensor sensor = new Sensor("Door", null);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void activateSensor_whenArmedAwayAndNoAlarm_setsPendingAlarm() {

        Sensor sensor = new Sensor("Door", null);

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void activateSensor_whenAlreadyPendingAlarm_setsAlarm() {

        Sensor sensor = new Sensor("Door", null);
        sensor.setActive(false);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void activateSensor_whenPendingAlarmAndSensorReactivated_setsAlarm() {

        Sensor sensor = new Sensor("Door", null);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void deactivateSensor_whenPendingAlarmAndAllSensorsInactive_setsNoAlarm() {

        Sensor sensor = new Sensor("Door", null);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository).updateSensor(sensor);
    }

    @Test
    void changeSensor_whenAlarmActive_onlyUpdatesSensor_noStateChange() {

        Sensor sensor = new Sensor("Door", null);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).updateSensor(sensor);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    void deactivateInactiveSensor_whenAnyAlarmStatus_doesNotChangeState(AlarmStatus status) {

        Sensor sensor = new Sensor("Door", null);

        when(securityRepository.getAlarmStatus()).thenReturn(status);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // ---------------- IMAGE PROCESSING ----------------

    @Test
    void detectCat_whenArmedHome_setsAlarm() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void detectNoCat_whenNoSensorsActive_setsNoAlarm() {

        when(securityRepository.getSensors()).thenReturn(Set.of());
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        securityService.processImage(null);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void processImage_whenDisarmed_doesNotChangeAlarmState() {

        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // ---------------- ARMING ----------------

    @Test
    void setArmingStatus_whenDisarmed_setsNoAlarm() {

        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void setArmingStatus_whenArmedHome_resetsAllSensorsToInactive() {

        Sensor s1 = new Sensor("Door", null);
        Sensor s2 = new Sensor("Window", null);

        s1.setActive(true);
        s2.setActive(true);

        when(securityRepository.getSensors()).thenReturn(Set.of(s1, s2));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertFalse(s1.getActive());
        assertFalse(s2.getActive());

        verify(securityRepository).updateSensor(s1);
        verify(securityRepository).updateSensor(s2);
    }

    @Test
    void setArmingStatus_whenCatPreviouslyDetected_setsAlarm() {

        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(null);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // ---------------- LISTENERS ----------------

    @Test
    void addListener_whenAlarmSet_notifiesAllListeners() {

        StatusListener listener = mock(StatusListener.class);

        securityService.addStatusListener(listener);
        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(listener).notify(AlarmStatus.ALARM);
    }

    @Test
    void removeListener_whenRemoved_doesNotReceiveNotifications() {

        StatusListener listener = mock(StatusListener.class);

        securityService.addStatusListener(listener);
        securityService.removeStatusListener(listener);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(listener, never()).notify(any());
    }

    @Test
    void multipleListeners_whenAlarmSet_allReceiveNotification() {

        StatusListener l1 = mock(StatusListener.class);
        StatusListener l2 = mock(StatusListener.class);

        securityService.addStatusListener(l1);
        securityService.addStatusListener(l2);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(l1).notify(AlarmStatus.ALARM);
        verify(l2).notify(AlarmStatus.ALARM);
    }

    // ---------------- DELEGATION ----------------

    @Test
    void getAlarmStatus_delegatesToRepository() {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.getAlarmStatus();

        verify(securityRepository).getAlarmStatus();
    }

    @Test
    void getSensors_delegatesToRepository() {

        when(securityRepository.getSensors()).thenReturn(Set.of());

        securityService.getSensors();

        verify(securityRepository).getSensors();
    }

    @Test
    void addSensor_delegatesToRepository() {

        Sensor sensor = new Sensor("Door", null);

        securityService.addSensor(sensor);

        verify(securityRepository).addSensor(sensor);
    }

    @Test
    void removeSensor_delegatesToRepository() {

        Sensor sensor = new Sensor("Door", null);

        securityService.removeSensor(sensor);

        verify(securityRepository).removeSensor(sensor);
    }
}