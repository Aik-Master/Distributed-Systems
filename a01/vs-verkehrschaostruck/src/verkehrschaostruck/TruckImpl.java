package verkehrschaostruck;

import java.util.concurrent.CountDownLatch;

import verkehrschaos.Truck;
import verkehrschaos.TruckCompany;
import verkehrschaos.TruckPOA;

public class TruckImpl extends TruckPOA{

    private String         name;
    private TruckCompany   truckCompany;
    private Truck          corbaTruck;
    private CountDownLatch cdl;

    public TruckImpl(String name, CountDownLatch cdl){
        this.name = name;
        this.cdl = cdl;
    }

    public void setCorbaTruck(Truck corbaTruck){
        this.corbaTruck = corbaTruck;
    }

    // //////////////////////////////////////////////////////////////////////////////////////
    // from now on only inherited Methods
    // //////////////////////////////////////////////////////////////////////////////////////
    @Override
    public String getName(){
        return name;
    }

    @Override
    public TruckCompany getCompany(){
        return truckCompany;
    }

    @Override
    public void setCompany(TruckCompany company){
        truckCompany = company;
        System.out.println(company.getName() + " verwaltet jetzt den Truck");
    }

    @Override
    public void setCoordinate(double x, double y){
        System.out.println("Der Truck ist jetzt hier:" + x + " " + y);
    }

    @Override
    public void putOutOfService(){
        truckCompany.removeTruck(corbaTruck);
        cdl.countDown();
        System.out.println("Truck(" + name + ") hat sich bei der Spedition(" + truckCompany.getName() + ") abgemeldet");
    }
}
