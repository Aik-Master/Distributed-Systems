package hawsensoring;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService(name = "HAWSensorWebservice", targetNamespace = "http://hawmetering/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class HAWSensorWebservice {

    private final HAWSensor hawSensor;

    public HAWSensorWebservice(HAWSensor hawSensor) {
        this.hawSensor = hawSensor;
    }

    public void registerSensor (@WebParam(name = "hawMeteringURL") String url,
                                @WebParam(name = "hawMeteringChart") String chart) throws Exception {

        hawSensor.registerSensor(url);
    }

    public String getCoordinatorUrl() {
        return hawSensor.getCoordinatorUrl();
    }

    public void trigger() {
        hawSensor.trigger();
    }

    public void refreshSensors (@WebParam(name = "sensorUrlMap") String[] sensorUrls) {
        hawSensor.refreshSensors(sensorUrls);
    }

    public void election() {
        hawSensor.election();
    }

    public void newCoordinator(String url) {
        hawSensor.newCoordinator(url);
    }

    public boolean isElectionRunning(){
        return hawSensor.isElectionRunning();
    }
}
