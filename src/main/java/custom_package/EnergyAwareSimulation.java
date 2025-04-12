package main.java.custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

public class EnergyAwareSimulation {

    static Map<Integer, Double> vmPowerMap = new HashMap<>();

    public static void main(String[] args) {

        int numUsers = 1;
        Calendar calendar = Calendar.getInstance();
        CloudSim.init(numUsers, calendar, false);

        Datacenter datacenter = createDatacenter("Datacenter_0");

        EnergyAwareBroker broker;
        try {
            broker = new EnergyAwareBroker("EnergyBroker");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int brokerId = broker.getId();

        List<Vm> vmList = new ArrayList<>();

        // Define 3 VMs with different power ratings
        Vm vm0 = new Vm(0, brokerId, 500, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
        Vm vm1 = new Vm(1, brokerId, 1000, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
        Vm vm2 = new Vm(2, brokerId, 2000, 1, 512, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());

        // Register energy usage per VM
        vmPowerMap.put(0, 2.0); // watts/sec
        vmPowerMap.put(1, 3.0);
        vmPowerMap.put(2, 5.0);

        vmList.add(vm0);
        vmList.add(vm1);
        vmList.add(vm2);

        broker.submitVmList(vmList);

        List<Cloudlet> cloudletList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Cloudlet cloudlet = new Cloudlet(
                    i,
                    10000, // length
                    1,
                    300,
                    300,
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull()
            );
            cloudlet.setUserId(brokerId);
            cloudletList.add(cloudlet);
        }

        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();

        List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();

        CloudSim.stopSimulation();

        printResults(finishedCloudlets);
    }

    private static Datacenter createDatacenter(String name) {
        List<Pe> peList = List.of(new Pe(0, new PeProvisionerSimple(4000)));

        Host host = new Host(0,
                new RamProvisionerSimple(2048),
                new BwProvisionerSimple(10000),
                1000000,
                peList,
                new VmSchedulerTimeShared(peList));

        List<Host> hostList = List.of(host);

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList,
                10.0, 3.0, 0.05, 0.001, 0.0);

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void printResults(List<Cloudlet> list) {
        System.out.println("\n=========== RESULTS ===========");
        System.out.printf("%-12s%-12s%-12s%-12s%-12s%-12s\n", "CloudletID", "STATUS", "VM_ID", "StartTime", "FinishTime", "Energy");

        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                int vmId = cloudlet.getVmId();
                double execTime = cloudlet.getActualCPUTime();
                double energy = execTime * vmPowerMap.get(vmId);

                System.out.printf("%-12d%-12s%-12d%-12.2f%-12.2f%-12.2f\n",
                        cloudlet.getCloudletId(),
                        "SUCCESS",
                        vmId,
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        energy);
            }
        }
    }
}