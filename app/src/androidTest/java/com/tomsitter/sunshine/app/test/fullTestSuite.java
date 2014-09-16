package com.tomsitter.sunshine.app.test;

import android.test.suitebuilder.TestSuiteBuilder;

import junit.framework.Test;

/**
 * Created by tom on 12/08/14.
 */
public class fullTestSuite {
    public static Test suite() {
        return new TestSuiteBuilder(fullTestSuite.class)
                .includeAllPackagesUnderHere().build();
    }

    public fullTestSuite(){
        super();
    }
}
