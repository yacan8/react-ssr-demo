package com.tool;


import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NashornHelper {

    /**
     * 用于本类的日志
     */
    private static final Logger       logger                       = LoggerFactory.getLogger(NashornHelper.class);

    private static final String       JAVASCRIPT_DIR               = "static";

    private static final String       LIB_DIR                      = JAVASCRIPT_DIR + File.separator + "lib";

    private static final String[]     VENDOR_FILE_NAME                = {"vendor.js"};

    private static final String       SRC_DIR                   = JAVASCRIPT_DIR + File.separator + "src";

    private static final String       POLYFILL_FILE_NAME           = "nashorn-polyfill.js";

    private final NashornScriptEngine engine;

    private static NashornHelper      nashornHelper;

    private static ScriptContext            sc                        = new SimpleScriptContext();

    private static ScheduledExecutorService globalScheduledThreadPool = Executors.newScheduledThreadPool(20);


    public static synchronized NashornHelper getInstance() {
        if (nashornHelper == null) {
            long start = System.currentTimeMillis();

            nashornHelper = new NashornHelper();

            long end = System.currentTimeMillis();

            logger.error("init nashornHelper cost time {}ms", (end - start));
        }

        return nashornHelper;
    }

    public NashornHelper(){
        long start = System.currentTimeMillis();
        engine = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
        Bindings bindings = new SimpleBindings();
        bindings.put("logger", logger);

        sc.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        sc.getBindings(ScriptContext.ENGINE_SCOPE).putAll(bindings);
        sc.setAttribute("__IS_SSR__", true, ScriptContext.ENGINE_SCOPE);
        sc.setAttribute("__NASHORN_POLYFILL_TIMER__", globalScheduledThreadPool, ScriptContext.ENGINE_SCOPE);
        engine.setBindings(sc.getBindings(ScriptContext.ENGINE_SCOPE), ScriptContext.ENGINE_SCOPE);

        long end = System.currentTimeMillis();
        logger.info("init nashornHelper cost time {}ms", (end - start));

        try {
            engine.eval(read(LIB_DIR + File.separator + POLYFILL_FILE_NAME));
            for (String fileName : NashornHelper.VENDOR_FILE_NAME) {
                engine.eval(read(SRC_DIR + File.separator + fileName));
            }
            engine.eval(read(SRC_DIR + File.separator + "app.js"));
        } catch (ScriptException e) {
            logger.error("load javascript failed.", e);
        }
    }

    public ScriptObjectMirror getGlobalGlobalMirrorObject(String objectName) {
        return (ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE).get(objectName);
    }

    public Object callRender(String methodName, Object... input) {
        try {
            return engine.invokeFunction(methodName, input);
        } catch (ScriptException e) {
            logger.error("run javascript failed.", e);
            return null;
        } catch (NoSuchMethodException e) {
            logger.error("no such method.", e);
            return null;
        }
    }

    private Reader read(String path) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(path);
        return new InputStreamReader(in);
    }
}