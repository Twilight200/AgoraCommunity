package com.example.agora.devtools.datagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;


@Component
@Profile("datagen")
public class DataGenRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataGenRunner.class);
    private final DataGenService dataGenService;

    public DataGenRunner(DataGenService dataGenService){
        this.dataGenService = dataGenService;
    }

    @Override
    public void run(ApplicationArguments args){

        log.info("Starting datagen v0.");

        dataGenService.generate();

        log.info("Datagen v0 completed successfully.");
    }

}
