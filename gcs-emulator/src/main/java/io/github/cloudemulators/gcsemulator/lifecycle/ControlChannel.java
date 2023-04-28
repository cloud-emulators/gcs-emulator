package io.github.cloudemulators.gcsemulator.lifecycle;

import io.github.cloudemulators.gcsemulator.GCSServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ControlChannel {
    private final ExecutorService executorService;
    private final List<GCSServer> servers;

    public ControlChannel(ExecutorService executorService, List<GCSServer> servers) {
        this.executorService = executorService;
        this.servers = servers;
    }

    public void start() throws IOException {
        SocketChannel client;
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(55555));
        while(true) {
            client = serverSocket.accept();
            System.out.println("Connection Set:  " + client.getRemoteAddress());
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int read;
            while ((read = client.read(buffer))> 0) {
                buffer.flip();
                byte[] bytes = new byte[read];
                buffer.get(bytes);
                String recievedString = new String(bytes, StandardCharsets.UTF_8);
                try {
                    CommandSender.Command command = CommandSender.Command.valueOf(recievedString);
                    switch (command) {
                        case CLOSE:
                            servers.forEach(GCSServer::stop);
                            executorService.shutdownNow();
                            return;
                        case WAIT_TILL_READY:
                            //Do nothing just break;
                            //Just the fact that we are here means the servers are started
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    //Unknown Command continue
                }
            }
            client.close();
        }
    }
}
