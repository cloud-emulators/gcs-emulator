package io.github.cloudemulators.gcsemulator.lifecycle;

import java.io.IOException;

public class Checker {
    public static void main(String[] args) throws IOException {
        CommandSender.sendCommand(CommandSender.Command.WAIT_TILL_READY);
    }
}
