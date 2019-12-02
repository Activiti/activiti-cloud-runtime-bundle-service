package org.activiti.cloud.services.message.connector.support;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.util.WhileLockedProcessor;
import org.springframework.messaging.MessagingException;
import org.springframework.util.ReflectionUtils;

public class LockTemplate {

    WhileLockedProcessor p;
    
    private final LockRegistry registry;

    public LockTemplate(LockRegistry registry) {
        this.registry = registry;
    }

    public <T> T tryLock(Object key, int timeoutDuration, TimeUnit tu, Callable<T> callable) {
        return this.doExecuteWithLock(key, lock -> lock.tryLock(timeoutDuration, tu), callable);
    }

    public <T> T tryLock(Object key, Callable<T> callable) {
        return this.doExecuteWithLock(key, Lock::tryLock, callable);
    }

    public <T> T lockInterruptibly(Object key, Callable <T> callable) {
        return this.executeWithLock(key, Lock::lockInterruptibly, callable);
    }

    public void lockInterruptibly(Object key, Runnable runnable) {
        Callable<Void> callable = () -> {
            runnable.run(); 
            return null; 
        };
        
        this.executeWithLock(key, Lock::lockInterruptibly, callable);
    }
    
    private <T> T doExecuteWithLock(Object key, 
                                    ExceptionSwallowingFunction<Lock, Boolean> lockProducer, 
                                    Callable<T> callable) {
        try {
            Lock lock = registry.obtain(key);
            boolean lockAcquired = lockProducer.apply(lock);
            if (lockAcquired) {
                try {
                    return callable.call();
                }
                finally {
                    lock.unlock();
                }
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Thread was interrupted while performing task", e);
        }
        catch (Exception e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null;
    }
    
    private <T> T executeWithLock(Object key, 
                                 ExceptionSwallowingProvider<Lock> lockProvider, 
                                 Callable <T> callable) {
        try {
            Lock lock = registry.obtain(key);
            lockProvider.apply(lock);
            try {
                return callable.call();
            }
            finally {
                lock.unlock();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Thread was interrupted while performing task", e);
        }
        catch (Exception e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        
        return null;
    }
    
    private interface ExceptionSwallowingProvider<I> {
        void apply(I i) throws Exception;
    }

    private interface ExceptionSwallowingFunction<I, O> {
        O apply(I i) throws Exception;
    }
}