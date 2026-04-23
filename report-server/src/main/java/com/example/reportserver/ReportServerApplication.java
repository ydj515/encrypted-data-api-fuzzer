package com.example.reportserver;

import com.example.reportserver.cli.KaratePublishCli;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class ReportServerApplication {

    public static void main(String[] args) {
        if (args.length > 0 && KaratePublishCli.COMMAND.equals(args[0])) {
            KaratePublishCli.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        SpringApplication.run(ReportServerApplication.class, args);
    }
}
