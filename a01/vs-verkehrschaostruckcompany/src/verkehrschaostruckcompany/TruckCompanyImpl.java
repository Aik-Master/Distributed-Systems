package verkehrschaostruckcompany;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import verkehrschaos.TTruckListHolder;
import verkehrschaos.Truck;
import verkehrschaos.TruckCompany;
import verkehrschaos.TruckCompanyPOA;

public class TruckCompanyImpl extends TruckCompanyPOA{

    private String           name;
    private ArrayList<Truck> availableTruckList;
    private ArrayList<Truck> incomingTruckList;
    private TruckCompany     corbaTruckCompany;
    private CountDownLatch   cdl;

    public TruckCompanyImpl(String name, CountDownLatch cdl){
        this.name = name;
        this.cdl = cdl;
        availableTruckList = new ArrayList<Truck>();
        incomingTruckList = new ArrayList<Truck>();
    }

    public void setCorbaTruckComapny(TruckCompany corbaTruckCompany){
        this.corbaTruckCompany = corbaTruckCompany;
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    // from now on only inherited Methods
    // //////////////////////////////////////////////////////////////////////////////////////
    
    @Override
    public String getName(){
        return name;
    }

    @Override
    public void addTruck(Truck truck){
        availableTruckList.add(truck);
        truck.setCompany(corbaTruckCompany);
    }

    @Override
    public void removeTruck(Truck truck){
        availableTruckList.remove(truck);
    }

    @Override
    public int getTrucks(TTruckListHolder trucks) {
        trucks.value = availableTruckList.toArray(new Truck[availableTruckList.size()]);

        return availableTruckList.size();
    }

    @Override
    public void leave(Truck truck){
        System.out.println("Der Truck "+truck.getName()+" hat die Spedition verlassen.");
        availableTruckList.remove(truck);
    }

    @Override
    public void advise(Truck truck){
        incomingTruckList.add(truck);
        truck.setCompany(corbaTruckCompany);
        System.out.println("Der LKW " + truck.getName() + " ist auf dem Weg...");
    }

    @Override
    public void arrive(Truck truck){
        if (incomingTruckList.remove(truck)){
            availableTruckList.add(truck);
            System.out.println("Der Truck " + truck.getName()+ " ist angekommen");
        }else {
            System.out.println("Der Truck " + truck.getName()+ " war gar nicht mehr auf dem Weg hier her!");
        }
    }

    @Override
    public void putOutOfService(){
        for(int i = 0 ; i < availableTruckList.size() ; i++){
            availableTruckList.get(i).putOutOfService();
        }
        for(int i = 0 ; i < incomingTruckList.size() ; i++){
            incomingTruckList.get(i).putOutOfService();
        }

        cdl.countDown();
        System.out.println("Die Spedition " + name + " ist leider insolvent und musste ihren Standort aufgeben");
    }
}
