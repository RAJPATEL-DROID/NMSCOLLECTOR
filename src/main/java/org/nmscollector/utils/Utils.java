package org.nmscollector.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.nmscollector.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


public class Utils {

    private Utils(){
        throw new IllegalStateException("Utils class");
    }

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static ConcurrentMap<String,Object> config= new ConcurrentHashMap<>();

    public static Future<Void> readConfig()
    {
        Promise<Void> promise = Promise.promise();
        try
        {
            Vertx vertx = Bootstrap.getVertx();

            vertx.fileSystem().readFile(Constants.CONFIG_PATH, handler ->
            {
                try
                {
                    if (handler.succeeded())
                    {
                        var data = handler.result().toJsonObject();

                        for (var key : data.fieldNames())
                        {
                            config.put(key, data.getValue(key));
                        }

                        logger.info("Config File Read Successfully...");
                        logger.info(config.toString());
                        promise.complete();
                    }
                    else
                    {
                        logger.error("Error Occurred reading the config file :  ",handler.cause());

                        promise.fail(handler.cause());
                    }

                }
                catch (Exception exception)
                {
                    logger.error("Error Occurred reading the config file :  ",exception);
                }
            });
        }
        catch (Exception exception)
        {
           logger.error("error reading config file {}", exception.getMessage());

           logger.error(Arrays.toString(exception.getStackTrace()));

           promise.fail("Exception Occurred in Reading Config File");
        }
        return promise.future();
    }

    public static boolean checkAvailability(String ip)
    {

        ProcessBuilder processBuilder = new ProcessBuilder("fping", "-c", "3", "-q", ip);

        processBuilder.redirectErrorStream(true);
        try
        {
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null)
            {
                if (line.contains("/0%"))
                {
                    logger.info("Device with IP address {} is up", ip);

                    return true;
                }
                else
                {
                    logger.info("Device with IP address {} is down", ip);
                }
            }

        } catch (Exception exception)
        {
            logger.error(exception.getMessage());

            return false;

        }
        return false;

    }

    public static void checkAvailability(JsonArray pollingArray)
    {
        for(var element : pollingArray)
        {
            var discoveryInfo = new JsonObject(element.toString());

            if (!Utils.checkAvailability(discoveryInfo.getString(Constants.IP) ) )
            {
                pollingArray.remove(element);
            }

        }
    }

    public static JsonArray spawnPluginEngine(String encodedString, Integer size)
    {
        try
        {
            var currentDir = System.getProperty(Constants.USER_DIRECTORY);

            var processBuilder = new ProcessBuilder(currentDir + Constants.PLUGIN_APPLICATION_PATH, encodedString);

            processBuilder.redirectErrorStream(true);

            var process = processBuilder.start();

            logger.info(String.valueOf(TimeUnit.SECONDS.toNanos(Long.parseLong(Utils.config.get(Constants.PLUGIN_PROCESS_TIMEOUT).toString()))));

            var exitStatus = process.waitFor(Long.parseLong(Utils.config.get(Constants.PLUGIN_PROCESS_TIMEOUT).toString()), TimeUnit.SECONDS);

            if(!exitStatus)
            {

                process.destroyForcibly();

                logger.error("Process Timed out, Killed Forcibly!!");

                return null;

            }

            // Read the output of the command
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            var buffer = Buffer.buffer();

            String line;

            var count = 0;

            while ((line = reader.readLine()) != null && count <= size) {
                buffer.appendString(line);
                if (line.contains(Constants.UNIQUE_SEPARATOR)) {
                    count++;
                }
            }

            logger.info("Context Received from the Plugin {} ", buffer);

            var contexts = buffer.toString().split(Constants.UNIQUE_SEPARATOR);

            var replyJson = new JsonArray();

            for (var context : contexts)
            {

                byte[] decodedBytes = Base64.getDecoder().decode(context);

                var decodedString = new String(decodedBytes);

                logger.info("Received Data from Device : {}" ,decodedString);

                replyJson.add(new JsonObject(decodedString));

            }

            return replyJson;

        }
        catch (Exception exception)
        {
            logger.error(exception.getMessage());

            logger.error(Arrays.toString(exception.getStackTrace()));

            var error = new JsonArray();

            var response = new JsonObject();

            response.put(Constants.ERROR, "Discovery Run Request failed")

                    .put(Constants.ERROR_CODE, Constants.BAD_REQUEST)

                    .put(Constants.ERROR_MESSAGE,"Exception Occurred during Spawning Process Builder" )

                    .put(Constants.STATUS, Constants.FAILED);

            error.add(response);

            return error;

        }
    }
}

