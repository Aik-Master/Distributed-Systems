package vsKoordinator;

import gcd.Coordinator.CoordinatorPOA;
import monitor.Monitor;
import gcd.Starter.GcdProcess;
import gcd.Starter.Starter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.omg.CORBA.ORB;

public class CoordinatorImpl extends CoordinatorPOA{
    private ORB                  orb;
    private String               name;
    private Monitor monitor;

    private List<GcdProcess>     processes;
    private List<Starter>        starters;

    private int                  successfullyTerminated = 0;
    private boolean              terminationInProgress  = false;
    private int                  currentSeq;

    private boolean              busy                   = false;

    private Thread               terminationThread;

    private Map<String, Boolean> processesThatCalledCalculationComplete;

    public CoordinatorImpl(String name, Monitor monitor, ORB orb) {
        this.orb = orb;
        this.name = name;
        this.monitor = monitor;
        processes = new ArrayList<GcdProcess>();
        starters = new ArrayList<Starter>();
        processesThatCalledCalculationComplete = new HashMap<String, Boolean>();
    }

    @Override
    public synchronized Starter[] getAllStarters(){
        System.out.println("Enter getAllStarters()");
        
        // check if starters are "alive", delete dead ones
        ArrayList<Starter> deleteList = new ArrayList<Starter>();
        for (Starter starter : starters) {
            try{
                starter.getId();
            } catch(Exception e){
                deleteList.add(starter);
                System.out.println("One starter is not available and reference got delete from List.");
            }
        }
        for (Starter starter2delete : deleteList) {
            starters.remove(starter2delete);
        }

        return starters.toArray(new Starter[starters.size()]);
    }

    @Override
    public synchronized void calculationComplete(boolean complete, int result, String processID, int seq){
        System.out.println("TerminationCall from " + processID + " -> {complete: " + complete + "; result: " + result + "; seq: " + seq + "}");

        /*
        if (complete) {
            monitor.ergebnis(processID, result);
        }
        */

        if(seq == currentSeq){

            if(!complete){
                terminationInProgress = false;
                successfullyTerminated = 0;
            } else{


                // ignore when a process calls this a second time for the current seq
                if(processesThatCalledCalculationComplete.containsKey(processID)){
                    System.out.println("The Process: " + processID + " has called calculationComplete() before");
                    return;
                }
                processesThatCalledCalculationComplete.put(processID, true);
                successfullyTerminated++;
                if(successfullyTerminated == processes.size()) {
                    System.out.println("Calculation finished -> {successfullyTerminated: " + successfullyTerminated + "; processes.size(): " + processes.size()
                            + "}");
                    this.terminationThread.interrupt();
                    terminationInProgress = false;
                    busy = false;
                    System.out.println("Das Ergebnis ist: " + result);
                    shutdownAllProcesses();
                    monitor.ergebnis(processID, result);
                }
            }
        }// ignore older seqs
    }

    private void shutdownAllProcesses(){
        this.processes.clear();
        new Thread(){
            @Override
            public void run(){
                for(Starter starter : starters){
                    starter.endGcdProcesses();
                    System.out.println(starter.getId() + ".endGcdProcesses();");
                }
            }
        }.start();
        System.out.println("All Processes shoudl be killed(end of \"shutdownAllProcesses()\")");
    }

    @Override
    public void calculateGcd(int minNoProcesses, int maxNoProcesses, int minDelay, int maxDelay, int tTimeout, int gcd){
        if(busy){
            System.out.println("Ich warte selbst noch... behalte deine Auftraege fuer dich!");
            return;
        }
        System.out.println("Enter calculateGcd() mit minNoProcesses: " + minNoProcesses + ", maxNoProcesses: " + maxNoProcesses + "minDelay: " + minDelay
                + ", maxDelay: " + maxDelay + ", tTimeout: " + tTimeout + ", gcd: " + gcd);

        busy = true;
        successfullyTerminated = 0;
        this.processes.clear();

        int starterNofProcesses = 0;
        int totalNofProcesses = 0;
        for(Starter starter : starters){
            starterNofProcesses = randomWithinInterval(minNoProcesses, maxNoProcesses);
            totalNofProcesses += starterNofProcesses;
            starter.startGcdProcesses(starterNofProcesses);
        }

        // wait for all the started processes to arrive
        try{
            while(processes.size() < totalNofProcesses){
                Thread.sleep(1000);
            }
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        // Abort if less than 3 processes
        if(totalNofProcesses < 3){
            System.out.println("Too few processes to build ring");
            shutdownAllProcesses();
            busy = false;
            return;
        }

        createGcdRing(gcd, minDelay, maxDelay);

        // get the 3 Processes with the lowest starting Mi by sorting all
        ArrayList<GcdProcess> sortedList = new ArrayList<GcdProcess>(processes);
        Collections.sort(sortedList, new Comparator<GcdProcess>(){
            @Override
            public int compare(GcdProcess process1, GcdProcess process2){
                return process1.getNumber() - process2.getNumber();
            }
        });

        // start those 3 processes
        for(int i = 0 ; i < 3 ; i++){
            sortedList.get(i).startGcdAlgorithm();
        }

        terminationRequest(tTimeout);

    }

    private void terminationRequest(final int interval){

        this.terminationThread = new Thread(){
            @Override
            public void run(){
                // initial sleep
                try{
                    sleep(interval);
                } catch(InterruptedException ex){
                    return;
                }
                while(!isInterrupted()){
                    if(!terminationInProgress){ //if(!terminationInProgress){
                        processes.get(randomWithinInterval(0, processes.size() - 1)).terminateGcdCalculation(name, ++currentSeq);
                        terminationInProgress = true;
                        processesThatCalledCalculationComplete.clear();
                        System.out.println("Calculation-Termination(seq:" + currentSeq + ") initiated!");
                    }
                    try{
                        sleep(interval);
                    } catch(InterruptedException ex){
                        return;
                    }
                }
            }
        };

        this.terminationThread.start();
    }

    private void createGcdRing(int gcd, int minDelay, int maxDelay){
        System.out.println("Enter createGcdRing mit: gcd: " + gcd + ", minDelay: " + minDelay + ", maxDelay: " + maxDelay);

        // variables for monitor
        int startzahlen[] = new int[processes.size()];
        String prozessIds[] = new String[processes.size()];

        // shuffeling randomizes the future neighbors
        Collections.shuffle(processes);

        for(int i = 0 ; i < processes.size() ; i++){
            int gcdRandom = gcdRandom(gcd);

            // collect data for monitor
            startzahlen[i] = gcdRandom;
            prozessIds[i] = processes.get(i).getName();

            // leftNeighbor index -1, rightNeighbour index +1 || incl. wrap around
            GcdProcess rightNeighbour = processes.get(i == processes.size() - 1 ? 0 : i + 1);
            GcdProcess leftNeighbour = processes.get(i == 0 ? processes.size() - 1 : i - 1);
            processes.get(i).initGcdProcess(rightNeighbour, leftNeighbour, gcdRandom, randomWithinInterval(maxDelay, minDelay), monitor); // todo: replace null with monitor
            System.out.println("Call von " + processes.get(i).getName() + ".initGcdProcess() mit rightNeighbour: " + rightNeighbour.getName()
                    + ", leftNeighbour: " + leftNeighbour.getName() + ", gcdRandom: " + gcdRandom);

        }

        monitor.ring(prozessIds);
        monitor.startzahlen(startzahlen);
    }

    private int gcdRandom(int gcd){
        return gcd * randomWithinInterval(1, 100) * randomWithinInterval(1, 100);
    }

    @Override
    public void registerStarter(Starter starter){
        System.out.println("Starter " + starter.getId() + " registerd.");
        this.starters.add(starter);
    }

    @Override
    public void registerGcdProcess(GcdProcess process){
        System.out.println("Starter " + process.getName() + " registerd.");
        processes.add(process);
    }

    @Override
    public void shutDown(){
        System.out.println("Enter shutDown()");

        for(Starter starter : starters){
            starter.shutDown();
        }

        System.out.println("shutDown(): All Starters have been told shutdown");

        new Thread(){
            @Override
            public void run(){
                try{
                    Thread.sleep(1000);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
                System.out.println("shutDown(): Coordinator has waited for 1 sec before calling orb.shutdown()");
                orb.shutdown(true);
                System.out.println("shutDown(): ORB wurde heruntergefahren\n");
            }
        }.start();
    }

    private int randomWithinInterval(int val1, int val2){
        // make val1=max and val2=min
        if(val2 < val1){
            int temp = val1;
            val1 = val2;
            val2 = temp;
        }
        Random random = new Random();
        return random.nextInt(val2 - val1 + 1) + val1;
    }

    @Override
    public boolean isBusy(){
        return busy;
    }
}
