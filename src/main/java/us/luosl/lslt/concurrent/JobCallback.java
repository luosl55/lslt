package us.luosl.lslt.concurrent;

public interface JobCallback<C> {
    void callback(C c) throws Exception;
}
