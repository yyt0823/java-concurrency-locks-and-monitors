import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;



public class q1 {
    // Q1  
    /*
    idea:
        want to let thread acquire the lock for each ingredient but will end up with deadlock
    e.g. thread 1 need salt and pepper, it takes salt thread 2 need pepper and salt it takes pepper.
    thread 1 will wait for thread 2 and vice versa end up with deadlock. 

    solution:
        we could use global order lock to break the dependency cycle to insure that deadlock does not happen
        we do this by reordering the locks needed for each thread in increasing order
        

    first try:
        first we reorder the locks needed for each thread
        then use synchronized to acquire the lock for each ingredient recursively
        this does not work since when doing the replacement
        a cook want 1234, get 123 lock, but not 4 yet.
        a shopper want 4 -> 6, it get 4 then replace it with 6
        the cook is now stuck

    second try:
        replace only the name of the ingredient not the lock
        asked about this on ed discussion, rejected
    
    third try:
        want a barrier kind of behavior that will let all cook thread finish before the shopper thread start
        did some research find out reentrant read write lock that will do this precisely
        now we can just lock cooks with read lock and shopper with write lock
        now when a shopper want to come in all cook must finish first which is the behavior we want
    */







    // init K and N at 10, you can change this to test the program, K for total number of ingredients, N for number of threads.
    
    public static int K = 4;
    public static int k = 3;
    public static int N = 3;
    public static int M = 3;

    // global rw lock
    static final ReentrantReadWriteLock pantryGate = new ReentrantReadWriteLock(true); 


    // make new datastuture ingredient to store the name of the ingredient as interger
    public static class Ingredient {
        public int ingredient;
        public Ingredient(int ingredient) {
            this.ingredient = ingredient;
        }
        @Override
        public String toString() {
            return String.valueOf(ingredient);
        }
    }
    
    // init the K ingredient lock list
    public static Ingredient[] init_lock_list(int K){
        Ingredient[] lock_list = new Ingredient[K];
        for (int i = 0; i < K; i++) {
            lock_list[i] = new Ingredient(i);
        }
        return lock_list;
    }
    

    // random seed
    public static Random random = new Random();
    

    // helper function to generate a random list of k ingredients from 0 to K of size k
    public static int[] picks(Random random, int K, int k) 
    {
        return random.ints(0, K ).distinct().limit(k).toArray();
    }   

    // we want to order the list of ingredients by increasing order to break the dependency cycle
    public static void reorder_locks(int[] picks)
    {
        Arrays.sort(picks);
    }

    // helper function to read the names of the ingredients from the lock list
    public static int[] read_names(int[] picks, Ingredient[] lock_list){
        int[] names = new int[picks.length];
        for (int i = 0; i < picks.length; i++) {
            names[i] = lock_list[picks[i]].ingredient;
        }
        return names;
    }
    // we acquire locks in the locklist recursively
    public static void acquire_locks_recursive(int i, int[] picks, Ingredient[] lock_list)
    {
        synchronized (lock_list[picks[i]]) {

            // all locks acquired then we cook
            if (i == picks.length - 1) {
                int[] names = read_names(picks, lock_list);
                System.out.println(Thread.currentThread() + " cooking " + " with ingredients: " + Arrays.toString(names));
                try {
                    Thread.sleep(random.nextInt(100,500));
                } catch (InterruptedException e) {}
            }
            // otherwise we acquire the next lock
            else {
                acquire_locks_recursive(i + 1, picks, lock_list);
            }
        }
    }

    // pick new random ingredients list of size M
    // this is for the shopper replacement function
    public static Ingredient[] pick_new_ingredients(int M){
        Ingredient[] new_ingredients = new Ingredient[M];
        for (int j = 0; j < M; j++) {
            new_ingredients[j] = new Ingredient(random.nextInt(5*K));
        }
        return new_ingredients;
    }



    // replace the ingredients with new random ingredients
    // update the field of the ingredient other than swapping the lock
    public static void replace_ingredients(int i, int[] picks, Ingredient[] lock_list){
        synchronized (lock_list[picks[i]]) {
            if (i == picks.length - 1) {
                Ingredient[] new_ingredients = pick_new_ingredients(picks.length);
                for (int j = 0; j < picks.length; j++) {
                    lock_list[picks[j]] = new_ingredients[j];
                }
                System.out.println("replacing ingredient " + Arrays.toString(picks) + "\nnew ingredients" + Arrays.toString(new_ingredients));
                try {
                    Thread.sleep(random.nextInt(100,500));
                } catch (InterruptedException e) {}
            }
            else {
                replace_ingredients(i + 1, picks, lock_list);
            }
        }
    }

    














    // main funciton
    public static void main(String[] args) {
        int n = Integer.parseInt(args[0]);
        int kp = Integer.parseInt(args[1]);
        int m = Integer.parseInt(args[2]);
        int Kp = Integer.parseInt(args[3]);
        if (n >= 1 && kp > 1 && m > 1 && Kp >= kp) {
            N = n;
            K = Kp;
            M = m;
            k = kp;
        }
        else {
            System.out.println("Invalid input, parameters n ≥ 1, k > 1, m > 1, K ≥ k");
            return;
        }


        Ingredient[] lock_list = init_lock_list(K);

        for (int i = 0; i < N; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        pantryGate.readLock().lock();
                        try {
                            int[] picks = picks(random, K, k);
                            reorder_locks(picks);
                            acquire_locks_recursive(0, picks, lock_list);
                        } finally {
                            pantryGate.readLock().unlock();
                        }
                        try {
                            Thread.sleep(random.nextInt(100, 200));
                        } catch (InterruptedException e) {}
                    }
                }
            });
            thread.start();
        }
        Thread shopkeeper = new Thread(new Runnable() {
            public void run() {
                System.out.println("Shopkeeper is starting");
                while (true) {
                    pantryGate.writeLock().lock();
                    try {
                        int[] picks = picks(random, K, M);
                        reorder_locks(picks);
                        replace_ingredients(0, picks, lock_list);
                    } finally {
                        pantryGate.writeLock().unlock();
                    }
                    try {
                        Thread.sleep(random.nextInt(800, 1200));
                    } catch (InterruptedException e) {}
                }
            }
        });
        shopkeeper.start();



    



}   
}
