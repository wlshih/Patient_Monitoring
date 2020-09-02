import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Quiz {

	public static int monitor_period;

	// read data from input file and store into patient_ordered_list
	static List<Patient> readInput(String filename) {

		List<Patient> patient_ordered_list = new ArrayList<>();

		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(filename));
			String line = file.readLine();
			monitor_period = Integer.parseInt(line);

			int i = 0;
			while((line = file.readLine()) != null) {
				// patient
				String patient_name = line.split(" ")[1];
				int period = Integer.parseInt(line.split(" ")[2]);
				Patient p = new Patient(i, patient_name, period);
				// System.out.println(p.name);
				// System.out.println(p.period);
				
				// device attached to the patient
				line = file.readLine();
				String category = line.split(" ")[0];
				String device_name = line.split(" ")[1];
				String dataset_file = line.split(" ")[2];
				int lower_bound = Integer.parseInt(line.split(" ")[3]);
				int upper_bound = Integer.parseInt(line.split(" ")[4]);
				Device d = new Device(category, device_name, dataset_file, lower_bound, upper_bound);
				p.attach(d);
				// System.out.println(d.category);
				// System.out.println(d.name);
				// System.out.println(d.factor_dataset_file);
				// System.out.println(d.safe_range_lower_bound);
				// System.out.println(d.safe_range_upper_bound);

				patient_ordered_list.add(p);
				i++;
			}

			file.close();

		}
		catch(IOException e) {
			e.printStackTrace();
		}

		return patient_ordered_list;
	}

	public static void main(String[] args) {

		List<Patient> patient_ordered_list = readInput(args[0]);

		// store patient_ordered_list into patient_list which is a pq
		// for selecting the next patient
		PriorityQueue<Patient> patient_list = new PriorityQueue<>(11, new MonitorComparator());
		patient_list.addAll(patient_ordered_list);

		int timestamp = 0;
		while(timestamp <= monitor_period) {
			// check the next mesure period of patients
			while(patient_list.peek().timeExpired(timestamp)) {
				Patient temp = patient_list.poll();
				temp.mesure(timestamp);
				patient_list.add(temp);
			}

			timestamp ++;
		}

		// timestamp reach monitor period
		// system finish monitoring
		// show factorDatabase contents

		// ugly code

		for(Patient p : patient_ordered_list) {
			System.out.printf("patient %s\n", p.name);
			System.out.printf("%s %s\n", p.device.category, p.device.name);
			for(int t = 0, i = 0; t <= monitor_period; t += p.period, i++) {
				int val;
				if(i < p.device.dataset.size())
					val = p.device.dataset.get(i);
				else
					val = -1;
				System.out.printf("[%d] %d\n", p.period, val);
			}

		}
	}
}



// implements of comparator in the priority queue
class MonitorComparator implements Comparator<Patient> {
	public int compare(Patient p1, Patient p2) {
		if(p1.next_mesure == p2.next_mesure) return (p1.id - p2.id);
		else return (p1.next_mesure - p2.next_mesure);
	}
}



class Patient {
	int id;
	String name;
	int period;
	int next_mesure;
	Device device;

	// class constructor
	Patient(int id, String name, int period) {
		this.id = id;
		this.name = name;
		this.period = period;
		this.next_mesure = 0;
	}
	void attach(Device device) {
		this.device = device;
	}

	boolean timeExpired(int timestamp) {
		return ((timestamp - next_mesure) == 0);
	}
	void mesure(int timestamp) {
		next_mesure = timestamp + period;
		if(device.alarm())
			if(device.currentValue() == -1)
				System.out.printf("[%d] %s falls\n", timestamp, device.name);
			else
				System.out.printf("[%d] %s is in danger! Cause: %s %.1f\n", timestamp, name, device.name, (float)device.currentValue());

		device.count++;

	}
}

class Device {
	String category;
	String name;
	String factor_dataset_file;
	int safe_range_lower_bound;
	int safe_range_upper_bound;
	List<Integer> dataset;
	int count; // for counting in dataset

	// class constructor
	Device(String category, String name, String dataset_file, int lower_bound, int upper_bound) {
		this.category = category;
		this.name = name;
		this.factor_dataset_file = dataset_file;
		this.safe_range_lower_bound = lower_bound;
		this.safe_range_upper_bound = upper_bound;
		this.dataset = new ArrayList<>();
		readData(dataset_file);
		this.count = 0;
	}

	// read from dataset file
	void readData(String filename) {
		BufferedReader file;
		try {
			file = new BufferedReader(new FileReader(filename));
			String line = file.readLine();
			// System.out.println("*");

			while(line != null) {
				dataset.add(Integer.parseInt(line));
				line = file.readLine();
			}

			file.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	// return true if data is out of bounds
	boolean alarm() {
		int val = currentValue();
		if(val == -1) return true;
		else if(val < safe_range_lower_bound) return true;
		else if(val > safe_range_upper_bound) return true;
		else return false;
	}

	int currentValue() {
		if(count < dataset.size())
			return dataset.get(count);
		else return -1;
	}

}

