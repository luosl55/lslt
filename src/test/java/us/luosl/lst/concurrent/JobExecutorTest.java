package us.luosl.lst.concurrent;


import org.junit.jupiter.api.Test;
import us.luosl.lslt.concurrent.JobExecutor;
import us.luosl.lslt.concurrent.JobObserver;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class JobExecutorTest {
    @Test
    public void test() throws ExecutionException, InterruptedException {
        List<String> strs = new LinkedList<>();
        for(int i=0; i< 100000; i++){
            strs.add(String.format("%d %d", new Random().nextInt(10), new Random().nextInt(10)));
        }
        AtomicInteger i = new AtomicInteger();


        JobExecutor jobExecutor = new JobExecutor();
        JobObserver<Void> observer = jobExecutor.beginJob();
        long s = System.currentTimeMillis();
        for(String str: strs){
            jobExecutor.submit(() -> {
                String[] arr = str.split(" ");
                Optional<Integer> sum = Arrays.stream(arr).map(Integer::parseInt).reduce((a, b) -> a + b);
                i.getAndIncrement();
            }, observer);
        }
        jobExecutor.endSubmitAndAwaitComplete(observer);
        long e = System.currentTimeMillis();
        System.out.println( String.format("耗时:%f秒", (e - s) / 1000f) );
        System.out.println(i.get());
    }
}
