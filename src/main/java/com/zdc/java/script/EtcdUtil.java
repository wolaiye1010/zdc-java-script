package com.zdc.java.script;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;

import java.util.List;
import java.util.concurrent.*;

/**
 * Created by zhudongchang on 2018/8/3.
 */
public class EtcdUtil {

    interface ListenCallBack{
        void run(String data);
    }

    interface CallBack{
        void run(WatchEvent watchEvent);
    }

    public static void main(String[] args) throws Exception {
        String key="/zdc/test1";
        String getRes = get(key);
        System.out.println(getRes);
        listen(key, new ListenCallBack() {
            @Override
            public void run(String data) {
                System.out.println(data);
            }
        });

//        listen(key, new CallBack() {
//            @Override
//            public void run(WatchEvent watchEvent) {
//                KeyValue keyValue = watchEvent.getKeyValue();
//                System.out.println("task2:");
//                System.out.println(String.format("event:%s,key:%s,value:%s",watchEvent.getEventType()
//                        ,keyValue.getKey().toStringUtf8(),keyValue.getValue().toStringUtf8()));
//            }
//        });
        System.out.println(111);
    }

//    private static final ExecutorService executorService=new ThreadPoolExecutor(2, 2,
//                0L,TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<Runnable>(10000));

    private static final ExecutorService executorService= new ThreadPoolExecutor(0,
            10000,0L, TimeUnit.SECONDS,new SynchronousQueue<>());

    public static void listen(String path,ListenCallBack listenCallBack) {
        listen(path, new CallBack() {
            @Override
            public void run(WatchEvent watchEvent) {
                listenCallBack.run(watchEvent.getKeyValue().getValue().toStringUtf8());
            }
        });
    }


    public static void listen(String path,CallBack callBack) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Watch.Watcher watcher = EtcdUtil.getEtclClient().getWatchClient().watch(ByteSequence.fromString(path));
                try {
                    for (WatchEvent watchEvent : watcher.listen().getEvents()) {
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                callBack.run(watchEvent);
                                listen(path,callBack);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    //etcl客户端链接
    private static Client client = null;

    //链接初始化
    public static synchronized Client getEtclClient() {
        if (null == client) {
            client = Client.builder().endpoints("http://10.9.193.121:2379", "http://10.9.193.135:2379", "http://10.9.193.136:2379").build();
        }
        return client;
    }

    /**
     * 根据指定的配置名称获取对应的value
     *
     * @param key 配置项
     * @return
     * @throws Exception
     */
    public static String get(String key) throws Exception {
        List<KeyValue> kvs = EtcdUtil.getEtclClient().getKVClient().get(ByteSequence.fromString(key)).get().getKvs();
        if (kvs.size() > 0) {
            String value = kvs.get(0).getValue().toStringUtf8();
            return value;
        } else {
            return null;
        }
    }

    /**
     * 新增或者修改指定的配置
     *
     * @param key
     * @param value
     * @return
     */
    public static void put(String key, String value) throws Exception {
        EtcdUtil.getEtclClient().getKVClient().put(ByteSequence.fromString(key), ByteSequence.fromBytes(value.getBytes("utf-8")));

    }

    /**
     * 删除指定的配置
     *
     * @param key
     * @return
     */
    public static void del(String key) {
        EtcdUtil.getEtclClient().getKVClient().delete(ByteSequence.fromString(key));

    }


    //V3 api配置初始化和监听
    public void init() {
        try {
            //加载配置
//            getConfig(EtcdUtil.getEtclClient().getKVClient().get(ByteSequence.fromString("ETCD_CONFIG_FILE_NAME")).get().getKvs());
//启动监听线程
            new Thread(() -> {
//对某一个配置进行监听
                Watch.Watcher watcher = EtcdUtil.getEtclClient().getWatchClient().watch(ByteSequence.fromString("etcd_key"));
                try {
                    while (true) {
                        watcher.listen().getEvents().stream().forEach(watchEvent -> {
                            KeyValue kv = watchEvent.getKeyValue();
//获取事件变化类型
                            System.out.println(watchEvent.getEventType());
//获取发生变化的key
                            System.out.println(kv.getKey().toStringUtf8());
//获取变化后的value
                            String afterChangeValue = kv.getValue().toStringUtf8();

                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();

        }

    }


    private String getConfig(List<KeyValue> kvs) {
        if (kvs.size() > 0) {
            String config = kvs.get(0).getValue().toStringUtf8();
            System.out.println("etcd 's config 's configValue is :" + config);
            return config;
        } else {
            return null;
        }
    }
}

