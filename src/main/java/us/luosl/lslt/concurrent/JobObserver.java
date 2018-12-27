package us.luosl.lslt.concurrent;

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
    private AtomicLong runningCount = new AtomicLong();
    private AtomicLong awaitingCount = new AtomicLong();
    private AtomicLong submitCount = new AtomicLong();
    private AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INIT);
    private BlockingDeque<Future<T>> futureQueue = new LinkedBlockingDeque<>();
    private JobCallback<T> jobCallback;
    private Future<?> observerFuture;

    protected JobObserver(String jobName) {
        this.jobName = jobName;
    }

    public void incrRunningCount(){
        runningCount.incrementAndGet();
    }

    public void decrRunningCount(){
        runningCount.decrementAndGet();
    }

    public void incrCompleteCount(){
        completeCount.incrementAndGet();
    }

    public void incrAwaitingCount(){
        awaitingCount.incrementAndGet();
    }

    public void decrAwaitingCount(){
        awaitingCount.decrementAndGet();
    }

    public void incrSubmitCount(){
        submitCount.getAndIncrement();
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

    public String getJobName() {
        return jobName;
    }

    protected void setObserverFuture(Future<?> observerFuture){
        this.observerFuture = observerFuture;
    }

    protected void awaitComplete() throws ExecutionException, InterruptedException {
        observerFuture.get();
    }

    protected void addFuture(Future<T> future){
        this.futureQueue.add(future);
    }

    @SuppressWarnings("unchecked")
    protected void addFutureToFirst(Future<?> future){
        ((BlockingDeque)this.futureQueue).addFirst(future);
    }

    protected Future<T> takeFuture() throws InterruptedException {
        return futureQueue.take();
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
            Future<T> future = futureQueue.poll();
            if(null == future) break;
            future.cancel(true);
        }
    }

}
