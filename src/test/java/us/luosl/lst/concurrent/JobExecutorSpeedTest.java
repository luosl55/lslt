package us.luosl.lst.concurrent;


import org.junit.jupiter.api.Test;
import us.luosl.lslt.concurrent.JobExecutor;
import us.luosl.lslt.concurrent.JobObserver;
import us.luosl.lslt.concurrent.JobStatistics;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class JobExecutorSpeedTest {

    @Test
    public void futureTest() {
        JobExecutor jobExecutor = JobExecutor.create(4, 4);
        JobObserver<?> observer = jobExecutor.beginJob("error test");
        int taskSize = 100;
        for(int i =0; i<taskSize; i++){
            int finalI = i;
            jobExecutor.submitWithJobObserver(() -> {
                if(finalI == 50) throw new RuntimeException("ggggg");
                System.out.println(finalI);
            }, observer);
        }
        // 忽略异常
        jobExecutor.awaitComplete(observer, e -> {});
        assert taskSize - 1 == observer.getCompleteCount();
    }
    /**
     * 回调测试
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void callbackTest() throws ExecutionException, InterruptedException {
        int num = 100000;
        AtomicInteger ai = new AtomicInteger();
        JobExecutor jobExecutor = JobExecutor.create(4, 4);
        JobObserver<Integer> observer = jobExecutor.beginJobWithCallback((Integer i) -> ai.getAndIncrement(), "异常测试");
        for(int i = 0; i< num; i++){
            int finalI = i;
            jobExecutor.submitWithJobObserver(() -> finalI, observer);
        }
        jobExecutor.awaitComplete(observer);
        assert num == ai.get();
    }

    /**
     * 速度测试
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void speedTest() throws ExecutionException, InterruptedException {
        List<String> dataSet = generateTestData();
        AtomicInteger i = new AtomicInteger();
        long s = 0L;
        long e = 0L;
        // 单线程执行
        s = System.currentTimeMillis();
        for(String str: dataSet){
            dataProcess(str);
        }
        e = System.currentTimeMillis();
        System.out.println( String.format("单线程执行, 耗时:%f秒", (e - s) / 1000f) );

        // 以 job 的方式执行
        s = System.currentTimeMillis();
        JobExecutor jobExecutor = JobExecutor.create(6, 6);
        JobObserver<?> observer = jobExecutor.beginJob("速度测试任务");
        JobStatistics.create(observer).setAllCount((long)dataSet.size()).setInterval(Duration.ofSeconds(2)).startStat();
        for(String str: dataSet){
            jobExecutor.submitWithJobObserver(() -> dataProcess(str) , observer);
        }
        jobExecutor.awaitComplete(observer);
        e = System.currentTimeMillis();
        System.out.println( String.format("以 job 的方式执行,耗时:%f秒", (e - s) / 1000f) );
        assert dataSet.size() == observer.getCompleteCount();

        // 以 Future 的方式执行
        s = System.currentTimeMillis();
        List<Future<Integer>> fs = dataSet.stream()
                .map(str -> jobExecutor.submit(() -> dataProcess(str))).collect(Collectors.toList());
        for(Future f :fs){
            f.get();
        }
        e = System.currentTimeMillis();
        System.out.println( String.format("以 Future 的方式执， 耗时:%f秒", (e - s) / 1000f) );

    }

    /**
     * 统计测试
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void jobStatisticsTest() throws ExecutionException, InterruptedException {
        List<String> datas = generateTestData();
        JobExecutor jobExecutor = JobExecutor.create(3, 3);
        JobObserver<?> observer = jobExecutor.beginJob();
        JobStatistics.create(observer).setAllCount((long)datas.size()).startStat();
        datas.forEach(s -> jobExecutor.submitWithJobObserver(() -> dataProcess(s), observer));
        jobExecutor.awaitComplete(observer);
        System.out.println(observer.getCompleteCount());
        assert datas.size() == observer.getCompleteCount();
    }


    /**
     * 定义一个耗时的处理
     * @param str str
     * @return
     */
    public Integer dataProcess(String str){

        for(int j = 0; j< 500; j++){
            String[] arr = str.split(" ");
            Optional<Double> ls = Arrays.stream(arr).map(Integer::parseInt)
                    .map(item -> item + 1)
                    .map(item -> item - 1)
                    .map(item -> (double) item / 2)
                    .map(item -> item * 2)
                    .reduce((a, b) -> a + b);
        }
        return 1;
    }

    /**
     * 生成测试数据
     * @return List<String>
     */
    private List<String> generateTestData(){
        List<String> strs = new LinkedList<>();
        for(int i=0; i< 200000; i++){
            StringBuilder sb = new StringBuilder();
            for(int j =0; j< 10; j++){
                sb.append(String.format(" %d", new Random().nextInt(10)));
            }
            strs.add(sb.toString().replaceFirst(" ", ""));
        }
        return strs;
    }

}
