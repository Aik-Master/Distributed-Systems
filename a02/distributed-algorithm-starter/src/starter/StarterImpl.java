package starter;

import gcd.Coordinator.*;
import gcd.Starter.*;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class StarterImpl extends StarterPOA {

    private String id;
    private List<GcdProcessImpl> processList = new ArrayList<GcdProcessImpl>();
    private List<Thread> threadList = new ArrayList<Thread>();
    private Semaphore processReturn = new Semaphore(100, true);

    private Starter starter;

    private ORB orb;
    private Coordinator coordinator;
    private POA rootPoa;

    private final CountDownLatch cdlShutDown;
    private CountDownLatch cdlProcesses;

    public StarterImpl(String id, Coordinator coordinator, ORB orb, POA rootPoa, CountDownLatch cdlShutDown) {
        this.id = id;
        this.coordinator = coordinator;
        this.orb = orb;
        this.rootPoa = rootPoa;
        this.cdlShutDown = cdlShutDown;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void startGcdProcesses(int anzahl) {
        anzahl = anzahl + this.processList.size();
        try {
            for (int i = this.processList.size(); i < anzahl; i++) {

                StringBuilder pid = new StringBuilder().append(getId()).append("_p").append(i);

                GcdProcessImpl gcdProcessImpl = new GcdProcessImpl(this.starter, this.coordinator, pid.toString(), this.rootPoa);
                this.processList.add(gcdProcessImpl);

                GcdProcess process = GcdProcessHelper.narrow(this.rootPoa.servant_to_reference(gcdProcessImpl));
                gcdProcessImpl.setProcess(process);

                Thread thread = new Thread(gcdProcessImpl);
                this.threadList.add(thread);
                thread.start();

                this.coordinator.registerGcdProcess(process);
            }
        } catch (ServantNotActive servantNotActive) {
            servantNotActive.printStackTrace();
        } catch (WrongPolicy wrongPolicy) {
            wrongPolicy.printStackTrace();
        }
    }

    @Override
    public void endGcdProcesses() {
        System.out.println("[shutting down my '"+ this.processList.size() +"' processes...]");

        this.cdlProcesses = new CountDownLatch(this.processList.size());
        for (GcdProcessImpl p : this.processList) {
            p.shutDown();
            try {
                processReturn.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            cdlProcesses.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[shutting down my '"+ this.threadList.size() +"' threads...]");

        for (Thread t : this.threadList) {
            try {
                System.out.println("joining -> T: " + t.getName() + "...");
                t.join();
                System.out.println("...left -> T: " + t.getName() + " !");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("[...done!]");

        this.processList.clear();
        this.threadList.clear();
    }

    @Override
    public void processFinished() {
        this.cdlProcesses.countDown();
    }

    @Override
    public void shutDown() {
        this.endGcdProcesses();
        cdlShutDown.countDown();
        System.out.println("\nshutting down starter(" + id + ")...");
    }

    public Starter getStarter() {
        return starter;
    }

    public void setStarter(Starter starter) {
        this.starter = starter;
    }
}
