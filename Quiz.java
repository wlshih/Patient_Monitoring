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
			String line;
			while((line = file.readLine()) != null) {
				if(line.equals("")) continue;

				monitor_period = Integer.parseInt(line);
				if(monitor_period <= 0) System.out.println("Input Error");
				else break;
			}

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
				else if(line.split(" ").length == 5) {
					String category = line.split(" ")[0];
					String name = line.split(" ")[1];
					String dataset_file = line.split(" ")[2];
					double lower_bound = Double.parseDouble(line.split(" ")[3]);
					double upper_bound = Double.parseDouble(line.split(" ")[4]);

					if(lower_bound > upper_bound || p == null) {
						System.out.println("Input Error");
						continue;
					}

					Device d = new Device(category, name, dataset_file, lower_bound, upper_bound);
					p.addDevice(d);
				}
				else {
					System.out.println("Input Error");
					continue;
				}
			}
			file.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	static void displayFactorDatabase() {
		for(Patient p : patient_list) {
			System.out.printf("patient %s\n", p.getName());
			for(Device d : p.getDevices()) {
				System.out.printf("%s %s\n", d.getCategory(), d.getName());
				for(int i=0, t=0; t <= monitor_period; t += p.getPeriod(), i++) {
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

		displayFactorDatabase();

	}
}

class MonitorComparator implements Comparator<Patient> {
	public int compare(Patient a, Patient b) {
		if(a.getNextMeasure() == b.getNextMeasure()) return a.getId() - b.getId();
		return a.getNextMeasure() - b.getNextMeasure();
	}
}

class Patient {
	private int id;
	private String name;
	private int period;
	private int nextMeasure;
	private ArrayList<Device> device;

	public Patient() {}
	public Patient(int id, String name, int period) {
		this.id = id;
		this.name = name;
		this.period = period;
		this.nextMeasure = 0;
		this.device = new ArrayList<>();
	}

	public int getId() { return this.id; }
	public String getName() { return this.name; }
	public int getPeriod() { return this.period; }
	public int getNextMeasure() { return this.nextMeasure; }
	public void addDevice(Device d) { this.device.add(d); }
	public ArrayList<Device> getDevices() { return this.device; }

	public boolean timeExpired(int timestamp) {
		// System.out.print(timestamp);
		if(timestamp == this.nextMeasure) return true;
		else return false;
	}

	public void measure(int timestamp) {
		// System.out.println(name);
		nextMeasure += period;
		for(Device d : this.device) {
			double val = d.getData();
			if(d.alarm()) {
				if(val == -1) 
					System.out.printf("[%d] %s falls\n", timestamp, d.getName());
				else
					System.out.printf("[%d] %s is in danger! Cause: %s %s\n", timestamp, this.name, d.getName(), Double.toString(val));
			}
			d.data_count++;
		}
	}
}

class Device {
	private String category;
	private String name;
	private String factor_dataset_file;
	private double safe_range_lower_bound;
	private double safe_range_upper_bound;
	public ArrayList<Double> data;
	public int data_count;

	public Device() {}
	public Device(String category, String name, String file, double lower, double upper) {
		this.category = category;
		this.name = name;
		this.factor_dataset_file = file;
		this.safe_range_lower_bound = lower;
		this.safe_range_upper_bound = upper;
		this.data = new ArrayList<>();
		readData(file);
		this.data_count = 0;
	}

	public String getCategory() { return this.category; }
	public String getName() { return this.name; }

	public void readData(String filename) {
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

	public double getData() {
		if(this.data_count >= data.size()) return -1;
		else return data.get(this.data_count);
	}

	public boolean alarm() {
		double val = getData();
		// System.out.println(val);
		if(val == -1 || safe_range_lower_bound > val || safe_range_upper_bound < val) return true;
		else return false;
	}
}
