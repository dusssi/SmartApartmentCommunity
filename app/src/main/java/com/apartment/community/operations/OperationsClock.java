package com.apartment.community.operations;

/** Injectable UTC epoch clock; avoids java.time so the domain remains Android API 24 compatible. */
public interface OperationsClock {
    long nowMillis();

    OperationsClock SYSTEM = System::currentTimeMillis;
}
