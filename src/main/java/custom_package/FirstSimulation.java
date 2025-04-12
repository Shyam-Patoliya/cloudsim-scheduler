package main.java.custom_package;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;

public class FirstSimulation {

    public static void main(String[] args) {
        CloudSim.init(1, Calendar.getInstance(), false);

        Datacenter datacenter = createDatacenter("Datacenter_0");

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        int brokerId = broker.getId();

        // Create two VMs with different schedulers
        Vm timeSharedVm = new Vm(0, brokerId, 1000, 1, 512, 1000, 10000, "Xen",
                new CloudletSchedulerTimeShared());

        Vm spaceSharedVm = new Vm(1, brokerId, 1000, 1, 512, 1000, 10000, "Xen",
                new CloudletSchedulerSpaceShared());

        List<Vm> vmList = new ArrayList<>();
        vmList.add(timeSharedVm);
        vmList.add(spaceSharedVm);

        broker.submitVmList(vmList);

        // Create identical cloudlets for both VMs
        List<Cloudlet> cloudletList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Cloudlet cloudlet = new Cloudlet(
                    i, // ID
                    40000, // length
                    1, // number of PEs
                    300, // file size
                    300, // output size
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull()
            );
            cloudlet.setUserId(brokerId);

            // Alternate assignment: 0, 1, 0, 1
            cloudlet.setVmId(i % 2);
            cloudletList.add(cloudlet);
        }

        broker.submitCloudletList(cloudletList);

        CloudSim.startSimulation();
        List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
        CloudSim.stopSimulation();

        printResults(finishedCloudlets);
    }

    private static Datacenter createDatacenter(String name) {
        List<Pe> peList = List.of(new Pe(0, new PeProvisionerSimple(1000)));

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
        System.out.printf("%-12s%-12s%-12s%-12s%-12s\n", "CloudletID", "STATUS", "VM_ID", "StartTime", "FinishTime");

        for (Cloudlet cloudlet : list) {
            if (cloudlet.getStatus() == Cloudlet.SUCCESS) {
                System.out.printf("%-12d%-12s%-12d%-12.2f%-12.2f\n",
                        cloudlet.getCloudletId(),
                        "SUCCESS",
                        cloudlet.getVmId(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime());
            }
        }
    }
}
