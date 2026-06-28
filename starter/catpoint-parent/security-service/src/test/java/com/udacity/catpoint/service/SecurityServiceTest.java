package com.udacity.catpoint.service;

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
import com.udacity.catpoint.application.StatusListener;

import static org.mockito.Mockito.*;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService =
                new SecurityService(
                        securityRepository,
                        imageService
                );
    }

    @Test
    void setSensorActiveWhenArmedChangesStateToPendingAlarm() {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(false);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void setSensorActiveWhenAlreadyPendingChangesStateToAlarm() {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(false);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void setSensorInactiveWhenPendingAlarmChangesStateToNoAlarm() {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void sensorStateChangeWhenAlarmActiveDoesNotChangeAlarmState() {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never())
                .setAlarmStatus(any());
    }

    @Test
    void activeSensorStillActiveWhenPendingChangesToAlarm() {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    void inactiveSensorStillInactiveDoesNotChangeAlarmState(
            AlarmStatus alarmStatus) {

        Sensor sensor = new Sensor("Front Door", null);
        sensor.setActive(false);

        when(securityRepository.getAlarmStatus())
                .thenReturn(alarmStatus);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never())
                .setAlarmStatus(any());
    }

    @Test
    void catDetectedWhileArmedHomeChangesToAlarm() {

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatDetectedChangesToNoAlarmWhenNoSensorsActive() {

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(false);

        when(securityRepository.getSensors())
                .thenReturn(Set.of());

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void disarmingSystemSetsNoAlarm() {

        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void armingSystemResetsAllSensorsToInactive() {

        Sensor sensor1 = new Sensor("Door", null);
        Sensor sensor2 = new Sensor("Window", null);

        sensor1.setActive(true);
        sensor2.setActive(true);

        when(securityRepository.getSensors())
                .thenReturn(Set.of(sensor1, sensor2));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
    }

    @Test
    void catAlreadyDetectedThenArmingHomeChangesToAlarm() {

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        securityService.setArmingStatus(
                ArmingStatus.ARMED_HOME
        );

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }
 //extra test added
    @Test
    void addStatusListenerRegistersListener() {

        StatusListener listener = mock(StatusListener.class);

        securityService.addStatusListener(listener);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(listener).notify(AlarmStatus.ALARM);
    }

    @Test
    void removeStatusListenerStopsNotifications() {

        StatusListener listener = mock(StatusListener.class);

        securityService.addStatusListener(listener);
        securityService.removeStatusListener(listener);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(listener, never()).notify(any());
    }

    @Test
    void getAlarmStatusReturnsRepositoryValue() {

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        securityService.getAlarmStatus();

        verify(securityRepository).getAlarmStatus();
    }

    @Test
    void getSensorsReturnsRepositorySensors() {

        when(securityRepository.getSensors())
                .thenReturn(Set.of());

        securityService.getSensors();

        verify(securityRepository).getSensors();
    }

    @Test
    void addSensorDelegatesToRepository() {

        Sensor sensor = new Sensor("Door", null);

        securityService.addSensor(sensor);

        verify(securityRepository).addSensor(sensor);
    }

    @Test
    void removeSensorDelegatesToRepository() {

        Sensor sensor = new Sensor("Door", null);

        securityService.removeSensor(sensor);

        verify(securityRepository).removeSensor(sensor);
    }
}