package org.nmscollector;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.nmscollector.collector.Collector;
import org.nmscollector.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Bootstrap
{

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static final Vertx vertx = Vertx.vertx();

    public static Vertx getVertx()
    {
        return vertx;
    }

    public static void main(String[] args)
    {
        logger.info("Starting Backend Server...");


        Utils.readConfig().onComplete(result ->
        {
            if (result.succeeded())
            {
                vertx.deployVerticle(Collector.class.getName())
                    .onComplete(status ->
                    {
                        if (status.succeeded())
                        {
                            logger.info("Collector started successfully...");
                        }
                        else
                        {
                            logger.error("Failed to start Collector", status.cause());
                        }
                    });
            }
            else
            {
                logger.error("Failed to start backend server", result.cause());
            };
        });
    }
}
