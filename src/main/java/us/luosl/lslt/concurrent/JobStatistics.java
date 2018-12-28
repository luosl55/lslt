package us.luosl.lslt.concurrent;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
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
        String threadName = String.format("[%s]统计线程", jobObserver.getJobName());
        Thread statThread = new Thread(() -> {
            try{
                int threshold = JobStatus.COMPLETE.getValue();
                while(jobObserver.getStatus().getValue() < threshold){
                    TimeUnit.NANOSECONDS.sleep(interval.toNanos());
                    invokePrint(jobObserver);
                }
                invokePrint(jobObserver);
            } catch (InterruptedException e) {
                statInfoFormat.print(String.format("%s异常终止!", threadName));
            }
        });
        statThread.setDaemon(true);
        statThread.setName(threadName);
        statThread.start();
    }

    private void invokePrint(JobObserver<?> jobObserver) {
        long completeCount = jobObserver.getCompleteCount();
        long intervalCompleteCount = completeCount - beforeCompleteCount.get();
        beforeCompleteCount.set(completeCount);
        statInfoFormat.print(
                statInfoFormat.mkStatInfo(interval, allCount, intervalCompleteCount, jobObserver)
        );
    }

    public static JobStatistics create(JobObserver<?> jobObserver){
        return new JobStatistics(jobObserver);
    }


    public interface StatInfoFormat{
        String mkStatInfo(Duration interval, Long allCount, long intervalCompleteCount, JobObserver<?> jobObserver);
        void print(String statInfo);
    }

    public class StandardOutputStatInfoFormat implements StatInfoFormat{

        @Override
        public String mkStatInfo(Duration interval, Long allCount, long intervalCompleteCount, JobObserver<?> jobObserver) {
            double speed = (double)intervalCompleteCount / interval.getSeconds();
            long costTime = System.currentTimeMillis() - jobObserver.getStartTime();
            String base = String.format("执行速度:%.2f/秒, 已完成数:%d, 错误数:%d, 正在运行数:%d, 等待运行数:%d, 已经运行:%s",
                    speed, jobObserver.getCompleteCount(), jobObserver.getErrorCount(),
                    jobObserver.getRunningCount(), jobObserver.getAwaitingCount(), costTimeFormat(costTime));
            if(null != allCount){
                double rate = (double)jobObserver.getCompleteCount() / allCount * 100;
                long estimatedTime = (long) ((allCount - jobObserver.getCompleteCount() ) / speed * 1000);
                base = String.format("当前进度:%.2f%%, 预计还需要花费:%s ,%s",
                        rate, costTimeFormat(estimatedTime), base);
            }
            return String.format("正在执行[%s] %s", jobObserver.getJobName(), base);
        }

        private String costTimeFormat(long costTime){
            double s = costTime / 1000D;
            if(s > 60D){
                double m = s / 60;
                if(m > 60){
                    return String.format("%.2f/小时", m / 60);
                }else{
                    return String.format("%.2f/分", m);
                }
            }else{
                return String.format("%.2f/秒", s);
            }
        }

        @Override
        public void print(String statInfo) {
            System.out.println(statInfo);
        }
    }
}
