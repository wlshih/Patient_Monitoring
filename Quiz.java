import java.util.*;
import java.io.*;

public class Quiz {
	static int monitor_period = 0;
	static List<Patient> patient_list;
	static PriorityQueue<Patient> patient_queue;
	
	static void readFile(String filename) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(filename));
			String line = file.readLine();
			monitor_period = Integer.parseInt(line);

			int i = 0;
			Patient p = null;
			while((line = file.readLine()) != null) {
				// patient
				if(line.split(" ").length == 3) {
					// System.out.println(line);
					String name = line.split(" ")[1];
					int period = Integer.parseInt(line.split(" ")[2]);

					// error input
					if(period <= 0){
						System.out.println("Input Error");
						continue;
					}
					p = new Patient(i++, name, period);
					patient_list.add(p);
				}
				// device
				else {
					String category = line.split(" ")[0];
					String name = line.split(" ")[1];
					String dataset_file = line.split(" ")[2];
					double lower_bound = Double.parseDouble(line.split(" ")[3]);
					double upper_bound = Double.parseDouble(line.split(" ")[4]);
					Device d = new Device(category, name, dataset_file, lower_bound, upper_bound);
					p.device.add(d);
				}
			}
			file.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	static void printPatient() {
		for(Patient p : patient_list) {
			System.out.printf("patient %s\n", p.name);
			for(Device d : p.device) {
				System.out.printf("%s %s\n", d.category, d.name);
				for(int i=0, t=0; t <= monitor_period; t += p.period, i++) {
					double val = (i < d.data.size()) ? d.data.get(i) : -1;
					System.out.printf("[%d] %s\n", t, Double.toString(val));
				}
			}
		}
	}

	public static void main(String[] args) {
		patient_list = new ArrayList<>();
		patient_queue = new PriorityQueue<>(11, new MonitorComparator());

		readFile(args[0]);
		
		patient_queue.addAll(patient_list);

		int timestamp = 0;
		while(timestamp <= monitor_period) {
			while(patient_queue.peek().timeExpired(timestamp)) { // check through every patient mesuring at the same time
				Patient p = patient_queue.poll();
				p.measure(timestamp);
				patient_queue.add(p);
			}
			timestamp++;
		}

		printPatient();

	}
}

class MonitorComparator implements Comparator<Patient> {
	public int compare(Patient a, Patient b) {
		if(a.next_mesure == b.next_mesure) return a.id - b.id;
		return a.next_mesure - b.next_mesure;
	}
}

class Patient {
	int id;
	String name;
	int period;
	int next_mesure;
	ArrayList<Device> device;

	Patient() {}
	Patient(int id, String name, int period) {
		this.id = id;
		this.name = name;
		this.period = period;
		this.next_mesure = 0;
		this.device = new ArrayList<>();
	}

	boolean timeExpired(int timestamp) {
		// System.out.print(timestamp);
		if(timestamp == this.next_mesure) return true;
		else return false;
	}

	void measure(int timestamp) {
		// System.out.println(name);
		next_mesure += period;
		for(Device d : this.device) {
			double val = d.getData();
			if(d.alarm()) {
				if(val == -1) 
					System.out.printf("[%d] %s falls\n", timestamp, d.name);
				else
					System.out.printf("[%d] %s is in danger! Cause: %s %s\n", timestamp, this.name, d.name, Double.toString(val));
			}
			d.count++;
		}
	}
}

class Device {
	String category;
	String name;
	String factor_dataset_file;
	double safe_range_lower_bound;
	double safe_range_upper_bound;
	ArrayList<Double> data;
	int count;

	Device() {}
	Device(String category, String name, String file, double lower, double upper) {
		this.category = category;
		this.name = name;
		this.factor_dataset_file = file;
		this.safe_range_lower_bound = lower;
		this.safe_range_upper_bound = upper;
		this.data = new ArrayList<>();
		readData(file);
		this.count = 0;
	}


	void readData(String filename) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(filename));
			String line = file.readLine();
			while(line != null) {
				data.add(Double.parseDouble(line));
				line = file.readLine();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	double getData() {
		if(this.count >= data.size()) return -1;
		else return data.get(this.count);
	}

	boolean alarm() {
		double val = getData();
		// System.out.println(val);
		if(val == -1 || safe_range_lower_bound > val || safe_range_upper_bound < val) return true;
		else return false;
	}
}
