package us.luosl.lslt.concurrent;

public enum JobStatus {
    INIT("INIT", 1),
    RUNNING("RUNNING", 2),
    AWAIT_COMPLETE("AWAIT_COMPLETE", 3),
    COMPLETE("COMPLETE", 4);

    private String name;
    private int value;

    JobStatus(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }
}
