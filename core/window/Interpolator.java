package core.window;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * This Class is able to distribute a function parameter over several frames
 * when registered.
 * 
 * @author Sean Duft
 */
public class Interpolator {

    /** parabolic interpolation function */
    public static final int PARABEL_FUNCTION = 1;
    /** second half of the parabolic interpolation function */
    public static final int HALF_PARABEL_FUNCTION = 2;
    /** parabolic function with a little overshoot and reverse at the end */
    public static final int PARABEL_FUNCTION_BACK = 3;

    private static Interpolator instance;

    /** list of the registered actions */
    private HashMap<Integer, Action> registered;

    /** next used id */
    private int nextID = -1;

    private Interpolator() {
        registered = new HashMap<>();
    }

    /**
     * returns the instance of the interpolator creates a new one if none exists
     * 
     * @return the instance of the interpolator
     */
    public static Interpolator getInstance() {
        if (Interpolator.instance == null) {
            instance = new Interpolator();
        }
        return instance;
    }

    /**
     * adds the given amount to the given id
     */
    public void add(float addition, int id) {
        if (!registered.keySet().contains(id))
            return;
        registered.get(id).add(addition);
    }

    /**
     * adds the given amount to the given id, notifys the given latch after
     * finish
     */
    public void add(float addition, int id, CountDownLatch countDownAfterFinish) {
        if (!registered.keySet().contains(id))
            return;
        registered.get(id).add(addition);
        registered.get(id).countDownAfterFinish = countDownAfterFinish;
    }

    /**
     * adds the given amount to the given id
     */
    public void add(float addition, int id, float time) {
        if (!registered.keySet().contains(id))
            return;
        registered.get(id).add(addition);
        registered.get(id).totalTime = time;
    }

    /**
     * adds the given amount to the given id, notifys the given latch after
     * finish
     */
    public void add(float addition, int id, CountDownLatch countDownAfterFinish, float time) {
        if (!registered.keySet().contains(id))
            return;
        registered.get(id).add(addition);
        registered.get(id).countDownAfterFinish = countDownAfterFinish;
        registered.get(id).totalTime = time;
    }

    /**
     * resets the interpolator removes all registered functions
     */
    public void reset() {
        synchronized (registered) {
            registered.clear();
        }
    }

    /**
     * removes all registered actions except the given ids
     * 
     * @param exception
     *            a list of ids that are not cleared
     */
    public void clearAllExcept(Integer[] exception) {
        synchronized (registered) {
            for (Integer i : new HashMap<>(registered).keySet()) {
                if (!Arrays.asList(exception).contains(i)) {
                    registered.remove(i);
                }
            }
        }
    }

    /**
     * Registers an action.
     * 
     * @param totalTime
     *            how long the action shall take
     * @param toCall
     *            the method to call
     * @param where
     *            the object to call the method on
     * @param function
     *            the interpolation function
     * @return id of the registered action
     */
    public int register(float totalTime, Consumer<Float> toCall, int function) {
        nextID++;
        registered.put(nextID, new Action(totalTime, toCall, function));
        return nextID;
    }

    public int register(float totalTime, Consumer<Float> toCall, int function, boolean invokeLater, boolean exact) {
        nextID++;
        registered.put(nextID, new Action(totalTime, toCall, function, invokeLater, exact));
        return nextID;
    }

    /**
     * Registers an action.
     * 
     * @param totalTime
     *            how long the action shall take
     * @param toCall
     *            the method to call
     * @param where
     *            the object to call the method on
     * @param function
     *            the interpolation function
     * @param maximum
     *            the maximum amount for the remaining amount of the parameter
     *            at any given time
     * @return id of the registered action
     */
    public int register(float totalTime, Consumer<Float> toCall, int function, int maximum) {
        nextID++;
        registered.put(nextID, new Action(totalTime, toCall, function, maximum));
        return nextID;
    }

    /** Executes all registered actions. */
    public synchronized void use() {
        synchronized (registered) {
            for (Integer i : new HashMap<>(registered).keySet()) {
                registered.get(i).execute();
            }
        }
    }

    private class Action {
        float totalTime;
        float currentTime;
        float registeredTime;
        float amount;
        int maximum;
        Consumer<Float> toCall;
        int function;
        LinkedList<Float> later = new LinkedList<>();
        boolean exact = false;
        boolean invokeLater = false;
        float totalAmount = 0;
        CountDownLatch countDownAfterFinish;

        public Action(float totalTime, Consumer<Float> toCall, int function) {
            this.totalTime = totalTime;
            this.registeredTime = totalTime;
            this.currentTime = 0;
            this.amount = 0;
            this.toCall = toCall;
            this.maximum = Integer.MAX_VALUE;
            this.function = function;
        }

        public Action(float totalTime, Consumer<Float> toCall, int function, int maximum) {
            this.totalTime = totalTime;
            this.registeredTime = totalTime;
            this.currentTime = 0;
            this.amount = 0;
            this.toCall = toCall;
            this.maximum = maximum;
            this.function = function;
        }

        public Action(float totalTime, Consumer<Float> toCall, int function, boolean invokeLater, boolean exact) {
            this.totalTime = totalTime;
            this.registeredTime = totalTime;
            this.currentTime = 0;
            this.amount = 0;
            this.toCall = toCall;
            this.maximum = Integer.MAX_VALUE;
            this.function = function;
            this.invokeLater = invokeLater;
            this.exact = exact;
        }

        public void execute() {
            float delta = Timer.getInstance().getDelta();
            if (this.amount != 0) {
                this.totalAmount += delta * function();
                this.toCall.accept(delta * function());
                this.currentTime += delta;
                if (this.function == 3) {
                    if (this.currentTime >= this.totalTime + 0.2) {
                        this.currentTime = 0;
                        if (this.exact) {
                            this.toCall.accept(amount - totalAmount);
                        }
                        if (this.countDownAfterFinish != null) {
                            countDownAfterFinish.countDown();
                        }
                        this.amount = 0;
                        this.totalAmount = 0;
                        this.totalTime = this.registeredTime;
                        if (this.invokeLater && later.size() > 0) {
                            this.amount = later.poll();
                        }
                    }
                } else if (this.function == 2) {
                    if (this.currentTime >= this.totalTime / 2) {
                        this.currentTime = 0;
                        if (this.exact) {
                            this.toCall.accept(amount - totalAmount);
                        }
                        if (this.countDownAfterFinish != null) {
                            countDownAfterFinish.countDown();
                        }
                        this.amount = 0;
                        this.totalAmount = 0;
                        this.totalTime = this.registeredTime;
                        if (this.invokeLater && later.size() > 0) {
                            this.amount = later.poll();
                        }
                    }

                } else {
                    if (this.currentTime >= this.totalTime) {
                        this.currentTime = 0;
                        if (this.exact) {
                            this.toCall.accept(amount - totalAmount);
                        }
                        if (this.countDownAfterFinish != null) {
                            countDownAfterFinish.countDown();
                        }
                        this.amount = 0;
                        this.totalAmount = 0;
                        this.totalTime = this.registeredTime;
                        if (this.invokeLater && later.size() > 0) {
                            this.amount = later.poll();
                        }
                    }
                }
            }
        }

        private float function() {
            switch (this.function) {
            case 2:
                return function2();

            default:
                return function1();
            }
        }

        private float function1() {
            return (float) (1 / Math.pow(totalTime, 3) * -6 * amount * currentTime * (currentTime - totalTime));
        }

        private float function2() {
            return (float) (1 / Math.pow(totalTime, 3) * 6 * amount * (currentTime - totalTime / 2)
                    * ((currentTime - totalTime / 2) - totalTime));
        }

        private void add(float addition) {
            if (this.invokeLater && this.amount != 0) {
                later.add(addition);
            } else {
                if (this.amount * addition < 0) {
                    this.amount = this.amount / -2;
                    this.currentTime /= 3;
                    this.totalAmount = 0;
                    // this.currentTime = 0;
                } else {
                    this.amount += addition;
                    this.currentTime /= 2;
                }
                if (this.amount > this.maximum) {
                    this.amount = this.maximum;
                } else if (this.amount < this.maximum * -1) {
                    this.amount = this.maximum * -1;
                }
            }

        }

    }

}
