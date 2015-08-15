package client;

import gcd.Coordinator.Coordinator;
import gcd.Coordinator.CoordinatorHelper;
import monitor.Monitor;
import monitor.MonitorHelper;
import gcd.Starter.Starter;

import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

public class Main {

    private static final int CALCULATION_ARGUMENTS_COUNT = 4;

    private static final int COUNT = 0;
    private static final int DELAY = 1;
    private static final int TERMINATE = 2;
    private static final int GCD = 3;

    public static void main(String[] args) {

        boolean running = true;
        boolean innerRunning = true;

        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBInitialPort", args[0]);
        props.put("org.omg.CORBA.ORBInitialHost", args[1]);

        String coodinatorName = args[2];
        String monitorName = args[3];
        try{
            Scanner scanner = new Scanner(System.in);
            ORB orb = ORB.init(args, props);
            POA rootPoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPoa.the_POAManager().activate();

            NamingContextExt ns = NamingContextExtHelper.narrow(orb.resolve_initial_references("NameService"));

            Coordinator coordinator = CoordinatorHelper.narrow(ns.resolve_str(coodinatorName));
            Monitor monitor = MonitorHelper.narrow(ns.resolve_str(monitorName));
            System.out.println("Client connected to coordinator: " + coodinatorName);
            System.out.println("Client connected to monitor: " + monitorName);

            running = true;
            while (running) {
                System.out.println("\n----\nType in '[l]ist' to list all startes");
                System.out.println("Type in '[c]alc' to start a calculation");
                System.out.println("Type in '[t]erminate' to shut down the system");
                System.out.println("Type in '[e]xit' to end the client");
                System.out.print(">");
                String input = scanner.next();

                switch(input) {
                    case "l":
                    case "list":
                        System.out.println("\nKnown starters:");
                        for (Starter starter : coordinator.getAllStarters()) {
                            System.out.println(starter.getId());
                        }
                        break;
                    case "t":
                    case "terminate":
                        running = false;
                        coordinator.shutDown();
                        System.out.println("Shutting down the system...");
                        continue;
                    case "e":
                    case "exit":
                        running = false;
                        System.out.println("Leaving client WITHOUT shutting down the system...");
                        continue;
                    case "c":
                    case "calc":
                        if(coordinator.isBusy()){
                            System.out.println("\nThe coordinator is 'busy' please try again later...");
                            continue;
                        } else {
                            innerRunning = true;
                            while (innerRunning) {
                                System.out.println("\n----\nType in the calculation arguments in the right order, seperated by \" \" or 'exit' to end calc-mode:");
                                System.out.println("Arguents:\n [process-count-interval]\n [process-delay-interval-millis]\n [termination-check-time-millis]\n [desired-gcd]");
                                System.out.println("Example: [d]ev  -> 3-3 1000-3000 2000 12");
                                System.out.println("Example: [f]ast -> 3-3 50-100 75 3");
                                System.out.println("Example: [s]low -> 5-10 5000-10000 7500 37");
                                System.out.println("Example: [h]tm -> 3-3 1000-1000 2000 42");
                                System.out.print(">");
                                input = scanner.nextLine();

                                if (input.equals("exit")) {
                                    innerRunning = false;
                                    continue;
                                } else {
                                    if (input.equals("dev") || input.equals("d")) {
                                        input = "3-3 1000-3000 2000 12";
                                    }
                                    if (input.equals("fast") || input.equals("f")) {
                                        input = "3-3 50-100 75 3";
                                    }
                                    if (input.equals("slow") || input.equals("s")) {
                                        input = "5-10 5000-10000 7500 37";
                                    }
                                    if (input.equals("htm") || input.equals("h")) {
                                        input = "3-3 1000-1000 2000 42";
                                    }
                                    String[] calcArgs = input.split(" ");
                                    if (calcArgs.length == CALCULATION_ARGUMENTS_COUNT) {
                                        System.out.println("\n----\n-> process-count-interval: " + calcArgs[COUNT]);
                                        System.out.println("-> process-delay-interval-millis: " + calcArgs[DELAY]);
                                        System.out.println("-> termination-check-time-millis: " + calcArgs[TERMINATE]);
                                        System.out.println("-> desired-gcd: " + calcArgs[GCD]);

                                        Calculation calculation = Calculation.parse(calcArgs[COUNT], calcArgs[DELAY], calcArgs[TERMINATE], calcArgs[GCD]);
                                        if (Calculation.validate(calculation)) {

                                            try {
                                                coordinator.calculateGcd(calculation.getProcessCountIntervalMin(),
                                                        calculation.getProcessCountIntervalMax(),
                                                        calculation.getProcessDelayIntervalMin(),
                                                        calculation.getProcessDelayIntervalMax(),
                                                        calculation.getTerminationCheckTime(),
                                                        calculation.getDesiredGcd());

                                                System.out.println("\nStarted calculation with following arguments:");
                                                System.out.println("  calculation.getProcessCountIntervalMin() -> " + calculation.getProcessCountIntervalMin());
                                                System.out.println("  calculation.getProcessCountIntervalMax() -> " + calculation.getProcessCountIntervalMax());
                                                System.out.println("  calculation.getProcessDelayIntervalMin() -> " + calculation.getProcessDelayIntervalMin());
                                                System.out.println("  calculation.getProcessDelayIntervalMax() -> " + calculation.getProcessDelayIntervalMax());
                                                System.out.println("  calculation.getTerminationCheckTime() -> " + calculation.getTerminationCheckTime());
                                                System.out.println("  calculation.getDesiredGcd() -> " + calculation.getDesiredGcd());

                                            } catch (Exception e) {
                                                System.out.println("\nCouldn't start calculation...");
                                                innerRunning = false;
                                            }

                                            innerRunning = false;
                                        } else {
                                            System.out.print("\nAn unknown error occured!");
                                        }
                                        continue;
                                    } else {
                                        System.out.println("\nERR: Your arguments are invalid! -> " + Arrays.toString(calcArgs));
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        System.out.println("\nUnknown command...");
                        break;
                }
            }
            /*
            CountDownLatch cdl = new CountDownLatch(1);


            Coordinator coordinator = CoordinatorHelper.narrow(ns.resolve_str(coordinatorName));

            StarterImpl starterImpl = new StarterImpl(id, coordinator, orb, rootPoa, cdl);
            org.omg.CORBA.Object ref = rootPoa.servant_to_reference(starterImpl);
            Starter starter = StarterHelper.narrow(ref);
            starterImpl.setStarter(starter);

            NameComponent path[] = ns.to_name(id);
            ns.rebind(path, starter);

            coordinator.registerStarter(starter);

            System.out.println("starter(" + id + ") is registered to coordinator(" + coordinatorName + ")");

            cdl.await();
            orb.shutdown(true);
            Thread.sleep(500);

            System.out.println("starter(" + id + ") TEST");
            */

        } catch(InvalidName e){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        } catch(AdapterInactive e){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        } catch(org.omg.CosNaming.NamingContextPackage.InvalidName e){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        } catch(NotFound e){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        } catch(CannotProceed e){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        } catch(NullPointerException e){
            e.printStackTrace();
        }
        System.out.println("----\nBYE!");
    }
}
