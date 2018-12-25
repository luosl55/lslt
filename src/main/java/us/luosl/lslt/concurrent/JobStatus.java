package us.luosl.lslt.concurrent;

public enum JobStatus {
    INIT("INIT", 1),
    RUNNING("RUNNING", 2),
    CANCEL("CANCEL", 3),
    END_SUBMIT("END_SUBMIT", 4),
    ERROR("ERROR", 5),
    CALLBACK_ERROR("CALLBACK_ERROR", 6),
    COMPLETE("COMPLETE", 7);
    private String name;
    private int value;

    JobStatus(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
