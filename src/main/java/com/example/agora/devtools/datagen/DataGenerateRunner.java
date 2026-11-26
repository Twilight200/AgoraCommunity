package com.example.agora.devtools.datagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Component
@Profile("datagen")
public class DataGenerateRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataGenerateRunner.class);

    @Override
    public void run(ApplicationArguments args){
        log.info("Datagen profile is active. No data has been written yet.");
    }
}
