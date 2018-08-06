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

    private static Client client = null;

    public static synchronized Client getEtclClient() {
        if (null == client) {
            client = Client.builder().endpoints("http://10.9.193.121:2379", "http://10.9.193.135:2379", "http://10.9.193.136:2379").build();
        }
        return client;
    }

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

    /**
     * 根据指定的配置名称获取对应的value
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
     * @param key
     * @param value
     * @return
     */
    public static void put(String key, String value) throws Exception {
        EtcdUtil.getEtclClient().getKVClient().put(ByteSequence.fromString(key), ByteSequence.fromBytes(value.getBytes("utf-8")));

    }

    /**
     * 删除指定的配置
     * @param key
     * @return
     */
    public static void del(String key) {
        EtcdUtil.getEtclClient().getKVClient().delete(ByteSequence.fromString(key));

    }

    interface ListenCallBack{
        void run(String data);
    }

    interface CallBack{
        void run(WatchEvent watchEvent);
    }
}

