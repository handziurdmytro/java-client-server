package dev.handziur.model;

public enum PacketType {
    GET_ITEM_QTY(1),
    REMOVE_ITEM_QTY(2),
    ADD_ITEM_QTY(3),
    CREATE_GROUP(4),
    ADD_GROUP_NAME(5),
    SET_ITEM_PRICE(6);

    private final int code;

    PacketType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static PacketType fromCode(int code) {
        for (PacketType type : values()) {
            if (type.code() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown command code: " + code);
    }
}
