package com.zdc.java.script;


import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhudongchang on 2018/4/17.
 */
@Ignore
public class TimeWheelTest {

    @Before
    public void before(){
    }

    @Test
    public void timer() throws InterruptedException {
        Timer time=new Timer();
        System.out.println("      start:"+new Date());
        time.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("task1 start:"+new Date());

                throw new RuntimeException("aaa");
//                try {
//                    Thread.sleep(4000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        },1000);


        time.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("task2 start:"+new Date());
            }
        },2000);

        Thread.sleep(Integer.MAX_VALUE);
    }


    @Test
    public void ScheduledExecutorService() throws InterruptedException {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(2);
        System.out.println("      start:"+new Date());

        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task1 start:"+new Date());
                throw new RuntimeException("aaa");
//                try {
//                    Thread.sleep(4000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        },1000, TimeUnit.MILLISECONDS);

        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task2 start:"+new Date());
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },2000, TimeUnit.MILLISECONDS);


        Thread.sleep(Integer.MAX_VALUE);
    }


    @Test
    public void TimeWheel() throws InterruptedException {
        TimeWheelService instance = TimeWheelService.instance;
        System.out.println("      start:"+new Date());
        instance.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task1 start:"+new Date());
//                throw new RuntimeException("aaa");
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },1000);

        instance.schedule(new Runnable() {
            @Override
            public void run() {
                System.out.println("task2 start:"+new Date());
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        },2000);


        Thread.sleep(Integer.MAX_VALUE);
    }
}
