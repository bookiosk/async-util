package io.github.bookiosk.enums;

public enum WorkStage {

    INIT(0),
    FINISH(1),
    ERROR(2),
    WORKING(3),
    ;

    private final Integer value;

    WorkStage(Integer value){
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
