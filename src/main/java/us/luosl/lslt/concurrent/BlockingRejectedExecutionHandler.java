package us.luosl.lslt.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 阻塞重复提交的线程池拒绝策略
 */
public class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            BlockingQueue<Runnable> queue = executor.getQueue();
            queue.put(r);
        } catch (InterruptedException e) {
            rejectedExecution(r, executor);
        }
    }
}
