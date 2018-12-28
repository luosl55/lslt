package us.luosl.lslt.concurrent;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static us.luosl.lslt.concurrent.JobStatus.*;

/**
 * 基于 job 的线程池
 */
public class JobExecutor {

    private ExecutorService jobExecutor;

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
     * 阻塞等待所有任务完成
     * @param jobObserver jobObserver
     * @param <C> <C>
     */
    public <C> void awaitComplete(JobObserver<C> jobObserver) {
        awaitComplete(jobObserver, (Throwable e) -> { throw new RuntimeException(e); } );
    }

    /**
     * 阻塞等待所有任务完成,并制定一个出现异常时的处理机制
     * @param jobObserver jobObserver
     * @param exceptionHandel exceptionHandel
     * @param <C> <C>
     */
    public <C> void awaitComplete(JobObserver<C> jobObserver, Consumer<Throwable> exceptionHandel) {
        endSubmit(jobObserver);
        LinkedList<Future<C>> fs = jobObserver.getFutures();
        for(Future<C> f: fs){
            try {
                f.get();
            } catch (Exception e) {
                exceptionHandel.accept(e);
            }
        }

    }


    /**
     * 根据 jobObserver 提交一个 Callable 任务
     * @param callable callable
     * @param jobObserver jobObserver
     * @param <C> <C>
     */
    public <C> void submitWithJobObserver(Callable<C> callable, JobObserver<C> jobObserver){
        if(jobObserver.getStatus().getValue() > RUNNING.getValue()){
            // todo 定义异常
            throw new RuntimeException("this status can not submit task！");
        }
        Future<C> future = jobExecutor.submit(() -> {
            jobObserver.decrAwaitingCount();
            jobObserver.incrRunningCount();
            C c;
            try {
                c = callable.call();
                // 执行回调函数
                if(null != jobObserver.getJobCallback()){
                    jobObserver.getJobCallback().callback(c);
                }
                jobObserver.incrCompleteCount();
            }catch (Exception e){
                jobObserver.incrErrorCount();
                throw e;
            }
            jobObserver.decrRunningCount();
            return c;
        });
        jobObserver.incrSubmitCount();
        jobObserver.incrAwaitingCount();
        jobObserver.getFutures().add(future);
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
        observer.setStatus(JobStatus.RUNNING);
        observer.setStartTime(System.currentTimeMillis());
        return observer;
    }

    private String generateJobName(){
        return String.format("job-%d", number.getAndIncrement());
    }

    public static JobExecutor create(ExecutorService executorService){
        return new JobExecutor(executorService);
    }

    public static JobExecutor create(int corePoolSize, int maximumPoolSize){
        ExecutorService executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(maximumPoolSize * 10),
                new BlockingRejectedExecutionHandler());
        return create(executorService);
    }

}
