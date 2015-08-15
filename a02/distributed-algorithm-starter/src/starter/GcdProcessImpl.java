package starter;

import gcd.Coordinator.Coordinator;
import monitor.Monitor;
import gcd.Starter.GcdProcess;
import gcd.Starter.GcdProcessPOA;
import gcd.Starter.Starter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import org.omg.PortableServer.POA;


public class GcdProcessImpl extends GcdProcessPOA implements Runnable {

    private String                       id;
    private volatile boolean             running               = true;
    private LinkedBlockingQueue<Message> todos                 = new LinkedBlockingQueue<Message>();

    private GcdProcess                   process;

    private int                          currentMi             = -1;
    private int                          currentSequenceNumber = -1;
    private int                          delay                 = -1;
    private boolean                      markerFromRight       = false;
    private boolean                      markerFromLeft        = false;
    private boolean                      terminate             = false;

    private GcdProcess                   rightNeighbour;
    private GcdProcess                   leftNeighbour;

    private Starter                      starter;
    private Coordinator                  coordinator;
    private POA                          rootPoa;

    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private Monitor monitor;

    public GcdProcessImpl(Starter starter, Coordinator coordinator, String id, POA rootPoa){
        this.starter = starter;
        this.coordinator = coordinator;
        this.id = id;
        this.rootPoa = rootPoa;
    }

    @Override
    public void initGcdProcess(GcdProcess rightNeighbour, GcdProcess leftNeighbour, int startWert, int delay, Monitor monitor){
        this.currentMi = startWert;
        this.currentSequenceNumber = -1;
        this.delay = delay;
        this.markerFromRight = false;
        this.markerFromLeft = false;
        this.terminate = false;

        this.rightNeighbour = rightNeighbour;
        this.leftNeighbour = leftNeighbour;
        this.monitor = monitor;
        System.out.println("[initGcdProcess(" + this.id + ") -> startValue(" + startWert + "), delay(" + delay + "), delay(" + delay + "]");
    }

    @Override
    public void run(){
        System.out.println("started gcd-process - id: " + this.id);

        while(running){
            if(!todos.isEmpty()){
                Message msg = todos.poll();

                //System.out.println("{" + this.id + "} processing -> " + msg);

                switch(msg.getType()){
                    case Message.TYPE_CALC:
                        //System.out.println("{" + this.id + "} Message.TYPE_CALC");
                        if (this.running) {
                            calculate(msg.getSender(), msg.getValue());
                        }
                        break;
                    case Message.TYPE_TERMINATE:
                        //System.out.println("{" + this.id + "} Message.TYPE_TERMINATE");
                        if (this.running) {
                            terminate(msg.getSender(), msg.getValue());
                        }
                        break;
                }
            }
        }

        starter.processFinished();
        System.out.println("stopped gcd-process - id: " + this.id);
    }

    private void terminate(String sender, int sequenceNumber){
        Date date = new Date();
        if (this.running) {
            this.monitor.terminieren(this.id, sender, this.terminate);
            System.out.println(date.getTime() + ": {" + sender + " >> " + this.id + "} -> T(" + sequenceNumber + ") => " + this.terminate);
        }
        if(sequenceNumber > this.currentSequenceNumber){
            this.markerFromRight = false;
            this.markerFromLeft = false;
            this.terminate = true;
            this.currentSequenceNumber = sequenceNumber;
            this.leftNeighbour.terminateGcdCalculation(this.id, sequenceNumber);
            this.rightNeighbour.terminateGcdCalculation(this.id, sequenceNumber);
        }

        if(sender.equals(this.rightNeighbour.getName())){
            this.markerFromRight = true;
        }

        if(sender.equals(this.leftNeighbour.getName())){
            this.markerFromLeft = true;
        }

        if(this.markerFromRight && this.markerFromLeft){
            //System.out.println("   > " + this.id + " TRYING to complete on TERMINATE(" + sequenceNumber + ")...");
            this.coordinator.calculationComplete(this.terminate, this.currentMi, this.id, this.currentSequenceNumber);
            //System.out.println("   > " + this.id + " ...complete on TERMINATE(" + sequenceNumber + ")");
        } else{
            //System.out.println("   > " + this.id + " not finished yet on TERMINATE(" + sequenceNumber + ")");
        }
    }

    private void calculate(String sender, int y){
        Date date = new Date();

        if (this.running) {
            this.monitor.rechnen(this.id, sender, y);
            System.out.println(date.getTime() + ": {" + sender + " >> " + this.id + "} -> " + y);
        }

        if(y < this.currentMi){

            try{
                Thread.sleep(this.delay);
            } catch(InterruptedException e){
                e.printStackTrace();
            }

            int newMi = ((this.currentMi - 1) % y) + 1;
            //System.out.println("     > " + this.id + " {y: " + y + ", Mi: " + this.currentMi + ", newMi: " + newMi + "}");

            if(newMi != this.currentMi){
                this.currentMi = newMi;
                this.terminate = false;
            }

            if (this.running) {
                this.leftNeighbour.calculateMsg(this.id, this.currentMi);
                this.rightNeighbour.calculateMsg(this.id, this.currentMi);
            }

            // System.out.println("   > " + this.id + " processed CALC(" + y + ")");
        } else{
            // System.out.println("   > " + this.id + " did nothing on CALC(" + y + ")");
        }
    }

    @Override
    public String getName(){
        return this.id;
    }

    @Override
    public int getNumber(){
        return this.currentMi;
    }

    @Override
    public void startGcdAlgorithm(){
        if (this.running) {
            this.leftNeighbour.calculateMsg(this.id, this.currentMi);
            this.rightNeighbour.calculateMsg(this.id, this.currentMi);
        }
    }

    @Override
    public void calculateMsg(String sender, int value){
        if(sender == null || sender.isEmpty()){
            sender = "NULL";
        }
        // System.out.println(" > " + this.id + " recieved CALC(" + value + ") from " + sender);
        this.todos.add(new Message(sender, Message.TYPE_CALC, value));
    }

    @Override
    public void terminateGcdCalculation(String sender, int value){
        if(sender == null || sender.isEmpty()){
            sender = "NULL";
        }
        // System.out.println(" > " + this.id + " recieved TERMINATE(" + value + ") from " + sender);
        this.todos.add(new Message(sender, Message.TYPE_TERMINATE, value));
    }

    public void shutDown(){
        this.running = false;
        System.out.println("running = false - id: " + this.id);
    }

    public GcdProcess getProcess(){
        return process;
    }

    public void setProcess(GcdProcess process){
        this.process = process;
    }
}
