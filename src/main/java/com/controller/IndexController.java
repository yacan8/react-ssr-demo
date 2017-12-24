package com.controller;


import com.tool.NashornHelper;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.function.Consumer;

@RestController
public class IndexController {

    private final Object     promiseLock     = new Object();
    private volatile boolean promiseResolved = false;
    private String           html            = null;

    private Consumer<Object> fnResolve       = object -> {
        synchronized (promiseLock) {
            html = (String) object;
            promiseResolved = true;
        }
    };

    @RequestMapping("/")
    public ModelAndView hello() {
        NashornHelper engine = NashornHelper.getInstance();
        ScriptObjectMirror promise = (ScriptObjectMirror) engine.callRender("ssr_render");
        promise.callMember("then", fnResolve);
        ScriptObjectMirror nashornEventLoop = engine.getGlobalGlobalMirrorObject("nashornEventLoop");

        nashornEventLoop.callMember("process");
        int i = 0;
        int jsWaitTimeout = 1000 * 60;
        int interval = 200;
        int totalWaitTime = 0;
        while (!promiseResolved && totalWaitTime < jsWaitTimeout) {
            nashornEventLoop.callMember("process");
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
            }
            totalWaitTime = totalWaitTime + interval;
            if (interval < 500) interval = interval * 2;
            i = i + 1;
        }
        ModelAndView mav = new ModelAndView("index");
        mav.addObject("html", html);
        return mav;
    }

    @RequestMapping("/getTodolist")
    public String getTodolist() {
        return "{\"data\":[{\"name\":\"a1\",\"state\":\"computed\"},{\"name\":\"a2\",\"state\":\"computed\"}],\"success\":true}";
    }
}
