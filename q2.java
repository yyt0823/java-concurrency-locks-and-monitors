import java.util.Random;
public class q2 {
    /*
    we want a monitor to store 
    shared state:
    1. number of empty plates
    2. number of full plates

    this way philosopher and robot both can see if the tray is full or empty and act accordingly
    if philosopher want to place an empty plate but sees it is full it goes to sleep and notify other threads
    if robot sees it is not full it goes to sleep and notify other threads
    similar with taking full plate

    robot state:
    1. accepting empty plates
    condition used by philosopher to place an empty plate, we want the philosophers to know if it is the turn to place or take
    2. at kitchen
    to let kitchen know if it need to refill the tray

    
    kitchen state:
    1. kitchen done
    to let robot know if it need to go back to cafe

    */

    public static class CafeMonitor {
        private final int k; 
        // shared state
        private int empty_on_tray;
        private int full_on_tray;

        // robot state
        private boolean accepting_empty = true;
        private boolean at_kitchen = false;

        // kitchen state
        private boolean kitchen_done = false;

        public CafeMonitor(int k) {
            if (k <= 0) {
                throw new IllegalArgumentException("k must be greater than 0");
            }
            this.k = k;
        }

        // philosopher
        public synchronized void place_empty(int pid) throws InterruptedException {
            while(!accepting_empty || empty_on_tray == k) {
                System.out.println("Philosopher " + pid + " want to place but robot is not accepting empty plates or tray is full");
                wait();
            }
            System.out.println("Philosopher " + pid + " placed an empty plate");
            empty_on_tray++;
            if (empty_on_tray == k) {
                System.out.println("philosopher " + pid + " placed the last empty plate, tray is full, notifying all");
                notifyAll();
            }
        }

        public synchronized void take_full(int pid) throws InterruptedException {
            while(full_on_tray == 0) {
                System.out.println("Philosopher " + pid + " want to take but robot is not at cafe or tray is empty");
                wait();
            }
            full_on_tray--;
            System.out.println("Philosopher " + pid + " took a full plate");
            if (full_on_tray == 0) {
                System.out.println("philosopher " + pid + " took the last full plate, tray is empty, notifying all");
                notifyAll();
            }
        }

        // robot
        public synchronized void robot_wait_tray_full() throws InterruptedException {
            while(empty_on_tray < k) {
                System.out.println("Robot is waiting for tray to be full");
                wait();
            }
            accepting_empty = false;
            System.out.println("tray is full of full plates");
            notifyAll();
        }

        public synchronized void robot_arrive_kitchen() throws InterruptedException {
            at_kitchen = true;
            kitchen_done = false;
            System.out.println("Robot is at kitchen");
            notifyAll();
        }

        public synchronized void robot_wait_for_kitchen_done() throws InterruptedException {
            while(!kitchen_done) {
                System.out.println("Robot is waiting for kitchen to be done");
                wait();
            }
        }

        public synchronized void robot_return_to_cafe() throws InterruptedException {
            at_kitchen = false;
            empty_on_tray = 0;
            full_on_tray = k;
            System.out.println("Robot is returning to cafe");
            notifyAll();
        }

        public synchronized void robot_wait_for_all_full_plates_taken() throws InterruptedException {
            while(full_on_tray > 0) {
                System.out.println("Robot is waiting for all full plates to be taken");
                wait();
            }
            accepting_empty = true;
            System.out.println("robot now takes empty plates");
            notifyAll();
        }

        // kitchen

        public synchronized void kitchen_wait_for_robot() throws InterruptedException {
            while(!at_kitchen || kitchen_done) {
                System.out.println("Kitchen is waiting for robot to arrive");
                wait();
            }
        }

        public synchronized void kitchen_finish_refill() throws InterruptedException {
            kitchen_done = true;
            System.out.println("Kitchen is done r ddefilling the tray");
            notifyAll();
            
        }





    }

    public static class Philosopher implements Runnable {
        private final int id;
        private final CafeMonitor cafeMonitor;
        private final Random random = new Random();

        public Philosopher(int id, CafeMonitor cafeMonitor) {
            this.id = id;
            this.cafeMonitor = cafeMonitor;
        }

        public void run() {
            while (true) {
                try {
                    // philosopher think and eat randomly
                    Thread.sleep(random.nextInt(400, 800));
                    // then he place the empty plate on the tray
                    cafeMonitor.place_empty(id);
                    // then he take a full plate from the tray
                    cafeMonitor.take_full(id);
                } catch (InterruptedException e) {}
                
            }
        }
    }

    public static class Robot implements Runnable {
        private final CafeMonitor cafeMonitor;
        public Robot(CafeMonitor cafeMonitor) {
            this.cafeMonitor = cafeMonitor;
        }
        public void run() {
            while (true) {
                try {
                    // robot wait for tray to be full
                    cafeMonitor.robot_wait_tray_full();
                    // robot arrive at kitchen
                    cafeMonitor.robot_arrive_kitchen();
                    // robot wait for kitchen to be done
                    cafeMonitor.robot_wait_for_kitchen_done();
                    // robot return to cafe
                    cafeMonitor.robot_return_to_cafe();
                    // robot wait for all full plates to be taken
                    cafeMonitor.robot_wait_for_all_full_plates_taken();
                } catch (InterruptedException e) {}
            }
        }
    }


    public static class Kitchen implements Runnable {
        private final CafeMonitor cafeMonitor;
        private final int k;
        private final Random random = new Random();
        public Kitchen(CafeMonitor cafeMonitor, int k) {
            this.cafeMonitor = cafeMonitor;
            this.k = k;
        }
        public void run() {
            while (true) {
                try {
                    // kitchen wait for robot to arrive
                    cafeMonitor.kitchen_wait_for_robot();
                    // kitchen refill the tray for k times each at random 20-50
                    for (int i = 0; i < k; i++) {
                        try {
                            Thread.sleep(random.nextInt(20, 50));
                        } catch (InterruptedException e) {}
                    }
                    // kitchen finish refilling the tray
                    cafeMonitor.kitchen_finish_refill();
                } catch (InterruptedException e) {}
            }
        }
    }























    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("n, k required for n - number of philosophers, k - number of plates");
            return;
        }
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        // if k is greater than n then we set k to n
        if (k > n) {
            k = n;
        }
       
        CafeMonitor cafeMonitor = new CafeMonitor(k);
        for (int i = 0; i < n; i++) {
            Thread thread = new Thread(new Philosopher(i, cafeMonitor));
            thread.start();
        }
        Thread robotThread = new Thread(new Robot(cafeMonitor));
        robotThread.start();
        Thread kitchenThread = new Thread(new Kitchen(cafeMonitor, k));
        kitchenThread.start();
        
    }
}