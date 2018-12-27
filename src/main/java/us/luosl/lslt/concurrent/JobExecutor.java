package us.luosl.lslt.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static us.luosl.lslt.concurrent.JobStatus.AWAIT_COMPLETE;
import static us.luosl.lslt.concurrent.JobStatus.CANCEL;

/**
 * 基于 job 的线程池
 */
public class JobExecutor {

    private enum EndJobTag{
        END_JOB
    }

    private ExecutorService jobExecutor;

    private ExecutorService observerExecutor = Executors.newCachedThreadPool();

    private AtomicLong number = new AtomicLong();

    private JobExecutor(ExecutorService executorService){
        this.jobExecutor = executorService;
    }

    /**
     * 终止向一个 Job 继续提交任务
     * @param jobObserver jobObserver
     * @param <C> <C>
     */
    public <C> void endSubmit(JobObserver<C> jobObserver) {
        jobObserver.setStatus(AWAIT_COMPLETE);
    }

    /**
     * 终止向一个 Job 继续提交任务,并且阻塞等待所有任务完成
     * @param jobObserver jobObserver
     * @param <C> <C>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public <C> void awaitComplete(JobObserver<C> jobObserver) throws ExecutionException, InterruptedException {
        endSubmit(jobObserver);
        FutureTask<EndJobTag> f = new FutureTask<>(() -> EndJobTag.END_JOB);
        f.run();
        jobObserver.addFutureToFirst(f);
        jobObserver.awaitComplete();
    }


    /**
     * 根据 jobObserver 提交一个 Callable 任务
     * @param callable callable
     * @param jobObserver jobObserver
     * @param <C> <C>
     */
    public <C> void submitWithJobObserver(Callable<C> callable, JobObserver<C> jobObserver){
        if(jobObserver.getStatus().getValue() >= CANCEL.getValue()){
            throw new RuntimeException("this status can not submit task！");
        }
        Future<C> future = jobExecutor.submit(() -> {
            jobObserver.decrAwaitingCount();
            jobObserver.incrRunningCount();
            C c = callable.call();
            // 执行回调函数
            if(null != jobObserver.getJobCallback()){
                jobObserver.getJobCallback().callback(c);
            }
            jobObserver.decrRunningCount();
            jobObserver.incrCompleteCount();
            return null;
        });
        jobObserver.incrSubmitCount();
        jobObserver.incrAwaitingCount();
        jobObserver.addFuture(future);
    }

    /**
     * 根据 jobObserver 提交一个 runnable 任务
     * @param runnable runnable
     * @param jobObserver jobObserver
     */
    public void submitWithJobObserver(Runnable runnable, JobObserver<?> jobObserver){
        submitWithJobObserver(() -> {
            runnable.run();
            return null;
        }, jobObserver);
    }

    public <V> Future<V> submit(Callable<V> callable){
        return jobExecutor.submit(callable);
    }

    public Future<?> submit(Runnable runnable){
        return jobExecutor.submit(runnable);
    }

    public void execute(Runnable runnable){
        jobExecutor.execute(runnable);
    }

    /**
     * 开始一个 job
     * @return JobObserver<?>
     */
    public JobObserver<?> beginJob(){
        return beginJobWithCallback(null);
    }

    /**
     * 开始一个 job 并且指定 job 的名称
     * @param jobName jobName
     * @return JobObserver<?>
     */
    public JobObserver<?> beginJob(String jobName){
        return beginJobWithCallback(null, jobName);
    }

    /**
     * 开始一个 job 并且指定 一个回调函数， 回调函数将在每一个任务执行完成后被调用
     * @param callback callback
     * @param <T> <T>
     * @return JobObserver<T>
     */
    public <T> JobObserver<T> beginJobWithCallback(JobCallback<T> callback){
        return beginJobWithCallback(callback, generateJobName());
    }

    /**
     * 开始一个 job 并且指定 一个回调函数和 job 名称， 回调函数将在每一个任务执行完成后被调用
     * @param callback callback
     * @param jobName jobName
     * @param <T> <T>
     * @return JobObserver<T>
     */
    public <T> JobObserver<T> beginJobWithCallback(JobCallback<T> callback, String jobName){
        JobObserver<T> observer = new JobObserver<>(jobName);
        observer.setJobCallback(callback);
        Future<?> observerFuture = observerExecutor.submit(() -> {
            while (needStop(observer)) {
                try {
                    Future<?> future = observer.takeFuture();
                    Object target = future.get();
                    if (target instanceof EndJobTag) {
                        if(observer.getCompleteCount().equals(observer.getSubmitCount())) break;
                    }
                } catch (Exception e) {
                    observer.setStatus(JobStatus.ERROR);
                    observer.cancel();
                    break;
                }
            }
            if(observer.getStatus().getValue() < CANCEL.getValue()){
                observer.setStatus(JobStatus.COMPLETE);
            }
        });
        observer.setStatus(JobStatus.RUNNING);
        observer.setObserverFuture(observerFuture);
        return observer;
    }

    private boolean needStop(JobObserver<?> jobObserver){
        return !(jobObserver.getStatus().equals(JobStatus.AWAIT_COMPLETE) &&
                jobObserver.getSubmitCount().equals(jobObserver.getCompleteCount()) );
    }

    private String generateJobName(){
        return String.format("job-%d", number.getAndIncrement());
    }

    public static JobExecutor create(ExecutorService executorService){
        return new JobExecutor(executorService);
    }

    public static JobExecutor create(int corePoolSize, int maximumPoolSize){
        ExecutorService executorService = new ThreadPoolExecutor(3, 3, 1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(maximumPoolSize * 3),
                new BlockingRejectedExecutionHandler());
        return create(executorService);
    }

}
