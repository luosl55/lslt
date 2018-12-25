package us.luosl.lslt.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class JobObserver<T> {

    private AtomicLong completeCount = new AtomicLong();
    private AtomicLong runningCount = new AtomicLong();
    private AtomicLong awaitingCount = new AtomicLong();
    private AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INIT);
    private BlockingDeque<Future<T>> futureQueue = new LinkedBlockingDeque<>();
    private JobCallback<T> jobCallback;
    private Future<Void> observerFuture;

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

    public void setObserverFuture(Future<Void> observerFuture){
        this.observerFuture = observerFuture;
    }

    public void awaitComplete() throws ExecutionException, InterruptedException {
        observerFuture.get();
    }

    public void addFuture(Future<T> future){
        this.futureQueue.add(future);
    }

    @SuppressWarnings("unchecked")
    public void addFutureToFirst(Future<?> future){
        ((BlockingDeque)this.futureQueue).addFirst(future);
    }

    public Future<T> takeFuture() throws InterruptedException {
        return futureQueue.take();
    }

    public void setStatus(JobStatus status){
        this.status.set(status);
    }

    public JobStatus getStatus(){
        return this.status.get();
    }

    public JobCallback<T> getJobCallback() {
        return jobCallback;
    }

    public void setJobCallback(JobCallback<T> jobCallback) {
        if(getStatus().equals(JobStatus.INIT)){
            this.jobCallback = jobCallback;
        }else{
            throw new RuntimeException("You cannot change the jobCallback at this status!");
        }
    }

    public void cancel(){
        while(true){
            Future<T> future = futureQueue.poll();
            if(null == future) break;
            future.cancel(true);
        }
    }

}
