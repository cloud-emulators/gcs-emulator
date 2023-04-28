package io.github.cloudemulators.gcsemulator.lifecycle;

import java.io.IOException;

public class Stopper {
    public static void main(String[] args) throws IOException {
        CommandSender.sendCommand(CommandSender.Command.CLOSE);
    }
}
