package us.luosl.lslt.lang;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

public class FileUtils {

    /**
     * 将一个文件中的每一行数据 转换为一个Stream
     * @param file file
     * @param charset 字符集
     * @return Stream<String>
     * @throws FileNotFoundException 文件未找到
     * @throws UnsupportedEncodingException 字符集不支持
     */
    public static Stream<String> asLineStream(File file, String charset)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader buffReader = openFileAsBufferedReader(file, charset);
        return buffReader.lines();
    }

    /**
     * 将文件读取未一个Iterator<String>
     * @param file file
     * @param charset 字符集
     * @return Iterator<String>
     * @throws FileNotFoundException 文件未找到
     * @throws UnsupportedEncodingException 字符集不支持
     */
    public static Iterator<String> asLineIterator(File file, String charset)
            throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader buffReader = openFileAsBufferedReader(file, charset);
        return new Iterator<String>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = buffReader.readLine();
                        return (nextLine != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    /**
     * 将一个文件打开为指定字符集的 BufferedReader
     * @param file file
     * @param charset 字符集
     * @return BufferedReader
     * @throws FileNotFoundException 文件未找到
     * @throws UnsupportedEncodingException 字符集不支持
     */
    public static BufferedReader openFileAsBufferedReader(File file, String charset)
            throws FileNotFoundException, UnsupportedEncodingException {
        assert null != file && null != charset;
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), charset);
        return new BufferedReader(reader);
    }

}
