package hawsensoring;

public class Main {

    public static final int START_SENSOR = 8;
    public static final int FOLLOWING_SENSOR = 6;

    public static final int ARG_HAWMETERING_IP = 0;
    public static final int ARG_HAWMETERING_PORT = 1;
    public static final int ARG_HAWMETERING_CHART = 2;
    public static final int ARG_HAWSENSOR_IP = 3;
    public static final int ARG_HAWSENSOR_PORT = 4;
    public static final int ARG_HAWSENSOR_SONSORNAME = 5;
    public static final int ARG_HAWSENSOR_START_IP = 6;
    public static final int ARG_HAWSENSOR_START_PORT = 7;

    public static void main(String[] args) {

        System.setProperty("sun.net.client.defaultConnectTimeout", "3000");
        System.setProperty("sun.net.client.defaultReadTimeout", "3000");

        switch(args.length){
            case START_SENSOR:
                new HAWSensor(
                        args[ARG_HAWMETERING_IP],
                        args[ARG_HAWMETERING_PORT],
                        args[ARG_HAWMETERING_CHART],
                        args[ARG_HAWSENSOR_IP],
                        args[ARG_HAWSENSOR_PORT],
                        args[ARG_HAWSENSOR_SONSORNAME],
                        args[ARG_HAWSENSOR_START_IP],
                        args[ARG_HAWSENSOR_START_PORT]);
                break;
            case FOLLOWING_SENSOR:
                new HAWSensor(
                        args[ARG_HAWMETERING_IP],
                        args[ARG_HAWMETERING_PORT],
                        args[ARG_HAWMETERING_CHART],
                        args[ARG_HAWSENSOR_IP],
                        args[ARG_HAWSENSOR_PORT],
                        args[ARG_HAWSENSOR_SONSORNAME]);
                break;
        }
    }
}
