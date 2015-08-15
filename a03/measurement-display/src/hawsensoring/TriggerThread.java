package hawsensoring;

import hawmetering.*;
import hawmetering.HAWSensorWebservice;

import java.lang.Exception;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TriggerThread extends Thread {

    private final HAWSensor hawSensor;
    private boolean running = true;

    public TriggerThread(HAWSensor hawSensor) {
        this.hawSensor = hawSensor;
    }

    @Override
    public void run() {

        while(running) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            sendTriggers();
        }

    }

    private void sendTriggers() {
        boolean needUpdate = false;
        Map<String, hawmetering.HAWSensorWebservice> sensorWebservices = hawSensor.getSensorWebservices();

        synchronized (sensorWebservices) {

            for (Iterator<Map.Entry<String, HAWSensorWebservice>> iterator = sensorWebservices.entrySet().iterator(); iterator.hasNext();) {

                Map.Entry<String, HAWSensorWebservice> sensorUrlsEntry = iterator.next();

                try {
                    sensorUrlsEntry.getValue().trigger();
                } catch (Exception e) {
                    needUpdate = true;
                    System.out.println(e.toString());
                    iterator.remove();
                }
            }
        }
        if (needUpdate) {
            hawSensor.publishSensorList();
        }
    }

    public void shutDown() {
        this.running = false;
    }
}
