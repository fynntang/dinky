package com.dlink.daemon.task;

import com.dlink.daemon.constant.FlinkTaskConstant;
import com.dlink.daemon.pool.DefaultThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

public class DaemonFactory {
    private static final Logger log = LoggerFactory.getLogger(DaemonFactory.class);

    public static void start(List<DaemonTaskConfig> configList){
        Thread thread = new Thread(() -> {
            DefaultThreadPool defaultThreadPool =  DefaultThreadPool.getInstance();
            for (DaemonTaskConfig config: configList) {
                DaemonTask daemonTask = DaemonTask.build(config);
                defaultThreadPool.execute(daemonTask);
            }
            while (true) {
                int taskSize = defaultThreadPool.getTaskSize();
                try {
                    Thread.sleep(Math.max(FlinkTaskConstant.MAX_POLLING_GAP / (taskSize + 1),  FlinkTaskConstant.MIN_POLLING_GAP));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int num = taskSize / 100 + 1;
                if (defaultThreadPool.getWorkCount() < num) {
                    defaultThreadPool.addWorkers(num - defaultThreadPool.getWorkCount() );
                }else if(defaultThreadPool.getWorkCount() > num) {
                    defaultThreadPool.removeWorker(defaultThreadPool.getWorkCount() - num);
                }
                log.info(" >>> taskSize:"  + taskSize +  " workCount: "+ defaultThreadPool.getWorkCount());
            }
        });
        thread.start();
    }

    public static void addTask(DaemonTaskConfig config){
        DefaultThreadPool.getInstance().execute(DaemonTask.build(config));
    }
}
