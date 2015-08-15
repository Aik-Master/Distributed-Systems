package client;

public class Calculation {

    private int processCountIntervalMin;
    private int processCountIntervalMax;
    private int processDelayIntervalMin;
    private int processDelayIntervalMax;
    private int terminationCheckTime;
    private int desiredGcd;

    public Calculation(int processCountIntervalMin, int processCountIntervalMax, int processDelayIntervalMin,
                       int processDelayIntervalMax, int terminationCheckTime, int desiredGcd) {

        this.processCountIntervalMin = processCountIntervalMin;
        this.processCountIntervalMax = processCountIntervalMax;
        this.processDelayIntervalMin = processDelayIntervalMin;
        this.processDelayIntervalMax = processDelayIntervalMax;
        this.terminationCheckTime = terminationCheckTime;
        this.desiredGcd = desiredGcd;
    }

    public int getProcessCountIntervalMin() {
        return processCountIntervalMin;
    }

    public int getProcessCountIntervalMax() {
        return processCountIntervalMax;
    }

    public int getProcessDelayIntervalMin() {
        return processDelayIntervalMin;
    }

    public int getProcessDelayIntervalMax() {
        return processDelayIntervalMax;
    }

    public int getTerminationCheckTime() {
        return terminationCheckTime;
    }

    public int getDesiredGcd() {
        return desiredGcd;
    }

    public static boolean validate(Calculation calculation) {
        boolean result = true;

        result = calculation.getProcessCountIntervalMin() > 0;
        result = result && calculation.getProcessCountIntervalMax() > 0;
        result = result && calculation.getProcessDelayIntervalMin() > 0;
        result = result && calculation.getProcessDelayIntervalMax() > 0;
        result = result && calculation.getTerminationCheckTime() > 0;
        result = result && calculation.getDesiredGcd() > 0;

        return result;
    }

    public static Calculation parse(String processCountIntervalStr, String processDelayIntervalStr,
                                    String  terminationCheckTimeStr, String desiredGcdStr) {

        String [] processCountInterval = processCountIntervalStr.split("-");
        String [] processDelayInterval = processDelayIntervalStr.split("-");

        int processCountIntervalMin = 0;
        int processCountIntervalMax = 0;
        int processDelayIntervalMin = 0;
        int processDelayIntervalMax = 0;
        int terminationCheckTime = 0;
        int desiredGcd = 0;

        try {
            processCountIntervalMin = Integer.valueOf(processCountInterval[0]);
        } catch (NumberFormatException e) {
            processCountIntervalMin = -1;
        }
        try {
            processCountIntervalMax = Integer.valueOf(processCountInterval[1]);
        } catch (NumberFormatException e) {
            processCountIntervalMax = -1;
        }
        try {
            processDelayIntervalMin = Integer.valueOf(processDelayInterval[0]);
        } catch (NumberFormatException e) {
            processDelayIntervalMin = -1;
        }
        try {
            processDelayIntervalMax = Integer.valueOf(processDelayInterval[1]);
        } catch (NumberFormatException e) {
            processDelayIntervalMax = -1;
        }
        try {
            terminationCheckTime = Integer.valueOf(terminationCheckTimeStr);
        } catch (NumberFormatException e) {
            terminationCheckTime = -1;
        }
        try {
            desiredGcd = Integer.valueOf(desiredGcdStr);
        } catch (NumberFormatException e) {
            desiredGcd = -1;
        }

        return new Calculation(processCountIntervalMin, processCountIntervalMax, processDelayIntervalMin,
                               processDelayIntervalMax, terminationCheckTime, desiredGcd);
    }
}
