package com.dianping.utils;

public interface ILock {

    Boolean tryLock(Long timeoutSec);

    void unLock();
}
