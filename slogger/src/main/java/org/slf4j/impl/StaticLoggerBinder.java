package org.slf4j.impl;

import org.slf4j.spi.LoggerFactoryBinder;
import org.slf4j.ILoggerFactory;
import org.ukenergywatch.slogger.impl.LoggerFactory;

public class StaticLoggerBinder implements LoggerFactoryBinder {

    private static final StaticLoggerBinder instance = new StaticLoggerBinder();

    private final ILoggerFactory loggerFactory;
    private static final String loggerFactoryClassStr = LoggerFactory.class.getName();

    private StaticLoggerBinder() {
	loggerFactory = new LoggerFactory();
    }

    public static final StaticLoggerBinder getSingleton() {
	return instance;
    }

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
	return loggerFactoryClassStr;
    }

}

