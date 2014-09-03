package com.solomonson.example.h2bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by randy.solomonson on 9/3/2014.
 */
public class Activator implements BundleActivator{
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Activator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("hello World");

        //Start H2
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:./data/h2data");
        conn.close();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Bye World");
    }
}
