package us.luosl.lslt.concurrent;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * job 进度信息统计
 */
public class JobStatistics {

    private Long allCount;
    private JobObserver<?> jobObserver;
    private Duration interval;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private StatInfoFormat statInfoFormat;
    private AtomicLong beforeCompleteCount = new AtomicLong();

    private JobStatistics(JobObserver<?> jobObserver){
        this.jobObserver = jobObserver;
    }

    public JobStatistics setAllCount(Long allCount){
        this.allCount = allCount;
        return this;
    }

    public JobStatistics setInterval(Duration interval) {
        this.interval = interval;
        return this;
    }

    public JobStatistics setStatInfoFormat(StatInfoFormat statInfoFormat) {
        this.statInfoFormat = statInfoFormat;
        return this;
    }

    private void init(){
        if(null == statInfoFormat){
            statInfoFormat = new StandardOutputStatInfoFormat();
        }
        if(null == interval){
            interval = Duration.ofSeconds(10);
        }
    }

    public void startStat(){
        assert !isRunning.get() && null != jobObserver;
        isRunning.set(true);
        init();
        Thread statThread = new Thread(() -> {
            try{
                int threshold = JobStatus.ERROR.getValue();
                while(jobObserver.getStatus().getValue() < threshold){
                    TimeUnit.NANOSECONDS.sleep(interval.toNanos());
                    invokePrint(jobObserver);
                }
                invokePrint(jobObserver);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        statThread.setDaemon(true);
        statThread.setName("job statistics thread");
        statThread.start();
    }

    private void invokePrint(JobObserver<?> jobObserver) {
        long completeCount = jobObserver.getCompleteCount();
        long intervalCompleteCount = completeCount - beforeCompleteCount.get();
        beforeCompleteCount.set(completeCount);
        long awaitingCount = jobObserver.getAwaitingCount();
        long RunningCount = jobObserver.getRunningCount();
        statInfoFormat.print(
                statInfoFormat.mkStatInfo(interval, allCount,
                        completeCount, intervalCompleteCount, RunningCount, awaitingCount)
        );

    }

    public static JobStatistics create(JobObserver<?> jobObserver){
        return new JobStatistics(jobObserver);
    }


    public interface StatInfoFormat{
        String mkStatInfo(Duration interval, Long allCount, Long completeCount, Long intervalCompleteCount, Long RunningCount, Long awaitingCount);
        void print(String statInfo);
    }

    public class StandardOutputStatInfoFormat implements StatInfoFormat{
        @Override
        public String mkStatInfo(Duration interval, Long allCount, Long completeCount, Long intervalCompleteCount, Long RunningCount, Long awaitingCount) {
            double speed = (double)intervalCompleteCount / interval.getSeconds();
            String base = String.format("执行速度:%.2f/秒, 已完成数:%d, 正在运行数:%d, 等待运行数:%d",
                    speed, completeCount, RunningCount, awaitingCount);
            if(null != allCount){
                double rate = (double)completeCount / allCount * 100;
                base = String.format("当前进度:%.2f%% ,%s", rate, base);
            }
            return String.format("正在执行[%s] %s", jobObserver.getJobName(), base);
        }

        @Override
        public void print(String statInfo) {
            System.out.println(statInfo);
        }
    }
}
