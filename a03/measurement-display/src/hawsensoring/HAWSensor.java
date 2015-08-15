package hawsensoring;

import hawmetering.Exception_Exception;
import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWSensorWebserviceService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import net.java.dev.jaxb.array.StringArray;

public class HAWSensor {

    private static final int TRIGGER_TIMEOUT = 4000;

    private Map<String, hawmetering.HAWSensorWebservice> sensorWebservices;

    private HAWMeteringWebservice hawMeteringWebservice;
    private String hawmeterChart;
    private String hawmeterURL;

    private String sensorURL;
    private String sensorName;

    private boolean coordinator;
    private String coordinatorUrl;

    private Endpoint endpoint;
    private Timer timer;
    private TimerTask triggerTimeoutTask;
    private boolean electionRunning;
    private TriggerThread triggerThread;



    public HAWSensor(String hawmeterIP, String hawmeterPort, String hawmeterChart,
                     String sensorIP, String sensorPort, String sensorName,
                     String startSensorIP, String startSensorPort) {

        init(hawmeterIP, hawmeterPort, hawmeterChart, sensorIP, sensorPort, sensorName);

        try {
            this.hawMeteringWebservice = getHawmeterService(this.hawmeterURL);

            String startSensorURL = "http://" + startSensorIP + ":" + startSensorPort + "/hawmetering/sensor";

            hawmetering.HAWSensorWebservice startSensor = getSensorWebservice(startSensorURL);

            for (int i = 1; i <= 10 && startSensor.isElectionRunning(); i++) {
                System.out.println("new Sensor() -> Election is currently running..." + i + "/10");
                Thread.sleep(1000);
                if(i == 10) {
                    System.exit(0);
                }
            }

            this.coordinatorUrl = startSensor.getCoordinatorUrl();
            hawmetering.HAWSensorWebservice coordinatorWebservice = getSensorWebservice(this.coordinatorUrl);
            this.hawMeteringWebservice.setTitle(sensorName);
            coordinatorWebservice.registerSensor(this.sensorURL, this.hawmeterChart);
            startTriggerTimeoutTask();

        } catch (InterruptedException | Exception_Exception e) {

            e.printStackTrace();
        }

    }

    public HAWSensor(String hawmeterIP, String hawmeterPort, String hawmeterChart,
                     String sensorIP, String sensorPort, String sensorName) {

        init(hawmeterIP, hawmeterPort, hawmeterChart, sensorIP, sensorPort, sensorName);

        try {
            this.hawMeteringWebservice = getHawmeterService(this.hawmeterURL);
            this.coordinator = true;
            this.hawMeteringWebservice.setTitle(sensorName);
            this.coordinatorUrl = this.sensorURL;

            registerSensor(this.coordinatorUrl);
            startTriggerThread();

        } catch (Exception e) {
            timer.cancel();
            if(endpoint != null)
                endpoint.stop();
            e.printStackTrace();
        }

    }


    private void init(String hawmeterIP, String hawmeterPort, String hawmeterChart, String sensorIP, String sensorPort, String sensorName) {

        sensorWebservices = new HashMap<>();

        this.hawmeterChart = hawmeterChart;
        this.hawmeterURL = "http://" + hawmeterIP + ":" + hawmeterPort + "/hawmetering/" + this.hawmeterChart;
        this.sensorURL = "http://" + sensorIP + ":" + sensorPort + "/hawmetering/sensor";
        this.sensorName = sensorName;

        timer = new Timer("coordinatorTriggerTimeout");

        addShutdownHook();

        publishWebservice(this.sensorURL);
    }

    private hawmetering.HAWMeteringWebservice getHawmeterService(String url) {
        hawmetering.HAWMeteringWebserviceService service;
        hawmetering.HAWMeteringWebservice metering = null;

        try {
            service = new hawmetering.HAWMeteringWebserviceService(new URL(url), new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
            metering = service.getHAWMeteringWebservicePort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return metering;
    }

    //Sensoren registrieren
    public void registerSensor(String url) {
        synchronized (sensorWebservices) {
            System.out.print("\n<---Register sensor--- " + url + "\n");

            hawmetering.HAWSensorWebservice sensor = getSensorWebservice(url);
            sensorWebservices.put(url, sensor);
            publishSensorList();
        }
    }

    public void publishSensorList() {

        System.out.print("\n---Updating sensors---> [ ");
        for(String sensor : sensorWebservices.keySet()) {
            System.out.print(sensor + " ");
        }
        System.out.print("]\n");

        String[] sensorUrlsArray = sensorWebservices.keySet().toArray(new String[0]);

        StringArray sensors = new StringArrayConverter(sensorUrlsArray);

        for (Map.Entry<String, hawmetering.HAWSensorWebservice> entry : sensorWebservices.entrySet()) {
            if(!entry.getValue().equals(this))
                entry.getValue().refreshSensors(sensors);
        }
    }

    public void refreshSensors(String[] sensorUrls) {

        System.out.print("\n---Refreshing sensors---> [ ");
        for(String sensor : sensorUrls) {
            System.out.print(sensor + " ");
        }
        System.out.print("]\n\n");

        HashMap<String, hawmetering.HAWSensorWebservice> newSensorWebservices = new HashMap<>();

        for (String sensorUrl : sensorUrls) {
            if (sensorWebservices.containsKey(sensorUrl)) {
                newSensorWebservices.put(sensorUrl, sensorWebservices.get(sensorUrl));
            } else {
                newSensorWebservices.put(sensorUrl, getSensorWebservice(sensorUrl));
            }
        }

        sensorWebservices = newSensorWebservices;
    }

    public String getCoordinatorUrl() {
        return this.coordinatorUrl;
    }

    public void trigger() {
        int messwert = 0;
        if (electionRunning) {
            System.out.println("trigger() -> Election is currently running...");
        } else {
            if (!coordinator) {
                startTriggerTimeoutTask();
            }
            System.out.println("Triggerd -> sensor: " + this.sensorName + " (value: " + this.calculate() + ")");

            messwert = calculate();
            hawMeteringWebservice.setValue(messwert);
            hawMeteringWebservice.setTitle(sensorName);
        }
    }

    private int calculate() {
        long lTicks = new Date().getTime();
        int messwert = ((int) (lTicks % 20000)) / 100;
        if (messwert > 100) {
            messwert = 200 - messwert;
        }
        return messwert;
    }

    private void startTriggerTimeoutTask() {
        if (triggerTimeoutTask != null) {
            triggerTimeoutTask.cancel();
        }

        triggerTimeoutTask = new TimerTask() {
            @Override
            public void run() {
                election();
            }
        };

        timer.schedule(triggerTimeoutTask, TRIGGER_TIMEOUT);
    }

    private void startTriggerThread() {
        if (triggerThread != null) {
            triggerThread.interrupt();
            triggerThread.shutDown();
        }
        triggerThread = new TriggerThread(this);
        triggerThread.start();
    }

    public void election() {

        if (electionRunning) {
            System.out.println("election() -> Election is currently running... by (" + Thread.currentThread().getName() + ")");
        } else {
            electionRunning = true;
            System.out.println("<<< Starting Election >>>");
            coordinator = true;
            for (Map.Entry<String, hawmetering.HAWSensorWebservice> entry : sensorWebservices.entrySet()) {
                try {
                    if (entry.getKey().compareTo(sensorURL) > 0) {
                        entry.getValue().election();
                        coordinator = false;
                    }
                } catch (Exception e) {
                    System.out.println("><><>< Unreachable: " + entry.getValue() + " ><><><");
                }
            }
            System.out.println("<<< Finished Election >>>");

            if (this.coordinator) {
                hawMeteringWebservice.setTitle(sensorName);

                for (Map.Entry<String, hawmetering.HAWSensorWebservice> entry : sensorWebservices.entrySet()) {
                    try {
                        entry.getValue().newCoordinator(sensorURL);

                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
                startTriggerThread();
            } else {
                startTriggerTimeoutTask();
            }
            electionRunning = false;
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("shutdown(" + sensorName + ")...");
                hawMeteringWebservice.setTitle(hawmeterChart);
                hawMeteringWebservice.setValue(0);
                endpoint.stop();
                System.out.println("...BYE!");
            }
        }));
    }

    private hawmetering.HAWSensorWebservice getSensorWebservice(String url) {
        HAWSensorWebserviceService service = null;
        hawmetering.HAWSensorWebservice sensor = null;

        try {
            service = new HAWSensorWebserviceService( new URL(url), new QName("http://hawmetering/", "HAWSensorWebserviceService"));
            sensor = service.getHAWSensorWebservicePort();
        } catch (MalformedURLException e) {
            //e.printStackTrace();
        }

        return sensor;
    }

    private void publishWebservice(String url) {
//		hawmetering.HAWSensorWebserviceService webservice = new hawmetering.HAWSensorWebserviceService(); 
        HAWSensorWebservice webservice = new HAWSensorWebservice(this);
        this.endpoint = Endpoint.publish(url, webservice);
        System.out.println("Published sensorWebservice -> " + this.sensorURL);
    }

    public Map<String, hawmetering.HAWSensorWebservice> getSensorWebservices() {
        return this.sensorWebservices;
    }

    public void newCoordinator(String cooridUrl) {
        this.coordinatorUrl = cooridUrl;
        System.out.println(">>> new Coordinator: " + cooridUrl);
    }

    public boolean isElectionRunning() {
        return this.electionRunning;
    }
}
