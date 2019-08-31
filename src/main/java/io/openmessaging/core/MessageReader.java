package io.openmessaging.core;

import io.openmessaging.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MessageReader {
    private static FileChannel aChannel;
    private static FileChannel bodyChannel;
    private static ThreadLocal<ByteBuffer> aLocalBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(1024 * 1024));
    private static ThreadLocal<ByteBuffer> bodyLocalBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(1024 * 1024));

    static {
        try {
            aChannel = FileChannel.open(Paths.get(Constants.A_Path), StandardOpenOption.CREATE, StandardOpenOption.READ);
            bodyChannel = FileChannel.open(Paths.get(Constants.Body_Path), StandardOpenOption.READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ByteBuffer readA(ByteBuffer buffer, long tMin, long tMax) throws Exception {
        long messageStart = PartitionIndex.getAStart(tMin);
        long messageEnd = PartitionIndex.getAEnd(tMax);
        ByteBuffer byteBuffer = aLocalBuffer.get();

        return adaptiveRead(byteBuffer, aChannel, messageStart, messageEnd - messageStart);

    }

    public static ByteBuffer readBody(ByteBuffer buffer, long tMin, long tMax) throws Exception {
        long messageStart = PartitionIndex.getBodyStart(tMin);
        long messageEnd = PartitionIndex.getBodyEnd(tMax);
        ByteBuffer byteBuffer = bodyLocalBuffer.get();

        return adaptiveRead(byteBuffer, bodyChannel, messageStart, messageEnd - messageStart);

    }

    public static ByteBuffer fastRead(ByteBuffer buffer, long tMin, long tMax) throws Exception {
        long aStart = PartitionIndex.getAStart(tMin);
        long aEnd = PartitionIndex.getAEnd(tMax);
        ByteBuffer byteBuffer = aLocalBuffer.get();

        return adaptiveRead(byteBuffer, aChannel, aStart, aEnd - aStart);

    }

    //private static Semaphore semaphore = new Semaphore(1);
    private static ByteBuffer adaptiveRead(ByteBuffer byteBuffer, FileChannel channel, long start, long length) throws Exception {
        if (length > 1024 * 1024) {
            System.out.println("mmap:\t" + length);
            return channel.map(FileChannel.MapMode.READ_ONLY, start, length);
        } else {
//            semaphore.acquire();
            long readStart = System.currentTimeMillis();
            byteBuffer.clear();
            byteBuffer.limit((int) length);
            channel.read(byteBuffer, start);
            byteBuffer.flip();
            System.out.println("rt:\t" + (System.currentTimeMillis() - readStart) + "\trl:\t" + length + "\trs:\t" + start);
//            semaphore.release();
            return byteBuffer;
        }
    }

//    private static ByteBuffer asyncRead(ByteBuffer buffer, AsynchronousFileChannel channel, long start, long length) throws Exception {
//        long readStart = System.currentTimeMillis();
//        synchronized (buffer) {
//            buffer.limit((int) length);
//            channel.read(buffer, start, buffer, new WriteCompletionHandler());
//            channel.read(buffer, start);
//            buffer.wait();
//        }
//        System.out.println("rb:\t" + length);
//
//        System.out.println("rt:\t" + (System.currentTimeMillis() - readStart));
//        return;
//
//    }

    private static class WriteCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {
        @Override
        public void completed(Integer result, ByteBuffer buffer) {
            synchronized (buffer) {
                System.out.println("rb:\t" + result);
                buffer.notify();
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {
            exc.printStackTrace();
        }
    }
}