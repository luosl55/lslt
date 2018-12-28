package us.luosl.lslt.lang;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class CollectionUtils {

    /**
     * stream 构建字符串
     * @param stream stream
     * @param separator 分隔符
     * @param toStringOp 对象转字符串
     * @param <T> 对象类型
     * @return Optional<String>
     */
    public static <T> Optional<String> mkStr(Stream<T> stream, String separator, Function<T, String> toStringOp){
        return stream.map(toStringOp).reduce((a, b) -> String.format("%s%s%s",a, separator, b));
    }

    public static <T> Optional<String> mkStr(Stream<T> stream, String separator){
        return mkStr(stream, separator, Objects::toString);
    }

    public static <T> Optional<String> mkStr(Collection<T> collection, String separator, Function<T, String> toStringOp){
        return mkStr(collection.stream(), separator, toStringOp);
    }

    public static <T> Optional<String> mkStr(Collection<T> collection, String separator){
        return mkStr(collection, separator, Objects::toString);
    }

    public static <T> Optional<String> mkStr(T[] array, String separator, Function<T, String> toStringOp){
        return mkStr(Arrays.stream(array), separator, toStringOp);
    }

    public static <T> Optional<String> mkStr(T[] array, String separator){
        return mkStr(array, separator, Objects::toString);
    }

}
