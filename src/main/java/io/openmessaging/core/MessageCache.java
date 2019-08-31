package io.openmessaging.core;

import io.openmessaging.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCache {
    static ConcurrentHashMap<Long, ByteBuffer> map = new ConcurrentHashMap<>();
    private static FileChannel aChannel;

    static {
        try {
            aChannel = FileChannel.open(Paths.get(Constants.A_Path), StandardOpenOption.CREATE, StandardOpenOption.READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void buildCache() throws Exception {
        long size = 3000000000L;
//        long size = 100000000L;

        NavigableMap<Long, PartitionIndex.PartitionInfo> partitionMap = PartitionIndex.partitionMap;
        Set<Map.Entry<Long, PartitionIndex.PartitionInfo>> entries = partitionMap.entrySet();
        for (Map.Entry<Long, PartitionIndex.PartitionInfo> entry : entries) {
            PartitionIndex.PartitionInfo value = entry.getValue();

            long length = value.aEnd - value.aStart;
            if (size < length) {
                break;
            }
            ByteBuffer buffer = ByteBuffer.allocate((int) length);
            aChannel.read(buffer, value.aStart);
            map.put(entry.getKey(), buffer);
            size -= length;
        }
        System.out.println();
    }

}
