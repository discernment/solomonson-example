package com.solomonson.example.hellobundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Created by randy.solomonson on 9/3/2014.
 */
public class Activator implements BundleActivator{
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("hello World");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Bye World");
    }
}
