package us.luosl.lslt.concurrent;

import java.util.concurrent.*;

import static us.luosl.lslt.concurrent.JobStatus.END_SUBMIT;

public class JobExecutor {

    private enum EndJobTag{
        END_JOB, CANCEL
    }

    private LinkedBlockingDeque<Runnable> jobExecutorQueue = new LinkedBlockingDeque<>(100);

    // todo
    private ExecutorService jobExecutor = new ThreadPoolExecutor(6, 6, 1L,
            TimeUnit.MINUTES,
            jobExecutorQueue,
            new BlockingRejectedExecutionHandler());

    private ExecutorService observerExecutor = Executors.newCachedThreadPool();

    public <C> void endSubmit(JobObserver<C> jobObserver) throws InterruptedException {
        FutureTask task =  new FutureTask<>(() -> END_SUBMIT);
        jobExecutorQueue.putFirst(task);
        jobObserver.setStatus(END_SUBMIT);
    }

    public <C> void awaitComplete(JobObserver<C> jobObserver) throws ExecutionException, InterruptedException {
        jobObserver.awaitComplete();
    }

    public <C> void endSubmitAndAwaitComplete(JobObserver<C> jobObserver) throws InterruptedException, ExecutionException {
        endSubmit(jobObserver);
        awaitComplete(jobObserver);
    }

    public <C> void submit(Callable<C> callable, JobObserver<C> jobObserver){
        if(jobObserver.getStatus().getValue() >= JobStatus.CANCEL.getValue()){
            throw new RuntimeException("this status can not submit task！");
        }
        Future<C> future = jobExecutor.submit(() -> {
            jobObserver.decrAwaitingCount();
            jobObserver.incrRunningCount();
            C c = callable.call();
            jobObserver.decrRunningCount();
            jobObserver.incrCompleteCount();
            return c;
        });
        jobObserver.incrAwaitingCount();
        jobObserver.addFuture(future);
    }

    public void submit(Runnable runnable, JobObserver<Void> jobObserver){
        submit(() -> {
            runnable.run();
            return null;
        }, jobObserver);
    }

    public JobObserver<Void> beginJob(){
        return beginJobWithCallback( new EmptyJobCallable());
    }

    @SuppressWarnings("unchecked")
    public <T> JobObserver<T> beginJobWithCallback(JobCallback<T> callback){
        JobObserver<T> observer = new JobObserver<>();
        observer.setJobCallback(callback);
        Future<Void> observerFuture = observerExecutor.submit(() -> {
            while (true) {
                try {
                    Future<?> future = observer.takeFuture();
                    Object target = future.get();
                    if (target instanceof EndJobTag) {
                        EndJobTag tag = (EndJobTag) target;
                        switch (tag) {
                            case END_JOB:
                                observer.setStatus(JobStatus.COMPLETE);
                                break;
                            case CANCEL:
                                observer.setStatus(JobStatus.CANCEL);
                                break;
                        }
                        break;
                    }
                    if (null != observer.getJobCallback()) {
                        try {
                            observer.getJobCallback().callback((T) target);
                        } catch (Exception e) {
                            observer.setStatus(JobStatus.CALLBACK_ERROR);
                            observer.cancel();
                            break;
                        }
                    }
                    observer.decrRunningCount();
                    observer.incrCompleteCount();
                } catch (Exception e) {
                    observer.setStatus(JobStatus.ERROR);
                    observer.cancel();
                    break;
                }
            }
            return null;
        });
        observer.setObserverFuture(observerFuture);
        return observer;
    }

    public <T> void cancel(JobObserver<T> jobObserver){
        jobObserver.addFutureToFirst(new FutureTask<>(() -> EndJobTag.CANCEL));
        jobObserver.cancel();
    }

}
