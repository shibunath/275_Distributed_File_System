package gash.router.server.tasks;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class CalCpuLoad {

/*	public static void main(String[] args) {
		System.out.println(checkCpuLoadOptions());
	}*/
	
	public static double checkCpuLoadOptions(){
		if(cpuLoad()!=0.0){
			return cpuLoad();
		} else {
			return cpuFromOperatingSystemBean();
		}
	}

	public static double cpuLoad() {
		System.loadLibrary("sigar-amd64-winnt");
		final int AVGSIZE = 5;
		CpuPerc cpuperc = null;
		double loadArray[] = new double[AVGSIZE];
		double sum = 0;
		try {
			Sigar sigar = new Sigar();
			for (int i = 0; i < AVGSIZE; i++) {
				cpuperc = sigar.getCpuPerc();
				// double load = cpuperc.getCombined() * 100;
				try {
					Thread.sleep(500);
				} catch (InterruptedException E) {
					System.out.println("Thread Interruption Error!" + E.getMessage());
				}
				loadArray[i] = cpuperc.getCombined();
				sum = sum + loadArray[i];
			}
			sum = sum / AVGSIZE;
			return sum;
		} catch (SigarException e) {
			System.out.println("Error in load Sigar Libary while calculating CPULoad" + e.getMessage());
		}
		return sum;
	}

	public static double cpuFromOperatingSystemBean() {
		double sum=0.0;
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
		for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
			method.setAccessible(true);
			if (method.getName().startsWith("get") && Modifier.isPublic(method.getModifiers())) {
				Object value;
				try {
					//System.out.println(" method " + method.getName());
					if (method.getName().equals("getProcessCpuTime"));
					value = method.invoke(operatingSystemMXBean);
				} catch (Exception e) {
					value = e;
					System.out.println("Exception in cpuLoad from Operating System" + value);

				}
				if (method.getName().equals("getSystemCpuLoad")) {
					sum=(double) value;
				}
			} 
		} 
		return sum;
	}

}
