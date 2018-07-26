package com.zdc.java.script;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zhudongchang on 2018/7/21.
 */
public class TimeWheelService {

    private TimeWheelThread thread=new TimeWheelThread();

    private ExecutorService executorService;

    private TimeWheelWheel wheel=new TimeWheelWheel();

    public static TimeWheelService instance=new TimeWheelService();

    private TimeWheelService(){
        this(5);
    }

    private TimeWheelService(int corePoolSize){
        this(new ThreadPoolExecutor(corePoolSize, corePoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(10000)));
    }

    private TimeWheelService(ThreadPoolExecutor threadPoolExecutor){
        executorService=threadPoolExecutor;
        thread.start();
    }

    /**
     * 非周期调度 period=0
     * @param runnable
     * @param delay 延迟毫秒数
     */
    public void schedule(Runnable runnable,
                          long delay){
        schedule(runnable,delay,0);
    }

    /**
     *  周期调度
     * @param runnable
     * @param delay
     * @param period
     */
    public void schedule(Runnable runnable,
                         long delay,long period){
        TimeWheelTask timeWheelTask = new TimeWheelTask(delay,period,runnable);
        schedule(timeWheelTask);
    }

    private void schedule(TimeWheelTask timeWheelTask){
        //修正 已走过的时间 ，因为只有 当timeWheel，否则会出现bug
        //eg： 在timeWheel 走到 24h59分59秒当时候加入 一个 1天1秒后执行的任务，
        //如果不修正 其实任务会在 2秒后执行
        timeWheelTask.delay =timeWheelTask.delay+ wheel.getWheelNowTime(timeWheelTask.delay);
        wheel.addTaskToWheel(timeWheelTask.delay,timeWheelTask);
    }


    /**
     * 时间轮的 轮子
     */
    class TimeWheelWheel{
        /**
         * 滴答间隔 毫秒
         */
        private static final int TICK=100;

        private static final int wheelIndexMillisecondLength=1000/TICK;
        private static final int wheelIndexSecondLength=60;
        private static final int wheelIndexMinuteLength=60;
        private static final int wheelIndexHourLength=24;
        private static final int wheelIndexDayLength=365;

        private static final long wheelMillisecondAllTicks=1;
        private static final long wheelSecondAllTicks=wheelIndexMillisecondLength*wheelMillisecondAllTicks;
        private static final long wheelMinuteAllTicks=wheelIndexSecondLength*wheelSecondAllTicks;
        private static final long wheelHourAllTicks=wheelIndexMinuteLength*wheelMinuteAllTicks;
        private static final long wheelDayAllTicks=wheelIndexHourLength*wheelHourAllTicks;


        private AtomicInteger wheelIndexMillisecond=new AtomicInteger(0);
        private AtomicInteger wheelIndexSecond=new AtomicInteger(0);
        private AtomicInteger wheelIndexMinute=new AtomicInteger(0);
        private AtomicInteger wheelIndexHour=new AtomicInteger(0);
        private AtomicInteger wheelIndexDay=new AtomicInteger(0);

        private volatile Vector[] wheelMillisecond=new Vector[wheelIndexMillisecondLength];
        private volatile Vector[] wheelSecond=new Vector[wheelIndexSecondLength];
        private volatile Vector[] wheelMinute=new Vector[wheelIndexMinuteLength];
        private volatile Vector[] wheelHour=new Vector[wheelIndexHourLength];
        private volatile Vector[] wheelDay =new Vector[wheelIndexDayLength];

        private boolean incIndex(TimeUnit timeUnit){
            long allTicksNext;
            Vector[] vectorsNext;
            AtomicInteger index;
            AtomicInteger indexNext;
            int wheelLength;
            int wheelLengthNext;

            switch (timeUnit){
                case DAYS:
                    allTicksNext=0;
                    vectorsNext= null;
                    indexNext=null;
                    wheelLengthNext=0;

                    index=wheelIndexDay;
                    wheelLength=wheelIndexDayLength;
                    break;
                case HOURS:
                    allTicksNext=wheelDayAllTicks;
                    vectorsNext=wheelDay;
                    index=wheelIndexHour;
                    indexNext=wheelIndexDay;
                    wheelLength=wheelIndexHourLength;
                    wheelLengthNext=wheelIndexDayLength;
                    break;
                case MINUTES:
                    allTicksNext=wheelHourAllTicks;
                    vectorsNext=wheelHour;
                    index=wheelIndexMinute;
                    indexNext=wheelIndexHour;
                    wheelLength=wheelIndexMinuteLength;
                    wheelLengthNext=wheelIndexHourLength;
                    break;
                case SECONDS:
                    allTicksNext=wheelMinuteAllTicks;
                    vectorsNext=wheelMinute;
                    index=wheelIndexSecond;
                    indexNext=wheelIndexMinute;
                    wheelLength=wheelIndexSecondLength;
                    wheelLengthNext=wheelIndexMinuteLength;
                    break;
                case MILLISECONDS:
                    allTicksNext=wheelSecondAllTicks;
                    vectorsNext=wheelSecond;
                    index=wheelIndexMillisecond;
                    indexNext=wheelIndexSecond;
                    wheelLength=wheelIndexMillisecondLength;
                    wheelLengthNext=wheelIndexSecondLength;
                    break;
                default:
                    throw new RuntimeException("timeUnit 参数错误");
            }

            index.getAndIncrement();
            if(index.get()<wheelLength){
                return true;
            }
            index.set(index.get()%wheelLength);
            if(timeUnit.equals(TimeUnit.DAYS)){
                return true;
            }

            List<TimeWheelTask> taskList = vectorsNext[(indexNext.get()+1)%wheelLengthNext];
            if(null!=taskList){
                for (TimeWheelTask task : taskList) {
                    addTaskToWheel(task.delay%(allTicksNext*TICK),task);
                }
                taskList.clear();
            }

            return false;
        }

        public void incIndex(){
            if(incIndex(TimeUnit.MILLISECONDS)){
                return;
            }

            if(incIndex(TimeUnit.SECONDS)){
                return;
            }

            if(incIndex(TimeUnit.MINUTES)){
                return;
            }

            if(incIndex(TimeUnit.HOURS)){
                return;
            }

            incIndex(TimeUnit.DAYS);
        }

        public List<TimeWheelTask> getTimeWheelTaskList(){
            return wheelMillisecond[wheelIndexMillisecond.get()];
        }


        /**
         * 获取时间轮 当前所有指针相对delay 转过的时间
         * @return
         */
        public long getWheelNowTime(long delay){
            long timeFromWheelStart=(wheelIndexDay.get()*wheelDayAllTicks+wheelIndexHour.get()*wheelHourAllTicks+wheelIndexMinute.get()*wheelMinuteAllTicks
                    +wheelIndexSecond.get()*wheelSecondAllTicks+wheelIndexMillisecond.get()*wheelMillisecondAllTicks)*TICK;
            if(0!=delay/(wheelDayAllTicks*TICK)){
                return timeFromWheelStart%(wheelDayAllTicks*TICK);
            }

            if(0!=delay/(wheelHourAllTicks*TICK)){
                return timeFromWheelStart%(wheelHourAllTicks*TICK);
            }

            if(0!=delay/(wheelMinuteAllTicks*TICK)){
                return timeFromWheelStart%(wheelMinuteAllTicks*TICK);
            }

            if(0!=delay/(wheelSecondAllTicks*TICK)){
                return timeFromWheelStart%(wheelSecondAllTicks*TICK);
            }

            return 0;
        }

        public void addTaskToWheel(long delay, TimeWheelTask timeWheelTask){
            if(delay>=wheelIndexDayLength*wheelDayAllTicks*TICK){
                throw new RuntimeException("delay 超过一年，真的有必要吗？");
            }

            if(addTaskToWheel(delay,timeWheelTask,TimeUnit.DAYS)){
                return;
            }

            if(addTaskToWheel(delay,timeWheelTask,TimeUnit.HOURS)){
                return;
            }

            if(addTaskToWheel(delay,timeWheelTask,TimeUnit.MINUTES)){
                return;
            }

            if(addTaskToWheel(delay,timeWheelTask,TimeUnit.SECONDS)){
                return;
            }

            addTaskToWheel(delay,timeWheelTask,TimeUnit.MILLISECONDS);
        }


        private boolean addTaskToWheel(long delay, TimeWheelTask timeWheelTask, TimeUnit timeUnit){
            long allTicks;
            Vector[] vectors;
            AtomicInteger index;
            int wheelLength;
            switch (timeUnit){
                case DAYS:
                    allTicks=wheelDayAllTicks;
                    vectors= wheelDay;
                    index=wheelIndexDay;
                    wheelLength=wheelIndexDayLength;
                    break;
                case HOURS:
                    allTicks=wheelHourAllTicks;
                    vectors=wheelHour;
                    index=wheelIndexHour;
                    wheelLength=wheelIndexHourLength;
                    break;
                case MINUTES:
                    allTicks=wheelMinuteAllTicks;
                    vectors=wheelMinute;
                    index=wheelIndexMinute;
                    wheelLength=wheelIndexMinuteLength;
                    break;
                case SECONDS:
                    allTicks=wheelSecondAllTicks;
                    vectors=wheelSecond;
                    index=wheelIndexSecond;
                    wheelLength=wheelIndexSecondLength;
                    break;
                case MILLISECONDS:
                    allTicks=wheelMillisecondAllTicks;
                    vectors=wheelMillisecond;
                    index=wheelIndexMillisecond;
                    wheelLength=wheelIndexMillisecondLength;
                    break;
                default:
                    throw new RuntimeException("timeUnit 参数错误");
            }

            if(0!=delay/(allTicks*TICK)||timeUnit.equals(TimeUnit.MILLISECONDS)){
                int indexNew=(index.get()+(int)(delay/(allTicks*TICK)))%wheelLength;
                if(null==vectors[indexNew]){
                    vectors[indexNew]=new Vector();
                }
                vectors[indexNew].add(timeWheelTask);
                return true;
            }
            return false;
        }
    }


    /**
     * 时间轮的task
     */
    class TimeWheelTask implements Runnable{
        private long delay;
        private long period;
        private Runnable runnable;

        public void setDelay(long delay) {
            this.delay = delay;
        }

        TimeWheelTask(long delay, long period, Runnable runnable){
            this.delay=delay;
            this.period=period;
            this.runnable=runnable;
        }

        /**
         * 判断是否是周期性的调度任务
         * @return
         */
        public boolean isPeriodSchedule(){
            return period>0;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    /**
     * 时间轮的 thread
     */
    class TimeWheelThread extends Thread{
        @Override
        public void run() {
            mainLoop();
        }

        private void runTaskList(List<TimeWheelTask>list){
            if(null==list||0==list.size()){
                return;
            }
            for (TimeWheelTask task : list) {
                executorService.execute(task);
                if(task.isPeriodSchedule()){
                    task.setDelay(task.period);
                    schedule(task);
                }
            }
            list.clear();
        }

        private void mainLoop(){
            while(true){
                runTaskList(wheel.getTimeWheelTaskList());
                wheel.incIndex();
                try {
                    Thread.sleep(TimeWheelWheel.TICK);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
