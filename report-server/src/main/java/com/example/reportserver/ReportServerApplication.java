package com.example.reportserver;

import com.example.reportserver.cli.CatsPublishCli;
import com.example.reportserver.cli.KaratePublishCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class ReportServerApplication {

    public static void main(String[] args) {
        if (args.length > 0) {
            String command = args[0];
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            if (KaratePublishCli.COMMAND.equals(command)) {
                KaratePublishCli.main(rest);
                return;
            }
            if (CatsPublishCli.COMMAND.equals(command)) {
                CatsPublishCli.main(rest);
                return;
            }
        }
        SpringApplication.run(ReportServerApplication.class, args);
    }
}
