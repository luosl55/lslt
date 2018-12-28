package us.luosl.lslt.concurrent;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * job 观察者
 * @param <T>
 */
public class JobObserver<T> {

    private String jobName;
    private AtomicLong completeCount = new AtomicLong();
    private AtomicLong errorCount = new AtomicLong();
    private AtomicLong runningCount = new AtomicLong();
    private AtomicLong awaitingCount = new AtomicLong();
    private AtomicLong submitCount = new AtomicLong();
    private long startTime;
    private AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INIT);
    private LinkedList<Future<T>> futures = new LinkedList<>();
    private JobCallback<T> jobCallback;

    protected JobObserver(String jobName) {
        this.jobName = jobName;
    }

    protected void incrRunningCount(){
        runningCount.incrementAndGet();
    }

    protected void decrRunningCount(){
        runningCount.decrementAndGet();
    }

    protected void incrCompleteCount(){
        completeCount.incrementAndGet();
    }

    protected void incrAwaitingCount(){
        awaitingCount.incrementAndGet();
    }

    protected void decrAwaitingCount(){
        awaitingCount.decrementAndGet();
    }

    protected void incrSubmitCount(){
        submitCount.getAndIncrement();
    }

    protected void incrErrorCount(){
        errorCount.getAndIncrement();
    }

    public LinkedList<Future<T>> getFutures() {
        return futures;
    }

    public Long getCompleteCount() {
        return completeCount.get();
    }

    public Long getRunningCount() {
        return runningCount.get();
    }

    public Long getAwaitingCount() {
        return awaitingCount.get();
    }

    public Long getSubmitCount(){
        return submitCount.get();
    }

    public Long getErrorCount(){
        return errorCount.get();
    }

    public String getJobName() {
        return jobName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    protected void setStatus(JobStatus status){
        this.status.set(status);
    }

    public JobStatus getStatus(){
        return this.status.get();
    }

    protected JobCallback<T> getJobCallback() {
        return jobCallback;
    }

    protected void setJobCallback(JobCallback<T> jobCallback) {
        if(getStatus().equals(JobStatus.INIT)){
            this.jobCallback = jobCallback;
        }else{
            throw new RuntimeException("You cannot change the jobCallback at this status!");
        }
    }

    protected void cancel(){
        while(true){
            Future<?> future = futures.poll();
            if(null == future) break;
            future.cancel(true);
        }
    }

}
