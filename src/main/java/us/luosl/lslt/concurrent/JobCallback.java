package us.luosl.lslt.concurrent;

/**
 * job 回调
 * @param <C>
 */
@FunctionalInterface
public interface JobCallback<C> {
    void callback(C c) throws Exception;
}
