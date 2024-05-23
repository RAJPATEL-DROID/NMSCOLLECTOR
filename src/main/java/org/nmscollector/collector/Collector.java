package org.nmscollector.collector;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.nmscollector.Bootstrap;
import org.nmscollector.utils.Constants;
import org.nmscollector.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Base64;

public class Collector extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(Collector.class);

    @Override
    public void start(Promise<Void> startPromise)
    {

        ZContext zContext = new ZContext();

        ZMQ.Socket poller = zContext.createSocket(SocketType.PULL);

        poller.connect(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.HOST_IP)  + Constants.COLON +  Utils.config.get(Constants.RECEIVER_PORT));

        ZMQ.Socket sender = zContext.createSocket(SocketType.PUSH);

        sender.connect(Constants.ZMQ_ADDRESS + Utils.config.get(Constants.HOST_IP)  + Constants.COLON + Utils.config.get(Constants.PUBLISHER_PORT));

        long pollTime = Long.parseLong(Utils.config.get(Constants.POLL_TIME).toString()) * 1000;

        logger.trace("Default Poll time set to {} ", pollTime);

        Vertx vertx = Bootstrap.getVertx();

        vertx.setPeriodic(pollTime, tid ->
        {
            var context = poller.recvStr(ZMQ.DONTWAIT);

            if(context == null)
            {
                logger.trace("No Context Received From Server");
            }
            else
            {
                var pollingArray = new JsonArray(context);

                logger.trace(pollingArray.encodePrettily());

                vertx.executeBlocking(future ->
                {

                    try
                    {
                        // Check Availability
                        Utils.checkAvailability(pollingArray);

                        logger.trace("Polling array : {}", pollingArray);

                        String encodedContext = Base64.getEncoder().encodeToString(pollingArray.toString().getBytes());

                        var replyJson = Utils.spawnPluginEngine(encodedContext, pollingArray.size());

                        if (replyJson == null)
                        {
                            logger.info("Plugin engine failed to spawn");
                            future.fail("Plugin engine failed to spawn");
                        }
                        else
                        {
                            logger.trace("Polling completed {}", replyJson);

                            sender.send(replyJson.toString(), ZMQ.DONTWAIT);

                            future.complete();

                        }

                    }
                    catch (Exception exception)
                    {
                        logger.error("Exception in SetPeriodic");

                        logger.error(exception.getMessage());

                        logger.error(exception.toString());
                    }
                });
            }
        });

        startPromise.complete();
    }

}
