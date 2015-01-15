/**
 * Tomitribe Confidential
 * <p/>
 * Copyright(c) Tomitribe Corporation. 2014
 * <p/>
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 * <p/>
 */
package com.tomitribe.watchdog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class SimpleWatchdog implements Runnable {

    public static String[] args;

    public static void main(final String[] args) {

        SimpleWatchdog.args = args;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // TODO - Cleanup
            }

        });

        final Thread t = new Thread(new SimpleWatchdog(), "SimpleWatchdog");
        t.start();

        try {
            System.in.read();
        } catch (final IOException e) {
            System.exit(-1);
        }

        t.interrupt();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        try {
            final WatchService watcher = FileSystems.getDefault().newWatchService();

            final URI uri = new File(System.getProperty("user.dir")).toURI();
            System.out.println("Watching uri = " + uri);

            final Path dir = Paths.get(uri);
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            for (; ; ) {

                final WatchKey key;
                try {
                    key = watcher.take();
                } catch (final InterruptedException x) {
                    return;
                }


                for (final WatchEvent<?> event : key.pollEvents()) {
                    try {
                        final WatchEvent.Kind<?> kind = event.kind();

                        if (kind == OVERFLOW) {
                            continue;
                        }

                        final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        final Path filename = ev.context();

                        try {
                            final Path child = dir.resolve(filename);
                            if (Files.probeContentType(child).equals("text/plain")) {
                                System.out.format("Process file %s%n", filename);
                            } else {
                                System.err.format("File '%s' is not a plain text file.%n", filename);
                            }
                        } catch (final IOException x) {
                            System.err.println(x);
                        }

                    } finally {
                        key.reset();
                    }
                }

            }

        } catch (final IOException e) {
            e.printStackTrace();
        }

    }
}
