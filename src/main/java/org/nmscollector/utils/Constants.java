package org.nmscollector.utils;

public class Constants
{
    public static final String RUN_API_RESULT = "/run/result/:id";

    private Constants() {
        throw new IllegalStateException("Constant class");
    }


    public static final String  DEFAULT_POLL_TIME = "default.poll.time";

    public static final String PLUGIN_PROCESS_TIMEOUT =   "plugin.process.timeout";

    public static final Integer BAD_REQUEST = 400;

    public static final String USER_DIRECTORY = "user.dir";

    public static final String CONFIG_FILE = "/config/config.json";

    public static final String CONFIG_PATH = System.getProperty(USER_DIRECTORY) + CONFIG_FILE;

    public static final String IP = "ip";

    public static final String STATUS = "status";

    public static final String FAILED = "failed";

    public static final String ERROR = "error";

    public static final String ERROR_CODE = "error.code";

    public static final String ERROR_MESSAGE = "error.message";

    public static final String PLUGIN_APPLICATION_PATH = "/PluginEngine/bootstrap";

    public static final String UNIQUE_SEPARATOR  = "~@@~";

    public static final String PUBLISHER_PORT = "publisher.zmq.port";

    public static final String ZMQ_ADDRESS  = "tcp://localhost:";

    public static final String RECEIVER_PORT = "receiver.zmq.port";

}