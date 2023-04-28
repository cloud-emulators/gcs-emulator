package io.github.cloudemulators.gcsemulator.lifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class CommandSender {
    private static final int SLEEP_PERIOD = 100;
    public static void sendCommand(Command command) {
        while (true) {
            try {
                SocketChannel server = SocketChannel.open();
                SocketAddress socketAddr = new InetSocketAddress("localhost", 55555);
                server.connect(socketAddr);

                ByteBuffer buffer = ByteBuffer.wrap(command.toString().getBytes(StandardCharsets.UTF_8));
                server.write(buffer);
                server.close();
                break;
            } catch (IOException e) {
                try {
                    TimeUnit.MILLISECONDS.sleep(SLEEP_PERIOD);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    enum Command {
        CLOSE,
        WAIT_TILL_READY,
    }
}
