package main.java.custom_package;

import org.cloudbus.cloudsim.*;

import java.util.*;

public class EnergyAwareBroker extends DatacenterBroker {

    public EnergyAwareBroker(String name) throws Exception {
        super(name);
    }

    Map<Integer, Integer> vmLoad = new HashMap<>();

    @Override
    protected void submitCloudlets() {
        for (Cloudlet cloudlet : getCloudletList()) {
            Vm bestVm = null;
            double minEnergy = Double.MAX_VALUE;

            for (Vm vm : getVmsCreatedList()) {
                int load = vmLoad.getOrDefault(vm.getId(), 0);
                if (load >= 2) continue; // Cap at 2 cloudlets per VM

                double execTime = (double) cloudlet.getCloudletLength() / vm.getMips();
                double energy = execTime * EnergyAwareSimulation.vmPowerMap.get(vm.getId());

                if (energy < minEnergy) {
                    minEnergy = energy;
                    bestVm = vm;
                }
            }

            if (bestVm != null) {
                cloudlet.setVmId(bestVm.getId());
                vmLoad.put(bestVm.getId(), vmLoad.getOrDefault(bestVm.getId(), 0) + 1);
                System.out.println("Sending cloudlet " + cloudlet.getCloudletId() + " to VM #" + bestVm.getId());
            }
        }

        super.submitCloudlets();
    }


}
