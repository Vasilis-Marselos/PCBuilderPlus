package com.pcbuilder;

import me.friwi.jcefmaven.*;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;

import java.io.File;




public final class ChromiumBrowserManager {

    private static final Object LOCK = new Object();
    private static volatile CefApp cefApp;

    private ChromiumBrowserManager() {
    }

    public static CefApp getOrCreateApp() throws Exception {
        if (cefApp != null) {
            return cefApp;
        }
        synchronized (LOCK) {
            if (cefApp != null) {
                return cefApp;
            }

            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(new File("jcef-bundle"));
            builder.setProgressHandler(new ConsoleProgressHandler());
            builder.getCefSettings().windowless_rendering_enabled = false;
            builder.addJcefArgs("--disable-gpu");
            builder.addJcefArgs("--disable-gpu-compositing");
            builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");
            builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            });

            cefApp = builder.build();
            return cefApp;
        }
    }
}
