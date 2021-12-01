package com.tuzhennan.purertc.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class VirtualThread {

    public interface Task {
        public void run();
    }

    private class DelayedTask implements Comparable<DelayedTask> {

        public long runAtMS;

        public Task task;

        public DelayedTask(long runAtMS, Task task) {
            this.runAtMS = runAtMS;
            this.task = task;
        }

        @Override
        public int compareTo(DelayedTask o) {
            return Long.compare(this.runAtMS, o.runAtMS);
        }
    }

    private final Clock clock;

    private List<Task> tasks;

    private PriorityQueue<DelayedTask> delayedTasks;

    public VirtualThread(Clock clock) {
        this.clock = clock;
        this.tasks = new ArrayList<Task>();
        this.delayedTasks = new PriorityQueue<DelayedTask>();
    }

    public void step() {
        List<Task> tasks = this.tasks;
        PriorityQueue<DelayedTask> delayedTasks = this.delayedTasks;
        this.tasks = new ArrayList<>();
        this.delayedTasks = new PriorityQueue<DelayedTask>();
        tasks.forEach((Task::run));
        long nowMS = clock.nowMS();
        while (delayedTasks.peek() != null && delayedTasks.peek().runAtMS <= nowMS) {
            delayedTasks.poll().task.run();
        }
    }

    public void postTask(Task task) {
        this.tasks.add(task);
    }

    public void postDelayedTask(long delayedMS, Task task) {
        this.delayedTasks.add(new DelayedTask(clock.nowMS() + delayedMS, task));
    }
}
